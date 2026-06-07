package com.liquidnote.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liquidnote.app.model.Block
import com.liquidnote.app.model.BlockType
import com.liquidnote.app.model.Category
import com.liquidnote.app.model.Note
import com.liquidnote.app.repository.NoteRepository
import com.liquidnote.app.util.AIClient
import com.liquidnote.app.util.MarkdownExport
import com.liquidnote.app.util.MarkdownImport
import com.liquidnote.app.util.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory(
    private val repository: NoteRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repository) as T
            modelClass.isAssignableFrom(NoteEditorViewModel::class.java) ->
                NoteEditorViewModel(repository, settingsManager) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository, settingsManager) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<Long?>(null)
    val selectedCategory: StateFlow<Long?> = _selectedCategory

    val notes = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        if (query.isNotBlank()) {
            repository.searchNotes(query)
        } else if (category != null) {
            repository.getNotesByCategory(category)
        } else {
            repository.getAllNotes()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategory.value = categoryId
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun createCategory(name: String, color: Int) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, color = color))
        }
    }
}

class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _note = MutableStateFlow(Note())
    val note: StateFlow<Note> = _note

    private val _blocks = MutableStateFlow<List<Block>>(emptyList())
    val blocks: StateFlow<List<Block>> = _blocks

    private val _isAiEnabled = MutableStateFlow(true)
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled

    private val _aiMessages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val aiMessages: StateFlow<List<Pair<String, Boolean>>> = _aiMessages

    private var snapshotBeforeAi: Pair<Note, List<Block>>? = null
    private var _aiEdited = MutableStateFlow(false)
    val aiEdited: StateFlow<Boolean> = _aiEdited

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading

    init {
        viewModelScope.launch {
            settingsManager.aiEnabled.collect { _isAiEnabled.value = it }
        }
    }

    fun loadNote(noteId: Long) {
        if (noteId == 0L) {
            _note.value = Note()
            _blocks.value = listOf(Block(noteId = 0, type = BlockType.PARAGRAPH, content = "", order = 0))
            return
        }
        viewModelScope.launch {
            val loaded = repository.getNoteWithBlocks(noteId)
            loaded?.let {
                _note.value = it.note.let { n ->
                    Note(n.id, n.title, n.categoryId, n.createdAt, n.updatedAt)
                }
                val loadedBlocks = it.blocks.map { b ->
                    Block(b.id, b.noteId, BlockType.valueOf(b.type.uppercase()), b.content, b.orderIndex, b.isChecked)
                }.sortedBy { it.order }
                _blocks.value = loadedBlocks.ifEmpty {
                    listOf(Block(noteId = noteId, type = BlockType.PARAGRAPH, content = "", order = 0))
                }
            } ?: run {
                _note.value = Note()
                _blocks.value = listOf(Block(noteId = 0, type = BlockType.PARAGRAPH, content = "", order = 0))
            }
        }
    }

    fun updateTitle(title: String) {
        _note.value = _note.value.copy(title = title, updatedAt = System.currentTimeMillis())
    }

    fun updateBlocks(newBlocks: List<Block>) {
        _blocks.value = newBlocks.mapIndexed { index, block -> block.copy(order = index) }
    }

    fun updateBlockContent(index: Int, content: String) {
        val list = _blocks.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(content = content)
            _blocks.value = list
        }
    }

    fun updateBlockType(index: Int, type: BlockType) {
        val list = _blocks.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(type = type)
            _blocks.value = list
        }
    }

    fun toggleCheckbox(index: Int) {
        val list = _blocks.value.toMutableList()
        if (index in list.indices && list[index].type == BlockType.CHECKBOX) {
            list[index] = list[index].copy(isChecked = !list[index].isChecked)
            _blocks.value = list
        }
    }

    fun insertBlockAfter(index: Int, type: BlockType = BlockType.PARAGRAPH) {
        val list = _blocks.value.toMutableList()
        val newBlock = Block(noteId = _note.value.id, type = type, content = "", order = index + 1)
        list.add(index + 1, newBlock)
        _blocks.value = list.mapIndexed { i, b -> b.copy(order = i) }
    }

    fun removeBlockAt(index: Int): String? {
        val list = _blocks.value.toMutableList()
        if (index in list.indices) {
            val removedContent = list[index].content
            list.removeAt(index)
            _blocks.value = list.mapIndexed { i, b -> b.copy(order = i) }
            return removedContent
        }
        return null
    }

    fun mergeWithPrevious(currentIndex: Int) {
        if (currentIndex <= 0) return
        val list = _blocks.value.toMutableList()
        val prev = list[currentIndex - 1]
        val curr = list[currentIndex]
        list[currentIndex - 1] = prev.copy(content = prev.content + curr.content)
        list.removeAt(currentIndex)
        _blocks.value = list.mapIndexed { i, b -> b.copy(order = i) }
    }

    fun saveNote(): Long {
        val currentNote = _note.value.copy(updatedAt = System.currentTimeMillis())
        val currentBlocks = _blocks.value
        var savedId = currentNote.id
        viewModelScope.launch {
            savedId = repository.saveNoteWithBlocks(currentNote, currentBlocks)
            if (savedId != currentNote.id) {
                _note.value = currentNote.copy(id = savedId)
            }
        }
        return savedId
    }

    fun deleteNote(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNoteById(_note.value.id)
            onDone()
        }
    }

    fun exportNote(context: android.content.Context): android.net.Uri? {
        val n = _note.value
        val b = _blocks.value
        return if (b.isNotEmpty()) {
            kotlinx.coroutines.runBlocking {
                MarkdownExport.exportToFile(context, n, b)
            }
        } else null
    }

    fun addAiMessage(text: String, isUser: Boolean) {
        _aiMessages.value = _aiMessages.value + (text to isUser)
    }

    fun clearAiChat() {
        _aiMessages.value = emptyList()
        snapshotBeforeAi = null
        _aiEdited.value = false
    }

    fun sendAiMessage(message: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            addAiMessage(message, true)

            val endpoint = settingsManager.aiEndpoint.first()
            val key = settingsManager.aiKey.first()
            val model = settingsManager.aiModel.first()

            if (endpoint.isBlank() || key.isBlank()) {
                _isAiLoading.value = false
                onError("Configure AI endpoint and key in Settings")
                return@launch
            }

            val noteContext = MarkdownExport.blocksToMarkdown(_note.value, _blocks.value)
            val systemPrompt = """You are an AI assistant inside a note-taking app called LiquidNote. 
The user is currently editing a note. Here is the full note content in Markdown:

---
$noteContext
---

You can help the user write, edit, summarize, or restructure their note. 
If the user asks you to make changes to the note, respond with the FULL updated note in Markdown format, starting with a heading for the title.
If the user just asks a question or wants advice, respond naturally without Markdown."""

            val result = AIClient.chat(endpoint, key, model, systemPrompt, message)
            _isAiLoading.value = false

            result.onSuccess { response ->
                addAiMessage(response, false)
                onResult(response)
            }.onFailure { error ->
                onError(error.message ?: "Unknown error")
            }
        }
    }

    fun applyAiMarkdown(markdown: String) {
        snapshotBeforeAi = _note.value to _blocks.value.toList()
        val (title, newBlocks) = MarkdownImport.parseMarkdown(markdown)
        if (title.isNotBlank()) {
            _note.value = _note.value.copy(title = title)
        }
        _blocks.value = newBlocks.mapIndexed { i, b -> b.copy(noteId = _note.value.id, order = i) }.ifEmpty {
            listOf(Block(noteId = _note.value.id, type = BlockType.PARAGRAPH, content = "", order = 0))
        }
        _aiEdited.value = true
    }

    fun revertAiEdits() {
        snapshotBeforeAi?.let { (note, blocks) ->
            _note.value = note
            _blocks.value = blocks
            _aiEdited.value = false
            snapshotBeforeAi = null
        }
    }
}

class SettingsViewModel(
    private val repository: NoteRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    val aiEnabled = settingsManager.aiEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val aiEndpoint = settingsManager.aiEndpoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val aiKey = settingsManager.aiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val aiModel = settingsManager.aiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setAiEnabled(enabled) }
    }

    fun setAiEndpoint(endpoint: String) {
        viewModelScope.launch { settingsManager.setAiEndpoint(endpoint) }
    }

    fun setAiKey(key: String) {
        viewModelScope.launch { settingsManager.setAiKey(key) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { settingsManager.setAiModel(model) }
    }

    suspend fun exportAllNotes(context: android.content.Context): List<android.net.Uri> {
        val notes = repository.getAllNotes().stateIn(viewModelScope).value
        return notes.mapNotNull { note ->
            val blocks = repository.getBlocksForNoteOnce(note.id)
            if (blocks.isNotEmpty()) {
                MarkdownExport.exportToFile(context, note, blocks)
            } else null
        }
    }
}
