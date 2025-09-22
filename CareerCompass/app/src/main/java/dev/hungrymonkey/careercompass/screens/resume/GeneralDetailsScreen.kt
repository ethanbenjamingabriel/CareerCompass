package dev.hungrymonkey.careercompass.screens.resume

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.hungrymonkey.careercompass.components.NavigationBar
import dev.hungrymonkey.careercompass.components.TopBar
import dev.hungrymonkey.careercompass.models.GeneralDetails
import dev.hungrymonkey.careercompass.viewmodel.GeneralDetailsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import dev.hungrymonkey.careercompass.utils.containsLatexSpecialChars
import dev.hungrymonkey.careercompass.utils.getLatexSpecialCharError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralDetailsScreen(
    navController: NavHostController,
    viewModel: GeneralDetailsViewModel = viewModel()
) {
    val context = LocalContext.current
    val existingDetails by viewModel.generalDetails.collectAsState()
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore
    val auth = Firebase.auth
    
    var fullName by remember { mutableStateOf("") }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var website2 by remember { mutableStateOf("") }
    var website2Error by remember { mutableStateOf<String?>(null) }
    var website1 by remember { mutableStateOf("") }
    var website1Error by remember { mutableStateOf<String?>(null) }
    
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(existingDetails) {
        existingDetails?.let { details ->
            if (!isLoaded) {
                fullName = details.fullName
                email = details.email
                phone = details.phone
                website2 = details.website2
                website1 = details.website1
                isLoaded = true
            }
        }
    }

    fun validateInput(): Boolean {
        var valid = true
        
        fullNameError = if (fullName.isBlank()) {
            valid = false
            "Full name is required"
        } else if (fullName.matches(Regex(".*\\d.*"))) {
            valid = false
            "Full name cannot contain numbers"
        } else if (containsLatexSpecialChars(fullName)) {
            valid = false
            getLatexSpecialCharError(fullName)
        } else null
        
        emailError = if (email.isBlank()) {
            valid = false
            "Email is required"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            valid = false
            "Invalid email address"
        } else null
        
        phoneError = if (phone.isBlank()) {
            valid = false
            "Phone number is required"
        } else if (!phone.matches(Regex("^\\+?[0-9]{7,15}"))) {
            valid = false
            "Invalid phone number"
        } else null
        
        website1Error = if (website1.isNotBlank() && !website1.startsWith("www")) {
            valid = false
            "Website 1 must start with www"
        } else null

        website2Error = if (website2.isNotBlank() && !website2.startsWith("www")) {
            valid = false
            "Website 2 must start with www"
        } else null
        
        return valid
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
                    
                    fullName = combinedName
                    email = userEmail
                    
                    Toast.makeText(context, "Loaded from account information", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load account information: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = { TopBar(title = "General Details") },
        bottomBar = { NavigationBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add your general details to use across all resumes",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(
                        text = "💡 Pro Tip",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Save your general details here and they'll automatically populate when creating new resumes!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { loadFromUserAccount() }
                ) {
                    Text("Load Account Details")
                }
            }
            
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                placeholder = { Text("John Doe", color = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.fillMaxWidth(),
                isError = fullNameError != null,
                supportingText = { 
                    fullNameError?.let { 
                        Text(it, color = MaterialTheme.colorScheme.error) 
                    }
                }
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("johndoe@gmail.com", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                isError = emailError != null,
                supportingText = { 
                    emailError?.let { 
                        Text(it, color = MaterialTheme.colorScheme.error) 
                    }
                }
            )
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("1112223333", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError != null,
                supportingText = { 
                    phoneError?.let { 
                        Text(it, color = MaterialTheme.colorScheme.error) 
                    }
                }
            )
            
            OutlinedTextField(
                value = website1,
                onValueChange = { website1 = it },
                label = { Text("Website 1 (optional)") },
                placeholder = { Text("ex: www.linkedin.com/in/johndoe", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                isError = website1Error != null,
                supportingText = { 
                    website1Error?.let { 
                        Text(it, color = MaterialTheme.colorScheme.error) 
                    }
                }
            )

            OutlinedTextField(
                value = website2,
                onValueChange = { website2 = it },
                label = { Text("Website 2 (optional)") },
                placeholder = { Text("ex: www.github.com/johndoe", color = MaterialTheme.colorScheme.outline) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                isError = website2Error != null,
                supportingText = { 
                    website2Error?.let { 
                        Text(it, color = MaterialTheme.colorScheme.error) 
                    }
                }
            )

            Button(
                onClick = {
                    if (validateInput()) {
                        val generalDetails = GeneralDetails(
                            id = existingDetails?.id ?: "",
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            website2 = website2.takeIf { it.isNotBlank() } ?: "",
                            website1 = website1.takeIf { it.isNotBlank() } ?: ""
                        )
                        viewModel.saveGeneralDetails(generalDetails)
                        Toast.makeText(context, "General details saved successfully!", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    } else {
                        Toast.makeText(context, "Please fix errors before saving", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existingDetails != null) "Update Details" else "Save Details")
            }
        }
    }
}
