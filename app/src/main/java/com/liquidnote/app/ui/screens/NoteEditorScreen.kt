package com.liquidnote.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
    import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidnote.app.LiquidNoteApplication
import com.liquidnote.app.R
import com.liquidnote.app.model.BlockType
import com.liquidnote.app.ui.components.LiquidButton
import com.liquidnote.app.ui.viewmodel.AppViewModelFactory
import com.liquidnote.app.ui.viewmodel.NoteEditorViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = LiquidNoteApplication.instance(context.applicationContext as android.app.Application)
    val factory = remember { AppViewModelFactory(app.repository, app.settingsManager) }
    val viewModel: NoteEditorViewModel = viewModel(factory = factory)

    val note by viewModel.note.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()

    var title by remember { mutableStateOf("") }
    val editorBlocks = remember { mutableStateListOf<EditorBlock>() }
    var focusedIndex by remember { mutableIntStateOf(-1) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAiPopup by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val backdrop = rememberLayerBackdrop()
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val glassSurface = if (isDark) Color(0xFFFAFAFA).copy(0.3f) else Color(0xFF121212).copy(0.3f)

    // Sync note & blocks
    LaunchedEffect(note) {
        if (note.id != 0L || title.isEmpty()) {
            title = note.title
        }
    }

    LaunchedEffect(blocks) {
        if (editorBlocks.isEmpty() && blocks.isNotEmpty()) {
            editorBlocks.clear()
            editorBlocks.addAll(blocks.map { b ->
                EditorBlock(
                    id = b.id,
                    type = b.type,
                    textFieldValue = TextFieldValue(b.content, TextRange(b.content.length)),
                    isChecked = b.isChecked
                )
            })
        } else if (editorBlocks.isEmpty()) {
            // Crash safety: always have at least one paragraph block
            editorBlocks.add(EditorBlock(type = BlockType.PARAGRAPH))
        }
    }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    // Auto-save debounced
    LaunchedEffect(Unit) {
        snapshotFlow {
            title to editorBlocks.map { it.textFieldValue.text }
        }
            .debounce(1000)
            .collect { (t, contents) ->
                viewModel.updateTitle(t)
                val newBlocks = editorBlocks.mapIndexed { index, eb ->
                    com.liquidnote.app.model.Block(
                        id = eb.id,
                        noteId = note.id,
                        type = eb.type,
                        content = contents.getOrElse(index) { "" },
                        order = index,
                        isChecked = eb.isChecked
                    )
                }
                viewModel.updateBlocks(newBlocks)
            }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
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

            // Title
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isBlank()) {
                            Text(
                                text = stringResource(R.string.untitled),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Blocks
            editorBlocks.forEachIndexed { index, editorBlock ->
                BlockEditor(
                    block = editorBlock,
                    onBlockChange = { newValue ->
                        editorBlocks[index] = editorBlock.copy(textFieldValue = newValue)
                    },
                    onFocus = { focusedIndex = index },
                    isFocused = focusedIndex == index,
                    onEnter = {
                        val text = editorBlock.textFieldValue.text
                        val cursor = editorBlock.textFieldValue.selection.start.coerceIn(0, text.length)
                        val before = text.substring(0, cursor)
                        val after = text.substring(cursor)
                        editorBlocks[index] = editorBlock.copy(textFieldValue = TextFieldValue(before, TextRange(before.length)))
                        val newType = if (editorBlock.type in listOf(BlockType.CHECKBOX, BlockType.BULLET, BlockType.NUMBERED)) editorBlock.type else BlockType.PARAGRAPH
                        editorBlocks.add(index + 1, EditorBlock(type = newType, textFieldValue = TextFieldValue(after, TextRange(0))))
                        focusedIndex = index + 1
                    },
                    onBackspaceAtStart = {
                        if (index > 0) {
                            val prev = editorBlocks[index - 1]
                            val merged = prev.textFieldValue.text + editorBlock.textFieldValue.text
                            editorBlocks[index - 1] = prev.copy(textFieldValue = TextFieldValue(merged, TextRange(prev.textFieldValue.text.length)))
                            editorBlocks.removeAt(index)
                            focusedIndex = index - 1
                        } else if (editorBlocks.size > 1) {
                            editorBlocks.removeAt(index)
                            focusedIndex = 0.coerceAtMost(editorBlocks.size - 1)
                        }
                    },
                    onToggleCheckbox = {
                        editorBlocks[index] = editorBlock.copy(isChecked = !editorBlock.isChecked)
                    }
                )
            }

            Spacer(modifier = Modifier.height(120.dp)) // space for bottom glass toolbar
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
                IconButton(onClick = {
                    viewModel.saveNote()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // More menu
        DropdownMenu(
            expanded = showMoreMenu,
            onDismissRequest = { showMoreMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export)) },
                onClick = {
                    showMoreMenu = false
                    val uri = viewModel.exportNote(context)
                    if (uri != null) {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/markdown"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share note"))
                    }
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = Color(0xFFFF3B30)) },
                onClick = {
                    showMoreMenu = false
                    showDeleteConfirm = true
                }
            )
        }

        // Glass bottom toolbar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                        lens(12f.dp.toPx(), 12f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(glassSurface) }
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column {
                // Inline formatting row
                if (focusedIndex >= 0 && editorBlocks.getOrNull(focusedIndex)?.type == BlockType.PARAGRAPH) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ToolbarGlassButton("B") { wrapSelection(editorBlocks, focusedIndex, "**", "**") }
                        ToolbarGlassButton("I") { wrapSelection(editorBlocks, focusedIndex, "_", "_") }
                        ToolbarGlassButton("`") { wrapSelection(editorBlocks, focusedIndex, "`", "`") }
                        ToolbarGlassButton("L") {
                            wrapSelection(editorBlocks, focusedIndex, "[", "](https://)")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Block type row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BlockGlassButton("T", BlockType.PARAGRAPH, focusedIndex, editorBlocks)
                    BlockGlassButton("H1", BlockType.HEADING1, focusedIndex, editorBlocks)
                    BlockGlassButton("H2", BlockType.HEADING2, focusedIndex, editorBlocks)
                    BlockGlassButton("H3", BlockType.HEADING3, focusedIndex, editorBlocks)
                    BlockGlassButton("•", BlockType.BULLET, focusedIndex, editorBlocks)
                    BlockGlassButton("1.", BlockType.NUMBERED, focusedIndex, editorBlocks)
                    BlockGlassButton("✓", BlockType.CHECKBOX, focusedIndex, editorBlocks)
                    BlockGlassButton("\"", BlockType.QUOTE, focusedIndex, editorBlocks)
                    BlockGlassButton("</>", BlockType.CODE, focusedIndex, editorBlocks)
                }
            }
        }

        // AI glass floating button
        if (isAiEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 20.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(8f.dp.toPx(), 8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(if (isDark) Color(0xFF0A84FF).copy(0.8f) else Color(0xFF007AFF).copy(0.8f))
                        }
                    )
                    .clickable { showAiPopup = true }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("Delete this note? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(onBack)
                }) { Text(stringResource(R.string.delete), color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAiPopup) {
        AiPopup(
            viewModel = viewModel,
            onDismiss = { showAiPopup = false },
            onApply = { markdown ->
                viewModel.applyAiMarkdown(markdown)
                val newBlocks = viewModel.blocks.value
                editorBlocks.clear()
                editorBlocks.addAll(newBlocks.map { b ->
                    EditorBlock(
                        id = b.id,
                        type = b.type,
                        textFieldValue = TextFieldValue(b.content, TextRange(b.content.length)),
                        isChecked = b.isChecked
                    )
                })
                if (editorBlocks.isEmpty()) {
                    editorBlocks.add(EditorBlock(type = BlockType.PARAGRAPH))
                }
            }
        )
    }
}

private fun wrapSelection(blocks: MutableList<EditorBlock>, index: Int, open: String, close: String) {
    if (index !in blocks.indices) return
    val block = blocks[index]
    val text = block.textFieldValue.text
    val sel = block.textFieldValue.selection
    val start = sel.start.coerceIn(0, text.length)
    val end = sel.end.coerceIn(0, text.length)
    val before = text.substring(0, start)
    val selected = text.substring(start, end)
    val after = text.substring(end)
    val newText = before + open + selected + close + after
    val newCursor = if (sel.collapsed) start + open.length else end + open.length + close.length
    blocks[index] = block.copy(textFieldValue = TextFieldValue(newText, TextRange(newCursor)))
}

@Composable
private fun ToolbarGlassButton(label: String, onClick: () -> Unit) {
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .padding(2.dp)
            .drawBackdrop(
                backdrop = rememberLayerBackdrop(),
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(if (isDark) Color(0xFFFAFAFA).copy(0.2f) else Color(0xFF121212).copy(0.2f))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun BlockGlassButton(
    label: String,
    type: BlockType,
    focusedIndex: Int,
    editorBlocks: MutableList<EditorBlock>
) {
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val selected = focusedIndex >= 0 && editorBlocks.getOrNull(focusedIndex)?.type == type
    Box(
        modifier = Modifier
            .padding(2.dp)
            .drawBackdrop(
                backdrop = rememberLayerBackdrop(),
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(if (isDark) Color(0xFFFAFAFA).copy(0.2f) else Color(0xFF121212).copy(0.2f))
                }
            )
            .clickable {
                if (focusedIndex in editorBlocks.indices) {
                    val curr = editorBlocks[focusedIndex]
                    editorBlocks[focusedIndex] = curr.copy(type = type, isChecked = if (type == BlockType.CHECKBOX) curr.isChecked else false)
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
private fun BlockEditor(
    block: EditorBlock,
    onBlockChange: (TextFieldValue) -> Unit,
    onFocus: () -> Unit,
    isFocused: Boolean,
    onEnter: () -> Unit,
    onBackspaceAtStart: () -> Unit,
    onToggleCheckbox: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isFocused) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    val commonModifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { if (it.isFocused) onFocus() }
        .focusRequester(focusRequester)
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                if (event.key == Key.Enter) {
                    onEnter()
                    return@onPreviewKeyEvent true
                }
                if (event.key == Key.Backspace) {
                    val text = block.textFieldValue.text
                    val cursor = block.textFieldValue.selection.start
                    if (cursor == 0) {
                        onBackspaceAtStart()
                        return@onPreviewKeyEvent true
                    }
                }
            }
            false
        }

    when (block.type) {
        BlockType.HEADING1 -> {
            BasicTextField(
                value = block.textFieldValue,
                onValueChange = onBlockChange,
                textStyle = MaterialTheme.typography.displayLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = commonModifier,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
            )
        }
        BlockType.HEADING2 -> {
            BasicTextField(
                value = block.textFieldValue,
                onValueChange = onBlockChange,
                textStyle = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = commonModifier,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
            )
        }
        BlockType.HEADING3 -> {
            BasicTextField(
                value = block.textFieldValue,
                onValueChange = onBlockChange,
                textStyle = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = commonModifier,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
            )
        }
        BlockType.BULLET -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("•", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
                BasicTextField(
                    value = block.textFieldValue,
                    onValueChange = onBlockChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    modifier = commonModifier.weight(1f),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
        BlockType.NUMBERED -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("1.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
                BasicTextField(
                    value = block.textFieldValue,
                    onValueChange = onBlockChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    modifier = commonModifier.weight(1f),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
        BlockType.QUOTE -> {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 8.dp)
                        .width(3.dp)
                        .height(20.dp)
                        .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
                BasicTextField(
                    value = block.textFieldValue,
                    onValueChange = onBlockChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    ),
                    modifier = commonModifier.weight(1f),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
        BlockType.CHECKBOX -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val checked = block.isChecked
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            if (checked) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onToggleCheckbox() },
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) {
                        Text("✓", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = block.textFieldValue,
                    onValueChange = onBlockChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    modifier = commonModifier.weight(1f),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
        BlockType.CODE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = block.textFieldValue,
                    onValueChange = onBlockChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = commonModifier.fillMaxWidth(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
        BlockType.PARAGRAPH -> {
            BasicTextField(
                value = block.textFieldValue,
                onValueChange = onBlockChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = commonModifier,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}

data class EditorBlock(
    val id: Long = 0,
    val type: BlockType = BlockType.PARAGRAPH,
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val isChecked: Boolean = false
)
