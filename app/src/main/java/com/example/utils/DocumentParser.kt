package com.example.utils

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset

/**
 * 简单的文档解析器。
 * - 仅支持 TXT 纯文本格式（UTF-8 / GBK 编码自动检测）
 * - WPS / DOC / DOCX / PDF 等二进制格式无法直接解析，请先另存为 .txt 纯文本
 */
object DocumentParser {

    /**
     * 解析文档。返回全文和按段落分页的列表。
     * @param mimeType 从 ContentResolver 获取的 MIME 类型
     */
    fun parse(
        context: Context,
        uri: Uri,
        mimeType: String? = null
    ): DocumentResult {
        // 读取字节（不再严格检查 MIME，部分系统将 .txt 报为 octet-stream）
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()

            if (bytes.isEmpty()) {
                return DocumentResult("", listOf("文件内容为空。"))
            }

            // 编码检测：UTF-8 BOM → UTF-8，否则试 UTF-8 → 乱码则 GBK
            val text = if (bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte()
            ) {
                // UTF-8 BOM，跳过前 3 字节
                bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
            } else {
                val utf8 = bytes.toString(Charsets.UTF_8)
                // 如果 UTF-8 解码后超过 5% 是替换字符 U+FFFD，用 GBK 重试
                if (utf8.count { it == '\uFFFD' } > utf8.length * 0.05 && utf8.length > 0) {
                    bytes.toString(Charset.forName("GBK"))
                } else {
                    utf8
                }
            }

            if (text.isBlank()) {
                DocumentResult("", listOf("文件内容为空。"))
            } else {
                // 按行分页：每页约 15 行（适合手机屏幕），过滤纯空行
                val lines = text.lines().filter { it.isNotBlank() }
                val maxLinesPerPage = 15
                val pages = mutableListOf<String>()

                var lineIndex = 0
                while (lineIndex < lines.size) {
                    val endIndex = minOf(lineIndex + maxLinesPerPage, lines.size)
                    val pageLines = lines.subList(lineIndex, endIndex).joinToString("\n")
                    pages.add(pageLines)
                    lineIndex = endIndex
                }

                DocumentResult(text, pages.ifEmpty { listOf(text.take(300)) })
            }
        } catch (e: Exception) {
            DocumentResult(
                "",
                listOf("文件解析失败: ${e.localizedMessage}。请上传 TXT 格式的文件。")
            )
        }
    }

    /**
     * 简单检测文本是否看起来有效（非纯乱码）
     */
    private fun isValidText(text: String): Boolean {
        val sample = text.take(500)
        val validChars = sample.count {
            it.code in 0x4E00..0x9FFF || // 中文
                it.code in 0x3000..0x303F || // CJK 标点
                it == '\r' || it == '\n' || it == '\t' ||
                it.code in 32..126 // 英文基本
        }
        return validChars.toFloat() / sample.length > 0.7f
    }

    data class DocumentResult(
        val fullText: String,
        val pages: List<String>
    )
}