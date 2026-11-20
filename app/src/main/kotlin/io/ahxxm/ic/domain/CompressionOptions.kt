package io.ahxxm.ic.domain

enum class Encoder {
    MOZJPEG,
    JPEGLI
}

data class CompressionOptions(
    val quality: Int = 85,
    val preserveExif: Boolean = true,
    val encoder: Encoder = Encoder.MOZJPEG,
    val halveOversized: Boolean = true,
    // Save the batch as one PDF instead of overwriting the originals
    val asPdf: Boolean = false
) {
    companion object {
        // Sources above this pixel count are halved when `halveOversized` is on
        const val OVERSIZE_PIXELS = 20_000_000
    }
}
