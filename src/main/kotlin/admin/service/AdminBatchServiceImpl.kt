package com.wafflestudio.seminar.spring2023.admin.service

import com.wafflestudio.seminar.spring2023.song.repository.*
import org.springframework.stereotype.Service

@Service
class AdminBatchServiceImpl(
    private val artistRepository: ArtistRepository,
    private val bulkRepository: BulkRepository,
) : AdminBatchService {

    override fun insertAlbums(albumInfos: List<BatchAlbumInfo>) {
        // 앨범, 아티스트, 곡 모두 atomic 하게 입력되어야 함
        // 앨범, 곡 : 새로 인서트, 아티스트 : 없으면 새로 인서트
        
        // [1] 아티스트 정보 뽑기
        
        // [1]-1 새 파일에 있는 아티스트들을 전부 뽑기
        val albumArtists = albumInfos.map {it.artist}
        val songArtists = albumInfos.map {a -> a.songs.flatMap {s -> s.artists}}.flatten()
        val nameList = HashSet(albumArtists) + HashSet(songArtists)
        
        // [1]-2 이미 DB에 저장된 아티스트 골라내기
        val savedArtists = artistRepository.findAllByNameList(nameList)
        val newArtists = ArrayList(nameList - HashSet(savedArtists.map {it.name}))

        // [2] DB에 데이터 저장하기
        bulkRepository.saveAll(albumInfos, savedArtists, newArtists)
    }
}
