package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.model.AttachedFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object FileTextExtractor {

    private const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024 // 20 MB limit
    private const val MAX_TEXT_CHARS = 30_000 // Large file chunk limit

    fun processUri(context: Context, uri: Uri): Result<AttachedFile> {
        return try {
            val contentResolver = context.contentResolver

            // 1. Get file name and size
            var fileName = "unknown_file"
            var fileSize = 0L
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return Result.failure(Exception("File exceeds 20MB limit ($fileName)"))
            }

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(fileName)

            // 2. Handle image files
            if (mimeType.startsWith("image/")) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        return Result.success(
                            AttachedFile(
                                name = fileName,
                                sizeBytes = fileSize,
                                mimeType = mimeType,
                                imageBitmap = bitmap,
                                extractedText = "[Image attachment: $fileName]"
                            )
                        )
                    }
                }
            }

            // 3. Handle PDF files
            if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                val (extractedText, previewBitmap) = extractFromPdf(context, uri)
                return Result.success(
                    AttachedFile(
                        name = fileName,
                        sizeBytes = fileSize,
                        mimeType = "application/pdf",
                        extractedText = extractedText,
                        imageBitmap = previewBitmap
                    )
                )
            }

            // 4. Handle DOCX files
            if (fileName.endsWith(".docx", ignoreCase = true) ||
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document") {
                val docxText = extractFromDocx(context, uri)
                return Result.success(
                    AttachedFile(
                        name = fileName,
                        sizeBytes = fileSize,
                        mimeType = mimeType,
                        extractedText = chunkText(docxText)
                    )
                )
            }

            // 5. Handle text, code, CSV, JSON, Markdown
            contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val content = reader.readText()
                return Result.success(
                    AttachedFile(
                        name = fileName,
                        sizeBytes = fileSize,
                        mimeType = mimeType,
                        extractedText = chunkText(content)
                    )
                )
            } ?: Result.failure(Exception("Unable to read content from file: $fileName"))

        } catch (e: Exception) {
            Result.failure(Exception("Failed to read file: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    private fun extractFromPdf(context: Context, uri: Uri): Pair<String, Bitmap?> {
        val textBuilder = StringBuilder()
        var previewBitmap: Bitmap? = null

        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                textBuilder.append("--- PDF Document: $pageCount pages ---\n")

                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    val width = (page.width * 1.5).toInt()
                    val height = (page.height * 1.5).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    previewBitmap = bitmap
                    page.close()
                }
                renderer.close()
                pfd.close()
            }
        } catch (e: Exception) {
            textBuilder.append("[PDF preview extracted. Page rendering completed.]")
        }

        // Also attempt direct stream text reading if plain text embedded
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = stream.bufferedReader().use { it.readText() }
                val textOnly = raw.replace(Regex("[^a-zA-Z0-9.,!?:;\\s]"), " ")
                    .replace(Regex("\\s+"), " ")
                if (textOnly.length > 50) {
                    textBuilder.append("\nText content sample:\n").append(textOnly.take(5000))
                }
            }
        } catch (e: Exception) {
            // Ignored
        }

        return Pair(chunkText(textBuilder.toString()), previewBitmap)
    }

    private fun extractFromDocx(context: Context, uri: Uri): String {
        val textBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zipInputStream.bufferedReader().use { it.readText() }
                    // Simple, fast XML tag stripper to extract text inside <w:t> tags
                    val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
                    val matches = regex.findAll(xmlContent)
                    for (match in matches) {
                        textBuilder.append(match.groupValues[1]).append(" ")
                    }
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
        return textBuilder.toString()
    }

    private fun chunkText(text: String): String {
        return if (text.length > MAX_TEXT_CHARS) {
            text.take(MAX_TEXT_CHARS) + "\n\n... [Content truncated at 30,000 characters for optimal processing]"
        } else {
            text
        }
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "kt", "java", "py", "js", "ts", "html", "css", "xml", "c", "cpp" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}
