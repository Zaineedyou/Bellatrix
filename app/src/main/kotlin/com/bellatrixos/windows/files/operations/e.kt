package com.bellatrixos.windows.files.operations

import android.view.View
import android.widget.CheckBox
import com.bellatrixos.windows.dialogs.UIDialogProgress
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import java.io.File
import java.util.concurrent.CountDownLatch

/** Runnable sintetis yang menampilkan dialog konflik dan menunggu keputusan. */
class e(
    @JvmField val A: CountDownLatch,
    @JvmField val B: BooleanArray,
    @JvmField val C: Any,
    @JvmField val u: Int,
    @JvmField val v: FileOperations,
    @JvmField val w: String,
    @JvmField val x: Array<File>,
    @JvmField val y: IntArray,
    @JvmField val z: IntArray
) : Runnable {

    constructor(
        item: ClipboardItem,
        operations: FileOperations,
        str: String,
        latch: CountDownLatch,
        iArr: IntArray,
        iArr2: IntArray,
        files: Array<File>,
        skip: BooleanArray
    ) : this(latch, skip, item, 2, operations, str, files, iArr, iArr2)

    constructor(
        operations: FileOperations,
        str: String,
        files: Array<File>,
        iArr: IntArray,
        iArr2: IntArray,
        latch: CountDownLatch,
        skip: BooleanArray,
        str2: String,
        i2: Int
    ) : this(latch, skip, str2, i2, operations, str, files, iArr, iArr2)

    override fun run() {
        when (u) {
            0 -> {
                val dialog = v.f4926e
                val checkBox: CheckBox? = dialog.F
                if (checkBox != null) checkBox.visibility = View.GONE
                val fileArr = x
                val iArr = y
                val iArr2 = z
                val countDownLatch = A
                val zArr = B
                val str = C as String
                val str2 = w
                dialog.c(str2, object : UIDialogProgress.ConflictListener {
                    override fun a(applyAll: Boolean) {
                        val file = fileArr[0]
                        val operations = v
                        operations.a(file, iArr, 1, iArr2)
                        operations.f4926e.a()
                        countDownLatch.countDown()
                    }

                    override fun b(applyAll: Boolean) {
                        zArr[0] = true
                        v.f4926e.a()
                        countDownLatch.countDown()
                    }

                    override fun c(applyAll: Boolean) {
                        fileArr[0] = Utils.c(File(str), str2)
                        v.f4926e.a()
                        countDownLatch.countDown()
                    }
                })
            }
            1 -> {
                val dialog = v.f4926e
                val checkBox: CheckBox? = dialog.F
                if (checkBox != null) checkBox.visibility = View.GONE
                val fileArr = x
                val iArr = y
                val iArr2 = z
                val countDownLatch = A
                val zArr = B
                val str = C as String
                val str2 = w
                dialog.c(str2, object : UIDialogProgress.ConflictListener {
                    override fun a(applyAll: Boolean) {
                        val file = fileArr[0]
                        val operations = v
                        operations.a(file, iArr, 1, iArr2)
                        operations.f4926e.a()
                        countDownLatch.countDown()
                    }

                    override fun b(applyAll: Boolean) {
                        zArr[0] = true
                        v.f4926e.a()
                        countDownLatch.countDown()
                    }

                    override fun c(applyAll: Boolean) {
                        fileArr[0] = Utils.c(File(str), str2)
                        v.f4926e.a()
                        countDownLatch.countDown()
                    }
                })
            }
            else -> {
                val dialog = v.f4926e
                val checkBox: CheckBox? = dialog.F
                if (checkBox != null) checkBox.visibility = View.VISIBLE
                val clipboardItem = C as ClipboardItem
                val str5 = clipboardItem.b
                val zArr = B
                val str6 = w
                val fileArr = x
                val iArr = y
                val iArr2 = z
                val countDownLatch = A
                val fileOperations = v
                dialog.c(str5, object : UIDialogProgress.ConflictListener {
                    override fun a(applyAll: Boolean) {
                        if (applyAll) {
                            fileOperations.g = FileOperations.ConflictAction.v
                        }
                        fileOperations.a(fileArr[0], iArr, 1, iArr2)
                        fileOperations.f4926e.a()
                        countDownLatch.countDown()
                    }

                    override fun b(applyAll: Boolean) {
                        if (applyAll) {
                            fileOperations.g = FileOperations.ConflictAction.w
                        }
                        zArr[0] = true
                        fileOperations.f4926e.a()
                        countDownLatch.countDown()
                    }

                    override fun c(applyAll: Boolean) {
                        if (applyAll) {
                            fileOperations.g = FileOperations.ConflictAction.x
                        }
                        fileArr[0] = Utils.c(File(str6), clipboardItem.b)
                        fileOperations.f4926e.a()
                        countDownLatch.countDown()
                    }
                })
            }
        }
    }
}
