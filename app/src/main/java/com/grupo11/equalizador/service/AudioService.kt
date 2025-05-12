package com.grupo11.equalizador

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.grupo11.equalizador.service.WavResPlayer
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_PAUSE
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_PLAY
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_SEEK
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_STOP
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_HIGH_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_LOW_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_MID_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_UI
import com.grupo11.equalizador.utils.EqualizerConstants.CHANNEL_ID
import com.grupo11.equalizador.utils.EqualizerConstants.DEFAULT_TRACK_ID
import com.grupo11.equalizador.utils.EqualizerConstants.DEFAULT_TRACK_RESOURCE_ID
import com.grupo11.equalizador.utils.EqualizerConstants.DEFAULT_NOTIFICATION_ID
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_CURRENT_POS
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_DURATION
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_POSITION
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_TRACK
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_UI_PLAYING_STATE
import com.grupo11.equalizador.utils.EqualizerConstants.LOG_TAG_AUDIO_SERVICE
import com.grupo11.equalizador.utils.EqualizerConstants.NOTIFICATION_CHANNEL_NAME


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

    private val notificationId = DEFAULT_NOTIFICATION_ID
    private val handler = Handler()
    private var currentTrackResId: Int = DEFAULT_TRACK_RESOURCE_ID
    private var trackId : Int = DEFAULT_TRACK_ID

    private lateinit var player: WavResPlayer

    override fun onCreate() {

        super.onCreate()
        Log.d(LOG_TAG_AUDIO_SERVICE, "Service onCreate() called")

        createMediaPlayerInstance()
        createAudioManager()
        createMediaSession()

        createNotificationChannel()
        startForeground(notificationId, createNotification())
        Log.d(LOG_TAG_AUDIO_SERVICE, "Foreground service started with notification")

        // Start updating UI using handler
        updateSeekBarProgress()

        //setupAudioTrack()
        player = WavResPlayer(_context)
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
        Log.d(LOG_TAG_AUDIO_SERVICE, "Creating AudioManager instance")
        audioManager = _context.getSystemService(AUDIO_SERVICE) as AudioManager
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createMediaSession() {
        Log.d(LOG_TAG_AUDIO_SERVICE, "Creating MediaSession instance")
        mediaSession = MediaSessionCompat(_context, "AudioService")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when(action) {
            ACTION_PLAY -> {
                // pega o resId que veio do MainActivity
                trackId = intent.getIntExtra(EXTRA_TRACK, -1)
                if (trackId != -1 && trackId != currentTrackResId) {
                    // troca de música
                    currentTrackResId = trackId
                    player.play(trackId)
                    updateUiStateButton(true)
                }
            }
            ACTION_PAUSE -> {
                Log.d(LOG_TAG_AUDIO_SERVICE, "Service received ACTION_PAUSE")
                player.pause()
                updateUiStateButton(false)
            }
            ACTION_STOP  -> {
                player.stop()
                updateUiStateButton(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SEEK  -> {
                intent.getIntExtra(EXTRA_POSITION, 0)
            }
            ACTION_UPDATE_LOW_GAIN -> {
                val gain = intent.getFloatExtra(EXTRA_GAIN, -1f)
                //UpdateGain is used just as an example to a kind of processing in the audio data
                player.updateGain((gain.coerceIn(-15f, +15f) + 15f) / 30f)

                player.updateLowBandGain(gain)
            }
            ACTION_UPDATE_MID_GAIN -> {
                val gain = intent.getFloatExtra(EXTRA_GAIN, -1f)
                player.updateMidBandGain(gain)
            }
            ACTION_UPDATE_HIGH_GAIN -> {
                val gain = intent.getFloatExtra(EXTRA_GAIN, -1f)
                player.updateHighBandGain(gain)
            }
            else -> Log.d(LOG_TAG_AUDIO_SERVICE, "Unknown action: $action")
        }
        return START_STICKY
    }

    private fun updateSeekBarProgress() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (player.isPlaying) {
                    val currentPosition = player.getCurrentPosition()
                    val duration = player.getDuration()
                    Log.d(LOG_TAG_AUDIO_SERVICE, "MediaPlayer is playing. Current Position: $currentPosition, Duration: $duration.")

                    val intent = Intent(ACTION_UPDATE_UI)
                    intent.putExtra(EXTRA_CURRENT_POS, currentPosition)
                    intent.putExtra(EXTRA_DURATION, duration)
                    intent.putExtra(EXTRA_UI_PLAYING_STATE, true)
                    sendBroadcast(intent)
                    Log.d(LOG_TAG_AUDIO_SERVICE, "Broadcast sent with Current Position: $currentPosition and Duration: $duration.")
                } else {
                    Log.d(LOG_TAG_AUDIO_SERVICE, "MediaPlayer is not playing.")
                    val intent = Intent(ACTION_UPDATE_UI)
                    intent.putExtra(EXTRA_CURRENT_POS, 0)
                    intent.putExtra(EXTRA_DURATION, 0)
                    intent.putExtra(EXTRA_UI_PLAYING_STATE, false)
                    sendBroadcast(intent)
                    Log.d(LOG_TAG_AUDIO_SERVICE, "Broadcast sent with Current Position: 0 and Duration: 0.")
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateUiStateButton(isPlaying: Boolean){
        Log.d(LOG_TAG_AUDIO_SERVICE, "updateUiStateButton() called with: isPlaying = $isPlaying")
        val intent = Intent(ACTION_UPDATE_UI)
        if (isPlaying){
            intent.putExtra(EXTRA_UI_PLAYING_STATE, true)
        } else {
            intent.putExtra(EXTRA_UI_PLAYING_STATE, false)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateUiStateButton(false)
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.release()
        Log.d(LOG_TAG_AUDIO_SERVICE, "Service onDestroy() called, MediaPlayer and MediaSession released")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d("grupo11", "onBind() called")
        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createNotificationChannel() {
        Log.d(LOG_TAG_AUDIO_SERVICE, "Creating notification channel")
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(serviceChannel)
        Log.d(LOG_TAG_AUDIO_SERVICE, "Notification channel created")
    }

    private fun createNotification(): Notification {
        Log.d(LOG_TAG_AUDIO_SERVICE, "Creating notification")
        val notificationIntent = Intent(_context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Player")
            .setContentText("Tocando música...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build().also {
                Log.d(LOG_TAG_AUDIO_SERVICE, "Notification created")
            }
    }
}
