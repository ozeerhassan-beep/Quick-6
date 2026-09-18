package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.CatalogViewModel

private val EmeraldSuccess = Color(0xFF10B981)
private val BlueAccent = Color(0xFF0F5132)
private val AmberWarning = Color(0xFFF59E0B)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForceResetDialog(viewModel: CatalogViewModel) {
    val isVisible by viewModel.showForceResetDialog.collectAsState()
    if (!isVisible) return

    val currentStep by viewModel.forceResetCurrentStep.collectAsState()
    val stepDetail by viewModel.forceResetStepDetail.collectAsState()
    val progressPercent by viewModel.forceResetProgressPercent.collectAsState()
    val totalFound by viewModel.forceResetTotalFound.collectAsState()
    val catalogStats by viewModel.forceResetCatalogSummary.collectAsState()
    val errorMessage by viewModel.forceResetErrorMessage.collectAsState()
    val isComplete by viewModel.forceResetIsComplete.collectAsState()
    val dialogTitle by viewModel.syncDialogTitle.collectAsState()
    val dialogSubtitle by viewModel.syncDialogSubtitle.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "force_reset_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val stepTitles = listOf(
        "Connexion Cloud Firestore & Préparation",
        "Sécurisation de la base locale",
        "Téléchargement complet des catalogues",
        "Déduplication & Structuration des articles",
        "Mise à jour et indexation dans Room SQLite"
    )

    Dialog(
        onDismissRequest = {
            if (isComplete || errorMessage != null) {
                viewModel.dismissForceResetDialog()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = isComplete || errorMessage != null,
            dismissOnClickOutside = isComplete || errorMessage != null,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("force_reset_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(
                1.5.dp,
                when {
                    isComplete -> EmeraldSuccess.copy(alpha = 0.5f)
                    errorMessage != null -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    else -> BlueAccent.copy(alpha = 0.4f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = CircleShape,
                    color = when {
                        isComplete -> EmeraldSuccess.copy(alpha = 0.15f)
                        errorMessage != null -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else -> BlueAccent.copy(alpha = 0.12f)
                    },
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            isComplete -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            errorMessage != null -> {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .rotate(rotationAngle)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = dialogTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = dialogSubtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar & Percentage
                val displayPercent = progressPercent.coerceIn(0f, 100f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isComplete -> "Terminé à 100%"
                            errorMessage != null -> "Échec de l'opération"
                            else -> "Étape ${currentStep + 1} sur ${stepTitles.size}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isComplete -> EmeraldSuccess
                            errorMessage != null -> MaterialTheme.colorScheme.error
                            else -> BlueAccent
                        }
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isComplete -> EmeraldSuccess.copy(alpha = 0.15f)
                            errorMessage != null -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else -> BlueAccent.copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = "${displayPercent.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isComplete -> EmeraldSuccess
                                errorMessage != null -> MaterialTheme.colorScheme.error
                                else -> BlueAccent
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { displayPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        isComplete -> EmeraldSuccess
                        errorMessage != null -> MaterialTheme.colorScheme.error
                        else -> BlueAccent
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live status detail box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isComplete -> EmeraldSuccess.copy(alpha = 0.08f)
                        errorMessage != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stepDetail,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isComplete -> EmeraldSuccess
                            errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))

                // 5 Steps Visual Stepper
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stepTitles.forEachIndexed { index, title ->
                        val isStepDone = isComplete || (currentStep > index)
                        val isStepActive = (currentStep == index && !isComplete && errorMessage == null)
                        val isStepFailed = (currentStep == index && errorMessage != null)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isStepDone -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Terminé",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    isStepActive -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = BlueAccent,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    isStepFailed -> {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Échec",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "En attente",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isStepActive || isStepDone) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isStepDone -> EmeraldSuccess
                                    isStepActive -> BlueAccent
                                    isStepFailed -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Catalog Breakdown Badges (if available)
                if (catalogStats.isNotEmpty() || totalFound > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Catalogues & Articles détectés :",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                catalogStats.forEach { (cat, count) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BlueAccent.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "$cat: $count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BlueAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (totalFound > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Total: $totalFound",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Button
                if (isComplete) {
                    Button(
                        onClick = { viewModel.dismissForceResetDialog() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("force_reset_done_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fermer & Explorer le Catalogue",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (errorMessage != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.dismissForceResetDialog() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Fermer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (dialogTitle.contains("Force Reset", ignoreCase = true)) {
                                    viewModel.forceUpdateFromFirebase { _, _ -> }
                                } else {
                                    viewModel.syncAllFilesFromServer { _, _ -> }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Réessayer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { /* In progress */ },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Synchronisation en cours...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
