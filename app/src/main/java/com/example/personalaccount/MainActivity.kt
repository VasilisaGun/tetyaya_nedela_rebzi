package com.example.personalaccount

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.personalaccount.util.GradeUtils
import java.util.Date

class MainActivity : AppCompatActivity() {

    // Подключаем ваш репозиторий


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Находим элементы на экране по их ID
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val btnCourses = findViewById<Button>(R.id.btnCourses)
        val btnAddGrade = findViewById<Button>(R.id.btnAddGrade)
        val btnAddCourse = findViewById<Button>(R.id.btnAddCourse)
        val btnDynamics = findViewById<Button>(R.id.btnDynamics)

// Переход на экран добавления дисциплины
        btnAddCourse.setOnClickListener {
            val intent = android.content.Intent(this, AddCourseActivity::class.java)
            startActivity(intent)
        }
        btnCourses.setOnClickListener {
            val intent = android.content.Intent(this, CoursesActivity::class.java)
            intent.putExtra("IS_ADD_MODE", false) // Только просмотр
            startActivity(intent)
        }

        btnAddGrade.setOnClickListener {
            val intent = android.content.Intent(this, CoursesActivity::class.java)
            intent.putExtra("IS_ADD_MODE", true) // Режим добавления оценки
            startActivity(intent)
        }




    }
}
