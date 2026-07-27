package com.ava.proto.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ava.proto.data.AppDatabase
import com.ava.proto.data.EventEntity
import com.ava.proto.data.SessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val events: List<EventEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
)

class HomeViewModel(database: AppDatabase) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        database.eventDao().observeRecent(),
        database.sessionDao().observeRecent(),
    ) { events, sessions ->
        HomeUiState(events = events, sessions = sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(database) as T
    }
}
