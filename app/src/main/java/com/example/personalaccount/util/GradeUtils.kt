package com.example.personalaccount.util
    // импорт grade в котором будет использоваться то что мы вводим в максимум иминимум
import com.example.personalaccount.data.model.Grade
    // Задаётся минимум и максимум в возможных оценках.
    // в будущем мы будем использовать эту функцию, но в других файлах
object GradeUtils {
    const val MIN_SCORE = 2
    const val MAX_SCORE = 5

    fun averageScore(grades: List<Grade>): Double? {
        if (grades.isEmpty()) return null
        return grades.map { it.score }.average()
    }
    // тут задаются цифры после запятой, но в нашем случае после точки
    fun formatAverage(avg: Double?): String {
        return if (avg == null) "Нет оценок" else String.format("%.2f", avg)
    }
}
