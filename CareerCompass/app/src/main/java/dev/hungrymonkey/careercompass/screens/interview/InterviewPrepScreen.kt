package dev.hungrymonkey.careercompass.screens.interview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.viewmodel.InterviewPrepViewModel
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.InterviewQuestion
import dev.hungrymonkey.careercompass.models.PracticeSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewPrepScreen(
    navController: NavHostController,
    onNavigateToQuestions: () -> Unit,
    viewModel: InterviewPrepViewModel = viewModel()
) {
    val allPracticeSessions by viewModel.practiceSessions.collectAsState()
    val allQuestions by viewModel.questions.collectAsState()
    
    val validPracticeSessions by remember { 
        derivedStateOf { 
            val questionIds = allQuestions.map { it.id }.toSet()
            allPracticeSessions.filter { it.questionId in questionIds }
        } 
    }
    
    val questionsCompleted by remember { derivedStateOf { validPracticeSessions.distinctBy { it.questionId }.size } }
    val averageScore by remember { derivedStateOf { 
        if (validPracticeSessions.isEmpty()) 0.0
        else validPracticeSessions.sumOf { it.feedback.overallScore }.toDouble() / validPracticeSessions.size
    } }
    
    val totalPracticeSessions = validPracticeSessions.size
    val categoryStats by remember { 
        derivedStateOf { 
            calculateCategoryPerformance(validPracticeSessions, allQuestions) 
        } 
    }
    val recentSessions by remember { 
        derivedStateOf { 
            validPracticeSessions.sortedByDescending { it.completedAt }.take(3) 
        } 
    }

    LaunchedEffect(allQuestions, allPracticeSessions) {
        if (allQuestions.isNotEmpty() && allPracticeSessions.isNotEmpty()) {
            viewModel.cleanupOrphanedSessions()
        }
    }

    Scaffold(
        topBar = { TopBar("Interview Prep", navController) },
        bottomBar = { NavigationBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToQuestions,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    Icons.Default.QuestionAnswer,
                    contentDescription = "Browse Questions"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Interview Prep Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                StatisticsOverviewCard(
                    questionsCompleted = questionsCompleted,
                    averageScore = averageScore,
                    totalSessions = totalPracticeSessions
                )
            }

            item {
                CategoryPerformanceCard(categoryStats)
            }

            if (recentSessions.isNotEmpty()) {
                item {
                    RecentSessionsCard(recentSessions, allQuestions)
                }
            }
        }
    }
}

@Composable
fun StatisticsOverviewCard(
    questionsCompleted: Int,
    averageScore: Double,
    totalSessions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Questions\nCompleted",
                    value = questionsCompleted.toString(),
                    icon = Icons.Default.QuestionAnswer,
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    label = "Average\nScore",
                    value = "${averageScore.toInt()}%",
                    icon = Icons.Default.Star,
                    color = getScoreColor(averageScore)
                )
                
                StatItem(
                    label = "Practice\nSessions",
                    value = totalSessions.toString(),
                    icon = Icons.Default.Psychology,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryPerformanceCard(categoryStats: Map<String, CategoryStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Performance by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (categoryStats.isEmpty()) {
                Text(
                    text = "No practice data yet. Start practicing to see your performance!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                categoryStats.forEach { (category, stats) ->
                    CategoryPerformanceItem(
                        category = category,
                        stats = stats
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryPerformanceItem(
    category: String,
    stats: CategoryStats
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stats.questionsAnswered} ${if (stats.questionsAnswered == 1) "question" else "questions"} • ${stats.avgScore.toInt()}% avg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = getScoreColor(stats.avgScore).copy(alpha = 0.1f)
            ) {
                Text(
                    text = getScoreLabel(stats.avgScore),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = getScoreColor(stats.avgScore),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        LinearProgressIndicator(
            progress = { (stats.avgScore / 100).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = getScoreColor(stats.avgScore),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun RecentSessionsCard(
    recentSessions: List<PracticeSession>,
    questions: List<InterviewQuestion>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Recent Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            recentSessions.forEach { session ->
                RecentSessionItem(session, questions)
                if (session != recentSessions.last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun RecentSessionItem(
    session: PracticeSession,
    questions: List<InterviewQuestion>
) {
    val question = questions.find { it.id == session.questionId }
    val questionText = question?.text ?: "Question not found"
    val truncatedText = if (questionText.length > 60) {
        questionText.take(57) + "..."
    } else {
        questionText
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = truncatedText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Score: ${session.feedback.overallScore}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = getScoreColor(session.feedback.overallScore.toDouble()).copy(alpha = 0.1f)
        ) {
            Text(
                text = getScoreLabel(session.feedback.overallScore.toDouble()),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = getScoreColor(session.feedback.overallScore.toDouble()),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class CategoryStats(
    val questionsAnswered: Int,
    val avgScore: Double
)

private fun calculateCategoryPerformance(
    sessions: List<PracticeSession>,
    questions: List<InterviewQuestion>
): Map<String, CategoryStats> {
    if (sessions.isEmpty() || questions.isEmpty()) return emptyMap()
    
    val questionMap = questions.associateBy { it.id }
    
    val sessionsByCategory = sessions.mapNotNull { session ->
        val question = questionMap[session.questionId]
        question?.let { session to it.category }
    }.groupBy { it.second }
    
    return sessionsByCategory.mapValues { (_, sessionCategoryPairs) ->
        val categorySessions = sessionCategoryPairs.map { it.first }
        val questionsAnswered = categorySessions.distinctBy { it.questionId }.size
        val avgScore = categorySessions.map { it.feedback.overallScore }.average()
        
        CategoryStats(questionsAnswered, avgScore)
    }.filterValues { it.questionsAnswered > 0 }
}

private fun getScoreColor(score: Double): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFFA726)
        else -> Color(0xFFFF5722)
    }
}

private fun getScoreLabel(score: Double): String {
    return when {
        score >= 80 -> "Excellent"
        score >= 60 -> "Good"
        else -> "Needs Work"
    }
}
