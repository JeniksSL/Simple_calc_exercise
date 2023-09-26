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
import com.google.gson.Gson
import java.io.Serializable
import kotlin.streams.toList

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
                    val newIntent = Intent(applicationContext, MainActivity2::class.java)
                    newIntent.putExtra(GlobalConstants.TRANSACTION_IN_INTENT, Transaction(list, mutableListOf()))
                    startActivity(newIntent)
                }
            }
        }

    }
    private fun applySettings(){
        val sharedPreferences = applicationContext.getSharedPreferences(GlobalConstants.SETTINGS_IN_PREFERENCE, Context.MODE_PRIVATE)
        val settings:AppSettings= Gson().fromJson(sharedPreferences.getString(GlobalConstants.MAIN_ACTIVITY_SETTINGS, AppSettings.defaultSettingsJson()),AppSettings::class.java )
        settings.checkedSCompact.forEach{ entry-> pairs[entry.key]?.isChecked=entry.value } //TODO its Compat, not Compact, derives from Compatibility, long story)
    }

    private fun saveSettings() {
        val sharedPreferences = applicationContext.getSharedPreferences(GlobalConstants.SETTINGS_IN_PREFERENCE, Context.MODE_PRIVATE)
        val settings = Gson().toJson(AppSettings(pairs.map{ entry->Pair(entry.key, entry.value.isChecked)}.toMap()))
        sharedPreferences.edit().putString(GlobalConstants.MAIN_ACTIVITY_SETTINGS, settings).apply()
    }

    private fun showResults(questions: List<Exercise>){
        taskTypeView.text = getString(R.string.result_label)
        resultView.text = questions
            .stream()
            .map { ex->ex.getAnswerDescription() }
            .toList()
            .joinToString("") //Java syntax
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


}
fun extractTransaction(intent: Intent):Transaction? { //TODO use Parcelable here
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra(GlobalConstants.TRANSACTION_IN_INTENT, Transaction::class.java)
    } else {
        intent.getSerializableExtra(GlobalConstants.TRANSACTION_IN_INTENT) as Transaction?
    }
}


data class AppSettings (val checkedSCompact: Map<ExerciseType, Boolean>
) {
    companion object{
        fun defaultSettingsJson(): String = Gson() //Gson is too heavy to use it here, separated settings preferred
            .toJson(AppSettings(ExerciseType.values().associateWith { false }))
//            .toJson(AppSettings(ExerciseType.values().associateWith { _ -> false })) Java syntax, that is enough
    }


}


enum class ExerciseType(val s: String) {
    ADDITION(" + "), SUBTRACTION(" - ") ,DIVISION(" % "), MULTIPLICATION(" * ");
}
interface GlobalConstants{
    companion object{
        const val SETTINGS_IN_PREFERENCE:String="SETTINGS_IN_PREFERENCE"
        const val MAIN_ACTIVITY_SETTINGS:String="MAIN_ACTIVITY_SETTINGS"
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
            textView.textSize = 80F
//            textView.text = types.stream().map { type->type.s }.toList().joinToString("")
            textView.text = types.map { it.s }.joinToString (separator = " ") //Kotlin version using collections functions
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