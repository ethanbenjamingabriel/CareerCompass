package dev.hungrymonkey.careercompass.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import dev.hungrymonkey.careercompass.models.BookmarkedQuestion
import dev.hungrymonkey.careercompass.models.InterviewFeedback
import dev.hungrymonkey.careercompass.models.InterviewQuestion
import dev.hungrymonkey.careercompass.models.PracticeSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import org.json.JSONObject

class InterviewPrepViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Firebase.firestore
    private val uid get() = Firebase.auth.currentUser?.uid
    
    private val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel("gemini-2.5-flash")

    private val _questions = MutableStateFlow<List<InterviewQuestion>>(emptyList())
    val questions: StateFlow<List<InterviewQuestion>> = _questions

    private val _bookmarkedQuestions = MutableStateFlow<List<BookmarkedQuestion>>(emptyList())
    val bookmarkedQuestions: StateFlow<List<BookmarkedQuestion>> = _bookmarkedQuestions

    private val _practiceSessions = MutableStateFlow<List<PracticeSession>>(emptyList())
    val practiceSessions: StateFlow<List<PracticeSession>> = _practiceSessions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadQuestions()
        loadBookmarkedQuestions()
        loadPracticeSessions()
    }

    fun loadQuestions() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                db.collection("interviewQuestions")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _error.value = error.message
                            _isLoading.value = false
                            return@addSnapshotListener
                        }
                        
                        val questions = snapshot?.documents?.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            InterviewQuestion(
                                id = doc.id,
                                text = data["text"] as? String ?: "",
                                category = data["category"] as? String ?: "Technical",
                                difficulty = data["difficulty"] as? String ?: "Junior",
                                tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                isCustom = data["isCustom"] as? Boolean ?: false,
                                createdBy = data["createdBy"] as? String
                            )
                        } ?: emptyList()
                        
                        _questions.value = questions
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadBookmarkedQuestions() {
        uid?.let { userId ->
            viewModelScope.launch {
                try {
                    db.collection("users")
                        .document(userId)
                        .collection("interviewQuestionBookmarks")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                _error.value = error.message
                                return@addSnapshotListener
                            }
                            
                            val bookmarks = snapshot?.documents?.mapNotNull { doc ->
                                val data = doc.data ?: return@mapNotNull null
                                BookmarkedQuestion(
                                    id = doc.id,
                                    questionId = data["questionId"] as? String ?: "",
                                    userId = userId
                                )
                            } ?: emptyList()
                            
                            _bookmarkedQuestions.value = bookmarks
                        }
                } catch (e: Exception) {
                    _error.value = e.message
                    _bookmarkedQuestions.value = emptyList()
                }
            }
        } ?: run {
            _bookmarkedQuestions.value = emptyList()
        }
    }

    fun loadPracticeSessions() {
        uid?.let { userId ->
            viewModelScope.launch {
                try {
                    db.collection("users")
                        .document(userId)
                        .collection("interviewQuestionAttempts")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                _error.value = error.message
                                return@addSnapshotListener
                            }
                            
                            val sessions = snapshot?.documents?.mapNotNull { doc ->
                                val data = doc.data ?: return@mapNotNull null
                                val feedbackData = data["feedback"] as? Map<String, Any> ?: emptyMap()
                                
                                PracticeSession(
                                    id = doc.id,
                                    questionId = data["questionId"] as? String ?: "",
                                    userId = userId,
                                    feedback = InterviewFeedback(
                                        overallScore = (feedbackData["overallScore"] as? Long)?.toInt() ?: 0,
                                        contentScore = (feedbackData["contentScore"] as? Long)?.toInt() ?: 0,
                                        deliveryScore = (feedbackData["deliveryScore"] as? Long)?.toInt() ?: 0,
                                        strengths = (feedbackData["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                        improvements = (feedbackData["improvements"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                        suggestedActions = (feedbackData["suggestedActions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                        starMethodUsage = feedbackData["starMethodUsage"] as? String ?: "",
                                        transcript = feedbackData["transcript"] as? String ?: ""
                                    ),
                                    completedAt = (data["completedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                                )
                            } ?: emptyList()
                            
                            _practiceSessions.value = sessions
                        }
                } catch (e: Exception) {
                    _error.value = e.message
                    _practiceSessions.value = emptyList()
                }
            }
        } ?: run {
            _practiceSessions.value = emptyList()
        }
    }

    fun toggleBookmark(questionId: String) {
        uid?.let { userId ->
            viewModelScope.launch {
                try {
                    val existingBookmark = _bookmarkedQuestions.value.find { it.questionId == questionId }
                    
                    if (existingBookmark != null) {
                        db.collection("users")
                            .document(userId)
                            .collection("interviewQuestionBookmarks")
                            .document(existingBookmark.id)
                            .delete()
                            .await()
                    } else {
                        val bookmark = mapOf(
                            "questionId" to questionId,
                            "userId" to userId
                        )
                        db.collection("users")
                            .document(userId)
                            .collection("interviewQuestionBookmarks")
                            .add(bookmark)
                            .await()
                    }
                } catch (e: Exception) {
                    _error.value = e.message
                }
            }
        }
    }

    fun analyzeAudioLocally(audioUri: Uri, questionId: String, onProcessingComplete: ((PracticeSession) -> Unit)? = null) {
        uid?.let { userId ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    
                    val questionText = getQuestionText(questionId)
                    val feedback = generateAIFeedbackLocally(audioUri, questionText)
                    val savedSession = savePracticeSession(questionId, userId, feedback)
                    
                    _isLoading.value = false
                    
                    savedSession?.let { session ->
                        onProcessingComplete?.invoke(session)
                    }
                } catch (e: Exception) {
                    println("Error in analyzeAudioLocally: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Failed to process audio: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }
    
    private suspend fun getQuestionText(questionId: String): String {
        return try {
            val doc = db.collection("interviewQuestions").document(questionId).get().await()
            doc.data?.get("text") as? String ?: ""
        } catch (e: Exception) {
            println("Error fetching question: ${e.message}")
            ""
        }
    }
    
    private suspend fun generateAIFeedbackLocally(audioUri: Uri, questionText: String): InterviewFeedback {
        val context = getApplication<Application>().applicationContext
        val inputStream = context.contentResolver.openInputStream(audioUri)
        val audioBytes = inputStream?.readBytes() ?: throw Exception("Could not read audio file")
        inputStream?.close()
        
        val prompt = """
            You are an expert interview coach. Analyze the audio response to this interview question and provide detailed feedback.
            
            Interview Question: "$questionText"
            
            Please listen to the audio response and provide feedback in the following JSON format (respond with ONLY the JSON, no other text. Do not use markdown or any other formatting syntax):
            {
              "contentScore": [score from 1-100],
              "deliveryScore": [score from 1-100],
              "strengths": ["strength 1", "strength 2", "strength 3"],
              "improvements": ["improvement 1", "improvement 2", "improvement 3"],
              "suggestedActions": ["action 1", "action 2", "action 3"],
              "starMethodUsage": "[Yes, clearly structured | Partially used | Not used | Not applicable]",
              "transcript": "[transcribed text of the response]"
            }
            
            Focus on:
            1. How well the response answers the question (contentScore)
            2. Communication clarity, pace, and delivery quality (deliveryScore)
            3. Specific strengths demonstrated in the response
            4. Areas that need improvement with actionable feedback
            5. Concrete next steps the candidate should take (suggestedActions)
            6. Use of the STAR method (Situation, Task, Action, Result) if applicable
            
            Provide constructive, actionable feedback that will help the candidate improve their interview performance.
        """.trimIndent()
        
        val inputContent = content {
            inlineData(audioBytes, "audio/m4a")
            text(prompt)
        }
        
        val response = generativeModel.generateContent(inputContent)
        val responseText = response.text ?: throw Exception("No response from AI")
        
        println("Raw AI response: $responseText")
        
        val cleanedResponse = responseText
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        val jsonObject = JSONObject(cleanedResponse)
        
        val contentScore = jsonObject.getInt("contentScore")
        val deliveryScore = jsonObject.getInt("deliveryScore")
        val overallScore = (contentScore + deliveryScore) / 2
        
        return InterviewFeedback(
            overallScore = overallScore,
            contentScore = contentScore,
            deliveryScore = deliveryScore,
            strengths = jsonObject.getJSONArray("strengths").let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            improvements = jsonObject.getJSONArray("improvements").let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            suggestedActions = jsonObject.getJSONArray("suggestedActions").let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            starMethodUsage = jsonObject.getString("starMethodUsage"),
            transcript = jsonObject.getString("transcript")
        )
    }

    private suspend fun savePracticeSession(questionId: String, userId: String, feedback: InterviewFeedback): PracticeSession? {
        return try {
            val session = mapOf(
                "questionId" to questionId,
                "userId" to userId,
                "feedback" to mapOf(
                    "overallScore" to feedback.overallScore,
                    "contentScore" to feedback.contentScore,
                    "deliveryScore" to feedback.deliveryScore,
                    "strengths" to feedback.strengths,
                    "improvements" to feedback.improvements,
                    "suggestedActions" to feedback.suggestedActions,
                    "starMethodUsage" to feedback.starMethodUsage,
                    "transcript" to feedback.transcript
                ),
                "completedAt" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = db.collection("users")
                .document(userId)
                .collection("interviewQuestionAttempts")
                .add(session)
                .await()
            
            PracticeSession(
                id = docRef.id,
                questionId = questionId,
                userId = userId,
                feedback = feedback,
                completedAt = Date()
            )
        } catch (e: Exception) {
            _error.value = e.message
            null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun saveCustomQuestion(
        text: String,
        category: String,
        difficulty: String,
        tags: List<String>,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        uid?.let { userId ->
            viewModelScope.launch {
                try {
                    val existingQuestions = _questions.value
                    val isDuplicate = existingQuestions.any { existingQuestion ->
                        existingQuestion.text.trim().lowercase() == text.trim().lowercase()
                    }
                    
                    if (isDuplicate) {
                        val errorMessage = "This question already exists. Please enter a different question."
                        _error.value = errorMessage
                        onFailure(errorMessage)
                        return@launch
                    }
                    
                    val questionData = mapOf(
                        "text" to text,
                        "category" to category,
                        "difficulty" to difficulty,
                        "tags" to tags,
                        "createdBy" to userId,
                        "isCustom" to true,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    
                    db.collection("interviewQuestions")
                        .add(questionData)
                        .await()
                    
                    onSuccess()
                } catch (e: Exception) {
                    val errorMessage = "Failed to save question: ${e.message}"
                    _error.value = errorMessage
                    onFailure(errorMessage)
                }
            }
        } ?: run {
            val errorMessage = "User not authenticated"
            _error.value = errorMessage
            onFailure(errorMessage)
        }
    }

    fun cleanupOrphanedSessions() {
        uid?.let { userId ->
            viewModelScope.launch {
                try {
                    val validQuestionIds = _questions.value.map { it.id }.toSet()
                    val allSessions = _practiceSessions.value
                    
                    val orphanedSessions = allSessions.filter { session ->
                        session.questionId !in validQuestionIds
                    }
                    
                    orphanedSessions.forEach { session ->
                        try {
                            db.collection("users")
                                .document(userId)
                                .collection("interviewQuestionAttempts")
                                .document(session.id)
                                .delete()
                                .await()
                        } catch (e: Exception) {
                            println("Failed to delete orphaned session ${session.id}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    _error.value = "Failed to cleanup sessions: ${e.message}"
                }
            }
        }
    }
}
