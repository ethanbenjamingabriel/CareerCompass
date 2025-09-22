package dev.hungrymonkey.careercompass.screens.resume

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.GeneralDetails
import dev.hungrymonkey.careercompass.models.ResumeData
import dev.hungrymonkey.careercompass.network.ResumeRepository
import dev.hungrymonkey.careercompass.viewmodel.GeneralDetailsViewModel
import dev.hungrymonkey.careercompass.viewmodel.ResumeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun formatPhoneNumber(phone: String): String {
    val cleaned = phone.replace(Regex("[^0-9]"), "")
    return when {
        cleaned.length == 10 -> {
            "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
        }
        cleaned.length == 11 && cleaned.startsWith("1") -> {
            "+1 (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
        }
        cleaned.length > 11 -> {
            val countryCode = cleaned.substring(0, cleaned.length - 10)
            val areaCode = cleaned.substring(cleaned.length - 10, cleaned.length - 7)
            val prefix = cleaned.substring(cleaned.length - 7, cleaned.length - 4)
            val number = cleaned.substring(cleaned.length - 4)
            "+$countryCode ($areaCode) $prefix-$number"
        }
        else -> phone
    }
}

@Composable
fun ResumeBuilderScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: ResumeViewModel = viewModel()
    val generalDetailsViewModel: GeneralDetailsViewModel = viewModel()
    val resumes by viewModel.resumes.collectAsState(initial = emptyList())
    val generalDetails by generalDetailsViewModel.generalDetails.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var resumeToDelete by remember { mutableStateOf<ResumeData?>(null) }
    var showTemplateDrawer by remember { mutableStateOf(false) }
    var previewLoadingIds by remember { mutableStateOf(setOf<String>()) }
    
    val resumeRepository = remember { ResumeRepository() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = { TopBar(title = "Resume Builder") },
        bottomBar = { NavigationBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    showTemplateDrawer = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Resume")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            generalDetails?.let { details ->
                GeneralDetailsCard(
                    generalDetails = details,
                    onEdit = { navController.navigate("general_details") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            } ?: run {
                GeneralDetailsEmptyCard(
                    onAddDetails = { navController.navigate("general_details") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            
            Text(
                text = "Recent Resumes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (resumes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No resumes yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap + to create your first resume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(resumes) { resume ->
                        ResumeCard(
                            resume = resume,
                            isPreviewLoading = previewLoadingIds.contains(resume.id),
                            onDelete = { 
                                resumeToDelete = resume
                                showDeleteDialog = true
                            },
                            onView = {
                                val currentUser = Firebase.auth.currentUser
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please log in to preview resume", Toast.LENGTH_SHORT).show()
                                    return@ResumeCard
                                }
                                
                                previewLoadingIds = previewLoadingIds + resume.id
                                scope.launch {
                                    try {
                                        val result = resumeRepository.generateAndPreviewResume(
                                            userId = currentUser.uid,
                                            resumeId = resume.id,
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
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error generating preview: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        previewLoadingIds = previewLoadingIds - resume.id
                                    }
                                }
                            },
                            onEdit = {
                                navController.navigate("resume_input/${resume.templateName}?resumeId=${resume.id}")
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        if (showDeleteDialog && resumeToDelete != null) {
            DeleteConfirmationDialog(
                resumeName = resumeToDelete!!.resumeName,
                onConfirm = {
                    viewModel.deleteResume(resumeToDelete!!.id)
                    showDeleteDialog = false
                    resumeToDelete = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    resumeToDelete = null
                }
            )
        }

        if (showTemplateDrawer) {
            TemplateSelectionDrawer(
                onTemplateSelected = { templateName ->
                    showTemplateDrawer = false
                    navController.navigate("resume_input/$templateName")
                },
                onDismiss = {
                    showTemplateDrawer = false
                }
            )
        }
    }
}

@Composable
fun ResumeCard(
    resume: ResumeData,
    isPreviewLoading: Boolean = false,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        resume.resumeName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        resume.resumeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Last Modified: ${dateFormat.format(resume.lastModifiedAt)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Resume",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onView) {
                    if (isPreviewLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "View Resume",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Resume",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    resumeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Delete Resume",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = { 
            Text(
                text = "Are you sure you want to delete \"$resumeName\"? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

@Composable
fun GeneralDetailsCard(
    generalDetails: GeneralDetails,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "General Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = generalDetails.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = generalDetails.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatPhoneNumber(generalDetails.phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                if (generalDetails.website1.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${generalDetails.website1.take(30)}${if (generalDetails.website1.length > 30) "..." else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                }

                if (generalDetails.website2.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${generalDetails.website2.take(30)}${if (generalDetails.website2.length > 30) "..." else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralDetailsEmptyCard(
    onAddDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "General Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TextButton(
                    onClick = onAddDetails,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Add your general details to quickly populate resumes with your personal information, contact details, and social links.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
