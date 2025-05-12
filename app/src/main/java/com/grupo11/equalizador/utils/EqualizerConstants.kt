package com.grupo11.equalizador.utils

object EqualizerConstants {

    const val ACTION_PLAY            = "PLAY"
    const val ACTION_PAUSE           = "PAUSE"
    const val ACTION_STOP            = "STOP"
    const val ACTION_SEEK            = "SEEK"
    const val ACTION_UPDATE_UI       = "UPDATE_UI"
    const val ACTION_UPDATE_LOW_GAIN  = "UPDATE_LOW_GAIN"
    const val ACTION_UPDATE_MID_GAIN  = "UPDATE_MID_GAIN"
    const val ACTION_UPDATE_HIGH_GAIN = "UPDATE_HIGH_GAIN"

    const val EXTRA_CURRENT_POS      = "CURRENT_POSITION"
    const val EXTRA_DURATION         = "DURATION"
    const val EXTRA_TRACK            = "TRACK_RES_ID"
    const val EXTRA_POSITION         = "SEEK_POSITION"
    const val EXTRA_GAIN             = "GAIN"

    const val EXTRA_UI_PLAYING_STATE = "UI_STATE"

    /* faixa −15 dB … +15 dB  */
    const val DB_RANGE  = 30     // 0-30
    const val DB_OFFSET = 15     // progress 15 = 0 dB

    const val DEFAULT_NOTIFICATION_ID = 1
    const val DEFAULT_TRACK_RESOURCE_ID = -1
    const val DEFAULT_TRACK_ID = 1

    const val NOTIFICATION_CHANNEL_NAME = "Audio Service Channel"
    const val CHANNEL_ID = "AudioServiceChannel"

    const val LOG_TAG_WAV_RES_PLAYER = "WavResPlayer"
    const val LOG_TAG_AUDIO_SERVICE = "AudioService"
    const val LOG_TAG_MAIN_ACTIVITY = "MainActivity"

}