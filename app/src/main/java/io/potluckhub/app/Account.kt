package io.potluckhub.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
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
                    Text("Potluck v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk)
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

// ---------------- Booking / checkout sheet ----------------

private enum class CheckoutStep { Details, Payment, Waiting, Success, Failed }

/** Opens the hosted payment page in a Chrome Custom Tab (falls back to a plain VIEW intent). */
private fun openPaymentPage(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder().setToolbarColor(Brand.Terracotta.toArgb()).build()
            )
            .build()
            .launchUrl(context, uri)
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            // No browser available — the polling screen still lets the user retry.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSheet(chefName: String, menu: Menu, auth: AuthViewModel, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(CheckoutStep.Details) }
    var guests by remember { mutableStateOf(2) }
    var notes by remember { mutableStateOf("") }
    var showAuth by remember { mutableStateOf(false) }

    // Schedule — server requires scheduledDate (YYYY-MM-DD) and scheduledTime (HH:mm).
    val dates = remember { List(14) { LocalDate.now().plusDays(it + 1L) } }
    var date by remember { mutableStateOf(dates.first()) }
    val times = remember { listOf("11:30", "12:30", "18:00", "18:30", "19:00") }
    var time by remember { mutableStateOf("18:30") }

    // Payment
    var provider by remember { mutableStateOf("stripe") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var order by remember { mutableStateOf<CheckoutOrder?>(null) }
    var failMessage by remember { mutableStateOf<String?>(null) }
    var resumeTick by remember { mutableStateOf(0) }

    val subtotal = menu.price * guests
    val fee = (subtotal * 0.04).toInt()
    val total = subtotal + fee

    // Re-poll immediately when the app returns from the payment browser tab.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Poll payment status every 2s (up to ~5 minutes) while waiting.
    LaunchedEffect(order?.orderId, step, resumeTick) {
        val o = order ?: return@LaunchedEffect
        if (step != CheckoutStep.Waiting) return@LaunchedEffect
        val deadline = System.currentTimeMillis() + 5 * 60_000L
        while (System.currentTimeMillis() < deadline) {
            val s = runCatching { Api.checkoutStatus(o.orderId) }.getOrNull()
            when (s?.status) {
                "paid" -> { step = CheckoutStep.Success; return@LaunchedEffect }
                "failed" -> { failMessage = "Your payment didn't go through. No money was taken."; step = CheckoutStep.Failed; return@LaunchedEffect }
                "cancelled" -> { failMessage = "The payment was cancelled. You can try again whenever you're ready."; step = CheckoutStep.Failed; return@LaunchedEffect }
            }
            delay(2_000)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Brand.Background) {
        when (step) {
            CheckoutStep.Details -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Book This Menu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${menu.name} • with $chefName", color = Brand.MutedInk)
                Text(menu.displayPrice, color = Brand.Terracotta, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                Text("Date", fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dates.forEach { d ->
                        ChoiceChip(
                            label = d.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                            selected = d == date,
                        ) { date = d }
                    }
                }
                Text("Time", fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    times.forEach { t ->
                        ChoiceChip(label = t, selected = t == time) { time = t }
                    }
                }
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
                PriceRow("Platform fee (4%)", money(fee))
                PriceRow("Total", money(total), bold = true)
                PrimaryButton(if (auth.isLoggedIn) "Continue to Payment" else "Sign in to Book") {
                    if (auth.isLoggedIn) step = CheckoutStep.Payment else showAuth = true
                }
                Text("Secure SGD payment — your booking is sent to the chef once payment is received.",
                    style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            CheckoutStep.Payment -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { step = CheckoutStep.Details }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text("Payment method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${menu.name} • ${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))}, $time • $guests ${if (guests == 1) "guest" else "guests"}",
                    color = Brand.MutedInk, style = MaterialTheme.typography.bodyMedium,
                )
                PaymentMethodRow(
                    selected = provider == "stripe",
                    title = "Credit / Debit Card", subtitle = "Powered by Stripe",
                    icon = Icons.Filled.CreditCard,
                ) { provider = "stripe" }
                PaymentMethodRow(
                    selected = provider == "paypal",
                    title = "PayPal", subtitle = null,
                    icon = Icons.Filled.AccountBalanceWallet,
                ) { provider = "paypal" }
                PaymentMethodRow(
                    selected = provider == "hitpay",
                    title = "PayNow / Card", subtitle = "Powered by HitPay",
                    icon = Icons.Filled.QrCode,
                ) { provider = "hitpay" }
                error?.let { Text(it, color = Brand.Terracotta, style = MaterialTheme.typography.bodySmall) }
                PrimaryButton(
                    text = if (creating) "Starting payment…" else "Proceed to Payment — ${money(total)}",
                    enabled = !creating,
                ) {
                    val user = auth.currentUser
                    if (user == null) { showAuth = true; return@PrimaryButton }
                    error = null
                    creating = true
                    scope.launch {
                        runCatching {
                            Api.createCheckout(
                                CheckoutRequest(
                                    menuId = menu.id,
                                    guests = guests,
                                    scheduledDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                    scheduledTime = time,
                                    specialRequests = notes.ifBlank { null },
                                    customerName = user.fullName,
                                    customerEmail = user.email,
                                    customerPhone = null,
                                    provider = provider,
                                    platform = "android",
                                )
                            )
                        }.onSuccess { o ->
                            order = o
                            step = CheckoutStep.Waiting
                            openPaymentPage(context, o.redirectUrl)
                        }.onFailure { error = it.message }
                        creating = false
                    }
                }
                Text("You'll be taken to a secure payment page to complete your booking.",
                    style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            CheckoutStep.Waiting -> Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(color = Brand.Terracotta)
                Text("Waiting for payment…", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Complete the payment in your browser — this screen updates automatically once it's confirmed.",
                    color = Brand.MutedInk, textAlign = TextAlign.Center)
                order?.orderNumber?.takeIf { it.isNotEmpty() }?.let {
                    Text("Order $it", style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk)
                }
                TextButton(onClick = { order?.redirectUrl?.takeIf { it.isNotEmpty() }?.let { openPaymentPage(context, it) } }) {
                    Text("Reopen payment page", color = Brand.Teal, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { step = CheckoutStep.Payment }) {
                    Text("Back", color = Brand.MutedInk)
                }
            }

            CheckoutStep.Success -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 20.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Brand.Teal, modifier = Modifier.size(64.dp))
                Text("Payment received!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                order?.orderNumber?.takeIf { it.isNotEmpty() }?.let {
                    Pill("Order $it", filled = true)
                }
                Text("Your booking request is now with the chef — you'll get a confirmation shortly.",
                    color = Brand.MutedInk, textAlign = TextAlign.Center)
                order?.amount?.let { a ->
                    HorizontalDivider()
                    PriceRow("Subtotal", money(a.subtotal, a.currency))
                    PriceRow("Platform fee (4%)", money(a.platformFee, a.currency))
                    PriceRow("Total paid", money(a.total, a.currency), bold = true)
                }
                PrimaryButton("Done", onClick = onDismiss)
            }

            CheckoutStep.Failed -> Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Filled.ErrorOutline, null, tint = Brand.Terracotta, modifier = Modifier.size(64.dp))
                Text("Payment not completed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(failMessage ?: "Something went wrong with the payment.", color = Brand.MutedInk, textAlign = TextAlign.Center)
                PrimaryButton("Try Again") {
                    order = null
                    failMessage = null
                    error = null
                    step = CheckoutStep.Payment
                }
                TextButton(onClick = onDismiss) { Text("Close", color = Brand.MutedInk) }
            }
        }
    }
    if (showAuth) AuthSheet(auth) { showAuth = false }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Brand.Terracotta else Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            label,
            color = if (selected) Color.White else Brand.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun PaymentMethodRow(
    selected: Boolean,
    title: String,
    subtitle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Brand.Sand else Color.White,
        border = BorderStroke(1.5.dp, if (selected) Brand.Terracotta else Brand.Sand),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = if (selected) Brand.Terracotta else Brand.MutedInk)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk) }
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Brand.Terracotta),
            )
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (bold) Brand.Ink else Brand.MutedInk, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (bold) Brand.Terracotta else Brand.Ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
