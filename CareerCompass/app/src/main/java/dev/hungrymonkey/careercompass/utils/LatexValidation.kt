package dev.hungrymonkey.careercompass.utils

private val LATEX_SPECIAL_CHARS = setOf('#', '$', '%', '^', '&', '_', '{', '}', '\\', '~')

fun containsLatexSpecialChars(text: String): Boolean {
    return text.any { it in LATEX_SPECIAL_CHARS }
}

fun getLatexSpecialCharError(text: String): String? {
    val foundChars = text.filter { it in LATEX_SPECIAL_CHARS }.toSet()
    return if (foundChars.isNotEmpty()) {
        "Text contains special characters that cannot be processed: ${foundChars.joinToString(", ")}"
    } else null
}
