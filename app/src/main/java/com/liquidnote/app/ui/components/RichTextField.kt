package com.liquidnote.app.ui.components

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.liquidnote.app.ui.theme.AppleBlue

@Composable
fun RichTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    singleLine: Boolean = false
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                annotatedString = buildStyledAnnotatedString(value.text),
                selection = value.selection
            )
        )
    }

    LaunchedEffect(value.text, value.selection) {
        if (value.text != textFieldValue.text || value.selection != textFieldValue.selection) {
            textFieldValue = TextFieldValue(
                annotatedString = buildStyledAnnotatedString(value.text),
                selection = value.selection
            )
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val rebuilt = TextFieldValue(
                annotatedString = buildStyledAnnotatedString(newValue.text),
                selection = newValue.selection
            )
            textFieldValue = rebuilt
            if (newValue.text != value.text || newValue.selection != value.selection) {
                onValueChange(TextFieldValue(newValue.text, newValue.selection))
            }
        },
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        singleLine = singleLine
    )
}

fun buildStyledAnnotatedString(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val tagPattern = Regex("""\[(b|i|c|/b|/i|/c)\]|\[l\]([^\]]*?)\|([^\]]*?)\[/l\]""")
    var lastEnd = 0
    val matches = tagPattern.findAll(text).toList()

    for (match in matches) {
        if (match.range.first > lastEnd) {
            builder.append(text.substring(lastEnd, match.range.first))
        }

        val tag = match.groupValues[1]
        val tagStyle = SpanStyle(
            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
            fontSize = 0.75.em
        )

        if (tag.isNotEmpty()) {
            builder.withStyle(tagStyle) { append(match.value) }
        } else {
            val label = match.groupValues[2]
            val url = match.groupValues[3]
            builder.withStyle(tagStyle) { append("[l]") }
            builder.withStyle(
                SpanStyle(
                    color = AppleBlue,
                    textDecoration = TextDecoration.Underline
                )
            ) { append(label) }
            builder.withStyle(tagStyle) { append("|") }
            builder.withStyle(tagStyle) { append(url) }
            builder.withStyle(tagStyle) { append("[/l]") }
        }

        lastEnd = match.range.last + 1
    }

    if (lastEnd < text.length) {
        builder.append(text.substring(lastEnd))
    }

    val plain = builder.toAnnotatedString()
    val finalBuilder = AnnotatedString.Builder(plain)

    applyStyleBetweenTags(finalBuilder, plain.text, "[b]", "[/b]", SpanStyle(fontWeight = FontWeight.Bold))
    applyStyleBetweenTags(finalBuilder, plain.text, "[i]", "[/i]", SpanStyle(fontStyle = FontStyle.Italic))
    applyStyleBetweenTags(
        finalBuilder,
        plain.text,
        "[c]",
        "[/c]",
        SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f)
        )
    )

    return finalBuilder.toAnnotatedString()
}

private fun applyStyleBetweenTags(
    builder: AnnotatedString.Builder,
    text: String,
    startTag: String,
    endTag: String,
    style: SpanStyle
) {
    var searchStart = 0
    while (true) {
        val start = text.indexOf(startTag, searchStart)
        if (start == -1) break
        val contentStart = start + startTag.length
        val end = text.indexOf(endTag, contentStart)
        if (end == -1) break
        builder.addStyle(style, contentStart, end)
        searchStart = end + endTag.length
    }
}

fun wrapSelection(text: String, selection: TextRange, openTag: String, closeTag: String): String {
    if (selection.collapsed) return text
    val start = selection.start.coerceIn(0, text.length)
    val end = selection.end.coerceIn(0, text.length)
    return text.substring(0, start) + openTag + text.substring(start, end) + closeTag + text.substring(end)
}
