package com.nafaq.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.nafaq.expenses.NafaqApp
import com.nafaq.expenses.ui.home.HomeViewModel
import com.nafaq.expenses.ui.search.SearchViewModel
import com.nafaq.expenses.ui.settings.SettingsViewModel
import com.nafaq.expenses.ui.stats.StatsViewModel

object NafaqViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NafaqApp
        val container = application.container
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container)
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(container)
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(container)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
            else -> throw IllegalArgumentException("ViewModel غير معروف: ${modelClass.name}")
        } as T
    }
}
