package dev.wolzard.recon.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A lightweight Compose renderer for standard Markdown:
 * - Headings (#, ##, ###)
 * - Bullet and numbered lists
 * - Blockquotes (>)
 * - Code blocks (```)
 * - Bold (**text**), Italic (*text*), Inline code (`code`), Links [text](url)
 * - Horizontal rules (---)
 */
@Composable
fun MarkdownView(text: String, modifier: Modifier = Modifier) {
    val lines = text.lines()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // end code block
                    val code = codeBlockLines.joinToString("\n")
                    CodeBlock(code)
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                continue
            }

            when {
                trimmed.isEmpty() -> Unit
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                trimmed.startsWith("---") || trimmed.startsWith("***") || trimmed.startsWith("___") -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                trimmed.startsWith("> ") -> {
                    BlockQuote(parseMarkdownInline(trimmed.removePrefix("> ")))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                    ListItem(bullet = "•", text = parseMarkdownInline(trimmed.substring(2)))
                }
                Regex("""^\d+\.\s+.*""").matches(trimmed) -> {
                    val number = trimmed.substringBefore('.')
                    val content = trimmed.substringAfter(". ").trim()
                    ListItem(bullet = "$number.", text = parseMarkdownInline(content))
                }
                else -> {
                    Text(
                        text = parseMarkdownInline(line),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            CodeBlock(codeBlockLines.joinToString("\n"))
        }
    }
}

@Composable
private fun ListItem(bullet: String, text: AnnotatedString) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = bullet,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BlockQuote(text: AnnotatedString) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 24.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private enum class ScriptType {
    BENGALI,
    ARABIC,
    OTHER
}

private fun charScript(c: Char): ScriptType = when (c.code) {
    in 0x0980..0x09FF -> ScriptType.BENGALI
    in 0x0600..0x06FF, in 0x0750..0x077F, in 0x08A0..0x08FF, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF -> ScriptType.ARABIC
    else -> ScriptType.OTHER
}

/**
 * Appends [text] to this [AnnotatedString.Builder], splitting into dynamic font spans
 * so Bengali text uses Bornomala, Arabic text uses Scheherazade, and other characters
 * inherit the default typeface. Punctuation/whitespace merges with preceding/following script.
 */
fun AnnotatedString.Builder.appendWithDynamicScriptFonts(text: String, baseStyle: SpanStyle? = null) {
    if (text.isEmpty()) return

    var i = 0
    while (i < text.length) {
        val c = text[i]
        val script = charScript(c)

        if (script == ScriptType.OTHER) {
            val start = i
            while (i < text.length && charScript(text[i]) == ScriptType.OTHER) {
                i++
            }
            val chunk = text.substring(start, i)
            if (baseStyle != null) {
                withStyle(baseStyle) { append(chunk) }
            } else {
                append(chunk)
            }
        } else {
            val start = i
            val currentScript = script
            while (i < text.length) {
                val sc = charScript(text[i])
                if (sc == currentScript || (sc == ScriptType.OTHER && (text[i].isWhitespace() || text[i] in "।.,;:!?-\"'()[]{}«»/\\"))) {
                    if (sc == ScriptType.OTHER && !text[i].isWhitespace()) {
                        var peek = i + 1
                        while (peek < text.length && (text[peek].isWhitespace() || text[peek] in "।.,;:!?-\"'()[]{}«»/\\")) peek++
                        if (peek < text.length && charScript(text[peek]) == currentScript) {
                            i++
                            continue
                        } else {
                            break
                        }
                    }
                    i++
                } else {
                    break
                }
            }
            val chunk = text.substring(start, i)
            val font = when (currentScript) {
                ScriptType.BENGALI -> Bornomala
                ScriptType.ARABIC -> Scheherazade
                ScriptType.OTHER -> null
            }
            val combinedStyle = when {
                baseStyle != null && font != null -> baseStyle.copy(fontFamily = font)
                font != null -> SpanStyle(fontFamily = font)
                else -> baseStyle
            }
            if (combinedStyle != null) {
                withStyle(combinedStyle) { append(chunk) }
            } else {
                append(chunk)
            }
        }
    }
}

/** Parses bold, italic, inline code, and links into an AnnotatedString with dynamic font support. */
fun parseMarkdownInline(input: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < input.length) {
            when {
                // Bold: **text** or __text__
                input.startsWith("**", i) -> {
                    val end = input.indexOf("**", i + 2)
                    if (end != -1) {
                        val content = input.substring(i + 2, end)
                        appendWithDynamicScriptFonts(content, SpanStyle(fontWeight = FontWeight.Bold))
                        i = end + 2
                    } else {
                        appendWithDynamicScriptFonts(input[i].toString())
                        i++
                    }
                }
                // Inline code: `code`
                input.startsWith("`", i) -> {
                    val end = input.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f))) {
                            append(" ${input.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        appendWithDynamicScriptFonts(input[i].toString())
                        i++
                    }
                }
                // Italic: *text* or _text_
                (input[i] == '*' || input[i] == '_') && (i + 1 < input.length && input[i + 1] != ' ') -> {
                    val mark = input[i]
                    val end = input.indexOf(mark, i + 1)
                    if (end != -1 && end > i + 1) {
                        val content = input.substring(i + 1, end)
                        appendWithDynamicScriptFonts(content, SpanStyle(fontStyle = FontStyle.Italic))
                        i = end + 1
                    } else {
                        appendWithDynamicScriptFonts(input[i].toString())
                        i++
                    }
                }
                // Link: [text](url)
                input[i] == '[' -> {
                    val textEnd = input.indexOf(']', i + 1)
                    if (textEnd != -1 && textEnd + 1 < input.length && input[textEnd + 1] == '(') {
                        val urlEnd = input.indexOf(')', textEnd + 2)
                        if (urlEnd != -1) {
                            val linkText = input.substring(i + 1, textEnd)
                            appendWithDynamicScriptFonts(
                                linkText,
                                SpanStyle(textDecoration = TextDecoration.Underline, color = androidx.compose.ui.graphics.Color(0xFF2684FF)),
                            )
                            i = urlEnd + 1
                        } else {
                            appendWithDynamicScriptFonts(input[i].toString())
                            i++
                        }
                    } else {
                        appendWithDynamicScriptFonts(input[i].toString())
                        i++
                    }
                }
                else -> {
                    val start = i
                    while (i < input.length && !input.startsWith("**", i) && !input.startsWith("`", i) && input[i] != '*' && input[i] != '_' && input[i] != '[') {
                        i++
                    }
                    val plain = input.substring(start, i)
                    appendWithDynamicScriptFonts(plain)
                }
            }
        }
    }
}
