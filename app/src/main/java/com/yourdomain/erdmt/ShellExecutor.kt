
package com.yourdomain.erdmt

import java.io.BufferedReader
import java.io.InputStreamReader

object ShellExecutor {
    fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            
            if (output.isEmpty()) {
                "Command executed successfully (no output)"
            } else {
                output.toString().trim()
            }
        } catch (e: Exception) {
            "Error executing command: ${e.message}"
        }
    }
}
package com.yourdomain.erdmt

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object ShellExecutor {
    private const val TAG = "ShellExecutor"
    
    suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing command: $command")
            
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            
            // Read output
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            // Read errors
            while (errorReader.readLine().also { line = it } != null) {
                output.append("ERROR: ").append(line).append("\n")
            }
            
            val exitCode = process.waitFor()
            output.append("Exit code: $exitCode")
            
            val result = output.toString()
            Log.d(TAG, "Command result: $result")
            
            result
        } catch (e: Exception) {
            val error = "Command execution failed: ${e.message}"
            Log.e(TAG, error, e)
            error
        }
    }
}
