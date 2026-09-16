package dev.brahmkshatriya.echo.dhun.ui.screens

sealed class Screens(val route: String) {
    data object Home : Screens("home")
    data object Search : Screens("search")
    data object MoodAndGenres : Screens("mood_and_genres")
    data object Library : Screens("library")
    companion object { val MainScreens: List<Screens> = listOf(Home, Search, MoodAndGenres, Library) }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }

const val LOGIN_URL_ARGUMENT = "url"
fun buildLoginRoute(url: String?): String = "login?url=${url.orEmpty()}"
