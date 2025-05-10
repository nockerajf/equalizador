package com.grupo11.equalizador.service

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28],
    resourceDir = "src/main/res",
    manifest = "src/main/AndroidManifest.xml"
)
class AudioServiceTest {

    private lateinit var audioService: AudioService
    private lateinit var mockAudioManager: AudioManager
    private lateinit var mockMediaPlayer: MediaPlayer
    private lateinit var mockMediaSession: MediaSessionCompat
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var mockHandler: Handler
    private val _applicationContext: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Initialize mocks
        mockAudioManager = mock(AudioManager::class.java)
        mockMediaPlayer = mock(MediaPlayer::class.java)
        mockMediaSession = mock(MediaSessionCompat::class.java)
        mockNotificationManager = mock(NotificationManager::class.java)
        mockHandler = mock(Handler::class.java)

        // Create an instance of AudioService
        audioService = spy(AudioService())

        // Inject mocks into the service
        audioService.apply {
            audioManager = mockAudioManager
            mediaPlayer = mockMediaPlayer
            mediaSession = mockMediaSession
            notificationManager = mockNotificationManager
            _context = _applicationContext
        }
    }

    @After
    fun tearDown() {
        // Clean up resources if needed
    }

    @Test
    fun `test onCreate initializes components`() {
        // Act
        audioService.onCreate()

        // Assert
        verify(audioService).createMediaPlayerInstance()
        verify(audioService).createAudioManager()
        verify(audioService).createMediaSession()
        verify(audioService).createNotificationChannel()
    }

    @Test
    fun `test onStartCommand handles PLAY action`() {
        // Arrange
        val intent = Intent().apply {
            action = "PLAY"
            putExtra("TRACK_RES_ID", 123)
        }

        // Act
        audioService.onStartCommand(intent, 0, 1)

        // Assert
        verify(mockMediaPlayer).reset()
        verify(mockMediaPlayer).setDataSource(any(), anyLong(), anyLong())
        verify(mockMediaPlayer).prepare()
        verify(mockMediaPlayer).start()
    }

    @Test
    fun `test onStartCommand handles PAUSE action`() {
        // Arrange
        val intent = Intent().apply {
            action = "PAUSE"
        }

        // Act
        audioService.onStartCommand(intent, 0, 1)

        // Assert
        verify(mockMediaPlayer).pause()
    }

    @Test
    fun `test onStartCommand handles STOP action`() {
        // Arrange
        val intent = Intent().apply {
            action = "STOP"
        }

        // Act
        audioService.onStartCommand(intent, 0, 1)

        // Assert
        verify(mockMediaPlayer).stop()
        verify(audioService).stopForeground(STOP_FOREGROUND_REMOVE)
        verify(audioService).stopSelf()
    }

    @Test
    fun `test onDestroy releases resources`() {
        // Act
        audioService.onDestroy()

        // Assert
        verify(mockMediaPlayer).release()
        verify(mockMediaSession).release()
    }
}