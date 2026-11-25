package com.example.personalaccount

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.personalaccount.data.db.AppDatabase
import com.example.personalaccount.data.db.Grade
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GradesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var disciplineId: Int = -1
    private lateinit var gridGrades: GridLayout
    private lateinit var tvAvg: TextView
    private lateinit var tvEmpty: TextView

    private val workTypes = arrayOf("Контрольная", "Практическая", "Лабораторная", "Ответ на паре", "Экзамен/Зачёт")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_grades)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)
        disciplineId = intent.getIntExtra("DISCIPLINE_ID", -1)
        val disciplineName = intent.getStringExtra("DISCIPLINE_NAME") ?: "Дисциплина"
        val teacherName = intent.getStringExtra("DISCIPLINE_TEACHER")

        findViewById<TextView>(R.id.tvDisciplineName).text = disciplineName
        findViewById<TextView>(R.id.tvTeacherName).text =
            if (teacherName.isNullOrBlank()) "Преподаватель не указан" else "Преподаватель: $teacherName"
        gridGrades = findViewById(R.id.gridGrades)
        tvAvg = findViewById(R.id.tvDisciplineAvg)
        tvEmpty = findViewById(R.id.tvEmptyGrades)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        val isAddMode = intent.getBooleanExtra("IS_ADD_MODE", false)
        val btnAddGrade = findViewById<Button>(R.id.btnOpenAddGrade)

        if (isAddMode) {
            btnAddGrade.visibility = View.VISIBLE
            btnAddGrade.setOnClickListener {
                showGradeFormDialog(existingGrade = null)
            }
        } else {
            btnAddGrade.visibility = View.GONE
        }


        loadGrades()
    }

    private fun loadGrades() {
        lifecycleScope.launch {
            val grades = db.appDao().getGradesForDiscipline(disciplineId)
            gridGrades.removeAllViews()

            if (grades.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                tvAvg.text = "—"
                return@launch
            }
            tvEmpty.visibility = View.GONE

            // Расчёт среднего балла
            val avg = grades.map { it.value }.average()
            tvAvg.text = String.format(Locale.US, "%.2f", avg)

            // Отрисовка плиток с оценками (как на макете)
            val sizePx = (56 * resources.displayMetrics.density).toInt()
            val marginPx = (6 * resources.displayMetrics.density).toInt()

            for (grade in grades) {
                val tvTile = TextView(this@GradesActivity).apply {
                    text = grade.value.toString()
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.dark_bg))
                    setBackgroundResource(R.drawable.bg_pink_rounded)

                    layoutParams = GridLayout.LayoutParams().apply {
                        width = sizePx
                        height = sizePx
                        setMargins(marginPx, marginPx, marginPx, marginPx)
                    }

                    // Нажатие на плитку -> окно "Что делаем?"
                    setOnClickListener { showGradeActionsDialog(grade) }
                }
                gridGrades.addView(tvTile)
            }
        }
    }

    // Окно просмотра деталей, редактирования и удаления (как на 7-м фото)
    private fun showGradeActionsDialog(grade: Grade) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_grade_actions, null)

        val tvValue = dialogView.findViewById<TextView>(R.id.tvDialogGradeValue)
        val tvType = dialogView.findViewById<TextView>(R.id.tvDialogGradeType)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDialogGradeDate)
        val tvComment = dialogView.findViewById<TextView>(R.id.tvDialogGradeComment)

        val btnEdit = dialogView.findViewById<Button>(R.id.btnActionEdit)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnActionDelete)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnActionCancel)

        tvValue.text = grade.value.toString()
        tvType.text = "Тип: ${grade.workType}"
        tvDate.text = "Дата: ${grade.date}"
        val commentText = if (grade.comment.isNullOrBlank()) "нет" else grade.comment
        tvComment.text = "Комментарий: $commentText"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Убираем стандартный белый фон системного окна
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showGradeFormDialog(existingGrade = grade)
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            val confirmView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
            val tvTitle = confirmView.findViewById<TextView>(R.id.tvConfirmTitle)
            val btnCancelConfirm = confirmView.findViewById<Button>(R.id.btnConfirmCancel)
            val btnYesConfirm = confirmView.findViewById<Button>(R.id.btnConfirmYes)

            tvTitle.text = "Удалить?"
            btnYesConfirm.text = "Удалить"

            val confirmDialog = AlertDialog.Builder(this)
                .setView(confirmView)
                .create()
            confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnCancelConfirm.setOnClickListener { confirmDialog.dismiss() }
            btnYesConfirm.setOnClickListener {
                lifecycleScope.launch {
                    db.appDao().deleteGrade(grade)
                    loadGrades()
                    confirmDialog.dismiss()
                }
            }
            confirmDialog.show()
        }


        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    // Форма создания и редактирования оценки с валидацией
    private fun showGradeFormDialog(existingGrade: Grade?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_grade_form, null)
        val etValue = dialogView.findViewById<EditText>(R.id.etGradeValue)
        val etDate = dialogView.findViewById<EditText>(R.id.etGradeDate)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerWorkType)
        val etComment = dialogView.findViewById<EditText>(R.id.etGradeComment)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        // Адаптер для Spinner, чтобы текст внутри тёмной капсулы был розовым и по центру
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, workTypes) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(ContextCompat.getColor(context, R.color.pink_accent))
                tv.gravity = Gravity.CENTER
                tv.textSize = 16f
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
                return tv
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter

        if (existingGrade != null) {
            etValue.setText(existingGrade.value.toString())
            etDate.setText(existingGrade.date)
            etComment.setText(existingGrade.comment ?: "")
            val typeIndex = workTypes.indexOf(existingGrade.workType)
            if (typeIndex >= 0) spinnerType.setSelection(typeIndex)
        } else {
            val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            etDate.setText(today)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Делаем стандартный белый фон системного окна прозрачным
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val valueInt = etValue.text.toString().trim().toIntOrNull()
            val dateStr = etDate.text.toString().trim()
            val typeStr = spinnerType.selectedItem.toString()
            val commentStr = etComment.text.toString().trim().ifEmpty { null }

            if (valueInt == null || valueInt !in 2..5) {
                Toast.makeText(this, "Оценка должна быть числом от 2 до 5", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dateStr.isEmpty()) {
                Toast.makeText(this, "Укажите дату", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (existingGrade == null) {
                    db.appDao().insertGrade(
                        Grade(
                            disciplineId = disciplineId,
                            value = valueInt,
                            date = dateStr,
                            workType = typeStr,
                            comment = commentStr
                        )
                    )
                } else {
                    db.appDao().updateGrade(
                        existingGrade.copy(
                            value = valueInt,
                            date = dateStr,
                            workType = typeStr,
                            comment = commentStr
                        )
                    )
                }
                loadGrades()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

}
