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
import android.util.Size
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.system.measureTimeMillis

fun LOGI(msg: String) {
    Log.i("COVERLOG", msg)
}

class CoverArtProvider : DocumentsProvider() {

    private val fullSize: Point
        get() {
            val x = Resources.getSystem().displayMetrics.widthPixels * 2
            val y = Resources.getSystem().displayMetrics.heightPixels * 2
            return Point(x, y)
        }


    private val rootProjection = arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_FLAGS,
        Root.COLUMN_TITLE,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_ICON
    )

    private val documentProjection = arrayOf(
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
        return MatrixCursor(projection ?: rootProjection).apply {
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
                AssetFileDescriptor(
                    ParcelFileDescriptor.open(
                        File(album.artUri),
                        ParcelFileDescriptor.MODE_READ_ONLY
                    ), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH
                )
            }

        } catch (e: FileNotFoundException) {
            null
        }
    }


    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        return MatrixCursor(projection ?: documentProjection).apply {
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
            add(Document.COLUMN_FLAGS, Document.FLAG_DIR_PREFERS_GRID)
            add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            add(Document.COLUMN_SIZE, 0L)
            add(Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val albums = Album.getAllAlbums(context!!)

        return MatrixCursor(projection ?: documentProjection).apply {
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
        //if (fdFromAlbum(album, Point(0, 0)) == null) return
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, album.albumId)
            add(Document.COLUMN_DISPLAY_NAME, album.albumName)
            add(Document.COLUMN_SUMMARY, album.artistName)
            add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL)
            add(Document.COLUMN_MIME_TYPE, "image/*")
            add(Document.COLUMN_SIZE, null)
            add(Document.COLUMN_LAST_MODIFIED, null)
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

    override fun getDocumentType(documentId: String): String {
        return if (documentId == "root") {
            Document.MIME_TYPE_DIR
        } else {
            "image/*"
        }
    }
}