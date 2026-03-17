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

    // Secondary key color (number row, action keys)
    var secondaryKeyColor: Int
        get() = prefs.getInt("secondary_key_color", Color.parseColor("#1E2133"))
        set(v) = prefs.edit().putInt("secondary_key_color", v).apply()

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



    // Show hint bar at bottom
    var showHintBar: Boolean
        get() = prefs.getBoolean("show_hint_bar", true)
        set(v) = prefs.edit().putBoolean("show_hint_bar", v).apply()

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

    // Font
    var fontFamily: String
        get() = prefs.getString("font_family", "default") ?: "default"
        set(v) = prefs.edit().putString("font_family", v).apply()

    var fontScale: Int
        get() = prefs.getInt("font_scale", 100) // percentage
        set(v) = prefs.edit().putInt("font_scale", v.coerceIn(50, 200)).apply()

    // Highlight style: "none", "border", "fill", "glow"
    var highlightStyle: String
        get() = prefs.getString("highlight_style", "border") ?: "border"
        set(v) = prefs.edit().putString("highlight_style", v).apply()

    // Key highlight border width (dp)
    var highlightBorderSize: Int
        get() = prefs.getInt("highlight_border_size", 3)
        set(v) = prefs.edit().putInt("highlight_border_size", v.coerceIn(1, 8)).apply()

    // Key corner rounding (dp)
    var keyRounding: Int
        get() = prefs.getInt("key_rounding", 6)
        set(v) = prefs.edit().putInt("key_rounding", v.coerceIn(0, 24)).apply()

    // Navigation effect: "wind", "slide", "ripple", "trail", "none"
    var navEffect: String
        get() = prefs.getString("nav_effect", "wind") ?: "wind"
        set(v) = prefs.edit().putString("nav_effect", v).apply()

    // Click animation: "none", "fill", "pop", "flash"
    var clickAnimation: String
        get() = prefs.getString("click_animation", "fill") ?: "fill"
        set(v) = prefs.edit().putString("click_animation", v).apply()

    // Visual style (design, not colors): "standard", "rounded", "minimal", "retro"
    var visualStyle: String
        get() = prefs.getString("visual_style", "standard") ?: "standard"
        set(v) = prefs.edit().putString("visual_style", v).apply()

    // Action bar buttons
    var showSpacebar: Boolean
        get() = prefs.getBoolean("show_spacebar", true)
        set(v) = prefs.edit().putBoolean("show_spacebar", v).apply()
    var showEnterBtn: Boolean
        get() = prefs.getBoolean("show_enter", true)
        set(v) = prefs.edit().putBoolean("show_enter", v).apply()
    var showBackspaceBtn: Boolean
        get() = prefs.getBoolean("show_backspace", true)
        set(v) = prefs.edit().putBoolean("show_backspace", v).apply()
    var showArrowKeys: Boolean
        get() = prefs.getBoolean("show_arrows", true)
        set(v) = prefs.edit().putBoolean("show_arrows", v).apply()
    var showVoiceBtn: Boolean
        get() = prefs.getBoolean("show_voice", true)
        set(v) = prefs.edit().putBoolean("show_voice", v).apply()
    var showSymbolsBtn: Boolean
        get() = prefs.getBoolean("show_symbols", true)
        set(v) = prefs.edit().putBoolean("show_symbols", v).apply()
    var showDialpadBtn: Boolean
        get() = prefs.getBoolean("show_dialpad", true)
        set(v) = prefs.edit().putBoolean("show_dialpad", v).apply()
    var showCopyBtn: Boolean
        get() = prefs.getBoolean("show_copy", true)
        set(v) = prefs.edit().putBoolean("show_copy", v).apply()
    var showPasteBtn: Boolean
        get() = prefs.getBoolean("show_paste", true)
        set(v) = prefs.edit().putBoolean("show_paste", v).apply()

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

    // Preset data class for comparison
    data class PresetValues(
        val keyRounding: Int, val highlightStyle: String, val highlightBorderSize: Int,
        val clickAnimation: String, val navEffect: String, val bgOpacity: Int,
        val accentColor: Int, val bgColor: Int, val keyColor: Int,
        val secondaryKeyColor: Int, val textColor: Int
    )

    val PRESETS = mapOf(
        "ps5" to PresetValues(6, "border", 3, "fill", "wind", 90,
            Color.parseColor("#0070D1"), Color.parseColor("#1A1D2E"),
            Color.parseColor("#2A2D3E"), Color.parseColor("#1E2133"), Color.WHITE),
        "xbox" to PresetValues(8, "glow", 3, "pop", "ripple", 92,
            Color.parseColor("#107C10"), Color.parseColor("#1A1A2E"),
            Color.parseColor("#2D2D44"), Color.parseColor("#222238"), Color.WHITE),
        "steam" to PresetValues(4, "border", 2, "fill", "slide", 90,
            Color.parseColor("#1A9FFF"), Color.parseColor("#1B2838"),
            Color.parseColor("#2A475E"), Color.parseColor("#1E3A50"), Color.parseColor("#C7D5E0")),
        "minimal" to PresetValues(2, "border", 1, "none", "none", 95,
            Color.parseColor("#FFFFFF"), Color.parseColor("#0A0A0A"),
            Color.parseColor("#1A1A1A"), Color.parseColor("#111111"), Color.WHITE),
        "rounded" to PresetValues(18, "glow", 4, "pop", "trail", 85,
            Color.parseColor("#BB86FC"), Color.parseColor("#121212"),
            Color.parseColor("#2A2A3A"), Color.parseColor("#1E1E2E"), Color.WHITE),
        "retro" to PresetValues(0, "fill", 3, "flash", "none", 100,
            Color.parseColor("#FF9F1C"), Color.parseColor("#0E0E0E"),
            Color.parseColor("#222222"), Color.parseColor("#181818"), Color.parseColor("#FFD93D"))
    )

    fun applyPreset(preset: String) {
        val p = PRESETS[preset] ?: return
        activePreset = preset
        keyRounding = p.keyRounding
        highlightStyle = p.highlightStyle
        highlightBorderSize = p.highlightBorderSize
        clickAnimation = p.clickAnimation
        navEffect = p.navEffect
        bgOpacity = p.bgOpacity
        accentColor = p.accentColor
        bgColor = p.bgColor
        keyColor = p.keyColor
        secondaryKeyColor = p.secondaryKeyColor
        textColor = p.textColor
    }

    fun getCurrentValues(): PresetValues = PresetValues(
        keyRounding, highlightStyle, highlightBorderSize,
        clickAnimation, navEffect, bgOpacity,
        accentColor, bgColor, keyColor, secondaryKeyColor, textColor
    )

    fun detectPreset(): String {
        val current = getCurrentValues()
        for ((name, values) in PRESETS) {
            if (current == values) return name
        }
        return "custom"
    }

    fun markCustomIfChanged() {
        if (activePreset != "custom" && detectPreset() != activePreset) {
            activePreset = "custom"
        }
    }
}
