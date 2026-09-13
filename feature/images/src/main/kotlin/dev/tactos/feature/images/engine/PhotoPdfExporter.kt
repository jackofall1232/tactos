package dev.tactos.feature.images.engine

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** A4 dimensions in PDF points; fit preserves aspect ratio and centers within margins. */
internal object PhotoPdfLayout {
    data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun fit(width: Int, height: Int, landscape: Boolean, margin: Float = 24f): Bounds {
        require(width > 0 && height > 0)
        val pageWidth = if (landscape) 842f else 595f
        val pageHeight = if (landscape) 595f else 842f
        require(margin >= 0 && margin * 2 < minOf(pageWidth, pageHeight))
        val scale = minOf((pageWidth - 2 * margin) / width, (pageHeight - 2 * margin) / height)
        val left = (pageWidth - width * scale) / 2
        val top = (pageHeight - height * scale) / 2
        return Bounds(left, top, pageWidth - left, pageHeight - top)
    }
}

internal object PhotoPdfExporter {
    const val MAX_PHOTOS = 12

    /** Writes only to a newly created SAF document; removes incomplete output on any failure. */
    suspend fun export(context: Context, sources: List<Uri>, destination: Uri, landscape: Boolean) {
        require(destination !in sources) { "Choose a new PDF destination, separate from the source photos." }
        var completed = false
        try {
            withContext(Dispatchers.IO) {
                require(sources.isNotEmpty() && sources.size <= MAX_PHOTOS)
                val document = PdfDocument()
                try {
                    sources.forEachIndexed { index, source ->
                        currentCoroutineContext().ensureActive()
                        val bitmap = ImageLoading.decode(context, source, maxDimension = 1600)
                            ?: error("Couldn't read photo ${index + 1}. Choose the photos again.")
                        try {
                            val page = document.startPage(
                                PdfDocument.PageInfo.Builder(
                                    if (landscape) 842 else 595,
                                    if (landscape) 595 else 842,
                                    index + 1,
                                ).create(),
                            )
                            try {
                                page.canvas.drawColor(Color.WHITE)
                                val fit = PhotoPdfLayout.fit(bitmap.width, bitmap.height, landscape)
                                page.canvas.drawBitmap(bitmap, null, RectF(fit.left, fit.top, fit.right, fit.bottom),
                                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                            } finally {
                                document.finishPage(page)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    currentCoroutineContext().ensureActive()
                    context.contentResolver.openOutputStream(destination, "wt")?.use { document.writeTo(it) }
                        ?: error("Couldn't open the PDF destination.")
                    currentCoroutineContext().ensureActive()
                } finally {
                    document.close()
                }
            }
            completed = true
        } finally {
            if (!completed) withContext(NonCancellable + Dispatchers.IO) { SafWriter.delete(context, destination) }
        }
    }
}
