package com.grupo11.equalizador.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.annotation.RawRes
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavResPlayer(private val context: Context) {

    private var track: AudioTrack? = null
    // Thread that does the actual streaming
    private var worker: Thread? = null
    // Volatile flag to tell the worker to stop streaming
    @Volatile private var shouldStop = false

    // Gain factor for the audio stream
    private var mGain: Float = 1.0f
    /**
     * Play a WAV file stored in res/raw.
     * @param rawResId The resource ID, e.g. R.raw.my_sound
     */
    @Throws(IOException::class)
    fun play(@RawRes rawResId: Int) {
        // If already playing, shut it down cleanly
        stop()
        // Reset the stop‐flag
        shouldStop = false

        // Kick off the streaming on a background thread
        worker = Thread {
            // Open the raw resource as an InputStream
            context.resources.openRawResourceFd(rawResId)?.use { afd ->
                afd.createInputStream().use { input ->
                    // Parse WAV header (first 44 bytes)
                    val header = readWavHeader(input)

                    // Determine channel config
                    val channelConfig = when (header.numChannels) {
                        1 -> AudioFormat.CHANNEL_OUT_MONO
                        2 -> AudioFormat.CHANNEL_OUT_STEREO
                        else -> throw IOException("Unsupported channel count: ${header.numChannels}")
                    }

                    // Determine PCM encoding
                    val audioEncoding = when (header.bitsPerSample) {
                        8 -> AudioFormat.ENCODING_PCM_8BIT
                        16 -> AudioFormat.ENCODING_PCM_16BIT
                        else -> throw IOException("Unsupported bit depth: ${header.bitsPerSample}")
                    }

                    // Figure out a good buffer size
                    val minBufSize = AudioTrack.getMinBufferSize(
                        header.sampleRate,
                        channelConfig,
                        audioEncoding
                    ).coerceAtLeast(1024)

                    // Build the AudioTrack
                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(audioEncoding)
                                .setSampleRate(header.sampleRate)
                                .setChannelMask(channelConfig)
                                .build()
                        )
                        .setBufferSizeInBytes(minBufSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    // Start playback
                    track!!.play()

                    // Stream the PCM payload
                    val buffer = ByteArray(minBufSize)
                    while (!shouldStop) {
                        val read = input.read(buffer)
                        if (read <= 0) break

                        // attenuate in-place:
                        applyGain(buffer, read, header.bitsPerSample, mGain)

                        track!!.write(buffer, 0, read)
                    }

                    // Done
                    track!!.stop()
                    track!!.release()
                    track = null
                }
            } ?: throw IOException("Resource not found: $rawResId")
        }.also { it.start() }
    }

    /** Holds the core WAV header info we need. */
    private data class WavHeader(
        val sampleRate: Int,
        val numChannels: Int,
        val bitsPerSample: Int
    )

    /**
     * Reads exactly 44 bytes from the input stream and parses
     * sampleRate, channels and bit depth.
     */
    @Throws(IOException::class)
    private fun readWavHeader(wavStream: InputStream): WavHeader {
        val buffer = ByteBuffer.allocate(44)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity())
        buffer.rewind()
        buffer.position(buffer.position() + 20)
        val format = buffer.getShort()
        val channels = buffer.getShort()
        val rate = buffer.getInt()
        buffer.position(buffer.position() + 6)
        val bits = buffer.getShort()

        var dataSize = 0
        while (buffer.getInt() !== 0x61746164) { // "data" marker
            Log.d("WaveResPlayer", "Skipping non-data chunk")
            val size = buffer.getInt()
            wavStream.skip(size.toLong())

            buffer.rewind()
            wavStream.read(buffer.array(), buffer.arrayOffset(), 8)
            buffer.rewind()
        }
        dataSize = buffer.getInt()
        // We assume the next chunk is "data". If it's not, a more complex parser is needed.
        Log.d("WaveResPlayer"," format ${format}, SampleRate $rate , NumChannels $channels , bitsPerSample $bits" )

        return WavHeader(
            sampleRate = rate,
            numChannels = channels.toInt(),
            bitsPerSample = bits.toInt()
        )
    }
    /** Pause playback (AudioTrack keeps its buffer, you can resume). */
    fun pause() {
        track?.pause()
    }

    /** Resume after a pause(). */
    fun resume() {
        track?.play()
    }

    /**
     * Stops playback completely.
     * After calling stop() you must call play(...) again to restart.
     */
    fun stop() {
        // Signal the worker loop to exit
        shouldStop = true

        // Wait for the thread to finish
        worker?.join()
        worker = null

        // Just in case the track is still there
        track?.apply {
            stop()
            release()
        }
        track = null
    }

    fun updateGain(gain: Float) {
        this.mGain = gain
    }

    /**
     * Apply a linear gain (attenuation) to a PCM buffer in-place.
     *
     * @param buf       the raw PCM bytes
     * @param bytesRead how many bytes of valid data are in buf
     * @param bps       bits‐per‐sample (8 or 16)
     * @param gain      linear gain factor: 1.0 = no change, 0.5 = –6dB, 0.0 = silence
     */
    fun applyGain(buf: ByteArray, bytesRead: Int, bps: Int, gain: Float) {
        when (bps) {
            16 -> {
                // 16-bit PCM is signed little-endian. We walk in 2-byte steps,
                // combine into a Short, multiply, clamp, then split back to bytes.
                var i = 0
                while (i + 1 < bytesRead) {
                    // assemble little-endian short
                    val low  = buf[i].toInt() and 0xFF
                    val high = buf[i + 1].toInt()       // preserves sign in top-byte
                    val sample = (high shl 8) or low
                    // apply gain
                    val scaled = (sample * gain)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    // write back
                    buf[i]     = (scaled and 0xFF).toByte()
                    buf[i + 1] = ((scaled shr 8) and 0xFF).toByte()
                    i += 2
                }
            }

            8  -> {
                // 8-bit PCM is typically unsigned [0..255], zero at 128.
                // Convert to signed [-128..127], multiply, re‐bias to unsigned.
                for (i in 0 until bytesRead) {
                    val unsigned = (buf[i].toInt() and 0xFF)
                    val signed   = unsigned - 128
                    val scaled   = (signed * gain)
                        .toInt()
                        .coerceIn(-128, 127)
                    buf[i] = (scaled + 128).toByte()
                }
            }

            else -> {
                // unsupported format
                throw IllegalArgumentException("Only 8-bit or 16-bit PCM supported, got $bps bps")
            }
        }
    }

}