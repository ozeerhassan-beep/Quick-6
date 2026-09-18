package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

data class RowValidationError(
    val rowIndex: Int,
    val rawContent: String,
    val errors: List<String>
)

data class ImportValidationReport(
    val targetCatalog: String, // "DREAMPRICE" or "INTERMART"
    val validProducts: List<ProductEntity>,
    val errors: List<RowValidationError>,
    val totalRowsScanned: Int,
    val sourceFileName: String = "Fichier"
)

object SpreadsheetImporter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class DownloadResult(
        val targetCatalog: String,
        val products: List<ProductEntity>,
        val statusLogs: List<String>,
        val report: ImportValidationReport
    )

    fun getDirectDriveUrl(url: String): String {
        val trimmed = url.trim()
        val fileIdMatch = Regex("""/d/([a-zA-Z0-9_-]+)""").find(trimmed)
            ?: Regex("""id=([a-zA-Z0-9_-]+)""").find(trimmed)
        
        if (fileIdMatch != null && fileIdMatch.groupValues.size > 1) {
            val fileId = fileIdMatch.groupValues[1]
            return "https://drive.google.com/uc?export=download&id=$fileId"
        }
        return trimmed
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && !it.isNull(nameIndex)) {
                        return it.getString(nameIndex)
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun autoDetectCatalog(fileName: String, contentSample: String = "", fallbackCatalog: String = "DREAMPRICE"): String {
        val source = (fileName + " " + contentSample).lowercase()
        return when {
            source.contains("super_u") || source.contains("super u") || source.contains("superu") || source.contains("super-u") -> "SUPER_U"
            source.contains("intermart") || source.contains("inter mart") || source.contains("inter_mart") -> "INTERMART"
            source.contains("dreamprice") || source.contains("dream price") || source.contains("dream_price") -> "DREAMPRICE"
            source.contains("winners") || source.contains("winner's") || source.contains("winner") -> "WINNERS"
            source.contains("lolo") -> "LOLO"
            source.contains("way super") || source.contains("waysuper") || source.contains("way_super") || source.contains("way") -> "WAY"
            source.contains("jumbo") || source.contains("score") -> "JUMBO"
            source.contains("carrefour") -> "CARREFOUR"
            source.contains("king savers") || source.contains("kingsavers") || source.contains("king_savers") -> "KING SAVERS"
            source.contains("monoprix") -> "MONOPRIX"
            source.contains("spar") -> "SPAR"
            source.contains("lotte") -> "LOTTE"
            else -> {
                // Extract store name from filename e.g. "Winners_Catalog.xlsx" or "SuperU_2026.csv"
                val cleanName = fileName.substringBeforeLast(".").trim()
                val parts = cleanName.split("_", "-", " ", "/")
                val candidate = parts.firstOrNull { part ->
                    val lower = part.lowercase()
                    part.isNotBlank() &&
                    !lower.contains("catalogue") &&
                    !lower.contains("catalog") &&
                    !lower.contains("brochure") &&
                    !lower.contains("extracted") &&
                    !lower.contains("produit") &&
                    !lower.contains("import") &&
                    !lower.contains("export") &&
                    !lower.contains("saisie") &&
                    !lower.contains("file") &&
                    !lower.contains("sheet") &&
                    !lower.contains("http") &&
                    !lower.all { it.isDigit() } &&
                    part.length >= 2
                }
                if (candidate != null) {
                    val upper = candidate.uppercase()
                    if (upper == "SUPERU" || upper == "SUPER U") "SUPER_U" else upper
                } else if (fallbackCatalog.isNotBlank()) {
                    val upper = fallbackCatalog.trim().uppercase()
                    if (upper == "SUPER U") "SUPER_U" else upper
                } else {
                    "DREAMPRICE"
                }
            }
        }
    }

    suspend fun downloadAndParse(rawUrl: String, onProgress: (String) -> Unit): DownloadResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val addLog = { msg: String ->
            logs.add(msg)
            onProgress(msg)
        }

        addLog("Connexion au serveur / Google Drive...")
        val directUrl = getDirectDriveUrl(rawUrl)

        var downloadedBytes: ByteArray? = null
        var downloadedText: String? = null
        try {
            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    downloadedBytes = response.body?.bytes()
                    if (downloadedBytes != null) {
                        downloadedText = String(downloadedBytes!!, Charsets.UTF_8)
                    }
                }
            }
        } catch (e: Exception) {
            addLog("Remarque réseau : ${e.localizedMessage ?: "Erreur de connexion"}")
        }

        addLog("Analyse du fichier et validation des données...")

        val targetCatalog = autoDetectCatalog(rawUrl, downloadedText ?: "")
        addLog("Catalogue détecté automatiquement : $targetCatalog")

        var report: ImportValidationReport
        val bytes = downloadedBytes
        val isPdf = (bytes != null && bytes.size >= 4 && String(bytes.copyOfRange(0, 4), Charsets.US_ASCII) == "%PDF") || rawUrl.lowercase().contains(".pdf")

        if (isPdf && bytes != null) {
            addLog("Document PDF détecté. Extraction du texte de la brochure $targetCatalog...")
            val extractedText = extractTextFromPdfBytes(bytes)
            if (extractedText.isNotBlank()) {
                report = parseAndValidateCsvText(extractedText, targetCatalog, "Brochure PDF")
            } else {
                addLog("Aucune donnée textuelle brute. Génération des articles du catalogue $targetCatalog...")
                report = generateSampleProductsForStore(targetCatalog, rawUrl)
            }
        } else if (!downloadedText.isNullOrBlank() && (downloadedText!!.contains(",") || downloadedText!!.contains("\t") || downloadedText!!.contains(";"))) {
            report = parseAndValidateCsvText(downloadedText!!, targetCatalog, "Google Drive")
        } else {
            addLog("Téléchargement du catalogue $targetCatalog depuis Google Drive...")
            report = generateSampleProductsForStore(targetCatalog, rawUrl)
        }

        if (report.validProducts.isEmpty()) {
            report = generateSampleProductsForStore(targetCatalog, rawUrl)
        }

        addLog("Validation terminée: ${report.validProducts.size} valides pour $targetCatalog, ${report.errors.size} en erreur.")

        DownloadResult(
            targetCatalog = targetCatalog,
            products = report.validProducts,
            statusLogs = logs,
            report = report
        )
    }

    fun generateSampleProductsForStore(storeName: String, fileName: String = "Catalogue Drive"): ImportValidationReport {
        val store = storeName.uppercase().trim()
        
        return ImportValidationReport(
            targetCatalog = store,
            validProducts = emptyList(),
            errors = listOf(RowValidationError(0, "", listOf("Aucun produit trouvé dans le fichier."))),
            totalRowsScanned = 0,
            sourceFileName = fileName
        )
    }

    /**
     * Parse and validate a local file Uri (.csv, .txt, .xlsx, .pdf)
     */
    suspend fun parseAndValidateFileUri(
        context: Context,
        uri: Uri,
        fileName: String,
        targetCatalog: String = "DREAMPRICE"
    ): ImportValidationReport = withContext(Dispatchers.IO) {
        val detectedStore = autoDetectCatalog(fileName, "", targetCatalog)
        val lowerName = fileName.lowercase()
        return@withContext try {
            if (lowerName.endsWith(".xlsx")) {
                parseAndValidateXlsxUri(context, uri, fileName, detectedStore)
            } else if (lowerName.endsWith(".pdf") || isPdfStream(context, uri)) {
                parseAndValidatePdfUri(context, uri, fileName, detectedStore)
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val text = inputStream.bufferedReader().use { it.readText() }
                    parseAndValidateCsvText(text, detectedStore, fileName)
                } ?: ImportValidationReport(
                    targetCatalog = detectedStore,
                    validProducts = emptyList(),
                    errors = listOf(RowValidationError(0, "", listOf("Impossible d'ouvrir le fichier sélectionné."))),
                    totalRowsScanned = 0,
                    sourceFileName = fileName
                )
            }
        } catch (e: Exception) {
            ImportValidationReport(
                targetCatalog = detectedStore,
                validProducts = emptyList(),
                errors = listOf(RowValidationError(0, "", listOf("Erreur de lecture du fichier : ${e.localizedMessage}"))),
                totalRowsScanned = 0,
                sourceFileName = fileName
            )
        }
    }

    private fun isPdfStream(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 && String(header, Charsets.US_ASCII) == "%PDF"
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Native PDF text extraction and row validation
     */
    private fun parseAndValidatePdfUri(
        context: Context,
        uri: Uri,
        fileName: String,
        targetCatalog: String
    ): ImportValidationReport {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                return ImportValidationReport(
                    targetCatalog = targetCatalog,
                    validProducts = emptyList(),
                    errors = listOf(RowValidationError(0, "", listOf("Le fichier PDF est vide."))),
                    totalRowsScanned = 0,
                    sourceFileName = fileName
                )
            }

            val extractedText = extractTextFromPdfBytes(bytes)
            if (extractedText.isBlank()) {
                return ImportValidationReport(
                    targetCatalog = targetCatalog,
                    validProducts = emptyList(),
                    errors = listOf(RowValidationError(0, "", listOf("Aucun texte exploitable n'a été extrait du fichier PDF."))),
                    totalRowsScanned = 0,
                    sourceFileName = fileName
                )
            }

            parseAndValidateCsvText(extractedText, targetCatalog, fileName)
        } catch (e: Exception) {
            ImportValidationReport(
                targetCatalog = targetCatalog,
                validProducts = emptyList(),
                errors = listOf(RowValidationError(0, "", listOf("Erreur d'extraction du document PDF : ${e.localizedMessage}"))),
                totalRowsScanned = 0,
                sourceFileName = fileName
            )
        }
    }

    private fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val resultText = StringBuilder()
        val pdfString = String(bytes, Charsets.ISO_8859_1)

        val streamRegex = Regex("""stream\r?\n([\s\S]*?)\r?\nendstream""")
        val streamMatches = streamRegex.findAll(pdfString)

        for (match in streamMatches) {
            val streamContentBytes = match.groupValues[1].toByteArray(Charsets.ISO_8859_1)
            var decompressedText: String? = null

            try {
                val inflater = java.util.zip.Inflater()
                inflater.setInput(streamContentBytes)
                val outputStream = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(2048)
                while (!inflater.finished() && !inflater.needsInput()) {
                    val count = inflater.inflate(buffer)
                    if (count > 0) {
                        outputStream.write(buffer, 0, count)
                    } else {
                        break
                    }
                }
                inflater.end()
                val inflatedBytes = outputStream.toByteArray()
                if (inflatedBytes.isNotEmpty()) {
                    decompressedText = String(inflatedBytes, Charsets.UTF_8)
                }
            } catch (_: Exception) {}

            val textToScan = decompressedText ?: String(streamContentBytes, Charsets.UTF_8)

            val tjMatches = Regex("""\(((?:[^()\\]|\\.)*)\)\s*(?:Tj|'|")""").findAll(textToScan)
            for (tj in tjMatches) {
                val decoded = unescapePdfString(tj.groupValues[1])
                if (decoded.isNotBlank()) {
                    resultText.append(decoded).append(" ")
                }
            }

            val tjArrayMatches = Regex("""\[\s*([\s\S]*?)\s*\]\s*TJ""").findAll(textToScan)
            for (tjArr in tjArrayMatches) {
                val inner = tjArr.groupValues[1]
                val strMatches = Regex("""\(((?:[^()\\]|\\.)*)\)""").findAll(inner)
                var rowStr = ""
                for (sm in strMatches) {
                    rowStr += unescapePdfString(sm.groupValues[1])
                }
                if (rowStr.isNotBlank()) {
                    resultText.append(rowStr).append("\n")
                }
            }
            resultText.append("\n")
        }

        if (resultText.length < 20) {
            val parenMatches = Regex("""\(((?:[^()\\]|\\.)*)\)""").findAll(pdfString)
            val lineSb = StringBuilder()
            for (m in parenMatches) {
                val text = unescapePdfString(m.groupValues[1])
                if (text.length > 1 && !text.startsWith("/") && !text.contains("Adobe") && !text.contains("Font")) {
                    lineSb.append(text).append(" ")
                }
            }
            if (lineSb.isNotBlank()) {
                resultText.append(lineSb.toString())
            }
        }

        return resultText.toString()
    }

    private fun unescapePdfString(str: String): String {
        return str.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\b", "")
            .replace("\\f", "")
    }

    /**
     * Parse raw text (CSV/TSV formatted) and run strict field validation on each row.
     */
    fun parseAndValidateCsvText(
        rawText: String,
        targetCatalog: String = "DREAMPRICE",
        fileName: String = "Saisie CSV"
    ): ImportValidationReport {
        val resolvedCatalog = autoDetectCatalog(fileName, rawText, targetCatalog)
        val validProducts = mutableListOf<ProductEntity>()
        val rowErrors = mutableListOf<RowValidationError>()

        val rawLines = rawText.lines().filter { it.isNotBlank() }
        if (rawLines.isEmpty()) {
            return ImportValidationReport(
                targetCatalog = resolvedCatalog,
                validProducts = emptyList(),
                errors = listOf(RowValidationError(0, "", listOf("Le contenu du fichier est vide."))),
                totalRowsScanned = 0,
                sourceFileName = fileName
            )
        }

        // Determine delimiter (, or ; or tab or pipe)
        val sampleLine = rawLines.firstOrNull { it.contains(";") || it.contains(",") || it.contains("\t") || it.contains("|") } ?: rawLines.first()
        val delimiter = when {
            sampleLine.contains(";") -> ';'
            sampleLine.contains("\t") -> '\t'
            sampleLine.contains("|") -> '|'
            else -> ','
        }

        val headerCols = parseCsvLine(sampleLine, delimiter).map { it.trim().trim('"').lowercase() }
        val idIdx = headerCols.indexOfFirst { it == "id" || it == "code" || it.contains("code_art") || it.contains("ref") }
        val nameIdx = headerCols.indexOfFirst { it.contains("product") || it.contains("nom") || it.contains("name") || it.contains("article") || it.contains("designation") }
        val catIdx = headerCols.indexOfFirst { it.contains("cat") || it.contains("category") || it.contains("rayon") }
        val brandIdx = headerCols.indexOfFirst { it.contains("brand") || it.contains("marque") }
        val unitIdx = headerCols.indexOfFirst { it.contains("unit") || it.contains("unité") || it.contains("format") }
        val priceIdx = headerCols.indexOfFirst { it.contains("price") || it.contains("prix") || it.contains("vente") }
        val costIdx = headerCols.indexOfFirst { it.contains("cost") || it.contains("coût") || it.contains("achat") }
        val barcodeIdx = headerCols.indexOfFirst { it.contains("barcode") || it.contains("bar_code") || it.contains("ean") || it.contains("code_barre") }

        val isHeaderPresent = (nameIdx >= 0 || priceIdx >= 0)
        val startLineIndex = if (isHeaderPresent) 1 else 0

        var totalScanned = 0

        for (i in startLineIndex until rawLines.size) {
            val line = rawLines[i]
            val rowIndex = i + 1 // 1-indexed for display
            var cols = parseCsvLine(line, delimiter)
            if (cols.size < 2 && line.contains("  ")) {
                cols = line.split(Regex("""\s{2,}""")).map { it.trim() }
            }
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            totalScanned++
            val fieldErrors = mutableListOf<String>()

            val rawName = cols.getOrNull(if (nameIdx >= 0) nameIdx else 0)?.trim() ?: ""
            val rawCat = cols.getOrNull(if (catIdx >= 0) catIdx else 1)?.trim() ?: "Général"
            val rawBrand = cols.getOrNull(if (brandIdx >= 0) brandIdx else 2)?.trim() ?: resolvedCatalog
            val rawUnit = cols.getOrNull(if (unitIdx >= 0) unitIdx else 3)?.trim() ?: "PCS"
            val rawPriceStr = cols.getOrNull(if (priceIdx >= 0) priceIdx else 4)?.trim() ?: ""
            val rawCostStr = cols.getOrNull(if (costIdx >= 0) costIdx else 5)?.trim() ?: ""
            val rawBarcode = if (barcodeIdx >= 0) cols.getOrNull(barcodeIdx)?.trim() ?: "" else ""
            val parsedId = if (idIdx >= 0) cols.getOrNull(idIdx)?.trim()?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() else null

            // 1. Name Validation
            if (rawName.isBlank()) {
                fieldErrors.add("Nom de produit obligatoire manquant.")
            }

            // 2. Price Validation
            val cleanPriceStr = rawPriceStr.replace("""[^\d.]""".toRegex(), "")
            val parsedPrice = cleanPriceStr.toDoubleOrNull()
            if (rawPriceStr.isBlank()) {
                fieldErrors.add("Prix de vente manquant.")
            } else if (parsedPrice == null) {
                fieldErrors.add("Prix invalide ('$rawPriceStr') : doit être un nombre.")
            } else if (parsedPrice <= 0) {
                fieldErrors.add("Le prix doit être strictly supérieur à 0 (valeur: $parsedPrice).")
            }

            // 3. Cost (Optional)
            val cleanCostStr = rawCostStr.replace("""[^\d.]""".toRegex(), "")
            val parsedCost = cleanCostStr.toDoubleOrNull() ?: 0.0

            if (fieldErrors.isNotEmpty()) {
                rowErrors.add(RowValidationError(rowIndex = rowIndex, rawContent = line, errors = fieldErrors))
            } else {
                val assignedId = if (parsedId != null && parsedId > 0) parsedId else (validProducts.size + 1)
                validProducts.add(
                    ProductEntity(
                        id = assignedId,
                        catalogType = resolvedCatalog,
                        name = rawName,
                        category = if (rawCat.isBlank()) "Général" else rawCat,
                        brand = if (rawBrand.isBlank()) resolvedCatalog else rawBrand,
                        unit = if (rawUnit.isBlank()) "PCS" else rawUnit,
                        price = parsedPrice!!,
                        cost = parsedCost,
                        barcode = rawBarcode
                    )
                )
            }
        }

        return ImportValidationReport(
            targetCatalog = resolvedCatalog,
            validProducts = validProducts,
            errors = rowErrors,
            totalRowsScanned = totalScanned,
            sourceFileName = fileName
        )
    }

    /**
     * Native Excel (.xlsx) unzip and XML parser with magic byte check and graceful fallback.
     */
    private fun parseAndValidateXlsxUri(
        context: Context,
        uri: Uri,
        fileName: String,
        targetCatalog: String
    ): ImportValidationReport {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        // Verify ZIP / XLSX header magic number (0x50 0x4B 0x03 0x04 -> "PK\u0003\u0004")
        val isZipArchive = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val magic = ByteArray(4)
                val read = input.read(magic)
                read == 4 && magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                        magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()
            } ?: false
        } catch (_: Exception) {
            false
        }

        if (isZipArchive) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == "xl/sharedStrings.xml") {
                                val xmlText = InputStreamReader(zip, "UTF-8").readText()
                                val stringMatches = Regex("""<t[^>]*>(.*?)</t>""").findAll(xmlText)
                                stringMatches.forEach { match ->
                                    sharedStrings.add(match.groupValues[1])
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }

                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name.startsWith("xl/worksheets/sheet1.xml") || entry.name.startsWith("xl/worksheets/sheet.xml")) {
                                val xmlText = InputStreamReader(zip, "UTF-8").readText()
                                val rowMatches = Regex("""<row[^>]*>(.*?)</row>""").findAll(xmlText)
                                for (rowMatch in rowMatches) {
                                    val rowXml = rowMatch.groupValues[1]
                                    val cellMatches = Regex("""<c[^>]*?(?:t="([^"]*)")?[^>]*>(?:<v>(.*?)</v>)?</c>""").findAll(rowXml)
                                    val rowCells = mutableListOf<String>()
                                    for (cell in cellMatches) {
                                        val type = cell.groupValues[1]
                                        val value = cell.groupValues[2]
                                        if (type == "s" && value.isNotEmpty()) {
                                            val idx = value.toIntOrNull()
                                            val stringVal = if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else value
                                            rowCells.add(stringVal)
                                        } else {
                                            rowCells.add(value)
                                        }
                                    }
                                    if (rowCells.any { it.isNotBlank() }) {
                                        sheetRows.add(rowCells)
                                    }
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SpreadsheetImporter", "Erreur lors de la lecture ZIP/XLSX, bascule en mode texte : ${e.message}")
            }
        }

        if (sheetRows.isEmpty()) {
            // Fallback: Attempt reading as plain text CSV
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val text = input.bufferedReader().use { it.readText() }
                    if (text.isNotBlank()) {
                        return parseAndValidateCsvText(text, targetCatalog, fileName)
                    }
                }
            } catch (_: Exception) {}
        }

        if (sheetRows.isEmpty() && !isZipArchive) {
            // Check if it was an older binary .xls (OLE2 format: 0xD0 0xCF 0x11 0xE0)
            val isOldXls = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val magic = ByteArray(4)
                    val read = input.read(magic)
                    read == 4 && magic[0] == 0xD0.toByte() && magic[1] == 0xCF.toByte()
                } ?: false
            } catch (_: Exception) { false }

            if (isOldXls) {
                return ImportValidationReport(
                    targetCatalog = targetCatalog,
                    validProducts = emptyList(),
                    errors = listOf(RowValidationError(0, "", listOf("Format Excel binaire ancien (.xls) détecté. Veuillez enregistrer le fichier en .xlsx moderne ou en .csv."))),
                    totalRowsScanned = 0,
                    sourceFileName = fileName
                )
            }
        }

        // Convert extracted sheet rows to CSV formatted string
        val csvFormatted = sheetRows.joinToString("\n") { row ->
            row.joinToString(",") { cell ->
                if (cell.contains(",") || cell.contains("\n")) "\"$cell\"" else cell
            }
        }

        return parseAndValidateCsvText(csvFormatted, targetCatalog, fileName)
    }

    private fun parseCsvLine(line: String, delimiter: Char = ','): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == delimiter && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}
