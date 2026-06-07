package com.liquidnote.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.liquidnote.app.ui.components.LiquidButton
import com.liquidnote.app.ui.components.LiquidFAB
import com.liquidnote.app.ui.components.LiquidIconButton
import com.liquidnote.app.ui.components.LiquidSearchBar
import com.liquidnote.app.ui.components.LiquidSurface
import com.liquidnote.app.ui.theme.AppleBlue
import com.liquidnote.app.ui.theme.AppleGreen
import com.liquidnote.app.ui.theme.AppleOrange
import com.liquidnote.app.ui.theme.ApplePink
import com.liquidnote.app.ui.theme.ApplePurple
import com.liquidnote.app.ui.theme.AppleRed
import com.liquidnote.app.ui.theme.AppleTeal
import com.liquidnote.app.ui.theme.AppleYellow
import com.liquidnote.app.ui.viewmodel.AppViewModelFactory
import com.liquidnote.app.ui.viewmodel.HomeViewModel
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                LiquidSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    blurRadius = 20.dp,
                    containerAlpha = 0.3f
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            LiquidIconButton(onClick = onSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidSearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Categories
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip(
                        label = stringResource(R.string.all_notes),
                        color = if (selectedCategory == null) AppleBlue else Color.Gray,
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(categories) { category ->
                    CategoryChip(
                        label = category.name,
                        color = Color(category.color),
                        selected = selectedCategory == category.id,
                        onClick = { viewModel.selectCategory(category.id) }
                    )
                }
                item {
                    LiquidButton(
                        onClick = { showCategoryDialog = true },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        blurRadius = 8.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_category),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.add_category),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes grid
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.no_notes),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.no_notes_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
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

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            LiquidFAB(onClick = onNewNote) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_note),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Delete dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(showDeleteDialog!!)
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = AppleRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Category dialog
    if (showCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        val colors = listOf(AppleBlue, AppleGreen, AppleOrange, AppleRed, ApplePurple, ApplePink, AppleYellow, AppleTeal)
        var selectedColor by remember { mutableStateOf(colors.first().toArgb()) }

        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(stringResource(R.string.add_category)) },
            text = {
                Column {
                    TextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .then(
                                        if (selectedColor == color.toArgb()) {
                                            Modifier.padding(2.dp)
                                        } else Modifier
                                    )
                                    .background(color, androidx.compose.foundation.shape.CircleShape)
                                    .clickable { selectedColor = color.toArgb() }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            viewModel.createCategory(categoryName, selectedColor)
                            showCategoryDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    LiquidButton(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        blurRadius = 6.dp
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) color else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    category: Category?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LiquidSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        blurRadius = 14.dp,
        containerAlpha = 0.25f
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                Box(
                    modifier = Modifier
                        .background(
                            Color(category.color).copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(category.color)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun Color.toArgb(): Int {
    return (alpha * 255 + 0.5f).toInt().shl(24) or
           (red * 255 + 0.5f).toInt().shl(16) or
           (green * 255 + 0.5f).toInt().shl(8) or
           (blue * 255 + 0.5f).toInt()
}

