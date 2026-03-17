package com.zebratic.sensekeyboard

data class KeyboardLayoutDef(
    val id: String,
    val name: String,
    val rows: Array<String>, // space-separated chars per row
    val shiftRows: Array<String>? = null // if null, auto-uppercase letters
)

object KeyboardLayouts {
    val US_QWERTY = KeyboardLayoutDef(
        id = "us",
        name = "English (US)",
        rows = arrayOf(
            "q w e r t y u i o p",
            "a s d f g h j k l '",
            "z x c v b n m , . ?"
        )
    )

    val DA_QWERTY = KeyboardLayoutDef(
        id = "da",
        name = "Dansk",
        rows = arrayOf(
            "q w e r t y u i o p å",
            "a s d f g h j k l æ ø",
            "z x c v b n m , . ?"
        )
    )

    val SYMBOLS = KeyboardLayoutDef(
        id = "sym",
        name = "Symbols",
        rows = arrayOf(
            "1 2 3 4 5 6 7 8 9 0",
            "@ # $ % & - + ( ) \"",
            "! : ; / \\ ~ * { } |"
        )
    )

    val ALL_LETTER_LAYOUTS = listOf(US_QWERTY, DA_QWERTY)

    fun getById(id: String): KeyboardLayoutDef {
        return ALL_LETTER_LAYOUTS.find { it.id == id } ?: US_QWERTY
    }
}
