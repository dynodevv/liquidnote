package com.liquidnote.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.liquidnote.app.ui.components.LiquidFAB
import com.liquidnote.app.ui.components.LiquidSurface
import com.liquidnote.app.ui.components.RichTextField
import com.liquidnote.app.ui.components.wrapSelection
import com.liquidnote.app.ui.theme.AppleBlue
import com.liquidnote.app.ui.viewmodel.AppViewModelFactory
import com.liquidnote.app.ui.viewmodel.NoteEditorViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

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

    var title by remember { mutableStateOf(note.title) }
    val editorBlocks = remember { mutableStateListOf<EditorBlock>() }
    var focusedIndex by remember { mutableIntStateOf(-1) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAiPopup by remember { mutableStateOf(false) }
    var showBlockTypeMenu by remember { mutableStateOf(false) }
    var showInlineMenu by remember { mutableStateOf(false) }

    // Sync blocks from ViewModel to local editor state
    LaunchedEffect(blocks) {
        if (editorBlocks.isEmpty() || editorBlocks.size != blocks.size) {
            editorBlocks.clear()
            editorBlocks.addAll(blocks.map { block ->
                EditorBlock(
                    id = block.id,
                    type = block.type,
                    textFieldValue = TextFieldValue(block.content, TextRange(block.content.length)),
                    isChecked = block.isChecked
                )
            })
        }
    }

    // Sync title
    LaunchedEffect(note) {
        title = note.title
    }

    // Auto-save debounced
    LaunchedEffect(editorBlocks, title) {
        snapshotFlow {
            title to editorBlocks.map { it.textFieldValue.text }
        }
            .debounce(1000)
            .collect { (t, contents) ->
                viewModel.updateTitle(t)
                val newBlocks = editorBlocks.mapIndexed { index, editorBlock ->
                    com.liquidnote.app.model.Block(
                        id = editorBlock.id,
                        noteId = note.id,
                        type = editorBlock.type,
                        content = contents.getOrElse(index) { "" },
                        order = index,
                        isChecked = editorBlock.isChecked
                    )
                }
                viewModel.updateBlocks(newBlocks)
                viewModel.saveNote()
            }
    }

    // Save on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateTitle(title)
            val newBlocks = editorBlocks.mapIndexed { index, editorBlock ->
                com.liquidnote.app.model.Block(
                    id = editorBlock.id,
                    noteId = note.id,
                    type = editorBlock.type,
                    content = editorBlock.textFieldValue.text,
                    order = index,
                    isChecked = editorBlock.isChecked
                )
            }
            viewModel.updateBlocks(newBlocks)
            viewModel.saveNote()
        }
    }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            LiquidSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                blurRadius = 20.dp,
                containerAlpha = 0.3f
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export)) },
                                onClick = {
                                    showMoreMenu = false
                                    val uri = viewModel.exportNote(context)
                                    // Share intent handled via Toast or system share
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
                                    viewModel.deleteNote(onBack)
                                }
                            )
                        }
                    }
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
                }
            }

            // Blocks
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            val newBlock = EditorBlock(
                                type = if (editorBlock.type == BlockType.CHECKBOX || editorBlock.type == BlockType.BULLET || editorBlock.type == BlockType.NUMBERED)
                                    editorBlock.type else BlockType.PARAGRAPH,
                                textFieldValue = TextFieldValue(after, TextRange(0))
                            )
                            editorBlocks.add(index + 1, newBlock)
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
                                focusedIndex = 0
                            }
                        },
                        onToggleCheckbox = {
                            editorBlocks[index] = editorBlock.copy(isChecked = !editorBlock.isChecked)
                        }
                    )
                }
            }

            // Bottom toolbar
            LiquidSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                blurRadius = 20.dp,
                containerAlpha = 0.35f
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Inline formatting (only for focused paragraph)
                    if (focusedIndex >= 0 && editorBlocks.getOrNull(focusedIndex)?.type == BlockType.PARAGRAPH) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ToolbarButton("B", FontWeight.Bold) {
                                applyInlineTag(editorBlocks, focusedIndex, "[b]", "[/b]")
                            }
                            ToolbarButton("I", FontStyle.Italic) {
                                applyInlineTag(editorBlocks, focusedIndex, "[i]", "[/i]")
                            }
                            ToolbarButton("`", FontFamily.Monospace) {
                                applyInlineTag(editorBlocks, focusedIndex, "[c]", "[/c]")
                            }
                            ToolbarButton("L", textDecoration = TextDecoration.Underline) {
                                // Link requires dialog; simplified: insert placeholder
                                applyInlineTag(editorBlocks, focusedIndex, "[l]", "|https://[/l]")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Block types
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BlockTypeButton("T", BlockType.PARAGRAPH, focusedIndex, editorBlocks)
                        BlockTypeButton("H1", BlockType.HEADING1, focusedIndex, editorBlocks)
                        BlockTypeButton("H2", BlockType.HEADING2, focusedIndex, editorBlocks)
                        BlockTypeButton("H3", BlockType.HEADING3, focusedIndex, editorBlocks)
                        BlockTypeButton("•", BlockType.BULLET, focusedIndex, editorBlocks)
                        BlockTypeButton("1.", BlockType.NUMBERED, focusedIndex, editorBlocks)
                        BlockTypeButton("✓", BlockType.CHECKBOX, focusedIndex, editorBlocks)
                        BlockTypeButton("\"", BlockType.QUOTE, focusedIndex, editorBlocks)
                        BlockTypeButton("</>", BlockType.CODE, focusedIndex, editorBlocks)
                    }
                }
            }
        }

        // AI FAB
        if (isAiEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 20.dp)
            ) {
                LiquidFAB(onClick = { showAiPopup = true }) {
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
    }

    if (showAiPopup) {
        AiPopup(
            viewModel = viewModel,
            onDismiss = { showAiPopup = false },
            onApply = { markdown ->
                viewModel.applyAiMarkdown(markdown)
                // Sync back to editorBlocks
                val newBlocks = viewModel.blocks.value
                editorBlocks.clear()
                editorBlocks.addAll(newBlocks.map { block ->
                    EditorBlock(
                        id = block.id,
                        type = block.type,
                        textFieldValue = TextFieldValue(block.content, TextRange(block.content.length)),
                        isChecked = block.isChecked
                    )
                })
            }
        )
    }
}

private fun applyInlineTag(blocks: MutableList<EditorBlock>, index: Int, openTag: String, closeTag: String) {
    if (index !in blocks.indices) return
    val block = blocks[index]
    val text = block.textFieldValue.text
    val selection = block.textFieldValue.selection
    val newText = wrapSelection(text, selection, openTag, closeTag)
    val newCursor = if (selection.collapsed) {
        selection.start + openTag.length
    } else {
        selection.end + openTag.length + closeTag.length
    }
    blocks[index] = block.copy(textFieldValue = TextFieldValue(newText, TextRange(newCursor)))
}

@Composable
private fun BlockTypeButton(
    label: String,
    type: BlockType,
    focusedIndex: Int,
    editorBlocks: MutableList<EditorBlock>
) {
    val selected = focusedIndex >= 0 && editorBlocks.getOrNull(focusedIndex)?.type == type
    LiquidButton(
        onClick = {
            if (focusedIndex in editorBlocks.indices) {
                val current = editorBlocks[focusedIndex]
                editorBlocks[focusedIndex] = current.copy(type = type)
                if (type == BlockType.CHECKBOX && !current.isChecked) {
                    editorBlocks[focusedIndex] = current.copy(type = type, isChecked = false)
                }
            }
        },
        shape = RoundedCornerShape(10.dp),
        blurRadius = 4.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) AppleBlue else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    fontWeight: FontWeight? = null,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontFamily: FontFamily? = null,
    textDecoration: TextDecoration? = null,
    onClick: () -> Unit
) {
    LiquidButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        blurRadius = 4.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontFamily = fontFamily,
                textDecoration = textDecoration,
                color = MaterialTheme.colorScheme.onBackground
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
                    if (cursor == 0 && text.isEmpty()) {
                        onBackspaceAtStart()
                        return@onPreviewKeyEvent true
                    }
                    if (cursor == 0 && text.isNotEmpty()) {
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
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
                            if (checked) AppleBlue else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .then(
                            if (!checked) Modifier.background(
                                Color.Gray.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            ) else Modifier
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
            RichTextField(
                value = block.textFieldValue,
                onValueChange = onBlockChange,
                modifier = commonModifier,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                singleLine = false
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
