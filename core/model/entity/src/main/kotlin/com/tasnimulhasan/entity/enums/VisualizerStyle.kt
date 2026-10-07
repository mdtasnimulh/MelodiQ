package com.tasnimulhasan.entity.enums

/** How (or whether) the live audio visualizer renders. OFF needs no RECORD_AUDIO permission
 * and no capture - it's the default so nothing changes for anyone who doesn't opt in. */
enum class VisualizerStyle {
    OFF,
    BARS,
    WAVEFORM,
    CIRCULAR,
}
