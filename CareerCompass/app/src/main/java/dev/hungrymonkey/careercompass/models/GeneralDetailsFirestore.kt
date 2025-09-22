package dev.hungrymonkey.careercompass.models

import com.google.firebase.Timestamp

data class GeneralDetailsFirestore(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val website1: String = "",
    val website2: String = "",
    val lastModifiedAt: Timestamp = Timestamp.now()
) {
    fun toUiModel(id: String): GeneralDetails {
        return GeneralDetails(
            id = id,
            fullName = fullName,
            email = email,
            phone = phone,
            website1 = website1,
            website2 = website2,
            lastModifiedAt = lastModifiedAt.toDate()
        )
    }
}
