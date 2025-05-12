#include <jni.h>
#include "ThreeBandFilter.h"
#include <android/log.h>
#include <cmath>   // powf()



#define LOG_TAG "NativeThreeBand"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define JNI_FN(pkg, cls, name) \
  Java_##pkg##_##cls##_##name
/* ---------- dB → linear ---------- */
static inline float db2lin(float db) {     // agora visível
    return powf(10.f, db / 20.f);
}
extern "C" {

// ------- cria nova instância e devolve ponteiro como jlong
JNIEXPORT jlong JNICALL
JNI_FN(com_grupo11_equalizador, NativeThreeBand, nativeCreate)
        (JNIEnv *, jobject, jint sampleRate) {
    auto *f = new ThreeBandFilter();
    f->init(static_cast<float>(sampleRate));   // usa defaults de cutoff
    return reinterpret_cast<jlong>(f);
}

// ------- configura novamente (cutoffs em Hz, mesmo Q p/ 3 bandas)
JNIEXPORT void JNICALL
JNI_FN(com_grupo11_equalizador, NativeThreeBand, nativeInit)
        (JNIEnv *, jobject,
         jlong handle, jfloat low, jfloat mid, jfloat high, jfloat q) {
    auto *f = reinterpret_cast<ThreeBandFilter *>(handle);
    if (f) f->init(0 /*sampleRate ignorado*/, low, mid, high, q);
}

// ------- processa buffer in-place (float[] do Java)
JNIEXPORT void JNICALL
JNI_FN(com_grupo11_equalizador, NativeThreeBand, nativeProcess)
        (JNIEnv *env, jobject,
         jlong handle, jfloatArray javaBuf, jint nFrames) {
    auto *f = reinterpret_cast<ThreeBandFilter *>(handle);
    if (!f || nFrames <= 0) return;

    jfloat *buf = env->GetFloatArrayElements(javaBuf, nullptr);
    f->processBuffer(buf, static_cast<size_t>(nFrames));
    env->ReleaseFloatArrayElements(javaBuf, buf, 0); // copy back
}

// ------- libera memória
JNIEXPORT void JNICALL
JNI_FN(com_grupo11_equalizador, NativeThreeBand, nativeDestroy)
        (JNIEnv *, jobject, jlong handle) {
    auto *f = reinterpret_cast<ThreeBandFilter *>(handle);
    delete f;
}

// -------- LOW ----------
JNIEXPORT void JNICALL
JNI_FN(com_grupo11_equalizador, NativeThreeBand, nativeUpdateLowBandGain)
        (JNIEnv*, jobject, jlong handle, jfloat gainDb) {
    auto* f = reinterpret_cast<ThreeBandFilter*>(handle);
    if (f) f->setLowGain(db2lin(gainDb));
    LOGD("LowGain %.2f dB", gainDb);
}

// -------- MID ----------
JNIEXPORT void JNICALL
JNI_FN(com_grupo11_equalizador, NativeThreeBand, nativeUpdateMidBandGain)
        (JNIEnv*, jobject, jlong handle, jfloat gainDb) {
    auto* f = reinterpret_cast<ThreeBandFilter*>(handle);
    if (f) f->setMidGain(db2lin(gainDb));
    LOGD("MidGain %.2f dB", gainDb);
}

// -------- HIGH ----------
JNIEXPORT void JNICALL
JNI_FN(com_grupo11_equalizador, NativeThreeBand, nativeUpdateHighBandGain)
        (JNIEnv*, jobject, jlong handle, jfloat gainDb) {
    auto* f = reinterpret_cast<ThreeBandFilter*>(handle);
    if (f) f->setHighGain(db2lin(gainDb));
    LOGD("HighGain %.2f dB", gainDb);
}

} // extern "C"
