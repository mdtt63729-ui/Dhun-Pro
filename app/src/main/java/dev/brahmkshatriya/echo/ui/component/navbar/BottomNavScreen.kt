package dev.brahmkshatriya.echo.ui.component.navbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : BottomNavScreen("Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Library : BottomNavScreen("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    data object Search : BottomNavScreen("Search", Icons.Filled.Search, Icons.Outlined.Search)
}
