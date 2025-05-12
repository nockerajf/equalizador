package com.grupo11.equalizador

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.io.IOException
import java.nio.ShortBuffer
import android.os.Build
import com.grupo11.equalizador.service.WavResPlayer


class AudioService : Service() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var mediaPlayer: MediaPlayer? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var audioManager: AudioManager? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var mediaSession: MediaSessionCompat? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var notificationManager : NotificationManager? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var _context: Context = this

    private val notificationId = 1
    private val handler = Handler()
    private var currentTrackResId: Int = -1
    private var trackId : Int = -1


    private val LOG_TAG = "AudioService"

    private lateinit var player: WavResPlayer

    override fun onCreate() {

        super.onCreate()
        Log.d("grupo 11", "Service onCreate() called")

        createMediaPlayerInstance()
        createAudioManager()
        createMediaSession()

        createNotificationChannel()
        startForeground(notificationId, createNotification())
        Log.d("grupo 11", "Foreground service started with notification")

        // Start updating UI using handler
        updateSeekBarProgress()

        //setupAudioTrack()
        player = WavResPlayer(_context)
        //textView.text = stringFromJNI()

    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createMediaPlayerInstance() {
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.reset()
        val fileDescriptor = _context.resources.openRawResourceFd(R.raw.dunno)
        mediaPlayer!!.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
        mediaPlayer!!.prepare()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createAudioManager() {
        Log.d("grupo 11", "Creating AudioManager instance")
        audioManager = _context.getSystemService(AUDIO_SERVICE) as AudioManager
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createMediaSession() {
        Log.d("grupo 11", "Creating MediaSession instance")
        mediaSession = MediaSessionCompat(_context, "AudioService")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when(action) {
            "PLAY" -> {
                // pega o resId que veio do MainActivity
                trackId = intent.getIntExtra("TRACK_RES_ID", -1)
                if (trackId != -1 && trackId != currentTrackResId) {
                    // troca de música
                    currentTrackResId = trackId
                    player.play(trackId)
                }
            }
            "PAUSE" -> player.pause()
            "STOP"  -> {
                player.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            "SEEK"  -> {
                val pos = intent.getIntExtra("SEEK_POSITION", 0)
            }
            "UPDATE_LOW_GAIN" -> {
                val gain = intent.getFloatExtra("GAIN", -1f)
                //UpdateGain is used just as an example to a kind of processing in the audio data
                player.updateGain((gain.coerceIn(-15f, +15f) + 15f) / 30f)

                player.updateLowBandGain(gain)
            }
            "UPDATE_MID_GAIN" -> {
                val gain = intent.getFloatExtra("GAIN", -1f)
                player.updateMidBandGain(gain)
            }
            "UPDATE_HIGH_GAIN" -> {
                val gain = intent.getFloatExtra("GAIN", -1f)
                player.updateHighBandGain(gain)
            }
            else -> Log.d("grupo 11", "Unknown action: $action")
        }
        return START_STICKY
    }

    private fun updateSeekBarProgress() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    val currentPosition = mediaPlayer!!.currentPosition
                    val duration = mediaPlayer!!.duration
                    Log.d("grupo 11", "MediaPlayer is playing. Current Position: $currentPosition, Duration: $duration.")

                    val intent = Intent("UPDATE_UI")
                    intent.putExtra("CURRENT_POSITION", currentPosition)
                    intent.putExtra("DURATION", duration)

                    sendBroadcast(intent)
                    Log.d("grupo 11", "Broadcast sent with Current Position: $currentPosition and Duration: $duration.")
                } else {
                    Log.d("grupo 11", "MediaPlayer is not playing.")
                    val intent = Intent("UPDATE_UI")
                    intent.putExtra("CURRENT_POSITION", 0)
                    intent.putExtra("DURATION", 0)

                    sendBroadcast(intent)
                    Log.d("grupo 11", "Broadcast sent with Current Position: 0 and Duration: 0.")
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.release()
        Log.d("grupo 11", "Service onDestroy() called, MediaPlayer and MediaSession released")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d("grupo11", "onBind() called")
        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createNotificationChannel() {
        Log.d("grupo 11", "Creating notification channel")
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Audio Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(serviceChannel)
        Log.d("grupo 11", "Notification channel created")
    }

    private fun createNotification(): Notification {
        Log.d("grupo 11", "Creating notification")
        val notificationIntent = Intent(_context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Player")
            .setContentText("Tocando música...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build().also {
                Log.d("grupo 11", "Notification created")
            }
    }


    companion object {
        private const val CHANNEL_ID = "AudioServiceChannel"
    }
}
