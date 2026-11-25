package com.example.personalaccount

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.personalaccount.data.db.AppDatabase
import kotlinx.coroutines.launch
import java.util.Locale

class CoursesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var llContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_courses)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)
        llContainer = findViewById(R.id.llCoursesContainer)
        tvEmpty = findViewById(R.id.tvEmptyCourses)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadCoursesWithAvg() // Обновляем список и средние баллы при возврате на экран
    }

    private fun loadCoursesWithAvg() {
        lifecycleScope.launch {
            val courses = db.appDao().getAllDisciplines()
            llContainer.removeAllViews()

            if (courses.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                return@launch
            }
            tvEmpty.visibility = View.GONE

            for (course in courses) {
                val grades = db.appDao().getGradesForDiscipline(course.id)
                val avgText = if (grades.isEmpty()) {
                    "-"
                } else {
                    val avg = grades.map { it.value }.average()
                    String.format(Locale.US, "%.2f", avg)
                }

                val view = LayoutInflater.from(this@CoursesActivity)
                    .inflate(R.layout.item_course_avg, llContainer, false)

                view.findViewById<TextView>(R.id.tvCourseTitle).text = course.name
                view.findViewById<TextView>(R.id.tvCourseTeacher).text =
                    course.teacherName ?: "Преподаватель не указан"
                view.findViewById<TextView>(R.id.tvCourseAvg).text = avgText

                // Переход к экрану выбранной дисциплины
                val isAddMode = intent.getBooleanExtra("IS_ADD_MODE", false)

                view.setOnClickListener {
                    val intent = Intent(this@CoursesActivity, GradesActivity::class.java)
                    intent.putExtra("DISCIPLINE_ID", course.id)
                    intent.putExtra("DISCIPLINE_NAME", course.name)
                    intent.putExtra("DISCIPLINE_TEACHER", course.teacherName)
                    intent.putExtra("IS_ADD_MODE", isAddMode)
                    startActivity(intent)
                }


                llContainer.addView(view)
            }
        }
    }
}
