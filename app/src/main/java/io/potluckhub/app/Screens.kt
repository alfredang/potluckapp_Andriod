package io.potluckhub.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class Cuisine(val slug: String, val label: String, val emoji: String) {
    Malay("malay", "Malay", "🍛"),
    Chinese("chinese", "Chinese", "🥟"),
    Thai("thai", "Thai", "🍲"),
    Indian("indian", "Indian", "🍛"),
    Halal("halal", "Halal", "🥘"),
    Vegetarian("vegetarian", "Vegetarian", "🥗"),
    Japanese("japanese", "Japanese", "🍱"),
    Korean("korean", "Korean", "🍜"),
    Western("western", "Western", "🍝"),
}

/** Shares plain text via the system share sheet. */
private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {
        // No app can handle the share — nothing to do.
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = Brand.MutedInk, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
    ) { Column(content = content) }
}

// ---------------- Explore ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onChef: (Chef) -> Unit) {
    var search by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf<Cuisine?>(null) }
    var featured by remember { mutableStateOf<List<Chef>>(emptyList()) }
    var chefs by remember { mutableStateOf<List<Chef>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val featuredIds = remember(featured) { featured.map { it.id }.toSet() }

    suspend fun reload() {
        loading = chefs.isEmpty()
        error = null
        runCatching {
            featured = Api.featuredChefs()
            chefs = Api.chefs(search.ifBlank { null }, cuisine?.slug)
        }.onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { TopAppBar(title = { Text("Explore", fontWeight = FontWeight.Bold) }) }) { pad ->
        when {
            loading && chefs.isEmpty() -> Box(Modifier.padding(pad)) { CenteredLoader() }
            error != null && chefs.isEmpty() -> Box(Modifier.padding(pad)) {
                CenteredMessage("Something went wrong", error!!) {
                    PrimaryButton("Try Again", Modifier.padding(top = 8.dp)) { scope.launch { reload() } }
                }
            }
            else -> LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { ExploreHero() }
                item {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        placeholder = { Text("Search home chefs") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { scope.launch { reload() } }),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(Cuisine.entries) { c ->
                            val on = cuisine == c
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (on) Brand.Terracotta else Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.clickable {
                                    cuisine = if (on) null else c
                                    scope.launch { reload() }
                                },
                            ) {
                                Text(
                                    "${c.emoji} ${c.label}",
                                    color = if (on) Color.White else Brand.Ink,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                if (featured.isNotEmpty()) {
                    item { SectionHeader("Featured Chefs", "Top-rated home cooks near you") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) { items(featured) { FeaturedChefCard(it, onChef) } }
                    }
                }
                item { SectionHeader("All Home Chefs", "${chefs.size} cooks ready to host") }
                items(chefs) { ChefRow(it, isFeatured = it.id in featuredIds, onChef = onChef) }
                if (chefs.isEmpty()) {
                    item { Text("No chefs match your filters yet.", color = Brand.MutedInk, modifier = Modifier.padding(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ExploreHero() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Home-cooked meals,\nfrom real Singapore kitchens.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Brand.Ink,
            )
            Text(
                "Discover talented home chefs in your neighbourhood. Book a seat at their table or have them cook a private dinner at yours.",
                style = MaterialTheme.typography.bodyMedium,
                color = Brand.MutedInk,
            )
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TrustBadge(Icons.Filled.Verified, "Identity-verified chefs")
            TrustBadge(Icons.Filled.Restaurant, "Halal & dietary-friendly")
            TrustBadge(Icons.Filled.CheckCircle, "Secure SGD payments")
        }
    }
}

@Composable
private fun TrustBadge(icon: ImageVector, text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 2.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, tint = Brand.Teal, modifier = Modifier.size(14.dp))
            Text(text, color = Brand.Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FeaturedChefCard(chef: Chef, onChef: (Chef) -> Unit) {
    Card(Modifier.width(220.dp).clickable { onChef(chef) }) {
        RemoteImage(chef.user.avatarUrl, Modifier.fillMaxWidth().height(140.dp))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(chef.user.fullName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FeaturedPill()
                if (chef.isVerified == true) VerifiedPill()
            }
            RatingLabel(chef.rating, chef.reviewCount)
            chef.specialties?.firstOrNull()?.let { Pill(it) }
        }
    }
}

@Composable
private fun ChefRow(chef: Chef, isFeatured: Boolean = false, onChef: (Chef) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onChef(chef) }) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RemoteImage(chef.user.avatarUrl, Modifier.size(78.dp).clip(RoundedCornerShape(14.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(chef.user.fullName, fontWeight = FontWeight.Bold)
                    if (chef.isVerified == true) VerifiedPill()
                    if (isFeatured) FeaturedPill()
                }
                chef.bio?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RatingLabel(chef.rating, chef.reviewCount)
                    chef.city?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = Brand.MutedInk, modifier = Modifier.size(13.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Chef detail ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefDetailScreen(chefId: String, auth: AuthViewModel, onBack: () -> Unit) {
    var chef by remember { mutableStateOf<Chef?>(null) }
    var reviews by remember { mutableStateOf<List<WebReview>>(emptyList()) }
    var reviewTotal by remember { mutableStateOf(0) }
    var reviewAverage by remember { mutableStateOf(0.0) }
    var error by remember { mutableStateOf<String?>(null) }
    var bookingMenu by remember { mutableStateOf<Menu?>(null) }
    var writeReview by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(chefId) {
        runCatching {
            chef = Api.chef(chefId)
            runCatching { Api.webReviews(chefId) }.onSuccess {
                reviews = it.reviews
                reviewTotal = it.total
                reviewAverage = it.average ?: 0.0
            }
        }.onFailure { error = it.message }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(chef?.user?.fullName ?: "Chef", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (chef?.isVerified == true) VerifiedPill()
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    chef?.let { c ->
                        IconButton(onClick = {
                            shareText(
                                context,
                                "Check out ${c.user.fullName} — home-cooked ${c.specialties?.firstOrNull() ?: "Singapore"} on Potluck 🍲 https://potluckhub.io",
                            )
                        }) { Icon(Icons.Filled.Share, "Share") }
                    }
                },
            )
        },
        bottomBar = {
            if (!chef?.menus.isNullOrEmpty()) {
                BookingBar(title = "See the Menu") { scope.launch { listState.animateScrollToItem(2) } }
            }
        },
    ) { pad ->
        val c = chef
        when {
            error != null && c == null -> Box(Modifier.padding(pad)) { CenteredMessage("Couldn't load chef", error!!) }
            c == null -> Box(Modifier.padding(pad)) { CenteredLoader() }
            else -> LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                item { RemoteImage(c.user.avatarUrl, Modifier.fillMaxWidth().height(220.dp)) }
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        RatingLabel(c.rating, c.reviewCount)
                        if (c.isVerified == true) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Verified, null, tint = Brand.Teal, modifier = Modifier.size(14.dp))
                                Text(
                                    "Identity & kitchen verified by Potluck (site visit)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Brand.Teal,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        c.bio?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                        c.specialties?.takeIf { it.isNotEmpty() }?.let { sp ->
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                sp.forEach { Pill(it) }
                            }
                        }
                    }
                }
                c.menus?.takeIf { it.isNotEmpty() }?.let { menus ->
                    item { SectionHeader("Menu") }
                    item { Spacer(Modifier.height(8.dp)) }
                    items(menus) { MenuRow(it) { bookingMenu = it } }
                }
                item { Spacer(Modifier.height(8.dp)); SectionHeader("Reviews", "What diners are saying") }
                item {
                    OutlinedButton(
                        onClick = { if (auth.isLoggedIn) writeReview = true else showAuth = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.Star, null, tint = Brand.Golden, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Write a Review", color = Brand.Teal, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (reviewTotal > 0) {
                    item { RatingSummary(reviewAverage, reviewTotal) }
                }
                items(reviews) { ReviewCard(it) }
                if (reviews.isEmpty()) {
                    item {
                        Text(
                            "No reviews yet — be the first to share your experience.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.MutedInk,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }

    bookingMenu?.let { menu ->
        chef?.let { c ->
            BookingSheet(chefName = c.user.fullName, menu = menu, auth = auth) { bookingMenu = null }
        }
    }
    if (writeReview) chef?.let { c ->
        WriteReviewSheet(
            chefId = c.id,
            chefName = c.user.fullName,
            auth = auth,
            onSubmitted = { r ->
                reviews = listOf(r) + reviews
                reviewAverage = ((reviewAverage * reviewTotal) + r.rating) / (reviewTotal + 1)
                reviewTotal += 1
            },
            onDismiss = { writeReview = false },
        )
    }
    if (showAuth) AuthSheet(auth) { showAuth = false }
}

@Composable
private fun MenuRow(menu: Menu, onBook: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(menu.firstImage, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(menu.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                menu.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Text(menu.displayPrice, fontWeight = FontWeight.Bold, color = Brand.Terracotta)
            }
            Surface(color = Brand.Teal, shape = RoundedCornerShape(50), modifier = Modifier.clickable { onBook() }) {
                Text("Book", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReviewCard(review: WebReview) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(null, initialsOf(review.authorName.ifBlank { "Potluck Diner" }), 36)
                Column(Modifier.weight(1f)) {
                    Text(review.authorName.ifBlank { "Diner" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    if (review.verifiedBooking) {
                        Text("Verified booking", style = MaterialTheme.typography.labelSmall, color = Brand.Teal, fontWeight = FontWeight.Medium)
                    }
                }
                RatingLabel(review.rating.toDouble(), maxStars = true)
            }
            review.title?.takeIf { it.isNotBlank() }?.let { Text(it, fontWeight = FontWeight.SemiBold) }
            Text(review.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteReviewSheet(
    chefId: String,
    chefName: String,
    auth: AuthViewModel,
    onSubmitted: (WebReview) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rating by remember { mutableStateOf(5) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var thanks by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Brand.Background) {
        if (thanks) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 12.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Brand.Teal, modifier = Modifier.size(56.dp))
                Text("Thank you!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Your review is live — it helps other diners find $chefName.",
                    color = Brand.MutedInk,
                    textAlign = TextAlign.Center,
                )
                PrimaryButton("Done", onClick = onDismiss)
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Write a Review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("How was your experience with $chefName?", color = Brand.MutedInk)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(5) { index ->
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                Icons.Filled.Star,
                                "${index + 1} star${if (index == 0) "" else "s"}",
                                tint = if (index < rating) Brand.Golden else Brand.Sand,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    title, { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    body, { body = it },
                    label = { Text("Your review") },
                    placeholder = { Text("Share your experience — the food, the host, the vibe…") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text("$it Tap Submit to try again.", color = Brand.Terracotta, style = MaterialTheme.typography.bodySmall)
                }
                PrimaryButton(
                    text = if (sending) "Sending…" else "Submit Review",
                    enabled = body.trim().length >= 10 && !sending,
                ) {
                    val user = auth.currentUser ?: return@PrimaryButton
                    error = null
                    sending = true
                    scope.launch {
                        runCatching {
                            Api.postWebReview(
                                WebReviewRequest(
                                    chefId = chefId,
                                    authorName = user.fullName,
                                    authorEmail = user.email.ifBlank { null },
                                    rating = rating,
                                    title = title.trim().ifBlank { null },
                                    body = body.trim(),
                                    platform = "android",
                                )
                            )
                        }.onSuccess { review ->
                            onSubmitted(review)
                            thanks = true
                        }.onFailure { error = it.message }
                        sending = false
                    }
                }
                Text(
                    "Reviews are public and appear on the chef's Potluck page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.MutedInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RatingSummary(rating: Double, count: Int) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f".format(rating), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                RatingLabel(rating, maxStars = true)
            }
            Column {
                Text("$count verified ${if (count == 1) "review" else "reviews"}", fontWeight = FontWeight.SemiBold)
                Text("From diners who've eaten here", style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk)
            }
        }
    }
}

// ---------------- Dishes ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishesScreen(onDish: (Menu) -> Unit) {
    var search by remember { mutableStateOf("") }
    var menus by remember { mutableStateOf<List<Menu>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = menus.isEmpty(); error = null
        runCatching { menus = Api.menus(search.ifBlank { null }) }.onFailure { error = it.message }
        loading = false
    }
    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { TopAppBar(title = { Text("Dishes", fontWeight = FontWeight.Bold) }) }) { pad ->
        when {
            loading && menus.isEmpty() -> Box(Modifier.padding(pad)) { CenteredLoader() }
            error != null && menus.isEmpty() -> Box(Modifier.padding(pad)) {
                CenteredMessage("Something went wrong", error!!) { PrimaryButton("Try Again") { scope.launch { reload() } } }
            }
            else -> Column(Modifier.padding(pad).fillMaxSize()) {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Search dishes") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { scope.launch { reload() } }),
                    modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(14.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) { items(menus) { DishCard(it, onDish) } }
            }
        }
    }
}

@Composable
private fun DishCard(menu: Menu, onDish: (Menu) -> Unit) {
    Card(Modifier.clickable { onDish(menu) }) {
        RemoteImage(menu.firstImage, Modifier.fillMaxWidth().height(120.dp))
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(menu.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            menu.chef?.user?.fullName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk, maxLines = 1) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(menu.displayPrice, fontWeight = FontWeight.Bold, color = Brand.Terracotta)
                if (menu.rating > 0) RatingLabel(menu.rating)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(menuId: String, auth: AuthViewModel, onBack: () -> Unit) {
    var menu by remember { mutableStateOf<Menu?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var booking by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(menuId) { runCatching { menu = Api.menu(menuId) }.onFailure { error = it.message } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(menu?.name ?: "Dish", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    menu?.let { m ->
                        IconButton(onClick = {
                            shareText(
                                context,
                                "${m.name} by ${m.chef?.user?.fullName ?: "a Potluck home chef"} on Potluck 🍲 https://potluckhub.io",
                            )
                        }) { Icon(Icons.Filled.Share, "Share") }
                    }
                })
        },
        bottomBar = {
            menu?.let { BookingBar(price = it.displayPrice, title = "Request This Dish") { booking = true } }
        },
    ) { pad ->
        val m = menu
        when {
            error != null && m == null -> Box(Modifier.padding(pad)) { CenteredMessage("Couldn't load dish", error!!) }
            m == null -> Box(Modifier.padding(pad)) { CenteredLoader() }
            else -> Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
                RemoteImage(m.firstImage, Modifier.fillMaxWidth().height(240.dp))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(m.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(m.displayPrice, style = MaterialTheme.typography.titleLarge, color = Brand.Terracotta, fontWeight = FontWeight.Bold)
                        if (m.rating > 0) RatingLabel(m.rating)
                    }
                    if (m.dietaryTags.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { m.dietaryTags.forEach { Pill(it) } }
                    m.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    m.chef?.user?.let { u ->
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(u.avatarUrl, initialsOf(u.fullName), 44)
                            Column { Text("Prepared by", style = MaterialTheme.typography.bodySmall, color = Brand.MutedInk); Text(u.fullName, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }

    if (booking) menu?.let { m ->
        BookingSheet(chefName = m.chef?.user?.fullName ?: "the chef", menu = m, auth = auth) { booking = false }
    }
}
