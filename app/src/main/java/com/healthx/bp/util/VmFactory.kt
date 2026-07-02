package com.healthx.bp.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Tiny helper so screens can build ViewModels from the app graph without Hilt. */
inline fun <reified T : ViewModel> appViewModel(
    crossinline create: () -> T
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create() }
}
