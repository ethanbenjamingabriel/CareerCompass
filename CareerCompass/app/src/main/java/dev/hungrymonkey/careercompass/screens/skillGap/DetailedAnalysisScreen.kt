package dev.hungrymonkey.careercompass.screens.skillGap

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.*
import dev.hungrymonkey.careercompass.viewmodel.SkillGapViewModel
import dev.hungrymonkey.careercompass.viewmodel.CareerGoalsViewModel
import dev.hungrymonkey.careercompass.viewmodel.CareerGoalsViewModelFactory
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DetailedAnalysisScreen(
    navController: NavHostController
) {
    val viewModel: SkillGapViewModel = viewModel()
    val careerGoalsViewModel: CareerGoalsViewModel = viewModel(
        factory = CareerGoalsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
    val uiState by viewModel.uiState.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    
    Scaffold(
        topBar = { TopBar(title = "Detailed Analysis") },
        bottomBar = { NavigationBar(navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val currentAnalysisResult = analysisResult
            val currentTargetRole = uiState.selectedTargetRole
            
            if (currentAnalysisResult != null && currentTargetRole != null) {
                DetailedAnalysisContent(
                    analysisResult = currentAnalysisResult,
                    targetRole = currentTargetRole,
                    careerGoalsViewModel = careerGoalsViewModel,
                    onQuickAddSkill = { skillGap ->
                        val newGoal = Goal(
                            id = "", 
                            title = "Learn ${skillGap.skill.name}",
                            targetDate = getDefaultTargetDate(),
                            subtasks = emptyList(),
                            icon = "📚", 
                            priority = when(skillGap.importance) {
                                SkillImportance.CRITICAL -> "High"
                                SkillImportance.IMPORTANT -> "Medium" 
                                SkillImportance.NICE_TO_HAVE -> "Low"
                            },
                            reminderSettings = ReminderSettings(
                                isEnabled = true,
                                daysBefore = 30,
                                notificationId = 0
                            )
                        )
                        careerGoalsViewModel.addGoal(newGoal)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = when {
                                uiState.isAnalyzing -> "Analyzing your skills..."
                                !uiState.hasInitialDataLoaded -> "Loading your data..."
                                else -> "Generating detailed analysis..."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getDefaultTargetDate(): String {
    return LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
}

@Composable
private fun DetailedAnalysisContent(
    analysisResult: SkillAnalysisResult,
    targetRole: JobRole,
    careerGoalsViewModel: CareerGoalsViewModel,
    onQuickAddSkill: (SkillGap) -> Unit = {},
    modifier: Modifier = Modifier
) {

    val addedSkills = remember { mutableStateMapOf<String, Boolean>() }
    

    val existingGoals by careerGoalsViewModel.goals.collectAsState()
    
    fun skillAlreadyExists(skillName: String): Boolean {
        return existingGoals.any { goal -> 
            goal.title.equals("Learn $skillName", ignoreCase = true)
        }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Complete Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Comprehensive breakdown for ${targetRole.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Overall Match Score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { analysisResult.overallMatchPercentage / 100f },
                            modifier = Modifier.size(120.dp),
                            strokeWidth = 12.dp,
                            color = getMatchColor(analysisResult.overallMatchPercentage),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${analysisResult.overallMatchPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = getMatchColor(analysisResult.overallMatchPercentage)
                            )
                            Text(
                                text = "Match",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = getMatchDescription(analysisResult.overallMatchPercentage),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (analysisResult.skillGaps.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Skills to Develop (${analysisResult.skillGaps.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "Focus on these skills to improve your match for this role:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (analysisResult.skillGaps.size > 1) {
                            val skillsToAdd = analysisResult.skillGaps.filter { gap ->
                                !skillAlreadyExists(gap.skill.name) && addedSkills[gap.skill.id] != true
                            }
                            val allSkillsAdded = skillsToAdd.isEmpty()
                            
                            if (skillsToAdd.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        skillsToAdd.forEach { gap ->
                                            onQuickAddSkill(gap)
                                            addedSkills[gap.skill.id] = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add ${skillsToAdd.size} Skills to Goals")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("All Skills Added to Goals")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        analysisResult.skillGaps.forEach { gap ->
                            SkillGapItem(
                                skillGap = gap,
                                onQuickAdd = { 
                                    onQuickAddSkill(gap)
                                    addedSkills[gap.skill.id] = true
                                },
                                isAdded = addedSkills[gap.skill.id] == true,
                                alreadyExists = skillAlreadyExists(gap.skill.name)
                            )
                        }
                    }
                }
            }
        }
        
        if (analysisResult.strengths.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Your Strengths (${analysisResult.strengths.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "You're already strong in these areas:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(analysisResult.strengths) { strength ->
                                StrengthCard(userSkill = strength)
                            }
                        }
                    }
                }
            }
        }
        
        if (analysisResult.recommendations.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Recommended Learning",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "Here are some resources to help you develop missing skills:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        analysisResult.recommendations.take(5).forEach { resource ->
                            LearningResourceCard(resource = resource)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillGapItem(
    skillGap: SkillGap,
    onQuickAdd: () -> Unit = {},
    isAdded: Boolean = false,
    alreadyExists: Boolean = false
) {
    var showQuickAddSuccess by remember { mutableStateOf(false) }
    var wasJustAdded by remember { mutableStateOf(false) }
    
    LaunchedEffect(isAdded) {
        if (isAdded && !showQuickAddSuccess) {
            showQuickAddSuccess = true
            wasJustAdded = true
        }
    }
    
    LaunchedEffect(showQuickAddSuccess) {
        if (showQuickAddSuccess) {
            kotlinx.coroutines.delay(3000)
            showQuickAddSuccess = false
        }
    }
    
    val gapColor = when (skillGap.gapSeverity) {
        GapSeverity.CRITICAL -> Color(0xFFF44336)
        GapSeverity.HIGH -> Color(0xFFFF9800)
        GapSeverity.MEDIUM -> Color(0xFFFFC107)
        GapSeverity.LOW -> MaterialTheme.colorScheme.secondary
    }
    
    val gapIcon = when (skillGap.gapSeverity) {
        GapSeverity.CRITICAL -> Icons.Default.PriorityHigh
        GapSeverity.HIGH -> Icons.AutoMirrored.Filled.TrendingUp
        GapSeverity.MEDIUM -> Icons.Default.Timeline
        GapSeverity.LOW -> Icons.Default.Info
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = gapColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, gapColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        gapIcon,
                        contentDescription = null,
                        tint = gapColor,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Column {
                        Text(
                            text = skillGap.skill.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Required: ${skillGap.requiredLevel.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (skillGap.currentLevel != null) {
                            Text(
                                text = "Current: ${skillGap.currentLevel.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Badge(
                        containerColor = gapColor
                    ) {
                        Text(
                            text = skillGap.gapSeverity.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (skillGap.importance == SkillImportance.CRITICAL) {
                        Text(
                            text = "Critical",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = !alreadyExists && !isAdded && !showQuickAddSuccess && !wasJustAdded,
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Button(
                    onClick = {
                        onQuickAdd()
                        showQuickAddSuccess = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add to Goals",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            AnimatedVisibility(
                visible = alreadyExists || showQuickAddSuccess || isAdded || wasJustAdded,
                enter = slideInHorizontally() + fadeIn()
            ) {
                val isNewlyAdded = showQuickAddSuccess || (wasJustAdded && showQuickAddSuccess)
                val bannerColor = if (isNewlyAdded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                val backgroundColor = if (isNewlyAdded) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                val borderColor = if (isNewlyAdded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                val messageText = if (isNewlyAdded) "Added to Career Goals!" else "Already in Career Goals"
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = backgroundColor
                    ),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            messageText,
                            style = MaterialTheme.typography.labelLarge,
                            color = bannerColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthCard(userSkill: UserSkill) {
    val proficiencyColor = when (userSkill.proficiency) {
        ProficiencyLevel.BEGINNER -> MaterialTheme.colorScheme.tertiary
        ProficiencyLevel.INTERMEDIATE -> MaterialTheme.colorScheme.primary
        ProficiencyLevel.EXPERT -> Color(0xFF4CAF50)
    }
    
    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = proficiencyColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, proficiencyColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = userSkill.skill.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Badge(
                containerColor = proficiencyColor
            ) {
                Text(
                    text = userSkill.proficiency.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userSkill.yearsOfExperience > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${userSkill.yearsOfExperience}yr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (userSkill.hasCertification) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Certified",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LearningResourceCard(resource: LearningResource) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = resource.provider,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Badge(
                    containerColor = when (resource.type) {
                        ResourceType.COURSE -> Color(0xFF2196F3)
                        ResourceType.TUTORIAL -> Color(0xFF4CAF50)
                        ResourceType.DOCUMENTATION -> Color(0xFFFF9800)
                        ResourceType.BOOK -> Color(0xFF795548)
                        ResourceType.CERTIFICATION -> Color(0xFFE91E63)
                        ResourceType.BOOTCAMP -> Color(0xFF9C27B0)
                        ResourceType.WORKSHOP -> Color(0xFF607D8B)
                        ResourceType.PROJECT -> Color(0xFF00BCD4)
                    }
                ) {
                    Text(
                        text = resource.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = resource.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = resource.cost,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (resource.rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Text(
                            text = String.format("%.1f", resource.rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getMatchColor(percentage: Float): Color {
    return when {
        percentage >= 80 -> Color(0xFF4CAF50)
        percentage >= 60 -> MaterialTheme.colorScheme.primary
        percentage >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun getMatchDescription(percentage: Float): String {
    return when {
        percentage >= 80 -> "Excellent match! You're well-qualified for this role."
        percentage >= 60 -> "Good match! Focus on a few key areas to become highly qualified."
        percentage >= 40 -> "Moderate match. Some skill development needed."
        else -> "Significant skill gaps. Consider focused learning before applying."
    }
}
