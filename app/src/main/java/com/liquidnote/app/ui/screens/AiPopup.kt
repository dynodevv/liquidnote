package com.liquidnote.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.liquidnote.app.R
import com.liquidnote.app.ui.theme.AppleRed
import com.liquidnote.app.ui.viewmodel.NoteEditorViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

@Composable
fun AiPopup(
    viewModel: NoteEditorViewModel,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val messages by viewModel.aiMessages.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()
    val aiEdited by viewModel.aiEdited.collectAsState()

    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val backdrop = rememberLayerBackdrop()
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val glassSurface = if (isDark) Color(0xFFFAFAFA).copy(0.35f) else Color(0xFF121212).copy(0.35f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Background content for blur
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .safeContentPadding()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Invisible placeholder so backdrop captures dialog background
                Spacer(modifier = Modifier.height(1.dp))
            }

            // Foreground glass panel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                            lens(16f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(glassSurface) }
                    )
                    .padding(20.dp)
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ai_assistant),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                if (aiEdited) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .drawBackdrop(
                                backdrop = rememberLayerBackdrop(),
                                shape = { Capsule() },
                                effects = { blur(4f.dp.toPx()); vibrancy() },
                                onDrawSurface = {
                                    val d = !androidx.compose.foundation.isSystemInDarkTheme()
                                    drawRect(if (d) Color(0xFFFAFAFA).copy(0.2f) else Color(0xFF121212).copy(0.2f))
                                }
                            )
                            .clickable { viewModel.revertAiEdits() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.revert),
                            style = MaterialTheme.typography.labelLarge.copy(color = AppleRed)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    messages.forEach { (text, isUser) ->
                        ChatBubble(text = text, isUser = isUser)
                    }
                    if (isLoading) {
                        Text(
                            text = stringResource(R.string.ai_editing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    if (error != null) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleRed,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .drawBackdrop(
                                backdrop = rememberLayerBackdrop(),
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(6f.dp.toPx())
                                    lens(6f.dp.toPx(), 6f.dp.toPx())
                                },
                                onDrawSurface = {
                                    val d = !androidx.compose.foundation.isSystemInDarkTheme()
                                    drawRect(if (d) Color(0xFFFAFAFA).copy(0.2f) else Color(0xFF121212).copy(0.2f))
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (input.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.type_message),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
                        )
                    }
                    // Glass send button
                    Box(
                        modifier = Modifier
                            .drawBackdrop(
                                backdrop = rememberLayerBackdrop(),
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(6f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(if (isDark) Color(0xFF0A84FF).copy(0.8f) else Color(0xFF007AFF).copy(0.8f))
                                }
                            )
                            .clickable {
                                if (input.isBlank()) return@clickable
                                error = null
                                val currentInput = input
                                input = ""
                                viewModel.sendAiMessage(
                                    currentInput,
                                    onResult = { response ->
                                        if (response.contains("# ") || response.contains("**")) {
                                            onApply(response)
                                        }
                                    },
                                    onError = { err -> error = err }
                                )
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = stringResource(R.string.send),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String, isUser: Boolean) {
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val bubbleColor = if (isUser) {
        if (isDark) Color(0xFF0A84FF).copy(0.6f) else Color(0xFF007AFF).copy(0.6f)
    } else {
        if (isDark) Color(0xFFFAFAFA).copy(0.25f) else Color(0xFF121212).copy(0.25f)
    }
    val backdrop = rememberLayerBackdrop()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(bubbleColor) }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                )
            )
        }
    }
}
