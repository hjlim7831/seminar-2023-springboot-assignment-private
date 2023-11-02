package com.wafflestudio.seminar.spring2023.customplaylist.service

import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistEntity
import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistRepository
import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistSongEntity
import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistSongRepository
import com.wafflestudio.seminar.spring2023.song.repository.SongEntity
import com.wafflestudio.seminar.spring2023.song.repository.SongRepository
import com.wafflestudio.seminar.spring2023.song.service.Song
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 스펙:
 *  1. 커스텀 플레이리스트 생성시, 자동으로 생성되는 제목은 "내 플레이리스트 #{내 커스텀 플레이리스트 갯수 + 1}"이다.
 *  2. 곡 추가 시  CustomPlaylistSongEntity row 생성, CustomPlaylistEntity의 songCnt의 업데이트가 atomic하게 동작해야 한다. (둘 다 모두 성공하거나, 둘 다 모두 실패해야 함)
 *
 * 조건:
 *  1. Synchronized 사용 금지.
 *  2. 곡 추가 요청이 동시에 들어와도 동시성 이슈가 없어야 한다.(PlaylistViewServiceImpl에서 동시성 이슈를 해결한 방법과는 다른 방법을 사용할 것)
 *  3. JPA의 변경 감지 기능을 사용해야 한다.
 */
@Service
class CustomPlaylistServiceImpl(
    private val customPlaylistRepository: CustomPlaylistRepository,
    private val customPlaylistSongRepository: CustomPlaylistSongRepository,
    private val songRepository: SongRepository,
) : CustomPlaylistService {

    override fun get(userId: Long, customPlaylistId: Long): CustomPlaylist {
        val playlist = customPlaylistRepository.findByIdAndUserIdWithJoinFetch(id = customPlaylistId, userId = userId)?:throw CustomPlaylistNotFoundException()

        val songs = songRepository.findAllByIdWithJoinFetch(ids = playlist.songs.map {it.song.id})

        return customPlaylist(playlist, songs)
    }

    override fun gets(userId: Long): List<CustomPlaylistBrief> {
        val playlists = customPlaylistRepository.findAllByUserId(userId)
        return playlists.map(::customPlaylistBrief)
    }

    @Transactional
    override fun create(userId: Long): CustomPlaylistBrief {
        val count = customPlaylistRepository.countByUserId(userId)
        val entity = CustomPlaylistEntity(userId = userId, title = "내 플레이리스트 #${count+1}")
        customPlaylistRepository.save(entity)
        return customPlaylistBrief(entity)
    }

    /**
     * title 바꾸기
     */
    @Transactional
    override fun patch(userId: Long, customPlaylistId: Long, title: String): CustomPlaylistBrief {
        val playlist =
            customPlaylistRepository.findByIdAndUserId(id = customPlaylistId, userId = userId)
                ?: throw CustomPlaylistNotFoundException()
        playlist.title = title
        return customPlaylistBrief(playlist)
    }

    @Retry
    @Transactional
    override fun addSong(userId: Long, customPlaylistId: Long, songId: Long): CustomPlaylistBrief {
        val playlist = customPlaylistRepository.findByIdAndUserId(id = customPlaylistId, userId = userId)
            ?: throw CustomPlaylistNotFoundException()
        val song = songRepository.findById(songId).orElseThrow { SongNotFoundException() }

        customPlaylistSongRepository.save(CustomPlaylistSongEntity(
            customPlaylist = playlist,
            song = song
        ))

        playlist.songCnt = playlist.songCnt + 1
        return customPlaylistBrief(playlist)
    }

}
fun customPlaylist(entity:CustomPlaylistEntity, songEntities:List<SongEntity>) = CustomPlaylist(
    id = entity.id,
    title = entity.title,
    songs = songEntities.map(::Song)
)

fun customPlaylistBrief(entity:CustomPlaylistEntity) = CustomPlaylistBrief(
    id = entity.id,
    title = entity.title,
    songCnt = entity.songCnt
)