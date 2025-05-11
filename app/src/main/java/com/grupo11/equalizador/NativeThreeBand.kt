package com.grupo11.equalizador

class NativeThreeBand(sampleRate: Int) : AutoCloseable {

    /* ponteiro C++ (jlong) */
    private var nativePtr: Long = nativeCreate(sampleRate).also {
        check(it != 0L) { "Falha ao criar filtro nativo" }
    }

    /* ---------------- inicialização dos filtros ---------------- */
    fun init(
        lowCut: Float = 200f,
        midCenter: Float = 1_000f,
        highCut: Float = 5_000f,
        q: Float = 0.707f
    ) = nativeInit(nativePtr, lowCut, midCenter, highCut, q)

    /* ---------------- processamento ---------------- */
    fun process(buffer: FloatArray) {
        if (buffer.isNotEmpty())
            nativeProcess(nativePtr, buffer, buffer.size)
    }

    /* ---------------- ajustes de ganho em dB ---------------- */
    fun updateLowBandGain (gainDb: Float) = nativeUpdateLowBandGain (nativePtr, gainDb)
    fun updateMidBandGain (gainDb: Float) = nativeUpdateMidBandGain (nativePtr, gainDb)
    fun updateHighBandGain(gainDb: Float) = nativeUpdateHighBandGain(nativePtr, gainDb)

    /* ---------------- liberação ---------------- */
    override fun close() {
        if (nativePtr != 0L) {
            nativeDestroy(nativePtr)
            nativePtr = 0
        }
    }
    fun release() = close()

    /* ---------------- métodos nativos ---------------- */
    private external fun nativeCreate(sampleRate: Int): Long
    private external fun nativeDestroy(handle: Long)

    private external fun nativeInit(
        handle: Long, low: Float, mid: Float, high: Float, q: Float
    )

    private external fun nativeProcess(
        handle: Long, buffer: FloatArray, nFrames: Int
    )

    /* os três setters agora recebem HANDLE + ganho */
    private external fun nativeUpdateLowBandGain (handle: Long, gainDb: Float)
    private external fun nativeUpdateMidBandGain (handle: Long, gainDb: Float)
    private external fun nativeUpdateHighBandGain(handle: Long, gainDb: Float)

    companion object {
        init { System.loadLibrary("threebandfilter") }
    }
}
