package com.wafflestudio.seminar.spring2023.customplaylist.repository

import org.springframework.data.jpa.repository.JpaRepository

interface CustomPlaylistSongRepository : JpaRepository<CustomPlaylistSongEntity, Long> {

}