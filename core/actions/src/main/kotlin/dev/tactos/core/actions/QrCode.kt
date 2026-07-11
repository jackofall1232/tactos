package dev.tactos.core.actions

import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * A square grid of QR modules produced by [QrCode.encode]; `true` cells are
 * dark. Coordinates are `(x, y)` with the origin at the top-left, and [size]
 * already includes the quiet-zone margin on every side.
 */
class QrMatrix internal constructor(
    private val modules: Array<BooleanArray>,
) {
    /** Edge length in modules, quiet-zone margin included. */
    val size: Int = modules.size

    /**
     * Returns whether the module at ([x], [y]) is dark.
     *
     * @throws IndexOutOfBoundsException if either coordinate is outside
     *   `0 until size`.
     */
    operator fun get(x: Int, y: Int): Boolean {
        if (x !in 0 until size || y !in 0 until size) {
            throw IndexOutOfBoundsException("($x, $y) is outside the $size x $size matrix")
        }
        return modules[y][x]
    }
}

/**
 * Offline QR generation (zxing, error correction M, UTF-8) for the URL
 * contextual action. Pure module math — rendering the matrix to pixels is the
 * caller's concern. Never throws.
 */
object QrCode {
    /** Longest input [encode] accepts; fits QR version 40 at EC level M. */
    const val MAX_INPUT_LENGTH: Int = 2000

    /**
     * Encodes [text] as a QR symbol and frames it with [margin] light
     * quiet-zone modules on every side.
     *
     * Returns `null` — never throws — when [text] is blank or longer than
     * [MAX_INPUT_LENGTH], [margin] is negative, or the content cannot be
     * encoded.
     */
    fun encode(text: String, margin: Int = 2): QrMatrix? {
        if (text.isBlank() || text.length > MAX_INPUT_LENGTH || margin < 0) return null
        val matrix = try {
            Encoder.encode(
                text,
                ErrorCorrectionLevel.M,
                mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
            ).matrix
        } catch (e: WriterException) {
            return null
        } ?: return null

        val size = matrix.width + 2 * margin
        val modules = Array(size) { y ->
            BooleanArray(size) { x ->
                val mx = x - margin
                val my = y - margin
                mx in 0 until matrix.width &&
                    my in 0 until matrix.height &&
                    matrix.get(mx, my).toInt() == 1
            }
        }
        return QrMatrix(modules)
    }
}
