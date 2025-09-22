package dev.hungrymonkey.careercompass.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import dev.hungrymonkey.careercompass.R

@Composable
fun UserLoadingScreen(
    navController: NavHostController,
    targetDestination: String = "home"
) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = Firebase.auth
    val uid = auth.currentUser?.uid
    
    var userName by remember { mutableStateOf("") }
    var loadingStage by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    
    val loadingMessages = listOf(
        "Connecting to your account...",
        "Loading your profile...",
        "Preparing your dashboard...",
    )
    
    var isVisible by remember { mutableStateOf(false) }
    
    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(1000),
        label = "contentAlpha"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
    LaunchedEffect(uid) {
        isVisible = true
        delay(500)
        
        uid?.let { userId ->
            try {
                loadingStage = 0
                delay(200)
                
                loadingStage = 1
                val document = db.collection("users").document(userId).get().await()
                userName = document.getString("firstName") ?: "User"
                delay(200)
                
                loadingStage = 2
                delay(300)
                
                loadingStage = 3
                delay(300)
                
                isComplete = true
                delay(200)
                
                navController.navigate(targetDestination) {
                    popUpTo("user_loading") { inclusive = true }
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_LONG).show()
                navController.navigate(targetDestination) {
                    popUpTo("user_loading") { inclusive = true }
                }
            }
        } ?: run {
            navController.navigate("login") {
                popUpTo("user_loading") { inclusive = true }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(logoScale * if (isComplete) pulseScale else 1f)
                    .alpha(contentAlpha),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_careercompass_foreground),
                    contentDescription = "CareerCompass Logo",
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedVisibility(
                visible = userName.isNotEmpty() && isComplete,
                enter = slideInVertically { -it } + fadeIn(tween(800))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome,",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!isComplete) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { (loadingStage + 1) / 4f },
                        modifier = Modifier
                            .width(160.dp)
                            .alpha(contentAlpha),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AnimatedContent(
                        targetState = loadingMessages.getOrNull(loadingStage) ?: "Loading...",
                        transitionSpec = {
                            slideInVertically { it } + fadeIn() togetherWith
                                    slideOutVertically { -it } + fadeOut()
                        },
                        label = "loadingMessage"
                    ) { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(shimmerAlpha)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isComplete) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { index ->
                        val delay = index * 200
                        val dotScale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, delayMillis = delay),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dotScale$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .scale(dotScale)
                                .alpha(contentAlpha * 0.7f)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}
