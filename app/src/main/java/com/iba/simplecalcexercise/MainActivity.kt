package com.iba.simplecalcexercise

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import java.io.Serializable
import kotlin.streams.toList

class MainActivity : AppCompatActivity() {

    private lateinit var pairs :Map<ExerciseType, SwitchCompat>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pairs= mapOf(
            Pair(ExerciseType.ADDITION, findViewById<SwitchCompat>(R.id.switch_add)),
            Pair(ExerciseType.SUBTRACTION, findViewById<SwitchCompat>(R.id.switch_sub)),
            Pair(ExerciseType.MULTIPLICATION, findViewById<SwitchCompat>(R.id.switch_multi)),
            Pair(ExerciseType.DIVISION, findViewById<SwitchCompat>(R.id.switch_divide))
        )
        val textView: TextView = findViewById(R.id.textView)
        val selectView: TextView = findViewById(R.id.task_type)
        val resultView: TextView = findViewById(R.id.textView4)
        val checker = SwitchChecker(pairs, textView)
        checker.check()
        ExerciseType.values().forEach { exerciseType ->
            val switchChecker = pairs[exerciseType]
            switchChecker!!.setOnCheckedChangeListener{ _, _ -> checker.check() }
            switchChecker.text=exerciseType.name
        }
        val startButton:Button = findViewById(R.id.button)
        val trans: Transaction? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("trans", Transaction::class.java)
        } else {
            intent.getSerializableExtra("trans") as Transaction?
        }
        if (trans!=null) {
            selectView.text = getString(R.string.result_label)
            val results = trans.questions
                .stream()
                .map { ex->ex.question
                    .plus(" "+ex.answers[ex.answerIndex].toString()+" ")
                    .plus((ex.answerIndex==ex.correctIndex).toString())
                    .plus(if (ex.answerIndex==ex.correctIndex) " " else " correct is "+ex.answers[ex.correctIndex].toString())
                    .plus("\n") }
                .toList()
            pairs.values.forEach{switchCompat -> switchCompat.visibility= View.INVISIBLE }
            resultView.text=results.joinToString("")
            startButton.text="Skip"
            textView.visibility= View.INVISIBLE
        }
        startButton.setOnClickListener {
            if ( startButton.text=="Skip") {
                startButton.text="Start"
                resultView.visibility=View.INVISIBLE
                pairs.values.forEach{switchCompat -> switchCompat.visibility= View.VISIBLE }
                textView.visibility= View.VISIBLE
                selectView.text=getString(R.string.select_tasks_type)

            } else {
                val list = checker.check()
                if(list.isNotEmpty()){
                    val newIntent = Intent(applicationContext, MainActivity2::class.java)
                    val transaction = Transaction(list, mutableListOf<Exercise>())
                    newIntent.putExtra("trans", transaction)
                    startActivity(newIntent)
                }
            }

        }
    }
}


enum class ExerciseType(val s: String) {
    ADDITION(" + "), SUBTRACTION(" - ") ,DIVISION(" % "), MULTIPLICATION(" * ");
}
const val TASK_COUNT = 10
data class Transaction (
    val exerciseTypes: List<ExerciseType>,
    var questions: MutableList<Exercise>,
    var taskNumber: Int = TASK_COUNT
): Serializable

class SwitchChecker(private val pairs :Map<ExerciseType, SwitchCompat>,
                    private val textView: TextView){


    fun check(): ArrayList<ExerciseType> {
        val list =ArrayList<ExerciseType>()
        var text = ""
        pairs.forEach{ (type, compact) ->if (compact.isChecked) {text=text.plus(type.s); list.add(type)}}
        if (text == "") {
            textView.textSize = 40F
            textView.text="Please choose the type of exercises"
        } else {
            textView.textSize = 80F
            textView.text = text
        }
        return list
    }
}

data class Exercise(val question:String, val answers: List<Int>, val correctIndex: Int, var answerIndex:Int=-1):Serializable