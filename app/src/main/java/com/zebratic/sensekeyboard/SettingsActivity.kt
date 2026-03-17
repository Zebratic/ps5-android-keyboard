package com.zebratic.sensekeyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        setContent {
            SenseKeyboardApp(
                onEnableKeyboard = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                onSelectKeyboard = {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                },
                onExit = { finish() }
            )
        }
    }
}

// PS5 Glass morphism colors
private val BgColor = Color(0xFF080A14)
private val SurfaceColor = Color(0xFF0E1020)
private val GlassColor = Color(0xFF151830)
private val GlassBorder = Color(0xFF252850)
private val AccentColor = Color(0xFF0070D1)
private val AccentGlow = Color(0x330070D1)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF6E7191)
private val DangerColor = Color(0xFFFF4757)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GlassColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
fun SenseKeyboardApp(onEnableKeyboard: () -> Unit, onSelectKeyboard: () -> Unit, onExit: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Nav bar - glassmorphism
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(SurfaceColor, SurfaceColor.copy(alpha = 0.8f)))
                    )
                    .border(BorderStroke(1.dp, GlassBorder.copy(alpha = 0.3f)))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎮", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SenseKeyboard", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(20.dp))
                listOf("Setup", "Settings", "Controls", "Test", "Debug").forEachIndexed { i, label ->
                    val isSelected = selectedTab == i
                    Box(
                        modifier = Modifier
                            .clickable { selectedTab = i }
                            .background(
                                if (isSelected) AccentColor else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isSelected) BorderStroke(1.dp, AccentColor.copy(alpha = 0.5f))
                                else BorderStroke(1.dp, Color.Transparent),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when (selectedTab) {
                    0 -> SetupTab(onEnableKeyboard, onSelectKeyboard)
                    1 -> SettingsTab()
                    2 -> ControlsTab()
                    3 -> TestTab()
                    4 -> DebugTab()
                }
            }
        }

        // Exit dialog
        if (showExitDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(modifier = Modifier.width(300.dp)) {
                    Text("Exit SenseKeyboard?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showExitDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("No", fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(containerColor = GlassColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) { Text("Yes", color = TextSecondary) }
                    }
                }
            }
        }
    }
}

// --- SETUP TAB ---

@Composable
fun SetupTab(onEnableKeyboard: () -> Unit, onSelectKeyboard: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard(modifier = Modifier.weight(1f)) {
            StepHeader("1", "Enable Keyboard")
            Text("Enable SenseKeyboard in input settings", color = TextSecondary, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))
            AccentButton("Open Settings") { onEnableKeyboard() }
        }
        GlassCard(modifier = Modifier.weight(1f)) {
            StepHeader("2", "Select Keyboard")
            Text("Set SenseKeyboard as active", color = TextSecondary, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))
            AccentButton("Select") { onSelectKeyboard() }
        }
        GlassCard(modifier = Modifier.weight(1f)) {
            StepHeader("3", "Type!")
            Text("Open any text field. Connect DualSense via Bluetooth.", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun StepHeader(step: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(22.dp).background(AccentColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center) {
            Text(step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(28.dp)
    ) { Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium) }
}

// --- SETTINGS TAB ---

@Composable
fun SettingsTab() {
    val context = LocalContext.current
    val settings = remember { KeyboardSettings(context) }
    var selectedLayout by remember { mutableStateOf(settings.keyboardLayout) }
    var selectedPreset by remember { mutableStateOf(settings.activePreset) }
    var bgOpacity by remember { mutableFloatStateOf(settings.bgOpacity.toFloat()) }
    var kbHeight by remember { mutableFloatStateOf(settings.keyboardHeightPercent.toFloat()) }
    var kbWidth by remember { mutableFloatStateOf(settings.keyboardWidthPercent.toFloat()) }
    var marginX by remember { mutableFloatStateOf(settings.marginX.toFloat()) }
    var marginY by remember { mutableFloatStateOf(settings.marginY.toFloat()) }
    var anchorX by remember { mutableIntStateOf(settings.anchorX) }
    var anchorY by remember { mutableIntStateOf(settings.anchorY) }
    var suggestions by remember { mutableStateOf(settings.suggestionsEnabled) }
    var vibrate by remember { mutableStateOf(settings.vibrateEnabled) }
    var hWrap by remember { mutableStateOf(settings.horizontalWrap) }
    var vWrap by remember { mutableStateOf(settings.verticalWrap) }
    var numberRow by remember { mutableStateOf(settings.numberRowEnabled) }
    var borderHighlight by remember { mutableStateOf(settings.borderHighlight) }
    var dpadSpeed by remember { mutableFloatStateOf(settings.dpadRepeatRate.toFloat()) }
    var accentColorIdx by remember { mutableIntStateOf(0) }

    val accentColors = listOf(
        "Blue" to Color(0xFF0070D1), "Purple" to Color(0xFFBB86FC),
        "Green" to Color(0xFF107C10), "Red" to Color(0xFFFF4757),
        "Orange" to Color(0xFFFF9F1C), "Cyan" to Color(0xFF1A9FFF),
        "Pink" to Color(0xFFFF6B9D), "Yellow" to Color(0xFFFFD93D)
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Column 1
        Column(modifier = Modifier.weight(1f)) {
            // Layout dropdown
            SectionLabel("Keyboard Layout")
            DropdownSetting(
                options = KeyboardLayouts.ALL_LETTER_LAYOUTS.map { it.id to it.name },
                selected = selectedLayout,
                onSelected = { selectedLayout = it; settings.keyboardLayout = it }
            )

            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel("Visual Style")
            DropdownSetting(
                options = listOf("ps5" to "PS5", "dark" to "Dark", "xbox" to "Xbox", "steam" to "Steam"),
                selected = selectedPreset,
                onSelected = { selectedPreset = it; settings.applyPreset(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel("Accent Color")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                accentColors.forEachIndexed { i, (name, color) ->
                    Box(
                        modifier = Modifier.size(22.dp)
                            .background(color, RoundedCornerShape(6.dp))
                            .border(
                                if (i == accentColorIdx) 2.dp else 0.dp,
                                if (i == accentColorIdx) Color.White else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                accentColorIdx = i
                                settings.accentColor = android.graphics.Color.rgb(
                                    (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()
                                )
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel("Behavior")
            SettingToggle("Word Suggestions", suggestions) { suggestions = it; settings.suggestionsEnabled = it }
            SettingToggle("Vibrate on Press", vibrate) { vibrate = it; settings.vibrateEnabled = it }
            SettingToggle("Number Row", numberRow) { numberRow = it; settings.numberRowEnabled = it }
            SettingToggle("Border Highlight", borderHighlight) { borderHighlight = it; settings.borderHighlight = it }
            SettingToggle("Horizontal Wrap", hWrap) { hWrap = it; settings.horizontalWrap = it }
            SettingToggle("Vertical Wrap", vWrap) { vWrap = it; settings.verticalWrap = it }

            Spacer(modifier = Modifier.height(6.dp))
            var showResetConfirm by remember { mutableStateOf(false) }
            if (!showResetConfirm) {
                Button(onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColor),
                    shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, GlassBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) { Text("Reset Word History", fontSize = 9.sp, color = DangerColor) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { WordSuggestions(context).resetHistory(); showResetConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) { Text("Confirm", fontSize = 9.sp) }
                    Button(onClick = { showResetConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassColor),
                        shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, GlassBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) { Text("Cancel", fontSize = 9.sp, color = TextSecondary) }
                }
            }
        }

        // Column 2 — Size & Position
        Column(modifier = Modifier.weight(1f)) {
            SectionLabel("Size")
            DpadSlider("Height", kbHeight, 20f, 60f, "%") { kbHeight = it; settings.keyboardHeightPercent = it.toInt() }
            DpadSlider("Width", kbWidth, 50f, 100f, "%") { kbWidth = it; settings.keyboardWidthPercent = it.toInt() }
            DpadSlider("Opacity", bgOpacity, 0f, 100f, "%") { bgOpacity = it; settings.bgOpacity = it.toInt() }

            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel("Anchor Position")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("X:", color = TextSecondary, fontSize = 10.sp)
                listOf(0 to "Left", 1 to "Center", 2 to "Right").forEach { (v, label) ->
                    val sel = anchorX == v
                    Box(modifier = Modifier
                        .background(if (sel) AccentColor else GlassColor, RoundedCornerShape(6.dp))
                        .border(1.dp, if (sel) AccentColor else GlassBorder, RoundedCornerShape(6.dp))
                        .clickable { anchorX = v; settings.anchorX = v }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { Text(label, color = if (sel) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Y:", color = TextSecondary, fontSize = 10.sp)
                listOf(0 to "Top", 1 to "Center", 2 to "Bottom").forEach { (v, label) ->
                    val sel = anchorY == v
                    Box(modifier = Modifier
                        .background(if (sel) AccentColor else GlassColor, RoundedCornerShape(6.dp))
                        .border(1.dp, if (sel) AccentColor else GlassBorder, RoundedCornerShape(6.dp))
                        .clickable { anchorY = v; settings.anchorY = v }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { Text(label, color = if (sel) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel("Margins")
            DpadSlider("X Margin", marginX, 0f, 20f, "%") { marginX = it; settings.marginX = it.toInt() }
            DpadSlider("Y Margin", marginY, 0f, 20f, "%") { marginY = it; settings.marginY = it.toInt() }

            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel("Input Speed")
            DpadSlider("D-pad Repeat", dpadSpeed, 30f, 200f, "ms") { dpadSpeed = it; settings.dpadRepeatRate = it.toInt() }
        }
    }
}

@Composable
fun DropdownSetting(options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second ?: selected

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassColor, RoundedCornerShape(8.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(selectedLabel, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(if (expanded) "▲" else "▼", color = AccentColor, fontSize = 10.sp)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(GlassColor),
            ) {
            options.forEach { (id, label) ->
                val isSel = id == selected
                DropdownMenuItem(
                    text = { Text(label, color = if (isSel) AccentColor else TextPrimary, fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onSelected(id); expanded = false },
                    modifier = Modifier.background(if (isSel) AccentGlow else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun SettingToggle(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(if (checked) AccentGlow else Color.Transparent, RoundedCornerShape(6.dp))
            .border(if (checked) BorderStroke(1.dp, AccentColor.copy(0.3f)) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (checked) TextPrimary else TextSecondary, fontSize = 11.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal)
        Switch(
            checked = checked, onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = AccentColor,
                uncheckedThumbColor = TextSecondary, uncheckedTrackColor = GlassColor
            ),
            modifier = Modifier.height(18.dp)
        )
    }
}

@Composable
fun DpadSlider(label: String, value: Float, min: Float, max: Float, unit: String, onChanged: (Float) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val step = (max - min) / 20f
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .background(
                when { editing -> AccentGlow; isFocused -> AccentGlow.copy(alpha = 0.3f); else -> Color.Transparent },
                RoundedCornerShape(6.dp)
            )
            .border(
                if (editing) BorderStroke(1.dp, AccentColor) else if (isFocused) BorderStroke(1.dp, GlassBorder) else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(6.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                    when (event.key) {
                        androidx.compose.ui.input.key.Key.DirectionCenter,
                        androidx.compose.ui.input.key.Key.Enter,
                        androidx.compose.ui.input.key.Key.ButtonA -> { editing = !editing; true }
                        androidx.compose.ui.input.key.Key.DirectionLeft -> { if (editing) { onChanged((value - step).coerceAtLeast(min)); true } else false }
                        androidx.compose.ui.input.key.Key.DirectionRight -> { if (editing) { onChanged((value + step).coerceAtMost(max)); true } else false }
                        androidx.compose.ui.input.key.Key.ButtonB, androidx.compose.ui.input.key.Key.Back -> { if (editing) { editing = false; true } else false }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = if (editing) TextPrimary else TextSecondary, fontSize = 10.sp)
            Row {
                if (editing) Text("◀ ", color = AccentColor, fontSize = 9.sp)
                Text("${value.toInt()}$unit", color = AccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (editing) Text(" ▶", color = AccentColor, fontSize = 9.sp)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(GlassColor, RoundedCornerShape(2.dp))) {
            Box(modifier = Modifier.fillMaxHeight()
                .fillMaxWidth(((value - min) / (max - min)).coerceIn(0f, 1f))
                .background(if (editing) AccentColor else AccentColor.copy(0.6f), RoundedCornerShape(2.dp)))
        }
    }
}

// --- CONTROLS TAB ---

@Composable
fun ControlsTab() {
    val controls = listOf(
        "✕ Cross" to "Select", "△ Triangle" to "Space", "□ Square" to "Backspace",
        "○ Circle" to "Close", "D-pad" to "Navigate", "L1 / R1" to "Cursor ←→",
        "L2 hold" to "Shift", "R2" to "Enter", "L2+R2" to "New line",
        "L3" to "Symbols", "Share" to "Voice", "Options" to "Done"
    )
    GlassCard {
        Text("Controller Mapping", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val rows = controls.chunked(2)
        rows.forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (button, action) ->
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.background(SurfaceColor, RoundedCornerShape(4.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(button, color = AccentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(action, color = TextSecondary, fontSize = 10.sp)
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// --- TEST TAB ---

@Composable
fun TestTab() {
    val focusManager = LocalFocusManager.current
    var testText by remember { mutableStateOf("") }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Test Input", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Focus field to test keyboard", color = TextSecondary, fontSize = 10.sp)
            if (testText.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Text("${testText.length} chars", color = TextSecondary, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear", color = AccentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { testText = ""; focusManager.clearFocus() })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = testText,
            onValueChange = { testText = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                .onKeyEvent { event ->
                    // Circle/Back when keyboard is hidden should unfocus
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                        (event.key == androidx.compose.ui.input.key.Key.Back ||                         event.key == androidx.compose.ui.input.key.Key.ButtonB)) {
                        focusManager.clearFocus()
                        true
                    } else false
                },
            placeholder = { Text("Type something here...", color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = AccentColor, focusedBorderColor = AccentColor,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = SurfaceColor, unfocusedContainerColor = SurfaceColor
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// --- DEBUG TAB ---

@Composable
fun DebugTab() {
    val context = LocalContext.current
    var loggingEnabled by remember { mutableStateOf(DebugLogger.isEnabled()) }
    var logs by remember { mutableStateOf(DebugLogger.getRecentLogs()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            logs = DebugLogger.getRecentLogs()
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Left: Controller viz
        GlassCard(modifier = Modifier.weight(1f)) {
            Text("Controller Input", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(SurfaceColor, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CtrlBtn("↑", false); Row { CtrlBtn("←", false); Spacer(Modifier.width(18.dp)); CtrlBtn("→", false) }; CtrlBtn("↓", false)
                }
                Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CtrlBtn("△", false, Color(0xFF00D4AA)); Row { CtrlBtn("□", false, Color(0xFFFF6B9D)); Spacer(Modifier.width(18.dp)); CtrlBtn("○", false, Color(0xFFFF4757)) }; CtrlBtn("✕", false, Color(0xFF6B9FFF))
                }
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MiniBtn("L1", false); MiniBtn("L2", false); MiniBtn("R2", false); MiniBtn("R1", false)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StickDot("L", 0f, 0f, false); StickDot("R", 0f, 0f, false)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MiniBtn("Share", false); MiniBtn("OPT", false)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Connect controller to see live input", color = TextSecondary, fontSize = 9.sp)
        }

        // Right: Logs
        GlassCard(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Debug Log", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Switch(checked = loggingEnabled, onCheckedChange = { loggingEnabled = it; DebugLogger.setEnabled(context, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentColor,
                        uncheckedThumbColor = TextSecondary, uncheckedTrackColor = GlassColor),
                    modifier = Modifier.height(18.dp))
            }
            if (loggingEnabled) {
                Text("File: ${DebugLogger.getLogFilePath()}", color = TextSecondary, fontSize = 7.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AccentButton("Refresh") { logs = DebugLogger.getRecentLogs() }
                Button(onClick = { DebugLogger.clearLogs(); logs = emptyList() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColor),
                    shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, GlassBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)) { Text("Clear", fontSize = 9.sp, color = DangerColor) }
            }
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)
                .background(Color(0xFF060810), RoundedCornerShape(6.dp))
                .border(1.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(6.dp).verticalScroll(rememberScrollState())) {
                Column {
                    if (logs.isEmpty()) Text("No logs", color = TextSecondary.copy(0.4f), fontSize = 8.sp)
                    else logs.takeLast(50).forEach { line ->
                        Text(line, color = when {
                            "ERROR" in line -> DangerColor; "WARN" in line -> Color(0xFFFFB86B)
                            "IME" in line -> Color(0xFF6BFFB8); else -> TextSecondary
                        }, fontSize = 7.sp, lineHeight = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun CtrlBtn(label: String, pressed: Boolean, activeColor: Color = AccentColor) {
    Box(modifier = Modifier.size(26.dp).background(if (pressed) activeColor else SurfaceColor, RoundedCornerShape(13.dp))
        .border(1.dp, if (pressed) activeColor else GlassBorder, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center) {
        Text(label, color = if (pressed) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MiniBtn(label: String, pressed: Boolean) {
    Box(modifier = Modifier.height(14.dp).background(if (pressed) AccentColor else SurfaceColor, RoundedCornerShape(3.dp))
        .border(1.dp, if (pressed) AccentColor else GlassBorder, RoundedCornerShape(3.dp))
        .padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (pressed) Color.White else TextSecondary, fontSize = 6.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StickDot(label: String, x: Float, y: Float, pressed: Boolean) {
    Box(modifier = Modifier.size(32.dp).background(SurfaceColor, RoundedCornerShape(16.dp))
        .border(1.dp, if (pressed) AccentColor else GlassBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.offset((x * 8).dp, (y * 8).dp).size(6.dp)
            .background(if (pressed) AccentColor else TextSecondary.copy(0.5f), RoundedCornerShape(3.dp)))
    }
}
