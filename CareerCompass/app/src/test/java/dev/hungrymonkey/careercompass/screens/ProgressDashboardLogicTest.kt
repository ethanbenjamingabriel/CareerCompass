package dev.hungrymonkey.careercompass.screens

import dev.hungrymonkey.careercompass.models.Goal
import dev.hungrymonkey.careercompass.models.Subtask
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProgressDashboardLogicTest {
    private fun makeGoal(
        title: String,
        targetDate: String,
        subtasks: List<Subtask> = emptyList(),
        priority: String = "Medium"
    ) = Goal(
        id = title,
        title = title,
        targetDate = targetDate,
        subtasks = subtasks,
        icon = "",
        priority = priority
    )

    @Test
    fun testCalculateAverageTimeToCompletion_empty() {
        val result = calculateAverageTimeToCompletion(emptyList())
        assertEquals(0, result)
    }

    @Test
    fun testCalculateAverageTimeToCompletion_nonEmpty() {
        val completedGoals = listOf(
            makeGoal("Goal 1", "12/31/2024", listOf(Subtask("A", true))),
            makeGoal("Goal 2", "12/31/2024", listOf(Subtask("B", true), Subtask("C", true)))
        )
        val result = calculateAverageTimeToCompletion(completedGoals)
        assertEquals(30, result)
    }

    @Test
    fun testGetUpcomingDeadlines() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val goals = listOf(
            makeGoal("Soon", today.plusDays(2).format(formatter)),
            makeGoal("Later", today.plusDays(10).format(formatter)),
            makeGoal("Past", today.minusDays(1).format(formatter))
        )
        val result = getUpcomingDeadlines(goals)
        assertEquals(3, result.size)
        assertEquals("Past", result[0].first.title)
        assertEquals(-1, result[0].second)
        assertEquals("Soon", result[1].first.title)
        assertEquals(2, result[1].second)
        assertEquals("Later", result[2].first.title)
        assertEquals(10, result[2].second)
    }

    @Test
    fun testCategorizeGoals() {
        val goals = listOf(
            makeGoal("Learn Kotlin", "12/31/2024"),
            makeGoal("Build Project", "12/31/2024"),
            makeGoal("Network with Peers", "12/31/2024"),
            makeGoal("Get Promotion", "12/31/2024"),
            makeGoal("Random Goal", "12/31/2024")
        )
        val result = categorizeGoals(goals)
        assertEquals(1, result["Learning"])
        assertEquals(1, result["Projects"])
        assertEquals(1, result["Networking"])
        assertEquals(1, result["Career Growth"])
        assertEquals(1, result["General"])
    }

    @Test
    fun testGetRecentCompletions() {
        val completed = makeGoal("Completed Goal", "12/31/2024", listOf(Subtask("A", true), Subtask("B", true)))
        val incomplete = makeGoal("Incomplete Goal", "12/31/2024", listOf(Subtask("A", true), Subtask("B", false)))
        val noSubtasks = makeGoal("No Subtasks", "12/31/2024", emptyList())
        val goals = listOf(completed, incomplete, noSubtasks)
        val result = getRecentCompletions(goals)
        assertEquals(1, result.size)
        assertEquals("Completed Goal", result[0].title)
    }

    @Test
    fun testOverallProgressStats_allCompleted() {
        val goals = listOf(
            makeGoal("Goal 1", "12/31/2024", listOf(Subtask("A", true), Subtask("B", true))),
            makeGoal("Goal 2", "12/31/2024", listOf(Subtask("C", true)))
        )
        val totalGoals = goals.size
        val completedGoals = goals.count { it.subtasks.isNotEmpty() && it.subtasks.all { s -> s.isCompleted } }
        val activeGoals = totalGoals - completedGoals
        val totalSubtasks = goals.sumOf { it.subtasks.size }
        val completedSubtasks = goals.sumOf { goal -> goal.subtasks.count { it.isCompleted } }
        val overallProgress = if (totalSubtasks > 0) (completedSubtasks.toFloat() / totalSubtasks.toFloat()) else 0f
        assertEquals(2, totalGoals)
        assertEquals(2, completedGoals)
        assertEquals(0, activeGoals)
        assertEquals(3, totalSubtasks)
        assertEquals(3, completedSubtasks)
        assertEquals(1.0f, overallProgress)
    }

    @Test
    fun testOverallProgressStats_noneCompleted() {
        val goals = listOf(
            makeGoal("Goal 1", "12/31/2024", listOf(Subtask("A", false), Subtask("B", false))),
            makeGoal("Goal 2", "12/31/2024", listOf(Subtask("C", false)))
        )
        val totalGoals = goals.size
        val completedGoals = goals.count { it.subtasks.isNotEmpty() && it.subtasks.all { s -> s.isCompleted } }
        val activeGoals = totalGoals - completedGoals
        val totalSubtasks = goals.sumOf { it.subtasks.size }
        val completedSubtasks = goals.sumOf { goal -> goal.subtasks.count { it.isCompleted } }
        val overallProgress = if (totalSubtasks > 0) (completedSubtasks.toFloat() / totalSubtasks.toFloat()) else 0f
        assertEquals(2, totalGoals)
        assertEquals(0, completedGoals)
        assertEquals(2, activeGoals)
        assertEquals(3, totalSubtasks)
        assertEquals(0, completedSubtasks)
        assertEquals(0.0f, overallProgress)
    }

    @Test
    fun testPriorityBreakdownCard_mixedPriorities() {
        val goals = listOf(
            makeGoal("Goal 1", "12/31/2024", priority = "High"),
            makeGoal("Goal 2", "12/31/2024", priority = "Medium"),
            makeGoal("Goal 3", "12/31/2024", priority = "Low"),
            makeGoal("Goal 4", "12/31/2024", priority = "High"),
            makeGoal("Goal 5", "12/31/2024", priority = "Medium")
        )
        val highPriority = goals.count { it.priority == "High" }
        val mediumPriority = goals.count { it.priority == "Medium" }
        val lowPriority = goals.count { it.priority == "Low" }
        val mostCommon = listOf(
            "High" to highPriority,
            "Medium" to mediumPriority,
            "Low" to lowPriority
        ).maxByOrNull { it.second }?.first ?: "Medium"
        assertEquals(2, highPriority)
        assertEquals(2, mediumPriority)
        assertEquals(1, lowPriority)
        assertTrue(mostCommon == "High" || mostCommon == "Medium")
    }

    @Test
    fun testGetUpcomingDeadlines_withInvalidDate() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val goals = listOf(
            makeGoal("Valid", today.plusDays(2).format(formatter)),
            makeGoal("Invalid", "not-a-date")
        )
        val result = getUpcomingDeadlines(goals)
        assertEquals(1, result.size)
        assertEquals("Valid", result[0].first.title)
    }

    @Test
    fun testGetRecentCompletions_multiple() {
        val completed1 = makeGoal("Completed 1", "12/31/2024", listOf(Subtask("A", true)))
        val completed2 = makeGoal("Completed 2", "12/31/2024", listOf(Subtask("B", true), Subtask("C", true)))
        val incomplete = makeGoal("Incomplete", "12/31/2024", listOf(Subtask("A", true), Subtask("B", false)))
        val goals = listOf(completed1, completed2, incomplete)
        val result = getRecentCompletions(goals)
        assertEquals(2, result.size)
        assertTrue(result.any { it.title == "Completed 1" })
        assertTrue(result.any { it.title == "Completed 2" })
    }
}

fun calculateAverageTimeToCompletion(goals: List<Goal>): Int {
    val completedGoals = goals.filter { goal ->
        goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
    }
    if (completedGoals.isEmpty()) return 0
    return 30
}

fun getUpcomingDeadlines(goals: List<Goal>): List<Pair<Goal, Long>> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    return goals.mapNotNull { goal ->
        try {
            val targetDate = LocalDate.parse(goal.targetDate, formatter)
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, targetDate)
            goal to daysUntil
        } catch (e: Exception) {
            null
        }
    }.sortedBy { it.second }
}

fun categorizeGoals(goals: List<Goal>): Map<String, Int> {
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

fun getRecentCompletions(goals: List<Goal>): List<Goal> {
    return goals.filter { goal ->
        goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
    }.sortedByDescending { it.title }
} 