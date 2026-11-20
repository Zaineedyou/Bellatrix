package com.bellatrixos.windows.icons.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.windows.icons.IconPack
import com.bellatrixos.windows.icons.adapters.AppIconAdapter
import com.bellatrixos.windows.icons.models.IconPackItem

/** Dialog pemilih ikon aplikasi: paket ikon, gallery, atau ikon sistem. */
class DialogAppIcons : DialogFragment() {

    fun interface SelectionListener {
        fun b(drawable: Drawable?)
    }

    fun interface SelectionPkgListener {
        fun a(str: String)
    }

    @JvmField var I0: Button? = null
    @JvmField var J0: ListView? = null
    @JvmField var K0: AppIconAdapter? = null
    @JvmField var L0: RecyclerView? = null
    @JvmField var M0: LinearLayout? = null
    @JvmField var N0: SelectionListener? = null
    @JvmField var O0: SelectionPkgListener? = null
    @JvmField var P0: ArrayList<Any>? = null
    @JvmField val Q0: ArrayList<String> = ArrayList()
    @JvmField var R0: ActivityResultLauncher<Intent>? = null

    override fun onDestroy() {
        super.onDestroy()
        K0 = null
    }

    override fun onResume() {
        super.onResume()
        val window = dialog?.window ?: return
        val metrics = resources.displayMetrics
        val params = window.attributes
        params.height = (metrics.heightPixels * 0.64).toInt()
        params.width = (metrics.widthPixels * 0.34).toInt()
        window.attributes = params
        J0?.let { list ->
            list.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    list.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val lp = list.layoutParams
                    lp.width = -1
                    list.layoutParams = lp
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<ListView>(R.id.listView_icons)
        J0 = list
        list.divider = null
        list.dividerHeight = 0
        L0 = view.findViewById(R.id.recyclerView_icons)
        M0 = view.findViewById(R.id.layout_choose)
        val negative = view.findViewById<Button>(R.id.btn_negative)
        I0 = view.findViewById(R.id.btnBack)
        negative.setOnClickListener { dismissAllowingStateLoss() }
        I0?.setOnClickListener {
            M0?.visibility = View.VISIBLE
            L0?.visibility = View.GONE
            I0?.visibility = View.INVISIBLE
        }
        if (P0 == null) {
            val packs = ArrayList<IconPackItem>()
            packs.add(
                IconPackItem(
                    ContextCompat.getDrawable(requireContext(), R.drawable.img_app_photos),
                    "gallery",
                    "Choose from Gallery"
                )
            )
            packs.add(
                IconPackItem(
                    ContextCompat.getDrawable(requireContext(), R.drawable.img_icon_pack_default),
                    "system",
                    "System Default"
                )
            )
            IconPack.a(requireContext(), packs)
            P0 = ArrayList<Any>(packs)
        }
        val rows = ArrayList<Map<String, Any?>>()
        for (pack in P0!!) {
            val item = pack as? IconPackItem ?: continue
            rows.add(
                hashMapOf(
                    "icon" to item.icon,
                    "name" to item.label,
                    "packageName" to item.packageName
                )
            )
        }
        val adapter = SimpleAdapter(
            requireContext(), rows, R.layout.ui_dialog_icon_list,
            arrayOf("icon", "name"), intArrayOf(R.id.icon, R.id.name)
        )
        adapter.setViewBinder(c(0))
        list.adapter = adapter
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val pkg = rows[position]["packageName"] as? String
            when (pkg) {
                "system" -> {
                    if (K0 == null) {
                        L0?.minimumWidth = M0?.width ?: 0
                        L0?.minimumHeight = M0?.height ?: 0
                        val icons = AppIconAdapter(requireContext(), ArrayList())
                        K0 = icons
                        L0?.setHasFixedSize(true)
                        L0?.layoutManager = GridLayoutManager(requireContext(), 5)
                        L0?.adapter = icons
                        icons.y = AppIconAdapter.OnItemClickListener { name -> b(this).b(name) }
                    }
                    M0?.visibility = View.GONE
                    L0?.visibility = View.VISIBLE
                    I0?.visibility = View.VISIBLE
                    AppExecutors.d().execute { e(this@DialogAppIcons, 0).run() }
                }
                "gallery" -> launchPick(R0, null)
                else -> {
                    val context = requireContext()
                    val pm: PackageManager = context.packageManager
                    val pick = Intent("android.intent.action.PICK").apply {
                        type = "image/*"
                        setPackage(pkg)
                    }
                    if (pm.queryIntentActivities(pick, 0).isNotEmpty()) {
                        launchPick(R0, pick)
                        return@OnItemClickListener
                    }
                    val content = Intent("android.intent.action.GET_CONTENT").apply {
                        type = "image/*"
                        setPackage(pkg)
                    }
                    if (pm.queryIntentActivities(content, 0).isEmpty()) {
                        Toast.makeText(context, R.string.settings_app_ic_pack_pick_empty, Toast.LENGTH_SHORT).show()
                        return@OnItemClickListener
                    }
                    launchPick(R0, content)
                }
            }
        }
    }

    private fun launchPick(launcher: ActivityResultLauncher<Intent>?, intent: Intent?) {
        val target = intent ?: Intent("android.intent.action.PICK").apply { type = "image/*" }
        target.putExtra("aspectX", 1)
        target.putExtra("aspectY", 1)
        target.putExtra("scale", true)
        launcher?.launch(target)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.UIDialogAnimated)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        R0 = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), b(this))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.ui_dialog_icon_chooser, container, false)
}
