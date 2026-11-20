package com.bellatrixos.app_settings.fragments

import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.widget.AdapterView
import com.bellatrixos.R

import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.UISpinnerAdapter
import java.util.Arrays

/** Runnable pengaturan bahasa, tanggal, dan waktu. */
class v(@JvmField val v: UILangRegion, @JvmField val u: Int) : Runnable {
    override fun run() {
        val region = v
        when (u) {
            0 -> {
                if (!region.isAdded || region.view == null) return
                val context = region.s0 ?: return
                val dateAdapter = UISpinnerAdapter(context, context.resources.getStringArray(R.array.app_date_formats))
                val timeAdapter = UISpinnerAdapter(context, context.resources.getStringArray(R.array.app_time_formats))
                val configuration: Configuration = context.resources.configuration
                val locale = if (Build.VERSION.SDK_INT >= 24) {
                    configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    configuration.locale
                }
                var languageTag = locale.toLanguageTag()
                var language = locale.language
                if (language == "id") {
                    languageTag = "in"
                    language = "in"
                }
                val codes = Arrays.asList(*context.resources.getStringArray(R.array.language_codes))
                var index = codes.indexOf(languageTag)
                if (index == -1) index = codes.indexOf(language)
                if (index == -1) index = 0
                region.y0?.setArray(R.array.language_names)
                if (index != 0) region.y0?.setSelection(index)
                region.y0?.setItemSelectListener(C0054c(5, region))

                timeAdapter.setDropDownViewResource(R.layout.ui_spinner_item)
                region.x0?.adapter = timeAdapter
                region.x0?.setSelection(if (Global.y == "HH:mm") 1 else 0)
                region.x0?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (region.v0) {
                            region.v0 = false
                        } else {
                            region.t0?.h("", if (position == 0) "hh:mm a" else "HH:mm")
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                dateAdapter.setDropDownViewResource(R.layout.ui_spinner_item)
                region.w0?.adapter = dateAdapter
                var dateIndex = 1
                val currentDate = Global.z
                if (currentDate == null) {
                    dateIndex = 0
                } else if (currentDate == "d-M-yy") {
                    dateIndex = 2
                } else if (currentDate != "dd-MM-yy") {
                    dateIndex = 0
                }
                region.w0?.setSelection(dateIndex)
                region.w0?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (region.u0) {
                            region.u0 = false
                            return
                        }
                        val str = if (position != 0) {
                            if (position != 1) "d-M-yy" else "dd-MM-yy"
                        } else {
                            "dd-MM-yyyy"
                        }
                        region.t0?.h(str, "")
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            1 -> {
                Global.f4841i = true
                region.t0?.n()
            }
            else -> region.t0?.n()
        }
    }
}
