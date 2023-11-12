package com.iba.simplecalcexercise

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random



class QuizActivity : AppCompatActivity() {

    private lateinit var buttons: List<Button>
    private lateinit var progressBar:ProgressBar
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        buttons= listOf<Button>(findViewById(R.id.btn_1_option), findViewById(R.id.btn_2_option), findViewById(R.id.btn_3_option))
       textView=findViewById(R.id.tv_question)
       progressBar=findViewById(R.id.pb_answers)
        progressBar.max= TASK_COUNT
    }


    override fun onStart() {
        super.onStart()
        val transaction: Transaction? = extractTransaction(intent)
        transaction
            ?.let {
                val buttonListener = ButtonListener(
                    buttons,
                    TaskCreator(transaction.exerciseTypes),
                    textView,
                    progressBar,
                    transaction,
                    this::returnToMainActivity)
                buttons.forEach{button -> button.setOnClickListener{buttonListener.handleClick(button.text.toString())} }
                buttonListener.updateExercise()
            }
            ?:{
                returnToMainActivity(Transaction(mutableSetOf(), mutableListOf()),Activity.RESULT_CANCELED)
            }
    }
    private fun returnToMainActivity(transaction: Transaction, result: Int){
            val newIntent=Intent(applicationContext, MainActivity::class.java)
            newIntent.putExtra(GlobalConstants.TRANSACTION_IN_INTENT, transaction)
            setResult(result, newIntent)
            finish()
    }
}

class ButtonListener(
    private val buttons: List<Button>,
    private val creator: TaskCreator,
    private val textView: TextView,
    private val progressBar: ProgressBar,
    private var transaction: Transaction,
    private val kFunction: (Transaction, Int) -> Unit
){
    private var previousEx: Exercise?=null
    @OptIn(DelicateCoroutinesApi::class)
    fun handleClick(text :String) {
        val answer:Int = Integer.parseInt(text)
        if (previousEx!=null) {
            val correctIndex = previousEx!!.correctIndex
            val userIndex = previousEx!!.answers.indexOf(answer)
            val correctText=previousEx!!.answers[correctIndex].toString()
            previousEx!!.answerIndex=userIndex
            transaction.taskNumber--
            transaction.questions.add(previousEx!!)
            if (correctIndex==userIndex) {
                buttons.find { button: Button -> button.text==text }?.setBackgroundColor(Color.GREEN)
            } else {
                buttons.find { button: Button -> button.text==text }?.setBackgroundColor(Color.RED)
                buttons.find { button: Button -> button.text==correctText }?.setBackgroundColor(Color.GREEN)
            }
            buttons.forEach{button: Button -> button.isEnabled = false }
        }
        GlobalScope.launch(Dispatchers.Main) {
            delay(300L)
           updateExercise()
        }
        val progress =  TASK_COUNT-transaction.taskNumber
        progressBar.setProgress(progress, true)
        if (progress>= TASK_COUNT)
        {
            kFunction.invoke(transaction, Activity.RESULT_OK)
        }

    }

    fun updateExercise(){
        val task = creator.getRandomTask()
        previousEx=task
        textView.text=task.question
        task.answers.zip(buttons).forEach { pair-> pair.second.text=pair.first.toString()}
        buttons.forEach{button: Button -> button.isEnabled = true; button.setBackgroundColor(Color.YELLOW) }
    }
}

class TaskCreator(private val types :Set<ExerciseType>) {
    private val maxValue:Int = 100
    private val minValue:Int = 20
    private val maxDelta:Int = 7
    private val minDelta:Int = 1
    private val maxDeltaDivMulti = 2
    private val symbolIs = " = "


    fun getRandomTask(): Exercise {
        return createTask(types.random())
    }

    private fun createTask(type: ExerciseType): Exercise {
        return when (type){
            ExerciseType.ADDITION ->generateAdd()
            ExerciseType.SUBTRACTION ->generateSub()
            ExerciseType.MULTIPLICATION ->generateMulti()
            ExerciseType.DIVISION ->generateDiv()
        }
    }
    private fun generateRandomDelta(maxDelta:Int):Int{
        return if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
        else -Random.nextInt(minDelta, maxDelta)
    }

    private fun generateAdd(): Exercise {
        val res = Random.nextInt(minValue, maxValue)
        val s1 = res - Random.nextInt(minValue-1, res)
        val s2 = res - s1;
        val lie1 = res + generateRandomDelta(maxDelta)
        var lie2 = res + generateRandomDelta(maxDelta)
        while (lie2 == lie1) lie2 = res + generateRandomDelta(maxDelta)
        val list = listOf(res, lie1, lie2).shuffled()
        return Exercise(
            s1.toString().plus(ExerciseType.ADDITION.s).plus(s2.toString()).plus(symbolIs),
            list,
            list.indexOf(res)
        )
    }
    private fun generateSub(): Exercise {
        val sum = Random.nextInt(minValue, maxValue)
        val sub = sum - Random.nextInt(minValue-1, sum)
        val res = sum - sub;
        val lie1 = res +generateRandomDelta(maxDelta)
        var lie2 = res +generateRandomDelta(maxDelta)
        while (lie2 == lie1) lie2 = res +generateRandomDelta(maxDelta)
        val list = listOf(res, lie1, lie2).shuffled()
        return Exercise(
            sum.toString().plus(ExerciseType.SUBTRACTION.s).plus(sub.toString()).plus(symbolIs),
            list,
            list.indexOf(res)
        )
    }
    private fun generateMulti(): Exercise {
        var res = Random.nextInt(minValue, maxValue)
        val m1 = Random.nextInt(1, minValue)
        val m2 = res/m1
        res = m1*m2;
        val lie1 = res +generateRandomDelta(maxDeltaDivMulti)
        var lie2 = res +generateRandomDelta(maxDeltaDivMulti)
        while (lie2 == lie1) lie2 = res +generateRandomDelta(maxDeltaDivMulti)
        val list = listOf(res, lie1, lie2).shuffled()
        return Exercise(
            m1.toString().plus(ExerciseType.MULTIPLICATION.s).plus(m2.toString()).plus(symbolIs),
            list,
            list.indexOf(res)
        )
    }

    private fun generateDiv() : Exercise {
        var div = Random.nextInt(minValue, maxValue)
        val d1 = Random.nextInt(1, minValue)
        val res = div/d1
        div = res*d1;
        val lie1 = res +generateRandomDelta(maxDeltaDivMulti)
        var lie2 = res +generateRandomDelta(maxDeltaDivMulti)
        while (lie2 == lie1) lie2 = res +generateRandomDelta(maxDeltaDivMulti)
        val list = listOf(res, lie1, lie2).shuffled()
        return Exercise(
            div.toString().plus(ExerciseType.DIVISION.s).plus(d1.toString()).plus(symbolIs),
            list,
            list.indexOf(res)
        )
    }


}

