package com.example.coolingoffjar.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coolingoffjar.CoolingOffJarApp
import com.example.coolingoffjar.data.repo.CoolingOffRepository
import com.example.coolingoffjar.domain.Jar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShelfViewModel(private val repository: CoolingOffRepository) : ViewModel() {

    /** Completed jars, newest first. Null until the first load, so the empty message does not flash. */
    val jars: StateFlow<List<Jar>?> = repository.completedJars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun useFreebie(jarId: Long) {
        viewModelScope.launch { repository.useFreebie(jarId) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CoolingOffJarApp
                ShelfViewModel(app.container.repository)
            }
        }
    }
}
