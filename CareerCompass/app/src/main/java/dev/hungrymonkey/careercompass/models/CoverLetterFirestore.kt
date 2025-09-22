package dev.hungrymonkey.careercompass.models

import com.google.firebase.Timestamp

data class CoverLetterFirestore(
    val coverLetterName: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhoneNumber: String = "",
    val senderLinkedInUrl: String = "",
    val company: String = "",
    val position: String = "",
    val body: String = "",
    val jobDescription: String = "",
    val selectedResumeId: String? = null,
    val lastModifiedAt: Timestamp = Timestamp.now()
) {
    fun toUiModel(id: String): CoverLetterData {
        return CoverLetterData(
            id = id,
            coverLetterName = coverLetterName,
            senderName = senderName,
            senderEmail = senderEmail,
            senderPhoneNumber = senderPhoneNumber,
            senderLinkedInUrl = senderLinkedInUrl,
            company = company,
            position = position,
            body = body,
            jobDescription = jobDescription,
            selectedResumeId = selectedResumeId,
            lastModifiedAt = lastModifiedAt.toDate()
        )
    }
}

fun CoverLetterData.toFirestoreModel(): Map<String, Any?> = mapOf(
    "coverLetterName" to coverLetterName,
    "senderName" to senderName,
    "senderEmail" to senderEmail,
    "senderPhoneNumber" to senderPhoneNumber,
    "senderLinkedInUrl" to senderLinkedInUrl,
    "company" to company,
    "position" to position,
    "body" to body,
    "jobDescription" to jobDescription,
    "selectedResumeId" to selectedResumeId,
    "lastModifiedAt" to Timestamp(lastModifiedAt)
) 