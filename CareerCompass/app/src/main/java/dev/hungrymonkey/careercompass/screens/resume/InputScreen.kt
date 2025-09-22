package dev.hungrymonkey.careercompass.screens.resume

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.Education
import dev.hungrymonkey.careercompass.models.Experience
import dev.hungrymonkey.careercompass.models.Project
import dev.hungrymonkey.careercompass.models.ResumeData
import dev.hungrymonkey.careercompass.models.Skills
import dev.hungrymonkey.careercompass.network.ResumeRepository
import dev.hungrymonkey.careercompass.viewmodel.GeneralDetailsViewModel
import dev.hungrymonkey.careercompass.viewmodel.ResumeViewModel
import kotlinx.coroutines.launch
import java.time.Year
import java.time.YearMonth
import dev.hungrymonkey.careercompass.utils.containsLatexSpecialChars
import dev.hungrymonkey.careercompass.utils.getLatexSpecialCharError

@Composable
fun ResumeInputScreen(
    navController: NavHostController,
    templateName: String,
    resumeId: String? = null,
    viewModel: ResumeViewModel = viewModel(),
    generalDetailsViewModel: GeneralDetailsViewModel = viewModel()
) {
    val context = LocalContext.current
    val resumes by viewModel.resumes.collectAsState()
    val generalDetails by generalDetailsViewModel.generalDetails.collectAsState()
    var loaded by remember { mutableStateOf(false) }
    
    val resumeRepository = remember { ResumeRepository() }
    val scope = rememberCoroutineScope()
    
    var isPreviewLoading by remember { mutableStateOf(false) }
    var isExportLoading by remember { mutableStateOf(false) }
    var showTemplateDrawer by remember { mutableStateOf(false) }
    var currentTemplateName by remember { mutableStateOf(templateName) }
    var currentResumeId by remember { mutableStateOf(resumeId) }

    var resumeName by remember { mutableStateOf("") }
    var resumeNameError by remember { mutableStateOf<String?>(null) }
    var fullName by remember { mutableStateOf("") }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var website1 by remember { mutableStateOf("") }
    var website1Error by remember { mutableStateOf<String?>(null) }
    var website2 by remember { mutableStateOf("") }
    var website2Error by remember { mutableStateOf<String?>(null) }
    var educationList by remember { mutableStateOf(listOf<Education>()) }
    var experienceList by remember { mutableStateOf(listOf<Experience>()) }
    var projectList by remember { mutableStateOf(listOf<Project>()) }
    var skills by remember { mutableStateOf(Skills("", "", "", "")) }
    var showEducationInput by remember { mutableStateOf(false) }
    var editingEducationIndex by remember { mutableStateOf<Int?>(null) }
    var educationEditInitial by remember { mutableStateOf<Education?>(null) }
    var showExperienceInput by remember { mutableStateOf(false) }
    var editingExperienceIndex by remember { mutableStateOf<Int?>(null) }
    var experienceEditInitial by remember { mutableStateOf<Experience?>(null) }
    var showProjectInput by remember { mutableStateOf(false) }
    var editingProjectIndex by remember { mutableStateOf<Int?>(null) }
    var projectEditInitial by remember { mutableStateOf<Project?>(null) }
    var showSkillsInput by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var originalResumeData by remember { mutableStateOf<ResumeData?>(null) }
    var hasUnsavedFormChanges by remember { mutableStateOf(false) }

    LaunchedEffect(resumeId, resumes) {
        if (!loaded && resumeId != null) {
            resumes.find { it.id == resumeId }?.let { resume ->
                resumeName = resume.resumeName
                fullName = resume.fullName
                phone = resume.phone ?: ""
                email = resume.email ?: ""
                website1 = resume.website1 ?: ""
                website2 = resume.website2 ?: ""
                educationList = resume.education
                experienceList = resume.experience
                projectList = resume.projects
                skills = resume.skills
                currentTemplateName = resume.templateName
                loaded = true
                originalResumeData = resume
            }
        }
    }
    
    fun resetAllMetadata() {
        resumeName = ""
        fullName = ""
        phone = ""
        email = ""
        website1 = ""
        website2 = ""
        educationList = listOf()
        experienceList = listOf()
        projectList = listOf()
        skills = Skills("", "", "", "")
        
        resumeNameError = null
        fullNameError = null
        phoneError = null
        emailError = null
        website1Error = null
        website2Error = null
    }

    fun validatePersonalInfo(): Boolean {
        var valid = true
        resumeNameError = if (resumeName.isBlank()) { 
            valid = false; "Resume name is required" 
        } else if (containsLatexSpecialChars(resumeName)) {
            valid = false; getLatexSpecialCharError(resumeName)
        } else null
        
        fullNameError = if (fullName.isBlank()) {
            valid = false; "Full name is required"
        } else if (fullName.matches(Regex(".*\\d.*"))) {
            valid = false; "Full name cannot contain numbers"
        } else if (containsLatexSpecialChars(fullName)) {
            valid = false; getLatexSpecialCharError(fullName)
        } else null
        
        phoneError = if (phone.isNotBlank() && !phone.matches(Regex("^\\+?[0-9]{7,15}"))) {
            valid = false; "Invalid phone number"
        } else null
        emailError = if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            valid = false; "Invalid email format"
        } else null
        website1Error = if (website1.isNotBlank() && !website1.startsWith("www")) {
            valid = false; "Website must start with www"
        } else null
        website2Error = if (website2.isNotBlank() && !website2.startsWith("www")) {
            valid = false; "Website must start with www"
        } else null
        return valid
    }
    
    fun hasUnsavedChanges(): Boolean {
        val currentData = ResumeData(
            id = resumeId ?: "",
            resumeName = resumeName,
            fullName = fullName,
            phone = phone,
            email = email,
            website1 = website1,
            website2 = website2,
            education = educationList,
            experience = experienceList,
            projects = projectList,
            skills = skills,
            templateName = currentTemplateName
        )
        
        return originalResumeData?.let { original ->
            original.resumeName != currentData.resumeName ||
            original.fullName != currentData.fullName ||
            original.phone != currentData.phone ||
            original.email != currentData.email ||
            original.website1 != currentData.website1 ||
            original.website2 != currentData.website2 ||
            original.education != currentData.education ||
            original.experience != currentData.experience ||
            original.projects != currentData.projects ||
            original.skills != currentData.skills ||
            original.templateName != currentData.templateName
        } ?: (resumeId == null && (resumeName.isNotBlank() || fullName.isNotBlank() || phone.isNotBlank() || 
              email.isNotBlank() || website1.isNotBlank() || website2.isNotBlank() || 
              educationList.isNotEmpty() || experienceList.isNotEmpty() || 
              projectList.isNotEmpty() || skills.languages.isNotBlank())) || hasUnsavedFormChanges
    }
    
    BackHandler {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = { TopBar(title = "Resume Details") },
        bottomBar = { NavigationBar(navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = resumeName,
                onValueChange = { resumeName = it },
                label = { Text("Resume Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = resumeNameError != null,
                supportingText = { resumeNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Current Template",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = currentTemplateName.replace(Regex("([A-Za-z]+)(\\d+)"), "$1 $2"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { showTemplateDrawer = true }
                    ) {
                        Text("Change")
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Personal Information",
                    style = MaterialTheme.typography.titleMedium
                )
                generalDetails?.let { details ->
                    TextButton(
                        onClick = {
                            fullName = details.fullName
                            email = details.email
                            phone = details.phone
                            website1 = details.website1
                            website2 = details.website2
                            Toast.makeText(context, "Loaded from general details", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Load from General Details")
                    }
                }
            }
            
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                placeholder = { Text("John Doe", color = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.fillMaxWidth(),
                isError = fullNameError != null,
                supportingText = { fullNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                placeholder = { Text("1112223333", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError != null,
                supportingText = { phoneError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("johndoe@gmail.com", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            OutlinedTextField(
                value = website1,
                onValueChange = { website1 = it },
                label = { Text("Website 1") },
                placeholder = { Text("ex: www.linkedin.com/in/johndoe", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                isError = website1Error != null,
                supportingText = { website1Error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            OutlinedTextField(
                value = website2,
                onValueChange = { website2 = it },
                label = { Text("Website 2") },
                placeholder = { Text("ex: www.github.com/johndoe", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                isError = website2Error != null,
                supportingText = { website2Error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            Text("Education", style = MaterialTheme.typography.titleMedium)
            educationList.forEachIndexed { idx, it ->
                val endDate = it.endDate?.let { d -> "${d.monthValue}/${d.year}" } ?: "Present"
                Text(
                    "${it.institution}, ${it.degree} (${it.startDate.monthValue}/${it.startDate.year} - $endDate)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        editingEducationIndex = idx
                        educationEditInitial = it
                        showEducationInput = true
                    }
                )
            }
            if (showEducationInput) {
                EducationInputForm(
                    initial = educationEditInitial,
                    onDone = { edu: Education ->
                        if (edu.isValid()) {
                            if (editingEducationIndex != null) {
                                educationList = educationList.toMutableList().also { list -> list[editingEducationIndex!!] = edu }
                            } else {
                                educationList = educationList.plus(edu)
                            }
                            showEducationInput = false
                            editingEducationIndex = null
                            educationEditInitial = null
                            hasUnsavedFormChanges = false
                        } else {
                            Toast.makeText(context, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = {
                        showEducationInput = false
                        editingEducationIndex = null
                        educationEditInitial = null
                        hasUnsavedFormChanges = false
                    },
                    onChanged = { hasChanges ->
                        hasUnsavedFormChanges = hasChanges
                    }
                )
            }
            Button(
                onClick = {
                    editingEducationIndex = null
                    educationEditInitial = null
                    showEducationInput = true
                }
            ) { Text("Add New Education") }
            Text("Experience", style = MaterialTheme.typography.titleMedium)
            experienceList.forEachIndexed { idx, it ->
                val endDate = it.endDate?.let { d -> "${d.monthValue}/${d.year}" } ?: "Present"
                Text(
                    "${it.title} at ${it.company} (${it.startDate.monthValue}/${it.startDate.year} - $endDate)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        editingExperienceIndex = idx
                        experienceEditInitial = it
                        showExperienceInput = true
                    }
                )
            }
            if (showExperienceInput) {
                ExperienceInputForm(
                    initial = experienceEditInitial,
                    onDone = { exp: Experience ->
                        if (exp.isValid()) {
                            if (editingExperienceIndex != null) {
                                experienceList = experienceList.toMutableList().also { list -> list[editingExperienceIndex!!] = exp }
                            } else {
                                experienceList = experienceList.plus(exp)
                            }
                            showExperienceInput = false
                            editingExperienceIndex = null
                            experienceEditInitial = null
                            hasUnsavedFormChanges = false
                        } else {
                            Toast.makeText(context, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = {
                        showExperienceInput = false
                        editingExperienceIndex = null
                        experienceEditInitial = null
                        hasUnsavedFormChanges = false
                    },
                    onChanged = { hasChanges ->
                        hasUnsavedFormChanges = hasChanges
                    }
                )
            }
            Button(
                onClick = {
                    editingExperienceIndex = null
                    experienceEditInitial = null
                    showExperienceInput = true
                }
            ) { Text("Add New Experience") }
            Text("Projects", style = MaterialTheme.typography.titleMedium)
            projectList.forEachIndexed { idx, it ->
                Text(
                    "${it.title} (${it.date.value})",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        editingProjectIndex = idx
                        projectEditInitial = it
                        showProjectInput = true
                    }
                )
            }
            if (showProjectInput) {
                ProjectInputForm(
                    initial = projectEditInitial,
                    onDone = { proj: Project ->
                        if (proj.isValid()) {
                            if (editingProjectIndex != null) {
                                projectList = projectList.toMutableList().also { list -> list[editingProjectIndex!!] = proj }
                            } else {
                                projectList = projectList.plus(proj)
                            }
                            showProjectInput = false
                            editingProjectIndex = null
                            projectEditInitial = null
                            hasUnsavedFormChanges = false
                        } else {
                            Toast.makeText(context, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = {
                        showProjectInput = false
                        editingProjectIndex = null
                        projectEditInitial = null
                        hasUnsavedFormChanges = false
                    },
                    onChanged = { hasChanges ->
                        hasUnsavedFormChanges = hasChanges
                    }
                )
            }
            Button(
                onClick = {
                    editingProjectIndex = null
                    projectEditInitial = null
                    showProjectInput = true
                }
            ) { Text("Add New Project") }
            Text("Skills", style = MaterialTheme.typography.titleMedium)
            if (showSkillsInput) {
                SkillsInputForm(
                    initial = skills,
                    onDone = { sk: Skills ->
                        if (sk.isValid()) {
                            skills = sk
                            showSkillsInput = false
                            hasUnsavedFormChanges = false
                        } else {
                            Toast.makeText(context, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = { 
                        showSkillsInput = false
                        hasUnsavedFormChanges = false
                    },
                    onChanged = { hasChanges ->
                        hasUnsavedFormChanges = hasChanges
                    }
                )
            } else {
                Text("Languages: ${skills.languages}")
                Text("Frameworks: ${skills.frameworks}")
                Text("Tools: ${skills.tools}")
                Text("Libraries: ${skills.libraries}")
            }
            Button(onClick = { showSkillsInput = true }) { Text("Edit Skills") }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val validPersonal = validatePersonalInfo()
                        val validSections = educationList.isNotEmpty() && experienceList.isNotEmpty() && projectList.isNotEmpty() && skills.isValid()
                        if (!validPersonal || !validSections) {
                            Toast.makeText(context, "Please fix errors and fill all required fields.", Toast.LENGTH_SHORT).show()
                        } else {
                            val tempResume = ResumeData(
                                id = currentResumeId ?: "",
                                resumeName = resumeName,
                                fullName = fullName,
                                phone = phone,
                                email = email,
                                website1 = website1,
                                website2 = website2,
                                education = educationList,
                                experience = experienceList,
                                projects = projectList,
                                skills = skills,
                                templateName = currentTemplateName
                            )
                            scope.launch {
                                try {
                                    if (currentResumeId == null) {
                                        val result = viewModel.saveResume(tempResume)
                                        result.fold(
                                            onSuccess = { savedId ->
                                                currentResumeId = savedId
                                                Toast.makeText(context, "Resume saved!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("resume_builder") {
                                                    popUpTo("resume_builder") { inclusive = false }
                                                }
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, "Failed to save resume: ${error.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        val result = viewModel.updateResume(tempResume)
                                        result.fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Resume updated!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("resume_builder") {
                                                    popUpTo("resume_builder") { inclusive = false }
                                                }
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, "Failed to update resume: ${error.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (currentResumeId == null) {
                        Text("Save Draft")
                    } else {
                        Text("Update Draft")
                    }
                }
                Spacer(Modifier.height(0.dp))
                Button(
                    onClick = {
                        val validPersonal = validatePersonalInfo()
                        val validSections = educationList.isNotEmpty() && experienceList.isNotEmpty() && projectList.isNotEmpty() && skills.isValid()
                        if (!validPersonal || !validSections) {
                            Toast.makeText(context, "Please fix errors and fill all required fields.", Toast.LENGTH_SHORT).show()
                        } else {
                            val tempResume = ResumeData(
                                id = currentResumeId ?: "",
                                resumeName = resumeName,
                                fullName = fullName,
                                phone = phone,
                                email = email,
                                website1 = website1,
                                website2 = website2,
                                education = educationList,
                                experience = experienceList,
                                projects = projectList,
                                skills = skills,
                                templateName = currentTemplateName
                            )
                            
                            val currentUser = Firebase.auth.currentUser
                            if (currentUser == null) {
                                Toast.makeText(context, "Please log in to preview resume", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isPreviewLoading = true
                            scope.launch {
                                try {
                                    val saveResult = if (currentResumeId == null) {
                                        viewModel.saveResume(tempResume)
                                    } else {
                                        viewModel.updateResume(tempResume).map { currentResumeId!! }
                                    }
                                    
                                    saveResult.fold(
                                        onSuccess = { savedResumeId ->
                                            if (currentResumeId == null) {
                                                currentResumeId = savedResumeId
                                            }
                                            
                                            val result = resumeRepository.generateAndPreviewResume(
                                                userId = currentUser.uid,
                                                resumeId = savedResumeId,
                                                context = context
                                            )
                                            
                                            result.fold(
                                                onSuccess = { uri ->
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/pdf")
                                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    }
                                                    
                                                    try {
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "No PDF viewer found. Please install a PDF reader.", Toast.LENGTH_LONG).show()
                                                    }
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(context, "Failed to generate preview: ${error.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, "Failed to save resume: ${error.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } finally {
                                    isPreviewLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPreviewLoading
                ) {
                    if (isPreviewLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isPreviewLoading) "Generating..." else "Preview Resume")
                }
                Spacer(Modifier.height(0.dp))
                Button(
                    onClick = {
                        val validPersonal = validatePersonalInfo()
                        val validSections = educationList.isNotEmpty() && experienceList.isNotEmpty() && projectList.isNotEmpty() && skills.isValid()
                        if (!validPersonal || !validSections) {
                            Toast.makeText(context, "Please fix errors and fill all required fields.", Toast.LENGTH_SHORT).show()
                        } else {
                            val tempResume = ResumeData(
                                id = currentResumeId ?: "",
                                resumeName = resumeName,
                                fullName = fullName,
                                phone = phone,
                                email = email,
                                website1 = website1,
                                website2 = website2,
                                education = educationList,
                                experience = experienceList,
                                projects = projectList,
                                skills = skills,
                                templateName = currentTemplateName
                            )
                            
                            val currentUser = Firebase.auth.currentUser
                            if (currentUser == null) {
                                Toast.makeText(context, "Please log in to export resume", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isExportLoading = true
                            scope.launch {
                                try {
                                    val saveResult = if (currentResumeId == null) {
                                        viewModel.saveResume(tempResume)
                                    } else {
                                        viewModel.updateResume(tempResume).map { currentResumeId!! }
                                    }
                                    
                                    saveResult.fold(
                                        onSuccess = { savedResumeId ->
                                            if (currentResumeId == null) {
                                                currentResumeId = savedResumeId
                                            }
                                            
                                            val result = resumeRepository.generateAndDownloadResume(
                                                userId = currentUser.uid,
                                                resumeId = savedResumeId,
                                                context = context
                                            )
                                            
                                            result.fold(
                                                onSuccess = { filePath ->
                                                    Toast.makeText(context, "Resume saved to Downloads folder!", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(context, "Failed to export resume: ${error.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, "Failed to save resume: ${error.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } finally {
                                    isExportLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExportLoading
                ) {
                    if (isExportLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isExportLoading) "Exporting..." else "Export Resume as PDF")
                }
                Spacer(Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {                    
                    OutlinedButton(
                        onClick = {
                            resetAllMetadata()
                            Toast.makeText(context, "All fields cleared!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset All Fields")
                    }
                }
            }
        }
        
        if (showTemplateDrawer) {
            TemplateSelectionDrawer(
                onTemplateSelected = { newTemplateName ->
                    currentTemplateName = newTemplateName
                    showTemplateDrawer = false
                },
                onDismiss = {
                    showTemplateDrawer = false
                }
            )
        }
        
        if (showUnsavedChangesDialog) {
            UnsavedChangesDialog(
                onDiscardChanges = {
                    showUnsavedChangesDialog = false
                    navController.navigateUp()
                },
                onKeepEditing = {
                    showUnsavedChangesDialog = false
                }
            )
        }
    }
}

@Composable
fun EducationInputForm(
    initial: Education? = null,
    onDone: (Education) -> Unit,
    onCancel: () -> Unit,
    onChanged: ((Boolean) -> Unit)? = null
) {
    var institution by remember { mutableStateOf(initial?.institution ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var degree by remember { mutableStateOf(initial?.degree ?: "") }
    var major by remember { mutableStateOf(initial?.major ?: "") }
    var minor by remember { mutableStateOf(initial?.minor ?: "") }
    var gpa by remember { mutableStateOf(initial?.gpa ?: "") }
    var specialization by remember { mutableStateOf(initial?.specialization ?: "") }
    var startMonth by remember { mutableIntStateOf(initial?.startDate?.monthValue ?: YearMonth.now().monthValue) }
    var startYear by remember { mutableIntStateOf(initial?.startDate?.year ?: YearMonth.now().year) }
    var endMonth by remember { mutableIntStateOf(initial?.endDate?.monthValue ?: YearMonth.now().monthValue) }
    var endYear by remember { mutableIntStateOf(initial?.endDate?.year ?: YearMonth.now().year) }

    var institutionError by remember { mutableStateOf<String?>(null) }
    var degreeError by remember { mutableStateOf<String?>(null) }
    var gpaError by remember { mutableStateOf<String?>(null) }

    val hasChanges by remember {
        derivedStateOf {
            institution != (initial?.institution ?: "") ||
            location != (initial?.location ?: "") ||
            degree != (initial?.degree ?: "") ||
            major != (initial?.major ?: "") ||
            minor != (initial?.minor ?: "") ||
            gpa != (initial?.gpa ?: "") ||
            specialization != (initial?.specialization ?: "") ||
            startMonth != (initial?.startDate?.monthValue ?: YearMonth.now().monthValue) ||
            startYear != (initial?.startDate?.year ?: YearMonth.now().year) ||
            endMonth != (initial?.endDate?.monthValue ?: YearMonth.now().monthValue) ||
            endYear != (initial?.endDate?.year ?: YearMonth.now().year)
        }
    }

    LaunchedEffect(hasChanges) {
        onChanged?.invoke(hasChanges)
    }

    fun validate(): Boolean {
        var valid = true
        institutionError = if (institution.isBlank()) { 
            valid = false; "Institution is required" 
        } else if (containsLatexSpecialChars(institution)) {
            valid = false; getLatexSpecialCharError(institution)
        } else null
        
        degreeError = if (degree.isBlank()) { 
            valid = false; "Degree is required" 
        } else if (containsLatexSpecialChars(degree)) {
            valid = false; getLatexSpecialCharError(degree)
        } else null
        
        gpaError = if (gpa.isNotBlank() && gpa.toDoubleOrNull() == null) { valid = false; "GPA must be a number" } else null
        return valid
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = institution, onValueChange = { institution = it }, label = { Text("Institution") }, isError = institutionError != null, supportingText = { institutionError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location (optional)") })
        OutlinedTextField(value = degree, onValueChange = { degree = it }, label = { Text("Degree") }, placeholder = { Text("BASc, BS, BSE, etc", color = MaterialTheme.colorScheme.outline) }, isError = degreeError != null, supportingText = { degreeError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = major, onValueChange = { major = it }, label = { Text("Major (optional)") })
        OutlinedTextField(value = minor, onValueChange = { minor = it }, label = { Text("Minor (optional)") })
        OutlinedTextField(value = gpa, onValueChange = { gpa = it }, label = { Text("GPA (optional)") }, isError = gpaError != null, supportingText = { gpaError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = specialization, onValueChange = { specialization = it }, label = { Text("Specialization (optional)") })
        Row {
            MonthYearPicker(
                "Start",
                startMonth,
                startYear,
                onMonthChange = { startMonth = it },
                onYearChange = { startYear = it }
            )
            Spacer(Modifier.width(8.dp))
            MonthYearPicker(
                "End",
                endMonth,
                endYear,
                onMonthChange = { endMonth = it },
                onYearChange = { endYear = it },
                years = ((startYear + 10) downTo (startYear - 60)).toList(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (validate()) {
                    onDone(
                        Education(
                            institution = institution,
                            location = location.ifBlank { null },
                            degree = degree,
                            major = major.ifBlank { null },
                            minor = minor.ifBlank { null },
                            gpa = gpa.ifBlank { null },
                            specialization = specialization.ifBlank { null },
                            startDate = YearMonth.of(startYear, startMonth),
                            endDate = YearMonth.of(endYear, endMonth)
                        )
                    )
                }
            }) { Text("Done") }
            OutlinedButton(onClick = { onCancel() }) { Text("Cancel") }
        }
    }
}

@Composable
fun ExperienceInputForm(
    initial: Experience? = null,
    onDone: (Experience) -> Unit,
    onCancel: () -> Unit,
    onChanged: ((Boolean) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var company by remember { mutableStateOf(initial?.company ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var startMonth by remember { mutableIntStateOf(initial?.startDate?.monthValue ?: YearMonth.now().monthValue) }
    var startYear by remember { mutableIntStateOf(initial?.startDate?.year ?: YearMonth.now().year) }
    var endMonth by remember { mutableIntStateOf(initial?.endDate?.monthValue ?: YearMonth.now().monthValue) }
    var endYear by remember { mutableIntStateOf(initial?.endDate?.year ?: YearMonth.now().year) }
    var currentlyWorking by remember { mutableStateOf(initial?.currentlyWorking == true) }
    
    var bullets by remember { 
        mutableStateOf(
            if (initial?.bullets?.isNotEmpty() == true) {
                initial.bullets.toMutableList().apply { add("") }
            } else {
                mutableListOf("")
            }
        )
    }

    var titleError by remember { mutableStateOf<String?>(null) }
    var companyError by remember { mutableStateOf<String?>(null) }
    var bulletsError by remember { mutableStateOf<String?>(null) }
    var bulletValidationErrors by remember { mutableStateOf(mutableMapOf<Int, String>()) }

    val initialBullets = remember { initial?.bullets ?: emptyList() }
    val hasChanges by remember {
        derivedStateOf {
            title != (initial?.title ?: "") ||
            company != (initial?.company ?: "") ||
            location != (initial?.location ?: "") ||
            startMonth != (initial?.startDate?.monthValue ?: YearMonth.now().monthValue) ||
            startYear != (initial?.startDate?.year ?: YearMonth.now().year) ||
            endMonth != (initial?.endDate?.monthValue ?: YearMonth.now().monthValue) ||
            endYear != (initial?.endDate?.year ?: YearMonth.now().year) ||
            currentlyWorking != (initial?.currentlyWorking == true) ||
            bullets.filter { it.isNotBlank() } != initialBullets
        }
    }

    LaunchedEffect(hasChanges) {
        onChanged?.invoke(hasChanges)
    }

    fun validate(): Boolean {
        var valid = true
        titleError = if (title.isBlank()) { 
            valid = false; "Title is required" 
        } else if (containsLatexSpecialChars(title)) {
            valid = false; getLatexSpecialCharError(title)
        } else null
        
        companyError = if (company.isBlank()) { 
            valid = false; "Company is required" 
        } else if (containsLatexSpecialChars(company)) {
            valid = false; getLatexSpecialCharError(company)
        } else null
        
        val nonEmptyBullets = bullets.filter { it.isNotBlank() }
        bulletsError = if (nonEmptyBullets.isEmpty()) { 
            valid = false; "At least one bullet is required" 
        } else null
        
        val newBulletValidationErrors = mutableMapOf<Int, String>()
        bullets.forEachIndexed { index, bullet ->
            if (bullet.isNotBlank() && containsLatexSpecialChars(bullet)) {
                newBulletValidationErrors[index] = getLatexSpecialCharError(bullet) ?: "Contains special characters"
                valid = false
            }
        }
        bulletValidationErrors = newBulletValidationErrors
        return valid
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, isError = titleError != null, supportingText = { titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, isError = companyError != null, supportingText = { companyError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location (optional)") })
        Row {
            MonthYearPicker("Start", startMonth, startYear, onMonthChange = { startMonth = it }, onYearChange = { startYear = it })
            Spacer(Modifier.width(8.dp))
            if (!currentlyWorking) {
                MonthYearPicker("End", endMonth, endYear, onMonthChange = { endMonth = it }, onYearChange = { endYear = it })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = currentlyWorking, onCheckedChange = { currentlyWorking = it })
            Text("Currently working here")
        }
        
        Text("Job Responsibilities:", style = MaterialTheme.typography.labelMedium)
        bullets.forEachIndexed { index, bullet ->
            val hasError = bulletValidationErrors.containsKey(index)
            Column {
                OutlinedTextField(
                    value = bullet,
                    onValueChange = { newValue ->
                        bullets = bullets.toMutableList().apply { 
                            this[index] = newValue
                            if (index == size - 1 && newValue.isNotBlank() && !any { it.isBlank() }) {
                                add("")
                            }
                            if (size > 1) {
                                val filtered = this.filterIndexed { idx, text -> 
                                    text.isNotBlank() || idx == size - 1 
                                }
                                clear()
                                addAll(filtered)
                                if (isEmpty()) add("")
                            }
                        }
                        if (!containsLatexSpecialChars(newValue)) {
                            bulletValidationErrors = bulletValidationErrors.toMutableMap().apply { 
                                remove(index) 
                            }
                        }
                    },
                    label = { Text("Bullet ${index + 1}") },
                    placeholder = { Text("Describe your responsibility or achievement...", color = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text(bulletValidationErrors[index] ?: "", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        if (bulletsError != null) {
            Text(
                text = bulletsError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (validate()) {
                    onDone(
                        Experience(
                            title = title,
                            company = company,
                            location = location.ifBlank { null },
                            startDate = YearMonth.of(startYear, startMonth),
                            endDate = if (currentlyWorking) null else YearMonth.of(endYear, endMonth),
                            currentlyWorking = currentlyWorking,
                            bullets = bullets.filter { it.isNotBlank() }
                        )
                    )
                }
            }) { Text("Done") }
            OutlinedButton(onClick = { onCancel() }) { Text("Cancel") }
        }
    }
}

@Composable
fun ProjectInputForm(
    initial: Project? = null,
    onDone: (Project) -> Unit,
    onCancel: () -> Unit,
    onChanged: ((Boolean) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var stack by remember { mutableStateOf(initial?.stack ?: "") }
    var year by remember { mutableIntStateOf(initial?.date?.value ?: Year.now().value) }
    
    var bullets by remember { 
        mutableStateOf(
            if (initial?.bullets?.isNotEmpty() == true) {
                initial.bullets.toMutableList().apply { add("") }
            } else {
                mutableListOf("")
            }
        )
    }

    var titleError by remember { mutableStateOf<String?>(null) }
    var stackError by remember { mutableStateOf<String?>(null) }
    var bulletsError by remember { mutableStateOf<String?>(null) }
    var bulletValidationErrors by remember { mutableStateOf(mutableMapOf<Int, String>()) }

    val initialBullets = remember { initial?.bullets ?: emptyList() }
    val hasChanges by remember {
        derivedStateOf {
            title != (initial?.title ?: "") ||
            stack != (initial?.stack ?: "") ||
            year != (initial?.date?.value ?: Year.now().value) ||
            bullets.filter { it.isNotBlank() } != initialBullets
        }
    }

    LaunchedEffect(hasChanges) {
        onChanged?.invoke(hasChanges)
    }

    fun validate(): Boolean {
        var valid = true
        titleError = if (title.isBlank()) { 
            valid = false; "Title is required" 
        } else if (containsLatexSpecialChars(title)) {
            valid = false; getLatexSpecialCharError(title)
        } else null
        
        stackError = if (stack.isBlank()) { 
            valid = false; "Stack is required" 
        } else if (containsLatexSpecialChars(stack)) {
            valid = false; getLatexSpecialCharError(stack)
        } else null
        
        val nonEmptyBullets = bullets.filter { it.isNotBlank() }
        bulletsError = if (nonEmptyBullets.isEmpty()) { 
            valid = false; "At least one bullet is required" 
        } else null
        
        val newBulletValidationErrors = mutableMapOf<Int, String>()
        bullets.forEachIndexed { index, bullet ->
            if (bullet.isNotBlank() && containsLatexSpecialChars(bullet)) {
                newBulletValidationErrors[index] = getLatexSpecialCharError(bullet) ?: "Contains special characters"
                valid = false
            }
        }
        bulletValidationErrors = newBulletValidationErrors
        
        return valid
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, isError = titleError != null, supportingText = { titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = stack, onValueChange = { stack = it }, label = { Text("Stack") }, isError = stackError != null, supportingText = { stackError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        Row {
            YearPicker(
                "Year",
                year,
                onYearChange = { year = it }
            )
        }
        
        Text("Project Details:", style = MaterialTheme.typography.labelMedium)
        bullets.forEachIndexed { index, bullet ->
            val hasError = bulletValidationErrors.containsKey(index)
            Column {
                OutlinedTextField(
                    value = bullet,
                    onValueChange = { newValue ->
                        bullets = bullets.toMutableList().apply { 
                            this[index] = newValue
                            if (index == size - 1 && newValue.isNotBlank() && !any { it.isBlank() }) {
                                add("")
                            }
                            if (size > 1) {
                                val filtered = this.filterIndexed { idx, text -> 
                                    text.isNotBlank() || idx == size - 1 
                                }
                                clear()
                                addAll(filtered)
                                if (isEmpty()) add("")
                            }
                        }
                        if (!containsLatexSpecialChars(newValue)) {
                            bulletValidationErrors = bulletValidationErrors.toMutableMap().apply { 
                                remove(index) 
                            }
                        }
                    },
                    label = { Text("Bullet ${index + 1}") },
                    placeholder = { Text("Describe what you built or accomplished...", color = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text(bulletValidationErrors[index] ?: "", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        if (bulletsError != null) {
            Text(
                text = bulletsError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (validate()) {
                    onDone(
                        Project(
                            title = title,
                            stack = stack,
                            date = Year.of(year),
                            bullets = bullets.filter { it.isNotBlank() }
                        )
                    )
                }
            }) { Text("Done") }
            OutlinedButton(onClick = { onCancel() }) { Text("Cancel") }
        }
    }
}

@Composable
fun SkillsInputForm(
    initial: Skills = Skills("", "", "", ""),
    onDone: (Skills) -> Unit,
    onCancel: () -> Unit,
    onChanged: ((Boolean) -> Unit)? = null
) {
    var languages by remember { mutableStateOf(initial.languages) }
    var frameworks by remember { mutableStateOf(initial.frameworks) }
    var tools by remember { mutableStateOf(initial.tools) }
    var libraries by remember { mutableStateOf(initial.libraries) }

    var languagesError by remember { mutableStateOf<String?>(null) }
    var frameworksError by remember { mutableStateOf<String?>(null) }
    var toolsError by remember { mutableStateOf<String?>(null) }
    var librariesError by remember { mutableStateOf<String?>(null) }

    val hasChanges by remember {
        derivedStateOf {
            languages != initial.languages ||
            frameworks != initial.frameworks ||
            tools != initial.tools ||
            libraries != initial.libraries
        }
    }

    LaunchedEffect(hasChanges) {
        onChanged?.invoke(hasChanges)
    }

    fun validate(): Boolean {
        var valid = true
        languagesError = if (languages.isBlank()) { 
            valid = false; "Languages required" 
        } else if (containsLatexSpecialChars(languages)) {
            valid = false; getLatexSpecialCharError(languages)
        } else null
        
        frameworksError = if (frameworks.isBlank()) { 
            valid = false; "Frameworks required" 
        } else if (containsLatexSpecialChars(frameworks)) {
            valid = false; getLatexSpecialCharError(frameworks)
        } else null
        
        toolsError = if (tools.isBlank()) { 
            valid = false; "Tools required" 
        } else if (containsLatexSpecialChars(tools)) {
            valid = false; getLatexSpecialCharError(tools)
        } else null
        
        librariesError = if (libraries.isBlank()) { 
            valid = false; "Libraries required" 
        } else if (containsLatexSpecialChars(libraries)) {
            valid = false; getLatexSpecialCharError(libraries)
        } else null
        
        return valid
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = languages, onValueChange = { languages = it }, label = { Text("Languages") }, isError = languagesError != null, supportingText = { languagesError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = frameworks, onValueChange = { frameworks = it }, label = { Text("Frameworks") }, isError = frameworksError != null, supportingText = { frameworksError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = tools, onValueChange = { tools = it }, label = { Text("Tools") }, isError = toolsError != null, supportingText = { toolsError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        OutlinedTextField(value = libraries, onValueChange = { libraries = it }, label = { Text("Libraries") }, isError = librariesError != null, supportingText = { librariesError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (validate()) {
                    onDone(Skills(languages, frameworks, tools, libraries))
                }
            }) { Text("Done") }
            OutlinedButton(onClick = { onCancel() }) { Text("Cancel") }
        }
    }
}

@Composable
fun MonthYearPicker(
    label: String,
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    years: List<Int>? = null,
    enabled: Boolean = true
) {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val currentYear = Year.now().value
    val defaultYears = (currentYear downTo currentYear - 60).toList()
    val yearOptions = years ?: defaultYears

    Column {
        Text(label)
        Row {
            var expandedMonth by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expandedMonth = true }, enabled = enabled) {
                    Text(months[selectedMonth - 1])
                }
                DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                    months.forEachIndexed { idx, month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                onMonthChange(idx + 1)
                                expandedMonth = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            var expandedYear by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expandedYear = true }, enabled = enabled) {
                    Text(selectedYear.toString())
                }
                DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
                    yearOptions.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                onYearChange(year)
                                expandedYear = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YearPicker(
    label: String,
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
) {
    val currentYear = Year.now().value
    val years = (currentYear downTo currentYear - 10).toList()

    Column {
        Text(label)
        var expandedYear by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expandedYear = true }) {
                Text(selectedYear.toString())
            }
            DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
                years.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearChange(year)
                            expandedYear = false
                        }
                    )
                }
            }
        }
    }
}

fun Education.isValid(): Boolean = institution.isNotBlank() && degree.isNotBlank()
fun Experience.isValid(): Boolean = title.isNotBlank() && company.isNotBlank()
fun Project.isValid(): Boolean = title.isNotBlank() && stack.isNotBlank()
fun Skills.isValid(): Boolean = languages.isNotBlank() && frameworks.isNotBlank()

@Composable
fun UnsavedChangesDialog(
    onDiscardChanges: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { 
            Text(
                text = "Unsaved Changes",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = { 
            Text(
                text = "You have unsaved changes. Are you sure you want to leave without saving?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDiscardChanges,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Discard Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text("Keep Editing")
            }
        }
    )
}