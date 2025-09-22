package dev.hungrymonkey.careercompass.models

import org.junit.Test
import org.junit.Assert.*
import java.time.Year
import java.time.YearMonth
import java.util.*

fun Education.isValid(): Boolean = institution.isNotBlank() && degree.isNotBlank()
fun Experience.isValid(): Boolean = title.isNotBlank() && company.isNotBlank()
fun Project.isValid(): Boolean = title.isNotBlank() && stack.isNotBlank()
fun Skills.isValid(): Boolean = languages.isNotBlank() && frameworks.isNotBlank()

class ResumeDataTest {

    @Test
    fun createResumeData_withAllFields_returnsValidResume() {
        val education = listOf(
            Education(
                institution = "Test University",
                location = "Test City",
                degree = "Bachelor of Science",
                major = "Computer Science",
                minor = "Mathematics",
                gpa = "3.8",
                specialization = "Software Engineering",
                startDate = YearMonth.of(2020, 9),
                endDate = YearMonth.of(2024, 5)
            )
        )

        val experience = listOf(
            Experience(
                title = "Software Engineer",
                company = "Tech Company",
                location = "San Francisco, CA",
                startDate = YearMonth.of(2022, 6),
                endDate = YearMonth.of(2024, 5),
                currentlyWorking = false,
                bullets = listOf("Developed web applications", "Collaborated with team")
            )
        )

        val projects = listOf(
            Project(
                title = "Resume Builder",
                stack = "Kotlin, Compose",
                date = Year.of(2024),
                bullets = listOf("Built Android app", "Implemented PDF generation")
            )
        )

        val skills = Skills(
            languages = "Kotlin, Java, Python",
            frameworks = "Android, Spring Boot",
            tools = "Git, IntelliJ IDEA",
            libraries = "Retrofit, Compose"
        )

        val resumeData = ResumeData(
            id = "test-id",
            resumeName = "Software Engineer Resume",
            fullName = "John Doe",
            phone = "555-123-4567",
            email = "john.doe@example.com",
            website1 = "https://johndoe.com",
            website2 = "https://github.com/johndoe",
            education = education,
            experience = experience,
            projects = projects,
            skills = skills,
            templateName = "Template1"
        )

        assertEquals("test-id", resumeData.id)
        assertEquals("Software Engineer Resume", resumeData.resumeName)
        assertEquals("John Doe", resumeData.fullName)
        assertEquals("555-123-4567", resumeData.phone)
        assertEquals("john.doe@example.com", resumeData.email)
        assertEquals("https://johndoe.com", resumeData.website1)
        assertEquals("https://github.com/johndoe", resumeData.website2)
        assertEquals(1, resumeData.education.size)
        assertEquals(1, resumeData.experience.size)
        assertEquals(1, resumeData.projects.size)
        assertEquals("Template1", resumeData.templateName)
    }

    @Test
    fun createResumeData_withMinimalFields_returnsValidResume() {
        val resumeData = ResumeData(
            id = "minimal-id",
            resumeName = "Basic Resume",
            fullName = "Jane Smith",
            phone = null,
            email = null,
            website1 = null,
            website2 = null,
            education = emptyList(),
            experience = emptyList(),
            projects = emptyList(),
            skills = Skills("", "", "", ""),
            templateName = "Template2"
        )

        assertEquals("minimal-id", resumeData.id)
        assertEquals("Basic Resume", resumeData.resumeName)
        assertEquals("Jane Smith", resumeData.fullName)
        assertNull(resumeData.phone)
        assertNull(resumeData.email)
        assertNull(resumeData.website1)
        assertNull(resumeData.website2)
        assertTrue(resumeData.education.isEmpty())
        assertTrue(resumeData.experience.isEmpty())
        assertTrue(resumeData.projects.isEmpty())
    }

    @Test
    fun education_isValid_withRequiredFields_returnsTrue() {
        val education = Education(
            institution = "Test University",
            location = null,
            degree = "Bachelor's",
            major = null,
            minor = null,
            gpa = null,
            specialization = null,
            startDate = YearMonth.of(2020, 9),
            endDate = YearMonth.of(2024, 5)
        )

        assertTrue(education.isValid())
    }

    @Test
    fun education_isValid_withBlankInstitution_returnsFalse() {
        val education = Education(
            institution = "",
            location = null,
            degree = "Bachelor's",
            major = null,
            minor = null,
            gpa = null,
            specialization = null,
            startDate = YearMonth.of(2020, 9),
            endDate = YearMonth.of(2024, 5)
        )

        assertFalse(education.isValid())
    }

    @Test
    fun education_isValid_withBlankDegree_returnsFalse() {
        val education = Education(
            institution = "Test University",
            location = null,
            degree = "",
            major = null,
            minor = null,
            gpa = null,
            specialization = null,
            startDate = YearMonth.of(2020, 9),
            endDate = YearMonth.of(2024, 5)
        )

        assertFalse(education.isValid())
    }

    @Test
    fun experience_isValid_withRequiredFields_returnsTrue() {
        val experience = Experience(
            title = "Software Engineer",
            company = "Tech Corp",
            location = null,
            startDate = YearMonth.of(2022, 1),
            endDate = null,
            currentlyWorking = true,
            bullets = listOf("Developed features", "Fixed bugs")
        )

        assertTrue(experience.isValid())
    }

    @Test
    fun experience_isValid_withBlankTitle_returnsFalse() {
        val experience = Experience(
            title = "",
            company = "Tech Corp",
            location = null,
            startDate = YearMonth.of(2022, 1),
            endDate = null,
            currentlyWorking = true,
            bullets = listOf("Developed features")
        )

        assertFalse(experience.isValid())
    }

    @Test
    fun experience_isValid_withBlankCompany_returnsFalse() {
        val experience = Experience(
            title = "Software Engineer",
            company = "",
            location = null,
            startDate = YearMonth.of(2022, 1),
            endDate = null,
            currentlyWorking = true,
            bullets = listOf("Developed features")
        )

        assertFalse(experience.isValid())
    }

    @Test
    fun project_isValid_withRequiredFields_returnsTrue() {
        val project = Project(
            title = "Mobile App",
            stack = "Kotlin, Android",
            date = Year.of(2023),
            bullets = listOf("Built native Android app", "Published to Play Store")
        )

        assertTrue(project.isValid())
    }

    @Test
    fun project_isValid_withBlankTitle_returnsFalse() {
        val project = Project(
            title = "",
            stack = "Kotlin, Android",
            date = Year.of(2023),
            bullets = listOf("Built native Android app")
        )

        assertFalse(project.isValid())
    }

    @Test
    fun project_isValid_withBlankStack_returnsFalse() {
        val project = Project(
            title = "Mobile App",
            stack = "",
            date = Year.of(2023),
            bullets = listOf("Built native Android app")
        )

        assertFalse(project.isValid())
    }

    @Test
    fun skills_isValid_withAllRequiredFields_returnsTrue() {
        val skills = Skills(
            languages = "Kotlin, Java",
            frameworks = "Android, Spring",
            tools = "Git, IDE",
            libraries = "Retrofit, Gson"
        )

        assertTrue(skills.isValid())
    }

    @Test
    fun skills_isValid_withBlankLanguages_returnsFalse() {
        val skills = Skills(
            languages = "",
            frameworks = "Android, Spring",
            tools = "Git, IDE",
            libraries = "Retrofit, Gson"
        )

        assertFalse(skills.isValid())
    }

    @Test
    fun skills_isValid_withBlankFrameworks_returnsFalse() {
        val skills = Skills(
            languages = "Kotlin, Java",
            frameworks = "",
            tools = "Git, IDE",
            libraries = "Retrofit, Gson"
        )

        assertFalse(skills.isValid())
    }

    @Test
    fun skills_isValid_withBlankTools_returnsTrue() {
        val skills = Skills(
            languages = "Kotlin, Java",
            frameworks = "Android, Spring",
            tools = "",
            libraries = "Retrofit, Gson"
        )

        assertTrue(skills.isValid())
    }

    @Test
    fun skills_isValid_withBlankLibraries_returnsTrue() {
        val skills = Skills(
            languages = "Kotlin, Java",
            frameworks = "Android, Spring",
            tools = "Git, IDE",
            libraries = ""
        )

        assertTrue(skills.isValid())
    }

    @Test
    fun resumeData_withDefaultConstructor_hasCorrectDefaults() {
        val resumeData = ResumeData(
            id = "test",
            resumeName = "Test Resume",
            fullName = "Test User",
            education = emptyList(),
            experience = emptyList(),
            projects = emptyList(),
            skills = Skills("", "", "", "")
        )

        assertNull(resumeData.phone)
        assertNull(resumeData.email)
        assertNull(resumeData.website1)
        assertNull(resumeData.website2)
        assertEquals("", resumeData.templateName)
        assertNotNull(resumeData.lastModifiedAt)
    }

    @Test
    fun experience_withCurrentlyWorking_hasNullEndDate() {
        val experience = Experience(
            title = "Current Job",
            company = "Current Company",
            location = "Remote",
            startDate = YearMonth.of(2023, 1),
            endDate = null,
            currentlyWorking = true,
            bullets = listOf("Working on current projects")
        )

        assertNull(experience.endDate)
        assertTrue(experience.currentlyWorking)
    }

    @Test
    fun experience_withEndDate_hasCurrentlyWorkingFalse() {
        val experience = Experience(
            title = "Past Job",
            company = "Past Company",
            location = "Office",
            startDate = YearMonth.of(2020, 1),
            endDate = YearMonth.of(2022, 12),
            currentlyWorking = false,
            bullets = listOf("Completed projects")
        )

        assertEquals(YearMonth.of(2022, 12), experience.endDate)
        assertFalse(experience.currentlyWorking)
    }

    @Test
    fun education_withOptionalFields_storesCorrectly() {
        val education = Education(
            institution = "University of Test",
            location = "Test City, TS",
            degree = "Master of Science",
            major = "Computer Science",
            minor = "Statistics",
            gpa = "3.9",
            specialization = "Machine Learning",
            startDate = YearMonth.of(2021, 8),
            endDate = YearMonth.of(2023, 5)
        )

        assertEquals("Test City, TS", education.location)
        assertEquals("Computer Science", education.major)
        assertEquals("Statistics", education.minor)
        assertEquals("3.9", education.gpa)
        assertEquals("Machine Learning", education.specialization)
    }

    @Test
    fun project_withMultipleBullets_storesAllBullets() {
        val bullets = listOf(
            "Designed and implemented user interface",
            "Integrated REST APIs for data fetching",
            "Optimized app performance by 40%",
            "Published to Google Play Store with 4.8 rating"
        )
        
        val project = Project(
            title = "E-commerce App",
            stack = "Kotlin, Jetpack Compose, Retrofit",
            date = Year.of(2024),
            bullets = bullets
        )

        assertEquals(4, project.bullets.size)
        assertEquals("Designed and implemented user interface", project.bullets[0])
        assertEquals("Published to Google Play Store with 4.8 rating", project.bullets[3])
    }

    @Test
    fun skills_withDetailedInformation_storesCorrectly() {
        val skills = Skills(
            languages = "Kotlin, Java, Python, JavaScript, TypeScript",
            frameworks = "Android SDK, Spring Boot, React, Node.js",
            tools = "Git, Docker, Jenkins, IntelliJ IDEA, VS Code",
            libraries = "Retrofit, Room, Compose, JUnit, Mockito"
        )

        assertTrue(skills.languages.contains("Kotlin"))
        assertTrue(skills.languages.contains("TypeScript"))
        assertTrue(skills.frameworks.contains("Android SDK"))
        assertTrue(skills.frameworks.contains("Node.js"))
        assertTrue(skills.tools.contains("Docker"))
        assertTrue(skills.libraries.contains("Compose"))
    }
}
