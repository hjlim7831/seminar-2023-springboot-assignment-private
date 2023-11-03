package com.wafflestudio.seminar.spring2023.song.repository

import com.wafflestudio.seminar.spring2023.admin.service.BatchAlbumInfo
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import java.lang.RuntimeException
import java.sql.PreparedStatement

@Repository
class BulkRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val transactionTemplate: TransactionTemplate
) {

    fun saveAll(albumInfos: List<BatchAlbumInfo>, savedArtists:List<ArtistEntity>, newArtists: List<String>) {
        
        // 하나의 트랜잭션으로 묶기
        transactionTemplate.execute { _ ->

            // [1] DB에 저장되지 않은 아티스트 리스트 저장하기
            val newArtistIds = saveNewArtists(newArtists)

            // artist 이름으로 id를 알 수 있는 HashMap 만들기
            var artistMap = HashMap<String, Long>()
            for (artist in savedArtists) {
                artistMap[artist.name] = artist.id
            }

            for (i in newArtists.indices) {
                artistMap[newArtists[i]] = newArtistIds[i]
            }
            // [2] 앨범 저장
            val newAlbumIds = saveAlbums(albumInfos, artistMap)

            // [3] 노래 저장
            val newSongIds = saveSongs(albumInfos, newAlbumIds)
            println(newSongIds)

            // [4] 노래 - 아티스트 연관관계 저장
            saveSongArtists(albumInfos, artistMap, newSongIds)
        }
    }

    private fun getSavedIdList(savedList:List<Any>):List<Long> {
        return jdbcTemplate.queryForObject("SELECT last_insert_id()", Long::class.java)?.let { no ->
            savedList.mapIndexed {index, _ -> no + index - savedList.size + 1}
        }?: emptyList()
    }

    private fun saveNewArtists(newArtists: List<String>):List<Long> {
        val sql:String = "INSERT INTO artists (name) VALUES (?)"
        jdbcTemplate.batchUpdate(sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setString(1, newArtists[i])
                }
                override fun getBatchSize() = newArtists.size
            }
        )

        // DB에 저장하면서 할당된 새로운 id List 찾기
        return getSavedIdList(newArtists)

    }

    private fun saveAlbums(albumInfos: List<BatchAlbumInfo>, artistMap: HashMap<String, Long>):List<Long> {
        val sql:String = "INSERT INTO albums (title, image, artist_id) VALUES (?, ?, ?)"
        jdbcTemplate.batchUpdate(sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setString(1, albumInfos[i].title)
                    ps.setString(2, albumInfos[i].image)
                    ps.setLong(3, artistMap.getValue(albumInfos[i].artist))
                }
                override fun getBatchSize() = albumInfos.size
            }
        )
        return getSavedIdList(albumInfos)
    }

    private fun saveSongs(albumInfos:List<BatchAlbumInfo>, newAlbumIds:List<Long>):List<Long> {
        val sql = "INSERT INTO songs (title, duration, album_id) VALUES (?, ?, ?)"

        val sqlSongInfos = albumInfos.mapIndexed { index, albumInfo ->
            albumInfo.songs.map { song ->
                SqlSongInfo(
                    title = song.title,
                    duration = song.duration,
                    albumId = newAlbumIds[index]
                )
            }
        }.flatten()

        jdbcTemplate.batchUpdate(sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setString(1, sqlSongInfos[i].title)
                    ps.setInt(2, sqlSongInfos[i].duration)
                    ps.setLong(3, sqlSongInfos[i].albumId)
                }
                override fun getBatchSize() = sqlSongInfos.size
            }
        )

        return getSavedIdList(sqlSongInfos)
    }

    private fun saveSongArtists(albumInfos: List<BatchAlbumInfo>, artistMap: HashMap<String, Long>, newSongIds:List<Long>) {
        val sql = "INSERT INTO song_artists (artist_id, song_id) VALUES (?, ?)"

        val songInfos = albumInfos.map { albumInfo ->
            albumInfo.songs
        }.flatten()

        val sqlSongArtistInfos = songInfos.mapIndexed { index, song ->
                song.artists.map { artist ->
                    SqlSongArtistInfo(
                        artistId = artistMap.getValue(artist),
                        songId = newSongIds[index]
                    )
                }
            }.flatten()

        jdbcTemplate.batchUpdate(sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setLong(1, sqlSongArtistInfos[i].artistId)
                    ps.setLong(2, sqlSongArtistInfos[i].songId)
                }
                override fun getBatchSize() = sqlSongArtistInfos.size
            }
        )
    }
}

data class SqlSongInfo(
    val title:String,
    val duration:Int,
    val albumId:Long
)

data class SqlSongArtistInfo(
    val artistId:Long,
    val songId:Long
)