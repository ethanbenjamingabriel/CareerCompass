package dev.hungrymonkey.careercompass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dev.hungrymonkey.careercompass.ui.theme.CareerCompassTheme
import com.google.firebase.FirebaseApp
import dev.hungrymonkey.careercompass.screens.LoginScreen
import dev.hungrymonkey.careercompass.screens.HomeScreen
import dev.hungrymonkey.careercompass.screens.SignupScreen
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.google.firebase.auth.FirebaseAuth
import dev.hungrymonkey.careercompass.screens.CareerGoalsScreen
import dev.hungrymonkey.careercompass.screens.coverLetter.CoverLetterBuilderScreen
import dev.hungrymonkey.careercompass.screens.coverLetter.CoverLetterGeneratorScreen
import dev.hungrymonkey.careercompass.screens.resume.GeneralDetailsScreen
import dev.hungrymonkey.careercompass.screens.interview.InterviewPrepScreen
import dev.hungrymonkey.careercompass.screens.interview.InterviewQuestionsScreen
import dev.hungrymonkey.careercompass.screens.interview.QuestionDetailScreen
import dev.hungrymonkey.careercompass.screens.interview.AddCustomQuestionScreen
import dev.hungrymonkey.careercompass.screens.ProgressDashboardScreen
import dev.hungrymonkey.careercompass.screens.resume.ResumeBuilderScreen
import dev.hungrymonkey.careercompass.screens.skillGap.SkillGapScreen
import dev.hungrymonkey.careercompass.screens.skillGap.DetailedAnalysisScreen
import dev.hungrymonkey.careercompass.screens.resume.ResumeInputScreen
import dev.hungrymonkey.careercompass.screens.AccountScreen
import dev.hungrymonkey.careercompass.screens.LoadingScreen
import dev.hungrymonkey.careercompass.screens.UserLoadingScreen
import dev.hungrymonkey.careercompass.ui.theme.rememberThemePreferences
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.core.provider.FontRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        
        initializeEmojiCompat()
        
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themePreferences = rememberThemePreferences(context)
            val isDarkMode = themePreferences.isDarkMode.collectAsState(initial = false)
            
            CareerCompassTheme(darkTheme = isDarkMode.value) {
                Nav()
            }
        }
    }
    
    private fun initializeEmojiCompat() {
        if (!EmojiCompat.isConfigured()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val fontRequest = FontRequest(
                        "com.google.android.gms.fonts",
                        "com.google.android.gms",
                        "Noto Color Emoji Compat",
                        R.array.com_google_android_gms_fonts_certs
                    )
                    val config = FontRequestEmojiCompatConfig(this@MainActivity, fontRequest)
                        .setReplaceAll(true)
                        .setEmojiSpanIndicatorEnabled(false)
                        .registerInitCallback(object : EmojiCompat.InitCallback() {
                            override fun onInitialized() {
                                android.util.Log.d("MainActivity", "EmojiCompat initialized with downloadable fonts")
                            }
                            
                            override fun onFailed(throwable: Throwable?) {
                                android.util.Log.w("MainActivity", "Downloadable fonts failed, falling back to bundled", throwable)
                                initializeBundledEmoji()
                            }
                        })
                    EmojiCompat.init(config)
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Exception during downloadable font init, falling back to bundled", e)
                    initializeBundledEmoji()
                }
            }
        }
    }
    
    private fun initializeBundledEmoji() {
        try {
            val bundledConfig = BundledEmojiCompatConfig(this)
                .setReplaceAll(true)
                .setEmojiSpanIndicatorEnabled(false)
            EmojiCompat.init(bundledConfig)
            android.util.Log.d("MainActivity", "EmojiCompat initialized with bundled fonts")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize emoji support", e)
        }
    }
}
@Composable
fun Nav() {
    val navController = rememberNavController()
    val user = FirebaseAuth.getInstance().currentUser
    
    val startDestination = "app_loading"
    
    NavHost(
        navController, 
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ){

        composable ("login") { LoginScreen(navController) }
        composable ("signup") { SignupScreen(navController) }
        
        composable("app_loading") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LoadingScreen(
                    message = "Welcome to CareerCompass",
                    subtitle = "Your journey to career success starts here",
                    onLoadingComplete = {
                        if (user != null) {
                            navController.navigate("home") {
                            popUpTo("app_loading") { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("app_loading") { inclusive = true }
                        }
                    }
                }
            )
            }
        }
        
        composable("user_loading") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                UserLoadingScreen(navController, "home")
            }
        }
        
        composable("auth_loading") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LoadingScreen(
                    message = "Setting up your workspace...",
                    subtitle = "Preparing your personalized experience",
                    onLoadingComplete = {
                        navController.navigate("user_loading") {
                            popUpTo("auth_loading") { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable ("home") { 
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                HomeScreen(navController) 
            }
        }
        composable ("career_goals") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CareerGoalsScreen(navController)
            }
        }
        composable ("skill_gap") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                SkillGapScreen(navController)
            }
        }
        composable ("detailed_analysis") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                DetailedAnalysisScreen(navController)
            }
        }
        composable ("resume_builder") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ResumeBuilderScreen(navController)
            }
        }
        composable("resume_input/{templateName}") { backStackEntry ->
            val templateName = backStackEntry.arguments?.getString("templateName") ?: "Template1"
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ResumeInputScreen(navController, templateName)
            }
        }
        composable(
            "resume_input/{templateName}?resumeId={resumeId}",
            arguments = listOf(
                navArgument("templateName") { defaultValue = "Template1" },
                navArgument("resumeId") { nullable = true }
            )
        ) { backStackEntry ->
            val templateName = backStackEntry.arguments?.getString("templateName") ?: "Template1"
            val resumeId = backStackEntry.arguments?.getString("resumeId")
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ResumeInputScreen(navController, templateName, resumeId)
            }
        }
        composable ("general_details") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                GeneralDetailsScreen(navController)
            }
        }
        composable ("interview_prep") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                InterviewPrepScreen(
                    navController = navController,
                    onNavigateToQuestions = { navController.navigate("interview_questions") }
                )
            }
        }
        composable ("cover_letter") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CoverLetterBuilderScreen(navController)
            }
        }
        composable ("dashboard") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ProgressDashboardScreen(navController)
            }
        }
        composable ("account") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AccountScreen(navController)
            }
        }
        composable ("interview_questions") {
            InterviewQuestionsScreen(
                navController = navController,
                onQuestionClick = { questionId -> 
                    navController.navigate("question_detail/$questionId") 
                }
            )
        }
        composable ("question_detail/{questionId}") { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            QuestionDetailScreen(
                navController = navController,
                questionId = questionId,
            )
        }
        composable ("add_custom_question") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AddCustomQuestionScreen(navController = navController)
            }
        }
        composable ("dashboard") {ProgressDashboardScreen(navController)}
        composable ("account") {AccountScreen(navController)}
        
        composable("coverletter_generator") {
            CoverLetterGeneratorScreen(navController)
        }
        composable(
            "coverletter_generator?coverLetterId={coverLetterId}",
            arguments = listOf(navArgument("coverLetterId") { nullable = true })
        ) { backStackEntry ->
            val coverLetterId = backStackEntry.arguments?.getString("coverLetterId")
            CoverLetterGeneratorScreen(navController, coverLetterId)
        }
    }
}