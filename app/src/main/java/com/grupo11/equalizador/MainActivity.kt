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
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_PLAY   = "PLAY"
        const val ACTION_PAUSE  = "PAUSE"
        const val ACTION_STOP   = "STOP"
        const val ACTION_SEEK   = "SEEK"
        const val EXTRA_TRACK   = "TRACK_RES_ID"
        const val EXTRA_POSITION = "SEEK_POSITION"
        const val ACTION_UPDATE_UI = "UPDATE_UI"
        const val EXTRA_CURRENT_POS = "CURRENT_POSITION"
        const val EXTRA_DURATION    = "DURATION"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var seekBarProgress: SeekBar
    private lateinit var textViewCurrentTime: TextView
    private lateinit var textViewTotalTime: TextView
    private lateinit var textViewSongTitle: TextView
    private var selectedTrackResId: Int? = null

    private val handler = Handler()
    private var currentPosition = 0

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("grupo 11", "onReceive - Broadcast received")
            val action = intent?.action
            Log.d("grupo 11", "Broadcast recebido → action=$action")

            if (intent?.action == ACTION_UPDATE_UI) {
                val pos = intent.getIntExtra(EXTRA_CURRENT_POS, 0)
                val dur = intent.getIntExtra(EXTRA_DURATION, 0)
                Log.d("grupo 112", "Broadcast recebido → pos=$pos dur=$dur")
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
        Log.d("grupo 11", "onCreate")

        recyclerView       = findViewById(R.id.audioTrackRecyclerView)
        playButton         = findViewById(R.id.button_play)
        pauseButton        = findViewById(R.id.button_pause)
        stopButton         = findViewById(R.id.button_stop)
        seekBarProgress    = findViewById(R.id.seekBarProgress)
        textViewCurrentTime= findViewById(R.id.textViewCurrentTime)
        textViewTotalTime  = findViewById(R.id.textViewTotalTime)
        textViewSongTitle  = findViewById(R.id.textViewSongTitle)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val intentFilter = IntentFilter("UPDATE_UI")
        ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)
        Log.d("grupo 11", "BroadcastReceiver registered.")

        val tracks = loadAudioTracks()
        recyclerView.adapter = AudioAdapter(tracks) { track ->
            textViewSongTitle.text = track.title
            selectedTrackResId = track.resId
            sendToService(ACTION_PLAY, Bundle().apply { putInt(EXTRA_TRACK, track.resId) })
            Toast.makeText(this, "Tocando: ${track.title}", Toast.LENGTH_SHORT).show()
            Log.d("grupo 11", "Faixa selecionada → ${track.title} id=${track.resId}")
        }

        playButton.setOnClickListener  {
            if (selectedTrackResId != null) {
                sendToService(ACTION_PLAY, Bundle().apply { putInt(EXTRA_TRACK, selectedTrackResId!!) })
                Log.d("grupo 11", "Play pressionado")
            } else {
                Toast.makeText(this, "Selecione uma faixa primeiro", Toast.LENGTH_SHORT).show()
            }
        }
        pauseButton.setOnClickListener {
            sendToService(ACTION_PAUSE, null)
            Log.d("grupo 11", "Pause pressionado")
        }
        stopButton.setOnClickListener  {
            sendToService(ACTION_STOP, null)
            sendToService(ACTION_SEEK, Bundle().apply { putInt(EXTRA_POSITION, 0) })
            seekBarProgress.max = 0
            seekBarProgress.progress = 0
            textViewCurrentTime.text = formatMs(0)
            textViewTotalTime.text    = formatMs(0)
            Log.d("grupo 11", "Stop pressionado")
        }

        seekBarProgress.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentPosition = progress
                    sendToService(ACTION_SEEK, Bundle().apply { putInt(EXTRA_POSITION, progress) })
                    Log.d("grupo 11", "Seek manual → $progress")
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { handler.removeCallbacksAndMessages(null) }
            override fun onStopTrackingTouch(sb: SeekBar?) { startAutoUpdate() }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        handler.removeCallbacksAndMessages(null)
        Log.d("grupo 11", "onDestroy")
    }

    private fun startAutoUpdate() {
        handler.postDelayed(object: Runnable {
            override fun run() {
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
            Log.d("grupo 11", "Intent enviada → $action $extras")
        }
    }

    private fun formatMs(ms: Int): String {
        val s = (ms / 1000) % 60
        val m = (ms / 1000) / 60
        return String.format("%d:%02d", m, s)
    }

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
        Log.d("grupo 11", "Tracks carregadas: ${list.size}")
        return list
    }
}
