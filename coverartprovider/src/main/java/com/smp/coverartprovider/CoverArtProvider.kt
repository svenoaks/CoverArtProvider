package com.smp.coverartprovider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

fun LOGI(msg: String) {
    Log.i("COVERLOG", msg)
}

class CoverArtProvider : DocumentsProvider() {


    val fullSize: Point
        get() {
            val x = Resources.getSystem().displayMetrics.widthPixels * 2
            val y = Resources.getSystem().displayMetrics.heightPixels * 2
            return Point(x, y)
        }


    private val DEFAULT_ROOT_PROJECTION = arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_FLAGS,
        Root.COLUMN_TITLE,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_ICON
    )

    /**
     * Default document projection: everything but Document.COLUMN_ICON and
     * Document.COLUMN_SUMMARY
     */
    private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_FLAGS,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_SIZE,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_SUMMARY
    )

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            with(newRow()) {
                add(Root.COLUMN_ROOT_ID, "root")
                add(Root.COLUMN_DOCUMENT_ID, "root")
                add(Root.COLUMN_TITLE, context!!.resources.getString(R.string.title_root))
                add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY)
                add(Root.COLUMN_ICON, R.drawable.cover_art)
            }
        }
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal
    ): AssetFileDescriptor? {
        val album = Album.getAlbumFromId(context!!, documentId.toLong())
        return if (album.isNotEmpty()) {
            fdFromAlbum(album[0], fullSize)
        } else {
            null
        }
    }

    private fun fdFromAlbum(album: Album, sizeHint: Point): AssetFileDescriptor? {
        val opts = Bundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                putParcelable(ContentResolver.EXTRA_SIZE, sizeHint)
            }
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = uriFromAlbum(album)
                context!!.contentResolver.openTypedAssetFileDescriptor(
                    uri, "image/*", opts
                )
            } else {
                AssetFileDescriptor(ParcelFileDescriptor.open(
                    File(album.artUri),
                    ParcelFileDescriptor.MODE_READ_ONLY), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH)
            }

        } catch (e: FileNotFoundException) {
            null
        }
    }


    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        LOGI(documentId)
        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            if (documentId == "root") {
                makeRootRow(this)
            } else {
                val albums = Album.getAlbumFromId(context!!, documentId.toLong())
                if (albums.isNotEmpty()) {
                    makeAlbumRow(this, albums[0])
                }
            }
        }
    }

    private fun makeRootRow(cursor: MatrixCursor) {
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, "root")
            add(Document.COLUMN_DISPLAY_NAME, "root")
            add(Document.COLUMN_SUMMARY, "root")
            add(Document.COLUMN_FLAGS, 0)
            add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            add(Document.COLUMN_SIZE, 0L)
            add(Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
        }
    }

    interface SongSortOrder {
        companion object {
            const val SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
            const val SONG_Z_A = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " DESC"
            const val SONG_MODIFIED_ASC = MediaStore.Audio.Media.DATE_MODIFIED
            const val SONG_MODIFIED_DESC = MediaStore.Audio.Media.DATE_MODIFIED + " DESC"
        }
    }

    /* val SONG_PROJECTION = arrayOf(
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

     fun getAllSongs(context: Context, sortOrder: String = SongSortOrder.SONG_A_Z): List<String> {
         val musicSelection = MediaStore.Audio.Media.IS_MUSIC + " != 0" + " AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''"
         val cur = context.contentResolver.query(
             MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
             SONG_PROJECTION,
             musicSelection,
             null,
             sortOrder
         )
             ?: return emptyList()
         cur.use {
             LOGI("songs = " + cur.count)
             return listOf()
         }
     }*/

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {

        val albums = Album.getAllAlbums(context!!)

        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            albums.forEach { album ->
                makeAlbumRow(this, album)
            }
        }
    }

    private fun uriFromAlbum(album: Album): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, album.albumId)
        } else {
            Uri.parse(album.artUri)
        }
    }

    private fun makeAlbumRow(cursor: MatrixCursor, album: Album) {

        val size = fdFromAlbum(album, fullSize)?.length ?: 0L
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, album.albumId)
            add(Document.COLUMN_DISPLAY_NAME, album.albumName)
            add(Document.COLUMN_SUMMARY, album.artistName)
            add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL)
            add(Document.COLUMN_MIME_TYPE, "image/*")
            add(Document.COLUMN_SIZE, size)
            add(Document.COLUMN_LAST_MODIFIED, 0)
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        val album = Album.getAlbumFromId(context!!, documentId.toLong())
        return if (album.isNotEmpty()) {
            fdFromAlbum(album[0], fullSize)?.parcelFileDescriptor
        } else {
            null
        }
    }
}