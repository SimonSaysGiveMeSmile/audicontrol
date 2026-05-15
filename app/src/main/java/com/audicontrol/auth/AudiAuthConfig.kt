package com.audicontrol.auth

import android.net.Uri

object AudiAuthConfig {
    const val CLIENT_ID = "09b6cbec-cd19-4589-82fd-363dfa8c24da"
    const val REDIRECT_URI = "com.audicontrol://callback"
    const val SCOPE = "openid profile address email birthdate nickname phone"

    val AUTHORIZATION_ENDPOINT: Uri = Uri.parse("https://identity.vwgroup.io/oidc/v1/authorize")
    val TOKEN_ENDPOINT: Uri = Uri.parse("https://identity.vwgroup.io/oidc/v1/token")

    const val MBB_TOKEN_URL = "https://mbboauth-1d.prd.ece.vwg-connect.com/mbbcoauth/mobile/oauth2/v1/token"
    const val MBB_CLIENT_ID = "77869e21-e30a-4a92-b016-48ab7d3db1d8"
}
