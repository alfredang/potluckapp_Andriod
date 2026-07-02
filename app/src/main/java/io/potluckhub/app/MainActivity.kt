package io.potluckhub.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PotluckTheme { PotluckApp() }
        }
    }

    // potluck://checkout/result deep link just brings the app forward — the
    // checkout sheet re-polls the order status on ON_RESUME.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Explore("explore", "Explore", Icons.Filled.AutoAwesome),
    Dishes("dishes", "Dishes", Icons.Filled.Restaurant),
    Bookings("bookings", "Bookings", Icons.Filled.CalendarMonth),
    Profile("profile", "Profile", Icons.Filled.Person),
}

@Composable
private fun PotluckApp() {
    val nav = rememberNavController()
    val auth: AuthViewModel = viewModel()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in Tab.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Brand.Terracotta,
                            selectedTextColor = Brand.Terracotta,
                            indicatorColor = Brand.Sand,
                        ),
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = Tab.Explore.route, modifier = Modifier.fillMaxSize().padding(pad)) {
            composable(Tab.Explore.route) { ExploreScreen(onChef = { nav.navigate("chef/${it.id}") }) }
            composable(Tab.Dishes.route) { DishesScreen(onDish = { nav.navigate("dish/${it.id}") }) }
            composable(Tab.Bookings.route) { BookingsScreen(auth) }
            composable(Tab.Profile.route) { ProfileScreen(auth) }
            composable("chef/{id}") { entry ->
                ChefDetailScreen(entry.arguments?.getString("id").orEmpty(), auth, onBack = { nav.popBackStack() })
            }
            composable("dish/{id}") { entry ->
                DishDetailScreen(entry.arguments?.getString("id").orEmpty(), auth, onBack = { nav.popBackStack() })
            }
        }
    }
}
