package dev.hungrymonkey.careercompass.models

import java.util.Date

data class CoverLetterData(
    val id: String,
    val coverLetterName: String,
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhoneNumber: String = "",
    val senderLinkedInUrl: String = "",
    val company: String,
    val position: String,
    val body: String,
    val jobDescription: String = "",
    val selectedResumeId: String? = null,
    val lastModifiedAt: Date = Date()
) 