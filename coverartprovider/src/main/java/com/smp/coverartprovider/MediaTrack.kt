package com.smp.coverartprovider

import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore

import java.util.ArrayList

import kotlinx.android.parcel.Parcelize
import java.io.File
import java.lang.IllegalStateException
import java.lang.NullPointerException

@Parcelize
class MediaTrack constructor(
        val trackName: String = "",
        val artistName: String = "",
        val songId: Long = 0L,
        val location: String = "",
        val duration: Long = 0L,
        val isInLibrary: Boolean = false, //does file exist on disk?
        val albumId: Long = 0L,
        val albumName:String = "",
        val artistId: Long = 0L,
        val trackNumber: Int = -1,
        val year: Int = -1,
        val dateModified: Long = -1L,
        val idInPlaylist: Long = -1L
        ) : Parcelable {

    constructor(trackName: String, albumName: String, artistName: String, fileName: String) :
            this (trackName = trackName, albumName = albumName, artistName = artistName,
                    location = fileName)
    constructor(fileName: String) :
            this(location = fileName)

    val file: File
        get() = File(location)

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && other is MediaTrack &&
                trackName == other.trackName &&
                artistName == other.artistName &&
                albumName == other.albumName &&
                songId == other.songId &&
                albumId == other.albumId &&
                artistId == other.artistId &&
                location == other.location &&
                duration == other.duration &&
                trackNumber == other.trackNumber &&
                year == other.year &&
                dateModified == other.dateModified &&
                idInPlaylist == other.idInPlaylist &&
                isInLibrary == other.isInLibrary
    }

    override fun toString(): String {
        return trackName
    }

    companion object {
        fun buildMediaTrackList(context: Context, audioCur: Cursor, videoCur: Cursor, res: Resources): List<MediaTrack> {
            return (buildAudioList(context, audioCur, res) + buildVideoList(videoCur, res))

        }

        private fun buildVideoList(videoCur: Cursor, res: Resources): List<MediaTrack> {
            val video = ArrayList<MediaTrack>(videoCur.count)

            val titleIndex = videoCur.getColumnIndex(MediaStore.Video.Media.TITLE)
            val artistIndex = videoCur.getColumnIndex(MediaStore.Video.Media.ARTIST)
            var idIndex = videoCur.getColumnIndex(MediaStore.Video.Media._ID)
            val dataIndex = videoCur.getColumnIndex(MediaStore.Video.Media.DATA)
            val durationIndex = videoCur.getColumnIndex(MediaStore.Video.Media.DURATION)
            val dateModifiedIndex = videoCur.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)

            val unknownSong = res.getString(R.string.unknown_song)
            val unknownAuthor = res.getString(R.string.unknown_artist)

            for (i in 0 until videoCur.count) {
                try {
                    videoCur.moveToPosition(i)

                    val loc = videoCur.getString(dataIndex) ?: ""
                    if (!loc.run { endsWith(".3gp", true) || endsWith(".mp4", true) }) continue


                    val next = MediaTrack(
                            trackName = parseUnknown(videoCur.getString(titleIndex), unknownSong),
                            songId = videoCur.getLong(idIndex),
                            artistName = parseUnknown(videoCur.getString(artistIndex), unknownAuthor),
                            location = videoCur.getString(dataIndex),
                            duration = videoCur.getLong(durationIndex),
                            isInLibrary = true,
                            dateModified = videoCur.getLong(dateModifiedIndex)
                    )

                    video.add(next)
                } catch (e: IllegalStateException) {}
            }
            return video
        }

        fun buildAudioList(context: Context, audioCur: Cursor, res: Resources): List<MediaTrack> {
            val audio = ArrayList<MediaTrack>(audioCur.count)

            val titleIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.ARTIST)

            val testId = audioCur.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)

            val idInPlaylistIndex = audioCur.getColumnIndex(MediaStore.Audio.Playlists.Members._ID)

            val idIndex = when {
                testId >= 0 -> testId
                else -> audioCur.getColumnIndex(MediaStore.Audio.Media._ID)
            }

            val dataIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val podcastIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.IS_PODCAST)
            val songIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
            val albumIdIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
            val albumIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val trackIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val dateModifiedIndex = audioCur.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

            val unknownSong = res.getString(R.string.unknown_song)
            val unknownAuthor = res.getString(R.string.unknown_artist)
            val unknownAlbum = res.getString(R.string.unknown_album)


            for (i in 0 until audioCur.count) {
                try {
                    audioCur.moveToPosition(i)

                    val isPodcast = audioCur.getInt(podcastIndex) != 0
                    val isSong = audioCur.getInt(songIndex) != 0

                    val next = MediaTrack(
                            trackName = parseUnknown(audioCur.getString(titleIndex), unknownSong),
                            songId = audioCur.getLong(idIndex),
                            artistName = parseUnknown(audioCur.getString(artistIndex), unknownAuthor),
                            location = audioCur.getString(dataIndex),
                            duration = audioCur.getLong(durationIndex),
                            isInLibrary = true,
                            albumId = if (isSong) audioCur.getLong(albumIdIndex) else -1L,
                            artistId = if (isSong) audioCur.getLong(artistIdIndex) else -1L,
                            trackNumber = if (isSong) audioCur.getInt(trackIndex) else -1,
                            year = if (isSong) audioCur.getInt(yearIndex) else -1,
                            albumName = if (isSong) parseUnknown(audioCur.getString(albumIndex), unknownAlbum) else "",
                            dateModified = audioCur.getLong(dateModifiedIndex),
                            idInPlaylist = if (idInPlaylistIndex >= 0) audioCur.getLong(idInPlaylistIndex) else -1L
                    )

                    audio.add(next)
                } catch (e: IllegalStateException) {}

            }
            return audio
        }
    }

    override fun describeContents(): Int {
        return 0
    }
}


