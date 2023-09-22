package com.iba.simplecalcexercise

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import java.io.Serializable

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
        val checker = SwitchChecker(pairs, textView)
        for (comp in pairs.values) comp.setOnCheckedChangeListener{ _, _ -> checker.check() }
        val startButton:Button = findViewById(R.id.button)
        startButton.setOnClickListener {
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


enum class ExerciseType(val s: String) {
    ADDITION(" + "), SUBTRACTION(" - ") ,DIVISION(" % "), MULTIPLICATION(" * ");
}

data class Transaction (
    val exerciseTypes: List<ExerciseType>,
    var questions: MutableList<Exercise>,
    var taskNumber: Int = 10
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