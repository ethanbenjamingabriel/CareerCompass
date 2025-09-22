package dev.hungrymonkey.careercompass.models

import org.junit.Test
import org.junit.Assert.*
import java.util.*

class GeneralDetailsTest {

    @Test
    fun createGeneralDetails_withAllFields_returnsValidObject() {
        val testDate = Date()
        
        val generalDetails = GeneralDetails(
            id = "test-id",
            fullName = "John Doe",
            email = "john.doe@example.com",
            phone = "555-123-4567",
            website1 = "https://linkedin.com/in/johndoe",
            website2 = "https://github.com/johndoe",
            lastModifiedAt = testDate
        )

        assertEquals("test-id", generalDetails.id)
        assertEquals("John Doe", generalDetails.fullName)
        assertEquals("john.doe@example.com", generalDetails.email)
        assertEquals("555-123-4567", generalDetails.phone)
        assertEquals("https://linkedin.com/in/johndoe", generalDetails.website1)
        assertEquals("https://github.com/johndoe", generalDetails.website2)
        assertEquals(testDate, generalDetails.lastModifiedAt)
    }

    @Test
    fun createGeneralDetails_withMinimalFields_hasCorrectDefaults() {
        val generalDetails = GeneralDetails()

        assertEquals("", generalDetails.id)
        assertEquals("", generalDetails.fullName)
        assertEquals("", generalDetails.email)
        assertEquals("", generalDetails.phone)
        assertEquals("", generalDetails.website1)
        assertEquals("", generalDetails.website2)
        assertNotNull(generalDetails.lastModifiedAt)
    }

    @Test
    fun createGeneralDetails_withPartialFields_storesCorrectly() {
        val generalDetails = GeneralDetails(
            id = "partial-id",
            fullName = "Jane Smith",
            email = "jane.smith@example.com",
            phone = "555-987-6543"
        )

        assertEquals("partial-id", generalDetails.id)
        assertEquals("Jane Smith", generalDetails.fullName)
        assertEquals("jane.smith@example.com", generalDetails.email)
        assertEquals("555-987-6543", generalDetails.phone)
        assertEquals("", generalDetails.website1)
        assertEquals("", generalDetails.website2)
    }

    @Test
    fun generalDetails_toFirestoreModel_convertsCorrectly() {
        val testDate = Date(1640995200000L)
        val generalDetails = GeneralDetails(
            id = "firestore-test",
            fullName = "Test User",
            email = "test@example.com",
            phone = "555-000-0000",
            website1 = "https://testsite.com",
            website2 = "https://github.com/testuser",
            lastModifiedAt = testDate
        )

        val firestoreModel = generalDetails.toFirestoreModel()

        assertEquals("Test User", firestoreModel.fullName)
        assertEquals("test@example.com", firestoreModel.email)
        assertEquals("555-000-0000", firestoreModel.phone)
        assertEquals("https://testsite.com", firestoreModel.website1)
        assertEquals("https://github.com/testuser", firestoreModel.website2)
        assertEquals(testDate, firestoreModel.lastModifiedAt.toDate())
    }

    @Test
    fun generalDetails_copyWithNewValues_updatesCorrectly() {
        val originalDetails = GeneralDetails(
            id = "copy-test",
            fullName = "Original Name",
            email = "original@example.com",
            phone = "555-111-1111",
            website1 = "https://original.com",
            website2 = "https://github.com/original"
        )

        val updatedDetails = originalDetails.copy(
            fullName = "Updated Name",
            email = "updated@example.com"
        )

        assertEquals("copy-test", updatedDetails.id)
        assertEquals("Updated Name", updatedDetails.fullName)
        assertEquals("updated@example.com", updatedDetails.email)
        assertEquals("555-111-1111", updatedDetails.phone)
        assertEquals("https://original.com", updatedDetails.website1)
        assertEquals("https://github.com/original", updatedDetails.website2)
    }

    @Test
    fun generalDetails_withLongWebsiteUrls_storesCorrectly() {
        val longUrl1 = "https://very-long-professional-website-url-for-portfolio.example.com/portfolio/projects/resume-builder"
        val longUrl2 = "https://github.com/very-long-username-for-testing-purposes/career-compass-android-application"
        
        val generalDetails = GeneralDetails(
            id = "long-url-test",
            fullName = "URL Tester",
            email = "url.tester@example.com",
            phone = "555-222-3333",
            website1 = longUrl1,
            website2 = longUrl2
        )

        assertEquals(longUrl1, generalDetails.website1)
        assertEquals(longUrl2, generalDetails.website2)
        assertTrue(generalDetails.website1.length > 50)
        assertTrue(generalDetails.website2.length > 50)
    }

    @Test
    fun generalDetails_withSpecialCharacters_storesCorrectly() {
        val generalDetails = GeneralDetails(
            id = "special-char-test",
            fullName = "José María O'Connor-Smith Jr.",
            email = "jose.maria@example-company.co.uk",
            phone = "+1 (555) 123-4567 ext. 890",
            website1 = "https://josé-maría.example.com/portfolio",
            website2 = "https://github.com/jose-maria-o-connor"
        )

        assertEquals("José María O'Connor-Smith Jr.", generalDetails.fullName)
        assertEquals("jose.maria@example-company.co.uk", generalDetails.email)
        assertEquals("+1 (555) 123-4567 ext. 890", generalDetails.phone)
        assertEquals("https://josé-maría.example.com/portfolio", generalDetails.website1)
        assertEquals("https://github.com/jose-maria-o-connor", generalDetails.website2)
    }

    @Test
    fun generalDetails_withEmptyWebsites_handledCorrectly() {
        val generalDetails = GeneralDetails(
            id = "empty-websites-test",
            fullName = "No Websites User",
            email = "nowebsites@example.com",
            phone = "555-444-5555",
            website1 = "",
            website2 = ""
        )

        assertEquals("", generalDetails.website1)
        assertEquals("", generalDetails.website2)
        assertTrue(generalDetails.website1.isEmpty())
        assertTrue(generalDetails.website2.isEmpty())
    }

    @Test
    fun generalDetails_dataClassProperties_workCorrectly() {
        val details1 = GeneralDetails(
            id = "equality-test",
            fullName = "Test User",
            email = "test@example.com",
            phone = "555-123-4567",
            website1 = "https://test.com",
            website2 = "https://github.com/test"
        )
        
        val details2 = GeneralDetails(
            id = "equality-test",
            fullName = "Test User",
            email = "test@example.com",
            phone = "555-123-4567",
            website1 = "https://test.com",
            website2 = "https://github.com/test"
        )
        
        val details3 = GeneralDetails(
            id = "different-id",
            fullName = "Test User",
            email = "test@example.com",
            phone = "555-123-4567",
            website1 = "https://test.com",
            website2 = "https://github.com/test"
        )

        assertEquals(details1, details2)
        assertNotEquals(details1, details3)
        assertEquals(details1.hashCode(), details2.hashCode())
    }

    @Test
    fun generalDetails_toString_containsAllFields() {
        val generalDetails = GeneralDetails(
            id = "toString-test",
            fullName = "String Test User",
            email = "string.test@example.com",
            phone = "555-789-0123",
            website1 = "https://stringtest.com",
            website2 = "https://github.com/stringtest"
        )

        val stringRepresentation = generalDetails.toString()

        assertTrue(stringRepresentation.contains("toString-test"))
        assertTrue(stringRepresentation.contains("String Test User"))
        assertTrue(stringRepresentation.contains("string.test@example.com"))
        assertTrue(stringRepresentation.contains("555-789-0123"))
        assertTrue(stringRepresentation.contains("https://stringtest.com"))
        assertTrue(stringRepresentation.contains("https://github.com/stringtest"))
    }
}
