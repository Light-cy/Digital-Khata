package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Budget
import com.example.data.model.PaymentMode
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTemplate
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportImportManager {

    data class ExportData(
        val transactions: List<Transaction>,
        val templates: List<RecurringTemplate>,
        val budgets: List<Budget>
    )

    data class ImportResult(
        val importedTransactionsCount: Int,
        val importedTemplatesCount: Int,
        val importedBudgetsCount: Int,
        val error: String? = null
    ) {
        val success: Boolean get() = error == null
        val transactionsCount: Int get() = importedTransactionsCount
        val budgetsCount: Int get() = importedBudgetsCount
        val message: String get() = error ?: "Success"
    }

    suspend fun shareExportedFile(context: Context, repository: KhataRepository) {
        val transactions = repository.getAllTransactionsSnapshot()
        val budgets = repository.getAllBudgetsSnapshot()
        val templates = repository.getActiveTemplatesSnapshot()
        val csv = generateCsvContent(ExportData(transactions, templates, budgets))
        shareCsvFile(context, csv)
    }

    suspend fun importFromUri(context: Context, uri: Uri, repository: KhataRepository): ImportResult {
        val content = readCsvFromUri(context, uri) ?: return ImportResult(0, 0, 0, error = "Unable to read selected file")
        return parseAndImportCsv(content, replaceExisting = false, repository = repository)
    }

    /**
     * Generates a complete CSV content string with transactions, summaries, budgets, and templates.
     */
    fun generateCsvContent(data: ExportData): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Header Metadata
        sb.append("# DIGITAL KHATA BACKUP EXPORT\n")
        sb.append("# Export Date: ").append(timestamp).append("\n")
        sb.append("# Application: Digital Khata\n\n")

        // SECTION 1: TRANSACTIONS
        sb.append("[TRANSACTIONS]\n")
        sb.append("ID,Date,Type,Amount,Title,Person Name,Category,Settled Status,Payment Mode,Note\n")

        for (tx in data.transactions) {
            val dateStr = DateUtils.formatCsvDate(tx.date)
            val typeStr = tx.type.name
            val amountStr = tx.amount.toString()
            val titleStr = escapeCsv(tx.title)
            val personStr = escapeCsv(tx.personName ?: "")
            val categoryStr = escapeCsv(tx.category ?: "")
            val settledStr = if (tx.isSettled) "YES" else "NO"
            val modeStr = tx.paymentMode.name
            val noteStr = escapeCsv(tx.note ?: "")

            sb.append("${tx.id},$dateStr,$typeStr,$amountStr,$titleStr,$personStr,$categoryStr,$settledStr,$modeStr,$noteStr\n")
        }
        sb.append("\n")

        // SECTION 2: BUDGETS
        sb.append("[BUDGETS]\n")
        sb.append("Category,Monthly Limit\n")
        for (budget in data.budgets) {
            sb.append("${escapeCsv(budget.category)},${budget.monthlyLimit}\n")
        }
        sb.append("\n")

        // SECTION 3: RECURRING TEMPLATES
        sb.append("[RECURRING_TEMPLATES]\n")
        sb.append("Title,Amount,Type,Category,Frequency,DayOfMonth,DayOfWeek,IsActive,Note\n")
        for (tmpl in data.templates) {
            sb.append("${escapeCsv(tmpl.title)},${tmpl.amount},${tmpl.type.name},${escapeCsv(tmpl.category ?: "")},${tmpl.frequency.name},${tmpl.dayOfMonth},${tmpl.dayOfWeek},${if (tmpl.isActive) "1" else "0"},${escapeCsv(tmpl.note ?: "")}\n")
        }
        sb.append("\n")

        // SECTION 4: FINANCIAL SUMMARY TOTALS
        sb.append("[SUMMARY_TOTALS]\n")
        sb.append("Metric,Value\n")

        val totalEarnings = data.transactions.filter { it.type == TransactionType.EARNING }.sumOf { it.amount }
        val totalExpenses = data.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val loansGiven = data.transactions.filter { it.type == TransactionType.LOAN_GIVEN }.sumOf { it.amount }
        val loansTaken = data.transactions.filter { it.type == TransactionType.LOAN_TAKEN }.sumOf { it.amount }
        val unsettledLoansGiven = data.transactions.filter { it.type == TransactionType.LOAN_GIVEN && !it.isSettled }.sumOf { it.amount }
        val unsettledLoansTaken = data.transactions.filter { it.type == TransactionType.LOAN_TAKEN && !it.isSettled }.sumOf { it.amount }
        val netSavings = totalEarnings - totalExpenses - loansGiven + loansTaken

        val cashEarnings = data.transactions.filter { it.type == TransactionType.EARNING && it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        val cashExpenses = data.transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        val cashLoansGiven = data.transactions.filter { it.type == TransactionType.LOAN_GIVEN && it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        val cashLoansTaken = data.transactions.filter { it.type == TransactionType.LOAN_TAKEN && it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        val cashNet = cashEarnings - cashExpenses - cashLoansGiven + cashLoansTaken

        val accountEarnings = data.transactions.filter { it.type == TransactionType.EARNING && it.paymentMode == PaymentMode.ACCOUNT }.sumOf { it.amount }
        val accountExpenses = data.transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMode == PaymentMode.ACCOUNT }.sumOf { it.amount }
        val accountLoansGiven = data.transactions.filter { it.type == TransactionType.LOAN_GIVEN && it.paymentMode == PaymentMode.ACCOUNT }.sumOf { it.amount }
        val accountLoansTaken = data.transactions.filter { it.type == TransactionType.LOAN_TAKEN && it.paymentMode == PaymentMode.ACCOUNT }.sumOf { it.amount }
        val accountNet = accountEarnings - accountExpenses - accountLoansGiven + accountLoansTaken

        sb.append("Total Lifetime Earnings,${CurrencyUtils.format(totalEarnings)}\n")
        sb.append("Total Lifetime Expenses,${CurrencyUtils.format(totalExpenses)}\n")
        sb.append("Total Receivables (Outflow),${CurrencyUtils.format(loansGiven)}\n")
        sb.append("Total Payables (Inflow),${CurrencyUtils.format(loansTaken)}\n")
        sb.append("Unsettled Receivables,${CurrencyUtils.format(unsettledLoansGiven)}\n")
        sb.append("Unsettled Payables,${CurrencyUtils.format(unsettledLoansTaken)}\n")
        sb.append("Cash Net Balance,${CurrencyUtils.format(cashNet)}\n")
        sb.append("Bank/Account Net Balance,${CurrencyUtils.format(accountNet)}\n")
        sb.append("Calculated Total Net Balance,${CurrencyUtils.format(netSavings)}\n")
        sb.append("Total Transaction Entries,${data.transactions.size}\n")

        return sb.toString()
    }

    /**
     * Parses CSV data back into entities and updates the repository.
     */
    suspend fun parseAndImportCsv(
        csvContent: String,
        replaceExisting: Boolean,
        repository: KhataRepository
    ): ImportResult = withContext(Dispatchers.IO) {
        val lines = csvContent.lines()
        var currentSection = ""
        val newTransactions = mutableListOf<Transaction>()
        val newBudgets = mutableListOf<Budget>()
        val newTemplates = mutableListOf<RecurringTemplate>()

        try {
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) continue

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line
                    continue
                }

                // Skip header rows
                if (line.startsWith("ID,Date") || line.startsWith("Category,Monthly") ||
                    line.startsWith("Title,Amount") || line.startsWith("Metric,Value")
                ) {
                    continue
                }

                val tokens = parseCsvLine(line)

                when (currentSection) {
                    "[TRANSACTIONS]" -> {
                        if (tokens.size >= 7) {
                            val dateMillis = DateUtils.parseCsvDate(tokens[1]) ?: System.currentTimeMillis()
                            val type = try { TransactionType.valueOf(tokens[2].trim().uppercase()) } catch (e: Exception) { TransactionType.EXPENSE }
                            val amount = tokens[3].trim().toDoubleOrNull() ?: 0.0
                            val title = tokens[4].trim()
                            val personName = tokens[5].trim().takeIf { it.isNotEmpty() }
                            val category = tokens[6].trim().takeIf { it.isNotEmpty() }
                            val isSettled = if (tokens.size > 7) tokens[7].trim().equals("YES", ignoreCase = true) else false
                            
                            val (paymentMode, note) = if (tokens.size >= 10) {
                                val mode = try { PaymentMode.valueOf(tokens[8].trim().uppercase()) } catch (e: Exception) { PaymentMode.CASH }
                                val n = tokens[9].trim().takeIf { it.isNotEmpty() }
                                Pair(mode, n)
                            } else if (tokens.size == 9) {
                                val candidate = tokens[8].trim().uppercase()
                                if (candidate == "CASH" || candidate == "ACCOUNT") {
                                    Pair(try { PaymentMode.valueOf(candidate) } catch (e: Exception) { PaymentMode.CASH }, null)
                                } else {
                                    Pair(PaymentMode.CASH, tokens[8].trim().takeIf { it.isNotEmpty() })
                                }
                            } else {
                                Pair(PaymentMode.CASH, null)
                            }

                            if (title.isNotEmpty() && amount > 0) {
                                newTransactions.add(
                                    Transaction(
                                        date = dateMillis,
                                        type = type,
                                        amount = amount,
                                        title = title,
                                        personName = personName,
                                        category = category,
                                        isSettled = isSettled,
                                        paymentMode = paymentMode,
                                        note = note
                                    )
                                )
                            }
                        }
                    }

                    "[BUDGETS]" -> {
                        if (tokens.size >= 2) {
                            val category = tokens[0].trim()
                            val limit = tokens[1].trim().toDoubleOrNull() ?: 0.0
                            if (category.isNotEmpty() && limit > 0) {
                                newBudgets.add(Budget(category = category, monthlyLimit = limit))
                            }
                        }
                    }

                    "[RECURRING_TEMPLATES]" -> {
                        if (tokens.size >= 5) {
                            val title = tokens[0].trim()
                            val amount = tokens[1].trim().toDoubleOrNull() ?: 0.0
                            val type = try { TransactionType.valueOf(tokens[2].trim().uppercase()) } catch (e: Exception) { TransactionType.EXPENSE }
                            val category = tokens[3].trim().takeIf { it.isNotEmpty() }
                            val freq = try { RecurringFrequency.valueOf(tokens[4].trim().uppercase()) } catch (e: Exception) { RecurringFrequency.MONTHLY }
                            val dayOfMonth = if (tokens.size > 5) tokens[5].trim().toIntOrNull() ?: 1 else 1
                            val dayOfWeek = if (tokens.size > 6) tokens[6].trim().toIntOrNull() ?: 1 else 1
                            val isActive = if (tokens.size > 7) tokens[7].trim() == "1" else true
                            val note = if (tokens.size > 8) tokens[8].trim().takeIf { it.isNotEmpty() } else null

                            if (title.isNotEmpty() && amount > 0) {
                                newTemplates.add(
                                    RecurringTemplate(
                                        title = title,
                                        amount = amount,
                                        type = type,
                                        category = category,
                                        frequency = freq,
                                        dayOfMonth = dayOfMonth,
                                        dayOfWeek = dayOfWeek,
                                        isActive = isActive,
                                        note = note
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (replaceExisting) {
                repository.clearAllData()
            }

            if (newTransactions.isNotEmpty()) {
                repository.insertTransactions(newTransactions)
            }
            for (b in newBudgets) {
                repository.insertOrUpdateBudget(b)
            }
            for (t in newTemplates) {
                repository.insertRecurringTemplate(t)
            }

            ImportResult(
                importedTransactionsCount = newTransactions.size,
                importedTemplatesCount = newTemplates.size,
                importedBudgetsCount = newBudgets.size
            )
        } catch (e: Exception) {
            ImportResult(0, 0, 0, error = e.localizedMessage ?: "Failed to parse CSV file")
        }
    }

    /**
     * Writes CSV string to the given Uri obtained from ACTION_CREATE_DOCUMENT
     */
    suspend fun writeCsvToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads text content from Uri obtained from ACTION_OPEN_DOCUMENT
     */
    suspend fun readCsvFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Shares backup CSV directly via system share sheet (Google Drive, WhatsApp, Email, etc.)
     */
    fun shareCsvFile(context: Context, csvContent: String) {
        try {
            val fileName = "DigitalKhata_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Digital Khata Backup ($fileName)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Save or Share Digital Khata Backup"))
        } catch (e: Exception) {
            // Fallback to plain text share if FileProvider is unavailable
            val plainIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Digital Khata Backup")
                putExtra(Intent.EXTRA_TEXT, csvContent)
            }
            context.startActivity(Intent.createChooser(plainIntent, "Export Digital Khata Data"))
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++ // Skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
