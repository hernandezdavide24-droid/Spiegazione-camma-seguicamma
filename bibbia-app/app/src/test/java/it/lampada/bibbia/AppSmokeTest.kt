package it.lampada.bibbia

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Prova di fumo: avvia l'app vera (con Robolectric) e attraversa le schermate principali,
 * per accorgersi di crash prima che l'APK arrivi sul telefono.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String, substring: Boolean = false) {
        rule.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 15_000)
    }

    @Test
    fun homeShowsDailyVerseAndCalendar() {
        waitFor("Versetto del giorno")
        waitFor("Oggi nel calendario cristiano")
    }

    @Test
    fun readSaveAndFindInSaved() {
        rule.onNodeWithText("Bibbia").performClick()
        waitFor("Capitolo 1")
        waitFor("Nel principio Iddio creò", substring = true)
        rule.onNodeWithText("Nel principio Iddio creò", substring = true).performClick()
        waitFor("Genesi 1:1")
        rule.onNodeWithText("Salva").performClick()

        rule.onNodeWithText("Salvati").performClick()
        waitFor("Genesi 1:1")
    }

    @Test
    fun chapterAnalysisOpens() {
        rule.onNodeWithText("Bibbia").performClick()
        waitFor("Capitolo 1")
        rule.onNodeWithContentDescription("Analisi del capitolo").performClick()
        waitFor("In sintesi")
    }

    @Test
    fun calendarBlockerAndSettingsOpen() {
        rule.onNodeWithText("Calendario").performClick()
        waitFor("Calendario cristiano")
        waitFor("Feste di questo mese")

        rule.onNodeWithText("Altro").performClick()
        rule.onNodeWithText("Limita app").performClick()
        waitFor("Permessi necessari")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        rule.onNodeWithText("Impostazioni").performClick()
        waitFor("Versetto del giorno")
    }

    @Test
    fun searchFindsText() {
        rule.onNodeWithText("Bibbia").performClick()
        waitFor("Capitolo 1")
        rule.onNodeWithContentDescription("Cerca").performClick()
        waitFor("Cerca nella Bibbia")
    }
}
