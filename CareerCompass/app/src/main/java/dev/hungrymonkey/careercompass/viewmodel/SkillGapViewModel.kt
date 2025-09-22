package dev.hungrymonkey.careercompass.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dev.hungrymonkey.careercompass.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SkillGapViewModel : ViewModel() {
    
    private val firebaseRepository = SkillGapFirebaseRepository()
    private val firestoreRepository = SkillGapFirestoreRepository(FirebaseFirestore.getInstance())
    
    private val _uiState = MutableStateFlow(SkillGapUiState())
    val uiState: StateFlow<SkillGapUiState> = _uiState.asStateFlow()
    
    private val _userSkills = MutableStateFlow<List<UserSkill>>(emptyList())
    val userSkills: StateFlow<List<UserSkill>> = _userSkills.asStateFlow()
    
    private val _analysisResult = MutableStateFlow<SkillAnalysisResult?>(null)
    val analysisResult: StateFlow<SkillAnalysisResult?> = _analysisResult.asStateFlow()
    
    companion object {
        private const val TAG = "SkillGapViewModel"
    }
    
    init {
        loadInitialData()
        setupFirebaseListeners()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting initial data load...")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                Log.d(TAG, "Fetching skills from Firestore...")
                val skillsResult = firestoreRepository.getAllSkills()
                
                Log.d(TAG, "Fetching job roles from Firestore...")
                val jobRolesResult = firestoreRepository.getAllJobRoles()
                
                val skills = skillsResult.getOrElse { 
                    Log.e(TAG, "Failed to load skills from Firestore", it)
                    emptyList()
                }
                val jobRoles = jobRolesResult.getOrElse { 
                    Log.e(TAG, "Failed to load job roles from Firestore", it)
                    emptyList()
                }
                
                Log.d(TAG, "Loaded ${skills.size} skills and ${jobRoles.size} job roles from Firestore")
                
                _uiState.value = _uiState.value.copy(
                    availableSkills = skills,
                    jobRoles = jobRoles,
                    isLoading = false,
                    hasInitialDataLoaded = true
                )
                
                Log.d(TAG, "Initial data load completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                _uiState.value = _uiState.value.copy(
                    availableSkills = emptyList(),
                    jobRoles = emptyList(),
                    isLoading = false,
                    hasInitialDataLoaded = true,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }
    
    private fun setupFirebaseListeners() {
        viewModelScope.launch {
            try {
                firebaseRepository.getUserSkillsFlow().collect { skills ->
                    Log.d(TAG, "Received ${skills.size} skills from Firebase")
                    _userSkills.value = skills
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasInitialDataLoaded = true
                    )
                    
                    if (skills.isNotEmpty() && _uiState.value.selectedTargetRole != null) {
                        Log.d(TAG, "Auto-triggering analysis: ${skills.size} skills + target role")
                        performAnalysis()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up skills listener", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load skills: ${e.message}",
                    isLoading = false
                )
            }
        }
        
        viewModelScope.launch {
            try {
                firebaseRepository.getCareerProfileFlow().collect { profile ->
                    Log.d(TAG, "Received career profile from Firebase: $profile")
                    
                    profile?.targetRoleId?.let { roleId ->
                        firestoreRepository.getAllJobRoles().fold(
                            onSuccess = { allJobRoles ->
                                val targetRole = allJobRoles.find { it.id == roleId }
                                if (targetRole != null && targetRole != _uiState.value.selectedTargetRole) {
                                    Log.d(TAG, "Restoring target role from Firebase: ${targetRole.title}")
                                    _uiState.value = _uiState.value.copy(
                                        selectedTargetRole = targetRole,
                                        currentStep = AnalysisStep.SKILL_INPUT
                                    )
                                    
                                    if (_userSkills.value.isNotEmpty()) {
                                        Log.d(TAG, "Auto-triggering analysis: restored role + existing skills")
                                        performAnalysis()
                                    }
                                }
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Failed to get job roles for target role restore", error)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up career profile listener", e)
            }
        }
    }
    
    fun selectTargetRole(role: JobRole) {
        viewModelScope.launch {
            Log.d(TAG, "Selecting target role: ${role.title}")
            
            _uiState.value = _uiState.value.copy(
                selectedTargetRole = role,
                currentStep = AnalysisStep.SKILL_INPUT
            )
            
            val hasSkills = _userSkills.value.isNotEmpty()
            if (hasSkills) {
                Log.d(TAG, "Target role selected with ${_userSkills.value.size} existing skills - triggering analysis immediately")
                _uiState.value = _uiState.value.copy(isAnalyzing = true)
                performAnalysis()
            } else {
                Log.d(TAG, "Target role selected but no skills yet")
            }
            
            launch {
                firebaseRepository.saveCareerProfile(role).fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully saved target role to Firebase")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to save target role to Firebase", error)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to save target role: ${error.message}"
                        )
                    }
                )
            }
        }
    }
    
    fun clearTargetRole() {
        viewModelScope.launch {
            Log.d(TAG, "Clearing target role")
            
            _uiState.value = _uiState.value.copy(
                selectedTargetRole = null,
                currentStep = AnalysisStep.ROLE_SELECTION
            )
            
            _analysisResult.value = null
            
            launch {
                firebaseRepository.clearCareerProfile().fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully cleared target role from Firebase")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to clear target role from Firebase", error)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to clear target role: ${error.message}"
                        )
                    }
                )
            }
        }
    }
    
    fun addUserSkill(skill: Skill, proficiency: ProficiencyLevel, hasExperience: Boolean = false, hasCertification: Boolean = false, yearsOfExperience: Int = 0) {
        viewModelScope.launch {
            val userSkill = UserSkill(
                skill = skill,
                proficiency = proficiency,
                hasExperience = hasExperience,
                hasCertification = hasCertification,
                yearsOfExperience = yearsOfExperience
            )
            
            Log.d(TAG, "Adding skill: ${skill.name}")
            
            val currentSkills = _userSkills.value.toMutableList()
            val existingIndex = currentSkills.indexOfFirst { it.skill.id == skill.id }
            if (existingIndex >= 0) {
                currentSkills[existingIndex] = userSkill
            } else {
                currentSkills.add(userSkill)
            }
            _userSkills.value = currentSkills
            
            if (_uiState.value.selectedTargetRole != null) {
                Log.d(TAG, "Skill added with target role - triggering analysis immediately")
                _uiState.value = _uiState.value.copy(isAnalyzing = true)
                performAnalysis()
            }
            
            launch {
                firebaseRepository.saveUserSkill(userSkill).fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully added skill to Firebase: ${skill.name}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to add skill to Firebase", error)
                        val revertedSkills = _userSkills.value.toMutableList()
                        if (existingIndex >= 0) {
                            currentSkills.removeAt(currentSkills.size - 1)
                        } else {
                            revertedSkills.removeAll { it.skill.id == skill.id }
                        }
                        _userSkills.value = revertedSkills
                        
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to add skill: ${error.message}"
                        )
                    }
                )
            }
        }
    }
    
    fun updateUserSkill(skill: Skill, proficiency: ProficiencyLevel, hasExperience: Boolean = false, hasCertification: Boolean = false, yearsOfExperience: Int = 0) {
        viewModelScope.launch {
            val userSkill = UserSkill(
                skill = skill,
                proficiency = proficiency,
                hasExperience = hasExperience,
                hasCertification = hasCertification,
                yearsOfExperience = yearsOfExperience
            )
            
            Log.d(TAG, "Updating skill: ${skill.name}")
            
            val currentSkills = _userSkills.value.toMutableList()
            val existingIndex = currentSkills.indexOfFirst { it.skill.id == skill.id }
            val previousSkill = if (existingIndex >= 0) currentSkills[existingIndex] else null
            
            if (existingIndex >= 0) {
                currentSkills[existingIndex] = userSkill
            } else {
                currentSkills.add(userSkill)
            }
            _userSkills.value = currentSkills
            
            if (_uiState.value.selectedTargetRole != null) {
                Log.d(TAG, "Skill updated with target role - triggering analysis immediately")
                _uiState.value = _uiState.value.copy(isAnalyzing = true)
                performAnalysis()
            }
            
            launch {
                firebaseRepository.saveUserSkill(userSkill).fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully updated skill in Firebase: ${skill.name}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to update skill in Firebase", error)
                        val revertedSkills = _userSkills.value.toMutableList()
                        if (previousSkill != null) {
                            revertedSkills[existingIndex] = previousSkill
                        } else {
                            revertedSkills.removeAll { it.skill.id == skill.id }
                        }
                        _userSkills.value = revertedSkills
                        
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update skill: ${error.message}"
                        )
                    }
                )
            }
        }
    }
    
    fun removeUserSkill(skill: Skill) {
        viewModelScope.launch {
            Log.d(TAG, "Removing skill: ${skill.name}")
            
            val currentSkills = _userSkills.value.toMutableList()
            val existingIndex = currentSkills.indexOfFirst { it.skill.id == skill.id }
            val removedSkill = if (existingIndex >= 0) currentSkills[existingIndex] else null
            
            if (existingIndex >= 0) {
                currentSkills.removeAt(existingIndex)
                _userSkills.value = currentSkills
                
                if (_uiState.value.selectedTargetRole != null) {
                    Log.d(TAG, "Skill removed with target role - triggering analysis immediately")
                    _uiState.value = _uiState.value.copy(isAnalyzing = true)
                    performAnalysis()
                }
            }
            
            launch {
                firebaseRepository.removeUserSkill(skill.id).fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully removed skill from Firebase: ${skill.name}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to remove skill from Firebase", error)
                        if (removedSkill != null) {
                            val revertedSkills = _userSkills.value.toMutableList()
                            revertedSkills.add(existingIndex, removedSkill)
                            _userSkills.value = revertedSkills
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to remove skill: ${error.message}"
                        )
                    }
                )
            }
        }
    }
    
    private fun performAnalysis() {
        val targetRole = _uiState.value.selectedTargetRole
        val currentSkills = _userSkills.value
        
        if (targetRole == null) {
            Log.w(TAG, "Cannot perform analysis without target role")
            return
        }
        
        if (currentSkills.isEmpty()) {
            Log.w(TAG, "Cannot perform analysis without skills")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "Performing analysis for ${targetRole.title} with ${currentSkills.size} skills")
            
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            
            try {
                val analysis = analyzeSkillGap(targetRole, currentSkills)
                _analysisResult.value = analysis
                
                Log.d(TAG, "Analysis completed: ${analysis.overallMatchPercentage}% match")
                
                firebaseRepository.saveCareerProfile(
                    targetRole = targetRole,
                    overallMatchPercentage = analysis.overallMatchPercentage
                ).fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully saved analysis result to Firebase")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to save analysis result to Firebase", error)
                    }
                )
                
                _uiState.value = _uiState.value.copy(
                    currentStep = AnalysisStep.RESULTS,
                    isAnalyzing = false
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error performing analysis", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = "Analysis failed: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun analyzeSkillGap(targetRole: JobRole, userSkills: List<UserSkill>): SkillAnalysisResult {
        val userSkillMap = userSkills.associateBy { it.skill.id }
        val skillGaps = mutableListOf<SkillGap>()
        val strengths = mutableListOf<UserSkill>()
        
        var totalRequiredSkills = 0
        var metRequirements = 0
        
        targetRole.requiredSkills.forEach { requiredSkill ->
            totalRequiredSkills++
            val userSkill = userSkillMap[requiredSkill.skill.id]
            
            if (userSkill != null) {
                if (userSkill.proficiency.value >= requiredSkill.minimumProficiency.value) {
                    metRequirements++
                    strengths.add(userSkill)
                } else {
                    val gap = SkillGap(
                        skill = requiredSkill.skill,
                        currentLevel = userSkill.proficiency,
                        requiredLevel = requiredSkill.minimumProficiency,
                        importance = requiredSkill.importance,
                        gapSeverity = calculateGapSeverity(
                            userSkill.proficiency,
                            requiredSkill.minimumProficiency,
                            requiredSkill.importance
                        )
                    )
                    skillGaps.add(gap)
                }
            } else {
                val gap = SkillGap(
                    skill = requiredSkill.skill,
                    currentLevel = null,
                    requiredLevel = requiredSkill.minimumProficiency,
                    importance = requiredSkill.importance,
                    gapSeverity = calculateGapSeverity(
                        null,
                        requiredSkill.minimumProficiency,
                        requiredSkill.importance
                    )
                )
                skillGaps.add(gap)
            }
        }
        
        val overallMatchPercentage = if (totalRequiredSkills > 0) {
            (metRequirements.toFloat() / totalRequiredSkills) * 100
        } else 0f
        
        val recommendations = skillGaps.take(5).flatMap { gap ->
            runCatching { 
                firestoreRepository.getRecommendationsForSkill(gap.skill).getOrElse { emptyList() }
            }.getOrElse { 
                Log.w(TAG, "Failed to get recommendations for skill ${gap.skill.id}")
                emptyList() 
            }
        }.distinctBy { it.id }
        
        return SkillAnalysisResult(
            targetRole = targetRole,
            overallMatchPercentage = overallMatchPercentage,
            skillGaps = skillGaps.sortedBy { it.gapSeverity },
            strengths = strengths,
            recommendations = recommendations
        )
    }
    
    private fun calculateGapSeverity(
        currentLevel: ProficiencyLevel?,
        requiredLevel: ProficiencyLevel,
        importance: SkillImportance
    ): GapSeverity {
        val gap = requiredLevel.value - (currentLevel?.value ?: 0)
        
        return when {
            importance == SkillImportance.CRITICAL && gap >= 3 -> GapSeverity.CRITICAL
            importance == SkillImportance.CRITICAL && gap >= 2 -> GapSeverity.HIGH
            importance == SkillImportance.IMPORTANT && gap >= 3 -> GapSeverity.HIGH
            importance == SkillImportance.IMPORTANT && gap >= 2 -> GapSeverity.MEDIUM
            gap >= 2 -> GapSeverity.MEDIUM
            else -> GapSeverity.LOW
        }
    }
    
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun reloadData() {
        Log.d(TAG, "Reloading data requested")
        loadInitialData()
    }
}

data class SkillGapUiState(
    val isLoading: Boolean = true,
    val isAnalyzing: Boolean = false,
    val hasInitialDataLoaded: Boolean = false,
    val currentStep: AnalysisStep = AnalysisStep.ROLE_SELECTION,
    val selectedTargetRole: JobRole? = null,
    val availableSkills: List<Skill> = emptyList(),
    val jobRoles: List<JobRole> = emptyList(),
    val error: String? = null
)

enum class AnalysisStep {
    ROLE_SELECTION,
    SKILL_INPUT,
    RESULTS
}
