package de.radiolumen.hoererfunk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.radiolumen.core.CatalogTrack
import de.radiolumen.core.FeedbackKind
import de.radiolumen.core.NowPlaying
import de.radiolumen.core.StudioEvent
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

@Composable
fun LumenApp(viewModel: LumenViewModel) {
    val pane by viewModel.pane.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val banner by viewModel.banner.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val studio by viewModel.studio.collectAsStateWithLifecycle()
    val wish by viewModel.wish.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(banner) {
        val message = banner ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeBanner()
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (sheet == null && pane != Pane.INFO) {
                StationBar(
                    pane = pane,
                    urgent = studio.snapshot.openUrgent,
                    onListen = { viewModel.open(Pane.LISTEN) },
                    onWish = { viewModel.open(Pane.WISH) },
                    onStudio = { viewModel.open(Pane.STUDIO) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (pane) {
                Pane.LISTEN -> ListenPane(
                    now = now,
                    ready = ready,
                    onPlaylist = viewModel::openPlaylistSheet,
                    onHost = viewModel::openHostSheet,
                    onInfo = { viewModel.open(Pane.INFO) },
                )

                Pane.WISH -> WishPane(
                    wish = wish,
                    ready = ready,
                    onQuery = viewModel::updateWishQuery,
                    onSelect = viewModel::selectWish,
                    onDedication = viewModel::updateDedication,
                    onSubmit = viewModel::submitWish,
                )

                Pane.STUDIO -> StudioPane(
                    studio = studio,
                    onCode = viewModel::updateStudioCode,
                    onUnlock = viewModel::unlockStudio,
                    onLock = viewModel::lockStudio,
                    onAcknowledge = viewModel::acknowledge,
                )

                Pane.INFO -> InfoPane(onBack = { viewModel.open(Pane.LISTEN) })
            }
            if (sheet != null) {
                RatingSheet(
                    sheet = sheet,
                    score = score,
                    tags = tags,
                    note = note,
                    options = viewModel.tagsFor(sheet),
                    onScore = viewModel::selectScore,
                    onTag = viewModel::toggleTag,
                    onNote = viewModel::updateNote,
                    onClose = viewModel::closeSheet,
                    onSubmit = viewModel::submitSheet,
                )
            }
        }
    }
}

@Composable
private fun ListenPane(
    now: NowPlaying?,
    ready: Boolean,
    onPlaylist: () -> Unit,
    onHost: () -> Unit,
    onInfo: () -> Unit,
) {
    if (now == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sendeablauf wird gelesen …", color = Muted)
        }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Lumen",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text("Radio der Hafenstadt", color = Muted, fontSize = 13.sp)
            }
            LiveMark()
            IconButton(onClick = onInfo) {
                Icon(Icons.Filled.Info, contentDescription = "Erläuterung zur App", tint = TealDark)
            }
        }
        Spacer(Modifier.height(16.dp))
        TrackCard(now)
        Spacer(Modifier.height(18.dp))
        SectionLabel("Als Nächstes")
        now.upNext.forEachIndexed { index, track ->
            Text(
                "${index + 1}.  ${track.title}  ·  ${track.artist}",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 15.sp,
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Playlist bewerten", enabled = ready, onClick = onPlaylist)
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            label = if (now.show.moderated) "Moderation bewerten" else "Keine Moderation in dieser Stunde",
            enabled = ready && now.show.moderated,
            onClick = onHost,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TrackCard(now: NowPlaying) {
    val clock by produceState(System.currentTimeMillis(), now.version) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Card)
            .border(1.dp, Line, RoundedCornerShape(4.dp))
            .padding(18.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            now.track.album,
            color = Muted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(now.track.title, style = MaterialTheme.typography.headlineLarge)
        Text(now.track.artist, fontSize = 18.sp, color = TealDark)
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { now.elapsedFraction(clock) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Teal,
            trackColor = Line,
        )
        Spacer(Modifier.height(8.dp))
        Text(elapsedLabel(now, clock), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(10.dp))
        Text(now.show.name, fontWeight = FontWeight.SemiBold)
        Text(
            if (now.show.moderated) "Moderation: ${now.show.hostName}" else "Ohne Moderation",
            color = Muted,
            fontSize = 14.sp,
        )
        Text(now.playlistLabel, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun WishPane(
    wish: WishUi,
    ready: Boolean,
    onQuery: (String) -> Unit,
    onSelect: (CatalogTrack) -> Unit,
    onDedication: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Wunsch", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Einen Titel aus dem Archiv wählen.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = wish.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Suche nach Titel, Interpret oder Album") },
        )
        Spacer(Modifier.height(8.dp))
        if (wish.selectedId != null) {
            Text(
                "Gewählt: ${wish.selectedTitle} · ${wish.selectedArtist}",
                color = TealDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            if (wish.results.isEmpty()) {
                item {
                    Text(
                        if (wish.query.isBlank()) "Archiv wird gelesen …" else "Kein Titel passt zu dieser Suche.",
                        color = Muted,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(wish.results, key = { it.id }) { track ->
                CatalogRow(track, selected = track.id == wish.selectedId, onClick = { onSelect(track) })
            }
        }
        OutlinedTextField(
            value = wish.dedication,
            onValueChange = onDedication,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Widmung, optional") },
            supportingText = { Text("${wish.dedication.length} von 140 Zeichen") },
        )
        PrimaryButton("Wunsch senden", enabled = ready && wish.selectedId != null, onClick = onSubmit)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun CatalogRow(track: CatalogTrack, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Card)
            .border(1.dp, if (selected) Teal else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = Ink,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text("${track.artist} · ${track.album}", color = Muted, fontSize = 13.sp)
        }
        Text(track.year.toString(), color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun StudioPane(
    studio: StudioUi,
    onCode: (String) -> Unit,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onAcknowledge: (String) -> Unit,
) {
    if (!studio.unlocked) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Text("Regie", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(
                "Bewertungen und Wünsche dieser Sitzung. Der Zugang gilt nur für die laufende Sendung auf diesem Gerät.",
                color = Muted,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = studio.code,
                onValueChange = onCode,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Studiocode") },
                isError = studio.error != null,
                supportingText = { Text(studio.error ?: "Referenzcode: lumen-regie") },
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Regie öffnen", enabled = studio.code.isNotBlank(), onClick = onUnlock)
        }
        return
    }
    val mood = studio.snapshot.mood
    val openUrgent = studio.snapshot.openUrgent
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Regie",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = onLock) { Text("Schließen") }
        }
        Text(
            mood.average?.let { "${mood.label} · ${"%.1f".format(it)} aus ${mood.sampleSize}" } ?: mood.label,
            color = Muted,
        )
        if (openUrgent > 0) {
            Text(
                if (openUrgent == 1) "1 dringender Hinweis" else "$openUrgent dringende Hinweise",
                color = Urgent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (studio.snapshot.events.isEmpty()) {
            Text(
                "Noch keine Rückmeldung in dieser Sitzung.",
                color = Muted,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(studio.snapshot.events, key = { it.id }) { event ->
                    EventCard(event, onAcknowledge)
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: StudioEvent, onAcknowledge: (String) -> Unit) {
    val highlighted = event.urgent && !event.acknowledged
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Card)
            .border(1.dp, if (highlighted) Urgent else Line, RoundedCornerShape(4.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                kindLabel(event.kind),
                fontSize = 13.sp,
                color = if (highlighted) Urgent else Teal,
                fontWeight = FontWeight.SemiBold,
            )
            if (highlighted) {
                Text("  ·  Dringend", fontSize = 13.sp, color = Urgent, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text(clockLabel(event.createdAtEpochMs), color = Muted, fontSize = 13.sp)
        }
        Text(event.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        if (event.detail.isNotBlank()) {
            Text(event.detail, color = Ink, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
        }
        event.score?.let { score ->
            Text("Stufe $score von 5", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (!event.acknowledged) {
            OutlinedButton(
                onClick = { onAcknowledge(event.id) },
                modifier = Modifier.padding(top = 8.dp).height(44.dp),
                shape = RoundedCornerShape(4.dp),
            ) { Text("Gesehen") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingSheet(
    sheet: Sheet?,
    score: Int,
    tags: Set<String>,
    note: String,
    options: List<String>,
    onScore: (Int) -> Unit,
    onTag: (String) -> Unit,
    onNote: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAA1C1915))
            .clickable(onClick = onClose),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(Paper)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Text(
                if (sheet == Sheet.HOST) "Moderation bewerten" else "Playlist bewerten",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                if (score in 1..5) "Stufe $score von 5" else "Zuerst eine Stufe von 1 bis 5 wählen.",
                color = Muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
            Row {
                (1..5).forEach { value ->
                    IconButton(onClick = { onScore(value) }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Stufe $value von 5",
                            tint = if (value <= score) Ink else Line,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            Text("Bis zu drei Schlagworte", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { tag ->
                    FilterChip(
                        selected = tag in tags,
                        onClick = { onTag(tag) },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Card,
                            labelColor = Ink,
                            selectedContainerColor = Teal,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            if (sheet == Sheet.PLAYLIST) {
                OutlinedTextField(
                    value = note,
                    onValueChange = onNote,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Kurzer Hinweis, optional") },
                    supportingText = { Text("${note.length} von 240 Zeichen") },
                )
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("An die Regie senden", enabled = score in 1..5, onClick = onSubmit)
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Abbrechen")
            }
        }
    }
}

@Composable
private fun InfoPane(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
        }
        Text("Was diese App leistet", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        Text("Lumen ist der Rückkanal zum laufenden Programm. Das Hören bleibt beim gewohnten Empfang über UKW, DAB oder den Stream des Senders.")
        Spacer(Modifier.height(12.dp))
        InfoPoint("Hören", "zeigt den laufenden Titel, die Sendung und die nächsten Titel.")
        InfoPoint("Wunsch", "schickt einen Titel aus dem Archiv an die Regie. Eine Widmung ist freiwillig.")
        InfoPoint("Regie", "sieht Bewertungen und Wünsche dieser Sitzung. Der Referenzcode lautet lumen-regie.")
        Spacer(Modifier.height(8.dp))
        Text("Playlist und Moderation lassen sich je Stunde bzw. je Strecke einmal bewerten. Zwischen zwei Wünschen liegen mindestens 15 Sekunden. Dringende Hinweise stehen in der Regie oben und sind rot markiert.")
    }
}

@Composable
private fun InfoPoint(title: String, body: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(body)
    }
}

@Composable
private fun StationBar(
    pane: Pane,
    urgent: Int,
    onListen: () -> Unit,
    onWish: () -> Unit,
    onStudio: () -> Unit,
) {
    Column(Modifier.background(Card)) {
        HorizontalDivider(color = Line)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BarItem("Hören", pane == Pane.LISTEN, onListen)
            BarItem("Wunsch", pane == Pane.WISH, onWish)
            BarItem(if (urgent > 0) "Regie · $urgent" else "Regie", pane == Pane.STUDIO, onStudio)
        }
    }
}

@Composable
private fun BarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .height(52.dp)
            .width(108.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            color = if (selected) TealDark else Muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Teal,
            contentColor = Color.White,
            disabledContainerColor = Line,
            disabledContentColor = Muted,
        ),
    ) { Text(label) }
}

@Composable
private fun SecondaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(4.dp),
    ) { Text(label) }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun LiveMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(99.dp)).background(Live))
        Spacer(Modifier.width(6.dp))
        Text("Live", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Live)
    }
}

private fun elapsedLabel(now: NowPlaying, clock: Long): String {
    val seconds = ((clock - now.startedAtEpochMs) / 1000L).toInt().coerceIn(0, now.durationSec)
    val remaining = (now.durationSec - seconds).coerceAtLeast(0)
    return "${clockText(seconds)}  ·  noch ${clockText(remaining)}"
}

private fun clockText(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

private fun clockLabel(epochMs: Long): String {
    val time = Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("Europe/Berlin"))
    return "%02d:%02d".format(time.hour, time.minute)
}

private fun kindLabel(kind: FeedbackKind): String = when (kind) {
    FeedbackKind.PLAYLIST -> "Playlist"
    FeedbackKind.HOST -> "Moderation"
    FeedbackKind.WISH -> "Wunsch"
}
