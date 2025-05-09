package com.grupo11.equalizador

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        // As mesmas Actions que o seu AudioService conhece:
        const val ACTION_PLAY   = "PLAY"
        const val ACTION_PAUSE  = "PAUSE"
        const val ACTION_STOP   = "STOP"
        const val ACTION_SEEK   = "SEEK"
        const val EXTRA_TRACK   = "TRACK_RES_ID"
        const val EXTRA_POSITION = "SEEK_POSITION"
        const val ACTION_UPDATE_UI = "UPDATE_UI"       // Broadcast vindo do Service
        const val EXTRA_CURRENT_POS = "CURRENT_POSITION"
        const val EXTRA_DURATION    = "DURATION"
    }

    // UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var seekBarProgress: SeekBar
    private lateinit var textViewCurrentTime: TextView
    private lateinit var textViewTotalTime: TextView
    private lateinit var textViewSongTitle: TextView

    // Handler para animar o SeekBar (caso queira)
    private val handler = Handler()
    private var currentPosition = 0

    // Recebe atualizações do Service via LocalBroadcastManager
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_UI) {
                val pos = intent.getIntExtra(EXTRA_CURRENT_POS, 0)
                val dur = intent.getIntExtra(EXTRA_DURATION, 0)
                Log.d(TAG, "UI Broadcast: pos=$pos / dur=$dur")
                if (dur > 0) {
                    seekBarProgress.max = dur
                    seekBarProgress.progress = pos
                    textViewCurrentTime.text = formatMs(pos)
                    textViewTotalTime.text    = formatMs(dur)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate")

        // 1) findViewById de tudo
        recyclerView       = findViewById(R.id.audioTrackRecyclerView)
        playButton         = findViewById(R.id.button_play)
        pauseButton        = findViewById(R.id.button_pause)
        stopButton         = findViewById(R.id.button_stop)
        seekBarProgress    = findViewById(R.id.seekBarProgress)
        textViewCurrentTime= findViewById(R.id.textViewCurrentTime)
        textViewTotalTime  = findViewById(R.id.textViewTotalTime)
        textViewSongTitle  = findViewById(R.id.textViewSongTitle)

        // 2) Configura RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val tracks = loadAudioTracks()
        recyclerView.adapter = AudioAdapter(tracks) { track ->
            // Clique na faixa dispara PLAY com o resId
            textViewSongTitle.text = track.title
            sendToService(ACTION_PLAY, Bundle().apply {
                putInt(EXTRA_TRACK, track.resId)
            })
            Toast.makeText(this, "Tocando: ${track.title}", Toast.LENGTH_SHORT).show()
        }

        // 3) Botões Play / Pause / Stop
        playButton.setOnClickListener  { sendToService(ACTION_PLAY, null) }
        pauseButton.setOnClickListener { sendToService(ACTION_PAUSE, null) }
        stopButton.setOnClickListener  {
            // Para e reseta posição
            sendToService(ACTION_STOP, null)
            sendToService(ACTION_SEEK, Bundle().apply { putInt(EXTRA_POSITION, 0) })
        }

        // 4) SeekBar manual (usuário arrasta)
        seekBarProgress.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentPosition = progress
                    sendToService(ACTION_SEEK, Bundle().apply {
                        putInt(EXTRA_POSITION, progress)
                    })
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { handler.removeCallbacksAndMessages(null) }
            override fun onStopTrackingTouch(sb: SeekBar?) { startAutoUpdate() }
        })
        startAutoUpdate()

        // 5) Registra o receiver
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_UPDATE_UI))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        handler.removeCallbacksAndMessages(null)
    }

    private fun startAutoUpdate() {
        handler.postDelayed(object: Runnable {
            override fun run() {
                // mantem o indicador em tela, recebe updates reais via Broadcast
                seekBarProgress.progress = currentPosition
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun sendToService(action: String, extras: Bundle?) {
        Intent(this, AudioService::class.java).also {
            it.action = action
            extras?.let { b -> it.putExtras(b) }
            startService(it)
            Log.d(TAG, "Sent to Service → $action : $extras")
        }
    }

    private fun formatMs(ms: Int): String {
        val s = (ms / 1000) % 60
        val m = (ms / 1000) / 60
        return String.format("%d:%02d", m, s)
    }

    // Varre R.raw e cria lista
    private fun loadAudioTracks(): List<AudioTrack> {
        val list = mutableListOf<AudioTrack>()
        R.raw::class.java.fields.forEach { f ->
            runCatching {
                val id   = f.getInt(null)
                val name = f.name
                    .replace('_', ' ')
                    .split(' ')
                    .joinToString(" ") { it.capitalize() }
                list += AudioTrack(name, id)
            }
        }
        return list
    }
}
