package dev.hungrymonkey.careercompass.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.toObject
import dev.hungrymonkey.careercompass.models.ResumeData
import dev.hungrymonkey.careercompass.models.ResumeFirestore
import dev.hungrymonkey.careercompass.models.CoverLetterData
import dev.hungrymonkey.careercompass.models.CoverLetterFirestore
import dev.hungrymonkey.careercompass.models.toFirestoreModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class CoverLetterViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    
    private val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel("gemini-2.5-flash")

    private val _coverLetters = MutableStateFlow<List<CoverLetterData>>(emptyList())
    val coverLetters: StateFlow<List<CoverLetterData>> = _coverLetters.asStateFlow()

    private val _resumes = MutableStateFlow<List<ResumeData>>(emptyList())
    val resumes: StateFlow<List<ResumeData>> = _resumes.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchCoverLetters()
        fetchResumes()
    }

    private fun fetchCoverLetters() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _coverLetters.value = emptyList()
            return
        }

        db.collection("users").document(userId).collection("coverLetters")
            .orderBy("lastModifiedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("CoverLetterViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val coverLetterList = snapshots?.mapNotNull { document ->
                    document.toObject<CoverLetterFirestore>().toUiModel(document.id)
                } ?: emptyList()
                _coverLetters.value = coverLetterList
            }
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
                    Log.w("CoverLetterViewModel", "Resume listen failed.", e)
                    return@addSnapshotListener
                }

                val resumeList = snapshots?.mapNotNull { document ->
                    document.toObject<ResumeFirestore>().toUiModel(document.id)
                } ?: emptyList()
                _resumes.value = resumeList
            }
    }

    fun generateCoverLetter(
        company: String,
        jobTitle: String,
        jobDescription: String,
        selectedResumeId: String,
        senderName: String = "",
        senderEmail: String = "",
        senderPhoneNumber: String = "",
        senderLinkedInUrl: String = "",
        onComplete: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid 
        if (userId == null) {
            _error.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            try {
                _isGenerating.value = true
                _error.value = null
                
                val selectedResume = _resumes.value.find { it.id == selectedResumeId }
                if (selectedResume == null) {
                    _error.value = "Selected resume not found. Please select a resume and try again."
                    _isGenerating.value = false
                    return@launch
                }
                
                val generatedContent = generateCoverLetterContent(
                    company, jobTitle, jobDescription, selectedResume, 
                    senderName, senderEmail, senderPhoneNumber, senderLinkedInUrl
                )
                
                _isGenerating.value = false
                onComplete(generatedContent)
                
            } catch (e: Exception) {
                Log.e("CoverLetterViewModel", "Error generating cover letter", e)
                _error.value = "Failed to generate cover letter: ${e.message}"
                _isGenerating.value = false
            }
        }
    }

    private suspend fun generateCoverLetterContent(
        company: String,
        jobTitle: String,
        jobDescription: String,
        resume: ResumeData,
        senderName: String = "",
        senderEmail: String = "",
        senderPhoneNumber: String = "",
        senderLinkedInUrl: String = ""
    ): String {
        return try {
            val resumeContext = buildResumeContext(resume)
            val contactInfo = buildContactInfo(senderName, senderEmail, senderPhoneNumber, senderLinkedInUrl)
            
            val prompt = """
                You are an expert cover letter writer. Generate a professional, personalized cover letter based on the following information:

                COMPANY: $company
                JOB TITLE: $jobTitle
                
                JOB DESCRIPTION:
                $jobDescription
                
                CANDIDATE CONTACT INFORMATION:
                $contactInfo
                
                CANDIDATE RESUME INFORMATION:
                $resumeContext
                
                Please generate a cover letter that:
                1. Is professional and engaging
                2. Highlights relevant experience from the resume that matches the job requirements
                3. Shows enthusiasm for the specific role and company
                4. Is approximately 3-4 paragraphs long
                5. Uses a professional but conversational tone
                6. Connects the candidate's experience to the job requirements
                7. If LinkedIn URL is provided, you may reference connecting on LinkedIn
                
                IMPORTANT FORMATTING REQUIREMENTS:
                - Format the response as plain text without any markdown or special formatting
                - Do NOT include any greeting at the beginning or any signature/closing at the end
                - START the cover letter body with something along the lines of: "I am [candidate name] and I am writing to express my interest in the [job title] position at [company name]."
                - Continue with the rest of the cover letter content
                - End with the final paragraph - do not add any closing statements or signatures
                
                Generate ONLY the body content of the cover letter.
            """.trimIndent()
            
            val inputContent = content {
                text(prompt)
            }
            
            val response = generativeModel.generateContent(inputContent)
            
            val responseText = response.text
            if (responseText.isNullOrBlank()) {
                throw Exception("AI returned empty response")
            }
            
            responseText.trim()
            
        } catch (e: Exception) {
            Log.e("CoverLetterViewModel", "Error generating AI content", e)
            throw Exception("Failed to generate cover letter content: ${e.message}")
        }
    }

    private fun buildResumeContext(resume: ResumeData): String {
        val context = StringBuilder()
        
        context.append("CANDIDATE INFORMATION:\n")
        context.append("Name: ${resume.fullName}\n")
        if (!resume.email.isNullOrBlank()) context.append("Email: ${resume.email}\n")
        if (!resume.phone.isNullOrBlank()) context.append("Phone: ${resume.phone}\n")
        if (!resume.website1.isNullOrBlank()) context.append("Website: ${resume.website1}\n")
        if (!resume.website2.isNullOrBlank()) context.append("Portfolio: ${resume.website2}\n")
        
        context.append("\nEDUCATION:\n")
        if (resume.education.isEmpty()) {
            context.append("No education information provided.\n")
        } else {
            resume.education.forEach { edu ->
                context.append("- ${edu.degree} from ${edu.institution}")
                if (!edu.major.isNullOrBlank()) context.append(" in ${edu.major}")
                if (edu.gpa != null) context.append(" (GPA: ${edu.gpa})")
                context.append(" (${edu.startDate}")
                if (edu.endDate != null) {
                    context.append(" to ${edu.endDate}")
                } else {
                    context.append(" to Present")
                }
                context.append(")")
                context.append("\n")
            }
        }
        
        context.append("\nWORK EXPERIENCE:\n")
        if (resume.experience.isEmpty()) {
            context.append("No work experience provided.\n")
        } else {
            resume.experience.forEach { exp ->
                context.append("- ${exp.title} at ${exp.company}")
                if (!exp.location.isNullOrBlank()) context.append(" (${exp.location})")
                context.append(" (${exp.startDate}")
                if (exp.endDate != null) {
                    context.append(" to ${exp.endDate}")
                } else {
                    context.append(" to Present")
                }
                context.append(")\n")
                
                if (exp.bullets.isNotEmpty()) {
                    exp.bullets.forEach { bullet ->
                        context.append("  • $bullet\n")
                    }
                }
            }
        }
        
        context.append("\nPROJECTS:\n")
        if (resume.projects.isEmpty()) {
            context.append("No projects provided.\n")
        } else {
            resume.projects.forEach { project ->
                context.append("- ${project.title}")
                if (project.stack.isNotBlank()) context.append(" (${project.stack})")
                context.append(" (${project.date})")
                context.append("\n")
                
                if (project.bullets.isNotEmpty()) {
                    project.bullets.forEach { bullet ->
                        context.append("  • $bullet\n")
                    }
                }
            }
        }
        
        context.append("\nTECHNICAL SKILLS:\n")
        val skillsList = mutableListOf<String>()
        if (resume.skills.languages.isNotBlank()) skillsList.add("Languages: ${resume.skills.languages}")
        if (resume.skills.frameworks.isNotBlank()) skillsList.add("Frameworks: ${resume.skills.frameworks}")
        if (resume.skills.tools.isNotBlank()) skillsList.add("Tools: ${resume.skills.tools}")
        if (resume.skills.libraries.isNotBlank()) skillsList.add("Libraries: ${resume.skills.libraries}")
        
        if (skillsList.isEmpty()) {
            context.append("No technical skills provided.\n")
        } else {
            skillsList.forEach { skill ->
                context.append("$skill\n")
            }
        }
        
        return context.toString()
    }
    
    private fun buildContactInfo(
        senderName: String,
        senderEmail: String,
        senderPhoneNumber: String,
        senderLinkedInUrl: String
    ): String {
        val contactInfo = StringBuilder()
        
        if (senderName.isNotBlank()) contactInfo.append("Name: $senderName\n")
        if (senderEmail.isNotBlank()) contactInfo.append("Email: $senderEmail\n")
        if (senderPhoneNumber.isNotBlank()) contactInfo.append("Phone: $senderPhoneNumber\n")
        if (senderLinkedInUrl.isNotBlank()) contactInfo.append("LinkedIn: $senderLinkedInUrl\n")
        
        return if (contactInfo.isNotEmpty()) {
            contactInfo.toString()
        } else {
            "No contact information provided.\n"
        }
    }

    fun saveCoverLetter(coverLetterData: CoverLetterData) {
        val userId = auth.currentUser?.uid ?: return
        val newCoverLetterRef = db.collection("users").document(userId).collection("coverLetters").document()
        val firestoreCoverLetter = coverLetterData.copy(id = newCoverLetterRef.id).toFirestoreModel()

        newCoverLetterRef.set(firestoreCoverLetter)
            .addOnSuccessListener { Log.d("CoverLetterViewModel", "Cover letter saved successfully!") }
            .addOnFailureListener { e -> Log.w("CoverLetterViewModel", "Error saving cover letter", e) }
    }

    suspend fun saveCoverLetterAsync(coverLetterData: CoverLetterData): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            if (isCoverLetterNameExists(coverLetterData.coverLetterName, excludeId = null)) {
                return Result.failure(Exception("A cover letter with this name already exists."))
            }
            
            val newCoverLetterRef = db.collection("users").document(userId).collection("coverLetters").document()
            val firestoreCoverLetter = coverLetterData.copy(id = newCoverLetterRef.id).toFirestoreModel()

            newCoverLetterRef.set(firestoreCoverLetter).await()
            Log.d("CoverLetterViewModel", "Cover letter saved successfully!")
            Result.success(newCoverLetterRef.id)
        } catch (e: Exception) {
            Log.w("CoverLetterViewModel", "Error saving cover letter", e)
            Result.failure(e)
        }
    }

    fun updateCoverLetter(coverLetterData: CoverLetterData) {
        val userId = auth.currentUser?.uid ?: return
        val firestoreCoverLetter = coverLetterData.copy(lastModifiedAt = Date()).toFirestoreModel()

        db.collection("users").document(userId).collection("coverLetters").document(coverLetterData.id)
            .set(firestoreCoverLetter, SetOptions.merge())
            .addOnSuccessListener { Log.d("CoverLetterViewModel", "Cover letter updated successfully!") }
            .addOnFailureListener { e -> Log.w("CoverLetterViewModel", "Error updating cover letter", e) }
    }

    suspend fun updateCoverLetterAsync(coverLetterData: CoverLetterData): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            if (isCoverLetterNameExists(coverLetterData.coverLetterName, excludeId = coverLetterData.id)) {
                return Result.failure(Exception("A cover letter with this name already exists."))
            }
            
            val firestoreCoverLetter = coverLetterData.copy(lastModifiedAt = Date()).toFirestoreModel()

            db.collection("users").document(userId).collection("coverLetters").document(coverLetterData.id)
                .set(firestoreCoverLetter, SetOptions.merge()).await()
            Log.d("CoverLetterViewModel", "Cover letter updated successfully!")
            Result.success(coverLetterData.id)
        } catch (e: Exception) {
            Log.w("CoverLetterViewModel", "Error updating cover letter", e)
            Result.failure(e)
        }
    }

    fun deleteCoverLetter(coverLetterId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("coverLetters").document(coverLetterId)
            .delete()
            .addOnSuccessListener { Log.d("CoverLetterViewModel", "Cover letter deleted successfully!") }
            .addOnFailureListener { e -> Log.w("CoverLetterViewModel", "Error deleting cover letter", e) }
    }

    fun clearError() {
        _error.value = null
    }
    
    suspend fun validateCoverLetterName(coverLetterName: String, excludeId: String?): Result<Unit> {
        return try {
            if (isCoverLetterNameExists(coverLetterName, excludeId)) {
                Result.failure(Exception("A cover letter with this name already exists."))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("CoverLetterViewModel", "Error validating cover letter name", e)
            Result.failure(Exception("Error checking cover letter name: ${e.message}"))
        }
    }
    
    private suspend fun isCoverLetterNameExists(coverLetterName: String, excludeId: String?): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val snapshot = db.collection("users").document(userId).collection("coverLetters")
                .whereEqualTo("coverLetterName", coverLetterName)
                .get()
                .await()
            
            if (excludeId == null) {
                return !snapshot.isEmpty
            }
            
            snapshot.documents.any { document -> document.id != excludeId }
        } catch (e: Exception) {
            Log.e("CoverLetterViewModel", "Error checking cover letter name uniqueness", e)
            false
        }
    }
} 