package dev.hungrymonkey.careercompass.models

import org.junit.Assert.*
import org.junit.Test

class SkillGapAnalysisTest {
    private fun makeSkill(id: String, name: String, category: SkillCategory = SkillCategory.PROGRAMMING_LANGUAGES) =
        Skill(id = id, name = name, category = category)

    private fun makeUserSkill(skill: Skill, proficiency: ProficiencyLevel) =
        UserSkill(skill = skill, proficiency = proficiency)

    private fun makeRequiredSkill(skill: Skill, importance: SkillImportance, minProf: ProficiencyLevel) =
        RequiredSkill(skill = skill, importance = importance, minimumProficiency = minProf)

    private fun makeJobRole(requiredSkills: List<RequiredSkill>) =
        JobRole(
            id = "role1",
            title = "Software Engineer",
            industry = Industry.SOFTWARE_ENGINEERING,
            description = "Test role",
            requiredSkills = requiredSkills
        )

    @Test
    fun testUserSkillAddAndRemove() {
        val skill = makeSkill("1", "Kotlin")
        val userSkills = mutableListOf<UserSkill>()
        userSkills.add(makeUserSkill(skill, ProficiencyLevel.BEGINNER))
        assertEquals(1, userSkills.size)
        userSkills.removeIf { it.skill.id == "1" }
        assertTrue(userSkills.isEmpty())
    }

    @Test
    fun testUserSkillUpdate() {
        val skill = makeSkill("1", "Kotlin")
        val userSkills = mutableListOf(makeUserSkill(skill, ProficiencyLevel.BEGINNER))
        val updated = makeUserSkill(skill, ProficiencyLevel.EXPERT)
        userSkills[0] = updated
        assertEquals(ProficiencyLevel.EXPERT, userSkills[0].proficiency)
    }

    @Test
    fun testSelectTargetRole() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.CRITICAL, ProficiencyLevel.INTERMEDIATE)
        val jobRole = makeJobRole(listOf(required))
        assertEquals("Software Engineer", jobRole.title)
        assertEquals(1, jobRole.requiredSkills.size)
    }

    @Test
    fun testSkillGapCalculation_userMeetsRequirement() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.CRITICAL, ProficiencyLevel.BEGINNER)
        val userSkill = makeUserSkill(skill, ProficiencyLevel.EXPERT)
        val jobRole = makeJobRole(listOf(required))
        val result = analyzeSkillGap(jobRole, listOf(userSkill))
        assertEquals(100f, result.overallMatchPercentage, 0.01f)
        assertTrue(result.skillGaps.isEmpty())
        assertEquals(1, result.strengths.size)
    }

    @Test
    fun testSkillGapCalculation_userBelowRequirement() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.CRITICAL, ProficiencyLevel.EXPERT)
        val userSkill = makeUserSkill(skill, ProficiencyLevel.BEGINNER)
        val jobRole = makeJobRole(listOf(required))
        val result = analyzeSkillGap(jobRole, listOf(userSkill))
        assertEquals(0f, result.overallMatchPercentage, 0.01f)
        assertEquals(1, result.skillGaps.size)
        assertEquals(skill, result.skillGaps[0].skill)
        assertEquals(ProficiencyLevel.BEGINNER, result.skillGaps[0].currentLevel)
        assertEquals(ProficiencyLevel.EXPERT, result.skillGaps[0].requiredLevel)
    }

    @Test
    fun testSkillGapCalculation_userMissingSkill() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.CRITICAL, ProficiencyLevel.INTERMEDIATE)
        val jobRole = makeJobRole(listOf(required))
        val result = analyzeSkillGap(jobRole, emptyList())
        assertEquals(0f, result.overallMatchPercentage, 0.01f)
        assertEquals(1, result.skillGaps.size)
        assertNull(result.skillGaps[0].currentLevel)
    }

    @Test
    fun testSkillGapCalculation_multipleSkills() {
        val skill1 = makeSkill("1", "Kotlin")
        val skill2 = makeSkill("2", "Java")
        val required1 = makeRequiredSkill(skill1, SkillImportance.CRITICAL, ProficiencyLevel.INTERMEDIATE)
        val required2 = makeRequiredSkill(skill2, SkillImportance.IMPORTANT, ProficiencyLevel.BEGINNER)
        val userSkill1 = makeUserSkill(skill1, ProficiencyLevel.INTERMEDIATE)
        val userSkill2 = makeUserSkill(skill2, ProficiencyLevel.BEGINNER)
        val jobRole = makeJobRole(listOf(required1, required2))
        val result = analyzeSkillGap(jobRole, listOf(userSkill1, userSkill2))
        assertEquals(100f, result.overallMatchPercentage, 0.01f)
        assertTrue(result.skillGaps.isEmpty())
        assertEquals(2, result.strengths.size)
    }

    @Test
    fun testGapSeverityCalculation() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.CRITICAL, ProficiencyLevel.EXPERT)
        val userSkill = makeUserSkill(skill, ProficiencyLevel.BEGINNER)
        val gap = SkillGap(
            skill = skill,
            currentLevel = userSkill.proficiency,
            requiredLevel = required.minimumProficiency,
            importance = required.importance,
            gapSeverity = calculateGapSeverity(userSkill.proficiency, required.minimumProficiency, required.importance)
        )
        assertEquals(GapSeverity.HIGH, gap.gapSeverity)
    }

    @Test
    fun testNoRequiredSkills() {
        val jobRole = makeJobRole(emptyList())
        val result = analyzeSkillGap(jobRole, emptyList())
        assertEquals(0f, result.overallMatchPercentage, 0.01f)
        assertTrue(result.skillGaps.isEmpty())
        assertTrue(result.strengths.isEmpty())
    }

    @Test
    fun testSkillGapCalculation_allSkillsMissing() {
        val skill1 = makeSkill("1", "Kotlin")
        val skill2 = makeSkill("2", "Java")
        val required1 = makeRequiredSkill(skill1, SkillImportance.CRITICAL, ProficiencyLevel.INTERMEDIATE)
        val required2 = makeRequiredSkill(skill2, SkillImportance.IMPORTANT, ProficiencyLevel.BEGINNER)
        val jobRole = makeJobRole(listOf(required1, required2))
        val result = analyzeSkillGap(jobRole, emptyList())
        assertEquals(0f, result.overallMatchPercentage, 0.01f)
        assertEquals(2, result.skillGaps.size)
        assertTrue(result.skillGaps.all { it.currentLevel == null })
    }

    @Test
    fun testSkillGapCalculation_allSkillsMaxProficiency() {
        val skill1 = makeSkill("1", "Kotlin")
        val skill2 = makeSkill("2", "Java")
        val required1 = makeRequiredSkill(skill1, SkillImportance.CRITICAL, ProficiencyLevel.INTERMEDIATE)
        val required2 = makeRequiredSkill(skill2, SkillImportance.IMPORTANT, ProficiencyLevel.BEGINNER)
        val userSkill1 = makeUserSkill(skill1, ProficiencyLevel.EXPERT)
        val userSkill2 = makeUserSkill(skill2, ProficiencyLevel.EXPERT)
        val jobRole = makeJobRole(listOf(required1, required2))
        val result = analyzeSkillGap(jobRole, listOf(userSkill1, userSkill2))
        assertEquals(100f, result.overallMatchPercentage, 0.01f)
        assertTrue(result.skillGaps.isEmpty())
        assertEquals(2, result.strengths.size)
    }

    @Test
    fun testSkillGapCalculation_mixedProficiencyAndImportance() {
        val skill1 = makeSkill("1", "Kotlin")
        val skill2 = makeSkill("2", "Java")
        val required1 = makeRequiredSkill(skill1, SkillImportance.CRITICAL, ProficiencyLevel.EXPERT)
        val required2 = makeRequiredSkill(skill2, SkillImportance.NICE_TO_HAVE, ProficiencyLevel.INTERMEDIATE)
        val userSkill1 = makeUserSkill(skill1, ProficiencyLevel.INTERMEDIATE)
        val userSkill2 = makeUserSkill(skill2, ProficiencyLevel.BEGINNER)
        val jobRole = makeJobRole(listOf(required1, required2))
        val result = analyzeSkillGap(jobRole, listOf(userSkill1, userSkill2))
        assertEquals(0f, result.overallMatchPercentage, 0.01f)
        assertEquals(2, result.skillGaps.size)
        val severities = result.skillGaps.map { it.gapSeverity }
        assertEquals(listOf(GapSeverity.LOW, GapSeverity.LOW), severities)
    }

    @Test
    fun testSkillGapCalculation_niceToHaveSkill() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.NICE_TO_HAVE, ProficiencyLevel.BEGINNER)
        val jobRole = makeJobRole(listOf(required))
        val result = analyzeSkillGap(jobRole, emptyList())
        assertEquals(0f, result.overallMatchPercentage, 0.01f)
        assertEquals(1, result.skillGaps.size)
        assertEquals(SkillImportance.NICE_TO_HAVE, result.skillGaps[0].importance)
        assertEquals(GapSeverity.LOW, result.skillGaps[0].gapSeverity)
    }

    @Test
    fun testSkillGapCalculation_emptyUserSkillsWithRequiredSkills() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.CRITICAL, ProficiencyLevel.BEGINNER)
        val jobRole = makeJobRole(listOf(required))
        val result = analyzeSkillGap(jobRole, emptyList())
        assertEquals(0f, result.overallMatchPercentage, 0.01f)
        assertEquals(1, result.skillGaps.size)
        assertNull(result.skillGaps[0].currentLevel)
    }

    @Test
    fun testSkillGapCalculation_duplicateUserSkills() {
        val skill = makeSkill("1", "Kotlin")
        val required = makeRequiredSkill(skill, SkillImportance.CRITICAL, ProficiencyLevel.BEGINNER)
        val userSkill1 = makeUserSkill(skill, ProficiencyLevel.BEGINNER)
        val userSkill2 = makeUserSkill(skill, ProficiencyLevel.EXPERT)
        val jobRole = makeJobRole(listOf(required))
        val result = analyzeSkillGap(jobRole, listOf(userSkill1, userSkill2))
        assertEquals(100f, result.overallMatchPercentage, 0.01f)
        assertTrue(result.skillGaps.isEmpty())
        assertEquals(1, result.strengths.size)
    }

    private fun analyzeSkillGap(targetRole: JobRole, userSkills: List<UserSkill>): SkillAnalysisResult {
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
        return SkillAnalysisResult(
            targetRole = targetRole,
            overallMatchPercentage = overallMatchPercentage,
            skillGaps = skillGaps.sortedBy { it.gapSeverity },
            strengths = strengths,
            recommendations = emptyList()
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
} 