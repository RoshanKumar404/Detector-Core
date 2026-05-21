package com.example.detector.presentation.screens.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.detector.presentation.navigation.Screen
import com.example.detector.ui.theme.DeepTeal
import com.example.detector.ui.theme.LightBackground
import com.example.detector.ui.theme.TextDark
import com.example.detector.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: RegisterViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val municipalities by viewModel.municipalities.collectAsState()
    val wards by viewModel.wards.collectAsState()
    val isLoadingGeo by viewModel.isLoadingGeo.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedMunicipalityId by remember { mutableStateOf<Int?>(null) }
    var selectedMunicipalityName by remember { mutableStateOf("Select Municipality") }
    var selectedWardId by remember { mutableStateOf<Int?>(null) }
    var selectedWardName by remember { mutableStateOf("Select Ward") }

    var municipalityExpanded by remember { mutableStateOf(false) }
    var wardExpanded by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is RegisterUiState.Success -> {
                Toast.makeText(context, "Registration successful! Please log in.", Toast.LENGTH_LONG).show()
                viewModel.resetState()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            }
            is RegisterUiState.Error -> {
                Toast.makeText(context, (uiState as RegisterUiState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = LightBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Create Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Join Floodwatch community",
                fontSize = 15.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Full Name
            Text("Full Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter your name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DeepTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email
            Text("Email", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("example@domain.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = DeepTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number
            Text("Phone Number", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text("+91 1234567890") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = DeepTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Municipality Selection Dropdown
            Text("Municipality", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedMunicipalityName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = DeepTeal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { municipalityExpanded = true },
                    enabled = false, // Intercepts clicks directly via the Box modifier
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextDark,
                        disabledBorderColor = Color.LightGray,
                        disabledLeadingIconColor = DeepTeal,
                        disabledPlaceholderColor = TextMuted
                    )
                )
                // Overlay click interceptor box
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { municipalityExpanded = !municipalityExpanded }
                )

                DropdownMenu(
                    expanded = municipalityExpanded,
                    onDismissRequest = { municipalityExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    if (municipalities.isEmpty() && isLoadingGeo) {
                        DropdownMenuItem(
                            text = { Text("Loading municipalities...") },
                            onClick = {}
                        )
                    } else if (municipalities.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No municipalities found") },
                            onClick = {}
                        )
                    } else {
                        municipalities.forEach { muni ->
                            DropdownMenuItem(
                                text = { Text(muni.municipalityName) },
                                onClick = {
                                    selectedMunicipalityId = muni.municipalityId
                                    selectedMunicipalityName = muni.municipalityName
                                    selectedWardId = null
                                    selectedWardName = "Select Ward"
                                    viewModel.onMunicipalitySelected(muni.municipalityId)
                                    municipalityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Ward Selection Dropdown (dependent on selected municipality)
            Text("Ward Number", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedWardName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = DeepTeal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (selectedMunicipalityId != null) wardExpanded = true },
                    enabled = false,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (selectedMunicipalityId == null) TextMuted else TextDark,
                        disabledBorderColor = Color.LightGray,
                        disabledLeadingIconColor = DeepTeal,
                        disabledPlaceholderColor = TextMuted
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            if (selectedMunicipalityId == null) {
                                Toast.makeText(context, "Please select a municipality first", Toast.LENGTH_SHORT).show()
                            } else {
                                wardExpanded = !wardExpanded
                            }
                        }
                )

                DropdownMenu(
                    expanded = wardExpanded,
                    onDismissRequest = { wardExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    if (wards.isEmpty() && isLoadingGeo) {
                        DropdownMenuItem(
                            text = { Text("Loading wards...") },
                            onClick = {}
                        )
                    } else if (wards.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No wards found") },
                            onClick = {}
                        )
                    } else {
                        wards.forEach { ward ->
                            DropdownMenuItem(
                                text = { Text("Ward ${ward.wardNumber}") },
                                onClick = {
                                    selectedWardId = ward.wardId
                                    selectedWardName = "Ward ${ward.wardNumber}"
                                    wardExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            Text("Password", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••••••") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DeepTeal) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Terms & Conditions Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = DeepTeal)
                )
                Text(
                    text = "I agree to the Terms & Conditions",
                    fontSize = 14.sp,
                    color = TextDark,
                    modifier = Modifier.clickable { agreedToTerms = !agreedToTerms }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button
            Button(
                onClick = {
                    if (!agreedToTerms) {
                        Toast.makeText(context, "Please agree to the Terms & Conditions", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.register(
                            name = name,
                            email = email,
                            password = password,
                            phone = phone,
                            municipalityId = selectedMunicipalityId,
                            wardId = selectedWardId
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                enabled = uiState !is RegisterUiState.Loading
            ) {
                if (uiState is RegisterUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Already have an account link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", color = TextMuted, fontSize = 14.sp)
                TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                    Text("Login", color = DeepTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
