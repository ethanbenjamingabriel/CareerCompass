package dev.hungrymonkey.careercompass.screens.coverLetter

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import kotlinx.coroutines.launch
import dev.hungrymonkey.careercompass.models.CoverLetterData
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hungrymonkey.careercompass.viewmodel.CoverLetterViewModel
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.widget.Toast
import java.util.Locale
import dev.hungrymonkey.careercompass.network.CoverLetterRepository

@Composable
fun CoverLetterBuilderScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coverLetterViewModel: CoverLetterViewModel = viewModel()
    val coverLetters by coverLetterViewModel.coverLetters.collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    var coverLetterToDelete by remember { mutableStateOf<CoverLetterData?>(null) }
    var previewLoadingIds by remember { mutableStateOf(setOf<String>()) }
    val coverLetterRepository = remember { CoverLetterRepository() }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Scaffold(
        topBar = { TopBar(title = "Cover Letter Builder") },
        bottomBar = { NavigationBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("coverletter_generator") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Cover Letter")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Recent Cover Letters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 16.dp, top = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (coverLetters.isEmpty()) {
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
                            "No cover letters yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap + to create your first cover letter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(coverLetters, key = { it.id }) { coverLetter ->
                        CoverLetterCard(
                            coverLetter = coverLetter,
                            dateFormat = dateFormat,
                            isPreviewLoading = previewLoadingIds.contains(coverLetter.id),
                            onDelete = {
                                coverLetterToDelete = coverLetter
                                showDeleteDialog = true
                            },
                            onView = {
                                val currentUser = Firebase.auth.currentUser
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please log in to preview cover letter", Toast.LENGTH_SHORT).show()
                                    return@CoverLetterCard
                                }
                                
                                previewLoadingIds = previewLoadingIds + coverLetter.id
                                scope.launch {
                                    try {
                                        val result = coverLetterRepository.generateAndPreviewCoverLetter(
                                            userId = currentUser.uid,
                                            coverLetterId = coverLetter.id,
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
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error generating preview: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        previewLoadingIds = previewLoadingIds - coverLetter.id
                                    }
                                }
                            },
                            onEdit = {
                                navController.navigate("coverletter_generator?coverLetterId=${coverLetter.id}")
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        if (showDeleteDialog && coverLetterToDelete != null) {
            DeleteConfirmationDialog(
                itemName = coverLetterToDelete!!.coverLetterName,
                onConfirm = {
                    coverLetterViewModel.deleteCoverLetter(coverLetterToDelete!!.id)
                    showDeleteDialog = false
                    coverLetterToDelete = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    coverLetterToDelete = null
                }
            )
        }
    }
}

@Composable
fun CoverLetterCard(
    coverLetter: CoverLetterData,
    dateFormat: SimpleDateFormat,
    isPreviewLoading: Boolean,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onEdit: () -> Unit
) {
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
                        coverLetter.coverLetterName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        coverLetter.coverLetterName,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                    Text(
                        "Last Modified: ${dateFormat.format(coverLetter.lastModifiedAt)}",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Cover Letter",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onView, enabled = !isPreviewLoading && coverLetter.body.isNotBlank()) {
                    if (isPreviewLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "View Cover Letter",
                            tint = if (coverLetter.body.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Cover Letter",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$itemName\"? This action cannot be undone.",
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