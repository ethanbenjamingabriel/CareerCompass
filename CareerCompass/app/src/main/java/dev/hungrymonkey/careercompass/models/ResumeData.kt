package dev.hungrymonkey.careercompass.models

import java.time.Year
import java.time.YearMonth
import java.util.Date

data class ResumeData(
    val id: String,
    val resumeName: String,
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val website1: String? = null,
    val website2: String? = null,
    val education: List<Education>,
    val experience: List<Experience>,
    val projects: List<Project>,
    val skills: Skills,
    val templateName: String = "",
    val lastModifiedAt: Date = Date()
)

data class Education(
    val institution: String, 
    val location: String? = null, 
    val degree: String, 
    val major: String? = null,
    val minor: String? = null,
    val gpa: String? = null,
    val specialization: String? = null,
    val startDate: YearMonth,
    val endDate: YearMonth?
)

data class Experience(
    val title: String, 
    val company: String, 
    val location: String? = null, 
    val startDate: YearMonth,
    val endDate: YearMonth? = null,
    val currentlyWorking: Boolean = false,
    val bullets: List<String>
)

data class Project(
    val title: String, 
    val stack: String, 
    val date: Year,
    val bullets: List<String>
)

data class Skills(
    val languages: String, 
    val frameworks: String, 
    val tools: String, 
    val libraries: String
)
