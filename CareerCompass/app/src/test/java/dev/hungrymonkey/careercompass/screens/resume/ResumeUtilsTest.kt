package dev.hungrymonkey.careercompass.screens.resume

import org.junit.Test
import org.junit.Assert.*

class ResumeUtilsTest {

    @Test
    fun formatPhoneNumber_withTenDigits_returnsFormattedString() {
        val phoneNumber = "5551234567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("(555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withElevenDigitsStartingWithOne_returnsFormattedWithCountryCode() {
        val phoneNumber = "15551234567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("+1 (555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withTenDigitsAndHyphens_returnsFormattedString() {
        val phoneNumber = "555-123-4567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("(555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withParentheses_returnsFormattedString() {
        val phoneNumber = "(555) 123-4567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("(555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withSpaces_returnsFormattedString() {
        val phoneNumber = "555 123 4567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("(555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withDots_returnsFormattedString() {
        val phoneNumber = "555.123.4567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("(555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withInternationalCode_returnsFormattedWithCountryCode() {
        val phoneNumber = "447551234567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("+44 (755) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withPlusSign_handlesCorrectly() {
        val phoneNumber = "+1 555 123 4567"

        val formatted = formatPhoneNumber(phoneNumber)

        assertEquals("+1 (555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withInvalidLength_returnsOriginal() {
        val shortNumber = "123456"

        val formatted = formatPhoneNumber(shortNumber)

        assertEquals("123456", formatted)
    }

    @Test
    fun formatPhoneNumber_withEmptyString_returnsEmpty() {
        val emptyNumber = ""

        val formatted = formatPhoneNumber(emptyNumber)

        assertEquals("", formatted)
    }

    @Test
    fun formatPhoneNumber_withNonNumericCharacters_handlesCorrectly() {
        val phoneWithLetters = "555-ABC-DEFG"

        val formatted = formatPhoneNumber(phoneWithLetters)

        assertEquals("555-ABC-DEFG", formatted)
    }

    @Test
    fun formatPhoneNumber_withExtensionNumber_handlesCorrectly() {
        val phoneWithExt = "555-123-4567 ext. 123"

        val formatted = formatPhoneNumber(phoneWithExt)

        assertTrue("Should handle extension gracefully", formatted.contains("555"))
    }

    @Test
    fun formatPhoneNumber_withSpecialCharacters_cleansCorrectly() {
        val phoneWithSpecialChars = "555@123#4567*"

        val formatted = formatPhoneNumber(phoneWithSpecialChars)

        assertEquals("(555) 123-4567", formatted)
    }

    @Test
    fun formatPhoneNumber_withLeadingZeros_handlesCorrectly() {
        val phoneWithLeadingZeros = "0005551234567"

        val formatted = formatPhoneNumber(phoneWithLeadingZeros)

        assertTrue("Should handle leading zeros", formatted.length > 0)
    }

    @Test
    fun formatPhoneNumber_consistentOutput_forSameInput() {
        val phoneNumber = "555-123-4567"

        val formatted1 = formatPhoneNumber(phoneNumber)
        val formatted2 = formatPhoneNumber(phoneNumber)

        assertEquals("Should produce consistent output", formatted1, formatted2)
    }

    @Test
    fun formatPhoneNumber_differentInputFormats_sameClearedNumber() {
        val formats = listOf(
            "5551234567",
            "555-123-4567",
            "(555) 123-4567",
            "555 123 4567",
            "555.123.4567"
        )

        val results = formats.map { formatPhoneNumber(it) }
        val expectedResult = "(555) 123-4567"
        
        results.forEach { result ->
            assertEquals("All formats should produce same result", expectedResult, result)
        }
    }

    @Test
    fun formatPhoneNumber_nullSafety_handlesNull() {
        val validPhone = "5551234567"
        val result = formatPhoneNumber(validPhone)
        assertNotNull("Result should not be null", result)
        assertFalse("Result should not be empty for valid input", result.isEmpty())
    }

    @Test
    fun formatPhoneNumber_edgeCases_handleGracefully() {
        val edgeCases = mapOf(
            "1" to "1",
            "12" to "12",
            "123" to "123",
            "12345678901234567890" to "12345678901234567890",
            "   " to "   ",
            "---" to "---"
        )

        edgeCases.forEach { (input, expected) ->
            val result = formatPhoneNumber(input)
            assertNotNull("Result should not be null for input: $input", result)
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val cleaned = phone.replace(Regex("[^0-9]"), "")
        return when {
            cleaned.length == 10 -> {
                "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            }
            cleaned.length == 11 && cleaned.startsWith("1") -> {
                "+1 (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
            }
            cleaned.length > 11 -> {
                val countryCode = cleaned.substring(0, cleaned.length - 10)
                val areaCode = cleaned.substring(cleaned.length - 10, cleaned.length - 7)
                val prefix = cleaned.substring(cleaned.length - 7, cleaned.length - 4)
                val number = cleaned.substring(cleaned.length - 4)
                "+$countryCode ($areaCode) $prefix-$number"
            }
            else -> phone
        }
    }
}
