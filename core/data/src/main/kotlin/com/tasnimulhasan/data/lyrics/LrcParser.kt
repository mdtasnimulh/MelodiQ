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
    private val OFFSET_REGEX = Regex("""^\[offset:\s*([+-]?\d+)\s*]$""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): List<LyricLine> {
        val rawLines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val synced = mutableListOf<LyricLine>()
        var sawAnyTimestamp = false

        // LRC's [offset:+/-ms] tag shifts every timestamp: a positive value means the lyrics
        // should appear that much SOONER, so it's subtracted. It used to be skipped as plain
        // metadata, which left any file that relies on it consistently early/late.
        val offsetMs = rawLines.firstNotNullOfOrNull { OFFSET_REGEX.find(it)?.groupValues?.get(1)?.toLongOrNull() } ?: 0L

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
            // An empty timestamped line is a deliberate marker (instrumental break / end of
            // the previous line). Dropping it - as this used to - left the previous lyric
            // highlighted straight through the break; kept as blank text, the UI can move the
            // highlight off it and show a music-note interlude instead.
            val text = line.substring(matches.last().range.last + 1).trim()

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
                val timestampMs = ((minutes * 60_000L) + (seconds * 1_000L) + fractionMs - offsetMs).coerceAtLeast(0L)
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

    /** Synced if most lines carry a timestamp. This used to demand EVERY line have one, so a
     * single stray credit line in an otherwise timed file turned off highlighting entirely. */
    fun isSynced(lines: List<LyricLine>): Boolean =
        lines.isNotEmpty() && lines.count { it.timestampMs != null } * 2 >= lines.size
}
