package com.zebratic.sensekeyboard

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent

enum class GamepadAction {
    NAVIGATE_UP, NAVIGATE_DOWN, NAVIGATE_LEFT, NAVIGATE_RIGHT,
    SELECT, CANCEL, BACKSPACE, SPACE,
    CURSOR_LEFT, CURSOR_RIGHT,
    SELECT_LEFT, SELECT_RIGHT,
    COPY, PASTE,
    SHIFT_ON, SHIFT_OFF,
    SWITCH_LAYOUT, ENTER, NEWLINE, SPEECH_INPUT, NONE
}

object GamepadInput {

    private const val TRIGGER_THRESHOLD = 0.5f
    private const val STICK_THRESHOLD = 0.5f

    // D-pad repeat: first press instant, then wait INITIAL_DELAY, then repeat at REPEAT_RATE
    private const val DPAD_INITIAL_DELAY_MS = 400L  // delay before repeating starts
    private const val DPAD_REPEAT_RATE_MS = 80L     // rate when holding

    private var l2Pressed = false
    private var r2Pressed = false
    private var stickNavigated = false
    private var lastL2PressTime = 0L
    private var shiftLocked = false
    fun isShiftLocked() = shiftLocked
    private const val DOUBLE_TAP_MS = 300L

    private var lastDpadTime = 0L
    private var dpadHeldSince = 0L
    private var lastDpadKeyCode = 0
    private var lastBumperTime = 0L
    private var bumperHeldSince = 0L
    private var lastActionTime = 0L
    private var actionHeldSince = 0L

    fun processKeyEvent(event: KeyEvent): GamepadAction {
        if (event.action != KeyEvent.ACTION_DOWN) return GamepadAction.NONE

        val isDpad = event.keyCode in intArrayOf(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT
        )
        val isBumper = event.keyCode in intArrayOf(
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1
        )
        val isRepeatable = event.keyCode in intArrayOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER, // Cross — type char
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_DEL           // Square — backspace
        )

        if (event.repeatCount > 0 && !isDpad && !isBumper && !isRepeatable) return GamepadAction.NONE

        if (isDpad) {
            val now = System.currentTimeMillis()
            if (event.repeatCount == 0) {
                // First press — always fire, record timing
                dpadHeldSince = now
                lastDpadTime = now
                lastDpadKeyCode = event.keyCode
            } else {
                // Held: check if past initial delay, then throttle at repeat rate
                val holdDuration = now - dpadHeldSince
                if (holdDuration < DPAD_INITIAL_DELAY_MS) return GamepadAction.NONE
                if (now - lastDpadTime < DPAD_REPEAT_RATE_MS) return GamepadAction.NONE
                lastDpadTime = now
            }
        }

        // Cross/Square repeat (hold to repeat char input / backspace)
        if (isRepeatable) {
            val now = System.currentTimeMillis()
            if (event.repeatCount == 0) {
                actionHeldSince = now; lastActionTime = now
            } else {
                val holdDuration = now - actionHeldSince
                if (holdDuration < DPAD_INITIAL_DELAY_MS) return GamepadAction.NONE
                if (now - lastActionTime < DPAD_REPEAT_RATE_MS) return GamepadAction.NONE
                lastActionTime = now
            }
        }

        // Bumper repeat (L1/R1)
        if (isBumper) {
            val now = System.currentTimeMillis()
            if (event.repeatCount == 0) {
                bumperHeldSince = now; lastBumperTime = now
            } else {
                val holdDuration = now - bumperHeldSince
                if (holdDuration < DPAD_INITIAL_DELAY_MS) return GamepadAction.NONE
                if (now - lastBumperTime < DPAD_REPEAT_RATE_MS) return GamepadAction.NONE
                lastBumperTime = now
            }
        }

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> GamepadAction.NAVIGATE_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> GamepadAction.NAVIGATE_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> GamepadAction.NAVIGATE_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadAction.NAVIGATE_RIGHT

            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER -> GamepadAction.SELECT
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> GamepadAction.CANCEL
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_DEL -> GamepadAction.BACKSPACE
            KeyEvent.KEYCODE_BUTTON_Y -> GamepadAction.SPACE
            KeyEvent.KEYCODE_BUTTON_L1 -> if (l2Pressed) GamepadAction.SELECT_LEFT else GamepadAction.CURSOR_LEFT
            KeyEvent.KEYCODE_BUTTON_R1 -> if (l2Pressed) GamepadAction.SELECT_RIGHT else GamepadAction.CURSOR_RIGHT
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> GamepadAction.ENTER
            KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadAction.SWITCH_LAYOUT
            // Share button — no longer mapped (voice is on-screen button now)
            // R3 stick — no longer mapped
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_MEDIA_RECORD,
            KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadAction.NONE
            else -> GamepadAction.NONE
        }
    }

    fun processMotionEvent(event: MotionEvent): List<GamepadAction> {
        if (event.action != MotionEvent.ACTION_MOVE) return emptyList()
        val actions = mutableListOf<GamepadAction>()

        // L2 trigger (shift) — double-tap to lock
        val l2Value = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val l2Now = l2Value > TRIGGER_THRESHOLD
        if (l2Now && !l2Pressed) {
            val now = System.currentTimeMillis()
            if (shiftLocked) {
                // Any press while locked → unlock
                shiftLocked = false
                actions.add(GamepadAction.SHIFT_OFF)
            } else if (now - lastL2PressTime < DOUBLE_TAP_MS) {
                // Double tap → lock shift on
                shiftLocked = true
                actions.add(GamepadAction.SHIFT_ON)
            } else {
                // Single tap → temporary shift
                actions.add(GamepadAction.SHIFT_ON)
            }
            lastL2PressTime = now
        } else if (!l2Now && l2Pressed && !shiftLocked) {
            actions.add(GamepadAction.SHIFT_OFF)
        }
        l2Pressed = l2Now

        // R2 trigger — Enter normally, Newline if L2 held
        val r2Value = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        val r2Now = r2Value > TRIGGER_THRESHOLD
        if (r2Now && !r2Pressed) {
            actions.add(if (l2Pressed) GamepadAction.NEWLINE else GamepadAction.ENTER)
        }
        r2Pressed = r2Now

        // Stick navigation — DISABLED for now to prevent double-move with d-pad HAT
        // D-pad on DualSense sends both KEYCODE_DPAD_* AND HAT axis events
        // We handle navigation purely through key events above

        return actions
    }

    fun isGamepad(event: InputEvent): Boolean {
        val source = event.source
        return (source and android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD ||
                (source and android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK ||
                (source and android.view.InputDevice.SOURCE_DPAD) == android.view.InputDevice.SOURCE_DPAD
    }

    fun reset() {
        l2Pressed = false
        r2Pressed = false
        stickNavigated = false
        lastDpadTime = 0L
        dpadHeldSince = 0L
    }
}
