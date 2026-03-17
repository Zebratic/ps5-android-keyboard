package com.zebratic.sensekeyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.offset

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        setContent {
            var showExitDialog by remember { mutableStateOf(false) }

            // Handle back press
            DisposableEffect(Unit) {
                val callback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        showExitDialog = true
                    }
                }
                onBackPressedDispatcher.addCallback(callback)
                onDispose { callback.remove() }
            }

            SenseKeyboardApp(
                onEnableKeyboard = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                onSelectKeyboard = {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            )

            if (showExitDialog) {
                ExitDialog(
                    onConfirm = { finish() },
                    onDismiss = { showExitDialog = false }
                )
            }
        }
    }
}

// PS5 Glassmorphism colors
private val BgColor = Color(0xFF080A14)
private val SurfaceColor = Color(0xFF101424)
private val CardColor = Color(0x33FFFFFF) // translucent glass
private val CardBorder = Color(0x22FFFFFF)
private val AccentColor = Color(0xFF0070D1)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF7A7F95)
private val GlassHighlight = Color(0x0AFFFFFF)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151930)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(GlassHighlight, Color.Transparent)
                    )
                )
                .padding(12.dp),
            content = content
        )
    }
}

@Composable
fun SenseKeyboardApp(onEnableKeyboard: () -> Unit, onSelectKeyboard: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Setup", "Settings", "Controls", "Debug")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Nav bar — glassmorphism style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor)
                .border(width = 0.5.dp, color = CardBorder, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎮", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SenseKeyboard", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(20.dp))

            tabs.forEachIndexed { i, label ->
                val isSelected = selectedTab == i
                var isFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable { selectedTab = i }
                        .onKeyEvent { event ->
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                                (event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
                                 event.key == androidx.compose.ui.input.key.Key.Enter ||
                                 event.key == androidx.compose.ui.input.key.Key.ButtonA)) {
                                selectedTab = i; true
                            } else false
                        }
                        .background(
                            when {
                                isSelected -> AccentColor
                                isFocused -> AccentColor.copy(alpha = 0.4f)
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            2.dp,
                            when {
                                isFocused -> AccentColor
                                isSelected -> Color.Transparent
                                else -> Color(0x11FFFFFF)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        color = when {
                            isSelected || isFocused -> Color.White
                            else -> TextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            when (selectedTab) {
                0 -> SetupTab(onEnableKeyboard, onSelectKeyboard)
                1 -> SettingsTab()
                2 -> ControlsTab()
                3 -> DebugTab()
            }
        }
    }
}

@Composable
fun ExitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.width(280.dp)) {
            Text("Exit SenseKeyboard?", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Are you sure you want to close the app?", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                ) { Text("No", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D3E)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                ) { Text("Yes", fontSize = 12.sp, color = TextSecondary) }
            }
        }
    }
}

// --- Setup Tab ---
@Composable
fun SetupTab(onEnableKeyboard: () -> Unit, onSelectKeyboard: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard(modifier = Modifier.weight(1f)) {
            StepHeader("1", "Enable")
            Text("Enable SenseKeyboard in input settings", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            AccentButton("Open Settings", onEnableKeyboard)
        }
        GlassCard(modifier = Modifier.weight(1f)) {
            StepHeader("2", "Select")
            Text("Set as active keyboard", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            AccentButton("Select", onSelectKeyboard)
        }
        GlassCard(modifier = Modifier.weight(1f)) {
            StepHeader("3", "Type!")
            Text("Open any text field. Connect DualSense via Bluetooth.", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun StepHeader(step: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(22.dp).background(AccentColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) { Text(step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        modifier = Modifier.height(28.dp)
    ) { Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
}

// --- Settings Tab ---
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

    var showHintBar by remember { mutableStateOf(settings.showHintBar) }
    var numberRow by remember { mutableStateOf(settings.numberRowEnabled) }
    var highlightStyle by remember { mutableStateOf(settings.highlightStyle) }
    var highlightBorderSize by remember { mutableFloatStateOf(settings.highlightBorderSize.toFloat()) }
    var keyRounding by remember { mutableFloatStateOf(settings.keyRounding.toFloat()) }
    var clickAnimation by remember { mutableStateOf(settings.clickAnimation) }
    var hWrap by remember { mutableStateOf(settings.horizontalWrap) }
    var vWrap by remember { mutableStateOf(settings.verticalWrap) }
    var dpadSpeed by remember { mutableFloatStateOf(settings.dpadRepeatRate.toFloat()) }

    Column {
    // Inline test input — press X to enter edit mode, navigable past with D-pad
    val focusManager = LocalFocusManager.current
    val settingsFocusTarget = remember { FocusRequester() }
    var testText by remember { mutableStateOf("") }
    var testEditing by remember { mutableStateOf(false) }
    val testFocusReq = remember { FocusRequester() }
    var testBoxFocused by remember { mutableStateOf(false) }

    if (testEditing) {
        OutlinedTextField(
            value = testText,
            onValueChange = { testText = it },
            modifier = Modifier.fillMaxWidth().height(42.dp)
                .focusRequester(testFocusReq)
                .onKeyEvent { event ->
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                        (event.key == androidx.compose.ui.input.key.Key.Back || event.key == androidx.compose.ui.input.key.Key.ButtonB)) {
                        testEditing = false; true
                    } else false
                },
            placeholder = { Text("Type something...", color = TextSecondary.copy(alpha = 0.4f), fontSize = 11.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentColor, unfocusedBorderColor = CardBorder,
                focusedContainerColor = SurfaceColor, unfocusedContainerColor = SurfaceColor,
                cursorColor = AccentColor
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        LaunchedEffect(Unit) { testFocusReq.requestFocus() }
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().height(36.dp)
                .onFocusChanged { testBoxFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                        (event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
                         event.key == androidx.compose.ui.input.key.Key.Enter ||
                         event.key == androidx.compose.ui.input.key.Key.ButtonA)) {
                        testEditing = true; true
                    } else false
                }
                .background(
                    if (testBoxFocused) AccentColor.copy(alpha = 0.15f) else SurfaceColor,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    if (testBoxFocused) 2.dp else 1.dp,
                    if (testBoxFocused) AccentColor else CardBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                if (testText.isEmpty()) "Press ✕ to test keyboard" else testText,
                color = if (testText.isEmpty()) TextSecondary.copy(alpha = 0.4f) else TextPrimary,
                fontSize = 11.sp, maxLines = 1
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Column 1
        Column(modifier = Modifier.weight(1f)) {
            GlassCard(modifier = Modifier.focusRequester(settingsFocusTarget)) {
                SectionLabel("Keyboard Layout")
                DropdownSetting(
                    options = KeyboardLayouts.ALL_LETTER_LAYOUTS.map { it.id to it.name },
                    selected = selectedLayout,
                    onSelected = { selectedLayout = it; settings.keyboardLayout = it }
                )

                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel("Visual Style")
                DropdownSetting(
                    options = listOf("standard" to "Standard", "rounded" to "Rounded", "minimal" to "Minimal", "retro" to "Retro"),
                    selected = selectedPreset,
                    onSelected = { selectedPreset = it; settings.applyPreset(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel("Accent Color")
                ColorPicker(
                    selected = settings.accentColor,
                    onSelected = { settings.accentColor = it }
                )

                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel("Highlight Style")
                DropdownSetting(
                    options = listOf("border" to "Border", "fill" to "Fill", "glow" to "Glow", "none" to "None"),
                    selected = highlightStyle,
                    onSelected = { highlightStyle = it; settings.highlightStyle = it }
                )

                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel("Click Animation")
                DropdownSetting(
                    options = listOf("fill" to "Fill", "pop" to "Pop", "flash" to "Flash", "none" to "None"),
                    selected = clickAnimation,
                    onSelected = { clickAnimation = it; settings.clickAnimation = it }
                )

                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel("Navigation Effect")
                var navEffect by remember { mutableStateOf(settings.navEffect) }
                DropdownSetting(
                    options = listOf("wind" to "Wind Particles", "slide" to "Sliding Border", "ripple" to "Ripple", "trail" to "Ghost Trail", "none" to "None"),
                    selected = navEffect,
                    onSelected = { navEffect = it; settings.navEffect = it }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            GlassCard {
                SectionLabel("Key Appearance")
                SettingSlider("Border Size", highlightBorderSize, 1f, 8f, "dp") { highlightBorderSize = it; settings.highlightBorderSize = it.toInt() }
                Spacer(modifier = Modifier.height(4.dp))
                SettingSlider("Corner Rounding", keyRounding, 0f, 24f, "dp") { keyRounding = it; settings.keyRounding = it.toInt() }

                Spacer(modifier = Modifier.height(8.dp))
                SectionLabel("Key Colors")
                Text("Primary (letters)", color = TextSecondary, fontSize = 9.sp)
                ColorPicker(selected = settings.keyColor, onSelected = { settings.keyColor = it })
                Spacer(modifier = Modifier.height(4.dp))
                Text("Secondary (numbers, actions)", color = TextSecondary, fontSize = 9.sp)
                ColorPicker(selected = settings.secondaryKeyColor, onSelected = { settings.secondaryKeyColor = it })
            }

            Spacer(modifier = Modifier.height(10.dp))
            GlassCard {
                SectionLabel("Font")
                var selectedFont by remember { mutableStateOf(settings.fontFamily) }
                var fontScale by remember { mutableFloatStateOf(settings.fontScale.toFloat()) }
                DropdownSetting(
                    options = listOf(
                        "default" to "Default (Sans Medium)",
                        "sans-serif-light" to "Sans Light",
                        "sans-serif-condensed" to "Sans Condensed",
                        "sans-serif-thin" to "Sans Thin",
                        "monospace" to "Monospace",
                        "serif" to "Serif",
                        "casual" to "Casual",
                        "cursive" to "Cursive"
                    ),
                    selected = selectedFont,
                    onSelected = { selectedFont = it; settings.fontFamily = it }
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingSlider("Font Scale", fontScale, 50f, 200f, "%") { fontScale = it; settings.fontScale = it.toInt() }
            }

            Spacer(modifier = Modifier.height(10.dp))
            GlassCard {
                SectionLabel("Behavior")
                SettingSwitch("Word Suggestions", suggestions) { suggestions = it; settings.suggestionsEnabled = it }
                Spacer(modifier = Modifier.height(6.dp))
                SettingSwitch("Number Row", numberRow) { numberRow = it; settings.numberRowEnabled = it }
                Spacer(modifier = Modifier.height(6.dp))
                SettingSwitch("Show Hint Bar", showHintBar) { showHintBar = it; settings.showHintBar = it }
                Spacer(modifier = Modifier.height(6.dp))
                SettingSwitch("Horizontal Wrap", hWrap) { hWrap = it; settings.horizontalWrap = it }
                Spacer(modifier = Modifier.height(6.dp))
                SettingSwitch("Vertical Wrap", vWrap) { vWrap = it; settings.verticalWrap = it }
                Spacer(modifier = Modifier.height(8.dp))
                SettingSlider("D-pad Speed", dpadSpeed, 30f, 200f, "ms") {
                    dpadSpeed = it; settings.dpadRepeatRate = it.toInt()
                }
                Spacer(modifier = Modifier.height(8.dp))
                ResetWordHistoryButton(context)
            }
        }

        // Column 2
        Column(modifier = Modifier.weight(1f)) {
            GlassCard {
                SectionLabel("Size")
                SettingSlider("Height", kbHeight, 20f, 60f, "%") {
                    kbHeight = it; settings.keyboardHeightPercent = it.toInt()
                }
                SettingSlider("Width", kbWidth, 50f, 100f, "%") {
                    kbWidth = it; settings.keyboardWidthPercent = it.toInt()
                }
                SettingSlider("Opacity", bgOpacity, 0f, 100f, "%") {
                    bgOpacity = it; settings.bgOpacity = it.toInt()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            GlassCard {
                SectionLabel("Position")
                Text("X Anchor", color = TextSecondary, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                AnchorPicker(
                    options = listOf("Left", "Center", "Right"),
                    selected = anchorX,
                    onSelected = { anchorX = it; settings.anchorX = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Y Anchor", color = TextSecondary, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                AnchorPicker(
                    options = listOf("Top", "Center", "Bottom"),
                    selected = anchorY,
                    onSelected = { anchorY = it; settings.anchorY = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingSlider("X Margin", marginX, 0f, 20f, "%") {
                    marginX = it; settings.marginX = it.toInt()
                }
                SettingSlider("Y Margin", marginY, 0f, 20f, "%") {
                    marginY = it; settings.marginY = it.toInt()
                }
            }
        }
    }

    // Action bar buttons section
    Spacer(modifier = Modifier.height(10.dp))
    GlassCard {
        SectionLabel("On-Screen Buttons")
        var showSpacebar by remember { mutableStateOf(settings.showSpacebar) }
        var showEnter by remember { mutableStateOf(settings.showEnterBtn) }
        var showBackspace by remember { mutableStateOf(settings.showBackspaceBtn) }
        var showArrows by remember { mutableStateOf(settings.showArrowKeys) }
        var showVoice by remember { mutableStateOf(settings.showVoiceBtn) }
        var showSymbols by remember { mutableStateOf(settings.showSymbolsBtn) }
        var showDialpad by remember { mutableStateOf(settings.showDialpadBtn) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                SettingSwitch("Spacebar", showSpacebar) { showSpacebar = it; settings.showSpacebar = it }
                SettingSwitch("Enter", showEnter) { showEnter = it; settings.showEnterBtn = it }
                SettingSwitch("Backspace", showBackspace) { showBackspace = it; settings.showBackspaceBtn = it }
                SettingSwitch("Arrow Keys", showArrows) { showArrows = it; settings.showArrowKeys = it }
            }
            Column(modifier = Modifier.weight(1f)) {
                SettingSwitch("Voice", showVoice) { showVoice = it; settings.showVoiceBtn = it }
                SettingSwitch("Symbols", showSymbols) { showSymbols = it; settings.showSymbolsBtn = it }
                SettingSwitch("Dialpad", showDialpad) { showDialpad = it; settings.showDialpadBtn = it }
            }
        }
    }
    } // close Column wrapper
}

@Composable
fun DropdownSetting(options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second ?: selected

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                        (event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
                         event.key == androidx.compose.ui.input.key.Key.Enter ||
                         event.key == androidx.compose.ui.input.key.Key.ButtonA)) {
                        expanded = !expanded; true
                    } else false
                }
                .background(if (isFocused) AccentColor.copy(alpha = 0.15f) else Color(0xFF1A1D30), RoundedCornerShape(8.dp))
                .border(if (isFocused) 2.dp else 1.dp, if (isFocused) AccentColor else CardBorder, RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedLabel, color = TextPrimary, fontSize = 11.sp)
            Text(if (expanded) "▲" else "▼", color = AccentColor, fontSize = 10.sp)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1D30))
        ) {
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = {
                        Text(label,
                            color = if (id == selected) AccentColor else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (id == selected) FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = { onSelected(id); expanded = false },
                    modifier = if (id == selected)
                        Modifier.background(AccentColor.copy(alpha = 0.1f))
                    else Modifier
                )
            }
        }
    }
}

@Composable
fun ColorPicker(selected: Int, onSelected: (Int) -> Unit) {
    val colors = listOf(
        0xFF0070D1.toInt() to "PS5 Blue",
        0xFFBB86FC.toInt() to "Purple",
        0xFF00BFA5.toInt() to "Teal",
        0xFF107C10.toInt() to "Xbox Green",
        0xFF1A9FFF.toInt() to "Steam Blue",
        0xFFFF6B6B.toInt() to "Red",
        0xFFFF9F1C.toInt() to "Orange",
        0xFFFFD93D.toInt() to "Gold",
        0xFFFF6B9D.toInt() to "Pink",
        0xFFFFFFFF.toInt() to "White"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        colors.forEach { (color, _) ->
            val isSelected = selected == color
            var isFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                            (event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
                             event.key == androidx.compose.ui.input.key.Key.Enter ||
                             event.key == androidx.compose.ui.input.key.Key.ButtonA)) {
                            onSelected(color); true
                        } else false
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(color.toLong() or 0xFF000000L))
                    .border(
                        when {
                            isSelected -> 3.dp
                            isFocused -> 2.dp
                            else -> 0.dp
                        },
                        when {
                            isSelected -> Color.White
                            isFocused -> AccentColor
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelected(color) }
            )
        }
    }
}

@Composable
fun AnchorPicker(options: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            var isFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                            (event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
                             event.key == androidx.compose.ui.input.key.Key.Enter ||
                             event.key == androidx.compose.ui.input.key.Key.ButtonA)) {
                            onSelected(i); true
                        } else false
                    }
                    .background(
                        when {
                            isSel -> AccentColor
                            isFocused -> AccentColor.copy(alpha = 0.3f)
                            else -> Color(0xFF1A1D30)
                        },
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        if (isFocused) 2.dp else if (isSel) 0.dp else 1.dp,
                        when {
                            isFocused -> AccentColor
                            isSel -> Color.Transparent
                            else -> CardBorder
                        },
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelected(i) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (isSel || isFocused) Color.White else TextSecondary,
                    fontSize = 10.sp, fontWeight = if (isSel || isFocused) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun ResetWordHistoryButton(context: android.content.Context) {
    var showConfirm by remember { mutableStateOf(false) }
    if (!showConfirm) {
        Button(
            onClick = { showConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1520)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
        ) { Text("Reset Word History", fontSize = 10.sp, color = Color(0xFFFF6B6B)) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { WordSuggestions(context).resetHistory(); showConfirm = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) { Text("Confirm", fontSize = 10.sp, color = Color.White) }
            Button(
                onClick = { showConfirm = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D3E)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) { Text("Cancel", fontSize = 10.sp) }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                    (event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
                     event.key == androidx.compose.ui.input.key.Key.Enter ||
                     event.key == androidx.compose.ui.input.key.Key.ButtonA)) {
                    onChanged(!checked); true
                } else false
            }
            .background(
                when {
                    isFocused -> AccentColor.copy(alpha = 0.25f)
                    checked -> AccentColor.copy(alpha = 0.08f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(6.dp)
            )
            .border(
                if (isFocused) 2.dp else 0.dp,
                if (isFocused) AccentColor else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (checked || isFocused) TextPrimary else TextSecondary, fontSize = 11.sp,
            fontWeight = if (checked || isFocused) FontWeight.Medium else FontWeight.Normal)
        Switch(
            checked = checked, onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentColor,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF2A2D3E)
            ),
            modifier = Modifier.height(20.dp)
        )
    }
}

@Composable
fun SettingSlider(label: String, value: Float, min: Float, max: Float, unit: String, onChanged: (Float) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val step = (max - min) / 20f
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .background(
                when {
                    editing -> AccentColor.copy(alpha = 0.25f)
                    isFocused -> AccentColor.copy(alpha = 0.15f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(6.dp)
            )
            .border(
                if (isFocused || editing) 2.dp else 0.dp,
                if (editing) AccentColor else if (isFocused) AccentColor.copy(alpha = 0.7f) else Color.Transparent,
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
                        androidx.compose.ui.input.key.Key.DirectionLeft -> {
                            if (editing) { onChanged((value - step).coerceAtLeast(min)); true } else false
                        }
                        androidx.compose.ui.input.key.Key.DirectionRight -> {
                            if (editing) { onChanged((value + step).coerceAtMost(max)); true } else false
                        }
                        androidx.compose.ui.input.key.Key.ButtonB,
                        androidx.compose.ui.input.key.Key.Back,
                        androidx.compose.ui.input.key.Key.Escape -> {
                            if (editing) { editing = false; true } else false
                        }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = if (editing) TextPrimary else TextSecondary, fontSize = 11.sp)
            Row {
                if (editing) Text("◀ ", color = AccentColor, fontSize = 9.sp)
                Text("${value.toInt()}$unit", color = AccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (editing) Text(" ▶", color = AccentColor, fontSize = 9.sp)
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Color(0xFF1A1D30), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxHeight()
                    .fillMaxWidth(((value - min) / (max - min)).coerceIn(0f, 1f))
                    .background(if (editing) AccentColor else AccentColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }
        if (isFocused && !editing) {
            Text("Press ✕ to edit", color = TextSecondary.copy(alpha = 0.4f), fontSize = 7.sp)
        }
    }
}

// --- Controls Tab ---
@Composable
fun ControlsTab() {
    val controls = listOf(
        "✕ Cross" to "Select", "△ Triangle" to "Space", "□ Square" to "Backspace",
        "○ Circle" to "Close", "D-pad" to "Navigate", "L1 / R1" to "Cursor ←→",
        "L2 hold" to "Shift", "R2" to "Enter", "L2+R2" to "New line",
        "L3" to "Symbols", "Share" to "Voice", "Options" to "Done"
    )

    GlassCard {
        Text("Controller Mapping", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        val rows = controls.chunked(2)
        rows.forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (button, action) ->
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.background(Color(0xFF1A1D30), RoundedCornerShape(4.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
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

// --- Test Tab ---
@Composable
fun TestTab() {
    var testText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Test Input", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Focus the field to test", color = TextSecondary, fontSize = 10.sp)
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
                    // Allow circle/B to clear focus from the text field
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                        (event.key == androidx.compose.ui.input.key.Key.ButtonB ||
                         event.key == androidx.compose.ui.input.key.Key.Back)) {
                        focusManager.clearFocus()
                        true
                    } else false
                },
            placeholder = { Text("Type something here...", color = TextSecondary.copy(alpha = 0.4f), fontSize = 12.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = AccentColor, focusedBorderColor = AccentColor,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = Color(0xFF0E1020), unfocusedContainerColor = Color(0xFF0E1020)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// --- Debug Tab ---
@Composable
fun DebugTab() {
    val context = LocalContext.current
    var loggingEnabled by remember { mutableStateOf(DebugLogger.isEnabled()) }
    var logs by remember { mutableStateOf(DebugLogger.getRecentLogs()) }
    var pressedButtons by remember { mutableStateOf(setOf<String>()) }
    var l2Value by remember { mutableFloatStateOf(0f) }
    var r2Value by remember { mutableFloatStateOf(0f) }
    var leftStick by remember { mutableStateOf(Pair(0f, 0f)) }
    var rightStick by remember { mutableStateOf(Pair(0f, 0f)) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            logs = DebugLogger.getRecentLogs()
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Controller visualization
        Column(modifier = Modifier.weight(1f)) {
            GlassCard {
                SectionLabel("Controller Input")
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                        .background(Color(0xFF0A0C18), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ControllerButton("↑", "DPAD_UP" in pressedButtons)
                        Row {
                            ControllerButton("←", "DPAD_LEFT" in pressedButtons)
                            Spacer(modifier = Modifier.width(20.dp))
                            ControllerButton("→", "DPAD_RIGHT" in pressedButtons)
                        }
                        ControllerButton("↓", "DPAD_DOWN" in pressedButtons)
                    }
                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ControllerButton("△", "TRIANGLE" in pressedButtons, Color(0xFF00D4AA))
                        Row {
                            ControllerButton("□", "SQUARE" in pressedButtons, Color(0xFFFF6B9D))
                            Spacer(modifier = Modifier.width(20.dp))
                            ControllerButton("○", "CIRCLE" in pressedButtons, Color(0xFFFF6B6B))
                        }
                        ControllerButton("✕", "CROSS" in pressedButtons, Color(0xFF6B9FFF))
                    }
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            MiniButton("L1", "L1" in pressedButtons)
                            TriggerBar("L2", l2Value)
                            TriggerBar("R2", r2Value)
                            MiniButton("R1", "R1" in pressedButtons)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StickIndicator("L", leftStick.first, leftStick.second, "L3" in pressedButtons)
                            StickIndicator("R", rightStick.first, rightStick.second, "R3" in pressedButtons)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Active: ${if (pressedButtons.isEmpty()) "none" else pressedButtons.joinToString(", ")}",
                    color = if (pressedButtons.isEmpty()) TextSecondary else AccentColor, fontSize = 9.sp)
            }
        }

        // Logs
        Column(modifier = Modifier.weight(1f)) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Debug Logging")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = loggingEnabled,
                        onCheckedChange = { loggingEnabled = it; DebugLogger.setEnabled(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = AccentColor,
                            uncheckedThumbColor = TextSecondary, uncheckedTrackColor = Color(0xFF2A2D3E)
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }
                if (loggingEnabled) {
                    Text("Path: ${DebugLogger.getLogFilePath()}", color = TextSecondary, fontSize = 7.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccentButton("Refresh") { logs = DebugLogger.getRecentLogs() }
                    Button(
                        onClick = { DebugLogger.clearLogs(); logs = emptyList() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1520)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("Clear", fontSize = 10.sp, color = Color(0xFFFF6B6B)) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .background(Color(0xFF060810), RoundedCornerShape(6.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                        .padding(6.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        if (logs.isEmpty()) {
                            Text("No logs yet", color = TextSecondary.copy(alpha = 0.3f), fontSize = 8.sp)
                        } else {
                            logs.takeLast(50).forEach { line ->
                                Text(line, color = when {
                                    "ERROR" in line -> Color(0xFFFF6B6B)
                                    "WARN" in line -> Color(0xFFFFB86B)
                                    "IME" in line -> Color(0xFF6BFFB8)
                                    else -> TextSecondary
                                }, fontSize = 7.sp, lineHeight = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControllerButton(label: String, pressed: Boolean, activeColor: Color = AccentColor) {
    Box(
        modifier = Modifier.size(26.dp)
            .background(if (pressed) activeColor else Color(0xFF0E1020), RoundedCornerShape(13.dp))
            .border(1.dp, if (pressed) activeColor else CardBorder, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (pressed) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun MiniButton(label: String, pressed: Boolean) {
    Box(
        modifier = Modifier.height(16.dp)
            .background(if (pressed) AccentColor else Color(0xFF0E1020), RoundedCornerShape(4.dp))
            .border(1.dp, if (pressed) AccentColor else CardBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (pressed) Color.White else TextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun TriggerBar(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 7.sp)
        Box(modifier = Modifier.width(24.dp).height(10.dp).background(Color(0xFF0E1020), RoundedCornerShape(3.dp))) {
            Box(modifier = Modifier.fillMaxHeight()
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .background(if (value > 0.5f) AccentColor else Color(0xFF2A2D3E), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
fun StickIndicator(label: String, x: Float, y: Float, pressed: Boolean) {
    Box(
        modifier = Modifier.size(34.dp)
            .background(Color(0xFF0E1020), RoundedCornerShape(17.dp))
            .border(1.dp, if (pressed) AccentColor else CardBorder, RoundedCornerShape(17.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.offset(x = (x * 10).dp, y = (y * 10).dp).size(8.dp)
            .background(if (pressed) AccentColor else Color(0xFF4A4F65), RoundedCornerShape(4.dp)))
        Text(label, color = TextSecondary.copy(alpha = 0.3f), fontSize = 7.sp)
    }
}