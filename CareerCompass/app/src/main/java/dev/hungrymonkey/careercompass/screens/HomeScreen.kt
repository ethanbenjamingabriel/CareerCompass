package dev.hungrymonkey.careercompass.screens

import android.widget.Toast
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.utils.NotificationPermissionUtils

object GlobalMemory {
    var hasShownAnimation = false
    var lastKnownName: String? = null
    var hasShownNotificationPrompt = false
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    var showNotificationPrompt by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        GlobalMemory.hasShownNotificationPrompt = true
        showNotificationPrompt = false
        println("Notification permission granted: $isGranted")
    }
    
    LaunchedEffect(Unit) {
        if (!GlobalMemory.hasShownNotificationPrompt && 
            !NotificationPermissionUtils.hasNotificationPermission(context)) {
            showNotificationPrompt = true
        }
    }
    
    BackHandler {
    }
    
    Scaffold(
        topBar = { TopBar(navController = navController) },
        bottomBar = { NavigationBar(navController) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item{GreetingSection()}

            item{ToolkitSection(navController)}

        }
    }
    
    if (showNotificationPrompt) {
        AlertDialog(
            onDismissRequest = {
                GlobalMemory.hasShownNotificationPrompt = true
                showNotificationPrompt = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Stay on Track",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Get helpful reminders for your career goals and never miss important deadlines.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                GlobalMemory.hasShownNotificationPrompt = true
                                showNotificationPrompt = false
                            }
                        }
                    ) {
                        Text("Enable Notifications")
                    }
                }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    TextButton(
                        onClick = {
                            GlobalMemory.hasShownNotificationPrompt = true
                            showNotificationPrompt = false
                        }
                    ) {
                        Text("Maybe Later")
                    }
                }
            }
        )
    }
}

@Composable
fun GreetingSection() {
    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid
    val context = LocalContext.current
    val firstNameState = remember { mutableStateOf("") }
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        uid?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    val firstName = document.getString("firstName") ?: ""
                    firstNameState.value = firstName
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "$e", Toast.LENGTH_SHORT).show()
                }
        }
    }

    LaunchedEffect(firstNameState.value) {
        if (firstNameState.value.isBlank()) return@LaunchedEffect
        val fullText = "Hi, ${firstNameState.value}! 👋\nLet's navigate your career."
        
        val shouldAnimate = !GlobalMemory.hasShownAnimation || (GlobalMemory.lastKnownName != null && GlobalMemory.lastKnownName != firstNameState.value)
        
        if (shouldAnimate) {
            displayedText = ""
            fullText.forEachIndexed { index, _ ->
                delay(15)
                displayedText = fullText.substring(0, index + 1)
            }
            GlobalMemory.hasShownAnimation = true
        } else {
            displayedText = fullText
        }
        
        GlobalMemory.lastKnownName = firstNameState.value
    }

    Column {
        Text(
            text = displayedText.ifEmpty { "" },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = "Career Toolkit",
            style = MaterialTheme.typography.titleMedium
        )
    }

}

@Composable
fun ToolkitSection(navController: NavHostController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        toolkitItems.forEach { item ->
            ToolkitCard(
                item = item,
                onClick = { navController.navigate(item.route) }
            )
        }
    }
}

@Composable
fun ToolkitCard(
    item: ToolkitItem,
    onClick: () -> Unit
) {
    val itemColor = getToolkitItemColor(item.colorIndex)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        itemColor.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = itemColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class ToolkitItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val colorIndex: Int
)

@Composable
fun getToolkitItemColor(index: Int): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (index) {
        0 -> colorScheme.primary
        1 -> colorScheme.secondary
        2 -> colorScheme.tertiary
        3 -> colorScheme.error
        4 -> colorScheme.outline
        else -> colorScheme.primary
    }
}

val toolkitItems = listOf(
    ToolkitItem(
        title = "Career Goals",
        description = "Set, track, and achieve your objectives",
        icon = Icons.Default.Flag,
        route = "career_goals",
        colorIndex = 0
    ),
    ToolkitItem(
        title = "Skill Gap Analysis", 
        description = "Identify skills to develop",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        route = "skill_gap",
        colorIndex = 1
    ),
    ToolkitItem(
        title = "Resume Builder",
        description = "Create professional resumes",
        icon = Icons.Default.Description,
        route = "resume_builder", 
        colorIndex = 2
    ),
    ToolkitItem(
        title = "Interview Prep",
        description = "Practice with mock interviews",
        icon = Icons.Default.RecordVoiceOver,
        route = "interview_prep",
        colorIndex = 3
    ),
    ToolkitItem(
        title = "Cover Letter",
        description = "Write compelling cover letters", 
        icon = Icons.Default.Email,
        route = "cover_letter",
        colorIndex = 4
    )
)