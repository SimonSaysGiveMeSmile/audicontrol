package com.audicontrol.ui.vinlookup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.audicontrol.data.VehicleImageService
import com.audicontrol.data.VinDecodeResult
import com.audicontrol.theme.*

@Composable
fun VinLookupScreen(
    viewModel: VinLookupViewModel,
    onScanVin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .okHttpClient(VehicleImageService.createAuthenticatedClient())
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AudiBlack)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("VIN LOOKUP", style = MaterialTheme.typography.labelLarge, color = AudiGreyLight)
        Text(
            "Decode any vehicle",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.vinInput,
                onValueChange = { viewModel.updateVin(it) },
                modifier = Modifier.weight(1f),
                label = { Text("Enter VIN") },
                placeholder = { Text("17 characters") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { viewModel.decode() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AudiRed,
                    cursorColor = AudiRed,
                    focusedLabelColor = AudiRed
                )
            )
            IconButton(
                onClick = onScanVin,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan VIN", tint = AudiRed)
            }
        }

        Button(
            onClick = { viewModel.decode() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = state.vinInput.length == 17 && !state.loading,
            colors = ButtonDefaults.buttonColors(containerColor = AudiRed),
            shape = MaterialTheme.shapes.small
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AudiWhite
                )
            } else {
                Text("DECODE", style = MaterialTheme.typography.labelLarge)
            }
        }

        state.error?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = AudiRed)
        }

        state.result?.let { result ->
            HorizontalDivider(color = AudiDivider)
            VinResultCard(result, imageLoader, context)

            Button(
                onClick = { viewModel.saveAsMyVehicle() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.saved) AudiGreyDark else AudiRed
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    if (state.saved) Icons.Default.Check else Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.saved) "SAVED AS MY VEHICLE" else "SET AS MY VEHICLE",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun VinResultCard(
    result: VinDecodeResult,
    imageLoader: ImageLoader,
    context: android.content.Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (result.photos.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(result.photos.first())
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "${result.year} ${result.make} ${result.model}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        val title = listOfNotNull(
            result.year?.toString(),
            result.make,
            result.model
        ).joinToString(" ")

        if (title.isNotBlank()) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Light
            )
        }

        result.trim?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AudiGreyLight)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(AudiCardSurface, MaterialTheme.shapes.small)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("VEHICLE INFO", style = MaterialTheme.typography.labelSmall, color = AudiGreyLight)
            result.engine?.let { InfoRow("Engine", it) }
            result.bodyStyle?.let { InfoRow("Body", it) }
            result.transmission?.let { InfoRow("Transmission", it) }
            result.exteriorColor?.let { InfoRow("Exterior", it) }
            result.interiorColor?.let { InfoRow("Interior", it) }
        }

        if (result.msrp != null || result.marketPrice != null || result.mileage != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AudiCardSurface, MaterialTheme.shapes.small)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("MARKET DATA", style = MaterialTheme.typography.labelSmall, color = AudiGreyLight)
                result.msrp?.let { InfoRow("MSRP", "$%,d".format(it)) }
                result.marketPrice?.let { InfoRow("Market Price", "$%,d".format(it)) }
                result.mileage?.let { InfoRow("Mileage", "%,d mi".format(it)) }
                result.dealer?.let { InfoRow("Dealer", it) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AudiGreyLight)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AudiWhite)
    }
}
