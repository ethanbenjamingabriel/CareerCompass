package dev.hungrymonkey.careercompass.viewmodel

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.hungrymonkey.careercompass.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SkillGapFirebaseRepository {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "SkillGapFirebaseRepo"
        private const val USERS_COLLECTION = "users"
        private const val SKILLS_SUBCOLLECTION = "skills"
        private const val CAREER_PROFILE_SUBCOLLECTION = "careerProfile"
    }
    
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }
    
    fun getUserSkillsFlow(): Flow<List<UserSkill>> = callbackFlow {
        val userId = getCurrentUserId()
        Log.d(TAG, "Setting up skills listener for user: $userId")
        
        val listener = db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(SKILLS_SUBCOLLECTION)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to skills", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val skills = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val firestoreSkill = doc.toObject(UserSkillFirestore::class.java)
                        firestoreSkill?.toUIModel()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting skill document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Loaded ${skills.size} skills from Firebase")
                trySend(skills)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getCareerProfileFlow(): Flow<CareerProfileFirestore?> = callbackFlow {
        val userId = getCurrentUserId()
        Log.d(TAG, "Setting up career profile listener for user: $userId")
        
        val listener = db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(CAREER_PROFILE_SUBCOLLECTION)
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to career profile", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val profile = try {
                    snapshot?.toObject(CareerProfileFirestore::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting career profile", e)
                    null
                }
                
                Log.d(TAG, "Loaded career profile: $profile")
                trySend(profile)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun saveUserSkill(userSkill: UserSkill): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            val firestoreSkill = userSkill.toFirestoreModel()
            
            Log.d(TAG, "Saving skill: ${userSkill.skill.name} for user: $userId")
            
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SKILLS_SUBCOLLECTION)
                .document(userSkill.skill.id)
                .set(firestoreSkill)
                .await()
            
            Log.d(TAG, "Successfully saved skill: ${userSkill.skill.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving skill", e)
            Result.failure(e)
        }
    }
    
    suspend fun removeUserSkill(skillId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            
            Log.d(TAG, "Removing skill: $skillId for user: $userId")
            
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SKILLS_SUBCOLLECTION)
                .document(skillId)
                .delete()
                .await()
            
            Log.d(TAG, "Successfully removed skill: $skillId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing skill", e)
            Result.failure(e)
        }
    }
    
    suspend fun saveCareerProfile(
        targetRole: JobRole?,
        overallMatchPercentage: Float? = null
    ): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            
            val profile = CareerProfileFirestore(
                targetRoleId = targetRole?.id,
                targetRoleTitle = targetRole?.title,
                lastAnalysisDate = if (overallMatchPercentage != null) System.currentTimeMillis() else null,
                overallMatchPercentage = overallMatchPercentage,
                updatedAt = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Saving career profile for user: $userId")
            
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CAREER_PROFILE_SUBCOLLECTION)
                .document("current")
                .set(profile)
                .await()
            
            Log.d(TAG, "Successfully saved career profile")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving career profile", e)
            Result.failure(e)
        }
    }

    suspend fun clearCareerProfile(): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            
            Log.d(TAG, "Clearing career profile for user: $userId")
            
            db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CAREER_PROFILE_SUBCOLLECTION)
                .document("current")
                .delete()
                .await()
            
            Log.d(TAG, "Successfully cleared career profile")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing career profile", e)
            Result.failure(e)
        }
    }
}
