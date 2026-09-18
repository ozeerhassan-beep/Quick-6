package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    LegalDialog(
        title = "Aide (Help & FAQ)",
        content = """
Kwic-Kart — Centre d'aide

Foire Aux Questions (FAQ)

Comment fonctionne la détection automatique de supermarché ? Lorsque vous chargez un fichier de catalogue (CSV, Excel), Kwic-Kart analyse automatiquement le nom du fichier et son contenu pour assigner directement les produits à la bonne enseigne (ex: Super U, Intermart, Winners, Dreamprice, Way).

Comment rechercher ou scanner un produit ? Vous pouvez utiliser la barre de recherche principale pour trouver des articles par nom, ou scanner le code-barres d'un produit en magasin avec l'appareil photo. Si le produit n'est pas répertorié localement, notre assistant IA (Gemini) tente de retrouver les informations de l'article en ligne.

Les prix sont-ils mis à jour hors-ligne ? Oui. Les produits enregistrés localement sont disponibles hors-ligne. Les mises à jour de prix et la synchronisation multi-fichiers se font automatiquement dès que vous êtes connecté à Internet.

Contact & Support

Email : support@kwickart.app

Version de l'application : 1.0.0
        """.trimIndent(),
        onDismiss = onDismiss
    )
}

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    LegalDialog(
        title = "Confidentialité",
        content = """
Politique de Confidentialité — Kwic-Kart Dernière mise à jour : 2026

Chez Kwic-Kart, nous respectons votre vie privée et nous nous engageons à protéger vos données personnelles.

Données collectées

Informations de compte : Email et identifiant utilisateur lorsque vous activez l'authentification.

Données d'utilisation : Listes de courses, catalogues importés et préférences de comparaison de prix.

Appareil photo : Utilisé uniquement avec votre autorisation pour scanner les codes-barres des produits.

Utilisation des données

Permettre la comparaison de prix en temps réel entre les supermarchés.

Synchroniser vos listes de courses sur vos différents appareils via Firebase.

Améliorer la recherche de produits à l'aide des services d'intelligence artificielle (Google Gemini API).

Protection et partage Vos données ne sont ni vendues ni cédées à des tiers à des fins publicitaires. Elles sont stockées de manière sécurisée sur les serveurs Google Firebase.
        """.trimIndent(),
        onDismiss = onDismiss
    )
}

@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    LegalDialog(
        title = "Conditions",
        content = """
Conditions Générales d'Utilisation — Kwic-Kart

En utilisant l'application Kwic-Kart, vous acceptez les présentes conditions :

1. Service de comparaison de prix Kwic-Kart est un outil d'aide à la décision d'achat et de comparaison de prix. Bien que nous nous efforcions de fournir des prix exacts et à jour issus des catalogues des enseignes, des écarts peuvent survenir en magasin. Kwic-Kart ne garantit pas la disponibilité ni l'exactitude des prix affichés en rayon.

2. Utilisation acceptable Vous vous engagez à ne pas importer de fichiers malveillants, à ne pas tenter de perturber le fonctionnement de l'application ou d'accéder sans autorisation aux données d'autres utilisateurs.

3. Propriété intellectuelle L'interface, le logo, le nom Kwic-Kart ainsi que le code source de l'application sont la propriété exclusive de leur créateur. Les marques et logos des supermarchés appartiennent à leurs propriétaires respectifs.

4. Modification des services Nous nous réservons le droit de modifier ou de suspendre certaines fonctionnalités de l'application à tout moment afin d'améliorer l'expérience utilisateur.
        """.trimIndent(),
        onDismiss = onDismiss
    )
}

@Composable
private fun LegalDialog(title: String, content: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = content, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fermer")
                }
            }
        }
    }
}
