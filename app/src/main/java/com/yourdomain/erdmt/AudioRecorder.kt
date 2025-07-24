
package com.yourdomain.erdmt

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File
import java.io.IOException

object AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null
    
    fun recordAudio(context: Context, durationMs: Long): String {
        val timestamp = System.currentTimeMillis()
        outputFile = "${context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/audio_$timestamp.3gp"
        
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            
            try {
                prepare()
                start()
                
                // Stop recording after specified duration
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    stopRecording()
                }, durationMs)
                
            } catch (e: IOException) {
                throw RuntimeException("Failed to start recording: ${e.message}")
            }
        }
        
        return outputFile ?: throw RuntimeException("Output file not set")
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Recording might have been stopped already
        } finally {
            mediaRecorder = null
        }
    }
}
