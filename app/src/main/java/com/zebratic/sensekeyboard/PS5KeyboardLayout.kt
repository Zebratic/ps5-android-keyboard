package com.zebratic.sensekeyboard

import android.content.Context
import android.graphics.*

import android.util.AttributeSet
import android.view.View

class PS5KeyboardLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Callbacks
    var onCharInput: ((Char) -> Unit)? = null
    var onSpace: (() -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null
    var onSpeech: (() -> Unit)? = null
    var onSuggestionPicked: ((String) -> Unit)? = null
    var onCursorLeft: (() -> Unit)? = null
    var onCursorRight: (() -> Unit)? = null
    var onCopy: (() -> Unit)? = null
    var onPaste: (() -> Unit)? = null

    // State
    private var focusRow = -1 // -1 = suggestion row, 0+ = key rows
    private var focusCol = 0
    private var clickAnimRow = -2
    private var clickAnimCol = -2
    private var clickAnimStart = 0L
    private val clickAnimDuration = 150L

    // Slide animation state
    private var slideFromX = 0f
    private var slideFromY = 0f
    private var slideToX = 0f
    private var slideToY = 0f
    private var slideStart = 0L
    private val slideDuration = 100L
    private var slideActive = false

    // Ripple effect
    private var rippleX = 0f
    private var rippleY = 0f
    private var rippleStart = 0L
    private val rippleDuration = 200L

    // Trail effect
    private data class TrailPoint(val x: Float, val y: Float, val w: Float, val h: Float, var alpha: Float)
    private val trailPoints = mutableListOf<TrailPoint>()
    private var shifted = false
    private var shiftLocked = false
    private var symbolMode = false
    private var dialpadMode = false
    private var listening = false
    private var currentLayoutId = "us"

    // Word suggestions
    private var suggestions = listOf<String>()
    private var currentWord = ""
    private var lastCompletedWord = ""

    private val wordSuggestions = WordSuggestions(context)

    // Wind effect state
    private data class WindParticle(
        var x: Float, var y: Float, var alpha: Float,
        var dx: Float, var dy: Float, var life: Float, var size: Float
    )
    private val windParticles = mutableListOf<WindParticle>()
    private val windPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastMoveDir = 0 // 0=none, 1=right, 2=left, 3=down, 4=up
    private var windAnimating = false

    // Settings
    val settings = KeyboardSettings(context)

    // Paints (initialized from settings, can be updated)
    private val dp = resources.displayMetrics.density
    private val bgPaint = Paint()
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondaryKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(12f * dp, BlurMaskFilter.Blur.NORMAL)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E91A4"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E91A4"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val hintBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val hintBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF3A3D4E") }
    private val shiftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFF9F1C"); textAlign = Paint.Align.RIGHT
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val listenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFF3D3D"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val listenBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#33FF3D3D") }
    private val suggPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val suggFocusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val suggBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF1E2130") }
    private val suggFocusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF0070D1") }
    private val layoutLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#660070D1"); textAlign = Paint.Align.LEFT
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    init {
        currentLayoutId = settings.keyboardLayout
        isFocusable = true
        isFocusableInTouchMode = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updatePaints()
    }

    fun reloadSettings() {
        currentLayoutId = settings.keyboardLayout
        updatePaints()
        requestLayout()
        invalidate()
    }

    private fun updatePaints() {
        val alpha = (settings.bgOpacity * 255 / 100)
        bgPaint.color = Color.argb(alpha,
            Color.red(settings.bgColor), Color.green(settings.bgColor), Color.blue(settings.bgColor))
        keyPaint.color = settings.keyColor or (0xFF shl 24).toInt()
        secondaryKeyPaint.color = settings.secondaryKeyColor or (0xFF shl 24).toInt()
        focusPaint.color = settings.accentColor or (0xFF shl 24).toInt()
        // Sync suggestion colors with theme
        suggBgPaint.color = settings.secondaryKeyColor or (0xFF shl 24).toInt()
        suggFocusBgPaint.color = settings.accentColor or (0xFF shl 24).toInt()
        suggPaint.color = Color.argb(200, Color.red(settings.textColor), Color.green(settings.textColor), Color.blue(settings.textColor))
        suggFocusPaint.color = settings.textColor or (0xFF shl 24).toInt()
        glowPaint.color = Color.argb(64,
            Color.red(settings.accentColor), Color.green(settings.accentColor), Color.blue(settings.accentColor))
        textPaint.color = settings.textColor or (0xFF shl 24).toInt()
        dimTextPaint.color = Color.argb(160,
            Color.red(settings.textColor), Color.green(settings.textColor), Color.blue(settings.textColor))

        // Font family
        val tf = when (settings.fontFamily) {
            "monospace" -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            "serif" -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            "sans-serif-light" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            "sans-serif-condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            "sans-serif-thin" -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
            "casual" -> Typeface.create("casual", Typeface.NORMAL)
            "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
            else -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val tfBold = Typeface.create(tf, Typeface.BOLD)
        textPaint.typeface = tfBold
        dimTextPaint.typeface = tf
        suggPaint.typeface = tf
        suggFocusPaint.typeface = tfBold
        hintPaint.typeface = tf
        hintBtnPaint.typeface = tfBold
    }

    private val DIALPAD_ROWS = arrayOf(
        "1 2 3",
        "4 5 6",
        "7 8 9",
        "← 0 → ABC"
    )

    private fun getLetterLayout(): KeyboardLayoutDef = KeyboardLayouts.getById(currentLayoutId)

    private fun getActiveRows(): Array<String> {
        if (dialpadMode) return DIALPAD_ROWS
        val base = if (symbolMode) KeyboardLayouts.SYMBOLS.rows else getLetterLayout().rows
        val letterRows = if (settings.numberRowEnabled && !symbolMode) {
            arrayOf("1 2 3 4 5 6 7 8 9 0") + base
        } else {
            base
        }
        // Add integrated bottom action row (PS5-style)
        val actionKeys = mutableListOf<String>()
        actionKeys.add("⇧") // shift
        if (settings.showSymbolsBtn) actionKeys.add(if (symbolMode) "ABC" else "@#:")
        if (settings.showDialpadBtn) actionKeys.add(if (dialpadMode) "ABC" else "123")
        if (settings.showSpacebar) actionKeys.add("␣")
        if (settings.showVoiceBtn) actionKeys.add("🎤")
        if (settings.showBackspaceBtn) actionKeys.add("⌫")
        if (settings.showCopyBtn) actionKeys.add("Copy")
        if (settings.showPasteBtn) actionKeys.add("Paste")
        if (settings.showEnterBtn) actionKeys.add("Done")
        return letterRows + arrayOf(actionKeys.joinToString(" "))
    }

    private val SPECIAL_KEYS = setOf("⇧", "@#:", "ABC", "123", "␣", "🎤", "⌫", "Done", "←", "→", "Copy", "Paste")

    private fun isSpecialKey(key: String): Boolean = key in SPECIAL_KEYS || key.all { it.isDigit() }

    private fun isSpecialRow(row: Int): Boolean {
        // Number row (first row when number row enabled) or action row (last row)
        val rows = getActiveRows()
        if (row < 0 || row >= rows.size) return false
        if (row == rows.size - 1) return true // action row
        if (settings.numberRowEnabled && !symbolMode && !dialpadMode && row == 0) return true // number row
        return false
    }

    private fun getChars(row: Int): List<String> {
        val rows = getActiveRows()
        val r = row.coerceIn(0, rows.size - 1)
        return rows[r].split(" ")
    }
    private fun rowCount() = getActiveRows().size
    private fun colCount(row: Int) = getChars(row).size

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val width = (screenW * settings.keyboardWidthPercent / 100)
        val height = (screenH * settings.keyboardHeightPercent / 100)
        setMeasuredDimension(width, height)
    }

    private fun spawnWindParticles() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        // Calculate actual key position
        val sidePad = w * 0.04f
        val topPad = h * 0.03f
        val suggestH = if (settings.suggestionsEnabled) h * 0.10f else 0f
        val hintH = if (settings.showHintBar) h * 0.14f else 0f
        val keyAreaH = h - topPad - suggestH - hintH
        val keyAreaW = w - sidePad * 2
        val rows = rowCount()
        val maxCols = (0 until rows).maxOf { colCount(it) }
        val keySpacing = 3f * dp
        val keyW = (keyAreaW - keySpacing * (maxCols - 1)) / maxCols
        val keyH = (keyAreaH - keySpacing * (rows - 1)) / rows
        val keyStartY = topPad + suggestH

        val row = focusRow.coerceAtLeast(0)
        val cols = if (row < rows) colCount(row) else maxCols
        val rowW = cols * keyW + (cols - 1) * keySpacing
        val offsetX = (w - rowW) / 2
        val fx = offsetX + focusCol * (keyW + keySpacing) + keyW / 2
        val fy = keyStartY + row * (keyH + keySpacing) + keyH / 2

        val count = (8..12).random()
        for (i in 0 until count) {
            val spread = (Math.random() * 40 - 20).toFloat()
            val speed = (3 + Math.random() * 5).toFloat()
            val particle = WindParticle(
                x = fx + spread,
                y = fy + (Math.random() * 30 - 15).toFloat(),
                alpha = (0.3f + Math.random() * 0.4f).toFloat(),
                dx = when (lastMoveDir) { 1 -> speed; 2 -> -speed; else -> spread * 0.1f },
                dy = when (lastMoveDir) { 3 -> speed; 4 -> -speed; else -> spread * 0.1f },
                life = 1f,
                size = (2f + Math.random() * 3f).toFloat() * dp
            )
            windParticles.add(particle)
        }
        if (!windAnimating) {
            windAnimating = true
            postDelayed(windAnimRunnable, 16)
        }
    }

    private val windAnimRunnable = object : Runnable {
        override fun run() {
            val iter = windParticles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                p.x += p.dx
                p.y += p.dy
                p.life -= 0.06f
                p.alpha *= 0.92f
                if (p.life <= 0) iter.remove()
            }
            invalidate()
            if (windParticles.isNotEmpty()) {
                postDelayed(this, 16)
            } else {
                windAnimating = false
            }
        }
    }

    private fun drawWindParticles(canvas: Canvas) {
        val accent = settings.accentColor
        for (p in windParticles) {
            windPaint.color = android.graphics.Color.argb(
                (p.alpha * 180).toInt().coerceIn(0, 255),
                android.graphics.Color.red(accent),
                android.graphics.Color.green(accent),
                android.graphics.Color.blue(accent)
            )
            canvas.drawCircle(p.x, p.y, p.size * p.life, windPaint)
        }
    }

    private fun drawSlideEffect(canvas: Canvas) {
        if (!slideActive) return
        val elapsed = System.currentTimeMillis() - slideStart
        if (elapsed > slideDuration) { slideActive = false; return }
        val t = elapsed.toFloat() / slideDuration
        // Animate border sliding from old to new position
        val cr = settings.keyRounding * dp
        val rect = getKeyRect(focusRow, focusCol) ?: return
        val bw = settings.highlightBorderSize * dp
        // Interpolate rect from old pos
        val fromRect = RectF(
            slideFromX - rect.width()/2, slideFromY - rect.height()/2,
            slideFromX + rect.width()/2, slideFromY + rect.height()/2
        )
        val curRect = RectF(
            fromRect.left + (rect.left - fromRect.left) * t,
            fromRect.top + (rect.top - fromRect.top) * t,
            fromRect.right + (rect.right - fromRect.right) * t,
            fromRect.bottom + (rect.bottom - fromRect.bottom) * t
        )
        val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = settings.accentColor or (0xFF shl 24).toInt()
            style = Paint.Style.STROKE; strokeWidth = bw
            alpha = ((1f - t * 0.3f) * 255).toInt()
        }
        canvas.drawRoundRect(curRect, cr, cr, bp)
        if (t < 1f) postInvalidateDelayed(16) else slideActive = false
    }

    private fun drawRippleEffect(canvas: Canvas) {
        val elapsed = System.currentTimeMillis() - rippleStart
        if (elapsed > rippleDuration) return
        val t = elapsed.toFloat() / rippleDuration
        val maxRadius = 30f * dp
        val radius = maxRadius * t
        val alpha = ((1f - t) * 120).toInt()
        val accent = settings.accentColor
        val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent))
            style = Paint.Style.STROKE; strokeWidth = 3f * dp * (1f - t)
        }
        canvas.drawCircle(rippleX, rippleY, radius, rp)
        // Inner fill
        val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((alpha * 0.3f).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent))
        }
        canvas.drawCircle(rippleX, rippleY, radius * 0.5f, fp)
        postInvalidateDelayed(16)
    }

    private fun drawTrailEffect(canvas: Canvas) {
        val cr = settings.keyRounding * dp
        val accent = settings.accentColor
        val iter = trailPoints.iterator()
        while (iter.hasNext()) {
            val tp = iter.next()
            tp.alpha -= 0.08f
            if (tp.alpha <= 0) { iter.remove(); continue }
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((tp.alpha * 80).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent))
            }
            canvas.drawRoundRect(RectF(tp.x, tp.y, tp.x + tp.w, tp.y + tp.h), cr, cr, p)
        }
        if (trailPoints.isNotEmpty()) postInvalidateDelayed(16)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val sidePad = w * 0.04f
        val topPad = h * 0.03f
        val suggestH = if (settings.suggestionsEnabled) h * 0.10f else 0f // always reserved when enabled
        val actionBarH = 0f // integrated into key grid now
        val hintH = if (settings.showHintBar) h * 0.14f else 0f
        val keyAreaH = h - topPad - suggestH - actionBarH - hintH
        val keyAreaW = w - sidePad * 2

        // --- Listening indicator (no language/shift status bar) ---
        if (listening) {
            listenPaint.textSize = 11f * dp
            val lt = "🎤 Listening..."
            val ltw = listenPaint.measureText(lt)
            val statusY = topPad * 0.7f + 8f * dp
            canvas.drawRoundRect(
                RectF(w/2 - ltw/2 - 12*dp, statusY - 10*dp, w/2 + ltw/2 + 12*dp, statusY + 6*dp),
                6*dp, 6*dp, listenBgPaint
            )
            canvas.drawText(lt, w/2, statusY, listenPaint)
        }

        // --- Suggestion row (always reserved space when enabled, draws when has suggestions) ---
        if (settings.suggestionsEnabled && suggestions.isNotEmpty()) {
            drawSuggestions(canvas, sidePad, topPad, keyAreaW, suggestH)
        }

        // --- Key grid ---
        val rows = rowCount()
        val maxCols = (0 until rows).maxOf { colCount(it) }
        val keySpacing = 3f * dp
        val keyW = (keyAreaW - keySpacing * (maxCols - 1)) / maxCols
        val keyH = (keyAreaH - keySpacing * (rows - 1)) / rows
        val keyStartY = topPad + suggestH
        val cr = settings.keyRounding * dp

        val fontMul = settings.fontScale / 100f
        textPaint.textSize = keyH * 0.38f * fontMul
        dimTextPaint.textSize = keyH * 0.33f * fontMul

        for (row in 0 until rows) {
            val chars = getChars(row)
            val cols = chars.size
            val rowW = cols * keyW + (cols - 1) * keySpacing
            val offsetX = (w - rowW) / 2

            for (col in 0 until cols) {
                val isFocused = row == focusRow && col == focusCol
                val x = offsetX + col * (keyW + keySpacing)
                val y = keyStartY + row * (keyH + keySpacing)
                val rect = RectF(x, y, x + keyW, y + keyH)

                // Click animation state
                val isClicked = row == clickAnimRow && col == clickAnimCol
                val clickProgress = if (isClicked) {
                    val elapsed = System.currentTimeMillis() - clickAnimStart
                    if (elapsed < clickAnimDuration) {
                        1f - (elapsed.toFloat() / clickAnimDuration)
                    } else { clickAnimRow = -2; 0f }
                } else 0f

                if (isFocused && settings.highlightStyle != "none") {
                    val bw = settings.highlightBorderSize * dp
                    val gr = RectF(rect.left - bw, rect.top - bw, rect.right + bw, rect.bottom + bw)
                    canvas.drawRoundRect(gr, cr + bw, cr + bw, glowPaint)
                }

                // Key background — secondary color for special keys
                val bgP = if (isSpecialRow(row) || isSpecialKey(chars[col])) secondaryKeyPaint else keyPaint
                canvas.drawRoundRect(rect, cr, cr, bgP)

                // Highlight
                if (isFocused) {
                    when (settings.highlightStyle) {
                        "border" -> {
                            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = focusPaint.color; style = Paint.Style.STROKE
                                strokeWidth = settings.highlightBorderSize * dp
                            }
                            canvas.drawRoundRect(rect, cr, cr, bp)
                        }
                        "fill" -> canvas.drawRoundRect(rect, cr, cr, focusPaint)
                        "glow" -> {
                            val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = focusPaint.color; alpha = 100
                            }
                            val bw = settings.highlightBorderSize * dp
                            val gr2 = RectF(rect.left - bw*2, rect.top - bw*2, rect.right + bw*2, rect.bottom + bw*2)
                            canvas.drawRoundRect(gr2, cr + bw*2, cr + bw*2, gp)
                            canvas.drawRoundRect(rect, cr, cr, focusPaint)
                        }
                        // "none" — no highlight
                    }
                }

                // Click animation overlay
                if (clickProgress > 0f) {
                    when (settings.clickAnimation) {
                        "fill" -> {
                            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = focusPaint.color; alpha = (clickProgress * 200).toInt()
                            }
                            canvas.drawRoundRect(rect, cr, cr, cp)
                        }
                        "pop" -> {
                            val scale = 1f + clickProgress * 0.15f
                            val pw = keyW * scale; val ph = keyH * scale
                            val pr = RectF(x + (keyW - pw)/2, y + (keyH - ph)/2, x + (keyW + pw)/2, y + (keyH + ph)/2)
                            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = focusPaint.color; alpha = (clickProgress * 150).toInt()
                            }
                            canvas.drawRoundRect(pr, cr, cr, cp)
                        }
                        "flash" -> {
                            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE; alpha = (clickProgress * 180).toInt()
                            }
                            canvas.drawRoundRect(rect, cr, cr, cp)
                        }
                    }
                }

                var displayChar = chars[col]
                if (shifted && !symbolMode && displayChar.length == 1 && displayChar[0].isLetter()) {
                    displayChar = displayChar.uppercase()
                }

                // Shift key: fill when held, underline when locked
                if (displayChar == "⇧" && (shifted || shiftLocked)) {
                    val shiftFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = settings.accentColor; alpha = if (shiftLocked) 200 else 120
                    }
                    canvas.drawRoundRect(rect, cr, cr, shiftFillPaint)
                }

                val paint = if (isFocused) textPaint else dimTextPaint
                val textY = y + keyH/2 - (paint.descent() + paint.ascent()) / 2
                canvas.drawText(displayChar, x + keyW/2, textY, paint)

                // Shift locked indicator: underline below the arrow
                if (displayChar == "⇧" && shiftLocked) {
                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = settings.accentColor; strokeWidth = 2f * dp
                        style = Paint.Style.STROKE
                    }
                    val lineY = textY + 3f * dp
                    val lineHalfW = keyW * 0.2f
                    canvas.drawLine(x + keyW/2 - lineHalfW, lineY, x + keyW/2 + lineHalfW, lineY, linePaint)
                }
            }
        }

        // Click animation tick — single invalidation
        if (clickAnimRow >= -1 && System.currentTimeMillis() - clickAnimStart < clickAnimDuration) {
            postInvalidateDelayed(16)
        }

        // --- Hint bar ---
        if (settings.showHintBar) drawHintBar(canvas, w, h, hintH, sidePad)

        // Navigation effects (only draw when active)
        when (settings.navEffect) {
            "wind" -> if (windParticles.isNotEmpty()) drawWindParticles(canvas)
            "slide" -> if (slideActive) drawSlideEffect(canvas)
            "ripple" -> if (System.currentTimeMillis() - rippleStart < rippleDuration) drawRippleEffect(canvas)
            "trail" -> if (trailPoints.isNotEmpty()) drawTrailEffect(canvas)
        }
    }

    private fun drawSuggestions(canvas: Canvas, sidePad: Float, topPad: Float, areaW: Float, suggestH: Float) {
        if (suggestions.isEmpty()) return

        val y = topPad
        val chipSpacing = 6f * dp
        val chipH = suggestH * 0.80f
        val chipY = y + (suggestH - chipH) / 2
        val cr = settings.keyRounding * dp * 0.8f

        suggPaint.textSize = chipH * 0.50f
        suggFocusPaint.textSize = chipH * 0.50f

        // Compact left-to-right: measure each word, fit-to-content
        var xCursor = sidePad
        for ((i, word) in suggestions.withIndex()) {
            if (i >= 5) break
            val isFocused = focusRow == -1 && focusCol == i
            val paint = if (isFocused) suggFocusPaint else suggPaint
            val textW = paint.measureText(word)
            val chipW = textW + 20f * dp
            if (xCursor + chipW > sidePad + areaW) break

            val rect = RectF(xCursor, chipY, xCursor + chipW, chipY + chipH)
            canvas.drawRoundRect(rect, cr, cr, if (isFocused) suggFocusBgPaint else suggBgPaint)

            // Focus border
            if (isFocused) {
                val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = focusPaint.color; style = Paint.Style.STROKE
                    strokeWidth = settings.highlightBorderSize * dp * 0.7f
                }
                canvas.drawRoundRect(rect, cr, cr, bp)
            }

            val textY = chipY + chipH/2 - (paint.descent() + paint.ascent()) / 2
            canvas.drawText(word, xCursor + chipW/2, textY, paint)
            xCursor += chipW + chipSpacing
        }
    }

    private fun drawHintBar(canvas: Canvas, w: Float, h: Float, hintH: Float, sidePad: Float) {
        val hints = arrayOf(
            "✕" to "Select", "△" to "Space", "□" to "Delete",
            "○" to "Close", "L2" to "Shift", "L3" to "Symbols", "R2" to "Enter"
        )

        val barY = h - hintH
        val chipH = hintH * 0.35f
        val labelGap = 4f * dp
        val centerY = barY + hintH / 2

        hintBtnPaint.textSize = chipH * 0.5f
        hintPaint.textSize = chipH * 0.45f

        val totalW = w - sidePad * 2
        val chipW = totalW / hints.size

        for ((i, hint) in hints.withIndex()) {
            val cx = sidePad + chipW * i + chipW / 2

            // Button background
            val btnText = hint.first
            val btnTW = hintBtnPaint.measureText(btnText)
            val btnPad = 8f * dp
            val btnRect = RectF(
                cx - btnTW/2 - btnPad,
                centerY - chipH/2 - 2*dp,
                cx + btnTW/2 + btnPad,
                centerY + chipH/2 - 2*dp
            )
            canvas.drawRoundRect(btnRect, 6*dp, 6*dp, hintBgPaint)
            canvas.drawText(btnText, cx, centerY - 2*dp - (hintBtnPaint.descent() + hintBtnPaint.ascent())/2 - chipH/2 + chipH/2, hintBtnPaint)

            // Label below
            canvas.drawText(hint.second, cx, centerY + chipH/2 + labelGap + hintPaint.textSize, hintPaint)
        }
    }

    // --- Public API ---

    private fun hasActionButtons(): Boolean {
        val s = settings
        return s.showSpacebar || s.showEnterBtn || s.showBackspaceBtn || s.showArrowKeys || s.showVoiceBtn || s.showSymbolsBtn || s.showDialpadBtn
    }

    private fun getActionButtons(): List<String> {
        val btns = mutableListOf<String>()
        if (settings.showArrowKeys) { btns.add("←"); btns.add("→") }
        if (settings.showBackspaceBtn) btns.add("⌫")
        if (settings.showSpacebar) btns.add("Space")
        if (settings.showEnterBtn) btns.add("Enter")
        if (settings.showSymbolsBtn) btns.add(if (symbolMode) "ABC" else "!@#")
        if (settings.showDialpadBtn) btns.add(if (dialpadMode) "ABC" else "123")
        if (settings.showVoiceBtn) btns.add("🎤")
        return btns
    }

    private val actionBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; textAlign = Paint.Align.CENTER
    }

    private fun drawActionBar(canvas: Canvas, sidePad: Float, y: Float, areaW: Float, barH: Float) {
        val btns = getActionButtons()
        if (btns.isEmpty()) return
        val spacing = 4f * dp
        val btnH = barH * 0.7f
        val btnY = y + (barH - btnH) / 2
        val btnW = (areaW - spacing * (btns.size - 1)) / btns.size
        val cr = settings.keyRounding * dp
        val actionRow = rowCount() // action bar is the row after keys

        actionBtnPaint.textSize = btnH * 0.4f

        for ((i, label) in btns.withIndex()) {
            val isFocused = focusRow == actionRow && focusCol == i
            val bx = sidePad + i * (btnW + spacing)
            val rect = RectF(bx, btnY, bx + btnW, btnY + btnH)

            canvas.drawRoundRect(rect, cr, cr, keyPaint)
            if (isFocused) {
                when (settings.highlightStyle) {
                    "border" -> {
                        val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = focusPaint.color; style = Paint.Style.STROKE
                            strokeWidth = settings.highlightBorderSize * dp
                        }
                        canvas.drawRoundRect(rect, cr, cr, bp)
                    }
                    "fill" -> canvas.drawRoundRect(rect, cr, cr, focusPaint)
                    "glow" -> {
                        val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = focusPaint.color; alpha = 100 }
                        canvas.drawRoundRect(rect, cr, cr, gp)
                        canvas.drawRoundRect(rect, cr, cr, focusPaint)
                    }
                }
            }

            actionBtnPaint.color = if (isFocused) android.graphics.Color.WHITE else android.graphics.Color.argb(180, 255, 255, 255)
            actionBtnPaint.isFakeBoldText = isFocused
            val textY = btnY + btnH / 2 - (actionBtnPaint.descent() + actionBtnPaint.ascent()) / 2
            canvas.drawText(label, bx + btnW / 2, textY, actionBtnPaint)
        }
    }

    private fun getKeyRect(row: Int, col: Int): RectF? {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f || h == 0f) return null
        val sidePad = w * 0.04f
        val topPad = h * 0.03f
        val suggestH = if (settings.suggestionsEnabled) h * 0.10f else 0f
        val hintH = if (settings.showHintBar) h * 0.14f else 0f
        val keyAreaH = h - topPad - suggestH - hintH
        val keyAreaW = w - sidePad * 2

        // Suggestion row
        if (row == -1) {
            if (suggestions.isEmpty()) return null
            val chipH = suggestH * 0.80f
            val chipY = topPad + (suggestH - chipH) / 2
            val chipSpacing = 6f * dp
            // Approximate position — compact chips
            var xCursor = sidePad
            val c = col.coerceIn(0, (suggestions.size - 1).coerceAtLeast(0))
            for (i in 0..c) {
                if (i >= suggestions.size) break
                val tw = suggPaint.measureText(suggestions[i])
                val cw = tw + 20f * dp
                if (i == c) return RectF(xCursor, chipY, xCursor + cw, chipY + chipH)
                xCursor += cw + chipSpacing
            }
            return null
        }

        val rows = rowCount()
        val maxCols = (0 until rows).maxOf { colCount(it) }
        val keySpacing = 3f * dp
        val keyW = (keyAreaW - keySpacing * (maxCols - 1)) / maxCols
        val keyH = (keyAreaH - keySpacing * (rows - 1)) / rows
        val keyStartY = topPad + suggestH
        val r = row.coerceAtLeast(0).coerceAtMost(rows - 1)
        val cols = colCount(r)
        val rowW = cols * keyW + (cols - 1) * keySpacing
        val offsetX = (w - rowW) / 2
        val c = col.coerceIn(0, cols - 1)
        val x = offsetX + c * (keyW + keySpacing)
        val y = keyStartY + r * (keyH + keySpacing)
        return RectF(x, y, x + keyW, y + keyH)
    }

    private fun triggerNavEffect() {
        val rect = getKeyRect(focusRow, focusCol) ?: return
        val cx = rect.centerX(); val cy = rect.centerY()
        when (settings.navEffect) {
            "wind" -> spawnWindParticles()
            "slide" -> {
                slideToX = cx; slideToY = cy
                if (!slideActive) { slideFromX = cx; slideFromY = cy }
                slideStart = System.currentTimeMillis()
                slideActive = true
                postInvalidateDelayed(16)
            }
            "ripple" -> {
                rippleX = cx; rippleY = cy
                rippleStart = System.currentTimeMillis()
                postInvalidateDelayed(16)
            }
            "trail" -> {
                trailPoints.add(TrailPoint(rect.left, rect.top, rect.width(), rect.height(), 1f))
                if (trailPoints.size > 5) trailPoints.removeAt(0)
                postInvalidateDelayed(16)
            }
        }
    }

    fun moveFocus(dx: Int, dy: Int) {
        // Record old position for slide
        val oldRect = getKeyRect(focusRow, focusCol)
        if (oldRect != null) { slideFromX = oldRect.centerX(); slideFromY = oldRect.centerY() }
        val minRow = if (settings.suggestionsEnabled && suggestions.isNotEmpty()) -1 else 0
        val maxRow = rowCount() - 1

        val oldRow = focusRow
        val oldColCount = when {
            oldRow == -1 -> suggestions.size.coerceAtLeast(1)
            oldRow in 0 until rowCount() -> colCount(oldRow)
            else -> 1
        }

        // Vertical movement
        if (dy != 0) {
            val newRow = focusRow + dy
            if (newRow < minRow) {
                focusRow = if (settings.verticalWrap) maxRow else minRow
            } else if (newRow > maxRow) {
                focusRow = if (settings.verticalWrap) minRow else maxRow
            } else {
                focusRow = newRow
            }

            // Proportional column mapping when changing rows
            if (focusRow != oldRow) {
                val newMaxCol = when {
                    focusRow == -1 -> (suggestions.size - 1).coerceAtLeast(0)
                    else -> colCount(focusRow) - 1
                }
                if (newMaxCol != oldColCount - 1 && oldColCount > 0) {
                    val proportion = focusCol.toFloat() / (oldColCount - 1).coerceAtLeast(1)
                    focusCol = (proportion * newMaxCol).toInt().coerceIn(0, newMaxCol)
                } else {
                    focusCol = focusCol.coerceIn(0, newMaxCol)
                }
            }
        }

        // Horizontal movement
        val maxCol = when {
            focusRow == -1 -> (suggestions.size - 1).coerceAtLeast(0)
            else -> colCount(focusRow) - 1
        }

        if (dx != 0) {
            val newCol = focusCol + dx
            if (newCol < 0) {
                focusCol = if (settings.horizontalWrap) maxCol else 0
            } else if (newCol > maxCol) {
                focusCol = if (settings.horizontalWrap) 0 else maxCol
            } else {
                focusCol = newCol
            }
        } else if (dy == 0) {
            focusCol = focusCol.coerceIn(0, maxCol)
        }


        // Navigation effect
        if (dx != 0 || dy != 0) {
            lastMoveDir = when {
                dx > 0 -> 1; dx < 0 -> 2; dy > 0 -> 3; else -> 4
            }
            triggerNavEffect()
        }

        invalidate()
    }

    fun resetFocus() {
        focusRow = 0
        focusCol = 0
        shifted = false
        shiftLocked = false
        currentWord = ""
        suggestions = emptyList()
        invalidate()
    }

    fun pressCurrentKey() {
        // Trigger click animation
        if (settings.clickAnimation != "none") {
            clickAnimRow = focusRow; clickAnimCol = focusCol
            clickAnimStart = System.currentTimeMillis()
            invalidate()
        }
        if (focusRow == -1) {
            // Suggestion row — pick word, stay on row, show next-word predictions
            if (focusCol < suggestions.size) {
                val word = suggestions[focusCol]
                onSuggestionPicked?.invoke(word)
                wordSuggestions.learnWord(word)
                // Show next-word predictions, keep focus on suggestion row
                val nextSuggs = wordSuggestions.getNextWordSuggestions(word, 5)
                currentWord = ""
                suggestions = nextSuggs
                // Keep focusCol at same position, but clamp
                focusCol = focusCol.coerceAtMost((suggestions.size - 1).coerceAtLeast(0))
                if (suggestions.isEmpty()) focusRow = 0
                invalidate()
            }
            return
        }

        val chars = getChars(focusRow)
        if (focusCol < chars.size) {
            var c = chars[focusCol]
            // Handle special keys
            if (c == "←") { onCursorLeft?.invoke(); return }
            if (c == "→") { onCursorRight?.invoke(); return }
            if (c == "⇧") {
                when {
                    shiftLocked -> { shiftLocked = false; shifted = false }  // locked → off
                    shifted -> { shiftLocked = true }                        // shift → locked
                    else -> { shifted = true }                               // off → shift
                }
                invalidate(); return
            }
            if (c == "⌫") { onBackspace?.invoke(); return }
            if (c == "␣") { onSpace?.invoke(); return }
            if (c == "Done") { onEnter?.invoke(); return }
            if (c == "🎤") { onSpeech?.invoke(); return }
            if (c == "Copy") { onCopy?.invoke(); return }
            if (c == "Paste") { onPaste?.invoke(); return }
            if (c == "@#:" || (c == "ABC" && symbolMode)) { toggleLayout(); return }
            if (c == "123" || (c == "ABC" && dialpadMode)) { toggleDialpad(); return }
            if (shifted && !symbolMode && !dialpadMode && c.length == 1 && c[0].isLetter()) {
                c = c.uppercase()
            }
            if (c.length == 1) {
                onCharInput?.invoke(c[0])
                // Auto-release shift after typing (unless locked)
                if (shifted && !shiftLocked) { shifted = false }
                // Track current word for suggestions
                if (c[0].isLetterOrDigit()) {
                    currentWord += c[0]
                    updateSuggestions()
                } else {
                    if (currentWord.isNotEmpty()) {
                        wordSuggestions.learnWord(currentWord)
                    }
                    currentWord = ""
                    suggestions = emptyList()
                }
                invalidate()
            }
        }
    }

    fun onSpacePressed() {
        if (currentWord.isNotEmpty()) {
            wordSuggestions.learnWord(currentWord)
            currentWord = ""
            suggestions = emptyList()
            invalidate()
        }
    }

    fun onBackspacePressed() {
        if (currentWord.isNotEmpty()) {
            currentWord = currentWord.dropLast(1)
            updateSuggestions()
            invalidate()
        }
    }

    fun setShift(on: Boolean) {
        shifted = on
        if (!on) shiftLocked = false
        invalidate()
    }

    fun setShiftLocked(locked: Boolean) {
        shiftLocked = locked
        shifted = locked
        invalidate()
    }

    fun toggleDialpad() {
        dialpadMode = !dialpadMode
        focusRow = 0
        focusCol = 0
        invalidate()
    }

    fun toggleLayout() {
        symbolMode = !symbolMode
        dialpadMode = false
        val maxRow = rowCount() - 1
        focusRow = focusRow.coerceIn(0, maxRow)
        focusCol = focusCol.coerceIn(0, colCount(focusRow) - 1)
        invalidate()
    }

    fun setListening(on: Boolean) {
        listening = on
        invalidate()
    }

    fun setKeyboardLayout(layoutId: String) {
        currentLayoutId = layoutId
        settings.keyboardLayout = layoutId
        invalidate()
    }

    fun getKeyboardLayoutId(): String = currentLayoutId

    private fun updateSuggestions() {
        suggestions = wordSuggestions.getSuggestions(currentWord, 5)
    }
}
