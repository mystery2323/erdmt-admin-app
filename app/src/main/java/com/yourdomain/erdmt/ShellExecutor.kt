package com.yourdomain.erdmt

import java.io.BufferedReader
import java.io.InputStreamReader

object ShellExecutor {
    fun execute(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readText()
        } catch (e: Exception) {
            e.message ?: "Error"
        }
    }
}