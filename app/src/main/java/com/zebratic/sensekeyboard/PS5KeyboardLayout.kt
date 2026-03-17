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

    // State
    private var focusRow = -1 // -1 = suggestion row, 0+ = key rows
    private var focusCol = 0
    private var shifted = false
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
        focusPaint.color = settings.accentColor or (0xFF shl 24).toInt()
        glowPaint.color = Color.argb(64,
            Color.red(settings.accentColor), Color.green(settings.accentColor), Color.blue(settings.accentColor))
        textPaint.color = settings.textColor or (0xFF shl 24).toInt()
        dimTextPaint.color = Color.argb(160,
            Color.red(settings.textColor), Color.green(settings.textColor), Color.blue(settings.textColor))
    }

    private val DIALPAD_ROWS = arrayOf(
        "1 2 3",
        "4 5 6",
        "7 8 9",
        "← 0 →"
    )

    private fun getLetterLayout(): KeyboardLayoutDef = KeyboardLayouts.getById(currentLayoutId)

    private fun getActiveRows(): Array<String> {
        if (dialpadMode) return DIALPAD_ROWS
        val base = if (symbolMode) KeyboardLayouts.SYMBOLS.rows else getLetterLayout().rows
        return if (settings.numberRowEnabled && !symbolMode) {
            arrayOf("1 2 3 4 5 6 7 8 9 0") + base
        } else {
            base
        }
    }

    private fun getChars(row: Int): List<String> = getActiveRows()[row].split(" ")
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

        // Spawn 8-12 particles at the focused key position
        val rows = if (focusRow >= 0) rowCount() else 1
        val keyH = h * 0.6f / rows
        val fy = if (focusRow >= 0) h * 0.15f + focusRow * keyH + keyH/2 else h * 0.08f
        val fx = w * focusCol / 10f + w * 0.05f

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val sidePad = w * 0.04f
        val topPad = h * 0.06f
        val suggestH = h * 0.12f      // suggestion row
        val hintH = h * 0.20f         // hint bar (bigger!)
        val keyAreaH = h - topPad - suggestH - hintH
        val keyAreaW = w - sidePad * 2

        // --- Status bar ---
        val statusY = topPad * 0.7f
        layoutLabelPaint.textSize = 11f * dp
        val layoutLabel = if (symbolMode) "!@#" else getLetterLayout().name.uppercase()
        canvas.drawText(layoutLabel, sidePad, statusY, layoutLabelPaint)

        if (listening) {
            listenPaint.textSize = 11f * dp
            val lt = "🎤 Listening..."
            val ltw = listenPaint.measureText(lt)
            canvas.drawRoundRect(
                RectF(w/2 - ltw/2 - 12*dp, statusY - 10*dp, w/2 + ltw/2 + 12*dp, statusY + 6*dp),
                6*dp, 6*dp, listenBgPaint
            )
            canvas.drawText(lt, w/2, statusY, listenPaint)
        }

        if (shifted) {
            shiftPaint.textSize = 11f * dp
            canvas.drawText("⬆ SHIFT", w - sidePad, statusY, shiftPaint)
        }

        // --- Suggestion row ---
        drawSuggestions(canvas, sidePad, topPad, keyAreaW, suggestH)

        // --- Key grid ---
        val rows = rowCount()
        val maxCols = (0 until rows).maxOf { colCount(it) }
        val keySpacing = 3f * dp
        val keyW = (keyAreaW - keySpacing * (maxCols - 1)) / maxCols
        val keyH = (keyAreaH - keySpacing * (rows - 1)) / rows
        val keyStartY = topPad + suggestH
        val cr = 6f * dp

        textPaint.textSize = keyH * 0.38f
        dimTextPaint.textSize = keyH * 0.33f

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

                if (isFocused) {
                    val gr = RectF(rect.left - 3*dp, rect.top - 3*dp, rect.right + 3*dp, rect.bottom + 3*dp)
                    canvas.drawRoundRect(gr, cr + 2*dp, cr + 2*dp, glowPaint)
                }

                // Key background
                canvas.drawRoundRect(rect, cr, cr, keyPaint)
                if (isFocused) {
                    if (settings.borderHighlight) {
                        // Border-only highlight
                        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = focusPaint.color; style = Paint.Style.STROKE; strokeWidth = 2.5f * dp
                        }
                        canvas.drawRoundRect(rect, cr, cr, borderPaint)
                    } else {
                        canvas.drawRoundRect(rect, cr, cr, focusPaint)
                    }
                }

                var displayChar = chars[col]
                if (shifted && !symbolMode && displayChar.length == 1 && displayChar[0].isLetter()) {
                    displayChar = displayChar.uppercase()
                }

                val paint = if (isFocused) textPaint else dimTextPaint
                val textY = y + keyH/2 - (paint.descent() + paint.ascent()) / 2
                canvas.drawText(displayChar, x + keyW/2, textY, paint)
            }
        }

        // --- Hint bar ---
        drawHintBar(canvas, w, h, hintH, sidePad)

        // Wind particles on top of everything
        drawWindParticles(canvas)
    }

    private fun drawSuggestions(canvas: Canvas, sidePad: Float, topPad: Float, areaW: Float, suggestH: Float) {
        if (suggestions.isEmpty()) return

        val y = topPad
        val chipSpacing = 8f * dp
        val chipH = suggestH * 0.75f
        val chipY = y + (suggestH - chipH) / 2
        val chipW = (areaW - chipSpacing * (suggestions.size - 1)) / suggestions.size.coerceAtMost(5)
        val cr = 8f * dp

        suggPaint.textSize = chipH * 0.45f
        suggFocusPaint.textSize = chipH * 0.45f

        for ((i, word) in suggestions.withIndex()) {
            if (i >= 5) break
            val isFocused = focusRow == -1 && focusCol == i
            val x = sidePad + i * (chipW + chipSpacing)
            val rect = RectF(x, chipY, x + chipW, chipY + chipH)

            canvas.drawRoundRect(rect, cr, cr, if (isFocused) suggFocusBgPaint else suggBgPaint)

            val paint = if (isFocused) suggFocusPaint else suggPaint
            val textY = chipY + chipH/2 - (paint.descent() + paint.ascent()) / 2
            canvas.drawText(word, x + chipW/2, textY, paint)
        }
    }

    private fun drawHintBar(canvas: Canvas, w: Float, h: Float, hintH: Float, sidePad: Float) {
        val hints = arrayOf(
            "✕" to "Select", "△" to "Space", "□" to "Delete",
            "○" to "Close", "L2" to "Shift", "L3" to "Symbols", "R2" to "Enter", "Share" to "Voice"
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

    fun moveFocus(dx: Int, dy: Int) {
        val minRow = if (suggestions.isNotEmpty()) -1 else 0
        val maxRow = rowCount() - 1

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
        }

        // Horizontal movement
        val maxCol = if (focusRow == -1) {
            (suggestions.size - 1).coerceAtLeast(0)
        } else {
            colCount(focusRow) - 1
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
        } else {
            focusCol = focusCol.coerceIn(0, maxCol)
        }

        // Spawn wind particles
        if (dx != 0 || dy != 0) {
            lastMoveDir = when {
                dx > 0 -> 1; dx < 0 -> 2; dy > 0 -> 3; else -> 4
            }
            spawnWindParticles()
        }

        invalidate()
    }

    fun resetFocus() {
        focusRow = 0
        focusCol = 0
        shifted = false
        currentWord = ""
        suggestions = emptyList()
        invalidate()
    }

    fun pressCurrentKey() {
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
            // Handle special dialpad keys
            if (c == "←") { onCursorLeft?.invoke(); return }
            if (c == "→") { onCursorRight?.invoke(); return }
            if (shifted && !symbolMode && !dialpadMode && c.length == 1 && c[0].isLetter()) {
                c = c.uppercase()
            }
            if (c.length == 1) {
                onCharInput?.invoke(c[0])
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
        focusCol = focusCol.coerceAtMost(
            if (focusRow >= 0) colCount(focusRow) - 1 else focusCol
        )
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
