package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.components.PullToRefreshContainer
import com.example.ui.components.SyncStatusIndicator
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCatalogScreen(viewModel: CatalogViewModel) {
    val products by viewModel.currentProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val isRefreshing by viewModel.isServerSyncing.collectAsState()

    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Catalogue", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.syncAllFilesFromServer() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                }
                Spacer(modifier = Modifier.width(6.dp))
                SyncStatusIndicator(viewModel = viewModel)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text(AppStrings.searchPlaceholder(currentLang)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        
        PullToRefreshContainer(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.syncAllFilesFromServer() },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(products, key = { "${it.catalogType}_${it.id}_${it.name}" }) { product ->
                    Card(
                        modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                        onClick = { selectedProduct = product }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Prix: Rs ${"%.2f".format(product.price)} | Coût: Rs ${"%.2f".format(product.cost)}")
                        }
                    }
                }
            }
        }

        selectedProduct?.let { product ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Détails: ${product.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Supermarché: ${product.catalogType} • Catégorie: ${product.category}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
