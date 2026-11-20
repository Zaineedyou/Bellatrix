package com.bellatrixos

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.Consumer
import com.bellatrixos.app_settings.searchbar.SuggestionItem
import com.bellatrixos.searchapp.SearchApp
import com.bellatrixos.searchapp.SearchItem
import com.bellatrixos.startmenu.StartMenu
import com.bellatrixos.startmenu.StartMenuAdapter
import com.bellatrixos.startmenu.listeners.StartMenuTextWatcher
import com.bellatrixos.windows.icons.IconEngine

/** Consumer synthetic yang mengisi panel pencarian Start Menu. */
class g(@JvmField val v: MainActivity, @JvmField val u: Int) : Consumer<Any?> {

    override fun accept(obj: Any?) {
        when (u) {
            0 -> {
                val searchItem = obj as? SearchItem
                val mainActivity = v
                val act = mainActivity.W ?: mainActivity
                val textView: TextView = mainActivity.V0 ?: return
                val textView2: TextView = mainActivity.W0 ?: return
                val textView3: TextView = mainActivity.b1 ?: return
                val textView4: TextView = mainActivity.c1 ?: return
                val imageView: ImageView = mainActivity.a1 ?: return
                val startMenuViews = mainActivity.S0 ?: return
                if (startMenuViews.f4793f.length() < 2) {
                    textView3.text = act.getString(R.string.st_menu_search_tips)
                    textView.text = act.getString(R.string.st_menu_search_translate)
                    textView2.text = act.getString(R.string.st_menu_today_in_history)
                    textView4.text = act.getString(R.string.st_menu_search_quick_searches)
                    imageView.setImageResource(R.drawable.ui_sm_ic_library)
                    textView.setCompoundDrawablesWithIntrinsicBounds(SearchApp.f4770e, null, null, null)
                    textView2.setCompoundDrawablesWithIntrinsicBounds(SearchApp.f4771f, null, null, null)
                    startMenuViews.j.visibility = View.GONE
                } else if (searchItem != null) {
                    SearchApp.b?.getOrNull(0)?.let { SearchApp.f4768a?.removeCallbacks(it) }
                    val runnable = Runnable {
                        if (startMenuViews.f4793f.length() < 2) return@Runnable
                        val searchItem2 = searchItem
                        val startMenuItem = searchItem2.b
                        val suggestionItem: SuggestionItem? = if (startMenuItem == null) searchItem2.c else null
                        if (startMenuItem == null) {
                            if (suggestionItem != null) {
                                val string: String
                                if (suggestionItem.c == null) {
                                    textView4.text = act.getString(R.string.st_menu_search_results_none)
                                    string = act.getString(R.string.st_menu_search_web)
                                } else {
                                    textView4.text = act.getString(R.string.settings_str_name)
                                    string = act.getString(R.string.st_menu_app_open)
                                }
                                textView.text = string
                                textView2.text = act.getString(R.string.st_menu_search_go_to_settings)
                                textView3.text = suggestionItem.f4723a
                                imageView.setImageResource(suggestionItem.b)
                            }
                            if (StartMenu.c) {
                                startMenuViews.j.visibility = View.VISIBLE
                            }
                        }
                        // Jalur aplikasi; jika item null, gunakan nilai aman.
                        if (startMenuItem != null) {
                            textView4.text = act.getString(R.string.app_default_title)
                            textView.text = act.getString(R.string.st_menu_app_open)
                            textView2.text = act.getString(R.string.st_menu_app_settings)
                            textView3.text = startMenuItem.b()
                            val iconEngineB = IconEngine.b(act)
                            iconEngineB.c(startMenuItem.c().orEmpty(), com.bellatrixos.startmenu.g(imageView))
                        }
                        textView.setCompoundDrawablesWithIntrinsicBounds(SearchApp.c, null, null, null)
                        textView2.setCompoundDrawablesWithIntrinsicBounds(SearchApp.f4769d, null, null, null)
                        if (StartMenu.c) {
                            startMenuViews.j.visibility = View.VISIBLE
                        }
                    }
                    val runnableArr = SearchApp.b ?: arrayOf(runnable).also { SearchApp.b = it }
                    runnableArr[0] = runnable
                    SearchApp.f4768a?.postDelayed(runnable, 100L)
                }
            }
            1 -> {
                val mainActivity3 = v
                mainActivity3.T0 = obj as? StartMenuTextWatcher
                // zzeu.c().d(mainActivity) — helper iklan hasil obfuskasi belum tersedia.
            }
            else -> {
                v.M0 = obj as? StartMenuAdapter
            }
        }
    }
}
