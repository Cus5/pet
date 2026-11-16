package com.petfather.gyeonsimi2.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petfather.gyeonsimi2.AppContainer
import com.petfather.gyeonsimi2.GyeonsimiApplication

/** AppContainer 를 받아 ViewModel 을 만드는 도우미 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as GyeonsimiApplication
    return viewModel(key = key) { create(app.container) }
}
