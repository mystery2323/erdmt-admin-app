package com.yourdomain.erdmt

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File

object MicRecorder {
    private var recorder: MediaRecorder? = null
    private var output: String? = null

    fun startRecording(context: Context): String? {
        output = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/recording_${System.currentTimeMillis()}.3gp"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(output)
            prepare()
            start()
        }
        return output
    }

    fun stopRecording(): String? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return output
    }
}