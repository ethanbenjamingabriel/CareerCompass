package dev.hungrymonkey.careercompass.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.animateContentSize
import androidx.navigation.NavHostController
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.tasks.await
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.ui.theme.rememberThemePreferences
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(navController: NavHostController) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val uid = auth.currentUser?.uid
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = rememberThemePreferences(context)
    val isDarkMode = themePreferences.isDarkMode.collectAsState(initial = false)
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPersonalInfo by remember { mutableStateOf(false) }
    var showPasswordChange by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
    var deleteEmail by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }
    var deleteEmailError by remember { mutableStateOf<String?>(null) }
    var deletePasswordError by remember { mutableStateOf<String?>(null) }
    var isDeletePasswordVisible by remember { mutableStateOf(false) }
    
    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        uid?.let { userId ->
            try {
                val document = db.collection("users").document(userId).get().await()
                firstName = document.getString("firstName") ?: ""
                lastName = document.getString("lastName") ?: ""
                email = document.getString("email") ?: auth.currentUser?.email ?: ""
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun validateForm(): Boolean {
        var valid = true
        firstNameError = if (firstName.isBlank()) { 
            valid = false; "First name is required" 
        } else null
        lastNameError = if (lastName.isBlank()) { 
            valid = false; "Last name is required" 
        } else null
        
        if (showPasswordChange) {
            currentPasswordError = if (currentPassword.isBlank()) {
                valid = false; "Current password is required"
            } else null
            
            passwordError = when {
                newPassword.length < 6 -> { valid = false; "Password must be at least 6 characters" }
                newPassword != confirmPassword -> { valid = false; "Passwords don't match" }
                newPassword == currentPassword -> { valid = false; "New password must be different from current password" }
                else -> null
            }
        }
        return valid
    }

    fun updateProfile() {
        if (!validateForm()) return
        
        scope.launch {
            isLoading = true
            uid?.let { userId ->
                try {
                    val userData = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email
                    )
                    
                    db.collection("users").document(userId).update(userData as Map<String, Any>).await()
                    
                    if (showPasswordChange && newPassword.isNotBlank() && currentPassword.isNotBlank()) {
                        val user = auth.currentUser
                        if (user != null && user.email != null) {
                            try {
                                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                                user.reauthenticate(credential).await()
                                
                                user.updatePassword(newPassword).await()
                                
                                showPasswordChange = false
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                currentPasswordError = null
                                passwordError = null
                                
                                Toast.makeText(context, "Profile and password updated successfully!", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                currentPasswordError = "Current password is incorrect"
                                Toast.makeText(context, "Profile updated but password change failed: Current password is incorrect", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Profile updated but unable to change password: User not properly authenticated", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun validateDeleteForm(): Boolean {
        var valid = true
        
        deleteEmailError = when {
            deleteEmail.isBlank() -> { valid = false; "Email is required" }
            deleteEmail != email -> { valid = false; "Email does not match your account" }
            else -> null
        }
        
        deletePasswordError = if (deletePassword.isBlank()) {
            valid = false; "Password is required"
        } else null
        
        return valid
    }

    fun deleteAccount() {
        if (!validateDeleteForm()) return
        
        scope.launch {
            isDeleting = true
            try {
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, deletePassword)
                    user.reauthenticate(credential).await()
                    
                    uid?.let { userId ->
                        val knownCollections = listOf(
                            "goals", 
                            "resumes", 
                            "generalDetails", 
                            "coverLetters", 
                            "interviewQuestionBookmarks", 
                            "interviewQuestionAttempts", 
                            "skills", 
                            "careerProfile"
                        )
                        
                        for (collectionName in knownCollections) {
                            try {
                                val snapshot = db.collection("users").document(userId)
                                    .collection(collectionName).get().await()
                                snapshot.documents.forEach { document ->
                                    document.reference.delete().await()
                                }
                            } catch (_: Exception) {
                            }
                        }
                        
                        db.collection("users").document(userId).delete().await()
                        
                        try {
                            val userDocExists = db.collection("users").document(userId).get().await().exists()
                            if (userDocExists) {
                                Toast.makeText(
                                    context, 
                                    "Account deletion failed. User document still exists. Please contact support.", 
                                    Toast.LENGTH_LONG
                                ).show()
                                deletePasswordError = "Deletion verification failed"
                                return@launch
                            }
                            
                            val remainingCollections = mutableListOf<String>()
                            
                            for (collectionName in knownCollections) {
                                val verificationSnapshot = db.collection("users").document(userId)
                                    .collection(collectionName).limit(1).get().await()
                                if (!verificationSnapshot.isEmpty) {
                                    remainingCollections.add(collectionName)
                                }
                            }
                            
                            if (remainingCollections.isNotEmpty()) {
                                Toast.makeText(
                                    context, 
                                    "Account was partially deleted. Some data may remain in: ${remainingCollections.joinToString(", ")}. Please contact support.", 
                                    Toast.LENGTH_LONG
                                ).show()
                                deletePasswordError = "Deletion verification failed"
                                return@launch
                            }
                        } catch (_: Exception) {
                            Toast.makeText(
                                context, 
                                "Could not verify complete deletion. Please contact support to ensure all data was removed.", 
                                Toast.LENGTH_LONG
                            ).show()
                            deletePasswordError = "Deletion verification failed"
                            return@launch
                        }
                    }
                    
                    user.delete().await()
                    
                    GlobalMemory.hasShownAnimation = false
                    GlobalMemory.lastKnownName = null
                    
                    Toast.makeText(context, "Account and all data deleted successfully", Toast.LENGTH_SHORT).show()
                    
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Unable to delete account: User not properly authenticated", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                deletePasswordError = "Incorrect password"
            } finally {
                isDeleting = false
            }
        }
    }

    Scaffold(
        topBar = { TopBar(navController = navController) },
        bottomBar = { NavigationBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Account Settings Icon",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Account Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPersonalInfo = !showPersonalInfo }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Personal Information")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Personal Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (showPersonalInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showPersonalInfo) "Collapse" else "Expand"
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = showPersonalInfo,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideInVertically(
                            initialOffsetY = { -it / 4 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = EaseInOutCubic
                            )
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideOutVertically(
                            targetOffsetY = { -it / 4 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic
                            )
                        )
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("First Name") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = firstNameError != null,
                                supportingText = { firstNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Last Name") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = lastNameError != null,
                                supportingText = { lastNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = email,
                                onValueChange = { },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Address") }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordChange = !showPasswordChange }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "Password Settings")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Change Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (showPasswordChange) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showPasswordChange) "Collapse" else "Expand"
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = showPasswordChange,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideInVertically(
                            initialOffsetY = { -it / 4 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = EaseInOutCubic
                            )
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideOutVertically(
                            targetOffsetY = { -it / 4 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic
                            )
                        )
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { 
                                    currentPassword = it
                                    currentPasswordError = null
                                },
                                label = { Text("Current Password") },
                                visualTransformation = if (isCurrentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isCurrentPasswordVisible = !isCurrentPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isCurrentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (isCurrentPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isError = currentPasswordError != null,
                                supportingText = { currentPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { 
                                    newPassword = it
                                    passwordError = null
                                },
                                label = { Text("New Password") },
                                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isNewPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (isNewPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isError = passwordError != null,
                                supportingText = { passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                    confirmPassword = it
                                    passwordError = null
                                },
                                label = { Text("Confirm New Password") },
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isError = passwordError != null,
                                supportingText = { passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )
                        }
                    }
                    
                    LaunchedEffect(showPasswordChange) {
                        if (!showPasswordChange) {
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            currentPasswordError = null
                            passwordError = null
                            isCurrentPasswordVisible = false
                            isNewPasswordVisible = false
                            isConfirmPasswordVisible = false
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode.value) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = if (isDarkMode.value) "Dark Mode Settings" else "Light Mode Settings"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "App Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isDarkMode.value) "Dark theme enabled" else "Light theme enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkMode.value,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    themePreferences.setDarkMode(newValue)
                                }
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { updateProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save Changes")
                }
                
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
                
                Button(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Account")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        Firebase.auth.signOut()
                        GlobalMemory.hasShownAnimation = false
                        GlobalMemory.lastKnownName = null
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }
            },
            text = { 
                Column {
                    Text("Are you absolutely sure you want to delete your account?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone. All your data will be permanently deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        showDeleteConfirmationDialog = true
                        deleteEmail = ""
                        deletePassword = ""
                        deleteEmailError = null
                        deletePasswordError = null
                        isDeletePasswordVisible = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, I'm sure")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isDeleting) {
                    showDeleteConfirmationDialog = false 
                }
            },
            title = { 
                Text(
                    "Verify Your Identity", 
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    if (isDeleting) {
                        Text(
                            "Deleting your account and all associated data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text(
                            "To delete your account, please enter your email and password:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    OutlinedTextField(
                        value = deleteEmail,
                        onValueChange = { 
                            deleteEmail = it
                            deleteEmailError = null
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = deleteEmailError != null,
                        supportingText = { 
                            deleteEmailError?.let { 
                                Text(it, color = MaterialTheme.colorScheme.error) 
                            }
                        },
                        enabled = !isDeleting
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { 
                            deletePassword = it
                            deletePasswordError = null
                        },
                        label = { Text("Password") },
                        visualTransformation = if (isDeletePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            if (!isDeleting) {
                                IconButton(onClick = { isDeletePasswordVisible = !isDeletePasswordVisible }) {
                                    Icon(
                                        imageVector = if (isDeletePasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (isDeletePasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = deletePasswordError != null,
                        supportingText = { 
                            deletePasswordError?.let { 
                                Text(it, color = MaterialTheme.colorScheme.error) 
                            }
                        },
                        enabled = !isDeleting
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { deleteAccount() },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
