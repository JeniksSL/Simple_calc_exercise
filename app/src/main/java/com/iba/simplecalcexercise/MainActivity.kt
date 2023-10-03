package com.iba.simplecalcexercise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.SwitchCompat
import org.apache.commons.lang3.StringUtils
import java.io.Serializable

class MainActivity : AppCompatActivity() {

    private lateinit var pairs :Map<ExerciseType, SwitchCompat>
    private lateinit var taskTypeView: TextView
    private lateinit var resultView: TextView
    private lateinit var startButton:Button
    private lateinit var viewPairs  :Map<ExerciseType, TextView>
    private lateinit var skipButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pairs = mapOf(
            Pair(ExerciseType.ADDITION, findViewById<SwitchCompat>(R.id.switch_add)),
            Pair(ExerciseType.SUBTRACTION, findViewById<SwitchCompat>(R.id.switch_sub)),
            Pair(ExerciseType.MULTIPLICATION, findViewById<SwitchCompat>(R.id.switch_multi)),
            Pair(ExerciseType.DIVISION, findViewById<SwitchCompat>(R.id.switch_divide))
        )
        viewPairs= mapOf(
            Pair(ExerciseType.ADDITION, findViewById<SwitchCompat>(R.id.tv_add_indicate)),
            Pair(ExerciseType.SUBTRACTION, findViewById<SwitchCompat>(R.id.tv_sub_indicate)),
            Pair(ExerciseType.MULTIPLICATION, findViewById<SwitchCompat>(R.id.tv_multi_indicate)),
            Pair(ExerciseType.DIVISION, findViewById<SwitchCompat>(R.id.tv_divide_indicate))
        )
        taskTypeView = findViewById(R.id.task_type)
        resultView = findViewById(R.id.tv_result)
        startButton = findViewById(R.id.button)
        skipButton=findViewById(R.id.btn_skip)
        viewPairs.forEach { entry ->  entry.value.text=entry.key.s}
        pairs.forEach { entry ->
            entry.value.text=entry.key.name
        }
    }

    override fun onStart() { //overall suggestion: Replace it with Command pattern to simplify logic https://refactoring.guru/design-patterns/command
        super.onStart()
        val sharedPreferences = applicationContext.getSharedPreferences(GlobalConstants.SETTINGS_IN_PREFERENCE, Context.MODE_PRIVATE)


        applySettings()
        pairs.forEach { (exerciseType, switchCompat) ->SwitchCompatInvoker(
            switchCompat,
            listOf(ShowCommand(TextViewReceiver(viewPairs[exerciseType]!!))),
            listOf(HideCommand(TextViewReceiver(viewPairs[exerciseType]!!))),
            listOf(SaveCommand(CompatPreferenceReceiver(sharedPreferences,switchCompat)))
        ) }

        ButtonInvoker(skipButton, listOf(
            ShowCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_task_type))),
            EnableCommand(ViewReceiver(startButton)),
            HideCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_result))),
            WriteCommand(TextViewReceiver(taskTypeView),getString(R.string.select_tasks_type)),
            ShowCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_compat))
            )))

        val clipboard=Clipboard()
        val activityLauncher = registerForActivityResult(QuizActivityContract(), ActivityResultInvoker(
            mutableListOf(HideCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_task_type))),
                    DisableCommand(ViewReceiver(startButton)),
                    ShowCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_result))),
                    WriteCommand(TextViewReceiver(taskTypeView),getString(R.string.result_label)),
                    HideCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_compat))),
                    PasteCommand(TextViewReceiver(resultView), clipboard))
        ).apply {this.beforeList=listOf(CopyCommand(this, clipboard))  })/* { result ->
            if (result!=null) {
                val questions = result.questions

                WriteCommand(TextViewReceiver(resultView), questions.joinToString(separator = ""){ it.getAnswerDescription()}).execute()
            }
        }*/
        ButtonInvoker(startButton, listOf(LoadCommand(ActivityResultReceiver(activityLauncher, Transaction(pairs.keys.filter { pairs[it]?.isChecked?:false }.toSet(), mutableListOf())))))

    }
    private fun applySettings(){
        val sharedPreferences = applicationContext.getSharedPreferences(GlobalConstants.SETTINGS_IN_PREFERENCE, Context.MODE_PRIVATE)
        pairs.forEach {entry ->  entry.value.isChecked=sharedPreferences.getString(entry.key.name, false.toString()).toBoolean()}
       //TODO its Compat, not Compact, derives from Compatibility, long story)
    }

    private fun startInvoker(commands:List<Command>){
        commands.forEach { it.execute() }
    }


}
data class Clipboard(public var text: String=StringUtils.EMPTY)

interface Command {
    fun execute()
}

class WriteCommand(private var receiver: Receiver, private val text: String):Command{
    override fun execute() {
        receiver.write(text)
    }
}
class ShowCommand(private var receiver: Receiver):Command{
    override fun execute() {
        receiver.show()
    }
}
class HideCommand(private var receiver: Receiver):Command{
    override fun execute() {
        receiver.hide()
    }
}
class SaveCommand(private var receiver: Receiver):Command{
    override fun execute() {
        receiver.save()
    }
}
class EnableCommand(private var receiver: Receiver):Command{
    override fun execute() {
        receiver.enabled(true)
    }
}
class DisableCommand(private var receiver: Receiver):Command{
    override fun execute() {
        receiver.enabled(false)
    }
}
class LoadCommand(private var receiver: Receiver):Command{
    override fun execute() {
        receiver.load()
    }
}
class CopyCommand(private var receiver: Receiver, private val clipboard: Clipboard):Command{
    override fun execute() {
        receiver.copy(clipboard)
    }
}
class PasteCommand(private var receiver: Receiver, private val clipboard: Clipboard):Command{
    override fun execute() {
        receiver.write(clipboard.text)
    }
}


abstract class Receiver{
    open fun hide(){}
    open fun show(){}
    open fun write(text:String){}
    open fun load(){}
    open fun save(){}
    open fun clear(text:String){}
    open fun copy(clipboard: Clipboard){clipboard.text=StringUtils.EMPTY}
    open fun coloring(color: Int){}
    open fun enabled(isEnabled: Boolean){}
    open fun cut(text:String){}
}

class ActivityResultReceiver<T>(private val activityResultLauncher: ActivityResultLauncher<T>, private val t:T):Receiver(){
    override fun load() {
        activityResultLauncher.launch(t)
    }
}
class ActivityResultInvoker(val commands:List<Command>):ActivityResultCallback<Transaction?>, Receiver(){
    var transaction:Transaction?=null
    lateinit var beforeList:List<Command>
    override fun onActivityResult(result: Transaction?) {

        transaction=result
        beforeList.forEach {  it.execute() }
        commands.forEach { it.execute() }
    }
    override fun copy(clipboard: Clipboard) {
        clipboard.text=transaction?.questions?.joinToString(separator = ""){ it.getAnswerDescription()}?:StringUtils.EMPTY
    }


}

class TextViewReceiver(private val textView: TextView):Receiver(){
    override fun hide(){textView.visibility=View.INVISIBLE}
    override fun show(){textView.visibility=View.VISIBLE}
    override fun write(text:String){textView.text=text}
    override fun clear(text:String){textView.text= StringUtils.EMPTY}
    override fun copy(clipboard: Clipboard){clipboard.text= textView.text.toString()}
    override fun coloring(color: Int){textView.setBackgroundColor(color)}
    override fun enabled(isEnabled: Boolean){textView.isEnabled=isEnabled}
    override fun cut(text:String){textView.text=textView.text.removePrefix(text)}
}
class ViewReceiver(private val view: View):Receiver(){
    override fun hide(){view.visibility=View.INVISIBLE}
    override fun show(){view.visibility=View.VISIBLE}
}

class CompatPreferenceReceiver(private val preferences: SharedPreferences,private val compat: SwitchCompat):Receiver(){

    override fun save() {
        preferences.edit().putBoolean(compat.id.toString(), compat.isChecked).apply()
    }
    override fun load() {
        compat.isChecked=preferences.getBoolean(compat.id.toString(),false)
    }
}

class SwitchCompatInvoker(private val switchCompat: SwitchCompat,
    private val commandsOnChecked:List<Command>,
                            private val commandsOnUnChecked:List<Command>,
                            private val commandsOnCheckChanged:List<Command>){
    init {
        switchCompat.setOnCheckedChangeListener{button, _->
            commandsOnCheckChanged.forEach { it.execute()  }
            if (button.isChecked) commandsOnChecked.forEach { it.execute()}
            else commandsOnUnChecked.forEach { it.execute()}
        }
    }
}
class ButtonInvoker(private val button: Button,  private val commandsOnClick:List<Command>){
    init {
        button.setOnClickListener {   commandsOnClick.forEach { it.execute() }}
    }
}

class QuizActivityContract : ActivityResultContract<Transaction, Transaction?>() {
    override fun createIntent(context: Context, input: Transaction): Intent {
        return Intent(context, QuizActivity::class.java)
            .putExtra(GlobalConstants.TRANSACTION_IN_INTENT, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Transaction? = when {
        resultCode != Activity.RESULT_OK -> null
        else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(GlobalConstants.TRANSACTION_IN_INTENT, Transaction::class.java)
        } else {
           intent?.getSerializableExtra(GlobalConstants.TRANSACTION_IN_INTENT)as Transaction?
        }
    }
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
data class Transaction(
    val exerciseTypes: Set<ExerciseType>,
    var questions: MutableList<Exercise>,
    var taskNumber: Int = TASK_COUNT
): Serializable

class SwitchChecker(private val pairs :Map<ExerciseType, SwitchCompat>){
    fun getChecked(): HashSet<ExerciseType> {
        return HashSet(pairs.filter{ entry ->entry.value.isChecked}.keys)
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