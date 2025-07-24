
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
package com.yourdomain.erdmt

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object AudioRecorder {
    private const val TAG = "AudioRecorder"
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null
    
    fun startRecording(context: Context): String? {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "audio_$timestamp.3gp"
            outputFile = File(context.cacheDir, fileName).absolutePath
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            
            Log.d(TAG, "Recording started: $outputFile")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            return null
        }
    }
    
    fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val file = outputFile
            outputFile = null
            Log.d(TAG, "Recording stopped: $file")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            null
        }
    }
    
    fun uploadToFirebase(filePath: String, deviceId: String, callback: (Boolean, String?) -> Unit) {
        val file = File(filePath)
        if (!file.exists()) {
            callback(false, "File not found")
            return
        }
        
        val storage = FirebaseStorage.getInstance()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "audio_${deviceId}_$timestamp.3gp"
        val storageRef = storage.reference.child("audio/$fileName")
        
        storageRef.putFile(android.net.Uri.fromFile(file))
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(true, uri.toString())
                    file.delete() // Clean up local file
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Upload failed", exception)
                callback(false, exception.message)
            }
    }
}
