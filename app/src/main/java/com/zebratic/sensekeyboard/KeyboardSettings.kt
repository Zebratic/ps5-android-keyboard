package com.zebratic.sensekeyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

/**
 * All keyboard customization settings, persisted in SharedPreferences.
 */
class KeyboardSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sensekeyboard_settings", Context.MODE_PRIVATE)

    // Layout
    var keyboardLayout: String
        get() = prefs.getString("layout", "us") ?: "us"
        set(v) = prefs.edit().putString("layout", v).apply()

    // Visual - Colors (stored as hex int)
    var accentColor: Int
        get() = prefs.getInt("accent_color", Color.parseColor("#0070D1"))
        set(v) = prefs.edit().putInt("accent_color", v).apply()

    var bgColor: Int
        get() = prefs.getInt("bg_color", Color.parseColor("#1A1D2E"))
        set(v) = prefs.edit().putInt("bg_color", v).apply()

    var keyColor: Int
        get() = prefs.getInt("key_color", Color.parseColor("#2A2D3E"))
        set(v) = prefs.edit().putInt("key_color", v).apply()

    var textColor: Int
        get() = prefs.getInt("text_color", Color.WHITE)
        set(v) = prefs.edit().putInt("text_color", v).apply()

    // Visual - Opacity (0-100)
    var bgOpacity: Int
        get() = prefs.getInt("bg_opacity", 90)
        set(v) = prefs.edit().putInt("bg_opacity", v.coerceIn(0, 100)).apply()

    // Visual - Size
    var keyboardHeightPercent: Int
        get() = prefs.getInt("kb_height_pct", 35)
        set(v) = prefs.edit().putInt("kb_height_pct", v.coerceIn(20, 60)).apply()

    var keyboardWidthPercent: Int
        get() = prefs.getInt("kb_width_pct", 100)
        set(v) = prefs.edit().putInt("kb_width_pct", v.coerceIn(50, 100)).apply()

    // Anchor position: 0=left/top, 1=center, 2=right/bottom
    var anchorX: Int
        get() = prefs.getInt("anchor_x", 1) // default center
        set(v) = prefs.edit().putInt("anchor_x", v.coerceIn(0, 2)).apply()

    var anchorY: Int
        get() = prefs.getInt("anchor_y", 2) // default bottom
        set(v) = prefs.edit().putInt("anchor_y", v.coerceIn(0, 2)).apply()

    // Margins (px percentage of screen)
    var marginX: Int
        get() = prefs.getInt("margin_x", 0)
        set(v) = prefs.edit().putInt("margin_x", v.coerceIn(0, 20)).apply()

    var marginY: Int
        get() = prefs.getInt("margin_y", 0)
        set(v) = prefs.edit().putInt("margin_y", v.coerceIn(0, 20)).apply()

    // Behavior
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", false)
        set(v) = prefs.edit().putBoolean("sound_enabled", v).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean("vibrate_enabled", true)
        set(v) = prefs.edit().putBoolean("vibrate_enabled", v).apply()

    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean("suggestions_enabled", true)
        set(v) = prefs.edit().putBoolean("suggestions_enabled", v).apply()

    // Wrapping
    var horizontalWrap: Boolean
        get() = prefs.getBoolean("horizontal_wrap", true)
        set(v) = prefs.edit().putBoolean("horizontal_wrap", v).apply()

    var verticalWrap: Boolean
        get() = prefs.getBoolean("vertical_wrap", false)
        set(v) = prefs.edit().putBoolean("vertical_wrap", v).apply()

    // Number row
    var numberRowEnabled: Boolean
        get() = prefs.getBoolean("number_row", false)
        set(v) = prefs.edit().putBoolean("number_row", v).apply()

    // Border-only highlight (vs filled key)
    var borderHighlight: Boolean
        get() = prefs.getBoolean("border_highlight", true)
        set(v) = prefs.edit().putBoolean("border_highlight", v).apply()

    // D-pad repeat speed (ms)
    var dpadRepeatRate: Int
        get() = prefs.getInt("dpad_repeat_rate", 80)
        set(v) = prefs.edit().putInt("dpad_repeat_rate", v.coerceIn(30, 200)).apply()

    var dpadInitialDelay: Int
        get() = prefs.getInt("dpad_initial_delay", 400)
        set(v) = prefs.edit().putInt("dpad_initial_delay", v.coerceIn(100, 800)).apply()

    var activePreset: String
        get() = prefs.getString("active_preset", "ps5") ?: "ps5"
        set(v) = prefs.edit().putString("active_preset", v).apply()

    // Presets
    fun applyPreset(preset: String) {
        activePreset = preset
        when (preset) {
            "ps5" -> {
                accentColor = Color.parseColor("#0070D1")
                bgColor = Color.parseColor("#1A1D2E")
                keyColor = Color.parseColor("#2A2D3E")
                textColor = Color.WHITE
                bgOpacity = 90
            }
            "dark" -> {
                accentColor = Color.parseColor("#BB86FC")
                bgColor = Color.parseColor("#121212")
                keyColor = Color.parseColor("#1E1E1E")
                textColor = Color.WHITE
                bgOpacity = 95
            }
            "xbox" -> {
                accentColor = Color.parseColor("#107C10")
                bgColor = Color.parseColor("#1A1A2E")
                keyColor = Color.parseColor("#2D2D44")
                textColor = Color.WHITE
                bgOpacity = 90
            }
            "steam" -> {
                accentColor = Color.parseColor("#1A9FFF")
                bgColor = Color.parseColor("#1B2838")
                keyColor = Color.parseColor("#2A475E")
                textColor = Color.parseColor("#C7D5E0")
                bgOpacity = 92
            }
        }
    }
}
