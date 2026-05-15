package com.audicontrol.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audicontrol.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val vehicle: Vehicle? = null,
    val status: VehicleStatus? = null,
    val loading: Boolean = true,
    val actionInProgress: Boolean = false,
    val message: String? = null
)

class DashboardViewModel(private val backend: VehicleBackend) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val vehicles = backend.getVehicles()
            if (vehicles is VehicleResult.Success && vehicles.data.isNotEmpty()) {
                val v = vehicles.data.first()
                val status = backend.getStatus(v.vin)
                _state.value = DashboardUiState(
                    vehicle = v,
                    status = (status as? VehicleResult.Success)?.data,
                    loading = false
                )
            } else {
                _state.value = DashboardUiState(loading = false, message = "Failed to load vehicle")
            }
        }
    }

    fun lock() = vehicleAction { backend.lock(it) }
    fun unlock() = vehicleAction { backend.unlock(it) }
    fun honkAndFlash() = vehicleAction { backend.honkAndFlash(it) }

    fun sendDestination(lat: Double, lon: Double, name: String) = vehicleAction {
        backend.sendDestination(it, lat, lon, name)
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    fun relativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    private fun vehicleAction(action: suspend (String) -> VehicleResult<Unit>) {
        val vin = _state.value.vehicle?.vin ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true, message = null)
            val result = action(vin)
            val msg = if (result is VehicleResult.Error) result.message else null
            val status = backend.getStatus(vin)
            _state.value = _state.value.copy(
                status = (status as? VehicleResult.Success)?.data ?: _state.value.status,
                actionInProgress = false,
                message = msg
            )
        }
    }
}
