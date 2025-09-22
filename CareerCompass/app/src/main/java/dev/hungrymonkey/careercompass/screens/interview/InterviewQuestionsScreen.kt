package dev.hungrymonkey.careercompass.screens.interview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.InterviewQuestion
import dev.hungrymonkey.careercompass.viewmodel.InterviewPrepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewQuestionsScreen(
    navController: NavHostController,
    onQuestionClick: (String) -> Unit,
    viewModel: InterviewPrepViewModel = viewModel<InterviewPrepViewModel>()
) {
    val questions by viewModel.questions.collectAsState()
    val bookmarkedQuestions by viewModel.bookmarkedQuestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String>("All") }
    var selectedDifficulty by remember { mutableStateOf<String>("All") }
    var selectedBookmarkFilter by remember { mutableStateOf<String>("All") }
    var selectedSourceFilter by remember { mutableStateOf<String>("All") }

    val categories = listOf("All", "Technical", "Behavioural", "System Design", "Leadership")
    val difficulties = listOf("All", "Junior", "Mid", "Senior")
    val bookmarkFilters = listOf("All", "Bookmarked", "Not Bookmarked")
    val sourceFilters = listOf("All", "System Questions", "Custom Questions")

    val filteredQuestions = questions.filter { question ->
        val isQuestionBookmarked = bookmarkedQuestions.any { it.questionId == question.id }
        val matchesSearch = searchQuery.isBlank() || 
                           question.text.contains(searchQuery, ignoreCase = true)
        
        matchesSearch &&
        (selectedCategory == "All" || question.category == selectedCategory) &&
        (selectedDifficulty == "All" || question.difficulty == selectedDifficulty) &&
        (selectedBookmarkFilter == "All" || 
         (selectedBookmarkFilter == "Bookmarked" && isQuestionBookmarked) ||
         (selectedBookmarkFilter == "Not Bookmarked" && !isQuestionBookmarked)) &&
        (selectedSourceFilter == "All" ||
         (selectedSourceFilter == "System Questions" && !question.isCustom) ||
         (selectedSourceFilter == "Custom Questions" && question.isCustom))
    }

    Scaffold(
        topBar = { TopBar(title = "Interview Questions") },
        bottomBar = { NavigationBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_custom_question") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Custom Question"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search questions") },
                    placeholder = { Text("Enter keywords to search...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                var showFilters by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = { showFilters = !showFilters }
                    ) {
                        Text(if (showFilters) "Hide Filters" else "Show Filters")
                        Icon(
                            imageVector = if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showFilters) "Hide" else "Show"
                        )
                    }
                }
                
                if (showFilters) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var categoryExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = !categoryExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                    modifier = Modifier.menuAnchor()
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
                            
                            var difficultyExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = difficultyExpanded,
                                onExpandedChange = { difficultyExpanded = !difficultyExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedDifficulty,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Difficulty") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                                    modifier = Modifier.menuAnchor()
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
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var bookmarkExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = bookmarkExpanded,
                                onExpandedChange = { bookmarkExpanded = !bookmarkExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedBookmarkFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Bookmarks") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bookmarkExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = bookmarkExpanded,
                                    onDismissRequest = { bookmarkExpanded = false }
                                ) {
                                    bookmarkFilters.forEach { filter ->
                                        DropdownMenuItem(
                                            text = { Text(filter) },
                                            onClick = {
                                                selectedBookmarkFilter = filter
                                                bookmarkExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            var sourceExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = sourceExpanded,
                                onExpandedChange = { sourceExpanded = !sourceExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedSourceFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Source") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = sourceExpanded,
                                    onDismissRequest = { sourceExpanded = false }
                                ) {
                                    sourceFilters.forEach { filter ->
                                        DropdownMenuItem(
                                            text = { Text(filter) },
                                            onClick = {
                                                selectedSourceFilter = filter
                                                sourceExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (selectedCategory != "All" || selectedDifficulty != "All" || 
                            selectedBookmarkFilter != "All" || selectedSourceFilter != "All") {
                            OutlinedButton(
                                onClick = {
                                    selectedCategory = "All"
                                    selectedDifficulty = "All"
                                    selectedBookmarkFilter = "All"
                                    selectedSourceFilter = "All"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Clear, 
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Clear All Filters")
                            }
                        }
                    }
                }
            }

            Divider()

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading interview questions...")
                    }
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error loading questions",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { 
                            viewModel.clearError()
                            viewModel.loadQuestions()
                        }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                if (filteredQuestions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "No questions found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Try adjusting your filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotBlank() || selectedCategory != "All" || 
                                selectedDifficulty != "All" || selectedBookmarkFilter != "All" || selectedSourceFilter != "All") {
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategory = "All"
                                        selectedDifficulty = "All"
                                        selectedBookmarkFilter = "All"
                                        selectedSourceFilter = "All"
                                    }
                                ) {
                                    Text("Clear all filters")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredQuestions) { question ->
                            val isBookmarked by remember(question.id, bookmarkedQuestions) {
                                derivedStateOf { 
                                    bookmarkedQuestions.any { it.questionId == question.id }
                                }
                            }
                            
                            QuestionItem(
                                question = question,
                                isBookmarked = isBookmarked,
                                onQuestionClick = { onQuestionClick(question.id) },
                                onBookmarkClick = { viewModel.toggleBookmark(question.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionItem(
    question: InterviewQuestion,
    isBookmarked: Boolean,
    onQuestionClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onQuestionClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = "📂 ${question.category}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = "⭐ ${question.difficulty}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                        if (question.isCustom) {
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        text = "✏️ Custom",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                    
                    if (question.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Tags:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            question.tags.take(3).forEach { tag ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                            if (question.tags.size > 3) {
                                SuggestionChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            text = "+${question.tags.size - 3}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
