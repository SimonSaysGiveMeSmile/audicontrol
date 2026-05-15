package com.audicontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.audicontrol.data.MockVehicleBackend
import com.audicontrol.data.MyAudiBackend
import com.audicontrol.data.VehicleBackend
import com.audicontrol.obd.ConnectionManager
import com.audicontrol.obd.LiveDataStream
import com.audicontrol.theme.AudiControlTheme
import com.audicontrol.ui.about.AboutScreen
import com.audicontrol.ui.actions.ActionsScreen
import com.audicontrol.ui.dashboard.DashboardScreen
import com.audicontrol.ui.dashboard.DashboardViewModel
import com.audicontrol.ui.dashboard.LiveDashboardScreen
import com.audicontrol.ui.login.LoginScreen
import com.audicontrol.ui.setup.ConnectionMode
import com.audicontrol.ui.setup.SetupScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var connectionManager: ConnectionManager

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
        enableEdgeToEdge()
        setContent {
            AudiControlTheme {
                AudiControlApp(
                    authManager = authManager,
                    connectionManager = connectionManager,
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

@Composable
fun AudiControlApp(
    authManager: AuthManager,
    connectionManager: ConnectionManager,
    onSignIn: () -> Unit
) {
    if (BuildConfig.USE_MOCK) {
        MockApp(connectionManager)
    } else {
        RealApp(authManager, connectionManager, onSignIn)
    }
}

@Composable
private fun RealApp(authManager: AuthManager, connectionManager: ConnectionManager, onSignIn: () -> Unit) {
    val authState by authManager.authState.collectAsState()

    when (authState) {
        is AuthState.LoggedIn -> {
            val backend = remember { MyAudiBackend(authManager) }
            MainScaffold(backend, connectionManager, onLogout = { authManager.logout() })
        }
        else -> {
            LoginScreen(authState = authState, onSignIn = onSignIn)
        }
    }
}

@Composable
private fun MockApp(connectionManager: ConnectionManager) {
    var connectionMode by remember { mutableStateOf<ConnectionMode?>(null) }

    when (connectionMode) {
        null -> {
            SetupScreen(
                connectionManager = connectionManager,
                onCloudSelected = { connectionMode = ConnectionMode.CLOUD },
                onOBDConnected = { connectionMode = ConnectionMode.BLUETOOTH_OBD }
            )
        }
        ConnectionMode.CLOUD -> {
            val backend = remember { MockVehicleBackend() }
            MainScaffold(backend, connectionManager, onLogout = { connectionMode = null })
        }
        ConnectionMode.BLUETOOTH_OBD -> {
            val liveDataStream = remember { LiveDataStream(connectionManager.obdConnection) }
            OBDScaffold(liveDataStream, connectionManager, onDisconnect = { connectionMode = null })
        }
    }
}

@Composable
private fun OBDScaffold(
    liveDataStream: LiveDataStream,
    connectionManager: ConnectionManager,
    onDisconnect: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Speed, null) },
                    label = { Text("Live") },
                    selected = currentRoute == "live",
                    onClick = { navController.navigate("live") { launchSingleTop = true } }
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
            startDestination = "live",
            modifier = Modifier.padding(padding)
        ) {
            composable("live") { LiveDashboardScreen(liveDataStream) }
            composable("about") {
                AboutScreen(onLogout = {
                    scope.launch {
                        connectionManager.disconnect()
                        onDisconnect()
                    }
                })
            }
        }
    }
}

@Composable
private fun MainScaffold(
    backend: VehicleBackend,
    connectionManager: ConnectionManager,
    onLogout: (() -> Unit)?
) {
    val navController = rememberNavController()

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
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = { navController.navigate("dashboard") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ControlCamera, null) },
                    label = { Text("Controls") },
                    selected = currentRoute == "actions",
                    onClick = { navController.navigate("actions") { launchSingleTop = true } }
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
                val viewModel = remember { DashboardViewModel(backend) }
                DashboardScreen(viewModel)
            }
            composable("actions") {
                val viewModel = remember { DashboardViewModel(backend) }
                ActionsScreen(viewModel, backend.capabilities)
            }
            composable("about") { AboutScreen(onLogout = onLogout) }
        }
    }
}
