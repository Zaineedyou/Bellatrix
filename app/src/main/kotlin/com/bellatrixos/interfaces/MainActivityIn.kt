package com.bellatrixos.interfaces

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import com.bellatrixos.UIFragment
import com.bellatrixos.desktop.DesktopItem
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.widgets.TaskbarBtn
import java.io.File

/**
 * Kontrak Activity utama. UIContextMenu dan TaskBarAdapter masih berupa tipe
 * placeholder sampai kedua kelas tersebut dimigrasikan.
 */
interface MainActivityIn {
    fun A(str: String)
    fun C(str: String, str2: String, str3: String)
    fun D(str: String)
    fun E(i2: Int)
    fun F()
    fun G(): Bitmap?
    fun H(z: Boolean)
    fun J()
    fun K(): Boolean
    fun M(str: String, item: StartMenuItem?, str2: String)
    fun N()
    fun P(fragment: UIFragment)
    fun Q(i2: Int)
    fun R(z: Boolean)
    fun S()
    fun U(): Drawable?
    fun W(bitmap: Bitmap?)
    fun Y(i2: Int)
    fun Z(z: Boolean)
    fun a(uri: Uri?)
    fun a0(): View?
    fun b(str: String, drawable: BitmapDrawable?)
    fun b0(i2: Int)
    fun c0(str: String, str2: String?)
    fun d(fragment: UIFragment?)
    fun d0(str: String, str2: String, str3: String, z: Boolean)
    fun e(file: File?, file2: File?)
    fun e0(): Any?
    fun f(): Context?
    fun g0()
    fun h(str: String, str2: String)
    fun i(i2: Int)
    fun i0(): ViewGroup?
    fun j(i2: Int, str: String, str2: String)
    fun j0(z: Boolean)
    fun k(): Any?
    fun k0(str: String)
    fun l(z: Boolean)
    fun l0(): Any?
    fun m0(item: DesktopItem?)
    fun n()
    fun n0(item: DesktopItem?)
    fun o(i2: Int)
    fun p(fragment: UIFragment?)
    fun p0(btn: TaskbarBtn?)
    fun q0(z: Boolean)
    fun r(str: String)
    fun s(str: String)
    fun t(view: View?, i2: Int)
    fun v(bitmap: Bitmap?)
    fun x(i2: Int)
}
