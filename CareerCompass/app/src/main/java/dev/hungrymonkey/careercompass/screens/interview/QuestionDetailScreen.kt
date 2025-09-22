package dev.hungrymonkey.careercompass.screens.interview

import android.widget.Toast
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.InterviewQuestion
import dev.hungrymonkey.careercompass.models.PracticeSession
import dev.hungrymonkey.careercompass.viewmodel.InterviewPrepViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    navController: NavHostController,
    questionId: String,
    viewModel: InterviewPrepViewModel = viewModel()
) {
    val context = LocalContext.current
    val questions by viewModel.questions.collectAsState()
    val practiceSessions by viewModel.practiceSessions.collectAsState()
    val sessions = practiceSessions.filter { it.questionId == questionId }
    val isLoading by viewModel.isLoading.collectAsState()
    
    var selectedSession by remember { mutableStateOf<PracticeSession?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val question = questions.find { it.id == questionId }
    
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording(context) { recorder, file ->
                mediaRecorder = recorder
                audioFile = file
                isRecording = true
            }
        }
    }

    Scaffold(
        topBar = { TopBar(title = "Question Details") },
        bottomBar = { NavigationBar(navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            question?.let { q ->
                item {
                    QuestionCard(question = q)
                }
                
                item {
                    RecordingSection(
                        isRecording = isRecording,
                        isLoading = isLoading,
                        onStartRecording = {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                                    startRecording(context) { recorder, file ->
                                        mediaRecorder = recorder
                                        audioFile = file
                                        isRecording = true
                                    }
                                }
                                else -> {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onStopRecording = {
                            try {
                                mediaRecorder?.let { recorder ->
                                    if (isRecording) {
                                        recorder.stop()
                                        recorder.release()
                                        
                                        audioFile?.let { file ->
                                            if (file.exists() && file.length() > 0) {
                                                val uri = Uri.fromFile(file)
                                                viewModel.analyzeAudioLocally(uri, questionId) { session ->
                                                    selectedSession = session
                                                    showBottomSheet = true
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context, 
                                    "Error stopping recording: ${e.message}", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                mediaRecorder = null
                                isRecording = false
                            }
                        }
                    )
                }
                
                item {
                    Text(
                        text = "Previous Attempts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (sessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No attempts yet. Record your first attempt!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sessions.sortedByDescending { it.completedAt }) { session ->
                    SessionCard(
                        session = session,
                        onClick = {
                            selectedSession = session
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }
    
    if (showBottomSheet && selectedSession != null) {
        FeedbackDetailSheet(
            session = selectedSession!!,
            onDismiss = {
                showBottomSheet = false
                selectedSession = null
            }
        )
    }
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isRecording) {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun QuestionCard(question: InterviewQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = question.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = "📂 ${question.category}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = "⭐ ${question.difficulty}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
            
            if (question.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Tags:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    question.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                    if (question.tags.size > 3) {
                        SuggestionChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = "+${question.tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingSection(
    isRecording: Boolean,
    isLoading: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    var recordingTime by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording) {
                delay(1000L)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }
    
    val formatTime = { seconds: Int ->
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        "%02d:%02d".format(minutes, remainingSeconds)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.RadioButtonChecked else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRecording) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isRecording) "Recording in Progress" else "Record Your Answer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Analyzing your response...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "This may take a few moments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = if (isRecording) onStopRecording else onStartRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .width(200.dp)
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isRecording) "Stop Recording" else "Start Recording",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isRecording) {
                            Text(
                                text = formatTime(recordingTime),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            Text(
                                text = "Ready to record",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: PracticeSession,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dateFormat.format(session.completedAt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Content: ${session.feedback.contentScore}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Delivery: ${session.feedback.deliveryScore}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${session.feedback.overallScore}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDetailSheet(
    session: PracticeSession,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attempt Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            
            Text(
                text = dateFormat.format(session.completedAt),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                modifier = Modifier.height(screenHeight - 325.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Scores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ScoreChip(
                                    label = "Overall",
                                    score = session.feedback.overallScore,
                                    highlighted = true
                                )
                                ScoreChip(
                                    label = "Content",
                                    score = session.feedback.contentScore
                                )
                                ScoreChip(
                                    label = "Delivery", 
                                    score = session.feedback.deliveryScore
                                )
                            }
                        }
                    }
                }

                if (session.feedback.transcript.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📝 Transcript",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = session.feedback.transcript,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                                )
                            }
                        }
                    }
                }
                
                if (session.feedback.strengths.isNotEmpty()) {
                    item {
                        FeedbackSection(
                            title = "✅ Strengths",
                            items = session.feedback.strengths,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (session.feedback.improvements.isNotEmpty()) {
                    item {
                        FeedbackSection(
                            title = "🎯 Areas for Improvement",
                            items = session.feedback.improvements,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                if (session.feedback.suggestedActions.isNotEmpty()) {
                    item {
                        FeedbackSection(
                            title = "📋 Next Steps",
                            items = session.feedback.suggestedActions,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                if (session.feedback.starMethodUsage.isNotBlank() && session.feedback.starMethodUsage != "Not applicable") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⭐ STAR Method Usage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = session.feedback.starMethodUsage,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FeedbackSection(
    title: String,
    items: List<String>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ScoreChip(
    label: String, 
    score: Int, 
    highlighted: Boolean = false
) {
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
            Text(
                text = "$score%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

private fun startRecording(
    context: Context,
    onStarted: (MediaRecorder, File) -> Unit
) {
    try {
        val audioFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
        
        audioFile.parentFile?.mkdirs()
        
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            
            try {
                prepare()
                start()
                onStarted(this, audioFile)
            } catch (e: Exception) {
                release()
                throw e
            }
        }
        
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context, 
            "Failed to start recording. Please try again.", 
            Toast.LENGTH_SHORT
        ).show()
    }
}
