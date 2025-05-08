package com.grupo11.audioservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class AudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private val notificationId = 1
    private val handler = Handler()

    override fun onCreate() {
        super.onCreate()
        Log.d("grupo11", "Service onCreate() called")

        mediaPlayer = MediaPlayer()
        mediaPlayer!!.reset()

        val fileDescriptor = resources.openRawResourceFd(R.raw.instrumental1)
        mediaPlayer!!.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
        mediaPlayer!!.prepare()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSessionCompat(this, "AudioService")

        createNotificationChannel()
        startForeground(notificationId, createNotification())
        Log.d("grupo11", "Foreground service started with notification")

        // Start updating UI using handler
        updateSeekBarProgress()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("grupo11", "onStartCommand received - Action: ${intent?.action}")

        intent?.action?.let { action ->
            when (action) {
                "PLAY" -> playAudio()
                "PAUSE" -> pauseAudio()
                "STOP" -> stopAudio()
                "SEEK" -> {
                    val seekPosition = intent.getIntExtra("SEEK_POSITION", 0)
                    mediaPlayer?.seekTo(seekPosition)
                    Log.d("grupo11", "MediaPlayer seek to position: $seekPosition")
                }
                else -> {
                    Log.d("grupo11", "Unknown action received: $action")
                    // Optionally, handle any non-matching cases here
                }
            }
        } ?: run {
            Log.d("grupo11", "Intent is null or has no action")
        }

        return START_STICKY
    }


    private fun playAudio() {
        Log.d("grupo11", "Attempting to play audio")
        try {
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                mediaPlayer!!.start()
                Log.d("grupo11", "Audio started playing")
            }
        } catch (e: Exception) {
            Log.e("grupo11", "Error playing audio", e)
        }
    }

    private fun pauseAudio() {
        Log.d("grupo11", "Attempting to pause audio")
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            Log.d("grupo11", "Audio paused")
        }
    }

    private fun stopAudio() {
        Log.d("grupo11", "Attempting to stop audio")
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            val fileDescriptor = resources.openRawResourceFd(R.raw.instrumental1)
            mediaPlayer!!.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
            mediaPlayer!!.prepare()
            Log.d("grupo11", "Audio stopped and MediaPlayer reset")
        }
        stopForeground(true)
        stopSelf()
        Log.d("grupo11", "Foreground service stopped")
    }

    private fun updateSeekBarProgress() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    val currentPosition = mediaPlayer!!.currentPosition
                    val duration = mediaPlayer!!.duration
                    Log.d("grupo11", "MediaPlayer is playing. Current Position: $currentPosition, Duration: $duration.")

                    val intent = Intent("UPDATE_UI")
                    intent.putExtra("CURRENT_POSITION", currentPosition)
                    intent.putExtra("DURATION", duration)

                    sendBroadcast(intent)
                    Log.d("grupo11", "Broadcast sent with Current Position: $currentPosition and Duration: $duration.")
                } else {
                    Log.d("grupo11", "MediaPlayer is not playing.")
                    val intent = Intent("UPDATE_UI")
                    intent.putExtra("CURRENT_POSITION", 0)
                    intent.putExtra("DURATION", 0)

                    sendBroadcast(intent)
                    Log.d("grupo11", "Broadcast sent with Current Position: 0 and Duration: 0.")
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
        Log.d("grupo11", "Service onDestroy() called, MediaPlayer and MediaSession released")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d("grupo11", "onBind() called")
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("grupo11", "Creating notification channel")
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Audio Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            Log.d("grupo11", "Notification channel created")
        }
    }

    private fun createNotification(): Notification {
        Log.d("grupo11", "Creating notification")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Player")
            .setContentText("Tocando música...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build().also {
                Log.d("grupo11", "Notification created")
            }
    }

    companion object {
        private const val CHANNEL_ID = "AudioServiceChannel"
    }
}
