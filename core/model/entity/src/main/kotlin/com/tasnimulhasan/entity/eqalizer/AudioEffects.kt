package com.tasnimulhasan.entity.eqalizer

import com.google.gson.annotations.SerializedName

data class AudioEffects(
    @SerializedName("selectedEffectType") val selectedEffectType: Int,
    @SerializedName("gainValues") val gainValues: List<Double>
)