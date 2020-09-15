package com.smp.coverartprovider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import java.io.FileNotFoundException

fun LOGI(msg: String) {
    Log.i("COVERART", msg)
}

class CoverArtProvider : DocumentsProvider() {
    /**
     * Default root projection: everything but Root.COLUMN_MIME_TYPES
     */
    private val DEFAULT_ROOT_PROJECTION = arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_FLAGS,
        Root.COLUMN_TITLE,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_ICON,
        Root.COLUMN_AVAILABLE_BYTES
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
            with (newRow()) {
                add(Root.COLUMN_ROOT_ID, "root")
                add(Root.COLUMN_DOCUMENT_ID, "root")
                add(Root.COLUMN_TITLE, context!!.resources.getString(R.string.title_root))
                add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY)
                add(Root.COLUMN_ICON, R.drawable.cover_art)
            }
        }
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        //Fix, inefficient
        LOGI(documentId)
        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            if (documentId == "root") {
                makeRootRow(this)
            } else {
                val album = Album.getAllAlbums(context!!).find { it.albumId == documentId.toLong() }
                    ?: throw FileNotFoundException()
                makeAlbumRow(this, album)
            }
        }
    }

    private fun makeRootRow(cursor: MatrixCursor) {
        with (cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, "root")
            add(Document.COLUMN_DISPLAY_NAME, "root")
            add(Document.COLUMN_SUMMARY, "root")
            add(
                Document.COLUMN_FLAGS,
                Document.FLAG_DIR_PREFERS_GRID)
            add(Document.COLUMN_MIME_TYPE)
            add(Document.COLUMN_SIZE, 0)
            add(Document.COLUMN_LAST_MODIFIED, 0)
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {

        LOGI("queryChilds")
        
        val albums = Album.getAllAlbums(context!!)

        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            albums.forEach { album ->
                makeAlbumRow(this, album)
            }
           
        }
    }

    private fun makeAlbumRow(cursor: MatrixCursor, album: Album) {
        with (cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, album.albumId)
            add(Document.COLUMN_DISPLAY_NAME, album.albumName)
            add(Document.COLUMN_SUMMARY, album.artistName)
            add(Document.COLUMN_FLAGS, 0)
            add(Document.COLUMN_MIME_TYPE)
            add(Document.COLUMN_SIZE, 0)
            add(Document.COLUMN_LAST_MODIFIED, 0)
        }
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        TODO("Not yet implemented")
    }

    override fun getDocumentType(documentId: String?): String? {
        return if (documentId == "root") {
            Document.MIME_TYPE_DIR
        } else {
            "application/octet-stream"
        }
    }
}