package com.tasnimulhasan.melodiq.component

enum class CustomDrawerState {
    OPENED,
    CLOSED
}

fun CustomDrawerState.isOpened(): Boolean {
    return this.name == "Opened"
}

fun CustomDrawerState.opposite(): CustomDrawerState {
    return if (this == CustomDrawerState.OPENED) CustomDrawerState.CLOSED
    else CustomDrawerState.OPENED
}