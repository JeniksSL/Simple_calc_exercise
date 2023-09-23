package com.iba.simplecalcexercise

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import kotlin.random.Random

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val textView:TextView=findViewById(R.id.textView2)
        val progressBar:ProgressBar=findViewById(R.id.progressBar)
        progressBar.max= TASK_COUNT
        val transaction: Transaction? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("trans", Transaction::class.java)
        } else {
            intent.getSerializableExtra("trans") as Transaction?
        }
        if(transaction==null) startActivity(Intent(applicationContext, MainActivity::class.java));
        val taskCreator= TaskCreator(transaction!!.exerciseTypes)
        val buttons= listOf<Button>(findViewById(R.id.button2), findViewById(R.id.button3), findViewById(R.id.button4))
        val buttonListener = ButtonListener(buttons, taskCreator, textView,progressBar, transaction)
        buttons.forEach{button -> button.setOnClickListener{checkTransaction(transaction); buttonListener.handleClick(button.text.toString())} }
        buttonListener.handleClick("-1")
    }

    private fun checkTransaction(transaction: Transaction){
        if (transaction.taskNumber<1) {
            val newIntent=Intent(applicationContext, MainActivity::class.java)
            newIntent.putExtra("trans", transaction)
            startActivity(newIntent)
        }
    }
}

class ButtonListener(
    val buttons:List<Button>,
    val creator: TaskCreator,
    val textView: TextView,
    val progressBar:ProgressBar,
    var transaction: Transaction){
    private var previousEx: Exercise?=null
    fun handleClick(text :String) {
        val answer:Int = Integer.parseInt(text)
        if (previousEx!=null) {
            previousEx!!.answerIndex=previousEx!!.answers.indexOf(answer)
            transaction.taskNumber--
            transaction.questions.add(previousEx!!)
        }
        val progress =  TASK_COUNT-transaction.taskNumber
        if (progress<= TASK_COUNT)
        {
            progressBar.setProgress(progress, true)
            val task = creator.getRandomTask()
            textView.text=task.question
            task.answers.zip(buttons).forEach { pair-> pair.second.text=pair.first.toString()}
            previousEx=task
        }

    }


}

class TaskCreator(private val types :List<ExerciseType>) {
    private val maxValue:Int = 100
    private val minValue:Int = 10
    private val maxDelta:Int = 7
    private val minDelta:Int = 1
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

    private fun generateAdd(): Exercise {
        val res = Random.nextInt(minValue, maxValue)
        val s1 = res - Random.nextInt(minValue, res)
        val s2 = res - s1;
        val lie1 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        var lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        while (lie2 == lie1) lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        val list = listOf(res, lie1, lie2).shuffled()
        return Exercise(
            s1.toString().plus(ExerciseType.ADDITION.s).plus(s2.toString()).plus(symbolIs),
            list,
            list.indexOf(res)
        )
    }
    private fun generateSub(): Exercise {
        val sum = Random.nextInt(minValue, maxValue)
        val sub = sum - Random.nextInt(minValue, sum)
        val res = sum - sub;
        val lie1 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        var lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        while (lie2 == lie1) lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
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
        val lie1 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        var lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        while (lie2 == lie1) lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
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
        val lie1 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        var lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        while (lie2 == lie1) lie2 = res +
                if (Random.nextBoolean()) Random.nextInt(minDelta, maxDelta)
                else -Random.nextInt(minDelta, maxDelta)
        val list = listOf(res, lie1, lie2).shuffled()
        return Exercise(
            div.toString().plus(ExerciseType.DIVISION.s).plus(d1.toString()).plus(symbolIs),
            list,
            list.indexOf(res)
        )
    }


}

