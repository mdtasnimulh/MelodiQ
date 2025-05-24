package com.tasnimulhasan.albums

data class AudioEffects(
    var selectedEffectType: Int = 0,
    var gainValues: ArrayList<Double>
)