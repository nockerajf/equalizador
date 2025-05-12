package com.grupo11.equalizador.service

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import com.grupo11.equalizador.AudioService
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_PAUSE
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_PLAY
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_STOP
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_TRACK
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_MID_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_HIGH_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_LOW_GAIN
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContextWrapper
import org.robolectric.shadows.ShadowService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28],
    resourceDir = "src/main/res",
    manifest = "src/main/AndroidManifest.xml",
    shadows = [ShadowService::class, ShadowContextWrapper::class]
)
class AudioServiceTest {

    private lateinit var audioService: AudioService
    private lateinit var mockMediaPlayer: MediaPlayer
    private lateinit var mockAudioManager: AudioManager
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var mockWavResPlayer: WavResPlayer
    private lateinit var mockHandler: Handler
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var service: AudioService
    private lateinit var mockContext: Context
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock dependencies
        mockMediaPlayer = mock(MediaPlayer::class.java)
        mockAudioManager = mock(AudioManager::class.java)
        mockNotificationManager = mock(NotificationManager::class.java)
        mockWavResPlayer = mock(WavResPlayer::class.java)
        mockHandler = mock(Handler::class.java)
        mockContext = mock(Context::class.java)
        // Setup the service
        service = AudioService()
        service.setMockPlayer(mockWavResPlayer)
        service.setContext(mockContext)
    }

    @After
    fun tearDown() {
        // Clean up resources
    }
    @Test
    fun `test service handles ACTION_PLAY`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AudioService::class.java).apply {
            action = ACTION_PLAY
            putExtra(EXTRA_TRACK, 1)
        }

        doNothing().`when`(mockContext).sendBroadcast(any(Intent::class.java))

        service.onStartCommand(intent, 0, 1)

        verify(mockWavResPlayer, atMostOnce()).play(1)
        verify(mockContext,times(1)).sendBroadcast(any())
    }

    @Test
    fun `test service handles ACTION_PAUSE`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AudioService::class.java).apply {
            action = ACTION_PAUSE
        }

        doNothing().`when`(mockContext).sendBroadcast(any(Intent::class.java))

        service.onStartCommand(intent, 0, 1)

        verify(mockWavResPlayer, atMostOnce()).pause()
        verify(mockContext, times(1)).sendBroadcast(any())
    }

    @Test
    fun `test service handles ACTION_STOP`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AudioService::class.java).apply {
            action = ACTION_STOP
        }

        doNothing().`when`(mockContext).sendBroadcast(any(Intent::class.java))

        service.onStartCommand(intent, 0, 1)

        verify(mockWavResPlayer, atMostOnce()).stop()
        verify(mockContext, times(1)).sendBroadcast(any())
    }

    @Test
    fun `test service handles ACTION_UPDATE_LOW_GAIN`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AudioService::class.java).apply {
            action = ACTION_UPDATE_LOW_GAIN
            putExtra(EXTRA_GAIN, 5.0f)
        }

        service.onStartCommand(intent, 0, 1)

        verify(mockWavResPlayer, atMostOnce()).updateLowBandGain(5.0f)
    }

    @Test
    fun `test service handles ACTION_UPDATE_MID_GAIN`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AudioService::class.java).apply {
            action = ACTION_UPDATE_MID_GAIN
            putExtra(EXTRA_GAIN, 3.0f)
        }

        service.onStartCommand(intent, 0, 1)

        verify(mockWavResPlayer, atMostOnce()).updateMidBandGain(3.0f)
    }

    @Test
    fun `test service handles ACTION_UPDATE_HIGH_GAIN`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AudioService::class.java).apply {
            action = ACTION_UPDATE_HIGH_GAIN
            putExtra(EXTRA_GAIN, 7.0f)
        }

        service.onStartCommand(intent, 0, 1)

        verify(mockWavResPlayer, atMostOnce()).updateHighBandGain(7.0f)
    }

}