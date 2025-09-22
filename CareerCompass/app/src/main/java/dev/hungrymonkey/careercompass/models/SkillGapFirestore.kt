package dev.hungrymonkey.careercompass.models

import com.google.firebase.firestore.FirebaseFirestore
import dev.hungrymonkey.careercompass.viewmodel.SkillGapFirestoreRepository
import kotlinx.coroutines.runBlocking
import com.google.firebase.firestore.PropertyName

data class UserSkillFirestore(
    @PropertyName("skillId") val skillId: String = "",
    @PropertyName("skillName") val skillName: String = "",
    @PropertyName("proficiency") val proficiency: String = "",
    @PropertyName("hasExperience") val hasExperience: Boolean = false,
    @PropertyName("hasCertification") val hasCertification: Boolean = false,
    @PropertyName("yearsOfExperience") val yearsOfExperience: Int = 0,
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("updatedAt") val updatedAt: Long = System.currentTimeMillis()
)

data class CareerProfileFirestore(
    @PropertyName("targetRoleId") val targetRoleId: String? = null,
    @PropertyName("targetRoleTitle") val targetRoleTitle: String? = null,
    @PropertyName("lastAnalysisDate") val lastAnalysisDate: Long? = null,
    @PropertyName("overallMatchPercentage") val overallMatchPercentage: Float? = null,
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("updatedAt") val updatedAt: Long = System.currentTimeMillis()
)

fun UserSkill.toFirestoreModel(): UserSkillFirestore {
    return UserSkillFirestore(
        skillId = skill.id,
        skillName = skill.name,
        proficiency = proficiency.name,
        hasExperience = hasExperience,
        hasCertification = hasCertification,
        yearsOfExperience = yearsOfExperience,
        updatedAt = System.currentTimeMillis()
    )
}

fun UserSkillFirestore.toUIModel(): UserSkill? {
    val firestoreRepository = SkillGapFirestoreRepository(FirebaseFirestore.getInstance())
    val skill = runBlocking { 
        firestoreRepository.findSkillById(skillId).getOrNull()
    }
    
    val proficiencyLevel = try {
        ProficiencyLevel.valueOf(proficiency)
    } catch (_: Exception) {
        ProficiencyLevel.BEGINNER
    }
    
    return skill?.let {
        UserSkill(
            skill = it,
            proficiency = proficiencyLevel,
            hasExperience = hasExperience,
            hasCertification = hasCertification,
            yearsOfExperience = yearsOfExperience
        )
    }
}
