package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ============================================================================
// 1. ÉCRAN AIDE & SUPPORT
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aide & Support") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Foire Aux Questions (FAQ)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            FaqItem(
                question = "Comment synchroniser les catalogues ?",
                answer = "La synchronisation est automatique via Firestore. Vous pouvez aussi glisser vers le bas (Pull-to-Refresh) sur l'écran des catalogues."
            )

            FaqItem(
                question = "Comment libérer de l'espace de stockage ?",
                answer = "Allez dans Paramètres > Taille du Cache & Utilisation des Données, puis appuyez sur 'Vider le cache' pour supprimer la base de données SQLite (Room) locale."
            )

            FaqItem(
                question = "Que signifient les indicateurs de synchronisation ?",
                answer = "• Vert : Données synchronisées\n• Jaune : Synchronisation en cours...\n• Gris : Mode hors-ligne\n• Rouge : Erreur de connexion"
            )

            HorizontalDivider()

            Text(
                text = "Contact Support",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Pour toute assistance : support@kwickart.com")
            Text("Heures d'ouverture : Du lundi au vendredi, 8h - 17h")
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = question, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = answer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ============================================================================
// 2. ÉCRAN POLITIQUE DE CONFIDENTIALITÉ
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Politique de Confidentialité") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicySection(
                title = "1. Données collectées",
                content = "• Informations de votre compte Google (Nom, e-mail) lors de l'authentification.\n• Fichiers téléversés (catalogues .xlsx, .csv, .pdf).\n• Métadonnées d'état du réseau pour la synchronisation."
            )

            PolicySection(
                title = "2. Utilisation des données",
                content = "• Authentification et gestion de profil utilisateur.\n• Stockage local sur l'appareil via la base SQLite (Room) pour la consultation hors-ligne.\n• Sauvegarde Cloud et synchronisation temps réel via Firebase Firestore."
            )

            PolicySection(
                title = "3. Protection & Partage",
                content = "Vos données ne sont ni vendues ni transmises à des tiers. Vous pouvez vider le cache local ou demander la suppression définitive de votre compte à tout moment."
            )
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = content, style = MaterialTheme.typography.bodyMedium)
    }
}

// ============================================================================
// 3. ÉCRAN CONDITIONS D'UTILISATION
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conditions d'Utilisation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicySection(
                title = "1. Objet du Service",
                content = "Kwic-Kart est une application de consultation, comparaison et gestion de catalogues de supermarchés (Dreamprice, Intermart, Super U, Winner's, Way, etc.)."
            )

            PolicySection(
                title = "2. Responsabilité sur les Contenus",
                content = "L'utilisateur s'engage à téléverser uniquement des fichiers autorisés et aux formats supportés (.xlsx, .csv, .pdf)."
            )

            PolicySection(
                title = "3. Exactitude des Prix",
                content = "Les prix et promotions proviennent des fichiers téléversés ou synchronisés. Kwic-Kart ne garantit pas la disponibilité effective des produits en magasin."
            )

            PolicySection(
                title = "4. Usage Autorisé",
                content = "Toute tentative de rétro-ingénierie ou d'utilisation abusive des services Cloud/Firebase est strictement interdite."
            )
        }
    }
}
