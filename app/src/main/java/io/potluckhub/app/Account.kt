package io.potluckhub.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// ---------------- Bookings ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(auth: AuthViewModel) {
    var showAuth by remember { mutableStateOf(false) }
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(auth.isLoggedIn) {
        if (auth.isLoggedIn) {
            loading = true; error = null
            runCatching { bookings = Api.myBookings() }.onFailure { error = it.message }
            loading = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Bookings", fontWeight = FontWeight.Bold) }) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                !auth.isLoggedIn -> SignedOut(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Track your dining plans",
                    message = "Sign in to view and manage your bookings with home chefs.",
                ) { showAuth = true }
                loading -> CenteredLoader()
                error != null -> CenteredMessage("Couldn't load bookings", error!!)
                bookings.isEmpty() -> CenteredMessage("No bookings yet", "Explore home chefs and request your first dining experience.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(bookings) { BookingCard(it) }
                }
            }
        }
    }
    if (showAuth) AuthSheet(auth) { showAuth = false }
}

@Composable
private fun BookingCard(b: Booking) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(b.bookingNumber ?: "Booking", fontWeight = FontWeight.SemiBold)
                b.status?.let { Pill(it.replaceFirstChar { c -> c.uppercase() }, filled = it == "confirmed") }
            }
            b.scheduledDate?.let { Text("$it ${b.scheduledTime ?: ""}", style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk) }
            b.total?.let { Text(money(it), fontWeight = FontWeight.Bold, color = Brand.Terracotta) }
        }
    }
}

@Composable
private fun SignedOut(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String, onSignIn: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Brand.Golden, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Brand.MutedInk, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Sign In", Modifier.fillMaxWidth(0.7f), onClick = onSignIn)
    }
}

// ---------------- Profile ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(auth: AuthViewModel) {
    var showAuth by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Profile", fontWeight = FontWeight.Bold) }) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val user = auth.currentUser
            if (user == null) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(40.dp))
                    BrandMark()
                    Spacer(Modifier.height(16.dp))
                    Text("Home-cooked meals,\nmade with love.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in to book unique dining experiences with talented home chefs across Singapore.", color = Brand.MutedInk, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton("Sign In or Register", Modifier.fillMaxWidth(0.85f)) { showAuth = true }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(24.dp))
                    Avatar(user.avatarUrl, user.initials, 88)
                    Spacer(Modifier.height(12.dp))
                    Text(user.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(user.email, color = Brand.MutedInk)
                    Spacer(Modifier.height(8.dp))
                    Pill(user.role.replaceFirstChar { it.uppercase() }, filled = true)
                    Spacer(Modifier.height(28.dp))
                    OutlinedButton(onClick = { auth.signOut() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign Out", color = Brand.Terracotta)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !auth.working,
                    ) {
                        Text("Delete Account", color = Brand.Terracotta)
                    }
                    deleteError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Brand.Terracotta, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Potluck v1.1", style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk)
                }
            }
        }
    }
    if (showAuth) AuthSheet(auth) { showAuth = false }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently removes your Potluck account and signs you out on this device.") },
            confirmButton = {
                TextButton(
                    enabled = !auth.working,
                    onClick = {
                        deleteError = null
                        scope.launch {
                            runCatching { auth.deleteAccount() }
                                .onSuccess { confirmDelete = false }
                                .onFailure { deleteError = it.message; confirmDelete = false }
                        }
                    },
                ) { Text(if (auth.working) "Deleting..." else "Delete", color = Brand.Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, enabled = !auth.working) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun BrandMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🌈", style = MaterialTheme.typography.displaySmall)
        Text("Potluck", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Brand.Ink)
    }
}

// ---------------- Auth sheet ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSheet(auth: AuthViewModel, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var register by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val valid = if (register)
        firstName.isNotBlank() && lastName.isNotBlank() && email.contains("@") && password.length >= 8
    else email.contains("@") && password.isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Brand.Background) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark()
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !register, onClick = { register = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Sign In") }
                SegmentedButton(selected = register, onClick = { register = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Register") }
            }
            if (register) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(firstName, { firstName = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(lastName, { lastName = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            OutlinedTextField(
                email, { email = it }, label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                password, { password = it }, label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = Brand.Terracotta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth()) }
            PrimaryButton(
                text = if (auth.working) "Please wait…" else if (register) "Create Account" else "Sign In",
                enabled = valid && !auth.working,
            ) {
                error = null
                scope.launch {
                    runCatching {
                        if (register) auth.register(email, password, firstName, lastName)
                        else auth.login(email, password)
                    }.onSuccess { onDismiss() }.onFailure { error = it.message }
                }
            }
            Text("By continuing you agree to Potluck's Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk, textAlign = TextAlign.Center)
        }
    }
}

// ---------------- Booking sheet ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSheet(chefName: String, menu: Menu, auth: AuthViewModel, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var guests by remember { mutableStateOf(2) }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }

    val subtotal = menu.price * guests
    val fee = (subtotal * 0.04).toInt()
    val total = subtotal + fee

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Brand.Background) {
        if (submitted) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Brand.Teal, modifier = Modifier.size(64.dp))
                Text("Request Sent!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("$chefName will review your request for ${menu.name} and get back to you shortly.", color = Brand.MutedInk, textAlign = TextAlign.Center)
                PrimaryButton("Done", onClick = onDismiss)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Request Booking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${menu.name} • with $chefName", color = Brand.MutedInk)
                Text(menu.displayPrice, color = Brand.Terracotta, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Guests", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { if (guests > 1) guests-- }) { Text("–") }
                        Text("  $guests  ", fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { if (guests < 20) guests++ }) { Text("+") }
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Special requests") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                HorizontalDivider()
                PriceRow("${menu.displayPrice} × $guests", money(subtotal))
                PriceRow("Service fee", money(fee))
                PriceRow("Total", money(total), bold = true)
                PrimaryButton(if (auth.isLoggedIn) "Request Booking" else "Sign in to Book") {
                    if (auth.isLoggedIn) submitted = true else showAuth = true
                }
                Text("You won't be charged until the chef confirms your booking.",
                    style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (showAuth) AuthSheet(auth) { showAuth = false }
}

@Composable
private fun PriceRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (bold) Brand.Ink else Brand.MutedInk, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (bold) Brand.Terracotta else Brand.Ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
