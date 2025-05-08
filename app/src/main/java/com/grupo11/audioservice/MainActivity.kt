package com.grupo11.audioservice

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

class MainActivity : AppCompatActivity() {

    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var seekBarProgress: SeekBar
    private lateinit var textViewCurrentTime: TextView
    private lateinit var textViewTotalTime: TextView

    private val handler = Handler()
    private var currentPosition = 0  // Track current position to update SeekBar

    // BroadcastReceiver to receive updates from the service
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("grupo11", "onReceive - Broadcast received")
            if (intent != null && intent.action == "UPDATE_UI") {
                currentPosition = intent.getIntExtra("CURRENT_POSITION", 0)
                val duration = intent.getIntExtra("DURATION", 0)

                Log.d("grupo11", "Broadcast received: Current Position = $currentPosition, Duration = $duration")

                // Update UI elements
                if (duration > 0) {
                    seekBarProgress.max = duration
                    seekBarProgress.progress = currentPosition
                    textViewCurrentTime.text = formatMilliseconds(currentPosition)
                    textViewTotalTime.text = formatMilliseconds(duration)

                    Log.d("grupo11", "Seek bar updated: Progress = ${seekBarProgress.progress}, Max = ${seekBarProgress.max}")
                } else {
                    Log.d("grupo11", "Duration is zero, UI not updated.")
                }
            } else {
                Log.d("grupo11", "Broadcast received with no action or null intent.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("grupo11", "onCreate - Activity Started")

        playButton = findViewById(R.id.button_play)
        pauseButton = findViewById(R.id.button_pause)
        stopButton = findViewById(R.id.button_stop)
        seekBarProgress = findViewById(R.id.seekBarProgress)
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime)
        textViewTotalTime = findViewById(R.id.textViewTotalTime)

        setupButtons()
        setupSeekBar()

        // Register the broadcast receiver to get updates
        val intentFilter = IntentFilter("UPDATE_UI")
        ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)
        Log.d("grupo11", "BroadcastReceiver registered.")
    }

    private fun setupButtons() {
        Log.d("grupo11", "Setting up buttons")
        playButton.setOnClickListener {
            Toast.makeText(this, "Play clicado", Toast.LENGTH_SHORT).show()
            sendAudioServiceCommand("PLAY")
            Log.d("grupo11", "Play button clicked.")
        }

        pauseButton.setOnClickListener {
            Toast.makeText(this, "Pause clicado", Toast.LENGTH_SHORT).show()
            sendAudioServiceCommand("PAUSE")
            Log.d("grupo11", "Pause button clicked.")
        }

        stopButton.setOnClickListener {
            Toast.makeText(this, "AudioService, Stop clicado", Toast.LENGTH_SHORT).show()
            sendAudioServiceCommand("STOP")
            Log.d("grupo11", "Stop button clicked.")
            val intent = Intent(this@MainActivity, AudioService::class.java)
            intent.action = "SEEK"
            intent.putExtra("SEEK_POSITION", 0)
            startService(intent)
            Log.d("grupo11", "User changed seek bar position to: $0")
            seekBarProgress.progress =0
        }
    }

    private fun sendAudioServiceCommand(action: String) {
        Log.d("grupo11", "sendAudioServiceCommand - Action: $action")
        val intent = Intent(this, AudioService::class.java)
        intent.action = action
        startService(intent)
        Log.d("grupo11", "Sent command to AudioService: $action")
    }

    private fun setupSeekBar() {
        Log.d("grupo11", "Setting up SeekBar")
        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d("grupo11", "SeekBar onProgressChanged - Progress: $progress, FromUser: $fromUser")
                if (fromUser) {
                    currentPosition = progress
                    val intent = Intent(this@MainActivity, AudioService::class.java)
                    intent.action = "SEEK"
                    intent.putExtra("SEEK_POSITION", progress)
                    startService(intent)
                    Log.d("grupo11", "User changed seek bar position to: $progress")
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.d("grupo11", "SeekBar onStartTrackingTouch")
                handler.removeCallbacksAndMessages(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.d("grupo11", "SeekBar onStopTrackingTouch")
                updateSeekBarProgress()
            }
        })
        updateSeekBarProgress()
    }

    private fun updateSeekBarProgress() {
        Log.d("grupo11", "updateSeekBarProgress - Updating progress")
        handler.postDelayed(object : Runnable {
            override fun run() {
                seekBarProgress.progress = currentPosition
                Log.d("grupo11", "Seek bar progress updated: Current Progress = $currentPosition")
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun formatMilliseconds(milliseconds: Int): String {
        Log.d("grupo11", "Formatting milliseconds: $milliseconds")
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        Log.d("grupo11", "BroadcastReceiver unregistered. Activity destroyed.")
    }
}
