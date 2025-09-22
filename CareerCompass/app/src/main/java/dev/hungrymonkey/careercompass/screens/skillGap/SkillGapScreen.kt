package dev.hungrymonkey.careercompass.screens.skillGap

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.*
import dev.hungrymonkey.careercompass.viewmodel.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SkillGapScreen(navController: NavHostController) {
    val viewModel: SkillGapViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val userSkills by viewModel.userSkills.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    var showSkillBottomSheet by remember { mutableStateOf(false) }
    var editingSkill by remember { mutableStateOf<UserSkill?>(null) }
    Scaffold(
        topBar = { TopBar(title = "Skill Gap Analysis") },
        bottomBar = { NavigationBar(navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading || !uiState.hasInitialDataLoaded -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { 
                            viewModel.clearError()
                            viewModel.reloadData()
                        }
                    )
                }
                else -> {
                    MainAnalysisScreen(
                        uiState = uiState,
                        userSkills = userSkills,
                        analysisResult = analysisResult,
                        onRoleSelected = { role ->
                            viewModel.selectTargetRole(role)
                        },
                        onRoleCleared = {
                            viewModel.clearTargetRole()
                        },
                        onAddSkillsClick = { showSkillBottomSheet = true },
                        onSkillRemoved = { viewModel.removeUserSkill(it.skill) },
                        onSkillEdit = { skill ->
                            editingSkill = skill
                            showSkillBottomSheet = true
                        },
                        onViewDetailedAnalysis = { 
                            navController.navigate("detailed_analysis")
                        }
                    )
                }
            }
            
            if (showSkillBottomSheet) {
                SkillSelectionBottomSheet(
                    availableSkills = uiState.availableSkills,
                    userSkills = userSkills,
                    editingSkill = editingSkill,
                    onSkillAdded = { skill, proficiency, hasExp, hasCert, years ->
                        if (editingSkill != null) {
                            viewModel.updateUserSkill(skill, proficiency, hasExp, hasCert, years)
                        } else {
                            viewModel.addUserSkill(skill, proficiency, hasExp, hasCert, years)
                        }
                        editingSkill = null
                    },
                    onDismiss = { 
                        showSkillBottomSheet = false
                        editingSkill = null
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    SkillGapSkeletonLoading()
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun MainAnalysisScreen(
    uiState: SkillGapUiState,
    userSkills: List<UserSkill>,
    analysisResult: SkillAnalysisResult?,
    onRoleSelected: (JobRole) -> Unit,
    onRoleCleared: () -> Unit,
    onAddSkillsClick: () -> Unit,
    onSkillRemoved: (UserSkill) -> Unit,
    onSkillEdit: (UserSkill) -> Unit,
    onViewDetailedAnalysis: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Skill Gap Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select your target role and manage your skills to see how you match up",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        item {
            TargetRoleSelectionSection(
                jobRoles = uiState.jobRoles,
                selectedRole = uiState.selectedTargetRole,
                onRoleSelected = onRoleSelected,
                onRoleCleared = onRoleCleared
            )
        }
        
        item {
            SkillsManagementSection(
                userSkills = userSkills,
                onAddSkillsClick = onAddSkillsClick,
                onSkillRemoved = onSkillRemoved,
                onSkillEdit = onSkillEdit
            )
        }
        
        item {
            analysisResult?.let { result ->
                AnalysisResultsSection(
                    analysisResult = result,
                    targetRole = uiState.selectedTargetRole!!,
                    onViewDetailedAnalysis = onViewDetailedAnalysis
                )
            } ?: run {
                if (uiState.selectedTargetRole != null || userSkills.isNotEmpty()) {
                    AnalysisSkeletonLoading()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetRoleSelectionSection(
    jobRoles: List<JobRole>,
    selectedRole: JobRole?,
    onRoleSelected: (JobRole) -> Unit,
    onRoleCleared: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Work,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Target Role",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (selectedRole != null) {
                    OutlinedButton(
                        onClick = onRoleCleared,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "Clear",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = selectedRole?.title ?: "Select a target role",
                    onValueChange = {},
                    label = { Text("Choose your career goal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    jobRoles.forEach { role ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = role.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = role.averageSalary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onRoleSelected(role)
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.WorkOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            selectedRole?.let { role ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = role.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillsManagementSection(
    userSkills: List<UserSkill>,
    onAddSkillsClick: () -> Unit,
    onSkillRemoved: (UserSkill) -> Unit,
    onSkillEdit: (UserSkill) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Your Skills",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (userSkills.isNotEmpty()) {
                    Badge {
                        Text(userSkills.size.toString())
                    }
                }
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 0.dp)
            ) {
                item {
                    AddSkillButton(onClick = onAddSkillsClick)
                }
                
                items(userSkills) { userSkill ->
                    SkillChip(
                        userSkill = userSkill,
                        onRemove = { onSkillRemoved(userSkill) },
                        onEdit = { onSkillEdit(userSkill) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSkillButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Skills")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillChip(
    userSkill: UserSkill,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    val proficiencyColor = when (userSkill.proficiency) {
        ProficiencyLevel.BEGINNER -> MaterialTheme.colorScheme.tertiary
        ProficiencyLevel.INTERMEDIATE -> MaterialTheme.colorScheme.primary
        ProficiencyLevel.EXPERT -> Color(0xFF4CAF50)
    }
    
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(
            containerColor = proficiencyColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, proficiencyColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = userSkill.skill.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove skill",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AnalysisResultsSection(
    analysisResult: SkillAnalysisResult,
    targetRole: JobRole,
    onViewDetailedAnalysis: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Analysis Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your match for ${targetRole.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            val animatedProgress by animateFloatAsState(
                targetValue = analysisResult.overallMatchPercentage / 100f,
                animationSpec = tween(durationMillis = 1000),
                label = "progress_animation"
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 8.dp,
                            color = getMatchColor(analysisResult.overallMatchPercentage)
                        )
                        Text(
                            text = "${analysisResult.overallMatchPercentage.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Overall Match",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatItem(
                        icon = Icons.Default.CheckCircle,
                        value = analysisResult.strengths.size.toString(),
                        label = "Strengths",
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        value = analysisResult.skillGaps.size.toString(),
                        label = "Skills to Develop",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    StatItem(
                        icon = Icons.Default.School,
                        value = analysisResult.recommendations.size.toString(),
                        label = "Recommendations",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            if (analysisResult.skillGaps.isNotEmpty() || analysisResult.strengths.isNotEmpty()) {
                Button(
                    onClick = onViewDetailedAnalysis,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "View Detailed Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillSelectionBottomSheet(
    availableSkills: List<Skill>,
    userSkills: List<UserSkill>,
    editingSkill: UserSkill? = null,
    onSkillAdded: (Skill, ProficiencyLevel, Boolean, Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var selectedSkill by remember(editingSkill) { 
        mutableStateOf(editingSkill?.skill)
    }
    var selectedProficiency by remember(editingSkill) { 
        mutableStateOf(editingSkill?.proficiency ?: ProficiencyLevel.BEGINNER) 
    }
    var hasExperience by remember(editingSkill) { 
        mutableStateOf(editingSkill?.hasExperience ?: false) 
    }
    var hasCertification by remember(editingSkill) { 
        mutableStateOf(editingSkill?.hasCertification ?: false) 
    }
    var yearsOfExperience by remember(editingSkill) {
        mutableIntStateOf(editingSkill?.yearsOfExperience ?: 0)
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SkillCategory?>(null) }
    
    val filteredSkills = remember(searchQuery, selectedCategory, userSkills, editingSkill) {
        availableSkills.filter { skill ->
            val userSkillIds = userSkills.map { it.skill.id }
            val isEditingThisSkill = editingSkill?.skill?.id == skill.id
            (!userSkillIds.contains(skill.id) || isEditingThisSkill) &&
            (selectedCategory == null || skill.category == selectedCategory) &&
            (searchQuery.isEmpty() || skill.name.contains(searchQuery, ignoreCase = true))
        }
    }
    
    val categories = SkillCategory.entries
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(
            modifier = Modifier
                .height(screenHeight - 200.dp)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingSkill != null) "Edit Skill" else "Add Skills",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (editingSkill == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search skills") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { selectedCategory = null },
                            label = { Text("All") },
                            selected = selectedCategory == null
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            onClick = { 
                                selectedCategory = if (selectedCategory == category) null else category 
                            },
                            label = { 
                                Text(
                                    category.name.lowercase()
                                        .split('_')
                                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                                )
                            },
                            selected = selectedCategory == category
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (selectedSkill == null && editingSkill == null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSkills) { skill ->
                        SkillSelectionItem(
                            skill = skill,
                            onClick = { selectedSkill = skill }
                        )
                    }
                    
                    if (filteredSkills.isEmpty()) {
                        item {
                            EmptySkillsState(searchQuery = searchQuery)
                        }
                    }
                }
            } else {
                val skillToEdit = selectedSkill ?: editingSkill?.skill!!
                SkillConfigurationSection(
                    skill = skillToEdit,
                    proficiency = selectedProficiency,
                    hasExperience = hasExperience,
                    hasCertification = hasCertification,
                    yearsOfExperience = yearsOfExperience,
                    onProficiencyChanged = { selectedProficiency = it },
                    onExperienceChanged = { hasExperience = it },
                    onCertificationChanged = { hasCertification = it },
                    onYearsChanged = { yearsOfExperience = it },
                    onBack = { 
                        if (editingSkill != null) {
                            onDismiss()
                        } else {
                            selectedSkill = null
                        }
                    },
                    onAdd = {
                        onSkillAdded(
                            skillToEdit,
                            selectedProficiency,
                            hasExperience,
                            hasCertification,
                            yearsOfExperience
                        )
                        selectedSkill = null
                        selectedProficiency = ProficiencyLevel.BEGINNER
                        hasExperience = false
                        hasCertification = false
                        yearsOfExperience = 0
                        onDismiss()
                    },
                    isEditing = editingSkill != null
                )
            }
        }
    }
}

@Composable
private fun SkillSelectionItem(
    skill: Skill,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = skill.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val categoryColor = getCategoryColor(skill.category)
                Badge(
                    containerColor = categoryColor.copy(alpha = 0.1f),
                    contentColor = categoryColor
                ) {
                    Text(
                        text = skill.category.name.lowercase()
                            .split('_')
                            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SkillConfigurationSection(
    skill: Skill,
    proficiency: ProficiencyLevel,
    hasExperience: Boolean,
    hasCertification: Boolean,
    yearsOfExperience: Int,
    onProficiencyChanged: (ProficiencyLevel) -> Unit,
    onExperienceChanged: (Boolean) -> Unit,
    onCertificationChanged: (Boolean) -> Unit,
    onYearsChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    isEditing: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = skill.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Proficiency Level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ProficiencyLevel.entries.forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onProficiencyChanged(level) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = proficiency == level,
                                    onClick = { onProficiencyChanged(level) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = level.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = when (level) {
                                            ProficiencyLevel.BEGINNER -> "Just starting to learn"
                                            ProficiencyLevel.INTERMEDIATE -> "Comfortable with basics"
                                            ProficiencyLevel.EXPERT -> "Deep expertise and mastery"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Professional Experience",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Used in professional projects",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hasExperience,
                                onCheckedChange = onExperienceChanged
                            )
                        }
                        
                        if (hasExperience) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Years of Experience",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "$yearsOfExperience ${if (yearsOfExperience == 1) "year" else "years"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Drag to set years:",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$yearsOfExperience years",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Slider(
                                            value = yearsOfExperience.toFloat(),
                                            onValueChange = { onYearsChanged(it.toInt()) },
                                            valueRange = 0f..20f,
                                            steps = 19,
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "0 years",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "20+ years",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Certification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Have a certification for this skill",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hasCertification,
                            onCheckedChange = onCertificationChanged
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(
                if (isEditing) Icons.Default.Edit else Icons.Default.Add, 
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isEditing) "Update Skill" else "Add Skill", 
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun EmptySkillsState(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (searchQuery.isEmpty()) "No more skills available" else "No skills found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (searchQuery.isEmpty()) "All available skills have been added" else "Try a different search term or category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun getCategoryColor(category: SkillCategory): Color {
    return when (category) {
        SkillCategory.PROGRAMMING_LANGUAGES -> Color(0xFF2196F3)
        SkillCategory.FRAMEWORKS_LIBRARIES -> Color(0xFF4CAF50)
        SkillCategory.DATABASES -> Color(0xFFFF9800)
        SkillCategory.CLOUD_PLATFORMS -> Color(0xFF9C27B0)
        SkillCategory.DEVOPS_TOOLS -> Color(0xFFE91E63)
        SkillCategory.AI_ML -> Color(0xFF00BCD4)
        SkillCategory.DATA_SCIENCE -> Color(0xFF8BC34A)
        SkillCategory.SOFT_SKILLS -> Color(0xFFFF5722)
        SkillCategory.DESIGN_UX -> Color(0xFF673AB7)
        SkillCategory.MOBILE_DEVELOPMENT -> Color(0xFF607D8B)
        SkillCategory.WEB_DEVELOPMENT -> Color(0xFF795548)
        SkillCategory.CYBERSECURITY -> Color(0xFFF44336)
    }
}
