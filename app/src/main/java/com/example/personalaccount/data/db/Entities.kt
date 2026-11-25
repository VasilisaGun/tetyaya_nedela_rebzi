package com.example.personalaccount.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "disciplines")
data class Discipline(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val teacherName: String? = null
)

@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = Discipline::class,
            parentColumns = ["id"],
            childColumns = ["disciplineId"],
            onDelete = ForeignKey.CASCADE // При удалении предмета удаляются его оценки
        )
    ],
    indices = [Index("disciplineId")]
)
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val disciplineId: Int,
    val value: Int,          // Оценка (например, от 2 до 5)
    val date: String,        // Дата (например, "25.11.2026")
    val workType: String,    // Тип работы (Контрольная, Практическая и т.д.)
    val comment: String? = null
)
