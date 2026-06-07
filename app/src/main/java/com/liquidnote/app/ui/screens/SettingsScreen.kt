package com.liquidnote.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidnote.app.LiquidNoteApplication
import com.liquidnote.app.R
import com.liquidnote.app.ui.viewmodel.AppViewModelFactory
import com.liquidnote.app.ui.viewmodel.SettingsViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = LiquidNoteApplication.instance(context.applicationContext as android.app.Application)
    val factory = remember { AppViewModelFactory(app.repository, app.settingsManager) }
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val scope = rememberCoroutineScope()

    val aiEnabled by viewModel.aiEnabled.collectAsState()
    val aiEndpoint by viewModel.aiEndpoint.collectAsState()
    val aiKey by viewModel.aiKey.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()

    var endpoint by remember { mutableStateOf(aiEndpoint) }
    var key by remember { mutableStateOf(aiKey) }
    var model by remember { mutableStateOf(aiModel) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val (title, blocks) = com.liquidnote.app.util.MarkdownImport.parseMarkdownFromStream(stream)
                        val note = com.liquidnote.app.model.Note(title = title)
                        app.repository.saveNoteWithBlocks(note, blocks)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val backdrop = rememberLayerBackdrop()
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val glassSurface = if (isDark) Color(0xFFFAFAFA).copy(0.3f) else Color(0xFF121212).copy(0.3f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background scrolling content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .safeContentPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(60.dp)) // space for top glass bar

            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // AI Settings glass card
            GlassCard(backdrop = backdrop, surface = glassSurface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ai_enabled),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = aiEnabled,
                            onCheckedChange = { viewModel.setAiEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsTextField(
                        label = stringResource(R.string.ai_endpoint),
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        placeholder = "https://api.openai.com/v1"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsTextField(
                        label = stringResource(R.string.ai_key),
                        value = key,
                        onValueChange = { key = it },
                        placeholder = "sk-...",
                        isPassword = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsTextField(
                        label = stringResource(R.string.ai_model),
                        value = model,
                        onValueChange = { model = it },
                        placeholder = "gpt-4o"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBackdrop(
                                backdrop = rememberLayerBackdrop(),
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(6f.dp.toPx())
                                    lens(6f.dp.toPx(), 6f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(if (isDark) Color(0xFFFAFAFA).copy(0.25f) else Color(0xFF121212).copy(0.25f))
                                }
                            )
                            .clickable {
                                viewModel.setAiEndpoint(endpoint)
                                viewModel.setAiKey(key)
                                viewModel.setAiModel(model)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Data glass card
            GlassCard(backdrop = backdrop, surface = glassSurface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBackdrop(
                                backdrop = rememberLayerBackdrop(),
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(6f.dp.toPx())
                                    lens(6f.dp.toPx(), 6f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(if (isDark) Color(0xFFFAFAFA).copy(0.25f) else Color(0xFF121212).copy(0.25f))
                                }
                            )
                            .clickable { importLauncher.launch("text/markdown") }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.import_notes),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBackdrop(
                                backdrop = rememberLayerBackdrop(),
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(6f.dp.toPx())
                                    lens(6f.dp.toPx(), 6f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(if (isDark) Color(0xFFFAFAFA).copy(0.25f) else Color(0xFF121212).copy(0.25f))
                                }
                            )
                            .clickable {
                                scope.launch {
                                    try {
                                        val uris = viewModel.exportAllNotes(context)
                                        if (uris.isNotEmpty()) {
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                                type = "text/markdown"
                                                putParcelableArrayListExtra(
                                                    android.content.Intent.EXTRA_STREAM,
                                                    ArrayList(uris)
                                                )
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                android.content.Intent.createChooser(shareIntent, "Export notes")
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.export),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Glass top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(10f.dp.toPx())
                        lens(10f.dp.toPx(), 10f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(glassSurface) }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun GlassCard(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    surface: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(10f.dp.toPx(), 10f.dp.toPx())
                },
                onDrawSurface = { drawRect(surface) }
            )
    ) {
        content()
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isPassword: Boolean = false
) {
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = rememberLayerBackdrop(),
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(4f.dp.toPx(), 4f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(if (isDark) Color(0xFFFAFAFA).copy(0.2f) else Color(0xFF121212).copy(0.2f))
                    }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                },
                visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}
