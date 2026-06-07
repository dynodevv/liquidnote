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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidnote.app.LiquidNoteApplication
import com.liquidnote.app.R
import com.liquidnote.app.model.Category
import com.liquidnote.app.model.Note
import com.liquidnote.app.ui.components.LiquidBottomTab
import com.liquidnote.app.ui.components.LiquidBottomTabs
import com.liquidnote.app.ui.components.LiquidButton
import com.liquidnote.app.ui.viewmodel.AppViewModelFactory
import com.liquidnote.app.ui.viewmodel.HomeViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = LiquidNoteApplication.instance(context.applicationContext as android.app.Application)
    val factory = remember { AppViewModelFactory(app.repository, app.settingsManager) }
    val viewModel: HomeViewModel = viewModel(factory = factory)

    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Note?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val backdrop = rememberLayerBackdrop()
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background content that will be blurred by glass
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .safeContentPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(8f.dp.toPx(), 8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(containerColor)
                        }
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(stringResource(R.string.search), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories row
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                item {
                    CategoryPill(
                        label = stringResource(R.string.all_notes),
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(categories) { cat ->
                    CategoryPill(
                        label = cat.name,
                        color = Color(cat.color),
                        selected = selectedCategory == cat.id,
                        onClick = { viewModel.selectCategory(cat.id) }
                    )
                }
                item {
                    CategoryPill(
                        label = "+",
                        selected = false,
                        onClick = { showCategoryDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes grid
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.no_notes), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.no_notes_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            category = categories.find { it.id == note.categoryId },
                            onClick = { onNoteClick(note.id) },
                            onDelete = { showDeleteDialog = note }
                        )
                    }
                }
            }
        }

        // Floating bottom tabs (Liquid Glass)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            LiquidBottomTabs(
                selectedTabIndex = { 0 },
                onTabSelected = {},
                backdrop = backdrop,
                tabsCount = 3,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidBottomTab(onClick = {}) {
                    Icon(Icons.Default.Search, stringResource(R.string.notes), tint = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.notes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                }
                LiquidBottomTab(onClick = onNewNote) {
                    Icon(Icons.Default.Add, stringResource(R.string.new_note), tint = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.new_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                }
                LiquidBottomTab(onClick = onSettings) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.settings), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(showDeleteDialog!!)
                    showDeleteDialog = null
                }) { Text("Delete", color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        val colors = listOf(
            Color(0xFF007AFF), Color(0xFF34C759), Color(0xFFFF9500),
            Color(0xFFFF3B30), Color(0xFFAF52DE), Color(0xFF5AC8FA),
            Color(0xFFFF2D55), Color(0xFF5856D6)
        )
        var selColor by remember { mutableStateOf(colors.first()) }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(stringResource(R.string.add_category)) },
            text = {
                Column {
                    TextField(value = catName, onValueChange = { catName = it }, label = { Text(stringResource(R.string.category_name)) }, singleLine = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colors.size) { idx ->
                            val c = colors[idx]
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .then(if (selColor == c) Modifier.padding(2.dp) else Modifier)
                                    .background(c, Capsule())
                                    .clickable { selColor = c }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (catName.isNotBlank()) {
                        viewModel.createCategory(catName, selColor.toArgb())
                        showCategoryDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun CategoryPill(
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val surface = if (isDark) Color(0xFFFAFAFA).copy(0.35f) else Color(0xFF121212).copy(0.35f)
    Box(
        modifier = Modifier
            .height(36.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(6f.dp.toPx())
                    lens(6f.dp.toPx(), 6f.dp.toPx())
                },
                onDrawSurface = { drawRect(surface) }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) color else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    category: Category?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val isDark = !androidx.compose.foundation.isSystemInDarkTheme()
    val surface = if (isDark) Color(0xFFFAFAFA).copy(0.25f) else Color(0xFF121212).copy(0.25f)
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                    lens(12f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = { drawRect(surface) }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = note.title.ifBlank { stringResource(R.string.untitled) },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateFormat.format(Date(note.updatedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (category != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color(category.color)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}


