package com.example.personalaccount.data.repository

import com.example.personalaccount.data.model.Grade
import com.example.personalaccount.util.GradeUtils

class GradeRapository {
    private val grades = mutableListOf<Grade>()
    private val nextId = 1L
    fun getALL(): List<Grade> = grades.toList()
    fun getByCourse(courseName: String): List<Grade> =
        grades.filter { it.courseName == courseName }
    fun add(courseName: String, score: Int, date: java.util.Date, type: String): Grade{
        val grade = Grade(nextId++, courseName, score, date, type)
        grades.add(grade)
        return grade
    }
    fun update(grade: Grade) {
        val idx = grades.indexOfFirst { it.id == grade.id }
        if (idx >= 0) grades[idx] = grade
    }
    fun delete(id: Long) {
        grades.removeAll { it.id == id }
    }
    fun averageForCourse(courseName: String): Double? =
        GradeUtils.averageScore(getByCourse(courseName))
    fun averageOverall(): Double? = GradeUtils.averageScore(grades)
}