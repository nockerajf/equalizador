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


class MainActivity : AppCompatActivity() {

    /* ───────────────── VIEW refs ───────────────── */
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

    /* ───────────────── VARS ───────────────── */
    private var selectedTrackResId: Int? = null
    private val handler   = Handler()
    private var currentPosition = 0

    /* ───────────────── BROADCAST ───────────────── */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_UI) {
                val pos = intent.getIntExtra(EXTRA_CURRENT_POS, 0)
                val dur = intent.getIntExtra(EXTRA_DURATION, 0)
                if (dur > 0) {
                    seekBarProgress.max      = dur
                    seekBarProgress.progress = pos
                    textViewCurrentTime.text = formatMs(pos)
                    textViewTotalTime.text   = formatMs(dur)
                }
            }
        }
    }

    /* ───────────────── LIFECYCLE ───────────────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                sendToService(ACTION_PLAY,  Bundle().apply { putInt(EXTRA_TRACK, it) })
            } ?: Toast.makeText(this, "Selecione uma faixa", Toast.LENGTH_SHORT).show()
        }
        pauseButton.setOnClickListener { sendToService(ACTION_PAUSE, null) }
        stopButton.setOnClickListener  {
            sendToService(ACTION_STOP, null)
            sendToService(ACTION_SEEK, Bundle().apply { putInt(EXTRA_POSITION, 0) })
            seekBarProgress.progress = 0
        }

        /* seek global (posição da música) */
        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentPosition = progress
                    sendToService(ACTION_SEEK, Bundle().apply { putInt(EXTRA_POSITION, progress) })
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { handler.removeCallbacksAndMessages(null) }
            override fun onStopTrackingTouch(sb: SeekBar?) { startAutoUpdate() }
        })

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
    }

    override fun onDestroy() {
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
        sendToService(action, Bundle().apply { putFloat(EXTRA_GAIN, gainDb.toFloat()) })
    }

    private fun startAutoUpdate() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                seekBarProgress.progress = currentPosition
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun sendToService(action: String, extras: Bundle?) {
        Intent(this, AudioService::class.java).also { intent ->
            intent.action = action
            extras?.let { intent.putExtras(it) }
            startService(intent)
        }
    }

    private fun formatMs(ms: Int): String {
        val s = (ms / 1000) % 60
        val m = (ms / 1000) / 60
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
