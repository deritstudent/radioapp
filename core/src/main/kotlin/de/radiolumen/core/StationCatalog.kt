package de.radiolumen.core

import java.text.Normalizer

class StationCatalog(private val tracks: List<CatalogTrack>) : CatalogPort {
    override fun search(query: String, limit: Int): List<CatalogTrack> {
        val folded = fold(query.trim())
        val pool = if (folded.isEmpty()) tracks else tracks.filter { track ->
            fold("${track.artist} ${track.title} ${track.album}").contains(folded)
        }
        return pool.take(limit.coerceAtLeast(0))
    }

    companion object {
        fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace("ß", "ss")
            .lowercase()
    }
}

object DemoStation {
    val tracks: List<CatalogTrack> = listOf(
        cut("kira-nordlicht", "Kira Sol", "Nordlicht", "Hafenlichter", 2024),
        cut("miran-kaianlage", "Miran", "Kaianlage", "Gegenlicht", 2023),
        cut("ensemble-spaetsonne", "Ensemble Lumen", "Spätsonne", "Kammerton", 2022),
        cut("nova-frequenz", "Nova Brück", "Frequenz", "Stadtplan", 2025),
        cut("lanterns-boats", "The Lanterns", "Paper Boats", "Low Tide", 2021),
        cut("jule-dachfenster", "Jule Hart", "Dachfenster", "Nachtpost", 2024),
        cut("sami-market", "Sami Okonkwo", "Market Day", "Open Windows", 2020),
        cut("klara-ebbe", "Klara Voss", "Ebbe", "Salz und Honig", 2023),
        cut("canal-buoy", "Blind Canal", "Red Buoy", "Charts of Sand", 2019),
        cut("pina-laranja", "Pina Marques", "Laranja", "Rua Dois", 2022),
        cut("henrik-winterhafen", "Henrik Dahl", "Winterhafen", "Grauer Samt", 2018),
        cut("youth-call", "Youth Station", "Call Sign", "Antenna", 2025),
        cut("mara-treppe", "Mara Elling", "Treppenhaus", "Halbe Etage", 2024),
        cut("goldkind-laternen", "Goldkind", "Laternen", "Funken", 2021),
        cut("ivo-gleis", "Ivo Berg", "Gleis 4", "Nahverkehr", 2023),
        cut("sera-alarm", "Sera Quinn", "Soft Alarm", "Morning Copy", 2022),
        cut("archive-nebel", "Tonarchiv Lumen", "Nebelhorn", "Werkstatt", 2016),
        cut("archive-deich", "Tonarchiv Lumen", "Deichweg", "Werkstatt", 2017),
    )

    private val durations = intArrayOf(
        186, 204, 228, 198, 176, 212, 194, 221, 183, 207, 234, 169, 201, 188, 216, 192,
    )

    fun playout(): PlayoutPort = ScriptedPlayout(
        slots = tracks.take(durations.size).mapIndexed { index, track ->
            ScriptedPlayout.Slot(
                track = Track(track.id, track.artist, track.title, track.album),
                durationSec = durations[index],
            )
        },
    )

    fun catalog(): CatalogPort = StationCatalog(tracks)

    private fun cut(id: String, artist: String, title: String, album: String, year: Int) =
        CatalogTrack(id, artist, title, album, year)
}

object Vocabulary {
    val playlistTags = listOf(
        "Stimmig",
        "Zu gleichförmig",
        "Mutige Mischung",
        "Zu vorhersehbar",
        "Lokalbezug fehlt",
    )

    val hostTags = listOf(
        "Nahbar",
        "Gute Überleitungen",
        "Zu viel Rede",
        "Angenehme Stimme",
        "Unruhig",
    )
}
