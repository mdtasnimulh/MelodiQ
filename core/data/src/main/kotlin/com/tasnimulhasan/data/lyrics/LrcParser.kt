package com.tasnimulhasan.data.lyrics

import com.tasnimulhasan.entity.lyrics.LyricLine

/**
 * Parses the LRC lyrics format: lines like `[01:23.45]Some lyric text`, optionally preceded
 * by metadata tags like `[ar:Artist]`/`[ti:Title]`/`[al:Album]`/`[offset:...]` which are
 * skipped. A line can carry more than one timestamp (`[00:12.00][00:45.00]Chorus`), which
 * LRC uses to repeat a line at multiple points - each becomes its own [LyricLine].
 *
 * Content with no recognizable timestamps at all is treated as unsynchronized plain lyrics
 * (every [LyricLine.timestampMs] is null) rather than failing to parse.
 */
object LrcParser {

    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val PURE_METADATA_LINE_REGEX = Regex("""^\[[a-zA-Z]+:[^]]*]$""")

    fun parse(raw: String): List<LyricLine> {
        val rawLines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val synced = mutableListOf<LyricLine>()
        var sawAnyTimestamp = false

        for (line in rawLines) {
            // A pure metadata tag like [ar:Some Artist] or [offset:100] - not a lyric line.
            if (PURE_METADATA_LINE_REGEX.matches(line)) continue

            val matches = TIMESTAMP_REGEX.findAll(line).toList()
            if (matches.isEmpty()) {
                if (line.isNotBlank() && !line.startsWith("[")) {
                    // A stray non-timestamped line mixed into an otherwise synced file -
                    // keep it, timestamp null, rather than dropping content on the floor.
                    synced.add(LyricLine(timestampMs = null, text = line))
                }
                continue
            }

            sawAnyTimestamp = true
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) continue

            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fraction = match.groupValues[3]
                val fractionMs = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.take(3).toLong()
                }
                val timestampMs = (minutes * 60_000L) + (seconds * 1_000L) + fractionMs
                synced.add(LyricLine(timestampMs = timestampMs, text = text))
            }
        }

        if (!sawAnyTimestamp) {
            // No timestamps anywhere in the file - this is plain, unsynchronized lyrics.
            return rawLines.filterNot { PURE_METADATA_LINE_REGEX.matches(it) }
                .map { LyricLine(timestampMs = null, text = it) }
        }

        return synced.sortedWith(compareBy(nullsLast()) { it.timestampMs })
    }

    fun isSynced(lines: List<LyricLine>): Boolean = lines.isNotEmpty() && lines.all { it.timestampMs != null }
}
