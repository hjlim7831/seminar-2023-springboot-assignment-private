package com.wafflestudio.seminar.spring2023.playlist.service

import com.wafflestudio.seminar.spring2023.playlist.repository.PlaylistRepository
import com.wafflestudio.seminar.spring2023.playlist.repository.PlaylistViewEntity
import com.wafflestudio.seminar.spring2023.playlist.repository.PlaylistViewRepository
import com.wafflestudio.seminar.spring2023.playlist.service.SortPlaylist.Type
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

@Service
class PlaylistViewServiceImpl(
    private val playlistViewRepository: PlaylistViewRepository,
    private val playlistRepository: PlaylistRepository
) : PlaylistViewService, SortPlaylist {

    /**
     * 스펙:
     *  1. 같은 유저-같은 플레이리스트의 조회 수는 1분에 1개까지만 허용한다.
     *  2. PlaylistView row 생성, PlaylistEntity의 viewCnt 업데이트가 atomic하게 동작해야 한다. (둘 다 모두 성공하거나, 둘 다 모두 실패해야 함)
     *  3. 플레이리스트 조회 요청이 동시에 다수 들어와도, 요청이 들어온 만큼 조회 수가 증가해야한다. (동시성 이슈가 없어야 함)
     *  4. 성공하면 true, 실패하면 false 반환
     *
     * 조건:
     *  1. Synchronized 사용 금지.
     *  2. create 함수 처리가, 플레이리스트 조회 API 응답시간에 영향을 미치면 안된다.
     *  3. create 함수가 실패해도, 플레이리스트 조회 API 응답은 성공해야 한다.
     *  4. Future가 리턴 타입인 이유를 고민해보며 구현하기.
     */

    /**
     * 조회수 생성 함수
     */
    // 공유락 걸기 (WRITE 가 READ 에 영향을 미치면 안됨)
    @Transactional
    @Lock(value = LockModeType.PESSIMISTIC_READ)
    override fun create(playlistId: Long, userId: Long, at: LocalDateTime): Future<Boolean> {
        // [1] playlist_views 에 1분 내에 조회된 결과가 있는지 확인하기.
        val results = playlistViewRepository.findByPlaylistIdAndUserIdAndCreatedAtAfter(playlistId, userId, at.minusMinutes(1))
        
        // [1]-1 1분 내에 조회된 결과가 없으면, 조회수 추가하기
        if (results.isEmpty()) {
            return CompletableFuture.runAsync {
                playlistViewRepository.save(PlaylistViewEntity(
                    playlistId = playlistId,
                    userId = userId,
                    createdAt = at
                ))
                playlistRepository.increaseViewCntById(playlistId)
            }.thenApply { true }
        }
        
        // [1]-2 1분 내에 조회된 결과가 있다면, 조회수 추가 불가. false 리턴하기
        return CompletableFuture.completedFuture(false)
    }

    /**
     * playlist 정렬 함수.
     */
    override fun invoke(playlists: List<PlaylistBrief>, type: Type, at: LocalDateTime): List<PlaylistBrief> {
        return when (type) {
            Type.DEFAULT -> playlists // 기본 정렬
            Type.VIEW -> viewPlaylist(playlists)
            Type.HOT -> hotPlaylist(playlists, at)
        }
    }

    // 전체 조회 수 정렬
    // playlistEntity 에 있는 viewCnt 로 고려
    private fun viewPlaylist(playlists:List<PlaylistBrief>):List<PlaylistBrief> {
        val playlistViews = playlistRepository.findViewsByIds(playlists.map { it.id })
        val viewMap = playlistViews.associate { it.id to it.viewCnt }
        return playlists.sortedBy { viewMap[it.id] }.reversed()
    }

    // 최근 1시간 조회 수 정렬
    // playlistViews 에서 최근 1시간만 고려해서 체크해야 함
    private fun hotPlaylist(playlists:List<PlaylistBrief>, at:LocalDateTime):List<PlaylistBrief> {
        val playlistViews = playlistViewRepository.findHotViewsByIds(at.minusHours(1), playlists.map { it.id })
        val viewMap = playlistViews.associate { it.id to it.viewCnt }
        return playlists.sortedBy { viewMap[it.id] }.reversed()
    }
}

data class PlaylistView(
    val id:Long,
    val viewCnt:Int
)

data class PlaylistHotView(
    val id:Long,
    val viewCnt:Long
)
