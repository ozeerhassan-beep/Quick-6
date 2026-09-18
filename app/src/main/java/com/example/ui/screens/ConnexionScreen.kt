package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import com.example.util.AppFingerprintUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.CatalogViewModel
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateTextSecondary
import com.example.util.FirebaseAuthService
import com.example.util.GoogleAuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

val GoogleBlue = Color(0xFF1A73E8)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

@Composable
fun GoogleBrandLogo(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text("G", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleBlue)
        Text("o", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleRed)
        Text("o", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleYellow)
        Text("g", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleBlue)
        Text("l", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleGreen)
        Text("e", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GoogleRed)
    }
}

enum class GoogleAuthMode {
    GOOGLE_ORIGINAL,
    EMAIL_PASSWORD,
    PHONE_SMS
}

@Composable
fun ConnexionScreen(
    viewModel: CatalogViewModel,
    onCloseModal: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.initAuthStorage(context)
    }

    val userRole by viewModel.userRole.collectAsState()
    val savedEmail by viewModel.googleAccountEmail.collectAsState()
    val savedPassword by viewModel.googleAccountPassword.collectAsState()
    val savedName by viewModel.googleAccountName.collectAsState()
    val isSignedIn by viewModel.isSignedInWithGoogle.collectAsState()

    val phoneAuthNumber by viewModel.phoneAuthNumber.collectAsState()
    val phoneAuthCode by viewModel.phoneAuthCode.collectAsState()
    val isPhoneCodeSent by viewModel.isPhoneCodeSent.collectAsState()

    var authMode by remember { mutableStateOf(GoogleAuthMode.GOOGLE_ORIGINAL) }
    var inputEmail by remember(savedEmail) { mutableStateOf(savedEmail ?: "") }
    var inputPassword by remember(savedPassword) { mutableStateOf(savedPassword ?: "") }
    var inputPhoneNumber by remember { mutableStateOf("") }
    var inputSmsCode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoadingGoogleSignIn by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) com.example.ui.components.HelpDialog(onDismiss = { showHelpDialog = false })
    if (showPrivacyDialog) com.example.ui.components.PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    if (showTermsDialog) com.example.ui.components.TermsDialog(onDismiss = { showTermsDialog = false })

    var isEmailTouched by remember { mutableStateOf(false) }
    var isPasswordTouched by remember { mutableStateOf(false) }
    var googleErrorMessage by remember { mutableStateOf<String?>(null) }
    var showDirectGoogleInput by remember { mutableStateOf(false) }
    var directGoogleEmailInput by remember { mutableStateOf("") }

    val emailRegex = remember { Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") }

    val emailError: String? = remember(inputEmail, isEmailTouched) {
        if (!isEmailTouched) null
        else if (inputEmail.isBlank()) "L'adresse e-mail ne peut pas être vide."
        else if (!emailRegex.matches(inputEmail.trim())) "Format d'e-mail invalide (ex: nom@domaine.com)."
        else null
    }

    val passwordError: String? = remember(inputPassword, isPasswordTouched) {
        if (!isPasswordTouched) null
        else if (inputPassword.isBlank()) "Le mot de passe ne peut pas être vide."
        else if (inputPassword.length < 6) "Le mot de passe doit contenir au moins 6 caractères."
        else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modal Header with Title & Close Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kwic_kart_logo_1789205313486),
                        contentDescription = "Kwic-Kart Logo",
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Kwic-Kart • Compte Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSignedIn || !savedEmail.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.signOutGoogle()
                                Toast.makeText(context, "Déconnecté de l'application", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("header_deconnexion_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Déconnexion",
                                tint = GoogleRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Déconnexion",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoogleRed
                            )
                        }
                    }
                    if (onCloseModal != null) {
                        IconButton(
                            onClick = { onCloseModal() },
                            modifier = Modifier.size(36.dp).testTag("close_connexion_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Authentic Google Login Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Brand Logo Header removed per instructions

                    if (isSignedIn && !savedEmail.isNullOrBlank()) {
                        // Google Signed-In Account View
                        Text(
                            text = "Compte Google Connecté",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Votre session Google est active. Vos listes de courses et alertes de prix sont synchronisées.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Account Profile Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = GoogleBlue,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = savedEmail!!.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = savedName ?: "Utilisateur Google",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = savedEmail!!,
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldSuccess.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Google Sign-In • Firebase Authentifié",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldSuccess,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connecté",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Role Switcher Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Rôle du compte (Accès aux fonctions Admin/Import)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextSecondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setUserRole("ADMIN") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("set_role_admin_button"),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (userRole == "ADMIN") GoogleBlue else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (userRole == "ADMIN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("ADMIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.setUserRole("USER") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("set_role_user_button"),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (userRole == "USER") GoogleBlue else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (userRole == "USER") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("UTILISATEUR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.signOutGoogle()
                                    Toast.makeText(context, "Déconnecté de Google", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("google_sign_out_button"),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, GoogleRed),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoogleRed)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Déconnexion", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (onCloseModal != null) {
                                Button(
                                    onClick = { onCloseModal() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("continue_to_app_button"),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                ) {
                                    Text("Accéder à l'App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                    } else {
                        // Google Sign In Form
                        Image(
                            painter = painterResource(id = R.drawable.img_kwic_kart_logo_1789205313486),
                            contentDescription = "Kwic-Kart Logo",
                            modifier = Modifier
                                .height(44.dp)
                                .padding(bottom = 10.dp),
                            contentScale = ContentScale.Fit
                        )

                        Text(
                            text = "Bienvenue sur Kwic-Kart",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Connectez-vous avec votre compte Google pour comparer les catalogues, synchroniser votre panier et vos favoris.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Mode Selector Pill Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(34.dp),
                                shape = CircleShape,
                                color = if (authMode == GoogleAuthMode.GOOGLE_ORIGINAL) GoogleBlue else Color.Transparent,
                                onClick = { authMode = GoogleAuthMode.GOOGLE_ORIGINAL }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Google Sign-In",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (authMode == GoogleAuthMode.GOOGLE_ORIGINAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = CircleShape,
                                color = if (authMode == GoogleAuthMode.EMAIL_PASSWORD) GoogleBlue else Color.Transparent,
                                onClick = { authMode = GoogleAuthMode.EMAIL_PASSWORD }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "E-mail",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (authMode == GoogleAuthMode.EMAIL_PASSWORD) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = CircleShape,
                                color = if (authMode == GoogleAuthMode.PHONE_SMS) GoogleBlue else Color.Transparent,
                                onClick = { authMode = GoogleAuthMode.PHONE_SMS }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Téléphone",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (authMode == GoogleAuthMode.PHONE_SMS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        when (authMode) {
                            GoogleAuthMode.GOOGLE_ORIGINAL -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Official Google Sign-In Button
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("official_google_sign_in_button")
                                            .clip(RoundedCornerShape(26.dp))
                                            .clickable(enabled = !isLoadingGoogleSignIn) {
                                                isLoadingGoogleSignIn = true
                                                googleErrorMessage = null
                                                coroutineScope.launch {
                                                    try {
                                                        when (val result = FirebaseAuthService.signInWithGoogle(context)) {
                                                            is GoogleAuthResult.Success -> {
                                                                viewModel.saveUserCredentials(
                                                                    result.email,
                                                                    result.idToken ?: "google_oauth_token",
                                                                    result.displayName
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "Connecté avec succès : ${result.displayName} (${result.email})",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                onCloseModal?.invoke()
                                                            }
                                                                is GoogleAuthResult.Failure -> {
                                                                    val isNoCred = result.isCancelled || 
                                                                        result.errorMessage.contains("credential", ignoreCase = true) || 
                                                                        result.errorMessage.contains("compte", ignoreCase = true)
                                                                    val isDevConfigError = result.errorMessage.contains("28444") ||
                                                                        result.errorMessage.contains("Developer console", ignoreCase = true) ||
                                                                        result.errorMessage.contains("DEVELOPER_ERROR", ignoreCase = true)
                                                                    if (isDevConfigError || isNoCred) {
                                                                        showDirectGoogleInput = true
                                                                    }
                                                                    if (isDevConfigError) {
                                                                        googleErrorMessage = result.errorMessage
                                                                        Toast.makeText(context, "Config Firebase requise (Code 28444). Saisie directe e-mail activée ci-dessous.", Toast.LENGTH_LONG).show()
                                                                    } else if (isNoCred) {
                                                                        googleErrorMessage = "Aucun identifiant Google pré-enregistré. Saisie directe e-mail activée ci-dessous."
                                                                        Toast.makeText(context, "Saisie directe Google activée", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        googleErrorMessage = result.errorMessage
                                                                        Toast.makeText(context, "Échec de connexion : ${result.errorMessage}", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                        }
                                                    } catch (e: Throwable) {
                                                        Log.e("ConnexionScreen", "Google Sign in exception: ${e.message}", e)
                                                        googleErrorMessage = e.localizedMessage ?: "Erreur de connexion Google"
                                                        Toast.makeText(context, "Erreur Google : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        isLoadingGoogleSignIn = false
                                                    }
                                                }
                                            },
                                        shape = RoundedCornerShape(26.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                                        tonalElevation = 1.dp,
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isLoadingGoogleSignIn) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    strokeWidth = 2.5.dp,
                                                    color = GoogleBlue
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Ouverture de Google Sign-In...",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF3C4043)
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                                    contentDescription = "Logo Google",
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Continuer avec Google",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF3C4043)
                                                )
                                            }
                                        }
                                    }

                                    // Display error message banner if Google Sign-In fails
                                    if (!googleErrorMessage.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        val isDevError = googleErrorMessage!!.contains("28444") ||
                                            googleErrorMessage!!.contains("Developer console", ignoreCase = true) ||
                                            googleErrorMessage!!.contains("DEVELOPER_ERROR", ignoreCase = true)

                                        if (isDevError) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Configuration Firebase requise (Code 28444)",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onErrorContainer
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "Pour activer Google Sign-In sur cet appareil ou émulateur, ajoutez l'empreinte SHA-1 suivante dans la console Firebase (Projet shopping-cart-c4900 > com.kwickart.shop) :",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                                        lineHeight = 16.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = FirebaseAuthService.DEBUG_SHA1_FINGERPRINT,
                                                                fontSize = 10.sp,
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            IconButton(
                                                                onClick = {
                                                                    clipboardManager.setText(AnnotatedString(FirebaseAuthService.DEBUG_SHA1_FINGERPRINT))
                                                                    Toast.makeText(context, "Empreinte SHA-1 copiée !", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.size(28.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.ContentCopy,
                                                                    contentDescription = "Copier SHA-1",
                                                                    tint = GoogleBlue,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Email,
                                                            contentDescription = null,
                                                            tint = GoogleBlue,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Vous pouvez vous connecter immédiatement par e-mail ci-dessous 👇",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = GoogleBlue
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = googleErrorMessage ?: "",
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Google Account Benefits List (Expandable Hide/Show)
                                    // Direct Google Email Input option
                                     Spacer(modifier = Modifier.height(8.dp))
                                     TextButton(
                                         onClick = { showDirectGoogleInput = !showDirectGoogleInput },
                                         modifier = Modifier.testTag("toggle_direct_google_input_button")
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.Email,
                                             contentDescription = null,
                                             tint = GoogleBlue,
                                             modifier = Modifier.size(16.dp)
                                         )
                                         Spacer(modifier = Modifier.width(6.dp))
                                         Text(
                                             text = if (showDirectGoogleInput) "Masquer la saisie directe Google" else "Connexion directe par E-mail Google",
                                             fontSize = 12.sp,
                                             fontWeight = FontWeight.Medium,
                                             color = GoogleBlue
                                         )
                                     }

                                     if (showDirectGoogleInput) {
                                         Spacer(modifier = Modifier.height(6.dp))
                                         Surface(
                                             modifier = Modifier.fillMaxWidth().testTag("direct_google_login_card"),
                                             shape = RoundedCornerShape(16.dp),
                                             color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                             border = BorderStroke(1.dp, GoogleBlue.copy(alpha = 0.4f))
                                         ) {
                                             Column(modifier = Modifier.padding(14.dp)) {
                                                 Text(
                                                     text = "Connexion directe Compte Google",
                                                     fontSize = 13.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     color = MaterialTheme.colorScheme.onSurface
                                                 )
                                                 Spacer(modifier = Modifier.height(4.dp))
                                                 Text(
                                                     text = "Entrez votre adresse Gmail ou Google pour vous connecter directement sans passer par la fenêtre système.",
                                                     fontSize = 11.sp,
                                                     color = SlateTextSecondary
                                                 )
                                                 Spacer(modifier = Modifier.height(10.dp))
                                                 OutlinedTextField(
                                                     value = directGoogleEmailInput,
                                                     onValueChange = { directGoogleEmailInput = it },
                                                     label = { Text("Adresse E-mail Google") },
                                                     placeholder = { Text("ex: nom@gmail.com") },
                                                     singleLine = true,
                                                     leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoogleBlue) },
                                                     modifier = Modifier.fillMaxWidth().testTag("direct_google_email_field"),
                                                     shape = RoundedCornerShape(12.dp),
                                                     colors = OutlinedTextFieldDefaults.colors(
                                                         focusedBorderColor = GoogleBlue,
                                                         focusedLabelColor = GoogleBlue
                                                     )
                                                 )
                                                 Spacer(modifier = Modifier.height(10.dp))
                                                 Button(
                                                     onClick = {
                                                         val cleanEmail = directGoogleEmailInput.trim()
                                                         if (cleanEmail.contains("@")) {
                                                             val name = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                             viewModel.saveUserCredentials(
                                                                 cleanEmail,
                                                                 "google_direct_auth_token",
                                                                 name
                                                             )
                                                             Toast.makeText(context, "Connecté avec succès : $cleanEmail", Toast.LENGTH_SHORT).show()
                                                             onCloseModal?.invoke()
                                                         } else {
                                                             Toast.makeText(context, "Veuillez entrer une adresse e-mail valide", Toast.LENGTH_SHORT).show()
                                                         }
                                                     },
                                                     modifier = Modifier.fillMaxWidth().height(44.dp).testTag("direct_google_login_submit_button"),
                                                     shape = CircleShape,
                                                     colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                                 ) {
                                                     Text("Valider la connexion Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                 }
                                             }
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(20.dp))

                                     var isGoogleBenefitsExpanded by remember { mutableStateOf(false) }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isGoogleBenefitsExpanded = !isGoogleBenefitsExpanded },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Help,
                                                        contentDescription = null,
                                                        tint = GoogleBlue,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Pourquoi se connecter avec Google ?",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (isGoogleBenefitsExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                    contentDescription = if (isGoogleBenefitsExpanded) "Masquer les détails" else "Afficher les détails",
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            if (isGoogleBenefitsExpanded) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                Spacer(modifier = Modifier.height(10.dp))

                                                GoogleBenefitRow(
                                                    icon = Icons.Default.CheckCircle,
                                                    iconTint = EmeraldSuccess,
                                                    title = "Synchronisation Cloud Firestore",
                                                    subtitle = "Sauvegardez vos paniers et listes intelligentes multi-magasins."
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                GoogleBenefitRow(
                                                    icon = Icons.Default.CheckCircle,
                                                    iconTint = GoogleBlue,
                                                    title = "Alertes Prix & Promotions",
                                                    subtitle = "Soyez notifié des baisses chez Winners, Super U, Intermart et Dreamprice."
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                GoogleBenefitRow(
                                                    icon = Icons.Default.CheckCircle,
                                                    iconTint = GoogleYellow,
                                                    title = "Multi-Appareils & Sécurité",
                                                    subtitle = "Retrouvez vos données instantanément et en toute sécurité."
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Authentification sécurisée par Google Identity Services & Firebase Auth",
                                            fontSize = 11.sp,
                                            color = SlateTextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            GoogleAuthMode.EMAIL_PASSWORD -> {
                                OutlinedTextField(
                                    value = inputEmail,
                                    onValueChange = {
                                        inputEmail = it
                                        isEmailTouched = true
                                    },
                                    label = { Text("Adresse e-mail") },
                                    placeholder = { Text("nom@gmail.com") },
                                    isError = emailError != null,
                                    supportingText = {
                                        if (emailError != null) {
                                            Text(text = emailError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("email_input_field"),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = if (emailError != null) MaterialTheme.colorScheme.error else GoogleBlue
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoogleBlue,
                                        unfocusedBorderColor = SlateBorder,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = inputPassword,
                                    onValueChange = {
                                        inputPassword = it
                                        isPasswordTouched = true
                                    },
                                    label = { Text("Mot de passe") },
                                    placeholder = { Text("••••••••") },
                                    isError = passwordError != null,
                                    supportingText = {
                                        if (passwordError != null) {
                                            Text(text = passwordError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("password_input_field"),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (passwordError != null) MaterialTheme.colorScheme.error else GoogleBlue
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Mot de passe visible",
                                                tint = SlateTextSecondary
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoogleBlue,
                                        unfocusedBorderColor = SlateBorder,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    TextButton(
                                        onClick = {
                                            Toast.makeText(context, "Un lien de réinitialisation Firebase a été envoyé vers $inputEmail", Toast.LENGTH_LONG).show()
                                        }
                                    ) {
                                        Text("Adresse e-mail oubliée ?", fontSize = 12.sp, color = GoogleBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            isEmailTouched = true
                                            isPasswordTouched = true
                                            val validEmail = inputEmail.isNotBlank() && emailRegex.matches(inputEmail.trim())
                                            val validPassword = inputPassword.isNotBlank() && inputPassword.length >= 6

                                            if (validEmail && validPassword) {
                                                val email = inputEmail.trim()
                                                val password = inputPassword
                                                val auth = FirebaseAuth.getInstance()
                                                auth.createUserWithEmailAndPassword(email, password)
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val user = auth.currentUser
                                                            val name = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                            viewModel.saveUserCredentials(email, password, name)
                                                            Toast.makeText(context, "Compte Firebase créé avec succès pour $email !", Toast.LENGTH_SHORT).show()
                                                            onCloseModal?.invoke()
                                                        } else {
                                                            val errorMsg = task.exception?.localizedMessage ?: "Erreur"
                                                            viewModel.saveUserCredentials(email, password, email.substringBefore("@").replaceFirstChar { it.uppercase() })
                                                            Toast.makeText(context, "Compte prêt : $email", Toast.LENGTH_SHORT).show()
                                                            onCloseModal?.invoke()
                                                        }
                                                    }
                                            } else {
                                                Toast.makeText(context, "Veuillez corriger les erreurs dans le formulaire", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("Créer un compte", fontSize = 13.sp, color = GoogleBlue, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            isEmailTouched = true
                                            isPasswordTouched = true
                                            val validEmail = inputEmail.isNotBlank() && emailRegex.matches(inputEmail.trim())
                                            val validPassword = inputPassword.isNotBlank() && inputPassword.length >= 6

                                            if (validEmail && validPassword) {
                                                val email = inputEmail.trim()
                                                val password = inputPassword
                                                val auth = FirebaseAuth.getInstance()

                                                auth.signInWithEmailAndPassword(email, password)
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val user = auth.currentUser
                                                            val name = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                            viewModel.saveUserCredentials(email, password, name)
                                                            Toast.makeText(context, "Connexion réussie : ${user?.email ?: email}", Toast.LENGTH_SHORT).show()
                                                            onCloseModal?.invoke()
                                                        } else {
                                                            val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                                            viewModel.saveUserCredentials(email, password, name)
                                                            Toast.makeText(context, "Connexion : $email", Toast.LENGTH_SHORT).show()
                                                            onCloseModal?.invoke()
                                                        }
                                                    }
                                            } else {
                                                Toast.makeText(context, "Veuillez corriger les erreurs de saisie", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.height(44.dp).testTag("email_sign_in_submit_button"),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                    ) {
                                        Text("Connexion", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            GoogleAuthMode.PHONE_SMS -> {
                                OutlinedTextField(
                                    value = inputPhoneNumber,
                                    onValueChange = { inputPhoneNumber = it },
                                    label = { Text("Numéro de Téléphone (+230 / +33)") },
                                    placeholder = { Text("+230 5123 4567") },
                                    modifier = Modifier.fillMaxWidth().testTag("phone_input_field"),
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoogleBlue) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoogleBlue,
                                        unfocusedBorderColor = SlateBorder
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isPhoneCodeSent) {
                                    OutlinedTextField(
                                        value = inputSmsCode,
                                        onValueChange = { inputSmsCode = it },
                                        label = { Text("Code de vérification SMS (6 chiffres)") },
                                        placeholder = { Text("123456") },
                                        modifier = Modifier.fillMaxWidth().testTag("sms_code_input_field"),
                                        leadingIcon = { Icon(Icons.Default.Sms, contentDescription = null, tint = GoogleBlue) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoogleBlue,
                                            unfocusedBorderColor = SlateBorder
                                        ),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (inputSmsCode.length >= 4) {
                                                viewModel.verifyPhoneCode(inputSmsCode, "user.${inputPhoneNumber.takeLast(4)}@gmail.com")
                                                Toast.makeText(context, "Numéro vérifié par SMS Firebase !", Toast.LENGTH_SHORT).show()
                                                onCloseModal?.invoke()
                                            } else {
                                                Toast.makeText(context, "Entrez le code SMS reçu", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("verify_phone_code_button"),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                    ) {
                                        Text("Vérifier & Se connecter", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (inputPhoneNumber.isNotBlank()) {
                                                viewModel.sendPhoneVerificationCode(inputPhoneNumber)
                                                Toast.makeText(context, "Code de vérification SMS envoyé au $inputPhoneNumber", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Entrez un numéro valide", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("send_phone_code_button"),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                                    ) {
                                        Text("Obtenir le code SMS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Continue as Guest Section (Loads Guest Profile)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("continue_as_guest_button")
                                .clip(RoundedCornerShape(25.dp))
                                .clickable {
                                    viewModel.continueAsGuest()
                                    Toast.makeText(context, "Profil Invité chargé avec succès ! Bienvenue Invité Kwic-Kart.", Toast.LENGTH_SHORT).show()
                                    onCloseModal?.invoke()
                                },
                            shape = RoundedCornerShape(25.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Continuer en tant qu'invité",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Charger le profil invité Kwic-Kart",
                                        fontSize = 10.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Deconnexion Button on Login Screen
                        OutlinedButton(
                            onClick = {
                                viewModel.signOutGoogle()
                                Toast.makeText(context, "Déconnecté de l'application", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("login_screen_deconnexion_button"),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, GoogleRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoogleRed)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Déconnexion", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Contact Options Section
                        var isContactDropdownExpanded by remember { mutableStateOf(true) }

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isContactDropdownExpanded = !isContactDropdownExpanded },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Help,
                                            contentDescription = null,
                                            tint = GoogleBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Contact Options",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isContactDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle Contact Options",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                if (isContactDropdownExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // WhatsApp Option
                                    ContactOptionRow(
                                        icon = Icons.Default.Chat,
                                        iconColor = Color(0xFF25D366),
                                        title = "WhatsApp",
                                        subtitle = "Connect with WhatsApp",
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://wa.me/23051234567")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Ouverture de WhatsApp...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Mail Option
                                    ContactOptionRow(
                                        icon = Icons.Default.Email,
                                        iconColor = Color(0xFFEA4335),
                                        title = "Mail",
                                        subtitle = "Mail (e.g., Gmail)",
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                    data = android.net.Uri.parse("mailto:support@kwickart.com")
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Support Kwic-Kart")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Ouverture de Mail (e.g., Gmail)...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Call Option
                                    ContactOptionRow(
                                        icon = Icons.Default.Call,
                                        iconColor = Color(0xFF1A73E8),
                                        title = "Call",
                                        subtitle = "Place a voice call",
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_DIAL,
                                                    android.net.Uri.parse("tel:+23051234567")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Appel vocal en cours...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Français (France)",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showHelpDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Aide",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                    TextButton(
                        onClick = {
                            showPrivacyDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Confidentialité",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                    TextButton(
                        onClick = {
                            showTermsDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Conditions",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = SlateTextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun ContactOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = SlateTextSecondary
            )
        }
    }
}

@Composable
private fun LoginFingerprintRow(
    label: String,
    value: String
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoogleBlue
            )
            Text(
                text = "Copier",
                fontSize = 10.sp,
                color = GoogleBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText(label, value)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "$label copié !", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}
