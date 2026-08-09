package com.example.musicon.logic

import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    private val TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcContent.split("\n").forEach { line ->
            val matcher = TIME_PATTERN.matcher(line)
            if (matcher.find()) {
                val min = matcher.group(1)?.toLong() ?: 0L
                val sec = matcher.group(2)?.toLong() ?: 0L
                val ms = matcher.group(3)?.toLong() ?: 0L
                
                // Handle 2-digit ms (centiseconds) vs 3-digit ms
                val totalMs = min * 60 * 1000 + sec * 1000 + (if (matcher.group(3)?.length == 2) ms * 10 else ms)
                
                val text = line.substring(matcher.end()).trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(totalMs, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
