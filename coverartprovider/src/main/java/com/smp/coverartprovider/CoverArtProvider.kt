package com.smp.coverartprovider

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.WindowManager
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

fun LOGI(msg: String) {
    Log.i("COVERLOG", msg)
}

class CoverArtProvider : DocumentsProvider() {


    val screenSize: Point
        get() {
            val x = Resources.getSystem().displayMetrics.widthPixels
            val y = Resources.getSystem().displayMetrics.heightPixels
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
        LOGI("queryROOTS")
        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            with(newRow()) {
                add(Root.COLUMN_ROOT_ID, "rootroot")
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
        return AssetFileDescriptor(
            fdFromAlbumId(documentId.toLong(), sizeHint), 0,
            AssetFileDescriptor.UNKNOWN_LENGTH
        )
    }

    private fun fdFromAlbumId(albumId: Long, sizeHint: Point): ParcelFileDescriptor? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            null
        } else {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumId
            )
            val size = Size(sizeHint.x, sizeHint.y)
            val bitmap = context!!.contentResolver.loadThumbnail(uri, size, null)

            fdFromBitmap(bitmap)
        }
    }

    private fun fdFromBitmap(bitmap: Bitmap): ParcelFileDescriptor? {
        val tempFile: File = File.createTempFile("image", "png", context!!.cacheDir)

        FileOutputStream(tempFile).use { out ->
            return try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (e: IOException) {
                null
            } finally {
                tempFile.delete()
            }
        }
    }


    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        //Fix, inefficient
        LOGI(documentId)
        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            if (documentId == "root") {
                makeRootRow(this)
            } else {
                LOGI("query regular doc")
                val album =
                    Album.getAllAlbums(context!!).find { it.albumId == documentId.toLong() }
                        ?: throw FileNotFoundException()
                makeAlbumRow(this, album)
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
            add(Document.COLUMN_SIZE, 1000L)
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
            LOGI("return count " + count)
        }
    }

    private fun makeAlbumRow(cursor: MatrixCursor, album: Album) {
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, album.albumId.toString())
            add(Document.COLUMN_DISPLAY_NAME, album.albumName)
            add(Document.COLUMN_SUMMARY, album.artistName)
            add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL)
            add(Document.COLUMN_MIME_TYPE, "image/png")
            add(Document.COLUMN_SIZE, 0)
            add(Document.COLUMN_LAST_MODIFIED, 0)
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        return fdFromAlbumId(documentId.toLong(), screenSize)
    }
}