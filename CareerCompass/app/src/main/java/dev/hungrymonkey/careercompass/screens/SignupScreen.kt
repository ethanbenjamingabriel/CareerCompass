package dev.hungrymonkey.careercompass.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import dev.hungrymonkey.careercompass.ui.theme.CareerCompassTheme

@Composable
fun SignupScreen(navController: NavHostController){
    CareerCompassTheme(darkTheme = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var firstName by remember { mutableStateOf("") }
            var lastName by remember { mutableStateOf("") }
            var isPasswordVisible by remember { mutableStateOf(false) }


            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sign Up", 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))


                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                val context = LocalContext.current

                Button(onClick = {
                    if (email == "" || password == "" || firstName == "" || lastName == ""){
                        Toast.makeText(
                            context,
                            "One or more fields are empty.\nPlease try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Firebase.auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = Firebase.auth.currentUser
                                    val db = Firebase.firestore
                                    val userData = hashMapOf(
                                        "email" to user?.email,
                                        "firstName" to firstName,
                                        "lastName" to lastName
                                    )
                                    user?.uid?.let { uid ->
                                        db.collection("users").document(uid).set(userData)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    context,
                                                    "Welcome!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.navigate("auth_loading") {
                                                    popUpTo("signup") { inclusive = true }
                                                }
                                            }.addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Error saving user: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Signup FAILED! Please try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign Up")
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                }) {
                    Text("Already have an account? Login")
                }
            }
        }
    }
}
}