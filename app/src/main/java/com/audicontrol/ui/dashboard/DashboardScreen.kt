package com.audicontrol.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.audicontrol.BuildConfig
import com.audicontrol.data.VehicleImageService
import com.audicontrol.theme.*
import com.audicontrol.ui.components.RingGauge
import com.audicontrol.ui.components.VehicleSilhouette

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AudiBlack)
            .verticalScroll(rememberScrollState())
    ) {
        if (BuildConfig.USE_MOCK) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(AudiRedDim)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("MOCK MODE", style = MaterialTheme.typography.labelSmall, color = AudiWhite)
            }
        }

        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.vehicle?.nickname ?: state.vehicle?.model ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = state.vehicle?.let { "${it.year} ${it.model}" } ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (state.loading) {
                CircularProgressIndicator(color = AudiRed, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { viewModel.load() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AudiGreyLight)
                }
            }
        }

        HorizontalDivider(color = AudiDivider)

        // Vehicle image / silhouette
        state.status?.let { status ->
            val vehicle = state.vehicle
            if (vehicle != null) {
                val context = LocalContext.current
                var imageUrl by remember(vehicle) { mutableStateOf<String?>(null) }

                LaunchedEffect(vehicle.vin) {
                    val apiPhoto = VehicleImageService.getPhotoFromApi(vehicle.vin)
                    imageUrl = apiPhoto ?: VehicleImageService.getImageUrl(vehicle.model)
                }

                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .okHttpClient(VehicleImageService.createAuthenticatedClient())
                        .build()
                }

                imageUrl?.let { url ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(url)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = "${vehicle.year} ${vehicle.model}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            } else {
                VehicleSilhouette(
                    locked = status.locked,
                    doorsOpen = status.doorsOpen,
                    windowsOpen = status.windowsOpen,
                    actionInProgress = state.actionInProgress,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        HorizontalDivider(color = AudiDivider)

        // Ring gauges
        state.status?.let { status ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RingGauge(
                    value = status.fuelLevelPercent.toFloat(),
                    maxValue = 100f,
                    label = "FUEL",
                    displayValue = "${status.fuelLevelPercent}%"
                )
                RingGauge(
                    value = status.rangeKm.toFloat(),
                    maxValue = 800f,
                    label = "RANGE",
                    displayValue = "${status.rangeKm} km"
                )
            }
        }

        // Detail rows
        state.status?.let { status ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                DetailRow("ODOMETER", "%,d km".format(status.odometerKm))
                DetailRow("LAST UPDATED", viewModel.relativeTime(status.lastUpdated))
                if (status.latitude != null && status.longitude != null) {
                    DetailRow("LOCATION", "%.4f, %.4f".format(status.latitude, status.longitude))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        state.message?.let { msg ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") } }
            ) { Text(msg) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AudiCardSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AudiWhite)
    }
}
