#include <jni.h>
#include <string>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <android/log.h>

#define LOG_TAG "NativeEQ"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// OpenSL ES engine and output mix
SLObjectItf engineObject = nullptr;
SLEngineItf engineEngine = nullptr;
SLObjectItf outputMixObject = nullptr;

// Audio player
SLObjectItf playerObject = nullptr;
SLPlayItf playerPlay = nullptr;
SLEqualizerItf equalizer = nullptr;

// Initialize OpenSL ES engine
void createEngine() {
    SLresult result;

    // Create engine
    result = slCreateEngine(&engineObject, 0, nullptr, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create OpenSL ES engine");
        return;
    }

    // Realize the engine
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize OpenSL ES engine");
        return;
    }

    // Get the engine interface
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get OpenSL ES engine interface");
        return;
    }

    // Create output mix
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create output mix");
        return;
    }

    // Realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize output mix");
        return;
    }
}

// Create an audio player and apply an equalizer
void createPlayerWithEqualizer(const char *audioPath) {
    SLresult result;

    // Configure audio source
    SLDataLocator_URI locUri = {SL_DATALOCATOR_URI, (SLchar *)audioPath};
    SLDataFormat_MIME formatMime = {SL_DATAFORMAT_MIME, nullptr, SL_CONTAINERTYPE_UNSPECIFIED};
    SLDataSource audioSrc = {&locUri, &formatMime};

    // Configure audio sink
    SLDataLocator_OutputMix locOutMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&locOutMix, nullptr};

    // Create audio player
    const SLInterfaceID ids[] = {SL_IID_PLAY, SL_IID_EQUALIZER};
    const SLboolean req[] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc, &audioSnk, 2, ids, req);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create audio player");
        return;
    }

    // Realize the player
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize audio player");
        return;
    }

    // Get the play interface
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get play interface");
        return;
    }

    // Get the equalizer interface
    result = (*playerObject)->GetInterface(playerObject, SL_IID_EQUALIZER, &equalizer);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get equalizer interface");
        return;
    }

    // Apply equalizer settings (example: boost band 0 by 5 dB)
    SLuint16 numBands;
    (*equalizer)->GetNumberOfBands(equalizer, &numBands);
    if (numBands > 0) {
        SLmillibel minLevel, maxLevel;
        (*equalizer)->GetBandLevelRange(equalizer, &minLevel, &maxLevel);
        (*equalizer)->SetBandLevel(equalizer, 0, minLevel + (maxLevel - minLevel) / 2); // Boost band 0
    }

    // Start playback
    (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_grupo11_equalizador_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_grupo11_equalizador_MainActivity_initEngine(JNIEnv *env, jobject /* this */) {
    createEngine();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_grupo11_equalizador_MainActivity_playAudioWithEqualizer(JNIEnv *env, jobject /* this */, jstring audioPath) {
    const char *path = env->GetStringUTFChars(audioPath, nullptr);
    createPlayerWithEqualizer(path);
    env->ReleaseStringUTFChars(audioPath, path);
}