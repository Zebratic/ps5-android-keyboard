package com.zebratic.sensekeyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class PS5KeyboardService : InputMethodService() {

    private var keyboardView: PS5KeyboardLayout? = null
    private var isKeyboardVisible = false
    private var speechInput: SpeechInput? = null

    // Never go fullscreen — always show input field above keyboard
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
        speechInput = SpeechInput(
            context = this,
            onResult = { text ->
                currentInputConnection?.commitText(text, 1)
                keyboardView?.setListening(false)
            },
            onListeningStateChanged = { listening ->
                keyboardView?.setListening(listening)
            }
        )
    }

    override fun onCreateInputView(): View {
        DebugLogger.log("IME", "onCreateInputView called")
        keyboardView = PS5KeyboardLayout(this).apply {
            onCharInput = { char ->
                currentInputConnection?.commitText(char.toString(), 1)
            }
            onSpace = {
                currentInputConnection?.commitText(" ", 1)
            }
            onBackspace = {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            onEnter = {
                currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
            }
            onClose = {
                requestHideSelf(0)
            }
            onSpeech = {
                if (speechInput?.isAvailable() == true) {
                    speechInput?.startListening()
                }
            }
            onCursorLeft = {
                val ic = currentInputConnection
                val ec = ic?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                if (ec != null) { val p = ec.selectionStart - 1; if (p >= 0) ic?.setSelection(p, p) }
            }
            onCursorRight = {
                val ic = currentInputConnection
                val ec = ic?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                if (ec != null) { val p = ec.selectionStart + 1; if (p <= (ec.text?.length ?: 0)) ic?.setSelection(p, p) }
            }
            onSuggestionPicked = { word ->
                val ic = currentInputConnection
                if (ic != null) {
                    val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                    if (extracted != null) {
                        val before = extracted.text?.substring(0, extracted.selectionStart) ?: ""
                        val wordStart = before.indexOfLast { !it.isLetterOrDigit() } + 1
                        val charsToDelete = extracted.selectionStart - wordStart
                        if (charsToDelete > 0) {
                            ic.deleteSurroundingText(charsToDelete, 0)
                        }
                    }
                    ic.commitText("$word ", 1)
                }
            }
        }
        return keyboardView!!
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        isKeyboardVisible = true
        keyboardView?.reloadSettings()
        DebugLogger.log("IME", "onStartInputView visible=true restarting=$restarting")
        keyboardView?.resetFocus()
        GamepadInput.reset()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        isKeyboardVisible = false
        speechInput?.stopListening()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        speechInput?.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || !isKeyboardVisible) return super.onKeyDown(keyCode, event)
        if (!GamepadInput.isGamepad(event)) return super.onKeyDown(keyCode, event)

        val action = GamepadInput.processKeyEvent(event)
        if (action == GamepadAction.NONE) return super.onKeyDown(keyCode, event)
        return handleAction(action)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || !isKeyboardVisible) return super.onKeyUp(keyCode, event)
        if (!GamepadInput.isGamepad(event)) return super.onKeyUp(keyCode, event)

        // Handle shift release from button
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            // L2 release handled via motion events
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null || !isKeyboardVisible) return super.onGenericMotionEvent(event)
        if (!GamepadInput.isGamepad(event)) return super.onGenericMotionEvent(event)

        val actions = GamepadInput.processMotionEvent(event)
        var handled = false
        for (action in actions) {
            if (handleAction(action)) handled = true
        }
        return if (handled) true else super.onGenericMotionEvent(event)
    }

    private fun handleAction(action: GamepadAction): Boolean {
        val view = keyboardView ?: return false
        when (action) {
            GamepadAction.NAVIGATE_UP -> view.moveFocus(0, -1)
            GamepadAction.NAVIGATE_DOWN -> view.moveFocus(0, 1)
            GamepadAction.NAVIGATE_LEFT -> view.moveFocus(-1, 0)
            GamepadAction.NAVIGATE_RIGHT -> view.moveFocus(1, 0)
            GamepadAction.SELECT -> view.pressCurrentKey()
            GamepadAction.SPACE -> {
                currentInputConnection?.commitText(" ", 1)
                view.onSpacePressed()
            }
            GamepadAction.BACKSPACE -> {
                currentInputConnection?.deleteSurroundingText(1, 0)
                view.onBackspacePressed()
            }
            GamepadAction.CANCEL -> requestHideSelf(0)
            GamepadAction.ENTER -> currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
            GamepadAction.NEWLINE -> currentInputConnection?.commitText("\n", 1)
            GamepadAction.CURSOR_LEFT -> {
                val ec = currentInputConnection?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                if (ec != null) { val p = ec.selectionStart - 1; if (p >= 0) currentInputConnection?.setSelection(p, p) }
            }
            GamepadAction.CURSOR_RIGHT -> {
                val ec = currentInputConnection?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                if (ec != null) { val p = ec.selectionStart + 1; if (p <= (ec.text?.length ?: 0)) currentInputConnection?.setSelection(p, p) }
            }
            GamepadAction.SHIFT_ON -> view.setShift(true)
            GamepadAction.SHIFT_OFF -> view.setShift(false)
            GamepadAction.SWITCH_LAYOUT -> view.toggleLayout()
            GamepadAction.SPEECH_INPUT -> speechInput?.startListening()
            GamepadAction.NONE -> return false
        }
        return true
    }
}
