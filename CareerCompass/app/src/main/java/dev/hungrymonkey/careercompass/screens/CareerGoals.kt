package dev.hungrymonkey.careercompass.screens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hungrymonkey.careercompass.models.Goal
import dev.hungrymonkey.careercompass.viewmodel.CareerGoalsViewModel
import dev.hungrymonkey.careercompass.viewmodel.CareerGoalsViewModelFactory
import android.app.Application
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar



enum class GoalSortOption(val displayName: String) {
    ALPHABETICAL("A-Z"),
    PRIORITY("Priority"),
    DATE_APPROACHING("Date (Nearest)"),
    DATE_FURTHEST("Date (Furthest)")
}


fun sortGoals(goals: List<Goal>, sortOption: GoalSortOption): List<Goal> {
    return when (sortOption) {
        GoalSortOption.ALPHABETICAL -> goals.sortedBy { it.title.lowercase() }
        GoalSortOption.PRIORITY -> {
            val priorityOrder = mapOf("High" to 0, "Medium" to 1, "Low" to 2)
            goals.sortedBy { priorityOrder[it.priority] ?: 3 }
        }
        GoalSortOption.DATE_APPROACHING -> {
            goals.sortedBy { goal ->
                try {
                    val parts = goal.targetDate.split("/")
                    if (parts.size == 3) {
                        val month = parts[0].toInt()
                        val day = parts[1].toInt() 
                        val year = parts[2].toInt()
                        year * 10000 + month * 100 + day
                    } else {
                        Int.MAX_VALUE
                    }
                } catch (_: Exception) {
                    Int.MAX_VALUE
                }
            }
        }
        GoalSortOption.DATE_FURTHEST -> {
            goals.sortedByDescending { goal ->
                try {
                    val parts = goal.targetDate.split("/")
                    if (parts.size == 3) {
                        val month = parts[0].toInt()
                        val day = parts[1].toInt()
                        val year = parts[2].toInt()
                        year * 10000 + month * 100 + day
                    } else {
                        Int.MIN_VALUE
                    }
                } catch (_: Exception) {
                    Int.MIN_VALUE
                }
            }
        }
    }
}

@Composable
fun CareerGoalsScreen(
    navController: NavHostController,
    viewModel: CareerGoalsViewModel = viewModel(factory = CareerGoalsViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val goals by viewModel.goals.collectAsState(initial = emptyList())
    var showAddGoalSheet by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<Goal?>(null) }
    var currentSortOption by remember { mutableStateOf(GoalSortOption.DATE_APPROACHING) }
    Scaffold(
        topBar = { TopBar("Career Goals", navController) },
        bottomBar = { NavigationBar(navController) },
        floatingActionButton = {
            AnimatedFab(
                onClick = {
                    goalToEdit = null
                    showAddGoalSheet = true
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            val unsortedActiveGoals = goals.filter { goal ->
                goal.subtasks.isEmpty() || !goal.subtasks.all { it.isCompleted }
            }
            val unsortedCompletedGoals = goals.filter { goal ->
                goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
            }
            
            val activeGoals = sortGoals(unsortedActiveGoals, currentSortOption)
            val completedGoals = sortGoals(unsortedCompletedGoals, currentSortOption)
            
            if (goals.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No goals yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap + to create your first career goal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (activeGoals.isEmpty() && completedGoals.isNotEmpty()) {
                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "All goals completed!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Great work! Add new goals or restore completed ones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    
                    item {
                        CompletedGoalsSection(
                            completedGoals = completedGoals,
                            onDelete = { goalId -> viewModel.deleteGoal(goalId) },
                            onEdit = { goal ->
                                goalToEdit = goal
                                showAddGoalSheet = true
                            },
                            onToggleSubtask = { goal, index, checked ->
                                if (index == -1) {
                                    if (goal.subtasks.isEmpty()) {
                                        val newSubtask = dev.hungrymonkey.careercompass.models.Subtask(
                                            title = "Complete ${goal.title}",
                                            isCompleted = checked
                                        )
                                        val updatedGoal = goal.copy(subtasks = listOf(newSubtask))
                                        viewModel.updateGoal(updatedGoal)
                                    } else {
                                        val updatedSubtasks = goal.subtasks.map { subtask ->
                                            subtask.copy(isCompleted = checked)
                                        }
                                        val updatedGoal = goal.copy(subtasks = updatedSubtasks)
                                        viewModel.updateGoal(updatedGoal)
                                    }
                                } else {
                                    val updatedSubtasks = goal.subtasks.mapIndexed { i, subtask ->
                                        if (i == index) subtask.copy(isCompleted = checked)
                                        else subtask
                                    }
                                    val updatedGoal = goal.copy(subtasks = updatedSubtasks)
                                    viewModel.updateGoal(updatedGoal)
                                }
                            },
                            onRestoreGoal = { goal ->
                                val restoredSubtasks = goal.subtasks.map { subtask ->
                                    subtask.copy(isCompleted = false)
                                }
                                val restoredGoal = goal.copy(subtasks = restoredSubtasks)
                                viewModel.updateGoal(restoredGoal)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.padding(16.dp)) {
                    if (activeGoals.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Active Goals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                SortButton(
                                    currentSortOption = currentSortOption,
                                    onSortOptionChanged = { newOption ->
                                        currentSortOption = newOption
                                    }
                                )
                            }
                        }
                        
                        items(activeGoals) { goal ->
                            GoalCard(
                                goal = goal,
                                isCompleted = false,
                                onDelete = {viewModel.deleteGoal(goal.id)},
                                onEdit = { 
                                    goalToEdit = goal
                                    showAddGoalSheet = true
                                },
                                onToggleSubtask = { index, checked ->
                                    if (index == -1) {
                                        if (goal.subtasks.isEmpty()) {
                                            val newSubtask = dev.hungrymonkey.careercompass.models.Subtask(
                                                title = "Complete ${goal.title}",
                                                isCompleted = checked
                                            )
                                            val updatedGoal = goal.copy(subtasks = listOf(newSubtask))
                                            viewModel.updateGoal(updatedGoal)
                                        } else {
                                            val updatedSubtasks = goal.subtasks.map { subtask ->
                                                subtask.copy(isCompleted = checked)
                                            }
                                            val updatedGoal = goal.copy(subtasks = updatedSubtasks)
                                            viewModel.updateGoal(updatedGoal)
                                        }
                                    } else {
                                        val updatedSubtasks = goal.subtasks.mapIndexed { i, subtask ->
                                            if (i == index) subtask.copy(isCompleted = checked)
                                            else subtask
                                        }
                                        val updatedGoal = goal.copy(subtasks = updatedSubtasks)
                                        viewModel.updateGoal(updatedGoal)
                                    }
                                },
                                onRestoreGoal = null
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                    
                    if (completedGoals.isNotEmpty()) {
                        item {
                            CompletedGoalsSection(
                                completedGoals = completedGoals,
                                onDelete = { goalId -> viewModel.deleteGoal(goalId) },
                                onEdit = { goal ->
                                    goalToEdit = goal
                                    showAddGoalSheet = true
                                },
                                onToggleSubtask = { goal, index, checked ->
                                    if (index == -1) {
                                        if (goal.subtasks.isEmpty()) {
                                            val newSubtask = dev.hungrymonkey.careercompass.models.Subtask(
                                                title = "Complete ${goal.title}",
                                                isCompleted = checked
                                            )
                                            val updatedGoal = goal.copy(subtasks = listOf(newSubtask))
                                            viewModel.updateGoal(updatedGoal)
                                        } else {
                                            val updatedSubtasks = goal.subtasks.map { subtask ->
                                                subtask.copy(isCompleted = checked)
                                            }
                                            val updatedGoal = goal.copy(subtasks = updatedSubtasks)
                                            viewModel.updateGoal(updatedGoal)
                                        }
                                    } else {
                                        val updatedSubtasks = goal.subtasks.mapIndexed { i, subtask ->
                                            if (i == index) subtask.copy(isCompleted = checked)
                                            else subtask
                                        }
                                        val updatedGoal = goal.copy(subtasks = updatedSubtasks)
                                        viewModel.updateGoal(updatedGoal)
                                    }
                                },
                                onRestoreGoal = { goal ->
                                    val restoredSubtasks = goal.subtasks.map { subtask ->
                                        subtask.copy(isCompleted = false)
                                    }
                                    val restoredGoal = goal.copy(subtasks = restoredSubtasks)
                                    viewModel.updateGoal(restoredGoal)
                                }
                            )
                        }
                    }
                }
            }

            if (showAddGoalSheet) {
                AddGoalSheet(
                    goalToEdit = goalToEdit,
                    onSave = { updatedGoal ->
                        if (goalToEdit != null) {
                            viewModel.updateGoal(updatedGoal)
                        } else {
                            viewModel.addGoal(updatedGoal)
                        }
                        showAddGoalSheet = false
                        goalToEdit = null
                    },
                    onCancel = { 
                        showAddGoalSheet = false
                        goalToEdit = null
                    }
                )
            }
        }
    }
}


@Composable
fun GoalCard(
    goal: Goal,
    isCompleted: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleSubtask: (Int, Boolean) -> Unit,
    onRestoreGoal: (() -> Unit)? = null
)  {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    val completedCount = goal.subtasks.count{it.isCompleted}
    val total = goal.subtasks.size
    val progress = if (total > 0) completedCount / total.toFloat() else 0f
    
    val isGoalCompleted = if (goal.subtasks.isEmpty()) {
        false
    } else {
        goal.subtasks.all { it.isCompleted }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)){
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            when (goal.priority) {
                                "High" -> Color(0xFFFF5722).copy(alpha = 0.12f)
                                "Medium" -> Color(0xFFFFA726).copy(alpha = 0.12f)
                                "Low" -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        goal.icon, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp,
                        color = when (goal.priority) {
                            "High" -> Color(0xFFFF5722)
                            "Medium" -> Color(0xFFFFA726)
                            "Low" -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)){
                    Text(
                        goal.title, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        style = if (isCompleted) {
                            MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            MaterialTheme.typography.titleMedium
                        }
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Target date",
                            modifier = Modifier.size(14.dp),
                            tint = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            goal.targetDate, 
                            fontSize = 14.sp, 
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                        )
                    }
                    if (isCompleted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            if (goal.subtasks.isEmpty()) {
                                val newSubtask = dev.hungrymonkey.careercompass.models.Subtask(
                                    title = "Complete ${goal.title}",
                                    isCompleted = true
                                )
                                val updatedGoal = goal.copy(subtasks = listOf(newSubtask))
                                onToggleSubtask(-1, true)
                            } else {
                                val allCompleted = goal.subtasks.all { it.isCompleted }
                                onToggleSubtask(-1, !allCompleted)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (goal.subtasks.isEmpty()) {
                                Icons.Outlined.Circle
                            } else if (goal.subtasks.all { it.isCompleted }) {
                                Icons.Default.CheckCircle 
                            } else {
                                Icons.Outlined.Circle
                            },
                            contentDescription = if (goal.subtasks.isEmpty()) {
                                "Mark goal as complete"
                            } else if (goal.subtasks.all { it.isCompleted }) {
                                "Mark as incomplete" 
                            } else {
                                "Mark as complete"
                            },
                            tint = if (goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }) {
                                MaterialTheme.colorScheme.primary 
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (goal.subtasks.isNotEmpty()) {
                        IconButton(onClick = {expanded = !expanded}) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showActionsMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert, 
                                contentDescription = "More actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false }
                        ) {
                            if (isCompleted && onRestoreGoal != null) {
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Restore, 
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Restore Goal",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        showActionsMenu = false
                                        onRestoreGoal()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Edit Goal")
                                    }
                                },
                                onClick = {
                                    showActionsMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Delete, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Delete Goal",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    showActionsMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            if (goal.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = {progress},
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    trackColor = if (isCompleted) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityBadge(priority = goal.priority)
                    Text(
                        "${completedCount}/${total} sub-tasks complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityBadge(priority = goal.priority)
                    Text(
                        "Incomplete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded && goal.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Sub-tasks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                goal.subtasks.forEachIndexed { index, subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onToggleSubtask(index, !subtask.isCompleted)
                            }
                            .padding(vertical = 8.dp)
                    ){
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = if (subtask.isCompleted) 
                                        MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { 
                                    onToggleSubtask(index, !subtask.isCompleted)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (subtask.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            subtask.title, 
                            modifier = Modifier.weight(1f),
                            style = if (subtask.isCompleted) {
                                MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                MaterialTheme.typography.bodyMedium
                            }
                        )
                    }
                    
                    if (index < goal.subtasks.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 32.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                } 
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to delete \"${goal.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (backgroundColor, textColor, icon) = when (priority) {
        "High" -> Triple(
            Color(0xFFFF5722),
            Color.White,
            Icons.Outlined.PriorityHigh
        )
        "Medium" -> Triple(
            Color(0xFFFFA726),
            Color.White,
            Icons.Outlined.Speed
        )
        "Low" -> Triple(
            Color(0xFF4CAF50),
            Color.White,
            Icons.AutoMirrored.Outlined.TrendingDown
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Outlined.Speed
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = priority.uppercase(),
                color = textColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun AnimatedFab(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "fab_scale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
    
    FloatingActionButton(
        onClick = {
            isPressed = true
            onClick()
        },
        containerColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Goal"
        )
    }
}

@Composable
fun CompletedGoalsSection(
    completedGoals: List<Goal>,
    onDelete: (String) -> Unit,
    onEdit: (Goal) -> Unit,
    onToggleSubtask: (Goal, Int, Boolean) -> Unit,
    onRestoreGoal: (Goal) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed goals",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Completed Goals (${completedGoals.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    completedGoals.forEach { goal ->
                        GoalCard(
                            goal = goal,
                            isCompleted = true,
                            onDelete = { onDelete(goal.id) },
                            onEdit = { onEdit(goal) },
                            onToggleSubtask = { index, checked ->
                                onToggleSubtask(goal, index, checked)
                            },
                            onRestoreGoal = { onRestoreGoal(goal) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SortButton(
    currentSortOption: GoalSortOption,
    onSortOptionChanged: (GoalSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (expanded) MaterialTheme.colorScheme.secondaryContainer 
                               else Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = BorderStroke(
                1.dp, 
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.Sort,
                contentDescription = "Sort options",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                currentSortOption.displayName,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(16.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
        ) {
            GoalSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (option == currentSortOption) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Spacer(Modifier.width(24.dp))
                            }
                            Text(
                                option.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (option == currentSortOption) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        onSortOptionChanged(option)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (option == currentSortOption) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        else 
                            Color.Transparent
                    )
                )
            }
        }
    }
}
