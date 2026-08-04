package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.Customer
import com.example.data.local.Product
import com.example.data.local.SalesInvoiceItem
import com.example.ui.screens.CustomerTransactionItemDetail
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStatementGenerator {

    data class StatementConfig(
        val fromTime: Long? = null,
        val toTime: Long? = null,
        val includeItemsDetail: Boolean = true,
        val storeName: String = "متجر النظام المتكامل للمبيعات"
    )

    fun generateCustomerStatementPdf(
        context: Context,
        customer: Customer,
        transactions: List<CustomerTransactionItemDetail>,
        salesInvoiceItemsMap: Map<String, List<SalesInvoiceItem>>,
        productsMap: Map<String, Product>,
        config: StatementConfig = StatementConfig()
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.ENGLISH)

            // Filter transactions by date if specified
            val filteredTransactions = transactions.filter { tx ->
                val matchFrom = config.fromTime == null || tx.date >= config.fromTime
                val matchTo = config.toTime == null || tx.date <= config.toTime
                matchFrom && matchTo
            }.sortedBy { it.date } // Chronological order for statement calculation

            // Paints setup
            val primaryPaint = Paint().apply {
                color = Color.parseColor("#1E1B4B") // Deep Indigo
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val primaryLightPaint = Paint().apply {
                color = Color.parseColor("#EEF2FF") // Light Indigo tint
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#C7D2FE")
                textSize = 10f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val headerLeftPaint = Paint().apply {
                color = Color.parseColor("#C7D2FE")
                textSize = 10f
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }

            val textBoldRight = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val textRegularRight = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val textRegularCenter = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val textSmallGrayRight = Paint().apply {
                color = Color.parseColor("#6B7280")
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val greenPaintRight = Paint().apply {
                color = Color.parseColor("#15803D") // Dark Green
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val redPaintRight = Paint().apply {
                color = Color.parseColor("#B91C1C") // Dark Red
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.parseColor("#312E81")
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val tableHeaderTextRight = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            val tableHeaderTextCenter = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val rowAltPaint = Paint().apply {
                color = Color.parseColor("#F9FAFB")
                style = Paint.Style.FILL
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            fun drawHeader(cv: Canvas, pgNum: Int) {
                // Header Banner
                val headerRect = RectF(20f, 20f, (pageWidth - 20).toFloat(), 80f)
                cv.drawRoundRect(headerRect, 10f, 10f, primaryPaint)

                // Right aligned titles
                cv.drawText(config.storeName, (pageWidth - 35).toFloat(), 45f, titlePaint)
                cv.drawText("كشف حساب عميل تفصيلي - نِظام المبيعات الذكي", (pageWidth - 35).toFloat(), 65f, subtitlePaint)

                // Left aligned info
                val dateStr = "تاريخ الإصدار: ${dateFormat.format(Date())}"
                cv.drawText(dateStr, 35f, 45f, headerLeftPaint)
                cv.drawText("صفحة $pgNum", 35f, 65f, headerLeftPaint)
            }

            drawHeader(canvas, currentPageNumber)

            var yPos = 95f

            // Customer Info Box (RTL)
            val custBoxRect = RectF(20f, yPos, (pageWidth - 20).toFloat(), yPos + 65f)
            canvas.drawRoundRect(custBoxRect, 8f, 8f, primaryLightPaint)
            canvas.drawRoundRect(custBoxRect, 8f, 8f, borderPaint)

            // Column 1 (Right Side)
            canvas.drawText("معلومات العميل:", (pageWidth - 35).toFloat(), yPos + 20f, textBoldRight)
            canvas.drawText("الاسم: ${customer.name}", (pageWidth - 35).toFloat(), yPos + 38f, textBoldRight)
            canvas.drawText("الهاتف: ${customer.phone ?: "غير مسجل"}", (pageWidth - 35).toFloat(), yPos + 54f, textRegularRight)

            // Column 2 (Middle/Left Side)
            val addressStr = "العنوان: ${customer.address ?: "غير محدد"}"
            canvas.drawText(addressStr, 280f, yPos + 38f, textRegularRight)

            val periodText = if (config.fromTime != null && config.toTime != null) {
                "الفترة: من ${dateFormat.format(Date(config.fromTime))} إلى ${dateFormat.format(Date(config.toTime))}"
            } else {
                "الفترة: كشف حساب شامل حتى الآن"
            }
            canvas.drawText(periodText, 280f, yPos + 54f, textSmallGrayRight)

            yPos += 75f

            // Calculate Metrics
            var totalSales = 0.0
            var totalPayments = 0.0
            filteredTransactions.forEach { tx ->
                when (tx) {
                    is CustomerTransactionItemDetail.Sale -> {
                        totalSales += tx.sale.totalAmount
                        totalPayments += tx.sale.paidAmount
                    }
                    is CustomerTransactionItemDetail.Payment -> totalPayments += tx.payment.amount
                }
            }
            val currentBalance = customer.balance

            // Metrics Summary Cards (RTL Order: Sales on Right, Payments Middle, Balance Left)
            val cardWidth = (pageWidth - 50f) / 3f

            // Card 1 (Far Right): Total Sales
            val card1 = RectF(pageWidth - 20f - cardWidth, yPos, pageWidth - 20f, yPos + 42f)
            canvas.drawRoundRect(card1, 6f, 6f, primaryLightPaint)
            canvas.drawRoundRect(card1, 6f, 6f, borderPaint)
            canvas.drawText("إجمالي الفواتير والآجل", pageWidth - 30f, yPos + 16f, textSmallGrayRight)
            canvas.drawText("${totalSales.formatCurrency()}", pageWidth - 30f, yPos + 33f, redPaintRight)

            // Card 2 (Middle): Total Payments
            val card2 = RectF(pageWidth - 25f - (cardWidth * 2), yPos, pageWidth - 25f - cardWidth, yPos + 42f)
            canvas.drawRoundRect(card2, 6f, 6f, primaryLightPaint)
            canvas.drawRoundRect(card2, 6f, 6f, borderPaint)
            canvas.drawText("إجمالي التحصيلات والمسدد", pageWidth - 35f - cardWidth, yPos + 16f, textSmallGrayRight)
            canvas.drawText("${totalPayments.formatCurrency()}", pageWidth - 35f - cardWidth, yPos + 33f, greenPaintRight)

            // Card 3 (Far Left): Current Net Balance
            val card3 = RectF(20f, yPos, 20f + cardWidth, yPos + 42f)
            canvas.drawRoundRect(card3, 6f, 6f, primaryLightPaint)
            canvas.drawRoundRect(card3, 6f, 6f, borderPaint)
            canvas.drawText("الرصيد المالي الحالي", 20f + cardWidth - 10f, yPos + 16f, textSmallGrayRight)

            val (balStr, balPaint) = when {
                currentBalance > 0 -> Pair("عليه دَين: ${currentBalance.formatCurrency()}", redPaintRight)
                currentBalance < 0 -> Pair("له مستحقات: ${(-currentBalance).formatCurrency()}", greenPaintRight)
                else -> Pair("متزن (0 ريال)", textBoldRight)
            }
            canvas.drawText(balStr, 20f + cardWidth - 10f, yPos + 33f, balPaint)

            yPos += 52f

            // Table Header (RTL Alignment)
            fun drawTableHeader(cv: Canvas, currentY: Float) {
                val tableHeaderRect = RectF(20f, currentY, (pageWidth - 20).toFloat(), currentY + 22f)
                cv.drawRect(tableHeaderRect, tableHeaderPaint)

                // RTL Table Column Boundaries (Total active width = 555, from 575 to 20):
                // 1. # (575 -> 550) center 562
                // 2. Date/Time (550 -> 445) right 540
                // 3. Statement/Items (445 -> 225) right 435
                // 4. Debit/Invoice (225 -> 145) right 215
                // 5. Credit/Payment (145 -> 85) right 135
                // 6. Running Balance (85 -> 20) right 75
                cv.drawText("#", 562f, currentY + 15f, tableHeaderTextCenter)
                cv.drawText("التاريخ والوقت", 540f, currentY + 15f, tableHeaderTextRight)
                cv.drawText("نوع الحركة والبيان التفصيلي", 435f, currentY + 15f, tableHeaderTextRight)
                cv.drawText("مدين (فاتورة)", 215f, currentY + 15f, tableHeaderTextRight)
                cv.drawText("دائن (مسدد)", 135f, currentY + 15f, tableHeaderTextRight)
                cv.drawText("الرصيد التراكمي", 75f, currentY + 15f, tableHeaderTextRight)
            }

            drawTableHeader(canvas, yPos)
            yPos += 22f

            // Draw Table Rows
            var runningBalance = 0.0
            val rowHeight = 20f

            filteredTransactions.forEachIndexed { index, tx ->
                // Check if page end is reached
                if (yPos > pageHeight - 65f) {
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas, currentPageNumber)

                    yPos = 95f
                    drawTableHeader(canvas, yPos)
                    yPos += 22f
                }

                // Row background alternating
                if (index % 2 == 1) {
                    val rowBg = RectF(20f, yPos, (pageWidth - 20).toFloat(), yPos + rowHeight)
                    canvas.drawRect(rowBg, rowAltPaint)
                }

                val txDateStr = dateTimeFormat.format(Date(tx.date))

                when (tx) {
                    is CustomerTransactionItemDetail.Sale -> {
                        val total = tx.sale.totalAmount
                        val paid = tx.sale.paidAmount
                        val remaining = tx.sale.remainingAmount

                        // Cumulative balance only increases by remaining unpaid debt
                        runningBalance += remaining

                        val saleLabel = when {
                            tx.sale.status == "RETURN" || tx.sale.paymentType == "RETURN" -> "مرتجع مبيعات (رقم: ${tx.sale.invoiceNumber})"
                            tx.sale.paymentType == "CASH" || remaining <= 0.0 -> "فاتورة مبيعات نقدي (رقم: ${tx.sale.invoiceNumber})"
                            tx.sale.paymentType == "PARTIAL" || (paid > 0.0 && remaining > 0.0) -> "فاتورة مبيعات جزئي (رقم: ${tx.sale.invoiceNumber})"
                            else -> "فاتورة مبيعات آجل (رقم: ${tx.sale.invoiceNumber})"
                        }

                        canvas.drawText("${index + 1}", 562f, yPos + 14f, textRegularCenter)
                        canvas.drawText(txDateStr, 540f, yPos + 14f, textSmallGrayRight)
                        canvas.drawText(saleLabel, 435f, yPos + 14f, textBoldRight)
                        
                        // Debit (Invoice value)
                        canvas.drawText("${total.formatCurrency()}", 215f, yPos + 14f, redPaintRight)
                        
                        // Credit (Cash paid upfront at checkout)
                        if (paid > 0.0) {
                            canvas.drawText("${paid.formatCurrency()}", 135f, yPos + 14f, greenPaintRight)
                        } else {
                            canvas.drawText("-", 135f, yPos + 14f, textSmallGrayRight)
                        }

                        // Cumulative running balance
                        canvas.drawText("${runningBalance.formatCurrency()}", 75f, yPos + 14f, textBoldRight)

                        yPos += rowHeight

                        // If items details requested and available
                        if (config.includeItemsDetail) {
                            val items = salesInvoiceItemsMap[tx.sale.id] ?: emptyList()
                            if (items.isNotEmpty()) {
                                items.forEach { item ->
                                    if (yPos > pageHeight - 50f) return@forEach
                                    val prodName = productsMap[item.productId]?.name ?: "منتج مجهول"
                                    val itemDetailStr = "  • $prodName | ${item.quantity.toInt()} قطعة × ${item.unitPrice.formatCurrency()}"
                                    canvas.drawText(itemDetailStr, 425f, yPos + 12f, textSmallGrayRight)
                                    yPos += 15f
                                }
                            }
                        }
                    }
                    is CustomerTransactionItemDetail.Payment -> {
                        val amount = tx.payment.amount
                        runningBalance -= amount

                        val methodStr = if (tx.payment.paymentMethod == "CASH") "نقدي" else "تحويل بنكي"
                        val noteStr = if (!tx.payment.note.isNullOrBlank()) " (${tx.payment.note})" else ""

                        canvas.drawText("${index + 1}", 562f, yPos + 14f, textRegularCenter)
                        canvas.drawText(txDateStr, 540f, yPos + 14f, textSmallGrayRight)
                        canvas.drawText("دفعة مقبوضة [$methodStr]$noteStr", 435f, yPos + 14f, greenPaintRight)
                        canvas.drawText("-", 215f, yPos + 14f, textSmallGrayRight)
                        canvas.drawText("${amount.formatCurrency()}", 135f, yPos + 14f, greenPaintRight)
                        canvas.drawText("${runningBalance.formatCurrency()}", 75f, yPos + 14f, textBoldRight)

                        yPos += rowHeight
                    }
                }

                // Divider line under row
                canvas.drawLine(20f, yPos, (pageWidth - 20).toFloat(), yPos, borderPaint)
            }

            yPos += 15f

            // Footer signature and notes block (RTL)
            if (yPos < pageHeight - 70f) {
                val notesRect = RectF(20f, yPos, (pageWidth - 20).toFloat(), yPos + 45f)
                canvas.drawRoundRect(notesRect, 6f, 6f, primaryLightPaint)
                canvas.drawRoundRect(notesRect, 6f, 6f, borderPaint)

                canvas.drawText("تنويه هام: يرجى مراجعة وتدقيق الحساب خلال 7 أيام من تاريخ الاستلام.", (pageWidth - 35).toFloat(), yPos + 18f, textSmallGrayRight)
                canvas.drawText("توقيع المحاسب / توقيع العميل بالاستلام: ____________________", (pageWidth - 35).toFloat(), yPos + 34f, textRegularRight)
            }

            pdfDocument.finishPage(page)

            // Save PDF to cache directory
            val cacheDir = File(context.cacheDir, "pdf_statements")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val pdfFile = File(cacheDir, "Statement_${customer.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareStatementToWhatsApp(
        context: Context,
        pdfFile: File,
        customer: Customer,
        totalSales: Double,
        totalPayments: Double
    ) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val balance = customer.balance
            val balanceSummary = when {
                balance > 0 -> "المبلغ المتبقي كدَين عليك: *${balance.formatCurrency()}*"
                balance < 0 -> "المبلغ المتبقي لك كمستحقات: *${(-balance).formatCurrency()}*"
                else -> "الحساب متزن بالكامل (0 ريال)"
            }

            val arabicMsg = """
                السلام عليكم ورحمة الله وبركاته،
                المحترم/ة: *${customer.name}*
                
                مرفق لكم كشف الحساب المالي التفصيلي (ملف PDF):
                ▫️ إجمالي الفواتير والآجل: *${totalSales.formatCurrency()}*
                ▫️ إجمالي الدفعات والمسدد: *${totalPayments.formatCurrency()}*
                ▫️ $balanceSummary
                
                يرجى الاطلاع على ملف PDF المرفق. شكراً لتعاملكم معنا!
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, arabicMsg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Normalize phone number (handle Arabic-Indic digits, leading zeros, country codes)
            val cleanPhone = normalizePhone(customer.phone)

            val isRegularInstalled = isPackageInstalled(context, "com.whatsapp")
            val isBusinessInstalled = isPackageInstalled(context, "com.whatsapp.w4b")

            val targetPkg = when {
                isRegularInstalled -> "com.whatsapp"
                isBusinessInstalled -> "com.whatsapp.w4b"
                else -> null
            }

            if (targetPkg != null) {
                val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, arabicMsg)
                    if (!cleanPhone.isNullOrBlank()) {
                        putExtra("jid", "$cleanPhone@s.whatsapp.net")
                        putExtra("phone", cleanPhone)
                    }
                    setPackage(targetPkg)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                Toast.makeText(
                    context,
                    "تم إرفاق ملف PDF للعميل (${customer.name}). اختر العميل من القائمة للإرسال.",
                    Toast.LENGTH_LONG
                ).show()

                context.startActivity(whatsappIntent)
                return
            }

            // General Chooser fallback if WhatsApp app is not specifically installed
            val chooser = Intent.createChooser(intent, "إرسال كشف الحساب (PDF) عبر واتساب / التطبيقات")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "حدث خطأ أثناء فتح واتساب: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun normalizePhone(rawPhone: String?): String? {
        if (rawPhone.isNullOrBlank()) return null
        val westernDigits = rawPhone
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')

        var clean = westernDigits.replace(Regex("[^0-9+]"), "")
        if (clean.isBlank()) return null

        if (clean.startsWith("+")) {
            clean = clean.substring(1)
        } else if (clean.startsWith("00")) {
            clean = clean.substring(2)
        } else if (clean.startsWith("0")) {
            if (clean.startsWith("05") && clean.length == 10) {
                clean = "966" + clean.substring(1)
            } else if (clean.startsWith("07") && clean.length == 9) {
                clean = "967" + clean.substring(1)
            } else if (clean.startsWith("01") && clean.length == 11) {
                clean = "20" + clean.substring(1)
            } else {
                clean = "966" + clean.substring(1)
            }
        }
        return clean
    }

    fun openPdfFile(context: Context, pdfFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "عرض ملف كشف الحساب"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر فتح ملف PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

