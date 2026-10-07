package it.lampada.bibbia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.prefs.Settings
import it.lampada.bibbia.data.prefs.ThemeMode
import it.lampada.bibbia.notifications.Notifications
import it.lampada.bibbia.ui.LampadaNavHost
import it.lampada.bibbia.ui.theme.LampadaTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as LampadaApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
            val dark = when (settings.theme) {
                ThemeMode.SISTEMA -> isSystemInDarkTheme()
                ThemeMode.CHIARO -> false
                ThemeMode.SCURO -> true
            }
            LampadaTheme(darkTheme = dark) {
                LampadaNavHost(container = container, settings = settings, darkTheme = dark)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Apertura da notifica: porta direttamente al versetto. */
    private fun handleIntent(intent: Intent?) {
        val book = intent?.getIntExtra(Notifications.EXTRA_BOOK, 0) ?: 0
        if (book <= 0) return
        container.navigator.open(
            VerseRef(
                book,
                intent!!.getIntExtra(Notifications.EXTRA_CHAPTER, 1),
                intent.getIntExtra(Notifications.EXTRA_VERSE, 1),
            ),
        )
        intent.removeExtra(Notifications.EXTRA_BOOK)
    }
}
