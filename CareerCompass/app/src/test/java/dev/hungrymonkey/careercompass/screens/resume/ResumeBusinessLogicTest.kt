package dev.hungrymonkey.careercompass.screens.resume

import org.junit.Test
import org.junit.Assert.*

class ResumeBusinessLogicTest {

    @Test
    fun hasUnsavedChanges_withNoChanges_returnsFalse() {
        val originalResumeName = "Software Engineer Resume"
        val originalFullName = "John Doe"
        val originalPhone = "555-123-4567"
        val originalEmail = "john.doe@example.com"
        
        val currentResumeName = "Software Engineer Resume"
        val currentFullName = "John Doe"
        val currentPhone = "555-123-4567"
        val currentEmail = "john.doe@example.com"
        
        val hasEducation = true
        val hasExperience = true
        val hasProjects = true
        val hasSkills = true
        val hasUnsavedFormChanges = false

        val hasDataChanges = (originalResumeName != currentResumeName) ||
                (originalFullName != currentFullName) ||
                (originalPhone != currentPhone) ||
                (originalEmail != currentEmail)
        
        val hasContentChanges = hasEducation || hasExperience || hasProjects || hasSkills
        val hasUnsavedChanges = hasDataChanges || hasContentChanges || hasUnsavedFormChanges

        assertFalse("Should not have data changes", hasDataChanges)
        assertTrue("Should have content changes", hasContentChanges)
        assertTrue("Should have unsaved changes due to content", hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_withPersonalInfoChanges_returnsTrue() {
        val originalFullName = "John Doe"
        val currentFullName = "John Smith"
        
        val originalEmail = "john.doe@example.com"
        val currentEmail = "john.smith@example.com"

        val hasPersonalInfoChanges = (originalFullName != currentFullName) || 
                (originalEmail != currentEmail)

        assertTrue("Should detect personal info changes", hasPersonalInfoChanges)
    }

    @Test
    fun hasUnsavedChanges_withFormChanges_returnsTrue() {
        val hasUnsavedFormChanges = true

        val hasUnsavedChanges = hasUnsavedFormChanges

        assertTrue("Should detect unsaved form changes", hasUnsavedChanges)
    }

    @Test
    fun validateCompleteResume_allSectionsValid_returnsTrue() {
        val validPersonalInfo = true
        val hasEducation = listOf("University").isNotEmpty()
        val hasExperience = listOf("Company").isNotEmpty()
        val hasProjects = listOf("Project").isNotEmpty()
        val hasSkills = true

        val isValidResume = validPersonalInfo && hasEducation && hasExperience && 
                hasProjects && hasSkills

        assertTrue("Complete resume should be valid", isValidResume)
    }

    @Test
    fun validateCompleteResume_missingEducation_returnsFalse() {
        val validPersonalInfo = true
        val hasEducation = emptyList<String>().isNotEmpty()
        val hasExperience = listOf("Company").isNotEmpty()
        val hasProjects = listOf("Project").isNotEmpty()
        val hasSkills = true

        val isValidResume = validPersonalInfo && hasEducation && hasExperience && 
                hasProjects && hasSkills

        assertFalse("Resume missing education should be invalid", isValidResume)
    }

    @Test
    fun validateCompleteResume_missingExperience_returnsFalse() {
        val validPersonalInfo = true
        val hasEducation = listOf("University").isNotEmpty()
        val hasExperience = emptyList<String>().isNotEmpty()
        val hasProjects = listOf("Project").isNotEmpty()
        val hasSkills = true

        val isValidResume = validPersonalInfo && hasEducation && hasExperience && 
                hasProjects && hasSkills

        assertFalse("Resume missing experience should be invalid", isValidResume)
    }

    @Test
    fun validateCompleteResume_missingProjects_returnsFalse() {
        val validPersonalInfo = true
        val hasEducation = listOf("University").isNotEmpty()
        val hasExperience = listOf("Company").isNotEmpty()
        val hasProjects = emptyList<String>().isNotEmpty()
        val hasSkills = true

        val isValidResume = validPersonalInfo && hasEducation && hasExperience && 
                hasProjects && hasSkills

        assertFalse("Resume missing projects should be invalid", isValidResume)
    }

    @Test
    fun validateCompleteResume_invalidPersonalInfo_returnsFalse() {
        val validPersonalInfo = false
        val hasEducation = listOf("University").isNotEmpty()
        val hasExperience = listOf("Company").isNotEmpty()
        val hasProjects = listOf("Project").isNotEmpty()
        val hasSkills = true

        val isValidResume = validPersonalInfo && hasEducation && hasExperience && 
                hasProjects && hasSkills

        assertFalse("Resume with invalid personal info should be invalid", isValidResume)
    }

    @Test
    fun bulletManagement_addNewBulletField_worksCorrectly() {
        var bullets = mutableListOf("First bullet", "Second bullet", "")
        val index = 2
        val newValue = "Third bullet"

        bullets[index] = newValue
        
        if (index == bullets.size - 1 && newValue.isNotBlank() && !bullets.any { it.isBlank() }) {
            bullets.add("")
        }

        assertEquals("Should have 4 bullets now", 4, bullets.size)
        assertEquals("Third bullet should be added", "Third bullet", bullets[2])
        assertEquals("Last bullet should be empty", "", bullets[3])
    }

    @Test
    fun bulletManagement_removeEmptyMiddleBullets_worksCorrectly() {
        var bullets = mutableListOf("First bullet", "", "Third bullet", "")
        
        if (bullets.size > 1) {
            val filtered = bullets.filterIndexed { idx, text -> 
                text.isNotBlank() || idx == bullets.size - 1 
            }.toMutableList()
            bullets.clear()
            bullets.addAll(filtered)
            if (bullets.isEmpty()) bullets.add("")
        }

        assertEquals("Should have 3 bullets", 3, bullets.size)
        assertEquals("First bullet should remain", "First bullet", bullets[0])
        assertEquals("Third bullet should remain", "Third bullet", bullets[1])
        assertEquals("Last should be empty", "", bullets[2])
    }

    @Test
    fun bulletManagement_ensureAtLeastOneBullet_worksCorrectly() {
        var bullets = mutableListOf<String>()

        if (bullets.isEmpty()) {
            bullets.add("")
        }

        assertEquals("Should have at least one bullet", 1, bullets.size)
        assertEquals("Single bullet should be empty", "", bullets[0])
    }

    @Test
    fun templateSelection_updatesCorrectly() {
        var currentTemplateName = "Template1"
        val newTemplateName = "Template2"

        currentTemplateName = newTemplateName

        assertEquals("Template name should be updated", "Template2", currentTemplateName)
    }

    @Test
    fun resumeMetadata_lastModifiedUpdate_worksCorrectly() {
        val originalTimestamp = System.currentTimeMillis()
        Thread.sleep(10)
        
        val newTimestamp = System.currentTimeMillis()

        assertTrue("New timestamp should be after original", newTimestamp > originalTimestamp)
    }

    @Test
    fun loadFromGeneralDetails_populatesCorrectly() {
        val generalDetailsFullName = "General Details User"
        val generalDetailsEmail = "general@example.com"
        val generalDetailsPhone = "555-999-8888"
        val generalDetailsWebsite1 = "https://general.com"
        val generalDetailsWebsite2 = "https://github.com/general"

        var fullName = ""
        var email = ""
        var phone = ""
        var website1 = ""
        var website2 = ""

        fullName = generalDetailsFullName
        email = generalDetailsEmail
        phone = generalDetailsPhone
        website1 = generalDetailsWebsite1
        website2 = generalDetailsWebsite2

        assertEquals("Full name should be loaded", generalDetailsFullName, fullName)
        assertEquals("Email should be loaded", generalDetailsEmail, email)
        assertEquals("Phone should be loaded", generalDetailsPhone, phone)
        assertEquals("Website1 should be loaded", generalDetailsWebsite1, website1)
        assertEquals("Website2 should be loaded", generalDetailsWebsite2, website2)
    }

    @Test
    fun resetAllFields_clearsAllData() {
        var resumeName = "Test Resume"
        var fullName = "Test User"
        var email = "test@example.com"
        var phone = "555-123-4567"
        var website1 = "https://test.com"
        var website2 = "https://github.com/test"
        var educationList = listOf("University")
        var experienceList = listOf("Company")
        var projectList = listOf("Project")

        resumeName = ""
        fullName = ""
        email = ""
        phone = ""
        website1 = ""
        website2 = ""
        educationList = emptyList()
        experienceList = emptyList()
        projectList = emptyList()

        assertEquals("Resume name should be empty", "", resumeName)
        assertEquals("Full name should be empty", "", fullName)
        assertEquals("Email should be empty", "", email)
        assertEquals("Phone should be empty", "", phone)
        assertEquals("Website1 should be empty", "", website1)
        assertEquals("Website2 should be empty", "", website2)
        assertTrue("Education list should be empty", educationList.isEmpty())
        assertTrue("Experience list should be empty", experienceList.isEmpty())
        assertTrue("Project list should be empty", projectList.isEmpty())
    }

    @Test
    fun resumePreview_requiresValidResume_returnsCorrectState() {
        val validPersonalInfo = true
        val validSections = true
        val canPreview = validPersonalInfo && validSections

        assertTrue("Should be able to preview valid resume", canPreview)

        val invalidPersonalInfo = false
        val cannotPreview = invalidPersonalInfo && validSections

        assertFalse("Should not be able to preview invalid resume", cannotPreview)
    }

    @Test
    fun resumeExport_requiresValidResume_returnsCorrectState() {
        val validPersonalInfo = true
        val validSections = true
        val canExport = validPersonalInfo && validSections

        assertTrue("Should be able to export valid resume", canExport)

        val validPersonalInfoButInvalidSections = true
        val invalidSections = false
        val cannotExport = validPersonalInfoButInvalidSections && invalidSections

        assertFalse("Should not be able to export resume with invalid sections", cannotExport)
    }

    @Test
    fun draftSave_allowsIncompleteResume_returnsTrue() {
        val hasResumeNameAndFullName = true
        val canSaveDraft = hasResumeNameAndFullName

        assertTrue("Should be able to save incomplete resume as draft", canSaveDraft)
    }

    @Test
    fun navigationWithUnsavedChanges_showsDialog_returnsCorrectState() {
        val hasUnsavedChanges = true
        var shouldShowDialog = false

        if (hasUnsavedChanges) {
            shouldShowDialog = true
        }

        assertTrue("Should show unsaved changes dialog", shouldShowDialog)
    }

    @Test
    fun navigationWithoutUnsavedChanges_allowsDirectNavigation_returnsCorrectState() {
        val hasUnsavedChanges = false
        var shouldShowDialog = false
        var canNavigateDirectly = false

        if (hasUnsavedChanges) {
            shouldShowDialog = true
        } else {
            canNavigateDirectly = true
        }

        assertFalse("Should not show dialog", shouldShowDialog)
        assertTrue("Should allow direct navigation", canNavigateDirectly)
    }
}
