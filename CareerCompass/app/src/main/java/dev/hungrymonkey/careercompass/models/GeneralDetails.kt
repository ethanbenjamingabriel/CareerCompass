package dev.hungrymonkey.careercompass.models

import java.util.Date

data class GeneralDetails(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val website1: String = "",
    val website2: String = "",
    val lastModifiedAt: Date = Date()
)

fun GeneralDetails.toFirestoreModel(): GeneralDetailsFirestore {
    return GeneralDetailsFirestore(
        fullName = fullName,
        email = email,
        phone = phone,
        website1 = website1,
        website2 = website2,
        lastModifiedAt = com.google.firebase.Timestamp(lastModifiedAt)
    )
}
