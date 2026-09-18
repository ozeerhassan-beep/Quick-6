package com.example.ui.screens

import java.util.Locale
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ServerCatalogFile
import com.example.ui.CatalogViewModel
import com.example.ui.FileUploadItem
import com.example.ui.components.ValidationReportDialog
import com.example.util.SpreadsheetImporter
import com.example.ui.theme.BlueLight
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.LoloColor
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SuperUColor
import com.example.ui.theme.WayColor
import com.example.ui.theme.WinnersColor

@Composable
fun ImportScreen(viewModel: CatalogViewModel) {
    val context = LocalContext.current
    val activeCatalog by viewModel.activeCatalog.collectAsState()
    val pendingReport by viewModel.pendingValidationReport.collectAsState()
    val successInfo by viewModel.importSuccessInfo.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importPercentProgress by viewModel.importPercentProgress.collectAsState()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState()
    val incomingExternalFiles by viewModel.incomingExternalFiles.collectAsState()
    val externalLinkNotification by viewModel.externalLinkNotification.collectAsState()

    if (userRole != "ADMIN") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEA4335).copy(alpha = 0.12f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFEA4335),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Accès Réservé aux Administrateurs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "La section d'importation et de gestion des catalogues est strictement réservée au compte Administrateur (mobsma23@gmail.com).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // Server-Published Files State
    val serverFiles by viewModel.serverCatalogFiles.collectAsState()
    val isServerFilesLoading by viewModel.isServerFilesLoading.collectAsState()
    val isServerUploading by viewModel.isServerUploading.collectAsState()
    val serverUploadProgress by viewModel.serverUploadProgress.collectAsState()
    val serverUploadPercentage by viewModel.serverUploadPercentage.collectAsState()
    val serverUploadSuccess by viewModel.serverUploadSuccess.collectAsState()
    val isServerSyncing by viewModel.isServerSyncing.collectAsState()
    val lastServerSyncTime by viewModel.lastServerSyncTime.collectAsState()

    // Force Update/Reset Progress Pop-up States
    val showForceResetDialog by viewModel.showForceResetDialog.collectAsState()
    val forceResetCurrentStep by viewModel.forceResetCurrentStep.collectAsState()
    val forceResetErrorMessage by viewModel.forceResetErrorMessage.collectAsState()
    val forceResetIsComplete by viewModel.forceResetIsComplete.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Server & Synchro, 1: Upload (Admin), 2: Gestion
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAutoUploadDialog by remember { mutableStateOf(false) }

    // Multi-file upload state for Admin & Local Import
    var selectedFiles by remember { mutableStateOf<List<FileUploadItem>>(emptyList()) }

    // File Picker for Admin & Local Import (.xlsx / .csv / .pdf) with MULTI-FILE support
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.map { uri ->
                val fileName = SpreadsheetImporter.getFileNameFromUri(context, uri) ?: "catalogue_import.xlsx"
                val detected = SpreadsheetImporter.autoDetectCatalog(fileName)
                FileUploadItem(
                    uri = uri,
                    fileName = fileName,
                    targetCatalog = detected
                )
            }
            // Append and deduplicate by fileName
            val combined = (selectedFiles + newItems).distinctBy { it.fileName }
            selectedFiles = combined
            Toast.makeText(
                context,
                "${newItems.size} fichier(s) sélectionné(s) (Total : ${combined.size})",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (pendingReport != null) {
        ValidationReportDialog(
            report = pendingReport!!,
            onConfirmImportValid = { viewModel.confirmImportValidProducts(pendingReport!!) },
            onDismiss = { viewModel.dismissValidationReport() }
        )
    }

    if (successInfo != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportSuccess() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Succès",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synchronisation Réussie !",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Les données du catalogue ont été intégrées avec succès dans votre base locale.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Source / Fichier :", fontSize = 12.sp, color = SlateTextSecondary)
                                Text(successInfo!!.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Articles ajoutés :", fontSize = 12.sp, color = SlateTextSecondary)
                                Text("${successInfo!!.count} produit(s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Catalogue Cible :", fontSize = 12.sp, color = SlateTextSecondary)
                                Text(successInfo!!.targetCatalog, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Horodatage :", fontSize = 12.sp, color = SlateTextSecondary)
                                Text(successInfo!!.timestamp, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissImportSuccess() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Super, Compris", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Réinitialiser",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Réinitialiser Toutes les Données ?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = "Êtes-vous sûr de vouloir supprimer L'ENSEMBLE des données de l'application (catalogues Dreamprice & Intermart, panier et historique) ? Cette action est irréversible.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "Toutes les données ont été réinitialisées.", Toast.LENGTH_LONG).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Oui, Tout Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Annuler", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showAutoUploadDialog) {
        Dialog(
            onDismissRequest = { showAutoUploadDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AutoUploadCatalogScreen(
                    onUploadComplete = { uri, supermarket ->
                        showAutoUploadDialog = false
                        // TODO: Add logic to handle auto-upload
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = BlueLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Centre Serveur & Catalogues",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userRole == "ADMIN") "Mode Administrateur • Téléversement & Diffusion" else "Synchronisation en direct avec le Serveur",
                        fontSize = 11.sp,
                        color = if (userRole == "ADMIN") BluePrimary else SlateTextSecondary,
                        fontWeight = if (userRole == "ADMIN") FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "En Ligne",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BluePrimary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BluePrimary,
                        height = 3.dp
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Synchro Firebase", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(if (userRole == "ADMIN") "Upload Firebase" else "Importer Fichier", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Gestion", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        HorizontalDivider(color = SlateBorder)

        // Tab Contents
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Incoming files section
            if (externalLinkNotification != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissExternalNotification() },
                    title = { Text("Fichiers reçus") },
                    text = { Text(externalLinkNotification!!) },
                    confirmButton = {
                        Button(onClick = { viewModel.dismissExternalNotification() }) {
                            Text("OK")
                        }
                    }
                )
            }

            if (incomingExternalFiles.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueLight.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Fichiers en attente d'importation (${incomingExternalFiles.size})", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        incomingExternalFiles.forEach { file ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(file.fileName, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.importMultipleFilesLocally(context, incomingExternalFiles) { success, msg ->
                                if (success) {
                                    viewModel.clearIncomingExternalFiles()
                                    Toast.makeText(context, "Importation terminée", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("Importer tous")
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> ServerFilesSyncTab(
                    context = context,
                    viewModel = viewModel,
                    serverFiles = serverFiles,
                    isLoading = isServerFilesLoading,
                    isSyncing = isServerSyncing,
                    lastSyncTime = lastServerSyncTime,
                    userRole = userRole,
                    onNavigateToUpload = { selectedTab = 1 }
                )

                1 -> AdminFileUploadTab(
                    context = context,
                    viewModel = viewModel,
                    userRole = userRole,
                    selectedFiles = selectedFiles,
                    isUploading = isServerUploading,
                    uploadProgress = serverUploadProgress,
                    uploadPercentage = serverUploadPercentage,
                    onPickFiles = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "text/csv",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "application/pdf",
                                "*/*"
                            )
                        )
                    },
                    onUpdateFileCatalog = { fileItem, newCat ->
                        selectedFiles = selectedFiles.map {
                            if (it.fileName == fileItem.fileName) it.copy(targetCatalog = newCat) else it
                        }
                    },
                    onRemoveFile = { fileItem ->
                        selectedFiles = selectedFiles.filterNot { it.fileName == fileItem.fileName }
                    },
                    onClearAll = {
                        selectedFiles = emptyList()
                    },
                    onUploadSuccess = {
                        selectedFiles = emptyList()
                        selectedTab = 0
                    },
                    onOpenAutoUpload = { showAutoUploadDialog = true }
                )

                2 -> AdminTab(
                    context = context,
                    viewModel = viewModel,
                    userRole = userRole,
                    isImporting = isImporting,
                    lastBackupTimestamp = lastBackupTimestamp,
                    onResetAllClick = { showResetConfirmDialog = true }
                )
            }
        }
    }
}

/**
 * TAB 0: SERVER & USER SYNCHRONIZATION
 * Allows all users to see Admin-published files on the Cloud Server and synchronize with 1 click.
 */
@Composable
private fun SynchroFirebaseContent(
    context: Context,
    viewModel: CatalogViewModel,
    serverFiles: List<ServerCatalogFile>,
    isSyncing: Boolean,
    lastSyncTime: String?
) {
    // Status Indicator
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, BluePrimary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = BlueLight,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Catalogues Firebase Firestore",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lastSyncTime != null) "Dernière synchro : $lastSyncTime" else "${serverFiles.size} catalogue(s) Firebase disponibles",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download All Button
            Button(
                onClick = {
                    viewModel.syncAllFilesFromServer { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !isSyncing && serverFiles.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Synchronisation...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tout Synchroniser (${serverFiles.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MAJ Button (Error-Alert Theme)
            Button(
                onClick = {
                    viewModel.forceUpdateFromFirebase { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !isSyncing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Mise à jour en cours...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAJ (Force Reset)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            val isTestingFirestore by viewModel.isTestingFirestoreConnection.collectAsState()
            val firestoreDiag by viewModel.firestoreConnectionDiagnostics.collectAsState()

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    viewModel.testFirestoreConnection { diag ->
                        Toast.makeText(context, diag.message, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !isTestingFirestore,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTestingFirestore) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test de connexion Firestore...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp), tint = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tester la Connexion Firestore", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BluePrimary)
                }
            }

            if (firestoreDiag != null) {
                val diag = firestoreDiag!!
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (diag.isConnected) EmeraldSuccess.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (diag.isConnected) EmeraldSuccess.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (diag.isConnected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (diag.isConnected) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (diag.isConnected) "Connexion Établie (${diag.latencyMs}ms)" else "Problème de Connexion",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (diag.isConnected) EmeraldSuccess else MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diag.message,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Projet: ${diag.projectId} | Lecture: ${if (diag.canRead) "OK" else "KO"} | Écriture: ${if (diag.canWrite) "OK" else "KO"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerFilesSyncTab(
    context: Context,
    viewModel: CatalogViewModel,
    serverFiles: List<ServerCatalogFile>,
    isLoading: Boolean,
    isSyncing: Boolean,
    lastSyncTime: String?,
    userRole: String,
    onNavigateToUpload: () -> Unit
) {
    val syncSummary by viewModel.serverSyncSummary.collectAsState()
    var syncingFileId by remember { mutableStateOf<String?>(null) }
    var selectedFileIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Confirmer la suppression",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Êtes-vous sûr de vouloir supprimer définitivement les ${selectedFileIds.size} catalogue(s) sélectionné(s) du serveur Firebase Firestore ? Cette action supprimera également tous les articles associés sur le serveur.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        val fileIdsToDelete = selectedFileIds.toList()
                        selectedFileIds = emptySet()
                        viewModel.deleteServerFiles(fileIdsToDelete) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Oui, Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SynchroFirebaseContent(
            context = context,
            viewModel = viewModel,
            serverFiles = serverFiles,
            isSyncing = isSyncing,
            lastSyncTime = lastSyncTime
        )

        if (userRole == "ADMIN") {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onNavigateToUpload,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Téléverser un Fichier vers Firebase Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Files Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catalogues Disponibles sur Firebase (${serverFiles.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }

        if (userRole == "ADMIN" && serverFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedFileIds.size} catalogue(s) sélectionné(s)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedFileIds.isNotEmpty()) {
                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Supprimer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedFileIds = serverFiles.map { it.id }.toSet()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Tout Sélectionner", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                selectedFileIds = emptySet()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Tout Désélectionner", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (serverFiles.isEmpty() && !isLoading) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = SlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucun catalogue sur Firebase Firestore pour le moment.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "L'administrateur peut publier des fichiers Excel/CSV/PDF depuis l'onglet 'Upload Firebase'.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            val sortedServerFiles = remember(serverFiles) {
                serverFiles.sortedBy { it.fileName.ifBlank { it.catalogType }.lowercase() }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sortedServerFiles.forEach { file ->
                    val isThisSyncing = isSyncing && syncingFileId == file.id
                    val storeColor = getStoreColor(file.catalogType)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.deleteServerFiles(listOf(file.id)) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                                Color(0xFFDC2626)
                            } else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Glisser pour supprimer",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, storeColor.copy(alpha = 0.3f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Top Row: Badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (userRole == "ADMIN") {
                                                Checkbox(
                                                    checked = selectedFileIds.contains(file.id),
                                                    onCheckedChange = { isChecked ->
                                                        selectedFileIds = if (isChecked) {
                                                            selectedFileIds + file.id
                                                        } else {
                                                            selectedFileIds - file.id
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = storeColor.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, storeColor.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = file.catalogType.uppercase(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = storeColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = ".${file.fileType}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = "${file.productCount} articles",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // File Name
                                    Text(
                                        text = file.fileName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (file.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = file.description,
                                            fontSize = 12.sp,
                                            color = SlateTextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Actions Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                syncingFileId = file.id
                                                viewModel.syncSingleFileFromServer(file) { success, msg ->
                                                    syncingFileId = null
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = !isSyncing,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = storeColor),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                        ) {
                                            if (isThisSyncing) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Synchro...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Synchroniser ce catalogue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * TAB 1: ADMIN FILE UPLOAD TO SERVER & LOCAL IMPORT
 * Allows administrators to select multiple files (.xlsx, .csv, .pdf) at once and upload them to the Cloud Server or import into Room.
 */
@Composable
private fun AdminFileUploadTab(
    context: Context,
    viewModel: CatalogViewModel,
    userRole: String,
    selectedFiles: List<FileUploadItem>,
    isUploading: Boolean,
    uploadProgress: String,
    uploadPercentage: Float = 0f,
    onPickFiles: () -> Unit,
    onUpdateFileCatalog: (FileUploadItem, String) -> Unit,
    onRemoveFile: (FileUploadItem) -> Unit,
    onClearAll: () -> Unit,
    onUploadSuccess: () -> Unit,
    onOpenAutoUpload: () -> Unit
) {
    val isImporting by viewModel.isImporting.collectAsState()
    val importStatusMessage by viewModel.importStatusMessage.collectAsState()
    val importPercentProgress by viewModel.importPercentProgress.collectAsState()
    var manualCsvText by remember { mutableStateOf("") }
    var showManualCsv by remember { mutableStateOf(false) }

    val catalogs = listOf("DREAMPRICE", "INTERMART", "SUPER_U", "WINNERS", "WAY", "LOLO", "KING_SAVERS", "GSR").sorted()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Auto Upload Button
        Button(
            onClick = onOpenAutoUpload,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Téléversement Automatique")
        }

        // Admin Role Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (userRole == "ADMIN") BluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, if (userRole == "ADMIN") BluePrimary.copy(alpha = 0.3f) else SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = if (userRole == "ADMIN") BluePrimary else SlateTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (userRole == "ADMIN") "Mode Téléversement Firebase Actif" else "Téléversement & Importation Multi-Fichiers",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (userRole == "ADMIN") BluePrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userRole == "ADMIN")
                            "Sélectionnez plusieurs fichiers à la fois. Ils seront analysés et téléversés vers Firebase Firestore pour tous les utilisateurs."
                        else
                            "Sélectionnez un ou plusieurs fichiers pour les importer dans votre base locale Room ou vers Firebase.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1: Choose Files Card (Multi-file support)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "1. Sélectionner les Fichiers Catalogues",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sélection multiple (.xlsx, .csv, .pdf)",
                            fontSize = 11.sp,
                            color = SlateTextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (selectedFiles.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BlueLight,
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${selectedFiles.size} sélectionné(s)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedFiles.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickFiles() },
                        shape = RoundedCornerShape(12.dp),
                        color = BlueLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.5.dp, BluePrimary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cliquez pour choisir un ou plusieurs fichiers...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Text(
                                text = "Vous pouvez sélectionner plusieurs catalogues d'un seul coup (.XLSX, .CSV, .PDF)",
                                fontSize = 11.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onPickFiles,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parcourir les fichiers", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // List of Selected Files
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedFiles.forEach { fileItem ->
                            val isPdf = fileItem.fileName.endsWith(".pdf", ignoreCase = true)
                            val isCsv = fileItem.fileName.endsWith(".csv", ignoreCase = true)
                            val isExcel = fileItem.fileName.endsWith(".xlsx", ignoreCase = true) || fileItem.fileName.endsWith(".xls", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when {
                                                isPdf -> Color(0xFFFEE2E2)
                                                isExcel -> Color(0xFFDCFCE7)
                                                isCsv -> Color(0xFFE0E7FF)
                                                else -> Color(0xFFF1F5F9)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when {
                                                        isPdf -> Icons.Default.PictureAsPdf
                                                        isExcel -> Icons.Default.InsertDriveFile
                                                        isCsv -> Icons.Default.Description
                                                        else -> Icons.Default.InsertDriveFile
                                                    },
                                                    contentDescription = null,
                                                    tint = when {
                                                        isPdf -> Color(0xFFDC2626)
                                                        isExcel -> Color(0xFF16A34A)
                                                        isCsv -> Color(0xFF4338CA)
                                                        else -> SlateTextSecondary
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fileItem.fileName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Magasin : ${fileItem.targetCatalog.replace("_", " ")}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onRemoveFile(fileItem) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Supprimer ce fichier",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Store chips for this individual file
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        catalogs.forEach { cat ->
                                            val isSelected = fileItem.targetCatalog.equals(cat, ignoreCase = true)
                                            val col = getStoreColor(cat)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onUpdateFileCatalog(fileItem, cat) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) col else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                border = if (isSelected) null else BorderStroke(0.5.dp, SlateBorder)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = cat.replace("_", " "),
                                                        fontSize = 9.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Options row under file list: Add more or Clear all
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onPickFiles,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter d'autres fichiers", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(
                                onClick = onClearAll,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tout effacer", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Upload Status Display
        if (isUploading || uploadProgress.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BlueLight.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BluePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = uploadProgress,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                        }
                        if (isUploading && uploadPercentage > 0f) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", uploadPercentage * 100f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                    }
                    if (isUploading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (uploadPercentage > 0f) uploadPercentage else 0.3f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = BluePrimary,
                            trackColor = BlueLight
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Local Import Status Display
        if (isImporting || importStatusMessage.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFDCFCE7),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF16A34A))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = importStatusMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                    if (isImporting) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progression",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF16A34A).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$importPercentProgress%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (importPercentProgress > 0) importPercentProgress / 100f else 0.5f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Action Buttons
        val count = selectedFiles.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Admin Upload to Server Button
            Button(
                onClick = {
                    if (selectedFiles.isNotEmpty()) {
                        viewModel.uploadMultipleFilesToServer(
                            context = context,
                            files = selectedFiles,
                            defaultDescription = ""
                        ) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                onUploadSuccess()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Veuillez d'abord sélectionner au moins un fichier.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedFiles.isNotEmpty() && !isUploading && !isImporting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Téléversement...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (count > 1) "Téléverser ($count fichiers)" else "Téléverser sur Firebase",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Local Import Only Fallback
            OutlinedButton(
                onClick = {
                    if (selectedFiles.isNotEmpty()) {
                        viewModel.importMultipleFilesLocally(context, selectedFiles) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Veuillez choisir au moins un fichier.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedFiles.isNotEmpty() && !isImporting && !isUploading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (count > 1) "Importer ($count) dans Room" else "Importer dans Room",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Manual CSV Text Input Accordion
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualCsv = !showManualCsv },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Coller du texte CSV manuellement", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(
                        imageVector = if (showManualCsv) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = SlateTextSecondary
                    )
                }

                if (showManualCsv) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualCsvText,
                        onValueChange = { manualCsvText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("nom,categorie,marque,unite,prix,cout\nHuile Rani,Épicerie,Rani,1L,89.0,72.0", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        minLines = 4
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (manualCsvText.isNotBlank()) {
                                viewModel.handleManualCsvTextImport(manualCsvText, "DREAMPRICE")
                                manualCsvText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Analyser & Importer le CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * TAB 2: GOOGLE DRIVE TAB
 */
/**
 * TAB 2: ADMIN & MAINTENANCE TAB
 */
@Composable
private fun AdminTab(
    context: Context,
    viewModel: CatalogViewModel,
    userRole: String,
    isImporting: Boolean,
    lastBackupTimestamp: String,
    onResetAllClick: () -> Unit
) {
    val serverFiles by viewModel.serverCatalogFiles.collectAsState()
    val isServerFilesLoading by viewModel.isServerFilesLoading.collectAsState()

    var selectedCsvIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var fileToDelete by remember { mutableStateOf<ServerCatalogFile?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var hasFetchedFiles by remember { mutableStateOf(false) }

    // Dialog: Delete single CSV file
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Supprimer de Firestore",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Voulez-vous vraiment supprimer définitivement ce fichier CSV et ses données de Cloud Firestore ?",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = target.fileName.ifBlank { "${target.catalogType}.csv" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${target.catalogType} • ${target.productCount} articles • ${target.fileSizeFormatted}",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = target.id
                        fileToDelete = null
                        selectedCsvIds = selectedCsvIds - toRemove
                        viewModel.deleteServerFiles(listOf(toRemove)) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Supprimer définitivement", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { fileToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog: Delete multiple selected CSV files
    if (showBulkDeleteDialog && selectedCsvIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Supprimer de Firestore",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Voulez-vous vraiment supprimer définitivement les ${selectedCsvIds.size} fichier(s) CSV sélectionné(s) de Cloud Firestore ?\n\nTous les documents et tous les articles associés seront définitivement effacés du serveur.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = selectedCsvIds.toList()
                        showBulkDeleteDialog = false
                        selectedCsvIds = emptySet()
                        viewModel.deleteServerFiles(toDelete) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Supprimer ${selectedCsvIds.size} fichier(s)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBulkDeleteDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    Column {
        // CARD: Firestore CSV Files Management
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("firestore_csv_management_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BluePrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fichiers CSV sur Firestore",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gérez et supprimez les fichiers CSV enregistrés sur Firestore",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action: Retrieve list of all CSV saved on Firestore
                Button(
                    onClick = {
                        hasFetchedFiles = true
                        viewModel.refreshServerFiles()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("retrieve_firestore_csv_button"),
                    enabled = !isServerFilesLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    if (isServerFilesLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Récupération depuis Firestore...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!hasFetchedFiles && serverFiles.isEmpty()) "Récupérer la liste des CSV Firestore" else "Actualiser les fichiers Firestore (${serverFiles.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // If files have been retrieved
                if (hasFetchedFiles || serverFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    if (serverFiles.isEmpty() && !isServerFilesLoading) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Aucun fichier CSV enregistré sur Cloud Firestore.",
                                fontSize = 12.sp,
                                color = SlateTextSecondary,
                                modifier = Modifier.padding(14.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else if (serverFiles.isNotEmpty()) {
                        // Multi-selection bar & Bulk Delete button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        selectedCsvIds = if (selectedCsvIds.size == serverFiles.size) {
                                            emptySet()
                                        } else {
                                            serverFiles.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Checkbox(
                                        checked = serverFiles.isNotEmpty() && selectedCsvIds.size == serverFiles.size,
                                        onCheckedChange = { checked ->
                                            selectedCsvIds = if (checked) {
                                                serverFiles.map { it.id }.toSet()
                                            } else {
                                                emptySet()
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                                    )
                                    Text(
                                        text = if (selectedCsvIds.isEmpty()) "Tout sélectionner (${serverFiles.size})" else "${selectedCsvIds.size} sélectionné(s)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (selectedCsvIds.isNotEmpty()) {
                                    Button(
                                        onClick = { showBulkDeleteDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Supprimer (${selectedCsvIds.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // List of CSV files
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            serverFiles.forEach { file ->
                                val isSelected = selectedCsvIds.contains(file.id)
                                val storeColor = getStoreColor(file.catalogType)

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                            viewModel.deleteServerFiles(listOf(file.id)) { success, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                            true
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                                            Color(0xFFDC2626)
                                        } else Color.Transparent

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(color)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Supprimer",
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Glisser pour supprimer",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) BluePrimary.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) BluePrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCsvIds = if (isSelected) {
                                                        selectedCsvIds - file.id
                                                    } else {
                                                        selectedCsvIds + file.id
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedCsvIds = if (checked) {
                                                            selectedCsvIds + file.id
                                                        } else {
                                                            selectedCsvIds - file.id
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                                                )

                                                Spacer(modifier = Modifier.width(4.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = storeColor.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = file.catalogType,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = storeColor,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = file.fileName.ifBlank { "${file.catalogType}.csv" },
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    Text(
                                                        text = "${file.productCount} articles • ${file.fileSizeFormatted} • ${file.formattedDate}",
                                                        fontSize = 10.sp,
                                                        color = SlateTextSecondary
                                                    )
                                                }

                                                // Individual delete button
                                                IconButton(
                                                    onClick = { fileToDelete = file },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Supprimer ce fichier",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
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

        // CARD: Periodic Cloud Backups
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sauvegardes & Synchronisation Automatique",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Dernière sauvegarde auto : $lastBackupTimestamp",
                    fontSize = 11.sp,
                    color = SlateTextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.performFirebaseBackup() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sauvegarder Maintenant sur Firebase Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Maintenance & Reset Operations Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Nettoyage & Maintenance des Données",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Réinitialisez l'ensemble des données locales (Room SQLite) de l'application en un clic.",
                    fontSize = 11.sp,
                    color = SlateTextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Button(
                    onClick = onResetAllClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Réinitialiser TOUTES les Données (Room)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.forceUpdateFromFirebase { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mise à jour en cours...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mise à jour (Upsert Firestore)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getStoreColor(catalogType: String): Color {
    val clean = catalogType.trim().uppercase()
    return when {
        clean.contains("DREAMPRICE") -> DreampriceColor
        clean.contains("INTERMART") -> IntermartColor
        clean.contains("SUPER_U") || clean.contains("SUPERU") -> SuperUColor
        clean.contains("WINNERS") -> WinnersColor
        clean.contains("LOLO") -> LoloColor
        clean.contains("WAY") -> WayColor
        else -> BluePrimary
    }
}
