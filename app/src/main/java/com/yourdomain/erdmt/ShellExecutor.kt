
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
