package io.ahxxm.ic.domain

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Writes a PDF with one page per JPEG. The JPEG bytes are embedded unchanged
 * (DCTDecode), so the pages carry exactly the compressed data, with no re-encoding.
 */
object PdfWriter {

    data class Page(val file: File, val width: Int, val height: Int, val components: Int)

    fun write(pages: List<Page>, out: OutputStream) {
        require(pages.isNotEmpty()) { "No pages" }
        val counter = CountingOutput(out)
        val n = pages.size
        val objectCount = 2 + 3 * n
        val offsets = LongArray(objectCount + 1)

        counter.text("%PDF-1.4\n%âãÏÓ\n")

        fun obj(num: Int, body: () -> Unit) {
            offsets[num] = counter.position
            counter.text("$num 0 obj\n")
            body()
            counter.text("\nendobj\n")
        }

        obj(1) { counter.text("<< /Type /Catalog /Pages 2 0 R >>") }
        obj(2) {
            val kids = (0 until n).joinToString(" ") { "${3 + 3 * it} 0 R" }
            counter.text("<< /Type /Pages /Kids [$kids] /Count $n >>")
        }

        pages.forEachIndexed { i, page ->
            val pageNum = 3 + 3 * i
            val contentNum = pageNum + 1
            val imageNum = pageNum + 2
            val colorSpace = if (page.components == 1) "DeviceGray" else "DeviceRGB"
            val content = "q ${page.width} 0 0 ${page.height} 0 0 cm /Im$i Do Q"

            obj(pageNum) {
                counter.text(
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${page.width} ${page.height}] " +
                        "/Contents $contentNum 0 R /Resources << /XObject << /Im$i $imageNum 0 R >> >> >>"
                )
            }
            obj(contentNum) {
                counter.text("<< /Length ${content.length} >>\nstream\n$content\nendstream")
            }
            obj(imageNum) {
                counter.text(
                    "<< /Type /XObject /Subtype /Image /Width ${page.width} /Height ${page.height} " +
                        "/ColorSpace /$colorSpace /BitsPerComponent 8 /Filter /DCTDecode " +
                        "/Length ${page.file.length()} >>\nstream\n"
                )
                page.file.inputStream().use { counter.copy(it) }
                counter.text("\nendstream")
            }
        }

        val xrefPosition = counter.position
        counter.text("xref\n0 ${objectCount + 1}\n0000000000 65535 f \n")
        for (num in 1..objectCount) {
            counter.text(String.format("%010d 00000 n \n", offsets[num]))
        }
        counter.text("trailer\n<< /Size ${objectCount + 1} /Root 1 0 R >>\nstartxref\n$xrefPosition\n%%EOF\n")
        out.flush()
    }

    /** Reads (width, height, components) from the JPEG's start-of-frame marker, or null if not a JPEG. */
    fun readJpegInfo(file: File): Page? {
        DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
            if (input.readUnsignedShort() != 0xFFD8) return null
            while (true) {
                var b = input.read()
                while (b != 0xFF) {
                    if (b == -1) return null
                    b = input.read()
                }
                var marker = input.read()
                while (marker == 0xFF) marker = input.read()
                if (marker == -1 || marker == 0xD9 || marker == 0xDA) return null

                val length = input.readUnsignedShort()
                val isFrame = marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
                if (isFrame) {
                    input.readUnsignedByte() // sample precision
                    val height = input.readUnsignedShort()
                    val width = input.readUnsignedShort()
                    val components = input.readUnsignedByte()
                    return Page(file, width, height, components)
                }
                skipFully(input, length - 2)
            }
        }
    }

    private fun skipFully(input: InputStream, count: Int) {
        var remaining = count.toLong()
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                if (input.read() == -1) return
                remaining--
            }
        }
    }

    private class CountingOutput(private val out: OutputStream) {
        var position: Long = 0
            private set

        fun text(s: String) {
            val bytes = s.toByteArray(Charsets.ISO_8859_1)
            out.write(bytes)
            position += bytes.size
        }

        fun copy(input: InputStream) {
            position += input.copyTo(out)
        }
    }
}
