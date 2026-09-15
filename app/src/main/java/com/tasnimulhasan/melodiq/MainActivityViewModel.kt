package com.tasnimulhasan.melodiq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.datastore.GetThemeConfigUseCase
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    getThemeConfigUseCase: GetThemeConfigUseCase,
) : ViewModel() {

    val themeConfig: StateFlow<DarkThemeConfig> = getThemeConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DarkThemeConfig.FOLLOW_SYSTEM)
}
