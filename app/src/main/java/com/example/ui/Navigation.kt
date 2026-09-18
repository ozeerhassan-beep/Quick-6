package com.example.ui

import com.example.ui.components.SingleDeviceConflictDialog
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Sync
import com.example.util.WorkSyncStatus
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.components.AppEditionSwitchDialog
import com.example.util.AppEdition
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.ActiveUserDensityChip
import com.example.ui.components.ActiveUserDensityDialog
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ForceResetDialog
import com.example.ui.components.FluidNavigationBar
import com.example.ui.components.NavigationTabItem
import com.example.ui.components.LanguageSelector
import com.example.ui.components.QuickLanguageDialog
import com.example.ui.components.LiquidNavigationBar
import com.example.ui.components.SmartShoppingListDialog
import com.example.ui.components.StoreRouteMapDialog
import com.example.ui.components.SyncStatusIndicator
import com.example.ui.components.WishlistDialog
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.ConnexionScreen
import com.example.ui.screens.DataUsageScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.LoyaltyCardsScreen
import com.example.ui.screens.PersonalizeDialog
import com.example.ui.screens.PersonalizeScreen
import com.example.ui.screens.ParametersScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ProductCatalogScreen
import com.example.ui.screens.ProfitsScreen
import com.example.ui.screens.SyncDashboardScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.FeaturesScreen
import com.example.ui.screens.GoogleBlue
import com.example.util.CatalogSyncManager
import com.example.util.AppLanguage
import com.example.util.AppStrings
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val icon: ImageVector) {
    abstract fun getTitle(lang: AppLanguage): String
    val title: String get() = getTitle(AppLanguage.FRENCH)

    object Products : Screen("products", Icons.Default.Search) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navProducts(lang)
    }
    object Compare : Screen("compare", Icons.AutoMirrored.Filled.CompareArrows) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navCompare(lang)
    }
    object Cart : Screen("cart", Icons.Default.ShoppingCart) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navCart(lang)
    }
    object Profits : Screen("profits", Icons.AutoMirrored.Filled.TrendingUp) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navProfits(lang)
    }
    object LoyaltyCards : Screen("loyalty_cards", Icons.Default.Favorite) {
        override fun getTitle(lang: AppLanguage) = if (lang == AppLanguage.ENGLISH) "Loyalty" else "Fidélité"
    }
    object Parameters : Screen("parameters", Icons.Default.Settings) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navParameters(lang)
    }
    object Features : Screen("features", Icons.Default.AutoAwesome) {
        override fun getTitle(lang: AppLanguage) = if (lang == AppLanguage.ENGLISH) "Features" else "Fonctionnalités"
    }
    object Personalize : Screen("personalize", Icons.Default.Palette) {
        override fun getTitle(lang: AppLanguage) = if (lang == AppLanguage.ENGLISH) "Personalise" else "Personnaliser"
    }
    object Import : Screen("import", Icons.Default.CloudUpload) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navImport(lang)
    }
    object Profile : Screen("profile", Icons.Default.Person) {
        override fun getTitle(lang: AppLanguage) = if (lang == AppLanguage.ENGLISH) "Profile" else "Mon Profil"
    }
    object AdminDashboard : Screen("admin_dashboard", Icons.Default.SupervisorAccount) {
        override fun getTitle(lang: AppLanguage) = if (lang == AppLanguage.ENGLISH) "Admin Dashboard" else "Tableau de Bord Admin"
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatalogManagerNavHost(viewModel: CatalogViewModel) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val userTier by viewModel.userTier.collectAsState()
    val savedEmail by viewModel.googleAccountEmail.collectAsState()
    val savedName by viewModel.googleAccountName.collectAsState()
    val isSignedIn by viewModel.isSignedInWithGoogle.collectAsState()
    val singleDeviceConflict by viewModel.singleDeviceConflictState.collectAsState()

    val cartItems by viewModel.cartItems.collectAsState()
    val totalCartCount = cartItems.sumOf { it.quantity }

    val comparedProducts by viewModel.comparedProducts.collectAsState()
    val compareCount = comparedProducts.size

    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val wishlistCount = wishlistItems.size
    val syncState by CatalogSyncManager.syncState.collectAsState()
    val hasCompletedWelcome by viewModel.hasCompletedWelcome.collectAsState()

    var showAccountLoginModal by remember { mutableStateOf(false) }
    var showUserProfileModal by remember { mutableStateOf(false) }
    var showAdminDashboardModal by remember { mutableStateOf(false) }
    var showStyleModal by remember { mutableStateOf(false) }
    var showWishlistModal by remember { mutableStateOf(false) }
    var showSmartShoppingListModal by remember { mutableStateOf(false) }
    var showStoreRouteMapModal by remember { mutableStateOf(false) }
    var showSyncDashboardModal by remember { mutableStateOf(false) }
    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var showLanguageModal by remember { mutableStateOf(false) }
    var showDataUsageModal by remember { mutableStateOf(false) }
    var showActiveUserDensityDialog by remember { mutableStateOf(false) }
    var showAppEditionSwitchModal by remember { mutableStateOf(false) }
    val currentEdition by viewModel.appEdition.collectAsState()
    val navBarStyle by viewModel.navBarStyleOption.collectAsState()


    // Display Welcome & Onboarding Screen on first launch or if not completed & not signed in
    if (!hasCompletedWelcome && !isSignedIn) {
        WelcomeScreen(
            onGetStartedClick = {
                showAccountLoginModal = true
            },
            onLoginClick = {
                showAccountLoginModal = true
            },
            onGuestClick = {
                viewModel.setCompletedWelcome(true)
            }
        )

        // Full Screen Google / Firebase Connexion Modal from Welcome Screen
        if (showAccountLoginModal) {
            Dialog(
                onDismissRequest = { showAccountLoginModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnexionScreen(
                        viewModel = viewModel,
                        onCloseModal = {
                            showAccountLoginModal = false
                        }
                    )
                }
            }
        }
        return
    }

    // Bottom Navigation Screens
    val isFeaturesEnabled by viewModel.isFeaturesEnabled.collectAsState()
    val isPriceCompareEnabled by viewModel.isPriceCompareEnabled.collectAsState(initial = true)
    val isSmartCartEnabled by viewModel.isSmartCartEnabled.collectAsState(initial = true)
    val isSavingsAnalyticsEnabled by viewModel.isSavingsAnalyticsEnabled.collectAsState(initial = true)
    val isLoyaltyCardsEnabled by viewModel.isLoyaltyCardsEnabled.collectAsState(initial = true)

    val bottomNavScreens = remember(
        currentEdition,
        userRole,
        userTier,
        isFeaturesEnabled,
        isPriceCompareEnabled,
        isSmartCartEnabled,
        isSavingsAnalyticsEnabled,
        isLoyaltyCardsEnabled
    ) {
        val list = mutableListOf<Screen>()
        if (currentEdition == com.example.util.AppEdition.ADMIN) {
            list.add(Screen.AdminDashboard)
            list.add(Screen.Products)
            list.add(Screen.Import)
            list.add(Screen.Profits)
            list.add(Screen.Parameters)
        } else {
            list.add(Screen.Products)
            if (isPriceCompareEnabled) {
                list.add(Screen.Compare)
            }
            if (isSmartCartEnabled) {
                list.add(Screen.Cart)
            }
            if (isLoyaltyCardsEnabled) {
                list.add(Screen.LoyaltyCards)
            }
            list.add(Screen.Parameters)
        }
        list.toList()
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var showProductWelcomeDialog by remember { mutableStateOf(false) }

    // Live sync states for dynamic Kwic-Kart title/logo indicator on Catalogue screen
    val workSyncStatus by CatalogSyncManager.workSyncStatus.collectAsState()
    val isOnline by CatalogSyncManager.isOnline.collectAsState()
    val isServerSyncing by viewModel.isServerSyncing.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()

    val isActivelySyncing = syncState.isRunning || isServerSyncing || isFirestoreSyncing || workSyncStatus == WorkSyncStatus.SYNCING
    val isSyncError = syncState.isError || workSyncStatus == WorkSyncStatus.ERROR

    // Dynamic sync colors based on real-time sync state:
    // Green (Color.Green / EmeraldSuccess): Complete & up to date
    // Orange / Amber: Syncing in progress
    // Red / Grey: Offline mode or sync error
    val dynamicBrandColor = when {
        isActivelySyncing -> Color(0xFFF59E0B) // Amber / Warning
        !isOnline -> Color(0xFF9CA3AF)        // Grey (Offline)
        isSyncError -> Color(0xFFEF4444)      // Red (Error)
        else -> Color(0xFF10B981)             // Green (Success / OK / Online)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val isProductScreen = currentDestination?.route == Screen.Products.route
                    if (showProductWelcomeDialog && isProductScreen) {
                        AlertDialog(
                            onDismissRequest = { showProductWelcomeDialog = false },
                            confirmButton = {
                                Button(onClick = { showProductWelcomeDialog = false }) {
                                    Text("Fermer")
                                }
                            },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_kwic_kart_logo_1789205313486),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Kwic-Kart", fontWeight = FontWeight.Bold)
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = AppStrings.productsBannerTitle(currentLang),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = AppStrings.productsBannerSubtitle(currentLang),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (isProductScreen) {
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showProductWelcomeDialog = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("kwic_kart_title_header")
                        } else {
                            Modifier
                        }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_kwic_kart_logo_1789205313486),
                                    contentDescription = "Kwic-Kart",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentEdition == AppEdition.ADMIN) "KwickArt Admin" else "Kwic-Kart",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isProductScreen) dynamicBrandColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentEdition == AppEdition.ADMIN) "Console Back-Office" else "Shopping & Scanner",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Edition Switcher Pill Chip (Client vs Admin APK) - Only show if ADMIN to keep Client view clean
                    if (currentEdition == AppEdition.ADMIN) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFD97706).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { showAppEditionSwitchModal = true }
                                .testTag("app_edition_switcher_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupervisorAccount,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ADMIN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // Account & Live User Density Badge in Top Bar (Exclusive to Catalogue/Products screen)
                    val isProductScreen = currentDestination?.route == Screen.Products.route

                    if (isProductScreen) {
                        val currentUserName = remember(isSignedIn, savedName, savedEmail) {
                            if (isSignedIn && !savedName.isNullOrBlank()) {
                                savedName!!
                            } else if (isSignedIn && !savedEmail.isNullOrBlank()) {
                                savedEmail!!.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            } else if (!savedName.isNullOrBlank()) {
                                savedName!!
                            } else if (!savedEmail.isNullOrBlank()) {
                                savedEmail!!.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            } else {
                                "Hassan"
                            }
                        }

                        // 1. Live Active User Density Badge/Chip
                        ActiveUserDensityChip(
                            onClick = { showActiveUserDensityDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // 2. User Profile Avatar & Name Chip
                        Surface(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .clickable { showUserProfileModal = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = currentUserName.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentUserName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            val selectedIndex = bottomNavScreens.indexOfFirst { screen ->
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }.coerceAtLeast(0)

            val navTabItems = remember(bottomNavScreens, currentLang, totalCartCount, compareCount) {
                bottomNavScreens.mapIndexed { index, screen ->
                    val badge = when (screen) {
                        Screen.Cart -> totalCartCount
                        Screen.Compare -> compareCount
                        else -> 0
                    }
                    NavigationTabItem(
                        index = index,
                        label = screen.getTitle(currentLang),
                        activeIcon = screen.icon,
                        inactiveIcon = screen.icon,
                        testTag = "bottom_tab_${screen.route}",
                        badgeCount = badge
                    )
                }
            }

            if (navBarStyle == com.example.ui.NavBarStyleOption.FLUID) {
                FluidNavigationBar(
                    items = navTabItems,
                    selectedTabIndex = selectedIndex,
                    onTabSelected = { index ->
                        val screen = bottomNavScreens.getOrNull(index)
                        if (screen != null) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            } else {
                val badgeMap = remember(totalCartCount, compareCount) {
                    mapOf(
                        Screen.Cart to totalCartCount,
                        Screen.Compare to compareCount
                    )
                }
                LiquidNavigationBar(
                    screens = bottomNavScreens,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index ->
                        val screen = bottomNavScreens.getOrNull(index)
                        if (screen != null) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    badgeCounts = badgeMap,
                    titleProvider = { it.getTitle(currentLang) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Products.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(250)) + slideInHorizontally(animationSpec = tween(250)) { it / 4 } },
            exitTransition = { fadeOut(animationSpec = tween(250)) + slideOutHorizontally(animationSpec = tween(250)) { -it / 4 } },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) + slideInHorizontally(animationSpec = tween(250)) { -it / 4 } },
            popExitTransition = { fadeOut(animationSpec = tween(250)) + slideOutHorizontally(animationSpec = tween(250)) { it / 4 } }
        ) {
            composable(Screen.Products.route) {
                ProductsScreen(viewModel = viewModel)
            }
            composable(Screen.Features.route) {
                FeaturesScreen(viewModel = viewModel)
            }
            composable(Screen.Compare.route) {
                CompareScreen(
                    viewModel = viewModel,
                    onNavigateToProducts = {
                        navController.navigate(Screen.Products.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(viewModel = viewModel)
            }
            composable(Screen.Profits.route) {
                ProfitsScreen(viewModel = viewModel)
            }
            composable(Screen.LoyaltyCards.route) {
                LoyaltyCardsScreen(viewModel = viewModel)
            }
            composable(Screen.Parameters.route) {
                ParametersScreen(
                    viewModel = viewModel,
                    onNavigateToAdminDashboard = {
                        showAdminDashboardModal = true
                    }
                )
            }
            composable(Screen.Personalize.route) {
                PersonalizeScreen(viewModel = viewModel)
            }
            composable(Screen.Import.route) {
                ImportScreen(viewModel = viewModel)
            }
            composable(Screen.Profile.route) {
                UserProfileScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToLogin = {
                        showAccountLoginModal = true
                    },
                    onNavigateToAdminDashboard = {
                        showAdminDashboardModal = true
                    }
                )
            }
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Admin Dashboard Full-Screen Dialog
        if (showAdminDashboardModal) {
            Dialog(
                onDismissRequest = { showAdminDashboardModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminDashboardScreen(
                        viewModel = viewModel,
                        onBack = { showAdminDashboardModal = false }
                    )
                }
            }
        }

        // User Profile Full-Screen Dialog
        if (showUserProfileModal) {
            Dialog(
                onDismissRequest = { showUserProfileModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UserProfileScreen(
                        viewModel = viewModel,
                        onBack = { showUserProfileModal = false },
                        onNavigateToLogin = {
                            showUserProfileModal = false
                            showAccountLoginModal = true
                        },
                        onNavigateToAdminDashboard = {
                            showUserProfileModal = false
                            showAdminDashboardModal = true
                        }
                    )
                }
            }
        }

        // Style Customization Popup Modal Dialog
        if (showStyleModal) {
            PersonalizeDialog(
                viewModel = viewModel,
                onDismiss = { showStyleModal = false }
            )
        }

        // Wishlist Modal Dialog
        if (showWishlistModal) {
            WishlistDialog(
                viewModel = viewModel,
                onDismiss = { showWishlistModal = false }
            )
        }

        // Smart Shopping List Modal Dialog (Lowest Available Store Price Engine)
        if (showSmartShoppingListModal) {
            SmartShoppingListDialog(
                viewModel = viewModel,
                onDismiss = { showSmartShoppingListModal = false }
            )
        }

        // Smart Supermarket Route Map Modal Dialog (2D Store Floorplan & TSP Pathfinding)
        if (showStoreRouteMapModal) {
            StoreRouteMapDialog(
                viewModel = viewModel,
                onDismiss = { showStoreRouteMapModal = false }
            )
        }

        // Sync Dashboard Modal Dialog (WorkManager Status & Manual Sync)
        if (showSyncDashboardModal) {
            Dialog(
                onDismissRequest = { showSyncDashboardModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SyncDashboardScreen(
                        viewModel = viewModel,
                        onBack = { showSyncDashboardModal = false }
                    )
                }
            }
        }

        // Full Screen Google Login Modal Dialog
        if (showAccountLoginModal) {
            Dialog(
                onDismissRequest = { showAccountLoginModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnexionScreen(
                        viewModel = viewModel,
                        onCloseModal = { showAccountLoginModal = false }
                    )
                }
            }
        }

        // Active User Density Detailed Pop-Up Dialog
        if (showActiveUserDensityDialog) {
            ActiveUserDensityDialog(
                onDismissRequest = { showActiveUserDensityDialog = false }
            )
        }

        // Global ML Kit Barcode Scanner Modal Dialog
        if (showBarcodeScannerModal) {
            BarcodeScannerDialog(
                viewModel = viewModel,
                onDismiss = { showBarcodeScannerModal = false }
            )
        }

        // Global Quick Language Switcher Modal Dialog
        if (showLanguageModal) {
            QuickLanguageDialog(
                selectedLanguage = currentLang,
                onLanguageSelected = { lang ->
                    viewModel.setAppLanguage(lang)
                    showLanguageModal = false
                },
                onDismiss = { showLanguageModal = false }
            )
        }

        // Data Usage & Local Storage Explanation Modal Dialog
        if (showDataUsageModal) {
            Dialog(
                onDismissRequest = { showDataUsageModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DataUsageScreen(
                        viewModel = viewModel,
                        onBack = { showDataUsageModal = false }
                    )
                }
            }
        }

        // Global Force Reset Progress Pop-up Dialog
        ForceResetDialog(viewModel = viewModel)

        // Single Device Conflict Dialog
        singleDeviceConflict?.let { conflict ->
            SingleDeviceConflictDialog(
                conflict = conflict,
                viewModel = viewModel,
                onDismiss = {
                    viewModel.singleDeviceConflictState.value = null
                }
            )
        }

        // App Edition Switcher (Client vs Admin APK) Dialog
        if (showAppEditionSwitchModal) {
            AppEditionSwitchDialog(
                currentEdition = currentEdition,
                onEditionSelected = { newEdition ->
                    viewModel.setAppEdition(newEdition)
                    if (newEdition == AppEdition.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Screen.Products.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
                onDismiss = { showAppEditionSwitchModal = false }
            )
        }
    }
}

