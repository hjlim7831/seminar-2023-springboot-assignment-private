package com.wafflestudio.seminar.spring2023.playlist.repository

import com.wafflestudio.seminar.spring2023.playlist.service.PlaylistHotView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PlaylistViewRepository : JpaRepository<PlaylistViewEntity, Long> {

    @Query("""
        SELECT new com.wafflestudio.seminar.spring2023.playlist.service.PlaylistHotView(pv.playlistId, COUNT(pv.id))
        FROM playlist_views pv WHERE pv.createdAt > :criteria AND pv.playlistId IN :ids GROUP BY pv.playlistId""")
    fun findHotViewsByIds(criteria: LocalDateTime, ids:List<Long>):List<PlaylistHotView>

    fun findByPlaylistIdAndUserIdAndCreatedAtAfter(playlistId:Long, userId:Long, criteria:LocalDateTime):List<PlaylistViewEntity>

}
