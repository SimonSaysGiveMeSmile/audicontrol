package com.audicontrol.ui.vinlookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audicontrol.data.UserPreferences
import com.audicontrol.data.VinDecodeResult
import com.audicontrol.data.VinLookupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VinLookupUiState(
    val vinInput: String = "",
    val loading: Boolean = false,
    val result: VinDecodeResult? = null,
    val error: String? = null,
    val saved: Boolean = false
)

class VinLookupViewModel(private val preferences: UserPreferences) : ViewModel() {

    private val _state = MutableStateFlow(VinLookupUiState())
    val state: StateFlow<VinLookupUiState> = _state

    fun updateVin(vin: String) {
        val filtered = vin.uppercase().filter { it.isLetterOrDigit() }.take(17)
        _state.value = _state.value.copy(vinInput = filtered, error = null, saved = false)
    }

    fun setVinFromScanner(vin: String) {
        updateVin(vin)
        decode()
    }

    fun decode() {
        val vin = _state.value.vinInput
        if (vin.length != 17) {
            _state.value = _state.value.copy(error = "VIN must be exactly 17 characters")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null, result = null)
        viewModelScope.launch {
            val result = VinLookupService.decodeVin(vin)
            _state.value = if (result != null) {
                _state.value.copy(loading = false, result = result)
            } else {
                _state.value.copy(loading = false, error = "Could not decode VIN. Check the number and try again.")
            }
        }
    }

    fun saveAsMyVehicle() {
        val result = _state.value.result ?: return
        preferences.saveVehicle(result.vin, result.make, result.model, result.year)
        _state.value = _state.value.copy(saved = true)
    }
}
