package com.bellatrixos.windows.drivers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.bellatrixos.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileOutputStream

/** Cetak gambar, teks, dan halaman WebView. */
object Printer {
    @JvmStatic
    fun a(context: Context, file: File?) {
        if (file == null) return
        val helper = PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
        try {
            helper.printBitmap("Bellatrix Image", FileProvider.getUriForFile(context, "${context.packageName}.provider", file))
        } catch (_: Exception) {
            Toast.makeText(context, R.string.toast_file_failed_print, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun b(context: Context, text: String) {
        val webView = WebView(context).apply { visibility = android.view.View.GONE }
        webView.loadDataWithBaseURL(null, "<html><body><pre>$text</pre></body></html>", "text/HTML", "UTF-8", null)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                manager.print(
                    "MyApp_Print_Job",
                    webView.createPrintDocumentAdapter("Document"),
                    PrintAttributes.Builder().build()
                )
            }
        }
    }

    @JvmStatic
    fun c(context: Context, file: File) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        manager.print("MyApp_Formatted_Text_Print", object : PrintDocumentAdapter() {
            private var document: PrintedPdfDocument? = null
            private var pageHeight = 0

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                document = PrintedPdfDocument(context, newAttributes ?: PrintAttributes.Builder().build())
                val media = newAttributes?.mediaSize ?: PrintAttributes.MediaSize.NA_LETTER
                pageHeight = (media.heightMils / 1000) * 72
                callback.onLayoutFinished(
                    PrintDocumentInfo.Builder(file.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),
                    true
                )
            }

            override fun onWrite(
                pages: Array<PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdf = document ?: return callback.onWriteFailed("PDF document belum siap")
                var page = pdf.startPage(0)
                var canvas: Canvas = page.canvas
                val paint = Paint().apply { textSize = 14f }
                try {
                    BufferedReader(FileReader(file)).use { reader ->
                        var y = 50
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (y + 20 > pageHeight) {
                                pdf.finishPage(page)
                                page = pdf.startPage(pdf.pages.size)
                                canvas = page.canvas
                                y = 50
                            }
                            canvas.drawText(line, 40f, y.toFloat(), paint)
                            y += 20
                        }
                    }
                    pdf.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }, null)
    }
}
