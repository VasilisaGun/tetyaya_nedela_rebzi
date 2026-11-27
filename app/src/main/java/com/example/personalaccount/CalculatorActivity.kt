package com.example.personalaccount

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.personalaccount.data.db.AppDatabase
import com.example.personalaccount.data.db.Discipline
import com.example.personalaccount.data.db.Grade
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil

class CalculatorActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var spinnerDisciplines: Spinner
    private lateinit var tvCurrentAvg: TextView
    private lateinit var tvGradesCount: TextView
    private lateinit var etTargetAvg: EditText
    private lateinit var tvResult: TextView

    private var disciplinesList: List<Discipline> = emptyList()
    private var currentGrades: List<Grade> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calculator)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)
        spinnerDisciplines = findViewById(R.id.spinnerDisciplines)
        tvCurrentAvg = findViewById(R.id.tvCurrentAvg)
        tvGradesCount = findViewById(R.id.tvGradesCount)
        etTargetAvg = findViewById(R.id.etTargetAvg)
        tvResult = findViewById(R.id.tvCalcResult)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCalculate).setOnClickListener { calculateNeededGrades() }

        loadDisciplines()
    }

    private fun loadDisciplines() {
        lifecycleScope.launch {
            disciplinesList = db.appDao().getAllDisciplines()
            if (disciplinesList.isEmpty()) {
                Toast.makeText(this@CalculatorActivity, "Сначала добавьте хотя бы одну дисциплину", Toast.LENGTH_LONG).show()
                findViewById<Button>(R.id.btnCalculate).isEnabled = false
                return@launch
            }

            val names = disciplinesList.map { it.name }
            val adapter = object : ArrayAdapter<String>(this@CalculatorActivity, android.R.layout.simple_spinner_item, names) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tv = super.getView(position, convertView, parent) as TextView
                    tv.setTextColor(ContextCompat.getColor(context, R.color.dark_bg))
                    tv.gravity = Gravity.CENTER
                    tv.textSize = 16f
                    tv.setTypeface(null, android.graphics.Typeface.BOLD)
                    return tv
                }
            }
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
            spinnerDisciplines.adapter = adapter


            spinnerDisciplines.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    tvResult.visibility = View.GONE
                    loadGradesForSelected(disciplinesList[position].id)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadGradesForSelected(disciplineId: Int) {
        lifecycleScope.launch {
            currentGrades = db.appDao().getGradesForDiscipline(disciplineId)
            val count = currentGrades.size
            tvGradesCount.text = "Всего оценок: $count"

            if (count == 0) {
                tvCurrentAvg.text = "Текущий средний балл: нет оценок"
            } else {
                val avg = currentGrades.map { it.value }.average()
                tvCurrentAvg.text = "Текущий средний балл: ${String.format(Locale.US, "%.2f", avg)}"
            }
        }
    }

    private fun calculateNeededGrades() {
        val targetStr = etTargetAvg.text.toString().trim().replace(',', '.')
        val target = targetStr.toDoubleOrNull()

        if (target == null || target <= 2.0 || target > 5.0) {
            Toast.makeText(this, "Введите желаемый балл от 2.1 до 5.0", Toast.LENGTH_SHORT).show()
            return
        }

        val count = currentGrades.size
        val sum = currentGrades.sumOf { it.value }
        val currentAvg = if (count == 0) 0.0 else sum.toDouble() / count

        tvResult.visibility = View.VISIBLE

        if (count > 0 && currentAvg >= target) {
            tvResult.text = "Ваш текущий балл (${String.format(Locale.US, "%.2f", currentAvg)}) уже не ниже желаемого!"
            return
        }

        if (target == 5.0 && currentGrades.any { it.value < 5 }) {
            tvResult.text = "Достичь ровно 5.00 невозможно, так как уже есть оценки ниже пятёрки."
            return
        }

        // Формула: (sum + k * grade) / (count + k) >= target
        // Отсюда: k >= (target * count - sum) / (grade - target)
        val neededFives = ceil((target * count - sum) / (5.0 - target)).toInt().coerceAtLeast(1)

        val sb = StringBuilder()
        sb.append("Чтобы достичь балла ${String.format(Locale.US, "%.2f", target)}, нужно получить:\n\n")
        sb.append("• Пятёрок (5): $neededFives шт.")

        if (target < 4.0) {
            val neededFours = ceil((target * count - sum) / (4.0 - target)).toInt().coerceAtLeast(1)
            sb.append("\n• Или четвёрок (4): $neededFours шт.")
        }

        tvResult.text = sb.toString()
    }
}
