package com.smp.coverartprovider

import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.provider.MediaStore
import java.util.*

class Album constructor(
    val albumId: Long = 0L,
    val albumName: String = "",
    val artistId: Long = 0,
    val artistName: String = "",
    val year: Int = 0,
    val artUri: String = ""
) {

    companion object {

        interface AlbumSortOrder {
            companion object {
                const val ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
                const val ALBUM_Z_A = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER + " DESC"
            }
        }

        private val SONG_PROJECTION = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_PODCAST,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_MUSIC)


        private val ALBUM_PROJECTION = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR,
            MediaStore.Audio.Albums.ALBUM_ART
        )

        fun getAlbumSongs(context: Context, albumId: Long): List<MediaTrack> {
            val audioSelection = MediaStore.Audio.Media.IS_MUSIC + " != 0" + " AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + " = " + albumId + " AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''"
            val audioCur = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION, audioSelection, null, null)
                ?: return emptyList()
            audioCur.use {
                return MediaTrack.buildAudioList(context, audioCur, context.resources).sortedBy { it.trackNumber }
            }
        }

        fun getAllAlbums(context: Context, sortOrder: String = AlbumSortOrder.ALBUM_A_Z): List<Album> {
            val cur = context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_PROJECTION, null, null, sortOrder
            ) ?: return emptyList()

            cur.use {
                return buildAlbumList(cur, context.resources)
            }
        }
        fun getAlbumFromId(context: Context, albumId: Long): List<Album> {
            val albumSelection = MediaStore.Audio.Albums._ID + " = " + albumId
            val cur = context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_PROJECTION, albumSelection, null, AlbumSortOrder.ALBUM_A_Z)
                ?: return emptyList()

            cur.use {
                return buildAlbumList(cur, context.resources)
            }
        }
        private fun buildAlbumList(cur: Cursor, res: Resources, artistId: Long = -1L): List<Album> {
            val albums = ArrayList<Album>(cur.count)

            val albumIdx = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)

            val idIndex = if (albumIdx != -1)
                albumIdx
            else
                cur.getColumnIndex(MediaStore.Audio.Albums._ID)

            val albumIndex = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
            val artistIndex = cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
            val artistIdIndex = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
            val yearIndex = cur.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)
            val artIndex = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)

            val unknownAlbum = res.getString(R.string.unknown_album)
            val unknownArtist = res.getString(R.string.unknown_artist)
            val unknownArtUri = ""

            for (i in 0 until cur.count) {
                cur.moveToPosition(i)
                val next = Album(
                    albumId = cur.getLong(idIndex),
                    albumName = parseUnknown(cur.getString(albumIndex), unknownAlbum),
                    artistName = parseUnknown(cur.getString(artistIndex), unknownArtist),
                    artistId = if (artistId == -1L) cur.getLong(artistIdIndex) else artistId,
                    year = cur.getInt(yearIndex),
                    artUri = parseUnknown(cur.getString(artIndex), unknownArtUri)
                )
                albums.add(next)
            }
            return albums
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && other is Album &&
                albumId == other.albumId &&
                albumName == other.albumName &&
                artistId == other.artistId &&
                artistName == other.artistName &&
                year == other.year &&
                artUri == other.artUri
    }

    override fun toString(): String {
        return albumName
    }
}

fun parseUnknown(value: String?, convertValue: String): String {
    return if (value == null || value == MediaStore.UNKNOWN_STRING) {
        convertValue
    } else {
        value
    }
}