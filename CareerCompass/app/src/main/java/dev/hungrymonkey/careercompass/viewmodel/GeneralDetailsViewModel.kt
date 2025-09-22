package dev.hungrymonkey.careercompass.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dev.hungrymonkey.careercompass.models.GeneralDetails
import dev.hungrymonkey.careercompass.models.GeneralDetailsFirestore
import dev.hungrymonkey.careercompass.models.toFirestoreModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class GeneralDetailsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _generalDetails = MutableStateFlow<GeneralDetails?>(null)
    val generalDetails: StateFlow<GeneralDetails?> = _generalDetails.asStateFlow()

    init {
        fetchGeneralDetails()
    }

    private fun fetchGeneralDetails() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _generalDetails.value = null
            return
        }

        db.collection("users").document(userId).collection("generalDetails")
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("GeneralDetailsViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val generalDetail = snapshots?.documents?.firstOrNull()?.let { document ->
                    document.toObject<GeneralDetailsFirestore>()?.toUiModel(document.id)
                }
                _generalDetails.value = generalDetail
            }
    }

    fun saveGeneralDetails(generalDetails: GeneralDetails) {
        val userId = auth.currentUser?.uid ?: return
        val currentDetails = _generalDetails.value
        val firestoreDetails = generalDetails.copy(lastModifiedAt = Date()).toFirestoreModel()
        
        if (currentDetails != null && currentDetails.id.isNotEmpty()) {
            db.collection("users").document(userId).collection("generalDetails").document(currentDetails.id)
                .set(firestoreDetails, SetOptions.merge())
                .addOnSuccessListener { Log.d("GeneralDetailsViewModel", "General details updated successfully!") }
                .addOnFailureListener { e -> Log.w("GeneralDetailsViewModel", "Error updating general details", e) }
        } else {
            val newRef = db.collection("users").document(userId).collection("generalDetails").document()
            newRef.set(firestoreDetails)
                .addOnSuccessListener { Log.d("GeneralDetailsViewModel", "General details saved successfully!") }
                .addOnFailureListener { e -> Log.w("GeneralDetailsViewModel", "Error saving general details", e) }
        }
    }
}
