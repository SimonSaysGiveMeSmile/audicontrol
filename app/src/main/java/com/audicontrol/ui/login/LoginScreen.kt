package com.audicontrol.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audicontrol.auth.AuthState
import com.audicontrol.theme.*

@Composable
fun LoginScreen(
    authState: AuthState,
    onSignIn: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AudiBlack)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "AUDI",
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 16.sp,
            color = AudiWhite
        )
        Text(
            "CONTROL",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 8.sp,
            color = AudiGreyLight
        )

        Spacer(Modifier.height(56.dp))

        when (authState) {
            is AuthState.Loading -> {
                CircularProgressIndicator(color = AudiRed)
                Spacer(Modifier.height(16.dp))
                Text("Authenticating...", color = AudiGreyLight)
            }
            is AuthState.Error -> {
                Text(
                    authState.message,
                    color = AudiRed,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                SignInButton(onSignIn)
            }
            else -> {
                SignInButton(onSignIn)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Not affiliated with Audi AG or Volkswagen Group.\nRequires active Audi connect PLUS subscription.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = AudiGreyMid
        )
    }
}

@Composable
private fun SignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AudiRed),
        shape = MaterialTheme.shapes.small
    ) {
        Text("SIGN IN WITH MYAUDI", style = MaterialTheme.typography.labelLarge)
    }
}
