package com.grupo11.equalizador.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.annotation.RawRes
import com.grupo11.equalizador.NativeThreeBand
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

    private lateinit var filter: NativeThreeBand
    private val sr = 48_000
    init {
        filter = NativeThreeBand(sr)
        filter.init(lowCut = 200f, midCenter = 1_000f, highCut = 5_000f)

        // Exemplo de buffer de teste
        val pcm = FloatArray(1024) { Math.sin(2.0 * Math.PI * 440 * it / sr).toFloat() }
        filter.process(pcm)
    }

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
                    val buffer = ByteArray(minBufSize )
                    while (!shouldStop) {
                        val startTime = System.currentTimeMillis() // Marca o início

                        val read = input.read(buffer)
                        Log.d("WavResPlayer", "Read $read bytes from input stream")
                        if (read <= 0) break

                        val bytesPerSample = header.bitsPerSample / 8
                        val numSamples = read / bytesPerSample
                        Log.d("WavResPlayer", "Calculated numSamples: $numSamples (Bytes read: $read, Bytes per sample: $bytesPerSample)") // Log 2: Cálculo do número de samples

                        val floatBuffer = FloatArray(numSamples)

// 1. Converter ByteArray (PCM) para FloatArray
                        if (header.bitsPerSample == 16) {
                            Log.d("WavResPlayer", "Converting 16-bit PCM to FloatArray") // Log 3: Início da conversão 16-bit
                            val byteBuffer = ByteBuffer.wrap(buffer, 0, read)
                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

                            var floatIdx = 0
                            while (byteBuffer.remaining() >= 2 && floatIdx < numSamples) {
                                val sample = byteBuffer.getShort()
                                floatBuffer[floatIdx] = sample.toFloat() / Short.MAX_VALUE.toFloat()
                                floatIdx += 1
                            }
                            if (floatIdx != numSamples) {
                                Log.e("WavResPlayer", "Mismatch during 16-bit conversion: Expected $numSamples floats, got $floatIdx") // Log 4: Verificação de consistência na conversão
                            }
                            // Opcional: logar alguns valores do floatBuffer convertido
                            if (numSamples > 0) {
                                Log.d("WavResPlayer", "First float sample: ${floatBuffer[0]}")
                                if (numSamples > 100) Log.d("WavResPlayer", "100th float sample: ${floatBuffer[99]}")
                                Log.d("WavResPlayer", "Last float sample: ${floatBuffer[numSamples - 1]}")
                            }

                        } else {
                            Log.e("WavResPlayer", "Unsupported bit depth for native processing: ${header.bitsPerSample}") // Log 5: Formato não suportado
                            // Se o formato não for suportado e você decidiu pular, logar isso.
                            // Por enquanto, o código atual lança exceção. Se você mudar para pular:
                            // track!!.write(buffer, 0, read)
                            // continue // Pula para a próxima iteração do loop
                            throw IOException("Unsupported bit depth for native processing: ${header.bitsPerSample}") // Manter a exceção se a lógica for essa
                        }


// 2. Processar o buffer de floats com o equalizador C++
                        Log.d("WavResPlayer", "Calling native filter process()") // Log 6: Antes de chamar o C++
                        if (::filter.isInitialized) {
                            filter.process(floatBuffer) // Assumindo que 'process' modifica 'floatBuffer' in-place
                            Log.d("WavResPlayer", "Native filter process() returned") // Log 7: Depois de chamar o C++
                            // Opcional: logar alguns valores do floatBuffer APÓS o processamento
                            if (numSamples > 0) {
                                Log.d("WavResPlayer", "First float sample after process: ${floatBuffer[0]}")
                                if (numSamples > 100) Log.d("WavResPlayer", "100th float sample after process: ${floatBuffer[99]}")
                                Log.d("WavResPlayer", "Last float sample after process: ${floatBuffer[numSamples - 1]}")
                            }
                        } else {
                            Log.e("WavResPlayer", "Native filter not initialized! Skipping native processing.") // Log 8: Filtro não inicializado
                            // Se o filtro não estiver inicializado, o floatBuffer não foi modificado.
                        }


// 3. Converter FloatArray processado de volta para ByteArray (PCM)
                        if (header.bitsPerSample == 16) {
                            Log.d("WavResPlayer", "Converting FloatArray back to 16-bit PCM ByteArray") // Log 9: Início da conversão de volta
                            val byteBuffer = ByteBuffer.wrap(buffer, 0, numSamples * 2)
                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

                            var floatIdx = 0
                            while (floatIdx < numSamples) {
                                val scaledSample = (floatBuffer[floatIdx] * Short.MAX_VALUE.toFloat()).toInt()
                                val finalSample = scaledSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                byteBuffer.putShort(finalSample.toShort())
                                floatIdx += 1
                            }
                            // Opcional: logar alguns valores do buffer convertido de volta
                            if (read > 1) { // Precisa de pelo menos 2 bytes para 1 sample de 16-bit
                                Log.d("WavResPlayer", "First byte after conversion back: ${buffer[0]}, Second byte: ${buffer[1]}")
                                if (read > 200) Log.d("WavResPlayer", "Bytes 200/201 after conversion back: ${buffer[199]}, ${buffer[200]}")
                                Log.d("WavResPlayer", "Last byte after conversion back: ${buffer[read - 1]}")
                            }

                        } else {
                            // Lógica de conversão de volta para outros formatos, se houver.
                            // Se você pulou o processamento nativo (Log 5 ou Log 8), você pode querer escrever o buffer original aqui.
                            Log.d("WavResPlayer", "Skipping conversion back for unsupported bit depth or uninitialized filter.")
                        }

                        val written = track!!.write(buffer, 0, read)
                        Log.d("WavResPlayer", "Wrote $written bytes to AudioTrack (Expected $read)")

                        val endTime = System.currentTimeMillis() // Marca o fim
                        val duration = endTime - startTime
                        Log.d("WavResPlayer", "Buffer processing took $duration ms") // Loga a duração

                        if (written < read) {
                            Log.w("WavResPlayer", "AudioTrack write warning: Wrote $written bytes, expected $read")
                        }
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

    fun updateLowBandGain(gainDb: Float) {
        filter.updateLowBandGain(gainDb)
    }
    fun updateMidBandGain(gainDb: Float) {
        filter.updateMidBandGain(gainDb)
    }
    fun updateHighBandGain(gainDb: Float) {
        filter.updateHighBandGain(gainDb)
    }

}