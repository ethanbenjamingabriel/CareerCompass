package dev.hungrymonkey.careercompass.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.Goal
import dev.hungrymonkey.careercompass.viewmodel.CareerGoalsViewModel
import dev.hungrymonkey.careercompass.viewmodel.CareerGoalsViewModelFactory
import android.app.Application
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Composable
fun ProgressDashboardScreen(
    navController: NavHostController,
    viewModel: CareerGoalsViewModel = viewModel(factory = CareerGoalsViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val goals by viewModel.goals.collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = { TopBar("Progress Dashboard") },
        bottomBar = { NavigationBar(navController) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Career Goals Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                OverallProgressCard(goals = goals)
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompletionRateCard(
                        goals = goals,
                        modifier = Modifier.weight(1f)
                    )
                    StreakCard(
                        goals = goals,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AverageTimeCard(
                        goals = goals,
                        modifier = Modifier.weight(1f)
                    )
                    PriorityBreakdownCard(
                        goals = goals,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                UpcomingDeadlinesCard(goals = goals)
            }
            
            item {
                GoalCategoriesCard(goals = goals)
            }
            
            item {
                RecentCompletionsCard(goals = goals)
            }
        }
    }
}

@Composable
fun OverallProgressCard(goals: List<Goal>) {
    val totalGoals = goals.size
    val completedGoals = goals.count { goal ->
        goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
    }
    val activeGoals = totalGoals - completedGoals
    val totalSubtasks = goals.sumOf { it.subtasks.size }
    val completedSubtasks = goals.sumOf { goal -> goal.subtasks.count { it.isCompleted } }
    
    val overallProgress = if (totalSubtasks > 0) {
        (completedSubtasks.toFloat() / totalSubtasks.toFloat())
    } else 0f
    
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Overall Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProgressStat("Total Goals", totalGoals.toString(), Icons.Default.Flag)
                ProgressStat("Active", activeGoals.toString(), Icons.Default.PlayArrow)
                ProgressStat("Completed", completedGoals.toString(), Icons.Default.CheckCircle)
            }
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Overall Progress",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${(overallProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$completedSubtasks of $totalSubtasks tasks completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressStat(
    label: String, 
    value: String, 
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompletionRateCard(
    goals: List<Goal>,
    modifier: Modifier = Modifier
) {
    val totalGoals = goals.size
    val completedGoals = goals.count { goal ->
        goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
    }
    val completionRate = if (totalGoals > 0) {
        (completedGoals.toFloat() / totalGoals.toFloat() * 100).toInt()
    } else 0
    
    MetricCard(
        title = "Success Rate",
        value = "$completionRate%",
        subtitle = "$completedGoals/$totalGoals goals",
        icon = Icons.Default.Assessment,
        color = getCompletionRateColor(completionRate),
        modifier = modifier
    )
}

@Composable
fun StreakCard(
    goals: List<Goal>,
    modifier: Modifier = Modifier
) {
    val recentCompletions = goals.count { goal ->
        goal.subtasks.isNotEmpty() && goal.subtasks.any { it.isCompleted }
    }
    val streak = min(recentCompletions, 7)
    
    MetricCard(
        title = "Activity Streak",
        value = "${streak}d",
        subtitle = "Days active",
        icon = Icons.Default.LocalFireDepartment,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier
    )
}

@Composable
fun AverageTimeCard(
    goals: List<Goal>,
    modifier: Modifier = Modifier
) {
    val avgDays = calculateAverageTimeToCompletion(goals)
    
    MetricCard(
        title = "Avg. Duration",
        value = "${avgDays}d",
        subtitle = "To completion",
        icon = Icons.Default.Schedule,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
    )
}

@Composable
fun PriorityBreakdownCard(
    goals: List<Goal>,
    modifier: Modifier = Modifier
) {
    val highPriority = goals.count { it.priority == "High" }
    val mediumPriority = goals.count { it.priority == "Medium" }
    val lowPriority = goals.count { it.priority == "Low" }
    val mostCommon = listOf(
        "High" to highPriority,
        "Medium" to mediumPriority,
        "Low" to lowPriority
    ).maxByOrNull { it.second }?.first ?: "Medium"
    
    MetricCard(
        title = "Priority Focus",
        value = mostCommon,
        subtitle = "${goals.count { it.priority == mostCommon }} goals",
        icon = Icons.Default.PriorityHigh,
        color = getPriorityColor(mostCommon),
        modifier = modifier
    )
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpcomingDeadlinesCard(goals: List<Goal>) {
    val upcomingGoals = getUpcomingDeadlines(goals).take(3)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Upcoming Deadlines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (upcomingGoals.isEmpty()) {
                Text(
                    "No upcoming deadlines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                upcomingGoals.forEach { (goal, daysUntil) ->
                    DeadlineItem(goal = goal, daysUntil = daysUntil)
                    if (goal != upcomingGoals.last().first) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DeadlineItem(goal: Goal, daysUntil: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val urgencyColor = getUrgencyColor(daysUntil)
        
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(urgencyColor, CircleShape)
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                goal.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                goal.targetDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            when {
                daysUntil == 0L -> "Today"
                daysUntil == 1L -> "Tomorrow"
                daysUntil < 0 -> "Overdue"
                else -> "${daysUntil}d"
            },
            style = MaterialTheme.typography.labelMedium,
            color = urgencyColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GoalCategoriesCard(goals: List<Goal>) {
    val categories = categorizeGoals(goals)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Goal Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (categories.isEmpty()) {
                Text(
                    "No goals to categorize",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.toList()) { (category, count) ->
                        CategoryChip(category = category, count = count)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(category: String, count: Int) {
    val categoryColor = getCategoryColor(category)
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = categoryColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            categoryColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(categoryColor, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                category,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(4.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentCompletionsCard(goals: List<Goal>) {
    val recentCompletions = getRecentCompletions(goals).take(3)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Recent Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (recentCompletions.isEmpty()) {
                Text(
                    "Complete your first goal to see achievements here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                recentCompletions.forEach { goal ->
                    CompletionItem(goal = goal)
                    if (goal != recentCompletions.last()) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionItem(goal: Goal) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                goal.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${goal.subtasks.size} tasks completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = getPriorityColor(goal.priority).copy(alpha = 0.1f)
        ) {
            Text(
                goal.priority,
                style = MaterialTheme.typography.labelSmall,
                color = getPriorityColor(goal.priority),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun calculateAverageTimeToCompletion(goals: List<Goal>): Int {
    val completedGoals = goals.filter { goal ->
        goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
    }
    
    if (completedGoals.isEmpty()) return 0
    
    return 30
}

private fun getUpcomingDeadlines(goals: List<Goal>): List<Pair<Goal, Long>> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    
    return goals.mapNotNull { goal ->
        try {
            val targetDate = LocalDate.parse(goal.targetDate, formatter)
            val daysUntil = ChronoUnit.DAYS.between(today, targetDate)
            goal to daysUntil
        } catch (_: Exception) {
            null
        }
    }.sortedBy { it.second }
}

private fun categorizeGoals(goals: List<Goal>): Map<String, Int> {
    val categories = mutableMapOf<String, Int>()
    
    goals.forEach { goal ->
        val category = when {
            goal.title.lowercase().contains("learn") || 
            goal.title.lowercase().contains("course") ||
            goal.title.lowercase().contains("study") -> "Learning"
            
            goal.title.lowercase().contains("project") ||
            goal.title.lowercase().contains("build") ||
            goal.title.lowercase().contains("develop") -> "Projects"
            
            goal.title.lowercase().contains("network") ||
            goal.title.lowercase().contains("meeting") ||
            goal.title.lowercase().contains("connect") -> "Networking"
            
            goal.title.lowercase().contains("career") ||
            goal.title.lowercase().contains("job") ||
            goal.title.lowercase().contains("promotion") -> "Career Growth"
            
            else -> "General"
        }
        
        categories[category] = categories.getOrDefault(category, 0) + 1
    }
    
    return categories
}

private fun getRecentCompletions(goals: List<Goal>): List<Goal> {
    return goals.filter { goal ->
        goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
    }.sortedByDescending { it.title }
}

@Composable
private fun getCategoryColor(category: String): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (category) {
        "Learning" -> colorScheme.secondary
        "Projects" -> colorScheme.primary
        "Networking" -> colorScheme.tertiary
        "Career Growth" -> colorScheme.primaryContainer
        else -> colorScheme.outline
    }
}

@Composable
private fun getPriorityColor(priority: String): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (priority) {
        "High" -> colorScheme.error
        "Medium" -> colorScheme.secondary
        "Low" -> colorScheme.primary
        else -> colorScheme.outline
    }
}

@Composable
private fun getCompletionRateColor(completionRate: Int): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when {
        completionRate >= 80 -> colorScheme.primary
        completionRate >= 60 -> colorScheme.secondary
        else -> colorScheme.error
    }
}

@Composable
private fun getUrgencyColor(daysUntil: Long): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when {
        daysUntil <= 3 -> colorScheme.error
        daysUntil <= 7 -> colorScheme.secondary
        else -> colorScheme.primary
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProgressDashboardScreen() {
    val navController = rememberNavController()
    ProgressDashboardScreen(navController)
}