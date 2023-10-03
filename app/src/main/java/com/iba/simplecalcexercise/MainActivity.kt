package com.iba.simplecalcexercise

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import java.io.Serializable

class MainActivity : AppCompatActivity() {

    private lateinit var pairs :Map<ExerciseType, SwitchCompat>
    private lateinit var selectedTaskView: TextView
    private lateinit var taskTypeView: TextView
    private lateinit var resultView: TextView
    private lateinit var startButton:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pairs = mapOf(
            Pair(ExerciseType.ADDITION, findViewById<SwitchCompat>(R.id.switch_add)),
            Pair(ExerciseType.SUBTRACTION, findViewById<SwitchCompat>(R.id.switch_sub)),
            Pair(ExerciseType.MULTIPLICATION, findViewById<SwitchCompat>(R.id.switch_multi)),
            Pair(ExerciseType.DIVISION, findViewById<SwitchCompat>(R.id.switch_divide))
        )
        selectedTaskView = findViewById(R.id.selectedTaskView)
        taskTypeView = findViewById(R.id.task_type)
        resultView = findViewById(R.id.resultView)
        startButton = findViewById(R.id.button)
    }

    override fun onStart() { //overall suggestion: Replace it with Command pattern to simplify logic https://refactoring.guru/design-patterns/command
        super.onStart()
        applySettings()
        val checker = SwitchChecker(pairs, selectedTaskView, getString(R.string.chose_type))
        checker.setTypes()

        pairs.forEach { entry ->
            entry.value.setOnCheckedChangeListener{ _, _ ->saveSettings(); checker.setTypes() };
            entry.value.text=entry.key.name
        }
        val trans= extractTransaction(intent)
        if (trans!=null&&trans.questions.isNotEmpty()) showResults(trans.questions)
        startButton.setOnClickListener {
            if (startButton.text==getString(R.string.skip)) hideResults()
            else {
                val list = checker.getChecked()
                if(list.isNotEmpty()){//TODO usually startActivity and any navigation action is a separate clearly named function
                  startQuizActivity(list)
                }
            }
        }

    }
    private fun applySettings(){
        val sharedPreferences = applicationContext.getSharedPreferences(GlobalConstants.SETTINGS_IN_PREFERENCE, Context.MODE_PRIVATE)
        pairs.forEach {entry ->  entry.value.isChecked=sharedPreferences.getString(entry.key.name, false.toString()).toBoolean()}
       //TODO its Compat, not Compact, derives from Compatibility, long story)
    }

    private fun saveSettings() {
        val sharedPreferences = applicationContext.getSharedPreferences(GlobalConstants.SETTINGS_IN_PREFERENCE, Context.MODE_PRIVATE)
        pairs.forEach { entry ->  sharedPreferences.edit().putString(entry.key.name, entry.value.isChecked.toString()).apply()}
    }

    private fun showResults(questions: List<Exercise>){
        taskTypeView.text = getString(R.string.result_label)
        resultView.text = questions.joinToString(separator = "") { it.getAnswerDescription() }
           //Java syntax
        pairs.values.forEach{switchCompat -> switchCompat.visibility= View.INVISIBLE }
        startButton.text=getString(R.string.skip)
        selectedTaskView.visibility= View.INVISIBLE
    }
    private fun hideResults(){
        startButton.text=getString(R.string.start)
        resultView.visibility=View.INVISIBLE
        pairs.values.forEach{switchCompat -> switchCompat.visibility= View.VISIBLE }
        selectedTaskView.visibility= View.VISIBLE
        taskTypeView.text=getString(R.string.select_tasks_type)
    }
    private fun startQuizActivity(exerciseTypes: HashSet<ExerciseType>){
        val newIntent = Intent(applicationContext, QuizActivity::class.java)
        newIntent.putExtra(GlobalConstants.TRANSACTION_IN_INTENT, Transaction(exerciseTypes, mutableListOf()))
        startActivity(newIntent)
    }


}

abstract class Command(private val receiver: Receiver) {
    abstract fun execute()
}
abstract class Receiver(private val textView: TextView){

}

class SwitchCompatContainer(){

}


fun extractTransaction(intent: Intent):Transaction? { //TODO use Parcelable here
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra(GlobalConstants.TRANSACTION_IN_INTENT, Transaction::class.java)
    } else {
        intent.getSerializableExtra(GlobalConstants.TRANSACTION_IN_INTENT) as Transaction?
    }
}


enum class ExerciseType(val s: String) {
    ADDITION(" + "), SUBTRACTION(" - ") ,DIVISION(" % "), MULTIPLICATION(" * ");
}
interface GlobalConstants{
    companion object{
        const val SETTINGS_IN_PREFERENCE:String="SETTINGS_IN_PREFERENCE"
        const val TRANSACTION_IN_INTENT:String="TRANSACTION_IN_INTENT"
    }
}

const val TASK_COUNT = 10
data class Transaction (
    val exerciseTypes: HashSet<ExerciseType>,
    var questions: MutableList<Exercise>,
    var taskNumber: Int = TASK_COUNT
): Serializable

class SwitchChecker(private val pairs :Map<ExerciseType, SwitchCompat>,
                    private val textView: TextView,
                    private val emptyText:String){
    fun getChecked(): HashSet<ExerciseType> {
        return HashSet(pairs.filter{ entry ->entry.value.isChecked}.keys)
    }
    fun setTypes(){
        val types = getChecked()
        if (types.isEmpty()) {
            textView.textSize = 40F
            textView.text=emptyText
        } else {
            textView.textSize = 80F//
            textView.text =
                types.joinToString(separator = "") { it.s } //Kotlin version using collections functions
        }
    }


}

data class Exercise(val question:String, val answers: List<Int>, val correctIndex: Int, var answerIndex:Int=-1):Serializable{
    fun getAnswerDescription():String{
       return question
            .plus(" "+answers[answerIndex].toString()+" ")
            .plus((answerIndex==correctIndex).toString())
            .plus(if (answerIndex==correctIndex) " " else " correct is ".plus(answers[correctIndex].toString()))
            .plus("\n")
    }
}