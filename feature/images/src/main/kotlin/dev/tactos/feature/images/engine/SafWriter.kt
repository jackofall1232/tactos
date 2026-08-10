package dev.tactos.feature.images.engine

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * All output goes through the Storage Access Framework, which needs no
 * manifest permission on any supported API level — the zero-permission
 * invariant is structural, not aspirational. Single saves write to an
 * ACTION_CREATE_DOCUMENT uri; batch saves create documents inside an
 * ACTION_OPEN_DOCUMENT_TREE grant.
 */
object SafWriter {

    /** Overwrite the (freshly created) document at [uri]. */
    fun write(context: Context, uri: Uri, bytes: ByteArray): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                ?: return false
            true
        }.getOrDefault(false)

    /**
     * Create [displayName].[extension-per-mime] inside the tree grant and
     * write [bytes] into it. The provider dedups colliding names itself
     * (" (1)" suffixes). Returns the created document uri, or null.
     */
    fun createInTree(
        context: Context,
        treeUri: Uri,
        displayName: String,
        mimeType: String,
        bytes: ByteArray,
    ): Uri? = runCatching {
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val doc = DocumentsContract.createDocument(
            context.contentResolver, parent, mimeType, displayName,
        ) ?: return null
        if (write(context, doc, bytes)) doc else null
    }.getOrNull()
}
