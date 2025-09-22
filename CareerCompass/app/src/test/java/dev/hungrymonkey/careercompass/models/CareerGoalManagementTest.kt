package dev.hungrymonkey.careercompass.models

import org.junit.Assert.*
import org.junit.Test

class CareerGoalManagementTest {
    private fun makeGoal(
        id: String = "goal1",
        title: String = "Test Goal",
        targetDate: String = "12/31/2024",
        subtasks: List<Subtask> = emptyList(),
        icon: String = "🎯",
        priority: String = "Medium"
    ) = Goal(
        id = id,
        title = title,
        targetDate = targetDate,
        subtasks = subtasks,
        icon = icon,
        priority = priority
    )

    @Test
    fun testAddGoal() {
        val goals = mutableListOf<Goal>()
        val newGoal = makeGoal(id = "goal2", title = "New Goal")
        goals.add(newGoal)
        assertEquals(1, goals.size)
        assertEquals("New Goal", goals[0].title)
    }

    @Test
    fun testEditGoal() {
        val goals = mutableListOf(
            makeGoal(id = "goal1", title = "Original Goal")
        )
        val updatedGoal = goals[0].copy(title = "Edited Goal")
        goals[0] = updatedGoal
        assertEquals("Edited Goal", goals[0].title)
    }

    @Test
    fun testDeleteGoal() {
        val goals = mutableListOf(
            makeGoal(id = "goal1"),
            makeGoal(id = "goal2")
        )
        goals.removeIf { it.id == "goal1" }
        assertEquals(1, goals.size)
        assertEquals("goal2", goals[0].id)
    }

    @Test
    fun testCompleteGoalWithSubtasks() {
        val subtasks = listOf(
            Subtask("Task 1", true),
            Subtask("Task 2", true)
        )
        val goal = makeGoal(subtasks = subtasks)
        val isCompleted = goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
        assertTrue(isCompleted)
    }

    @Test
    fun testIncompleteGoalWithSubtasks() {
        val subtasks = listOf(
            Subtask("Task 1", true),
            Subtask("Task 2", false)
        )
        val goal = makeGoal(subtasks = subtasks)
        val isCompleted = goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
        assertFalse(isCompleted)
    }

    @Test
    fun testCompleteGoalWithoutSubtasks() {
        val goal = makeGoal(subtasks = emptyList())
        val isCompleted = goal.subtasks.isNotEmpty() && goal.subtasks.all { it.isCompleted }
        assertFalse(isCompleted)
    }

    @Test
    fun testRestoreGoal() {
        val subtasks = listOf(
            Subtask("Task 1", true),
            Subtask("Task 2", true)
        )
        val completedGoal = makeGoal(subtasks = subtasks)
        val restoredSubtasks = completedGoal.subtasks.map { it.copy(isCompleted = false) }
        val restoredGoal = completedGoal.copy(subtasks = restoredSubtasks)
        assertTrue(restoredGoal.subtasks.all { !it.isCompleted })
    }

    @Test
    fun testUpdateSubtaskCompletion() {
        val subtasks = listOf(
            Subtask("Task 1", false),
            Subtask("Task 2", false)
        )
        val goal = makeGoal(subtasks = subtasks)
        val updatedSubtasks = goal.subtasks.mapIndexed { i, subtask ->
            if (i == 1) subtask.copy(isCompleted = true) else subtask
        }
        val updatedGoal = goal.copy(subtasks = updatedSubtasks)
        assertFalse(updatedGoal.subtasks[0].isCompleted)
        assertTrue(updatedGoal.subtasks[1].isCompleted)
    }

    @Test
    fun testSortGoalsByTitle() {
        val goals = listOf(
            makeGoal(id = "1", title = "Zebra"),
            makeGoal(id = "2", title = "Apple"),
            makeGoal(id = "3", title = "Monkey")
        )
        val sorted = goals.sortedBy { it.title.lowercase() }
        assertEquals(listOf("Apple", "Monkey", "Zebra"), sorted.map { it.title })
    }

    @Test
    fun testSortGoalsByPriority() {
        val goals = listOf(
            makeGoal(id = "1", priority = "Low"),
            makeGoal(id = "2", priority = "High"),
            makeGoal(id = "3", priority = "Medium")
        )
        val priorityOrder = mapOf("High" to 0, "Medium" to 1, "Low" to 2)
        val sorted = goals.sortedBy { priorityOrder[it.priority] ?: 3 }
        assertEquals(listOf("High", "Medium", "Low"), sorted.map { it.priority })
    }

    @Test
    fun testSortGoalsByDateApproaching() {
        val goals = listOf(
            makeGoal(id = "1", targetDate = "01/01/2025"),
            makeGoal(id = "2", targetDate = "12/31/2024"),
            makeGoal(id = "3", targetDate = "06/15/2024")
        )
        val sorted = goals.sortedBy {
            val parts = it.targetDate.split("/")
            if (parts.size == 3) {
                val month = parts[0].toInt()
                val day = parts[1].toInt()
                val year = parts[2].toInt()
                year * 10000 + month * 100 + day
            } else Int.MAX_VALUE
        }
        assertEquals(listOf("06/15/2024", "12/31/2024", "01/01/2025"), sorted.map { it.targetDate })
    }

    @Test
    fun testSortGoalsByDateFurthest() {
        val goals = listOf(
            makeGoal(id = "1", targetDate = "01/01/2025"),
            makeGoal(id = "2", targetDate = "12/31/2024"),
            makeGoal(id = "3", targetDate = "06/15/2024")
        )
        val sorted = goals.sortedByDescending {
            val parts = it.targetDate.split("/")
            if (parts.size == 3) {
                val month = parts[0].toInt()
                val day = parts[1].toInt()
                val year = parts[2].toInt()
                year * 10000 + month * 100 + day
            } else Int.MIN_VALUE
        }
        assertEquals(listOf("01/01/2025", "12/31/2024", "06/15/2024"), sorted.map { it.targetDate })
    }
} 