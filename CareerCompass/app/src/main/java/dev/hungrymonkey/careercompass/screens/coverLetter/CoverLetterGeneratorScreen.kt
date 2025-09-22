package dev.hungrymonkey.careercompass.screens.coverLetter

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.ResumeData
import dev.hungrymonkey.careercompass.models.CoverLetterData
import dev.hungrymonkey.careercompass.network.CoverLetterRepository
import dev.hungrymonkey.careercompass.viewmodel.CoverLetterViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class GenerationCacheData(
    val company: String,
    val jobTitle: String,
    val jobDescription: String,
    val selectedResumeId: String,
    val senderName: String,
    val senderEmail: String,
    val senderPhoneNumber: String,
    val senderLinkedInUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterGeneratorScreen(
    navController: NavHostController,
    coverLetterId: String? = null,
    viewModel: CoverLetterViewModel = viewModel()
) {
    val context = LocalContext.current
    val coverLetters by viewModel.coverLetters.collectAsState()
    val resumes by viewModel.resumes.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()
    val db = Firebase.firestore
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()
    
    val coverLetterRepository = remember { CoverLetterRepository() }
    
    var isPreviewLoading by remember { mutableStateOf(false) }
    var isExportLoading by remember { mutableStateOf(false) }
    
    var loaded by remember { mutableStateOf(false) }
    var coverLetterName by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var senderEmail by remember { mutableStateOf("") }
    var senderPhoneNumber by remember { mutableStateOf("") }
    var senderLinkedInUrl by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedResumeId by remember { mutableStateOf<String?>(null) }
    var generatedContent by remember { mutableStateOf("") }
    var showResumeSelector by remember { mutableStateOf(false) }
    var lastGenerationData by remember { mutableStateOf<GenerationCacheData?>(null) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var originalCoverLetterData by remember { mutableStateOf<CoverLetterData?>(null) }
    
    fun resetAllFields() {
        coverLetterName = ""
        senderName = ""
        senderEmail = ""
        senderPhoneNumber = ""
        senderLinkedInUrl = ""
        company = ""
        jobTitle = ""
        jobDescription = ""
        selectedResumeId = null
        generatedContent = ""
        lastGenerationData = null
    }
    
    fun hasUnsavedChanges(): Boolean {
        val currentData = CoverLetterData(
            id = coverLetterId ?: "",
            coverLetterName = coverLetterName,
            senderName = senderName,
            senderEmail = senderEmail,
            senderPhoneNumber = senderPhoneNumber,
            senderLinkedInUrl = senderLinkedInUrl,
            company = company,
            position = jobTitle,
            body = generatedContent,
            jobDescription = jobDescription,
            selectedResumeId = selectedResumeId,
            lastModifiedAt = Date()
        )
        
        return originalCoverLetterData?.let { original ->
            original.coverLetterName != currentData.coverLetterName ||
            original.senderName != currentData.senderName ||
            original.senderEmail != currentData.senderEmail ||
            original.senderPhoneNumber != currentData.senderPhoneNumber ||
            original.senderLinkedInUrl != currentData.senderLinkedInUrl ||
            original.company != currentData.company ||
            original.position != currentData.position ||
            original.body != currentData.body ||
            original.jobDescription != currentData.jobDescription ||
            original.selectedResumeId != currentData.selectedResumeId
        } ?: (coverLetterId == null && (coverLetterName.isNotBlank() || senderName.isNotBlank() || 
              senderEmail.isNotBlank() || senderPhoneNumber.isNotBlank() || 
              senderLinkedInUrl.isNotBlank() || company.isNotBlank() || 
              jobTitle.isNotBlank() || jobDescription.isNotBlank() || 
              generatedContent.isNotBlank()))
    }
    
    fun loadFromUserAccount() {
        scope.launch {
            auth.currentUser?.uid?.let { userId ->
                try {
                    val document = db.collection("users").document(userId).get().await()
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val userEmail = document.getString("email") ?: auth.currentUser?.email ?: ""
                    
                    val combinedName = "$firstName $lastName"
                    
                    senderName = combinedName
                    senderEmail = userEmail
                    
                    Toast.makeText(context, "Loaded from account information", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load account information: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    LaunchedEffect(coverLetterId, coverLetters) {
        if (!loaded && coverLetterId != null) {
            coverLetters.find { it.id == coverLetterId }?.let { cl ->
                coverLetterName = cl.coverLetterName
                senderName = cl.senderName
                senderEmail = cl.senderEmail
                senderPhoneNumber = cl.senderPhoneNumber
                senderLinkedInUrl = cl.senderLinkedInUrl
                company = cl.company
                jobTitle = cl.position
                jobDescription = cl.jobDescription
                selectedResumeId = cl.selectedResumeId
                generatedContent = cl.body
                
                originalCoverLetterData = cl
                
                if (cl.selectedResumeId != null) {
                    lastGenerationData = GenerationCacheData(
                        company = cl.company,
                        jobTitle = cl.position,
                        jobDescription = cl.jobDescription,
                        selectedResumeId = cl.selectedResumeId,
                        senderName = cl.senderName,
                        senderEmail = cl.senderEmail,
                        senderPhoneNumber = cl.senderPhoneNumber,
                        senderLinkedInUrl = cl.senderLinkedInUrl
                    )
                }
                
                loaded = true
            }
        }
    }
    
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    error?.let { errorMessage ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.clearError() }
                ) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }

    BackHandler {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = { TopBar(title = "Cover Letter Generator") },
        bottomBar = { NavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = coverLetterName,
                onValueChange = { coverLetterName = it },
                label = { Text("Cover Letter Name") },
                placeholder = { Text("e.g., Software Engineer at Google") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Personal Information",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = { loadFromUserAccount() }
                ) {
                    Text("Load Account Details")
                }
            }
            
            OutlinedTextField(
                value = senderName,
                onValueChange = { senderName = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = senderEmail,
                onValueChange = { senderEmail = it },
                label = { Text("Your Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = senderPhoneNumber,
                onValueChange = { senderPhoneNumber = it },
                label = { Text("Your Phone Number") },
                placeholder = { Text("e.g., 5551234567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = senderLinkedInUrl,
                onValueChange = { senderLinkedInUrl = it },
                label = { Text("Your LinkedIn URL") },
                placeholder = { Text("e.g., https://linkedin.com/in/yourprofile") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select Resume",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            selectedResumeId?.let { resumeId ->
                                resumes.find { it.id == resumeId }?.let { resume ->
                                    Text(
                                        text = resume.resumeName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            } ?: Text(
                                text = "No resume selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        TextButton(
                            onClick = { showResumeSelector = true }
                        ) {
                            Text("Choose")
                        }
                    }
                }
            }
            
            Text(
                text = "Job Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company Name") },
                placeholder = { Text("e.g., Google") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = { Text("Job Title") },
                placeholder = { Text("e.g., Software Engineer") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = jobDescription,
                onValueChange = { jobDescription = it },
                label = { Text("Job Description") },
                placeholder = { Text("Paste the job description here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 8
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (generatedContent.isEmpty()) {
                    Button(
                        onClick = {
                            if (coverLetterName.isBlank()) {
                                Toast.makeText(context, "Please enter a cover letter name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            scope.launch {
                                try {
                                    val validationResult = viewModel.validateCoverLetterName(
                                        coverLetterName, 
                                        excludeId = coverLetterId
                                    )
                                    
                                    validationResult.fold(
                                        onSuccess = {
                                            val coverLetter = CoverLetterData(
                                                id = coverLetterId ?: "",
                                                coverLetterName = coverLetterName,
                                                senderName = senderName,
                                                senderEmail = senderEmail,
                                                senderPhoneNumber = senderPhoneNumber,
                                                senderLinkedInUrl = senderLinkedInUrl,
                                                company = company,
                                                position = jobTitle,
                                                body = generatedContent,
                                                jobDescription = jobDescription,
                                                selectedResumeId = selectedResumeId,
                                                lastModifiedAt = Date()
                                            )
                                            
                                            if (coverLetterId == null) {
                                                viewModel.saveCoverLetter(coverLetter)
                                                Toast.makeText(context, "Cover letter saved!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.updateCoverLetter(coverLetter)
                                                Toast.makeText(context, "Cover letter updated!", Toast.LENGTH_SHORT).show()
                                            }
                                            
                                            navController.navigate("cover_letter") {
                                                popUpTo("cover_letter") { inclusive = false }
                                            }
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (coverLetterId == null) "Save Cover Letter Details" else "Update Cover Letter Details")
                    }
                }
                
                Button(
                onClick = {
                    if (selectedResumeId == null) {
                        Toast.makeText(context, "Please select a resume first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (company.isBlank() || jobTitle.isBlank() || jobDescription.isBlank()) {
                        Toast.makeText(context, "Please fill in all job details", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val currentGenerationData = GenerationCacheData(
                        company = company.trim(),
                        jobTitle = jobTitle.trim(),
                        jobDescription = jobDescription.trim(),
                        selectedResumeId = selectedResumeId!!,
                        senderName = senderName.trim(),
                        senderEmail = senderEmail.trim(),
                        senderPhoneNumber = senderPhoneNumber.trim(),
                        senderLinkedInUrl = senderLinkedInUrl.trim()
                    )
                    
                    if (lastGenerationData == currentGenerationData && generatedContent.isNotEmpty()) {
                        Toast.makeText(context, "Using existing cover letter (no changes detected)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    viewModel.generateCoverLetter(
                        company = currentGenerationData.company,
                        jobTitle = currentGenerationData.jobTitle,
                        jobDescription = currentGenerationData.jobDescription,
                        selectedResumeId = currentGenerationData.selectedResumeId,
                        senderName = currentGenerationData.senderName,
                        senderEmail = currentGenerationData.senderEmail,
                        senderPhoneNumber = currentGenerationData.senderPhoneNumber,
                        senderLinkedInUrl = currentGenerationData.senderLinkedInUrl
                    ) { content ->
                        if (content.isNotBlank()) {
                            generatedContent = content
                            lastGenerationData = currentGenerationData
                            Toast.makeText(context, "Cover letter generated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Generated content is empty. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Cover Letter")
                }
            }
            }
            
            if (generatedContent.isNotEmpty()) {
                Text(
                    text = "Generated Cover Letter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = generatedContent,
                    onValueChange = { generatedContent = it },
                    label = { Text("Cover Letter Content") },
                    placeholder = { Text("Generated content will appear here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    maxLines = 15
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (coverLetterName.isBlank()) {
                                Toast.makeText(context, "Please enter a cover letter name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            scope.launch {
                                try {
                                    val validationResult = viewModel.validateCoverLetterName(
                                        coverLetterName, 
                                        excludeId = coverLetterId
                                    )
                                    
                                    validationResult.fold(
                                        onSuccess = {
                                            val coverLetter = CoverLetterData(
                                                id = coverLetterId ?: "",
                                                coverLetterName = coverLetterName,
                                                senderName = senderName,
                                                senderEmail = senderEmail,
                                                senderPhoneNumber = senderPhoneNumber,
                                                senderLinkedInUrl = senderLinkedInUrl,
                                                company = company,
                                                position = jobTitle,
                                                body = generatedContent,
                                                jobDescription = jobDescription,
                                                selectedResumeId = selectedResumeId,
                                                lastModifiedAt = Date()
                                            )
                                            
                                            if (coverLetterId == null) {
                                                viewModel.saveCoverLetter(coverLetter)
                                                Toast.makeText(context, "Cover letter saved!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.updateCoverLetter(coverLetter)
                                                Toast.makeText(context, "Cover letter updated!", Toast.LENGTH_SHORT).show()
                                            }
                                            
                                            navController.navigate("cover_letter") {
                                                popUpTo("cover_letter") { inclusive = false }
                                            }
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (coverLetterId == null) "Save Cover Letter Details" else "Update Cover Letter Details")
                    }
                    
                    Spacer(Modifier.height(0.dp))
                    
                    Button(
                        onClick = {
                            if (coverLetterName.isBlank()) {
                                Toast.makeText(context, "Please enter a cover letter name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val coverLetter = CoverLetterData(
                                id = coverLetterId ?: "",
                                coverLetterName = coverLetterName,
                                senderName = senderName,
                                senderEmail = senderEmail,
                                senderPhoneNumber = senderPhoneNumber,
                                senderLinkedInUrl = senderLinkedInUrl,
                                company = company,
                                position = jobTitle,
                                body = generatedContent,
                                jobDescription = jobDescription,
                                selectedResumeId = selectedResumeId,
                                lastModifiedAt = Date()
                            )
                            
                            val currentUser = Firebase.auth.currentUser
                            if (currentUser == null) {
                                Toast.makeText(context, "Please log in to preview cover letter", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isPreviewLoading = true
                            scope.launch {
                                try {
                                    val validationResult = viewModel.validateCoverLetterName(
                                        coverLetterName, 
                                        excludeId = coverLetterId
                                    )
                                    
                                    validationResult.fold(
                                        onSuccess = {
                                            val saveResult = if (coverLetterId == null) {
                                                viewModel.saveCoverLetterAsync(coverLetter)
                                            } else {
                                                viewModel.updateCoverLetterAsync(coverLetter)
                                            }
                                            
                                            saveResult.fold(
                                                onSuccess = { savedCoverLetterId ->
                                                    val result = coverLetterRepository.generateAndPreviewCoverLetter(
                                                        userId = currentUser.uid,
                                                        coverLetterId = savedCoverLetterId,
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
                                                            Toast.makeText(context, "${error.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(context, "Failed to save cover letter: ${error.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } finally {
                                    isPreviewLoading = false
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
                        Text(if (isPreviewLoading) "Generating..." else "Preview Cover Letter")
                    }
                    
                    Spacer(Modifier.height(0.dp))
                    
                    Button(
                        onClick = {
                            if (coverLetterName.isBlank()) {
                                Toast.makeText(context, "Please enter a cover letter name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val coverLetter = CoverLetterData(
                                id = coverLetterId ?: "",
                                coverLetterName = coverLetterName,
                                senderName = senderName,
                                senderEmail = senderEmail,
                                senderPhoneNumber = senderPhoneNumber,
                                senderLinkedInUrl = senderLinkedInUrl,
                                company = company,
                                position = jobTitle,
                                body = generatedContent,
                                jobDescription = jobDescription,
                                selectedResumeId = selectedResumeId,
                                lastModifiedAt = Date()
                            )
                            
                            val currentUser = Firebase.auth.currentUser
                            if (currentUser == null) {
                                Toast.makeText(context, "Please log in to export cover letter", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isExportLoading = true
                            scope.launch {
                                try {
                                    val validationResult = viewModel.validateCoverLetterName(
                                        coverLetterName, 
                                        excludeId = coverLetterId
                                    )
                                    
                                    validationResult.fold(
                                        onSuccess = {
                                            val saveResult = if (coverLetterId == null) {
                                                viewModel.saveCoverLetterAsync(coverLetter)
                                            } else {
                                                viewModel.updateCoverLetterAsync(coverLetter)
                                            }
                                            
                                            saveResult.fold(
                                                onSuccess = { savedCoverLetterId ->
                                                    val result = coverLetterRepository.generateAndDownloadCoverLetter(
                                                        userId = currentUser.uid,
                                                        coverLetterId = savedCoverLetterId,
                                                        context = context
                                                    )
                                                    
                                                    result.fold(
                                                        onSuccess = { filePath ->
                                                            Toast.makeText(context, "Cover letter saved to Downloads folder!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onFailure = { error ->
                                                            Toast.makeText(context, "Failed to export cover letter: ${error.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(context, "Failed to save cover letter: ${error.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } finally {
                                    isExportLoading = false
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
                        Text(if (isExportLoading) "Exporting..." else "Export Cover Letter as PDF")
                    }
                }
            }
            
            OutlinedButton(
                onClick = {
                    resetAllFields()
                    Toast.makeText(context, "All fields cleared!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset All Fields")
            }
        }
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
    
    if (showResumeSelector) {
        ResumeSelectionDialog(
            resumes = resumes,
            onResumeSelected = { resume ->
                selectedResumeId = resume.id
                showResumeSelector = false
            },
            onDismiss = { showResumeSelector = false }
        )
    }
}

@Composable
fun ResumeSelectionDialog(
    resumes: List<ResumeData>,
    onResumeSelected: (ResumeData) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Resume") },
        text = {
            if (resumes.isEmpty()) {
                Text("No resumes found. Please create a resume first.")
            } else {
                Column {
                    resumes.forEach { resume ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { onResumeSelected(resume) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = resume.resumeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = resume.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

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
