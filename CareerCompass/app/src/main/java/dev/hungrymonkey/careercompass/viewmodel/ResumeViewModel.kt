package dev.hungrymonkey.careercompass.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dev.hungrymonkey.careercompass.models.ResumeData
import dev.hungrymonkey.careercompass.models.ResumeFirestore
import dev.hungrymonkey.careercompass.models.toFirestoreModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class ResumeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _resumes = MutableStateFlow<List<ResumeData>>(emptyList())
    val resumes: StateFlow<List<ResumeData>> = _resumes.asStateFlow()

    init {
        fetchResumes()
    }

    private fun fetchResumes() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _resumes.value = emptyList()
            return
        }

        db.collection("users").document(userId).collection("resumes")
            .orderBy("lastModifiedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ResumeViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val resumeList = snapshots?.mapNotNull { document ->
                    document.toObject<ResumeFirestore>().toUiModel(document.id)
                } ?: emptyList()
                _resumes.value = resumeList
            }
    }

    suspend fun saveResume(resumeData: ResumeData): Result<String> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("User not authenticated"))
        
        if (isResumeNameExists(resumeData.resumeName, excludeId = null)) {
            return@withContext Result.failure(Exception("A resume with this name already exists."))
        }
        
        val newResumeRef = db.collection("users").document(userId).collection("resumes").document()
        val firestoreResume = resumeData.copy(id = newResumeRef.id).toFirestoreModel()

        try {
            newResumeRef.set(firestoreResume).await()
            Log.d("ResumeViewModel", "Resume saved successfully!")
            Result.success(newResumeRef.id)
        } catch (e: Exception) {
            Log.w("ResumeViewModel", "Error saving resume", e)
            Result.failure(e)
        }
    }

    suspend fun updateResume(resumeData: ResumeData): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("User not authenticated"))
        
        if (isResumeNameExists(resumeData.resumeName, excludeId = resumeData.id)) {
            return@withContext Result.failure(Exception("A resume with this name already exists."))
        }
        
        val firestoreResume = resumeData.copy(lastModifiedAt = Date()).toFirestoreModel()

        try {
            db.collection("users").document(userId).collection("resumes").document(resumeData.id)
                .set(firestoreResume, SetOptions.merge()).await()
            Log.d("ResumeViewModel", "Resume updated successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("ResumeViewModel", "Error updating resume", e)
            Result.failure(e)
        }
    }

    fun deleteResume(resumeId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("resumes").document(resumeId)
            .delete()
            .addOnSuccessListener { Log.d("ResumeViewModel", "Resume deleted successfully!") }
            .addOnFailureListener { e -> Log.w("ResumeViewModel", "Error deleting resume", e) }
    }
    
    private suspend fun isResumeNameExists(resumeName: String, excludeId: String?): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val snapshot = db.collection("users").document(userId).collection("resumes")
                .whereEqualTo("resumeName", resumeName)
                .get()
                .await()
            
            if (excludeId == null) {
                return !snapshot.isEmpty
            }
            
            snapshot.documents.any { document -> document.id != excludeId }
        } catch (e: Exception) {
            Log.e("ResumeViewModel", "Error checking resume name uniqueness", e)
            false
        }
    }
}