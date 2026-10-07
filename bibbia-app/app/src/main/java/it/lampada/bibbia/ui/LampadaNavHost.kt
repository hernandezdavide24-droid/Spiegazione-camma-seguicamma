package it.lampada.bibbia.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.prefs.Settings
import it.lampada.bibbia.ui.analysis.AnalysisScreen
import it.lampada.bibbia.ui.blocker.BlockerScreen
import it.lampada.bibbia.ui.books.BooksScreen
import it.lampada.bibbia.ui.calendar.CalendarScreen
import it.lampada.bibbia.ui.calendar.FeastScreen
import it.lampada.bibbia.ui.calendar.SeasonScreen
import it.lampada.bibbia.ui.home.HomeScreen
import it.lampada.bibbia.ui.reader.ReaderScreen
import it.lampada.bibbia.ui.saved.SavedScreen
import it.lampada.bibbia.ui.search.SearchScreen
import it.lampada.bibbia.ui.settings.InfoScreen
import it.lampada.bibbia.ui.settings.MoreScreen
import it.lampada.bibbia.ui.settings.SettingsScreen
import java.time.LocalDate

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    OGGI("oggi", "Oggi", Icons.Outlined.WbSunny),
    BIBBIA("bibbia", "Bibbia", Icons.AutoMirrored.Outlined.MenuBook),
    SALVATI("salvati", "Salvati", Icons.Outlined.BookmarkBorder),
    CALENDARIO("calendario", "Calendario", Icons.Outlined.CalendarMonth),
    ALTRO("altro", "Altro", Icons.Outlined.MoreHoriz),
}

private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LampadaNavHost(container: AppContainer, settings: Settings, darkTheme: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val pending by container.navigator.pending.collectAsStateWithLifecycle()

    // Una richiesta di apertura (notifica, ricerca, preferiti...) porta sempre al lettore.
    LaunchedEffect(pending) {
        if (pending != null && route != Tab.BIBBIA.route) nav.goToTab(Tab.BIBBIA.route)
    }

    val openVerse: (VerseRef) -> Unit = { container.navigator.open(it) }
    val openAnalysis: (Int, Int, Int) -> Unit = { b, c, v -> nav.navigate("analisi/$b/$c/$v") }
    val openFeast: (String, LocalDate) -> Unit = { id, date -> nav.navigate("festa/$id/$date") }
    val openSeason: (String) -> Unit = { id -> nav.navigate("periodo/$id") }

    // Gli inset di sistema li gestisce ogni schermata (barra in alto); qui solo la barra di navigazione.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (Tab.entries.any { it.route == route }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.OGGI.route, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(Tab.OGGI.route) {
                HomeScreen(
                    container = container,
                    settings = settings,
                    onOpenVerse = openVerse,
                    onContinueReading = { nav.goToTab(Tab.BIBBIA.route) },
                    onOpenAnalysis = openAnalysis,
                    onOpenFeast = openFeast,
                    onOpenSeason = openSeason,
                    onOpenCalendar = { nav.goToTab(Tab.CALENDARIO.route) },
                )
            }
            composable(Tab.BIBBIA.route) {
                ReaderScreen(
                    container = container,
                    settings = settings,
                    darkTheme = darkTheme,
                    onOpenBooks = { nav.navigate("libri") },
                    onOpenSearch = { nav.navigate("cerca") },
                    onOpenAnalysis = openAnalysis,
                )
            }
            composable(Tab.SALVATI.route) {
                SavedScreen(container = container, darkTheme = darkTheme, onOpenVerse = openVerse)
            }
            composable(Tab.CALENDARIO.route) {
                CalendarScreen(
                    container = container,
                    settings = settings,
                    onOpenFeast = openFeast,
                    onOpenSeason = openSeason,
                )
            }
            composable(Tab.ALTRO.route) {
                MoreScreen(
                    onOpenBlocker = { nav.navigate("blocco") },
                    onOpenSettings = { nav.navigate("impostazioni") },
                    onOpenInfo = { nav.navigate("info") },
                )
            }
            composable("libri") {
                BooksScreen(
                    container = container,
                    onPick = { book, chapter ->
                        container.navigator.open(VerseRef(book, chapter, 1))
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("cerca") {
                SearchScreen(container = container, onOpenVerse = openVerse, onBack = { nav.popBackStack() })
            }
            composable(
                "analisi/{book}/{chapter}/{verse}",
                arguments = listOf(
                    navArgument("book") { type = NavType.IntType },
                    navArgument("chapter") { type = NavType.IntType },
                    navArgument("verse") { type = NavType.IntType },
                ),
            ) { entry ->
                val args = entry.arguments!!
                AnalysisScreen(
                    container = container,
                    book = args.getInt("book"),
                    chapter = args.getInt("chapter"),
                    verse = args.getInt("verse"),
                    onOpenVerse = openVerse,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("festa/{id}/{date}") { entry ->
                FeastScreen(
                    container = container,
                    feastId = entry.arguments?.getString("id").orEmpty(),
                    date = LocalDate.parse(entry.arguments?.getString("date") ?: LocalDate.now().toString()),
                    onOpenVerse = openVerse,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("periodo/{id}") { entry ->
                SeasonScreen(
                    container = container,
                    seasonId = entry.arguments?.getString("id").orEmpty(),
                    onOpenVerse = openVerse,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("blocco") { BlockerScreen(container = container, onBack = { nav.popBackStack() }) }
            composable("impostazioni") {
                SettingsScreen(container = container, settings = settings, onBack = { nav.popBackStack() })
            }
            composable("info") { InfoScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
