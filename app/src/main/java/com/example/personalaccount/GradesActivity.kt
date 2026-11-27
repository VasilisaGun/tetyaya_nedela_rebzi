package com.example.personalaccount

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private enum class SortOrder {
        NEWEST, OLDEST, ASCENDING, DESCENDING
    }

    private var currentSortOrder = SortOrder.NEWEST
    private var filterDate: String? = null
    private var filterWorkType: String? = null

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
        findViewById<ImageButton>(R.id.btnFilter).setOnClickListener { showFilterDialog() }
        findViewById<ImageButton>(R.id.btnSort).setOnClickListener { showSortDialog() }

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
            val rawGrades = db.appDao().getGradesForDiscipline(disciplineId)
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            val filteredGrades = rawGrades.filter { grade ->
                val matchesDate = filterDate == null || grade.date == filterDate
                val matchesType = filterWorkType == null || grade.workType == filterWorkType
                matchesDate && matchesType
            }

            val grades = when (currentSortOrder) {
                SortOrder.NEWEST -> filteredGrades.sortedWith(
                    compareByDescending<Grade> { parseDateMillis(it.date, dateFormat) }
                        .thenByDescending { it.id }
                )
                SortOrder.OLDEST -> filteredGrades.sortedWith(
                    compareBy<Grade> { parseDateMillis(it.date, dateFormat) }
                        .thenBy { it.id }
                )
                SortOrder.ASCENDING -> filteredGrades.sortedWith(
                    compareBy<Grade> { it.value }
                        .thenByDescending { it.id }
                )
                SortOrder.DESCENDING -> filteredGrades.sortedWith(
                    compareByDescending<Grade> { it.value }
                        .thenByDescending { it.id }
                )
            }

            gridGrades.removeAllViews()

            if (grades.isEmpty()) {
                tvEmpty.text = if (filterDate != null || filterWorkType != null) {
                    "По выбранному фильтру оценок нет."
                } else {
                    "Оценок по этой дисциплине пока нет."
                }
                tvEmpty.visibility = View.VISIBLE
                tvAvg.text = "—"
                return@launch
            }
            tvEmpty.visibility = View.GONE

            val avg = grades.map { it.value }.average()
            tvAvg.text = String.format(Locale.US, "%.2f", avg)

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

                    setOnClickListener { showGradeActionsDialog(grade) }
                }
                gridGrades.addView(tvTile)
            }
        }
    }

    private fun parseDateMillis(dateStr: String, format: SimpleDateFormat): Long {
        return try {
            format.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun showCustomDatePicker(initialDateStr: String?, onDateSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_calendar, null)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.customCalendarView)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCalCancel)
        val btnOk = dialogView.findViewById<Button>(R.id.btnCalOk)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        if (!initialDateStr.isNullOrBlank()) {
            try {
                val parsed = dateFormat.parse(initialDateStr)
                if (parsed != null) calendar.time = parsed
            } catch (_: Exception) {}
        }

        calendarView.date = calendar.timeInMillis
        var tempSelectedDate = dateFormat.format(calendar.time)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            tempSelectedDate = String.format(Locale.getDefault(), "%02d.%02d.%04d", dayOfMonth, month + 1, year)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener {
            onDateSelected(tempSelectedDate)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_grades, null)

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFilterType)
        val etDate = dialogView.findViewById<EditText>(R.id.etFilterDate)
        val btnReset = dialogView.findViewById<Button>(R.id.btnFilterReset)
        val btnApply = dialogView.findViewById<Button>(R.id.btnFilterApply)

        val filterTypes = arrayOf("Все типы") + workTypes
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filterTypes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(ContextCompat.getColor(context, R.color.pink_accent))
                tv.gravity = Gravity.CENTER
                tv.textSize = 16f
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
                return tv
            }
        }
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinnerType.adapter = adapter

        if (filterWorkType != null) {
            val idx = filterTypes.indexOf(filterWorkType)
            if (idx >= 0) spinnerType.setSelection(idx)
        }

        var selectedDateTemp: String? = filterDate
        etDate.setText(selectedDateTemp ?: "")

        etDate.setOnClickListener {
            showCustomDatePicker(selectedDateTemp) { formatted ->
                selectedDateTemp = formatted
                etDate.setText(formatted)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnReset.setOnClickListener {
            filterDate = null
            filterWorkType = null
            loadGrades()
            dialog.dismiss()
        }

        btnApply.setOnClickListener {
            val chosenType = spinnerType.selectedItem.toString()
            filterWorkType = if (chosenType == "Все типы") null else chosenType
            filterDate = selectedDateTemp?.takeIf { it.isNotBlank() }
            loadGrades()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSortDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sort_grades, null)

        val rowNewest = dialogView.findViewById<LinearLayout>(R.id.rowSortNewest)
        val rowOldest = dialogView.findViewById<LinearLayout>(R.id.rowSortOldest)
        val rowAsc = dialogView.findViewById<LinearLayout>(R.id.rowSortAsc)
        val rowDesc = dialogView.findViewById<LinearLayout>(R.id.rowSortDesc)

        val ivNewest = dialogView.findViewById<ImageView>(R.id.ivCheckNewest)
        val ivOldest = dialogView.findViewById<ImageView>(R.id.ivCheckOldest)
        val ivAsc = dialogView.findViewById<ImageView>(R.id.ivCheckAsc)
        val ivDesc = dialogView.findViewById<ImageView>(R.id.ivCheckDesc)

        val btnSave = dialogView.findViewById<Button>(R.id.btnSortSave)

        var selectedOrder = currentSortOrder

        fun updateCheckboxes() {
            ivNewest.setImageResource(if (selectedOrder == SortOrder.NEWEST) R.drawable.ic_check_pink else 0)
            ivOldest.setImageResource(if (selectedOrder == SortOrder.OLDEST) R.drawable.ic_check_pink else 0)
            ivAsc.setImageResource(if (selectedOrder == SortOrder.ASCENDING) R.drawable.ic_check_pink else 0)
            ivDesc.setImageResource(if (selectedOrder == SortOrder.DESCENDING) R.drawable.ic_check_pink else 0)
        }

        updateCheckboxes()

        rowNewest.setOnClickListener {
            selectedOrder = SortOrder.NEWEST
            updateCheckboxes()
        }
        rowOldest.setOnClickListener {
            selectedOrder = SortOrder.OLDEST
            updateCheckboxes()
        }
        rowAsc.setOnClickListener {
            selectedOrder = SortOrder.ASCENDING
            updateCheckboxes()
        }
        rowDesc.setOnClickListener {
            selectedOrder = SortOrder.DESCENDING
            updateCheckboxes()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            currentSortOrder = selectedOrder
            loadGrades()
            dialog.dismiss()
        }

        dialog.show()
    }

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

    private fun showGradeFormDialog(existingGrade: Grade?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_grade_form, null)
        val etValue = dialogView.findViewById<EditText>(R.id.etGradeValue)
        val etDate = dialogView.findViewById<EditText>(R.id.etGradeDate)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerWorkType)
        val etComment = dialogView.findViewById<EditText>(R.id.etGradeComment)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, workTypes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(ContextCompat.getColor(context, R.color.pink_accent))
                tv.gravity = Gravity.CENTER
                tv.textSize = 16f
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
                return tv
            }
        }
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
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

        etDate.isFocusable = false
        etDate.isClickable = true
        etDate.setOnClickListener {
            showCustomDatePicker(etDate.text.toString()) { formatted ->
                etDate.setText(formatted)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

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
