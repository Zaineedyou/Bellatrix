package com.bellatrixos.windows.files.operations

import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import java.io.File
import java.util.concurrent.CountDownLatch

/** Runnable penyalin banyak berkas beserta dialog progresnya. */
class h(
    @JvmField val u: Int,
    @JvmField val v: FileOperations,
    @JvmField val w: String,
    @JvmField val x: Any
) : Runnable {

    constructor(operations: FileOperations, str: String, str2: String) :
        this(1, operations, str, str2)

    constructor(operations: FileOperations, items: ArrayList<ClipboardItem>, str: String) :
        this(0, operations, str, items)

    override fun run() {
        when (u) {
            0 -> {
                @Suppress("UNCHECKED_CAST")
                val list = x as List<ClipboardItem>
                val str2 = w
                val fileOperations = v
                val handler = fileOperations.b
                val iArr5 = IntArray(1)
                val iArr6 = IntArray(1)
                var iA = 0
                try {
                    for (clipboardItem in list) {
                        try {
                            iA += Utils.a(File(clipboardItem.f4921a, clipboardItem.b))
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                    iA = 0
                }
                val i4 = iA
                val arrayList2 = ArrayList<File>()
                try {
                    val it2 = list.iterator()
                    var z3 = true
                    while (it2.hasNext()) {
                        val clipboardItem2 = it2.next()
                        val str3 = clipboardItem2.f4921a
                        val str4 = clipboardItem2.b
                        var file = File(str3, str4)
                        var file2 = File(str2, str4)
                        var handled = false
                        if (file2.exists()) {
                            when (fileOperations.g) {
                                FileOperations.ConflictAction.w -> {
                                    continue
                                }
                                FileOperations.ConflictAction.v -> {
                                    fileOperations.a(file2, iArr5, 1, iArr6)
                                }
                                FileOperations.ConflictAction.x -> {
                                    file2 = Utils.c(File(str2), str4)
                                }
                                FileOperations.ConflictAction.u -> {
                                    val countDownLatch = CountDownLatch(1)
                                    val fileArr = arrayOf(file2)
                                    val zArr = booleanArrayOf(false)
                                    handler.post(e(clipboardItem2, fileOperations, str2, countDownLatch, iArr5, iArr6, fileArr, zArr))
                                    try {
                                        countDownLatch.await()
                                    } catch (_: InterruptedException) {
                                        Thread.currentThread().interrupt()
                                    }
                                    if (zArr[0]) {
                                        handled = true
                                    } else {
                                        file2 = fileArr[0]
                                    }
                                }
                                else -> {}
                            }
                        }
                        if (handled) {
                            z3 = false
                            continue
                        }
                        handler.post {
                            val dialog = fileOperations.f4926e
                            if (dialog.isShowing) {
                                dialog.v?.text = fileOperations.f4924a.getString(R.string.dialog_file_name, clipboardItem2.b)
                            }
                        }
                        val ok = if (file.isDirectory) {
                            fileOperations.e(file, file2, iArr5, i4, iArr6)
                        } else {
                            fileOperations.d(file, file2, iArr5, i4, iArr6)
                        }
                        z3 = if (ok) z3 else false
                        arrayList2.add(file2)
                        if (Files.g && ok) {
                            fileOperations.a(file, iArr5, 1, iArr6)
                        }
                    }
                    handler.post(i(0, fileOperations, arrayList2, z3))
                } catch (_: Exception) {
                    handler.post(d(3, fileOperations))
                }
            }
            else -> {
                val fileOperations2 = v
                val dialog = fileOperations2.f4926e
                if (fileOperations2.f4928h && !dialog.isShowing) {
                    dialog.show()
                    dialog.b(0)
                    dialog.w?.text = w
                    val str5 = x as? String
                    if (str5 != null) {
                        val text = fileOperations2.f4924a.getString(R.string.dialog_file_name, str5)
                        dialog.v?.text = text
                    }
                }
            }
        }
    }
}
