package de.radiolumen.hoererfunk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.radiolumen.core.CatalogTrack
import de.radiolumen.core.Mood
import de.radiolumen.core.NowPlaying
import de.radiolumen.core.StudioSnapshot
import de.radiolumen.core.SubmitResult
import de.radiolumen.core.Vocabulary
import de.radiolumen.hoererfunk.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Pane { LISTEN, WISH, STUDIO, INFO }

enum class Sheet { PLAYLIST, HOST }

data class WishUi(
    val query: String = "",
    val results: List<CatalogTrack> = emptyList(),
    val selectedId: String? = null,
    val selectedTitle: String = "",
    val selectedArtist: String = "",
    val dedication: String = "",
)

data class StudioUi(
    val unlocked: Boolean = false,
    val code: String = "",
    val error: String? = null,
    val snapshot: StudioSnapshot = StudioSnapshot(
        events = emptyList(),
        mood = Mood(null, "Noch keine Rückmeldung", 0),
        openUrgent = 0,
    ),
)

@OptIn(FlowPreview::class)
class LumenViewModel(private val graph: AppGraph) : ViewModel() {
    val now: StateFlow<NowPlaying?> = graph.nowPlaying.stream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _pane = MutableStateFlow(Pane.LISTEN)
    val pane: StateFlow<Pane> = _pane.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _banner = MutableStateFlow<String?>(null)
    val banner: StateFlow<String?> = _banner.asStateFlow()

    private val _sheet = MutableStateFlow<Sheet?>(null)
    val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _tags = MutableStateFlow<Set<String>>(emptySet())
    val tags: StateFlow<Set<String>> = _tags.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _studio = MutableStateFlow(StudioUi())
    val studio: StateFlow<StudioUi> = _studio.asStateFlow()

    private val _wish = MutableStateFlow(WishUi())
    val wish: StateFlow<WishUi> = _wish.asStateFlow()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            graph.warm()
            _ready.value = true
            publishStudio()
        }
        viewModelScope.launch {
            queries.debounce { query -> if (query.isEmpty()) 0L else 200L }.collect { query ->
                val results = withContext(Dispatchers.Default) { graph.catalog.search(query, limit = 18) }
                _wish.update { it.copy(results = results) }
            }
        }
    }

    fun open(pane: Pane) {
        _pane.value = pane
    }

    fun openPlaylistSheet() = openSheet(Sheet.PLAYLIST)

    fun openHostSheet() {
        val playing = now.value
        if (playing != null && !playing.show.moderated) {
            _banner.value = "Diese Strecke läuft ohne Moderation."
            return
        }
        openSheet(Sheet.HOST)
    }

    fun closeSheet() {
        _sheet.value = null
    }

    fun selectScore(value: Int) {
        _score.value = value
    }

    fun toggleTag(tag: String) {
        val current = _tags.value
        if (tag !in current && current.size >= 3) {
            _banner.value = "Höchstens drei Schlagworte."
            return
        }
        _tags.value = if (tag in current) current - tag else current + tag
    }

    fun updateNote(value: String) {
        _note.value = value.take(240)
    }

    fun updateWishQuery(value: String) {
        _wish.update { it.copy(query = value) }
        queries.value = value
    }

    fun selectWish(track: CatalogTrack) {
        _wish.update {
            it.copy(selectedId = track.id, selectedTitle = track.title, selectedArtist = track.artist)
        }
    }

    fun updateDedication(value: String) {
        _wish.update { it.copy(dedication = value.take(140)) }
    }

    fun updateStudioCode(value: String) {
        _studio.update { it.copy(code = value.take(40), error = null) }
    }

    fun unlockStudio() {
        val code = _studio.value.code.trim()
        if (!code.equals(AppGraph.STUDIO_CODE, ignoreCase = true)) {
            _studio.update { it.copy(error = "Der Regiezugang passt nicht.") }
            return
        }
        _studio.update { it.copy(unlocked = true, error = null) }
        publishStudio()
    }

    fun lockStudio() {
        _studio.update { it.copy(unlocked = false, code = "", error = null) }
    }

    fun acknowledge(id: String) {
        graph.coordinator.acknowledge(id)
        publishStudio()
    }

    fun submitSheet() {
        when (_sheet.value) {
            Sheet.PLAYLIST -> submitPlaylist()
            Sheet.HOST -> submitHost()
            null -> Unit
        }
    }

    fun submitWish() {
        val selectedId = _wish.value.selectedId
        val selected = graph.catalog.search("", limit = 50).firstOrNull { it.id == selectedId }
        if (selected == null) {
            _banner.value = "Zuerst einen Titel aus dem Archiv wählen."
            return
        }
        val dedication = _wish.value.dedication
        launchSubmit {
            graph.coordinator.wish(selected, dedication)
        }
    }

    fun consumeBanner() {
        _banner.value = null
    }

    fun tagsFor(sheet: Sheet?): List<String> = when (sheet) {
        Sheet.PLAYLIST -> Vocabulary.playlistTags
        Sheet.HOST -> Vocabulary.hostTags
        null -> emptyList()
    }

    private fun openSheet(sheet: Sheet) {
        _score.value = 0
        _tags.value = emptySet()
        _note.value = ""
        _sheet.value = sheet
    }

    private fun submitPlaylist() {
        val playing = now.value ?: return
        val score = _score.value
        val tags = _tags.value.toList()
        val note = _note.value
        launchSubmit {
            graph.coordinator.ratePlaylist(playing.playlistId, score, tags, note)
        }
    }

    private fun submitHost() {
        val playing = now.value ?: return
        val score = _score.value
        val tags = _tags.value.toList()
        launchSubmit {
            graph.coordinator.rateHost(
                showInstanceId = playing.show.instanceId,
                hostId = playing.show.hostId,
                hostName = playing.show.hostName,
                score = score,
                tags = tags,
                moderated = playing.show.moderated,
            )
        }
    }

    private fun launchSubmit(block: () -> SubmitResult) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { block() }
            when (result) {
                is SubmitResult.Accepted -> {
                    _sheet.value = null
                    _wish.update {
                        it.copy(selectedId = null, selectedTitle = "", selectedArtist = "", dedication = "")
                    }
                    _banner.value = if (result.delivered) {
                        "An die Regie übergeben."
                    } else {
                        "Gespeichert. Die Übergabe wird nachgereicht."
                    }
                    publishStudio()
                }

                is SubmitResult.Rejected -> _banner.value = result.reason
            }
        }
    }

    private fun publishStudio() {
        _studio.update { it.copy(snapshot = graph.coordinator.snapshot()) }
    }

    companion object {
        fun factory(graph: AppGraph): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LumenViewModel(graph) as T
            }
    }
}
