package com.example.util

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val subtitle: String
) {
    FRENCH(
        code = "fr",
        displayName = "Français",
        nativeName = "Français",
        flagEmoji = "🇫🇷",
        subtitle = "Langue française standard"
    ),
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        flagEmoji = "🇬🇧",
        subtitle = "English (UK / US)"
    ),
    CREOLE(
        code = "mfe",
        displayName = "Kreol Morisien",
        nativeName = "Kreol Morisien",
        flagEmoji = "🇲🇺",
        subtitle = "Lang kreol Repiblik Moris"
    );

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: FRENCH
        }
    }
}

object AppStrings {
    // Personalize Screen Strings
    fun personalizeTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Personnalisation & Style"
        AppLanguage.ENGLISH -> "Personalization & Style"
        AppLanguage.CREOLE -> "Personnalizasion & Stil"
    }

    fun personalizeSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Customisez la langue, les couleurs, les polices, les arrondis et la disposition."
        AppLanguage.ENGLISH -> "Customize language, colors, fonts, shapes, and layout density."
        AppLanguage.CREOLE -> "Sanz langaz, bann kouler, stil lekritir, kwin bann kart ek lorganizasion."
    }

    // Language Section
    fun languageSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Langue de l'Application"
        AppLanguage.ENGLISH -> "Application Language"
        AppLanguage.CREOLE -> "Langaz Laplikasion"
    }

    fun languageSelectedToast(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Langue changée en Français 🇫🇷"
        AppLanguage.ENGLISH -> "Language changed to English 🇬🇧"
        AppLanguage.CREOLE -> "Langaz inn sanze pou Kreol Morisien 🇲🇺"
    }

    // Live Preview
    fun livePreview(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Aperçu en Direct"
        AppLanguage.ENGLISH -> "Live Preview"
        AppLanguage.CREOLE -> "Laperersu an Direk"
    }

    fun sampleProductName(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Riz Basmati Superiore 5kg"
        AppLanguage.ENGLISH -> "Basmati Superiore Rice 5kg"
        AppLanguage.CREOLE -> "Diri Basmati Superiore 5kg"
    }

    fun sampleCategory(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Épicerie Fine • Marque Laila"
        AppLanguage.ENGLISH -> "Fine Grocery • Laila Brand"
        AppLanguage.CREOLE -> "Bann Grosi Fin • Mark Laila"
    }

    fun marginLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Marge: +Rs 75.00"
        AppLanguage.ENGLISH -> "Profit Margin: +Rs 75.00"
        AppLanguage.CREOLE -> "Bénéfis: +Rs 75.00"
    }

    fun addButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ajouter"
        AppLanguage.ENGLISH -> "Add"
        AppLanguage.CREOLE -> "Azoute"
    }

    // Theme Mode Section
    fun themeModeTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Mode de Thème"
        AppLanguage.ENGLISH -> "Theme Mode"
        AppLanguage.CREOLE -> "Mod Tem"
    }

    fun themeLight(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Clair"
        AppLanguage.ENGLISH -> "Light"
        AppLanguage.CREOLE -> "Kler"
    }

    fun themeDark(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Sombre"
        AppLanguage.ENGLISH -> "Dark"
        AppLanguage.CREOLE -> "Som"
    }

    fun themeSystem(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Système"
        AppLanguage.ENGLISH -> "System"
        AppLanguage.CREOLE -> "Sistem"
    }

    // Primary Color Section
    fun primaryColorTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Palette & Couleur Principale"
        AppLanguage.ENGLISH -> "Color Palette & Primary Hue"
        AppLanguage.CREOLE -> "Palet & Kouler Prinsipal"
    }

    // Font Style Section
    fun fontStyleTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Style de Police"
        AppLanguage.ENGLISH -> "Font Style"
        AppLanguage.CREOLE -> "Stil Lekritir"
    }

    fun fontSampleText(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Aa Bb Cc 123 - Exemple de texte"
        AppLanguage.ENGLISH -> "Aa Bb Cc 123 - Sample typography"
        AppLanguage.CREOLE -> "Aa Bb Cc 123 - Legzanp lekritir"
    }

    // Text Scale Section
    fun textScaleTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Échelle du Texte"
        AppLanguage.ENGLISH -> "Text Scale"
        AppLanguage.CREOLE -> "Grandeur Lekritir"
    }

    fun searchPlaceholder(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Rechercher un produit"
        AppLanguage.ENGLISH -> "Search for a product"
        AppLanguage.CREOLE -> "Resers enn produi"
    }

    // Shapes Section
    fun shapesTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Arrondi des Formes & Cartes"
        AppLanguage.ENGLISH -> "Card & Shape Corner Radius"
        AppLanguage.CREOLE -> "Kwin bann Kart & Form"
    }

    fun shapesSubtitle(lang: AppLanguage, radius: Int): String = when (lang) {
        AppLanguage.FRENCH -> "Applique des coins arrondis de ${radius}dp à tous les composants"
        AppLanguage.ENGLISH -> "Applies ${radius}dp rounded corners to all UI cards"
        AppLanguage.CREOLE -> "Aplik kwin ${radius}dp lor tou bann kart ek bwat"
    }

    // Layout Section
    fun layoutTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Disposition du Catalogue"
        AppLanguage.ENGLISH -> "Catalog Layout"
        AppLanguage.CREOLE -> "Lorganizasion Katalog"
    }

    fun layoutGridSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Affichage côte à côte en 2 colonnes"
        AppLanguage.ENGLISH -> "Side-by-side 2-column grid layout"
        AppLanguage.CREOLE -> "Afisaz kot-a-kot lor 2 kolonn"
    }

    fun layoutListSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Affichage en liste verticale détaillée"
        AppLanguage.ENGLISH -> "Detailed vertical list layout"
        AppLanguage.CREOLE -> "Afisaz an lalis vertikal detaye"
    }

    // Reset button
    fun resetButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Réinitialiser les paramètres par défaut"
        AppLanguage.ENGLISH -> "Reset to default settings"
        AppLanguage.CREOLE -> "Remet tou reglaz par defo"
    }

    fun resetToast(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Paramètres de style et langue réinitialisés"
        AppLanguage.ENGLISH -> "Style and language settings reset"
        AppLanguage.CREOLE -> "Bann reglaz stil ek langaz inn remet par defo"
    }

    fun closeButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Fermer"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.CREOLE -> "Ferme"
    }

    // Navigation Tab Titles
    fun navProducts(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Produits"
        AppLanguage.ENGLISH -> "Products"
        AppLanguage.CREOLE -> "Produi"
    }

    fun navCompare(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Comparer"
        AppLanguage.ENGLISH -> "Compare"
        AppLanguage.CREOLE -> "Konpare"
    }

    fun navCart(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Panier"
        AppLanguage.ENGLISH -> "Cart"
        AppLanguage.CREOLE -> "Pannie"
    }

    fun navProfits(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Profits"
        AppLanguage.ENGLISH -> "Profits"
        AppLanguage.CREOLE -> "Bénéfis"
    }

    fun navStyle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Style"
        AppLanguage.ENGLISH -> "Style"
        AppLanguage.CREOLE -> "Laparans"
    }

    fun navImport(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Importer"
        AppLanguage.ENGLISH -> "Import"
        AppLanguage.CREOLE -> "Inporte"
    }

    fun navParameters(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Paramètres"
        AppLanguage.ENGLISH -> "Parameters"
        AppLanguage.CREOLE -> "Reglaz"
    }

    fun navWishlist(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Favoris"
        AppLanguage.ENGLISH -> "Wishlist"
        AppLanguage.CREOLE -> "Prefere"
    }

    // Products Screen Banner & Greetings
    fun welcomeBannerTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Bienvenue sur Kwic-Kart\nArticles de supermarché & Comparateur de prix"
        AppLanguage.ENGLISH -> "Welcome to Kwic-Kart\nSupermarket Items & Price Comparison"
        AppLanguage.CREOLE -> "Binvini lor Kwic-Kart\nLartik Sipèrmarse & Konparater Pri"
    }

    fun welcomeBannerSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Comparez les prix des catalogues, suivez les promotions et économisez sur vos courses."
        AppLanguage.ENGLISH -> "Compare catalog prices, track promotions, and save on your groceries."
        AppLanguage.CREOLE -> "Konpar pri katalog, swiv bann promo ek fer lekonomi lor ou bann komision."
    }

    // Common Action Strings
    fun searchProductsPlaceholder(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Rechercher un produit, marque..."
        AppLanguage.ENGLISH -> "Search product, brand..."
        AppLanguage.CREOLE -> "Resers enn produi, mark..."
    }

    fun allCategory(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Tous"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.CREOLE -> "Tou"
    }

    fun onlyPromotionsLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Promotions"
        AppLanguage.ENGLISH -> "Discounts"
        AppLanguage.CREOLE -> "Bann Promo"
    }

    fun sortLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Trier par"
        AppLanguage.ENGLISH -> "Sort by"
        AppLanguage.CREOLE -> "Triye par"
    }

    fun allCatalogs(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Tous les catalogues"
        AppLanguage.ENGLISH -> "All Catalogs"
        AppLanguage.CREOLE -> "Tou Katalog"
    }

    fun productsCountLabel(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.FRENCH -> "$count article(s) trouvé(s)"
        AppLanguage.ENGLISH -> "$count item(s) found"
        AppLanguage.CREOLE -> "$count lartik trouve"
    }

    fun emptyCatalogTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Aucun produit pour le moment"
        AppLanguage.ENGLISH -> "No products available yet"
        AppLanguage.CREOLE -> "Péna okenn produi pou lemoman"
    }

    fun emptyCatalogSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Importez un catalogue ou synchronisez avec Firebase pour commencer."
        AppLanguage.ENGLISH -> "Import a catalog or sync with Firebase to get started."
        AppLanguage.CREOLE -> "Inport enn katalog ou sinkroniz avek Firebase pou koumanse."
    }

    fun addToCart(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ajouter au Panier"
        AppLanguage.ENGLISH -> "Add to Cart"
        AppLanguage.CREOLE -> "Met dan Pannie"
    }

    fun addedToCartToast(lang: AppLanguage, name: String): String = when (lang) {
        AppLanguage.FRENCH -> "$name ajouté au panier"
        AppLanguage.ENGLISH -> "$name added to cart"
        AppLanguage.CREOLE -> "$name inn azoute dan pannie"
    }

    fun compareButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Comparer"
        AppLanguage.ENGLISH -> "Compare"
        AppLanguage.CREOLE -> "Konpare"
    }

    fun priceLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prix"
        AppLanguage.ENGLISH -> "Price"
        AppLanguage.CREOLE -> "Pri"
    }

    fun unitLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Unité"
        AppLanguage.ENGLISH -> "Unit"
        AppLanguage.CREOLE -> "Inite"
    }

    fun categoryLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Catégorie"
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.CREOLE -> "Kategori"
    }

    fun brandLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Marque"
        AppLanguage.ENGLISH -> "Brand"
        AppLanguage.CREOLE -> "Mark"
    }

    // Cart Screen Strings
    fun cartEmptyTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Votre panier est vide"
        AppLanguage.ENGLISH -> "Your cart is empty"
        AppLanguage.CREOLE -> "Ou pannie vid"
    }

    fun cartEmptySubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Explorez les catalogues et ajoutez des articles."
        AppLanguage.ENGLISH -> "Browse catalogs and add products to your cart."
        AppLanguage.CREOLE -> "Gét bann katalog ek azout bann lartik."
    }

    fun cartTotal(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Total Panier"
        AppLanguage.ENGLISH -> "Cart Total"
        AppLanguage.CREOLE -> "Total Pannie"
    }

    fun checkoutButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Enregistrer la Vente / Valider"
        AppLanguage.ENGLISH -> "Record Sale / Checkout"
        AppLanguage.CREOLE -> "Anrezistre Vant / Valide"
    }

    fun clearCartButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Vider le Panier"
        AppLanguage.ENGLISH -> "Clear Cart"
        AppLanguage.CREOLE -> "Vid Pannie"
    }

    fun totalItems(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.FRENCH -> "$count article(s)"
        AppLanguage.ENGLISH -> "$count item(s)"
        AppLanguage.CREOLE -> "$count lartik"
    }

    // Compare Screen Strings
    fun compareScreenTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Comparateur Multi-Supermarchés"
        AppLanguage.ENGLISH -> "Multi-Supermarket Comparator"
        AppLanguage.CREOLE -> "Konparater Bann Sipèrmarse"
    }

    fun compareEmptyTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Aucun produit en comparaison"
        AppLanguage.ENGLISH -> "No products to compare"
        AppLanguage.CREOLE -> "Péna produi pou konpare"
    }

    fun bestPriceTag(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Meilleur Prix"
        AppLanguage.ENGLISH -> "Best Deal"
        AppLanguage.CREOLE -> "Meyer Pri"
    }

    fun priceDifference(lang: AppLanguage, diff: Double): String = when (lang) {
        AppLanguage.FRENCH -> "Économie max: Rs ${String.format("%.2f", diff)}"
        AppLanguage.ENGLISH -> "Max savings: Rs ${String.format("%.2f", diff)}"
        AppLanguage.CREOLE -> "Bann lekonomi: Rs ${String.format("%.2f", diff)}"
    }

    // Profits Screen Strings
    fun profitsScreenTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Marge & Analyse des Ventes"
        AppLanguage.ENGLISH -> "Profit & Sales Analytics"
        AppLanguage.CREOLE -> "Bénéfis & Bann Vant"
    }

    fun totalProfitLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Bénéfice Net Total"
        AppLanguage.ENGLISH -> "Total Net Profit"
        AppLanguage.CREOLE -> "Total Bénéfis Net"
    }

    fun totalRevenueLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Chiffre d'Affaires"
        AppLanguage.ENGLISH -> "Total Revenue"
        AppLanguage.CREOLE -> "Total Larzan Rantré"
    }

    // Settings Screen Unified Strings
    fun parametersMainTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Paramètres & Préférences"
        AppLanguage.ENGLISH -> "Parameters & Preferences"
        AppLanguage.CREOLE -> "Reglaz & Bann Preferans"
    }

    fun parametersMainSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Personnalisation, Langue, Sync Cloud, Scanner & Données"
        AppLanguage.ENGLISH -> "Personalization, Language, Cloud Sync, Scanner & Data"
        AppLanguage.CREOLE -> "Laparans, Langaz, Sinkro Cloud, Skaner & Bann Done"
    }

    fun parametersSectionAppearance(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "PERSONNALISATION & STYLE"
        AppLanguage.ENGLISH -> "APPEARANCE & PERSONALIZATION"
        AppLanguage.CREOLE -> "LAPARANS & PERSONNALIZASION"
    }

    fun parametersSectionSync(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "SYNCHRONISATION CLOUD FIRESTORE"
        AppLanguage.ENGLISH -> "CLOUD FIRESTORE SYNCHRONIZATION"
        AppLanguage.CREOLE -> "SINKRONIZASION CLOUD FIRESTORE"
    }

    fun parametersSectionScanner(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "SCANNER DE CODE-BARRES (ML KIT)"
        AppLanguage.ENGLISH -> "BARCODE SCANNER (ML KIT)"
        AppLanguage.CREOLE -> "SKANER KOD-BAR (ML KIT)"
    }

    fun parametersSectionDatabase(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "BASE DE DONNÉES LOCALE ROOM SQLITE"
        AppLanguage.ENGLISH -> "ROOM SQLITE LOCAL DATABASE"
        AppLanguage.CREOLE -> "BAZ DONE LOKAL ROOM SQLITE"
    }

    fun parametersSectionAccount(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "COMPTE & SÉCURITÉ"
        AppLanguage.ENGLISH -> "ACCOUNT & SECURITY"
        AppLanguage.CREOLE -> "KONT & SEKIRITE"
    }

    fun parametersSectionAbout(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "À PROPOS DE KWIC-KART"
        AppLanguage.ENGLISH -> "ABOUT KWIC-KART"
        AppLanguage.CREOLE -> "LOR KWIC-KART"
    }

    // Quick Language Selector
    fun changeLanguageTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Changer la Langue de l'App"
        AppLanguage.ENGLISH -> "Change App Language"
        AppLanguage.CREOLE -> "Sanz Langaz Laplikasion"
    }

    fun changeLanguageSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Sélectionnez votre langue préférée pour toute l'interface"
        AppLanguage.ENGLISH -> "Select your preferred language across the entire application"
        AppLanguage.CREOLE -> "Swazi ou langaz prefere pou tou linterfas laplikasion"
    }

    // Products Screen Banner & Search Strings
    fun productsBannerTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Bienvenue sur Kwic-Kart\nArticles de Supermarché et Comparaison des Prix"
        AppLanguage.ENGLISH -> "Welcome to Kwic-Kart\nYour Supermarket Items and Price Comparison"
        AppLanguage.CREOLE -> "Binvini lor Kwic-Kart\nBann lartik sipèrmarse ek Konparater Pri"
    }

    fun productsBannerSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Comparez les prix des catalogues, suivez les promotions et économisez sur vos courses."
        AppLanguage.ENGLISH -> "Compare catalog prices, track promotions, and save on your groceries."
        AppLanguage.CREOLE -> "Konpar pri katalog, swiv bann promo ek fer lekonomi lor ou komision."
    }

    fun allCategoriesOption(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Tous"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.CREOLE -> "Tou"
    }
}

