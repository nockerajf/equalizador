package com.grupo11.equalizador

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Assert
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Test


@RunWith(AndroidJUnit4::class)
class InstrumentedTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun useAppContext() {
        val appContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.grupo11.equalizador", appContext.packageName)
    }

    @Test
    fun appNameTextViewIsVisible() {
        val appContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val appName = appContext.getString(com.grupo11.equalizador.R.string.app_name)
        onView(withText(appName))
            .check(matches(isDisplayed()))
    }

    @Test
    fun playButtonIsClickable() {
        onView(withId(com.grupo11.equalizador.R.id.button_play))
            .check(matches(isClickable()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun pauseAndStopButtonsAreDisplayed() {
        onView(withId(com.grupo11.equalizador.R.id.button_pause))
            .check(matches(isDisplayed()))

        onView(withId(com.grupo11.equalizador.R.id.button_stop))
            .check(matches(isDisplayed()))
    }

    @Test
    fun seekBarProgressIsDisplayed() {
        onView(withId(com.grupo11.equalizador.R.id.seekBarProgress))
            .check(matches(isDisplayed()))
    }

    @Test
    fun songTitleTextViewShowsDefaultTextInitially() {
        onView(withId(com.grupo11.equalizador.R.id.textViewSongTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText("Nenhuma faixa selecionada")))
    }

    @Test
    fun eqBandFrequencyLabelsAreDisplayed() {
        onView(withId(com.grupo11.equalizador.R.id.eqBand1_freqLabel)).check(matches(isDisplayed()))
        onView(withId(com.grupo11.equalizador.R.id.eqBand3_freqLabel)).check(matches(isDisplayed()))
        onView(withId(com.grupo11.equalizador.R.id.eqBand5_freqLabel)).check(matches(isDisplayed()))

        onView(withId(com.grupo11.equalizador.R.id.eqBand1_freqLabel)).check(matches(withText("Low")))
        onView(withId(com.grupo11.equalizador.R.id.eqBand3_freqLabel)).check(matches(withText("Med")))
        onView(withId(com.grupo11.equalizador.R.id.eqBand5_freqLabel)).check(matches(withText("Hig")))
    }

}