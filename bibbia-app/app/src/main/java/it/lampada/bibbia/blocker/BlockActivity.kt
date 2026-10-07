package it.lampada.bibbia.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.lampada.bibbia.LampadaApp
import it.lampada.bibbia.MainActivity
import it.lampada.bibbia.data.daily.DailyVerse
import it.lampada.bibbia.ui.theme.LampadaTheme

/** Schermata di pausa mostrata al posto di un'app limitata. */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        val container = (application as LampadaApp).container
        setContent {
            val verse by produceState<DailyVerse?>(null) { value = container.dailyVerses.forDate() }
            LampadaTheme {
                BlockScreen(
                    app = intent.getStringExtra(EXTRA_APP) ?: "Questa app",
                    message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty(),
                    verse = verse,
                    onOpenBible = {
                        startActivity(
                            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                        finish()
                    },
                    onClose = ::goHome,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        const val EXTRA_APP = "app"
        const val EXTRA_MESSAGE = "messaggio"
    }
}

@Composable
private fun BlockScreen(
    app: String,
    message: String,
    verse: DailyVerse?,
    onOpenBible: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(
                Icons.Outlined.SelfImprovement, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text("Un momento di pausa", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                message.ifEmpty { "$app è in pausa." },
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (verse != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "«${verse.text}»",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(verse.reference, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Button(onClick = onOpenBible, modifier = Modifier.fillMaxWidth()) { Text("Apri la Bibbia") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Torna alla schermata Home") }
            Spacer(Modifier.height(16.dp))
            Text(
                "Puoi cambiare i limiti in Lampada › Altro › Limita app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
