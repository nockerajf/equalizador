package com.grupo11.equalizador

class NativeThreeBand(sampleRate: Int) : AutoCloseable {

    /* ponteiro C++ (jlong) */
    private var nativePtr: Long

    init {
        nativePtr = nativeCreate(sampleRate)
        check(nativePtr != 0L) { "Falha ao criar filtro nativo" }
    }

    fun init(
        lowCut: Float = 200f,
        midCenter: Float = 1_000f,
        highCut: Float = 5_000f,
        q: Float = 0.707f
    ) {
        nativeInit(nativePtr, lowCut, midCenter, highCut, q)
    }

    /** Processa um vetor mono de amostras float PCM.  */
    fun process(buffer: FloatArray) {
        if (nativePtr != 0L && buffer.isNotEmpty())
            nativeProcess(nativePtr, buffer, buffer.size)
    }
    /** Atualiza o ganho do filtro de graves. */
    fun updateLowBandGain(gain: Float) {
        if (nativePtr != 0L)
            nativeUpdateLowBandGain(gain)
    }
    /** Atualiza o ganho do filtro de médios. */
    fun updateMidBandGain(gain: Float) {
        if (nativePtr != 0L)
            nativeUpdateMidBandGain(gain)
    }
    /** Atualiza o ganho do filtro de agudos. */
    fun updateHighBandGain(gain: Float) {
        if (nativePtr != 0L)
            nativeUpdateHighBandGain(gain)
    }

    /** Libera a instância nativa (você também pode chamar close()). */
    fun release() = close()

    /* AutoCloseable */
    override fun close() {
        if (nativePtr != 0L) {
            nativeDestroy(nativePtr)
            nativePtr = 0
        }
    }

    /* ---------- métodos nativos ---------- */
    private external fun nativeInit(
        handle: Long,
        low: Float,
        mid: Float,
        high: Float,
        q: Float
    )

    private external fun nativeProcess(
        handle: Long,
        buffer: FloatArray,
        nFrames: Int
    )

    private external fun nativeDestroy(handle: Long)

    private external fun nativeCreate(sampleRate: Int): Long

    private external fun nativeUpdateLowBandGain(gain: Float)
    private external fun nativeUpdateMidBandGain(gain: Float)
    private external fun nativeUpdateHighBandGain(gain: Float)

    companion object {
        init {                      // carrega a .so apenas uma vez
            System.loadLibrary("threebandfilter")
        }
    }
}