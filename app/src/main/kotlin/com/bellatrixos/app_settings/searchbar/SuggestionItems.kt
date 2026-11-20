package com.bellatrixos.app_settings.searchbar

import android.content.Context
import com.bellatrixos.R

/** Daftar saran pengaturan yang bisa dicari. */
object SuggestionItems {
    @JvmStatic
    fun a(context: Context): List<SuggestionItem> = listOf(
        SuggestionItem(R.drawable.ic_display_24_regular, context.getString(R.string.settings_system_display), "11"),
        SuggestionItem(R.drawable.ic_resize_24_regular, context.getString(R.string.settings_display_scale), "11_2131362556"),
        SuggestionItem(R.drawable.ic_statusbar_24_regular, context.getString(R.string.settings_display_statusbar), "11_2131362624"),
        SuggestionItem(R.drawable.ic_fluent_storage_24_regular, context.getString(R.string.settings_storage_management), "12"),
        SuggestionItem(R.drawable.ic_multitasking_24, context.getString(R.string.settings_system_multitasking), "13"),
        SuggestionItem(R.drawable.ic_home_24_regular, context.getString(R.string.settings_system_default_launcher), "14"),
        SuggestionItem(R.drawable.ic_info_26_regular, context.getString(R.string.settings_system_about), "15"),
        SuggestionItem(R.drawable.ic_image_24_regular, context.getString(R.string.settings_personalize_background), "31"),
        SuggestionItem(R.drawable.ic_color_26_regular, context.getString(R.string.settings_colors_colors), "32"),
        SuggestionItem(R.drawable.ic_taskbar_24_regular, context.getString(R.string.settings_taskbar), "33"),
        SuggestionItem(R.drawable.ic_start_menu_24_regular, context.getString(R.string.settings_personalize_startmenu_title), "34"),
        SuggestionItem(R.drawable.ic_apps_list_24_regular, context.getString(R.string.settings_apps_installed_list), "41"),
        SuggestionItem(R.drawable.ic_taskbar_24_regular, context.getString(R.string.explorer_str_name), "42"),
        SuggestionItem(R.drawable.ic_local_language_24_regular, context.getString(R.string.settings_time_lang_language), "61"),
        SuggestionItem(R.drawable.ic_mouse_24_regular, context.getString(R.string.settings_system_mouse), "21"),
        SuggestionItem(R.drawable.ic_transparent_effect_24, context.getString(R.string.settings_colors_blur_effects), "32_2131362684"),
        SuggestionItem(R.drawable.ic_paint_brush_24_regular, context.getString(R.string.settings_colors_scheme), "32_2131362621"),
        SuggestionItem(R.drawable.ic_taskbar_24_regular, context.getString(R.string.settings_taskbar_alignment), "33_2131362620"),
        SuggestionItem(R.drawable.ic_app_icons_24_regular, context.getString(R.string.settings_app_ic_pack), "4"),
        SuggestionItem(R.drawable.ic_apps_add_in_24_regular, context.getString(R.string.settings_apps_installer), "4")
    )
}
