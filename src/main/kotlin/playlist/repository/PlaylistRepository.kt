package com.wafflestudio.seminar.spring2023.playlist.repository

import com.wafflestudio.seminar.spring2023.playlist.service.PlaylistView
import jakarta.persistence.Tuple
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface PlaylistRepository : JpaRepository<PlaylistEntity, Long> {
    @Query("""
        SELECT p FROM playlists p 
        JOIN FETCH p.songs ps
        WHERE p.id = :id
    """)
    fun findByIdWithSongs(id: Long): PlaylistEntity?

    @Modifying
    @Transactional
    @Query("UPDATE playlists p set p.viewCnt = p.viewCnt + 1 WHERE p.id = :playlistId")
    fun increaseViewCntById(playlistId:Long)

    @Query("""
        SELECT new com.wafflestudio.seminar.spring2023.playlist.service.PlaylistView(p.id, p.viewCnt) FROM playlists p
        WHERE p.id IN :ids
    """)
    fun findViewsByIds(ids: List<Long>): List<PlaylistView>
}

