package com.example.personalaccount.data.model
    // импорт утилы из которой мы берём значения максимума и минимум для оценок
import java.util.Date

data class Grade (
    val id: Long,
    val courseName: String,
    val score: Int,
    val date: Date
){
    init {
        require(score in 2..5) {"Балл не может быть меньше 2 или больше 5"}
        require(courseName.isNotBlank()) {"Дисциплина не может быть пустой"}
    }
}