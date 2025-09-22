package dev.hungrymonkey.careercompass.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.hungrymonkey.careercompass.models.Goal
import dev.hungrymonkey.careercompass.models.GoalFirestore
import dev.hungrymonkey.careercompass.models.toFirestoreModel
import dev.hungrymonkey.careercompass.notifications.GoalNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class CareerGoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val db = Firebase.firestore
    private val uid get() = Firebase.auth.currentUser?.uid
    private val notificationManager = GoalNotificationManager(context)

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals

    init {
        loadGoals()
    }

    private fun loadGoals() {
        uid?.let { userId ->
            db.collection("users")
                .document(userId)
                .collection("goals")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    val uiGoals = mutableListOf<Goal>()
                    snap.documents.forEach { doc ->
                        val gf = doc.toObject(GoalFirestore::class.java) ?: return@forEach
                        val goal = gf.toUiModel(doc.id)
                        uiGoals.add(goal)
                    }
                    _goals.value = uiGoals
                }
        }
    }

    fun addGoal(goal: Goal) {
        uid?.let { userId ->
            viewModelScope.launch {
                db.collection("users")
                  .document(userId)
                  .collection("goals")
                  .add(goal.toFirestoreModel())
                  .addOnSuccessListener { documentReference ->
                      val goalWithId = goal.copy(id = documentReference.id)
                      if (goalWithId.reminderSettings.isEnabled) {
                          notificationManager.scheduleGoalReminder(goalWithId)
                      }
                  }
                  .addOnFailureListener { e ->
                  }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        uid?.let { userId ->
            viewModelScope.launch {
                val goalToDelete = _goals.value.find { it.id == goalId }
                goalToDelete?.let { goal ->
                    if (goal.reminderSettings.isEnabled) {
                        notificationManager.cancelGoalReminder(goal.reminderSettings.notificationId)
                    }
                }
                
                db.collection("users")
                    .document(userId)
                    .collection("goals")
                    .document(goalId)
                    .delete()
            }
        }
    }

    fun updateGoal(goal: Goal) {
        val userId = uid ?: return
        val docRef = db.collection("users")
                    .document(userId)
                    .collection("goals")
                    .document(goal.id)

        val data = goal.toFirestoreModel()

        viewModelScope.launch {
            docRef.set(data, SetOptions.merge())
                .addOnSuccessListener {
                    if (goal.reminderSettings.isEnabled) {
                        notificationManager.scheduleGoalReminder(goal)
                    } else {
                        notificationManager.cancelGoalReminder(goal.reminderSettings.notificationId)
                    }
                }
        }
    }

}