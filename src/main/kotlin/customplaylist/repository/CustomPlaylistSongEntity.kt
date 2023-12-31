package com.wafflestudio.seminar.spring2023.customplaylist.repository

import com.wafflestudio.seminar.spring2023.song.repository.SongEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity(name = "custom_playlist_songs")
class CustomPlaylistSongEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_playlist_id")
    val customPlaylist: CustomPlaylistEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    val song: SongEntity,
)
