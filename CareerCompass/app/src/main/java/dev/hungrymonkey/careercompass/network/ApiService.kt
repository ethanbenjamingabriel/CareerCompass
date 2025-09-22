package dev.hungrymonkey.careercompass.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    
    @POST("generate-resume")
    suspend fun generateResume(@Body request: GenerateResumeRequest): Response<ResponseBody>
    
    @POST("generate-cover-letter")
    suspend fun generateCoverLetter(@Body request: GenerateCoverLetterRequest): Response<ResponseBody>
}

data class GenerateResumeRequest(
    val user_id: String,
    val resume_id: String
)

data class GenerateCoverLetterRequest(
    val user_id: String,
    val cover_letter_id: String
)