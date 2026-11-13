package com.example.dwellping.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.dwellping.PermState
import com.example.dwellping.PermStep
import com.example.dwellping.PermissionChecklist
import com.example.dwellping.Perms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val perms: PermState = PermState(location = false, backgroundLocation = false, sms = false),
    /** The permission step Android won't prompt for any more. Null = none. */
    val blockedAt: PermStep? = null,
) {
    /** What the permissions button does next. */
    val permStep get() = PermissionChecklist.nextStep(perms, blockedAt)
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val app get() = getApplication<Application>()

    private val _state = MutableStateFlow(SettingsUiState(Perms.state(app)))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Re-read permissions. Call on resume and after a permission result. */
    fun refresh() = _state.update { it.copy(perms = Perms.state(app)) }

    /** Call when a permission request returns. [canAskAgain]: see [PermissionChecklist.blockedAfter]. */
    fun onPermissionResult(requested: PermStep, canAskAgain: Boolean) {
        refresh()
        _state.update { it.copy(blockedAt = PermissionChecklist.blockedAfter(requested, it.perms, canAskAgain)) }
    }
}
