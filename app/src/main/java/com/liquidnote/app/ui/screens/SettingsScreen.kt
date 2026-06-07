package com.liquidnote.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.liquidnote.app.ui.components.LiquidButton
import com.liquidnote.app.ui.components.LiquidSurface
import com.liquidnote.app.ui.theme.AppleBlue
import com.liquidnote.app.ui.viewmodel.AppViewModelFactory
import com.liquidnote.app.ui.viewmodel.SettingsViewModel
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
                        val noteId = app.repository.saveNoteWithBlocks(note, blocks)
                        // Note saved, user can navigate to it
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        LiquidSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            blurRadius = 20.dp,
            containerAlpha = 0.3f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Section
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LiquidSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                blurRadius = 14.dp,
                containerAlpha = 0.25f
            ) {
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

                    LiquidButton(
                        onClick = {
                            viewModel.setAiEndpoint(endpoint)
                            viewModel.setAiKey(key)
                            viewModel.setAiModel(model)
                        },
                        shape = RoundedCornerShape(12.dp),
                        blurRadius = 6.dp
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

            // Import/Export
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LiquidSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                blurRadius = 14.dp,
                containerAlpha = 0.25f
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LiquidButton(
                        onClick = { importLauncher.launch("text/markdown") },
                        shape = RoundedCornerShape(12.dp),
                        blurRadius = 6.dp
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

                    LiquidButton(
                        onClick = {
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
                        },
                        shape = RoundedCornerShape(12.dp),
                        blurRadius = 6.dp
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
        }

        Spacer(modifier = Modifier.height(32.dp))
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
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LiquidSurface(
            shape = RoundedCornerShape(12.dp),
            blurRadius = 6.dp,
            containerAlpha = 0.2f
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
