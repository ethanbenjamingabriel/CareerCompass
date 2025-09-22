package dev.hungrymonkey.careercompass.models

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId

data class ResumeFirestore(
    val resumeName: String = "",
    val fullName: String = "",
    val phone: String? = null,
    val email: String? = null,
    val website1: String? = null,
    val website2: String? = null,
    val educationList: List<Map<String, Any>> = emptyList(),
    val experienceList: List<Map<String, Any>> = emptyList(),
    val projectList: List<Map<String, Any>> = emptyList(),
    val skills: Map<String, String> = emptyMap(),
    val templateName: String = "",
    val lastModifiedAt: Timestamp = Timestamp.now()
) {
    fun toUiModel(id: String): ResumeData {
        val uiEducationList = educationList.map { map ->
            Education(
                institution = map["institution"] as? String ?: "",
                location = map["location"] as? String,
                degree = map["degree"] as? String ?: "",
                major = map["major"] as? String,
                minor = map["minor"] as? String,
                gpa = map["gpa"] as? String,
                specialization = map["specialization"] as? String,
                startDate = (map["startDate"] as? Timestamp)?.let {
                    YearMonth.from(Instant.ofEpochSecond(it.seconds).atZone(ZoneId.systemDefault()).toLocalDate())
                } ?: YearMonth.now(),
                endDate = (map["endDate"] as? Timestamp)?.let {
                    YearMonth.from(Instant.ofEpochSecond(it.seconds).atZone(ZoneId.systemDefault()).toLocalDate())
                } ?: YearMonth.now(),
            )
        }
        val uiExperienceList = experienceList.map { map ->
            Experience(
                title = map["title"] as? String ?: "",
                company = map["company"] as? String ?: "",
                location = map["location"] as? String,
                startDate = (map["startDate"] as? Timestamp)?.let {
                    YearMonth.from(Instant.ofEpochSecond(it.seconds).atZone(ZoneId.systemDefault()).toLocalDate())
                } ?: YearMonth.now(),
                endDate = (map["endDate"] as? Timestamp)?.let {
                    YearMonth.from(Instant.ofEpochSecond(it.seconds).atZone(ZoneId.systemDefault()).toLocalDate())
                },
                currentlyWorking = map["currentlyWorking"] as? Boolean == true,
                bullets = (map["bullets"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }
        val uiProjectList = projectList.map { map ->
            Project(
                title = map["title"] as? String ?: "",
                stack = map["stack"] as? String ?: "",
                date = (map["date"] as? String).let { Year.parse(it) },
                bullets = (map["bullets"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }
        val uiSkills = Skills(
            languages = skills["languages"] ?: "",
            frameworks = skills["frameworks"] ?: "",
            tools = skills["tools"] ?: "",
            libraries = skills["libraries"] ?: ""
        )
        return ResumeData(id, resumeName, fullName, phone, email, website1, website2, uiEducationList, uiExperienceList, uiProjectList, uiSkills, templateName, lastModifiedAt.toDate())
    }
}

fun ResumeData.toFirestoreModel(): Map<String, Any?> = mapOf(
    "resumeName" to resumeName,
    "fullName" to fullName,
    "phone" to phone,
    "email" to email,
    "website1" to website1,
    "website2" to website2,
    "educationList" to education.map {
        mapOf(
            "institution" to it.institution,
            "location" to it.location,
            "degree" to it.degree,
            "major" to it.major,
            "minor" to it.minor,
            "gpa" to it.gpa,
            "specialization" to it.specialization,
            "startDate" to Timestamp(it.startDate.atDay(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
            "endDate" to it.endDate?.atDay(1)?.atStartOfDay(ZoneId.systemDefault())
                ?.let { it1 -> Timestamp(it1.toEpochSecond(), 0) }
        )
    },
    "experienceList" to experience.map {
        mapOf(
            "title" to it.title,
            "company" to it.company,
            "location" to it.location,
            "startDate" to Timestamp(it.startDate.atDay(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
            "endDate" to it.endDate?.let { d -> Timestamp(d.atDay(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0) },
            "currentlyWorking" to it.currentlyWorking,
            "bullets" to it.bullets
        )
    },
    "projectList" to projects.map {
        mapOf(
            "title" to it.title,
            "stack" to it.stack,
            "date" to it.date.toString(),
            "bullets" to it.bullets
        )
    },
    "skills" to mapOf(
        "languages" to skills.languages,
        "frameworks" to skills.frameworks,
        "tools" to skills.tools,
        "libraries" to skills.libraries
    ),
    "templateName" to templateName,
    "lastModifiedAt" to Timestamp(lastModifiedAt)
)