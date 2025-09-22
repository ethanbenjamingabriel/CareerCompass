package dev.hungrymonkey.careercompass.screens.interview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.viewmodel.InterviewPrepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomQuestionScreen(
    navController: NavHostController,
    viewModel: InterviewPrepViewModel = viewModel()
) {
    val context = LocalContext.current
    
    var questionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var questionError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var difficultyError by remember { mutableStateOf<String?>(null) }
    var tagsError by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf("Technical", "Behavioural", "System Design", "Leadership")
    val difficulties = listOf("Junior", "Mid", "Senior")
    
    fun validateForm(): Boolean {
        var isValid = true
        
        questionError = null
        categoryError = null
        difficultyError = null
        tagsError = null
        
        if (questionText.trim().isEmpty()) {
            questionError = "Question text is required"
            isValid = false
        } else if (questionText.trim().length < 10) {
            questionError = "Question must be at least 10 characters long"
            isValid = false
        } else if (questionText.trim().length > 500) {
            questionError = "Question must be less than 500 characters"
            isValid = false
        }
        
        if (selectedCategory.trim().isEmpty()) {
            categoryError = "Category is required"
            isValid = false
        } else if (!categories.contains(selectedCategory)) {
            categoryError = "Please select a valid category"
            isValid = false
        }
        
        if (selectedDifficulty.trim().isEmpty()) {
            difficultyError = "Difficulty level is required"
            isValid = false
        } else if (!difficulties.contains(selectedDifficulty)) {
            difficultyError = "Please select a valid difficulty level"
            isValid = false
        }
        
        if (tagsText.trim().isEmpty()) {
            tagsError = "At least one tag is required"
            isValid = false
        } else {
            val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isEmpty()) {
                tagsError = "At least one tag is required"
                isValid = false
            } else if (tags.any { it.length < 2 }) {
                tagsError = "Each tag must be at least 2 characters long"
                isValid = false
            } else if (tags.size > 10) {
                tagsError = "Maximum 10 tags allowed"
                isValid = false
            } else if (tags.any { it.length > 50 }) {
                tagsError = "Each tag must be less than 50 characters"
                isValid = false
            }
        }
        
        return isValid
    }
    
    fun submitQuestion() {
        if (!validateForm()) return
        
        isSubmitting = true
        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        viewModel.saveCustomQuestion(
            text = questionText.trim(),
            category = selectedCategory,
            difficulty = selectedDifficulty,
            tags = tags,
            onSuccess = {
                Toast.makeText(context, "Question added successfully!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            },
            onFailure = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                isSubmitting = false
            }
        )
    }

    Scaffold(
        topBar = { TopBar(title = "Add Custom Question", navController = navController) },
        bottomBar = { NavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Create Your Question",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Add a custom interview question that fits your needs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Question Text *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Enter your interview question") },
                        placeholder = { Text("e.g., Describe your experience with designing scalable systems...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        isError = questionError != null,
                        supportingText = {
                            if (questionError != null) {
                                Text(
                                    text = questionError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = "${questionText.length}/500 characters",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Category *",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        var categoryExpanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("category") },
                                trailingIcon = { 
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) 
                                },
                                isError = categoryError != null,
                                supportingText = {
                                    if (categoryError != null) {
                                        Text(
                                            text = categoryError!!,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            selectedCategory = category
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Difficulty *",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        var difficultyExpanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = difficultyExpanded,
                            onExpandedChange = { difficultyExpanded = !difficultyExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDifficulty,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("difficulty") },
                                trailingIcon = { 
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) 
                                },
                                isError = difficultyError != null,
                                supportingText = {
                                    if (difficultyError != null) {
                                        Text(
                                            text = difficultyError!!,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = difficultyExpanded,
                                onDismissRequest = { difficultyExpanded = false }
                            ) {
                                difficulties.forEach { difficulty ->
                                    DropdownMenuItem(
                                        text = { Text(difficulty) },
                                        onClick = {
                                            selectedDifficulty = difficulty
                                            difficultyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tags *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Add relevant tags") },
                        placeholder = { Text("algorithms, data structures, problem solving") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = tagsError != null,
                        supportingText = {
                            if (tagsError != null) {
                                Text(
                                    text = tagsError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = "Separate tags with commas. At least 1 tag required, max 10 tags.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }
            
            if (questionText.isNotEmpty()) {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Preview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Question Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Text(
                            text = questionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (selectedCategory.isNotEmpty()) {
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            text = "📂 $selectedCategory",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                            if (selectedDifficulty.isNotEmpty()) {
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            text = "⭐ $selectedDifficulty",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                        
                        if (tagsText.isNotEmpty()) {
                            val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (tags.isNotEmpty()) {
                                Text(
                                    text = "Tags: ${tags.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Button(
                onClick = { submitQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSubmitting && questionText.trim().isNotEmpty() && 
                         selectedCategory.isNotEmpty() && selectedDifficulty.isNotEmpty() &&
                         tagsText.trim().isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("Adding Question...")
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Add Question",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
