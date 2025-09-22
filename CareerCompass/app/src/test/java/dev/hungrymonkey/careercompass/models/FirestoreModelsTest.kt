package dev.hungrymonkey.careercompass.models

import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

class FirestoreModelsTest {

    @Test
    fun resumeFirestore_toUiModel_convertsEducationCorrectly() {
        val startTimestamp = Timestamp(YearMonth.of(2020, 9).atDay(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
        val endTimestamp = Timestamp(YearMonth.of(2024, 5).atDay(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
        
        val educationMap = mapOf(
            "institution" to "Test University",
            "location" to "Test City",
            "degree" to "Bachelor of Science",
            "major" to "Computer Science",
            "minor" to "Mathematics",
            "gpa" to "3.8",
            "specialization" to "Software Engineering",
            "startDate" to startTimestamp,
            "endDate" to endTimestamp
        )

        val resumeFirestore = ResumeFirestore(
            resumeName = "Test Resume",
            fullName = "John Doe",
            phone = "555-123-4567",
            email = "john.doe@example.com",
            website1 = "https://johndoe.com",
            website2 = "https://github.com/johndoe",
            educationList = listOf(educationMap),
            experienceList = emptyList(),
            projectList = emptyList(),
            skills = mapOf(
                "languages" to "Kotlin",
                "frameworks" to "Android",
                "tools" to "Git",
                "libraries" to "Compose"
            ),
            templateName = "Template1"
        )

        val uiModel = resumeFirestore.toUiModel("test-id")

        assertEquals("test-id", uiModel.id)
        assertEquals("Test Resume", uiModel.resumeName)
        assertEquals("John Doe", uiModel.fullName)
        assertEquals(1, uiModel.education.size)
        
        val education = uiModel.education.first()
        assertEquals("Test University", education.institution)
        assertEquals("Test City", education.location)
        assertEquals("Bachelor of Science", education.degree)
        assertEquals("Computer Science", education.major)
        assertEquals("Mathematics", education.minor)
        assertEquals("3.8", education.gpa)
        assertEquals("Software Engineering", education.specialization)
        assertEquals(YearMonth.of(2020, 9), education.startDate)
        assertEquals(YearMonth.of(2024, 5), education.endDate)
    }

    @Test
    fun resumeFirestore_toUiModel_convertsExperienceCorrectly() {
        val startTimestamp = Timestamp(YearMonth.of(2022, 6).atDay(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
        val endTimestamp = Timestamp(YearMonth.of(2024, 5).atDay(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
        
        val experienceMap = mapOf(
            "title" to "Software Engineer",
            "company" to "Tech Corp",
            "location" to "San Francisco, CA",
            "startDate" to startTimestamp,
            "endDate" to endTimestamp,
            "currentlyWorking" to false,
            "bullets" to listOf("Developed applications", "Fixed bugs")
        )

        val resumeFirestore = ResumeFirestore(
            resumeName = "Test Resume",
            fullName = "Jane Smith",
            educationList = emptyList(),
            experienceList = listOf(experienceMap),
            projectList = emptyList(),
            skills = mapOf(
                "languages" to "Java",
                "frameworks" to "Spring",
                "tools" to "IntelliJ",
                "libraries" to "JUnit"
            )
        )

        val uiModel = resumeFirestore.toUiModel("exp-test-id")

        assertEquals(1, uiModel.experience.size)
        
        val experience = uiModel.experience.first()
        assertEquals("Software Engineer", experience.title)
        assertEquals("Tech Corp", experience.company)
        assertEquals("San Francisco, CA", experience.location)
        assertEquals(YearMonth.of(2022, 6), experience.startDate)
        assertEquals(YearMonth.of(2024, 5), experience.endDate)
        assertFalse(experience.currentlyWorking)
        assertEquals(2, experience.bullets.size)
        assertEquals("Developed applications", experience.bullets[0])
        assertEquals("Fixed bugs", experience.bullets[1])
    }

    @Test
    fun resumeFirestore_toUiModel_convertsProjectsCorrectly() {
        val projectMap = mapOf(
            "title" to "Mobile App",
            "stack" to "Kotlin, Android",
            "date" to "2023",
            "bullets" to listOf("Built Android app", "Published to store")
        )

        val resumeFirestore = ResumeFirestore(
            resumeName = "Project Resume",
            fullName = "Project Developer",
            educationList = emptyList(),
            experienceList = emptyList(),
            projectList = listOf(projectMap),
            skills = mapOf(
                "languages" to "Kotlin",
                "frameworks" to "Android",
                "tools" to "Android Studio",
                "libraries" to "Retrofit"
            )
        )

        val uiModel = resumeFirestore.toUiModel("proj-test-id")

        assertEquals(1, uiModel.projects.size)
        
        val project = uiModel.projects.first()
        assertEquals("Mobile App", project.title)
        assertEquals("Kotlin, Android", project.stack)
        assertEquals(Year.of(2023), project.date)
        assertEquals(2, project.bullets.size)
        assertEquals("Built Android app", project.bullets[0])
        assertEquals("Published to store", project.bullets[1])
    }

    @Test
    fun resumeFirestore_toUiModel_convertsSkillsCorrectly() {
        val skillsMap = mapOf(
            "languages" to "Python, Java, Kotlin",
            "frameworks" to "Django, Spring Boot, Android",
            "tools" to "Git, Docker, Jenkins",
            "libraries" to "NumPy, Pandas, Retrofit"
        )

        val resumeFirestore = ResumeFirestore(
            resumeName = "Skills Resume",
            fullName = "Skills Expert",
            educationList = emptyList(),
            experienceList = emptyList(),
            projectList = emptyList(),
            skills = skillsMap
        )

        val uiModel = resumeFirestore.toUiModel("skills-test-id")

        val skills = uiModel.skills
        assertEquals("Python, Java, Kotlin", skills.languages)
        assertEquals("Django, Spring Boot, Android", skills.frameworks)
        assertEquals("Git, Docker, Jenkins", skills.tools)
        assertEquals("NumPy, Pandas, Retrofit", skills.libraries)
    }

    @Test
    fun resumeData_toFirestoreModel_convertsCorrectly() {
        val education = Education(
            institution = "Test University",
            location = "Test City",
            degree = "Bachelor's",
            major = "CS",
            minor = null,
            gpa = "3.5",
            specialization = null,
            startDate = YearMonth.of(2020, 8),
            endDate = YearMonth.of(2024, 5)
        )

        val experience = Experience(
            title = "Developer",
            company = "Dev Corp",
            location = "Remote",
            startDate = YearMonth.of(2022, 1),
            endDate = YearMonth.of(2023, 12),
            currentlyWorking = false,
            bullets = listOf("Coded features", "Tested software")
        )

        val project = Project(
            title = "Web App",
            stack = "React, Node.js",
            date = Year.of(2023),
            bullets = listOf("Built frontend", "Deployed to cloud")
        )

        val skills = Skills(
            languages = "JavaScript, TypeScript",
            frameworks = "React, Express",
            tools = "VS Code, Git",
            libraries = "Lodash, Axios"
        )

        val resumeData = ResumeData(
            id = "firestore-conversion-test",
            resumeName = "Firestore Test Resume",
            fullName = "Firestore Tester",
            phone = "555-000-1111",
            email = "firestore@example.com",
            website1 = "https://firestoretest.com",
            website2 = "https://github.com/firestoretest",
            education = listOf(education),
            experience = listOf(experience),
            projects = listOf(project),
            skills = skills,
            templateName = "Template2"
        )

        val firestoreModel = resumeData.toFirestoreModel()

        assertEquals("Firestore Test Resume", firestoreModel["resumeName"])
        assertEquals("Firestore Tester", firestoreModel["fullName"])
        assertEquals("555-000-1111", firestoreModel["phone"])
        assertEquals("firestore@example.com", firestoreModel["email"])
        assertEquals("https://firestoretest.com", firestoreModel["website1"])
        assertEquals("https://github.com/firestoretest", firestoreModel["website2"])
        assertEquals("Template2", firestoreModel["templateName"])
        
        val educationList = firestoreModel["educationList"] as List<*>
        assertEquals(1, educationList.size)
        val educationMap = educationList[0] as Map<*, *>
        assertEquals("Test University", educationMap["institution"])
        assertEquals("Bachelor's", educationMap["degree"])
        
        val experienceList = firestoreModel["experienceList"] as List<*>
        assertEquals(1, experienceList.size)
        val experienceMap = experienceList[0] as Map<*, *>
        assertEquals("Developer", experienceMap["title"])
        assertEquals("Dev Corp", experienceMap["company"])
        
        val projectList = firestoreModel["projectList"] as List<*>
        assertEquals(1, projectList.size)
        val projectMap = projectList[0] as Map<*, *>
        assertEquals("Web App", projectMap["title"])
        assertEquals("React, Node.js", projectMap["stack"])
        
        val skillsMap = firestoreModel["skills"] as Map<*, *>
        assertEquals("JavaScript, TypeScript", skillsMap["languages"])
        assertEquals("React, Express", skillsMap["frameworks"])
    }

    @Test
    fun resumeFirestore_withNullFields_handlesGracefully() {
        val resumeFirestore = ResumeFirestore(
            resumeName = "Null Fields Resume",
            fullName = "Null Tester",
            phone = null,
            email = null,
            website1 = null,
            website2 = null,
            educationList = emptyList(),
            experienceList = emptyList(),
            projectList = emptyList(),
            skills = emptyMap()
        )

        val uiModel = resumeFirestore.toUiModel("null-test-id")

        assertNull(uiModel.phone)
        assertNull(uiModel.email)
        assertNull(uiModel.website1)
        assertNull(uiModel.website2)
        assertTrue(uiModel.education.isEmpty())
        assertTrue(uiModel.experience.isEmpty())
        assertTrue(uiModel.projects.isEmpty())
        assertEquals("", uiModel.skills.languages)
        assertEquals("", uiModel.skills.frameworks)
        assertEquals("", uiModel.skills.tools)
        assertEquals("", uiModel.skills.libraries)
    }

    @Test
    fun generalDetailsFirestore_toUiModel_convertsCorrectly() {
        val timestamp = Timestamp.now()
        val generalDetailsFirestore = GeneralDetailsFirestore(
            fullName = "General Details Tester",
            email = "general@example.com",
            phone = "555-222-3333",
            website1 = "https://generaltest.com",
            website2 = "https://github.com/generaltest",
            lastModifiedAt = timestamp
        )

        val uiModel = generalDetailsFirestore.toUiModel("general-test-id")

        assertEquals("general-test-id", uiModel.id)
        assertEquals("General Details Tester", uiModel.fullName)
        assertEquals("general@example.com", uiModel.email)
        assertEquals("555-222-3333", uiModel.phone)
        assertEquals("https://generaltest.com", uiModel.website1)
        assertEquals("https://github.com/generaltest", uiModel.website2)
        assertEquals(timestamp.toDate(), uiModel.lastModifiedAt)
    }

    @Test
    fun generalDetails_toFirestoreModel_convertsCorrectly() {
        val testDate = Date()
        val generalDetails = GeneralDetails(
            id = "firestore-general-test",
            fullName = "Firestore General Tester",
            email = "firestoregeneral@example.com",
            phone = "555-444-5555",
            website1 = "https://firestoregeneraltest.com",
            website2 = "https://github.com/firestoregeneraltest",
            lastModifiedAt = testDate
        )

        val firestoreModel = generalDetails.toFirestoreModel()

        assertEquals("Firestore General Tester", firestoreModel.fullName)
        assertEquals("firestoregeneral@example.com", firestoreModel.email)
        assertEquals("555-444-5555", firestoreModel.phone)
        assertEquals("https://firestoregeneraltest.com", firestoreModel.website1)
        assertEquals("https://github.com/firestoregeneraltest", firestoreModel.website2)
        assertEquals(testDate, firestoreModel.lastModifiedAt.toDate())
    }

    @Test
    fun resumeFirestore_withMissingOptionalFields_handlesDefaults() {
        val educationMap = mapOf(
            "institution" to "Minimal University",
            "degree" to "Minimal Degree",
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now()
        )

        val resumeFirestore = ResumeFirestore(
            resumeName = "Minimal Resume",
            fullName = "Minimal User",
            educationList = listOf(educationMap),
            experienceList = emptyList(),
            projectList = emptyList(),
            skills = mapOf(
                "languages" to "Kotlin",
                "frameworks" to "Android"
            )
        )

        val uiModel = resumeFirestore.toUiModel("minimal-test-id")

        val education = uiModel.education.first()
        assertNull(education.location)
        assertNull(education.major)
        assertNull(education.minor)
        assertNull(education.gpa)
        assertNull(education.specialization)
        
        assertEquals("", uiModel.skills.tools)
        assertEquals("", uiModel.skills.libraries)
    }
}
