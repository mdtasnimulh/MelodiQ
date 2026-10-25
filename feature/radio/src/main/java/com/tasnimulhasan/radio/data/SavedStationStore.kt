package com.tasnimulhasan.radio.data

import android.content.Context
import com.tasnimulhasan.radio.domain.RadioStation
import com.tasnimulhasan.radio.domain.StationGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The stations the user saved from the "Your own stream" box, kept in SharedPreferences (the same
 * lightweight storage the audio book voice settings use). One line per station:
 * `GROUP <tab> name <tab> url`.
 */
@Singleton
class SavedStationStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("radio_saved_stations", Context.MODE_PRIVATE)

    private val _stations = MutableStateFlow(load())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    /** Adds [station], replacing any saved station with the same address. */
    @Synchronized
    fun save(station: RadioStation) {
        update(_stations.value.filterNot { it.url == station.url } + station)
    }

    @Synchronized
    fun remove(url: String) {
        update(_stations.value.filterNot { it.url == url })
    }

    private fun update(list: List<RadioStation>) {
        _stations.value = list
        prefs.edit().putString(KEY, list.joinToString("\n") { "${it.group.name}\t${clean(it.name)}\t${clean(it.url)}" }).apply()
    }

    private fun load(): List<RadioStation> =
        prefs.getString(KEY, "").orEmpty().lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size != 3 || parts[2].isBlank()) return@mapNotNull null
            RadioStation(
                name = parts[1],
                genre = "Saved by you",
                url = parts[2],
                group = StationGroup.entries.firstOrNull { it.name == parts[0] } ?: StationGroup.OTHERS,
            )
        }

    private fun clean(text: String) = text.replace('\t', ' ').replace('\n', ' ').trim()

    private companion object {
        const val KEY = "stations"
    }
}
