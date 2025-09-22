package dev.hungrymonkey.careercompass.models

import java.util.Date

data class InterviewFeedback(
    val overallScore: Int = 0,
    val contentScore: Int = 0,
    val deliveryScore: Int = 0,
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val suggestedActions: List<String> = emptyList(),
    val starMethodUsage: String = "",
    val transcript: String = ""
)

data class PracticeSession(
    val id: String = "",
    val questionId: String = "",
    val userId: String = "",
    val feedback: InterviewFeedback = InterviewFeedback(),
    val completedAt: Date = Date()
)

data class InterviewQuestion(
    val id: String = "",
    val text: String = "",
    val category: String = "Technical",
    val difficulty: String = "Junior",
    val tags: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val createdBy: String? = null
)

data class BookmarkedQuestion(
    val id: String = "",
    val questionId: String = "",
    val userId: String = ""
)
