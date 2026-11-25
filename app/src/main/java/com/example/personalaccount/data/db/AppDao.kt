package com.example.personalaccount.data.db

import androidx.room.*

@Dao
interface AppDao {
    // --- Дисциплины ---
    @Query("SELECT * FROM disciplines ORDER BY id DESC")
    suspend fun getAllDisciplines(): List<Discipline>

    @Insert
    suspend fun insertDiscipline(discipline: Discipline)

    @Delete
    suspend fun deleteDiscipline(discipline: Discipline)

    // --- Оценки ---
    @Query("SELECT * FROM grades WHERE disciplineId = :disciplineId ORDER BY id DESC")
    suspend fun getGradesForDiscipline(disciplineId: Int): List<Grade>

    @Query("SELECT * FROM grades")
    suspend fun getAllGrades(): List<Grade>

    @Insert
    suspend fun insertGrade(grade: Grade)

    @Update
    suspend fun updateGrade(grade: Grade)

    @Delete
    suspend fun deleteGrade(grade: Grade)
}
