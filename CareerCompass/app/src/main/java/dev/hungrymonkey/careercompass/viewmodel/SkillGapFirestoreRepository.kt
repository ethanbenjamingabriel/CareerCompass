package dev.hungrymonkey.careercompass.viewmodel

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.hungrymonkey.careercompass.models.*
import kotlinx.coroutines.tasks.await

class SkillGapFirestoreRepository(private val db: FirebaseFirestore) {
    
    companion object {
        private const val TAG = "SkillGapFirestoreRepo"
        
        const val SKILLS_COLLECTION = "skills"
        const val JOB_ROLES_COLLECTION = "jobRoles"
        const val LEARNING_RESOURCES_COLLECTION = "learningResources"
    }

    suspend fun getAllSkills(): Result<List<Skill>> {
        return try {
            val snapshot = db.collection(SKILLS_COLLECTION)
                .get()
                .await()
            
            val skills = snapshot.documents.mapNotNull { doc ->
                try {
                    Skill(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        category = SkillCategory.valueOf(doc.getString("category") ?: return@mapNotNull null),
                        description = doc.getString("description") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing skill document ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Fetched ${skills.size} skills from Firestore")
            Result.success(skills)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching skills from Firestore", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllJobRoles(): Result<List<JobRole>> {
        return try {
            val skillsResult = getAllSkills()
            if (skillsResult.isFailure) {
                return Result.failure(skillsResult.exceptionOrNull() ?: Exception("Failed to fetch skills"))
            }
            
            val skillsMap = skillsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            val snapshot = db.collection(JOB_ROLES_COLLECTION)
                .get()
                .await()
            
            val jobRoles = snapshot.documents.mapNotNull { doc ->
                try {
                    val requiredSkillsData = doc.get("requiredSkills") as? List<Map<String, Any>> ?: emptyList()
                    
                    val requiredSkills = requiredSkillsData.mapNotNull { skillData ->
                        val skillId = skillData["skillId"] as? String ?: return@mapNotNull null
                        val skill = skillsMap[skillId] ?: return@mapNotNull null
                        val importance = SkillImportance.valueOf(skillData["importance"] as? String ?: return@mapNotNull null)
                        val minimumProficiency = ProficiencyLevel.valueOf(skillData["minimumProficiency"] as? String ?: return@mapNotNull null)
                        
                        RequiredSkill(skill, importance, minimumProficiency)
                    }
                    
                    JobRole(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        industry = Industry.valueOf(doc.getString("industry") ?: return@mapNotNull null),
                        description = doc.getString("description") ?: "",
                        requiredSkills = requiredSkills,
                        averageSalary = doc.getString("averageSalary") ?: "",
                        growthRate = doc.getString("growthRate") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing job role document ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Fetched ${jobRoles.size} job roles from Firestore")
            Result.success(jobRoles)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching job roles from Firestore", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllLearningResources(): Result<List<LearningResource>> {
        return try {
            val skillsResult = getAllSkills()
            if (skillsResult.isFailure) {
                return Result.failure(skillsResult.exceptionOrNull() ?: Exception("Failed to fetch skills"))
            }
            
            val skillsMap = skillsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            val snapshot = db.collection(LEARNING_RESOURCES_COLLECTION)
                .get()
                .await()
            
            val resources = snapshot.documents.mapNotNull { doc ->
                try {
                    val skillIds = doc.get("skillIds") as? List<String> ?: emptyList()
                    val skills = skillIds.mapNotNull { skillsMap[it] }
                    
                    LearningResource(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        type = ResourceType.valueOf(doc.getString("type") ?: return@mapNotNull null),
                        provider = doc.getString("provider") ?: return@mapNotNull null,
                        duration = doc.getString("duration") ?: return@mapNotNull null,
                        cost = doc.getString("cost") ?: return@mapNotNull null,
                        rating = (doc.getDouble("rating") ?: 0.0).toFloat(),
                        url = doc.getString("url") ?: return@mapNotNull null,
                        skills = skills
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing learning resource document ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Fetched ${resources.size} learning resources from Firestore")
            Result.success(resources)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching learning resources from Firestore", e)
            Result.failure(e)
        }
    }
    
    suspend fun getRecommendationsForSkill(skill: Skill): Result<List<LearningResource>> {
        return try {
            val allResourcesResult = getAllLearningResources()
            if (allResourcesResult.isFailure) {
                return Result.failure(allResourcesResult.exceptionOrNull() ?: Exception("Failed to fetch learning resources"))
            }
            
            val resources = allResourcesResult.getOrNull()?.filter { resource ->
                resource.skills.any { it.id == skill.id }
            } ?: emptyList()
            
            Result.success(resources)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendations for skill ${skill.id}", e)
            Result.failure(e)
        }
    }
    
    suspend fun findSkillById(id: String): Result<Skill?> {
        return try {
            val allSkillsResult = getAllSkills()
            if (allSkillsResult.isFailure) {
                return Result.failure(allSkillsResult.exceptionOrNull() ?: Exception("Failed to fetch skills"))
            }
            
            val skill = allSkillsResult.getOrNull()?.find { it.id == id }
            Result.success(skill)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding skill by ID $id", e)
            Result.failure(e)
        }
    }
}
