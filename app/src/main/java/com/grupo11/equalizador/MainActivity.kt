package com.grupo11.equalizador

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grupo11.equalizador.data.AudioTrack
import com.grupo11.equalizador.ui.AudioAdapter
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_PAUSE
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_PLAY
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_SEEK
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_STOP
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_UI
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_LOW_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_MID_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.ACTION_UPDATE_HIGH_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_CURRENT_POS
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_DURATION
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_TRACK
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_POSITION
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_GAIN
import com.grupo11.equalizador.utils.EqualizerConstants.DB_RANGE
import com.grupo11.equalizador.utils.EqualizerConstants.DB_OFFSET
import com.grupo11.equalizador.utils.EqualizerConstants.EXTRA_UI_PLAYING_STATE
import com.grupo11.equalizador.utils.EqualizerConstants.LOG_TAG_MAIN_ACTIVITY


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var seekBarProgress: SeekBar
    private lateinit var textViewCurrentTime: TextView
    private lateinit var textViewTotalTime: TextView
    private lateinit var textViewSongTitle: TextView
    private lateinit var lowSeek: SeekBar
    private lateinit var midSeek: SeekBar
    private lateinit var highSeek: SeekBar
    private var selectedTrackResId: Int? = null
    private val handler   = Handler()
    private var currentPosition = 0
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.i(LOG_TAG_MAIN_ACTIVITY, "broadcastReceiver onReceive() called with: intent = $intent")
            if (intent?.action == ACTION_UPDATE_UI) {
                Log.i(LOG_TAG_MAIN_ACTIVITY, "ACTION_UPDATE_UI received")
                val pos = intent.getIntExtra(EXTRA_CURRENT_POS, 0)
                val dur = intent.getIntExtra(EXTRA_DURATION, 0)
                val uiState = intent.getBooleanExtra(EXTRA_UI_PLAYING_STATE,false)
                Log.i(LOG_TAG_MAIN_ACTIVITY, "uiState: $uiState")
                if (uiState) {
                    playButton.isEnabled = false
                    stopButton.isEnabled = true
                    pauseButton.isEnabled = true
                } else {
                    playButton.isEnabled = true
                    stopButton.isEnabled = false
                    pauseButton.isEnabled = false
                }
                Log.i(LOG_TAG_MAIN_ACTIVITY, "pos: $pos, dur: $dur")
                if (dur > 0) {
                    seekBarProgress.max      = dur
                    seekBarProgress.progress = pos
                    textViewCurrentTime.text = formatMs(pos * 1000)
                    textViewTotalTime.text   = formatMs(dur * 1000)                }
            }
        }
    }
    /* ───────────────── LIFECYCLE ───────────────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(LOG_TAG_MAIN_ACTIVITY, "onCreate() called")

        initViews()

        /* Recycler */
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AudioAdapter(loadAudioTracks()) { track ->
            selectedTrackResId = track.resId
            textViewSongTitle.text = track.title
            sendToService(ACTION_PLAY, Bundle().apply { putInt(EXTRA_TRACK, track.resId) })
            Toast.makeText(this, "Tocando: ${track.title}", Toast.LENGTH_SHORT).show()
        }
        /* Register receiver */
        ContextCompat.registerReceiver(
            this, broadcastReceiver, IntentFilter(ACTION_UPDATE_UI),
            ContextCompat.RECEIVER_EXPORTED
        )
        /* botões */
        playButton.setOnClickListener  {
            selectedTrackResId?.let {
                Log.i(LOG_TAG_MAIN_ACTIVITY, "Play button clicked")
                sendToService(ACTION_PLAY,  Bundle().apply { putInt(EXTRA_TRACK, it) })
            } ?: Toast.makeText(this, "Selecione uma faixa", Toast.LENGTH_SHORT).show()
        }
        pauseButton.setOnClickListener {
            Log.i(LOG_TAG_MAIN_ACTIVITY, "Pause button clicked")
            sendToService(ACTION_PAUSE, null)
        }
        stopButton.setOnClickListener  {
            Log.i(LOG_TAG_MAIN_ACTIVITY, "Stop button clicked")
            sendToService(ACTION_STOP, null)
            sendToService(ACTION_SEEK, Bundle().apply { putInt(EXTRA_POSITION, 0) })
            seekBarProgress.progress = 0
        }
        /* ───── CONFIGURA EQUALIZADOR ───── */
        listOf(lowSeek, midSeek, highSeek).forEach { sb ->
            sb.max      = DB_RANGE
            sb.progress = DB_OFFSET          // 0 dB
        }

        lowSeek.setOnSeekBarChangeListener(simpleBandListener(ACTION_UPDATE_LOW_GAIN))
        midSeek.setOnSeekBarChangeListener(simpleBandListener(ACTION_UPDATE_MID_GAIN))
        highSeek.setOnSeekBarChangeListener(simpleBandListener(ACTION_UPDATE_HIGH_GAIN))
    }

    private fun initViews(){
        Log.i(LOG_TAG_MAIN_ACTIVITY, "initViews() called")
        recyclerView        = findViewById(R.id.audioTrackRecyclerView)
        playButton          = findViewById(R.id.button_play)
        pauseButton         = findViewById(R.id.button_pause)
        stopButton          = findViewById(R.id.button_stop)
        seekBarProgress     = findViewById(R.id.seekBarProgress)
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime)
        textViewTotalTime   = findViewById(R.id.textViewTotalTime)
        textViewSongTitle   = findViewById(R.id.textViewSongTitle)
        lowSeek   = findViewById(R.id.eqBand1)
        midSeek   = findViewById(R.id.eqBand3)
        highSeek  = findViewById(R.id.eqBand5)
        playButton.isEnabled = true
        stopButton.isEnabled = false
        pauseButton.isEnabled = false
    }

    override fun onDestroy() {
        Log.i(LOG_TAG_MAIN_ACTIVITY, "onDestroy() called")
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        handler.removeCallbacksAndMessages(null)
    }

    /* ───────────────── HELPERS ───────────────── */
    private fun simpleBandListener(action: String) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val gainDb = progress - DB_OFFSET               // -15…+15
                sendGainToService(action, gainDb)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        }

    private fun sendGainToService(action: String, gainDb: Int) {
        // Se o serviço quer fator linear, converta:
        // val linear = 10f.pow(gainDb / 20f)
        Log.i(LOG_TAG_MAIN_ACTIVITY, "sendGainToService() called with: action = $action, gainDb = $gainDb")
        sendToService(action, Bundle().apply { putFloat(EXTRA_GAIN, gainDb.toFloat()) })
    }

    private fun sendToService(action: String, extras: Bundle?) {
        Log.i(LOG_TAG_MAIN_ACTIVITY, "sendToService() called with: action = $action, extras = $extras")
        Intent(this, AudioService::class.java).also { intent ->
            intent.action = action
            extras?.let { intent.putExtras(it) }
            startService(intent)
        }
    }

    private fun formatMs(ms: Int): String {
        Log.i(LOG_TAG_MAIN_ACTIVITY, "formatMs() called with: ms = $ms")
        val s = (ms / 1000) % 60
        val m = (ms / 1000) / 60
        Log.i(LOG_TAG_MAIN_ACTIVITY, "formatMs() returning: %d:%02d".format(m, s))
        return "%d:%02d".format(m, s)
    }

    private fun loadAudioTracks(): List<AudioTrack> {
        val list = mutableListOf<AudioTrack>()
        R.raw::class.java.fields.forEach { f ->
            runCatching {
                val id   = f.getInt(null)
                val name = f.name.replace('_', ' ')
                    .split(' ')
                    .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                list += AudioTrack(name, id)
            }
        }
        return list
    }
}
