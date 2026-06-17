package io.potluckhub.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun RemoteImage(url: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Box(
        modifier.background(Brush.linearGradient(listOf(Brand.Sand, Brand.Cream))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Restaurant,
            contentDescription = null,
            tint = Brand.Golden,
            modifier = Modifier.size(30.dp),
        )
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun RatingLabel(rating: Double, count: Int? = null, maxStars: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (maxStars) {
            repeat(5) { index ->
                Icon(
                    Icons.Filled.Star,
                    null,
                    tint = if (index < rating.toInt()) Brand.Golden else Brand.Sand,
                    modifier = Modifier.size(13.dp),
                )
            }
        } else {
            Icon(Icons.Filled.Star, null, tint = Brand.Golden, modifier = Modifier.size(14.dp))
            Text(
                "%.1f".format(rating),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (count != null && count > 0) {
                Text("($count)", color = Brand.MutedInk, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun Pill(text: String, filled: Boolean = false) {
    Surface(
        color = if (filled) Brand.Terracotta else Brand.Teal.copy(alpha = 0.12f),
        shape = CircleShape,
    ) {
        Text(
            text,
            color = if (filled) Color.White else Brand.Teal,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun Avatar(url: String?, initials: String, size: Int = 44) {
    if (url.isNullOrEmpty()) {
        Box(
            Modifier.size(size.dp).clip(CircleShape).background(Brand.Teal),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.4).sp)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape).background(Brand.Sand),
        )
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand.Terracotta, contentColor = Color.White),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
fun BookingBar(price: String? = null, title: String, onClick: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            price?.let {
                Text(it, style = MaterialTheme.typography.titleLarge, color = Brand.Terracotta, fontWeight = FontWeight.Bold)
            }
            PrimaryButton(title, Modifier.weight(1f), onClick = onClick)
        }
    }
}

@Composable
fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Brand.Terracotta)
    }
}

@Composable
fun CenteredMessage(title: String, message: String = "", action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (message.isNotEmpty()) {
                Text(message, color = Brand.MutedInk, textAlign = TextAlign.Center)
            }
            action?.invoke()
        }
    }
}

fun initialsOf(name: String): String =
    name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
