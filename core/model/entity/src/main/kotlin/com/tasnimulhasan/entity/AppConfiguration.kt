package com.tasnimulhasan.entity

import com.tasnimulhasan.entity.eqalizer.AudioEffects

data class AppConfiguration(
    val audioEffects: AudioEffects,
    val enableEqualizer: Boolean
)