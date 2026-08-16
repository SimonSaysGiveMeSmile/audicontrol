package com.audicontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.audicontrol.auth.AuthManager
import com.audicontrol.auth.AuthState
import com.audicontrol.data.*
import com.audicontrol.obd.ConnectionManager
import com.audicontrol.obd.LiveDataStream
import com.audicontrol.theme.AudiControlTheme
import com.audicontrol.ui.about.AboutScreen
import com.audicontrol.ui.actions.ActionsScreen
import com.audicontrol.ui.dashboard.DashboardScreen
import com.audicontrol.ui.dashboard.DashboardViewModel
import com.audicontrol.ui.dashboard.LiveDashboardScreen
import com.audicontrol.ui.login.LoginScreen
import com.audicontrol.ui.setup.SetupScreen
import com.audicontrol.ui.vinlookup.VinLookupScreen
import com.audicontrol.ui.vinlookup.VinLookupViewModel
import com.audicontrol.ui.vinlookup.VinScannerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var connectionManager: ConnectionManager
    private lateinit var preferences: UserPreferences

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        lifecycleScope.launch {
            authManager.handleAuthResponse(data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        connectionManager = ConnectionManager(this)
        preferences = UserPreferences(this)
        enableEdgeToEdge()
        setContent {
            AudiControlTheme {
                AudiControlApp(
                    authManager = authManager,
                    connectionManager = connectionManager,
                    preferences = preferences,
                    onSignIn = { authLauncher.launch(authManager.buildAuthIntent()) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authManager.dispose()
    }
}
// PLACEHOLDER_COMPOSABLES

@Composable
fun AudiControlApp(
    authManager: AuthManager,
    connectionManager: ConnectionManager,
    preferences: UserPreferences,
    onSignIn: () -> Unit
) {
    val authState by authManager.authState.collectAsState()
    var setupComplete by remember { mutableStateOf(preferences.setupCompleted) }

    if (!setupComplete) {
        SetupFlow(
            connectionManager = connectionManager,
            preferences = preferences,
            onComplete = {
                preferences.setupCompleted = true
                setupComplete = true
            },
            authManager = authManager,
            onSignIn = onSignIn
        )
    } else {
        when (authState) {
            is AuthState.LoggedIn -> {
                val backend = remember { MyAudiBackend(authManager) }
                MainScaffold(
                    backend = backend,
                    preferences = preferences,
                    onLogout = {
                        authManager.logout()
                        preferences.clear()
                        setupComplete = false
                    }
                )
            }
            else -> {
                MainScaffold(
                    backend = null,
                    preferences = preferences,
                    onLogout = {
                        preferences.clear()
                        setupComplete = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SetupFlow(
    connectionManager: ConnectionManager,
    preferences: UserPreferences,
    onComplete: () -> Unit,
    authManager: AuthManager,
    onSignIn: () -> Unit
) {
    var showScanner by remember { mutableStateOf(false) }
    val vinLookupViewModel = remember { VinLookupViewModel(preferences) }

    if (showScanner) {
        VinScannerScreen(
            onVinDetected = { vin ->
                vinLookupViewModel.setVinFromScanner(vin)
                showScanner = false
            },
            onClose = { showScanner = false }
        )
    } else {
        SetupScreen(
            connectionManager = connectionManager,
            preferences = preferences,
            vinLookupViewModel = vinLookupViewModel,
            onScanVin = { showScanner = true },
            onCloudSelected = {
                onSignIn()
                onComplete()
            },
            onOBDConnected = { onComplete() },
            onSkipConnection = { onComplete() }
        )
    }
}

@Composable
private fun MainScaffold(
    backend: VehicleBackend?,
    preferences: UserPreferences,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var showScanner by remember { mutableStateOf(false) }
    val vinLookupViewModel = remember { VinLookupViewModel(preferences) }

    if (showScanner) {
        VinScannerScreen(
            onVinDetected = { vin ->
                vinLookupViewModel.setVinFromScanner(vin)
                showScanner = false
            },
            onClose = { showScanner = false }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, null) },
                    label = { Text("Home") },
                    selected = currentRoute == "dashboard",
                    onClick = { navController.navigate("dashboard") { launchSingleTop = true } }
                )
                if (backend != null) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ControlCamera, null) },
                        label = { Text("Controls") },
                        selected = currentRoute == "actions",
                        onClick = { navController.navigate("actions") { launchSingleTop = true } }
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Lookup") },
                    selected = currentRoute == "lookup",
                    onClick = { navController.navigate("lookup") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, null) },
                    label = { Text("About") },
                    selected = currentRoute == "about",
                    onClick = { navController.navigate("about") { launchSingleTop = true } }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                if (backend != null) {
                    val viewModel = remember { DashboardViewModel(backend) }
                    DashboardScreen(viewModel)
                } else {
                    HomePlaceholderScreen(preferences)
                }
            }
            if (backend != null) {
                composable("actions") {
                    val viewModel = remember { DashboardViewModel(backend) }
                    ActionsScreen(viewModel, backend.capabilities)
                }
            }
            composable("lookup") {
                VinLookupScreen(
                    viewModel = vinLookupViewModel,
                    onScanVin = { showScanner = true }
                )
            }
            composable("about") { AboutScreen(onLogout = onLogout) }
        }
    }
}

@Composable
private fun HomePlaceholderScreen(preferences: UserPreferences) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "AUDICONTROL",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (preferences.hasVehicle()) {
            Text(
                listOfNotNull(
                    preferences.savedYear.takeIf { it > 0 }?.toString(),
                    preferences.savedMake,
                    preferences.savedModel
                ).joinToString(" ").ifEmpty { "My Vehicle" },
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "VIN: ${preferences.savedVin}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Welcome",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Use the Lookup tab to decode any VIN, or sign in with myAudi for remote vehicle access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
