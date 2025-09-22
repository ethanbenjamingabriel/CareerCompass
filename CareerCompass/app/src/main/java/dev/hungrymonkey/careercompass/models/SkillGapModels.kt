package dev.hungrymonkey.careercompass.models

data class Skill(
    val id: String,
    val name: String,
    val category: SkillCategory,
    val description: String = ""
)

data class UserSkill(
    val skill: Skill,
    val proficiency: ProficiencyLevel,
    val hasExperience: Boolean = false,
    val hasCertification: Boolean = false,
    val yearsOfExperience: Int = 0
)

data class JobRole(
    val id: String,
    val title: String,
    val industry: Industry,
    val description: String,
    val requiredSkills: List<RequiredSkill>,
    val averageSalary: String = "",
    val growthRate: String = ""
)

data class RequiredSkill(
    val skill: Skill,
    val importance: SkillImportance,
    val minimumProficiency: ProficiencyLevel
)

data class SkillGap(
    val skill: Skill,
    val currentLevel: ProficiencyLevel?,
    val requiredLevel: ProficiencyLevel,
    val importance: SkillImportance,
    val gapSeverity: GapSeverity
)

data class LearningResource(
    val id: String,
    val title: String,
    val type: ResourceType,
    val provider: String,
    val duration: String,
    val cost: String,
    val rating: Float,
    val url: String,
    val skills: List<Skill>
)

data class SkillAnalysisResult(
    val targetRole: JobRole,
    val overallMatchPercentage: Float,
    val skillGaps: List<SkillGap>,
    val strengths: List<UserSkill>,
    val recommendations: List<LearningResource>
)

enum class SkillCategory(val displayName: String) {
    PROGRAMMING_LANGUAGES("Programming Languages"),
    FRAMEWORKS_LIBRARIES("Frameworks & Libraries"),
    DATABASES("Databases"),
    CLOUD_PLATFORMS("Cloud Platforms"),
    DEVOPS_TOOLS("DevOps & Tools"),
    SOFT_SKILLS("Soft Skills"),
    DESIGN_UX("Design & UX"),
    DATA_SCIENCE("Data Science & Analytics"),
    MOBILE_DEVELOPMENT("Mobile Development"),
    WEB_DEVELOPMENT("Web Development"),
    CYBERSECURITY("Cybersecurity"),
    AI_ML("AI & Machine Learning")
}

enum class ProficiencyLevel(val displayName: String, val value: Int) {
    BEGINNER("Beginner", 1),
    INTERMEDIATE("Intermediate", 2),
    EXPERT("Expert", 3)
}

enum class SkillImportance(val displayName: String) {
    CRITICAL("Critical"),
    IMPORTANT("Important"),
    NICE_TO_HAVE("Nice to Have")
}

enum class GapSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class Industry(val displayName: String) {
    SOFTWARE_ENGINEERING("Software Engineering"),
    DATA_SCIENCE("Data Science"),
    CYBERSECURITY("Cybersecurity"),
    PRODUCT_MANAGEMENT("Product Management"),
    UI_UX_DESIGN("UI/UX Design"),
    DEVOPS("DevOps & Infrastructure"),
    MOBILE_DEVELOPMENT("Mobile Development"),
    FRONTEND_DEVELOPMENT("Frontend Development"),
    BACKEND_DEVELOPMENT("Backend Development"),
    FULL_STACK_DEVELOPMENT("Full Stack Development"),
    AI_ML_ENGINEERING("AI/ML Engineering"),
    CLOUD_ARCHITECTURE("Cloud Architecture")
}

enum class ResourceType(val displayName: String) {
    COURSE("Online Course"),
    CERTIFICATION("Certification"),
    BOOK("Book"),
    TUTORIAL("Tutorial"),
    BOOTCAMP("Bootcamp"),
    WORKSHOP("Workshop"),
    DOCUMENTATION("Documentation"),
    PROJECT("Practice Project")
}
