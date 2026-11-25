package com.example.personalaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.personalaccount.data.db.AppDatabase
import com.example.personalaccount.data.db.Discipline
import kotlinx.coroutines.launch

class AddCourseActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var llCoursesList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_course)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)
        llCoursesList = findViewById(R.id.llCoursesList)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etCourseName = findViewById<EditText>(R.id.etCourseName)
        val etTeacherName = findViewById<EditText>(R.id.etTeacherName)
        val btnSaveCourse = findViewById<Button>(R.id.btnSaveCourse)

        btnBack.setOnClickListener { finish() }

        btnSaveCourse.setOnClickListener {
            val name = etCourseName.text.toString().trim()
            val teacher = etTeacherName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название дисциплины", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Кастомное окно "Сохранить?" из dialog_confirm.xml
            val confirmView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
            val tvTitle = confirmView.findViewById<TextView>(R.id.tvConfirmTitle)
            val btnCancel = confirmView.findViewById<Button>(R.id.btnConfirmCancel)
            val btnYes = confirmView.findViewById<Button>(R.id.btnConfirmYes)

            tvTitle.text = "Сохранить?"
            btnYes.text = "Сохранить"

            val confirmDialog = AlertDialog.Builder(this)
                .setView(confirmView)
                .create()
            confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnCancel.setOnClickListener { confirmDialog.dismiss() }
            btnYes.setOnClickListener {
                lifecycleScope.launch {
                    db.appDao().insertDiscipline(
                        Discipline(name = name, teacherName = teacher.ifEmpty { null })
                    )
                    etCourseName.text.clear()
                    etTeacherName.text.clear()
                    loadCourses()
                    confirmDialog.dismiss()
                }
            }
            confirmDialog.show()
        }

        loadCourses()
    }

    private fun loadCourses() {
        lifecycleScope.launch {
            val courses = db.appDao().getAllDisciplines()
            llCoursesList.removeAllViews()

            for (course in courses) {
                val itemView = LayoutInflater.from(this@AddCourseActivity)
                    .inflate(R.layout.item_course, llCoursesList, false)

                val tvName = itemView.findViewById<TextView>(R.id.tvItemCourseName)
                val tvTeacher = itemView.findViewById<TextView>(R.id.tvItemTeacherName)
                val btnDelete = itemView.findViewById<Button>(R.id.btnDeleteCourse)

                tvName.text = course.name
                tvTeacher.text = if (course.teacherName.isNullOrBlank()) {
                    "Преподаватель не указан"
                } else {
                    "Преподаватель: ${course.teacherName}"
                }

                btnDelete.setOnClickListener {
                    // Кастомное окно "Удалить?" из dialog_confirm.xml
                    val confirmView = LayoutInflater.from(this@AddCourseActivity)
                        .inflate(R.layout.dialog_confirm, null)
                    val tvTitle = confirmView.findViewById<TextView>(R.id.tvConfirmTitle)
                    val btnCancel = confirmView.findViewById<Button>(R.id.btnConfirmCancel)
                    val btnYes = confirmView.findViewById<Button>(R.id.btnConfirmYes)

                    tvTitle.text = "Удалить?"
                    btnYes.text = "Удалить"

                    val confirmDialog = AlertDialog.Builder(this@AddCourseActivity)
                        .setView(confirmView)
                        .create()
                    confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                    btnCancel.setOnClickListener { confirmDialog.dismiss() }
                    btnYes.setOnClickListener {
                        lifecycleScope.launch {
                            db.appDao().deleteDiscipline(course)
                            loadCourses()
                            confirmDialog.dismiss()
                        }
                    }
                    confirmDialog.show()
                }

                llCoursesList.addView(itemView)
            }
        }
    }
}
