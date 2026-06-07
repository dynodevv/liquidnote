package com.liquidnote.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.liquidnote.app.model.Block
import com.liquidnote.app.model.BlockType
import com.liquidnote.app.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarkdownExport {

    fun blocksToMarkdown(note: Note, blocks: List<Block>): String {
        val sb = StringBuilder()
        if (note.title.isNotBlank()) {
            sb.append("# ").append(note.title).append("\n\n")
        }

        blocks.sortedBy { it.order }.forEach { block ->
            val text = block.content
            when (block.type) {
                BlockType.HEADING1 -> sb.append("# ").append(inlineToMarkdown(text)).append("\n\n")
                BlockType.HEADING2 -> sb.append("## ").append(inlineToMarkdown(text)).append("\n\n")
                BlockType.HEADING3 -> sb.append("### ").append(inlineToMarkdown(text)).append("\n\n")
                BlockType.BULLET -> sb.append("- ").append(inlineToMarkdown(text)).append("\n")
                BlockType.NUMBERED -> sb.append("1. ").append(inlineToMarkdown(text)).append("\n")
                BlockType.QUOTE -> sb.append("> ").append(inlineToMarkdown(text)).append("\n")
                BlockType.CHECKBOX -> {
                    val check = if (block.isChecked) "[x]" else "[ ]"
                    sb.append("- ").append(check).append(" ").append(inlineToMarkdown(text)).append("\n")
                }
                BlockType.CODE -> sb.append("```\n").append(text).append("\n```\n\n")
                BlockType.PARAGRAPH -> {
                    if (text.isNotBlank()) {
                        sb.append(inlineToMarkdown(text)).append("\n\n")
                    } else {
                        sb.append("\n")
                    }
                }
            }
        }
        return sb.toString().trimEnd()
    }

    private fun inlineToMarkdown(text: String): String {
        return text
            .replace("[b]", "**").replace("[/b]", "**")
            .replace("[i]", "*").replace("[/i]", "*")
            .replace("[c]", "`").replace("[/c]", "`")
            .replace(Regex("\\[l\\](.*?)\\|(.*?)\\[/l\\]")) { match ->
                val label = match.groupValues[1]
                val url = match.groupValues[2]
                "[$label]($url)"
            }
    }

    suspend fun exportToFile(context: Context, note: Note, blocks: List<Block>): Uri = withContext(Dispatchers.IO) {
        val safeTitle = note.title.ifBlank { "Untitled" }.replace(Regex("[^a-zA-Z0-9\\s-]"), "_")
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${safeTitle}_$dateStr.md"
        val dir = File(context.getExternalFilesDir(null), "exports")
        dir.mkdirs()
        val file = File(dir, fileName)
        file.writeText(blocksToMarkdown(note, blocks))
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}

object MarkdownImport {

    fun parseMarkdown(input: String): Pair<String, List<Block>> {
        val lines = input.lines()
        val blocks = mutableListOf<Block>()
        var title = ""
        var codeBuffer = StringBuilder()
        var inCode = false
        var numberedIndex = 1

        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()

            if (line.startsWith("```")) {
                if (inCode) {
                    blocks.add(Block(
                        noteId = 0,
                        type = BlockType.CODE,
                        content = codeBuffer.toString().trimEnd('\n')
                    ))
                    codeBuffer = StringBuilder()
                    inCode = false
                } else {
                    inCode = true
                }
                return@forEach
            }

            if (inCode) {
                codeBuffer.append(line).append("\n")
                return@forEach
            }

            when {
                line.startsWith("# ") && blocks.isEmpty() && title.isEmpty() -> {
                    title = line.removePrefix("# ").trim()
                }
                line.startsWith("# ") -> {
                    blocks.add(Block(noteId = 0, type = BlockType.HEADING1, content = markdownToInline(line.removePrefix("# ").trim())))
                }
                line.startsWith("## ") -> {
                    blocks.add(Block(noteId = 0, type = BlockType.HEADING2, content = markdownToInline(line.removePrefix("## ").trim())))
                }
                line.startsWith("### ") -> {
                    blocks.add(Block(noteId = 0, type = BlockType.HEADING3, content = markdownToInline(line.removePrefix("### ").trim())))
                }
                line.startsWith("> ") -> {
                    blocks.add(Block(noteId = 0, type = BlockType.QUOTE, content = markdownToInline(line.removePrefix("> ").trim())))
                }
                line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                    val checked = line.startsWith("- [x] ", ignoreCase = true)
                    val text = line.substringAfter("] ").trim()
                    blocks.add(Block(noteId = 0, type = BlockType.CHECKBOX, content = markdownToInline(text), isChecked = checked))
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    blocks.add(Block(noteId = 0, type = BlockType.BULLET, content = markdownToInline(line.removePrefix("- ").removePrefix("* ").trim())))
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    blocks.add(Block(noteId = 0, type = BlockType.NUMBERED, content = markdownToInline(line.replaceFirst(Regex("^\\d+\\.\\s"), ""))))
                }
                line.isBlank() -> {
                    // Skip or add empty paragraph if needed
                }
                else -> {
                    blocks.add(Block(noteId = 0, type = BlockType.PARAGRAPH, content = markdownToInline(line.trim())))
                }
            }
        }

        if (title.isEmpty() && blocks.isNotEmpty()) {
            val first = blocks.first()
            if (first.type == BlockType.HEADING1) {
                title = first.content.let { markdownToInlineReverse(it) }
                blocks.removeAt(0)
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(Block(noteId = 0, type = BlockType.PARAGRAPH, content = ""))
        }

        return title to blocks.mapIndexed { index, block -> block.copy(order = index) }
    }

    fun parseMarkdownFromStream(inputStream: InputStream): Pair<String, List<Block>> {
        return parseMarkdown(inputStream.bufferedReader().use { it.readText() })
    }

    private fun markdownToInline(text: String): String {
        var result = text
        // Links first
        result = result.replace(Regex("\\[(.*?)]\\((.*?)\\)")) { match ->
            "[l]${match.groupValues[1]}|${match.groupValues[2]}[/l]"
        }
        result = result.replace("**", "[b]", false).replace("**", "[/b]", false)
        result = result.replace("__", "[b]", false).replace("__", "[/b]", false)
        result = result.replace("*", "[i]", false).replace("*", "[/i]", false)
        result = result.replace("_", "[i]", false).replace("_", "[/i]", false)
        result = result.replace("`", "[c]", false).replace("`", "[/c]", false)
        return result
    }

    private fun markdownToInlineReverse(text: String): String {
        return text
            .replace("[b]", "").replace("[/b]", "")
            .replace("[i]", "").replace("[/i]", "")
            .replace("[c]", "").replace("[/c]", "")
            .replace(Regex("\\[l\\](.*?)\\|(.*?)\\[/l\\]")) { it.groupValues[1] }
    }
}
