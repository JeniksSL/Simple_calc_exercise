package com.iba.simplecalcexercise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.SwitchCompat
import kotlinx.parcelize.Parcelize
import org.apache.commons.lang3.StringUtils
import java.io.Serializable

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = applicationContext.getSharedPreferences(
            GlobalConstants.SETTINGS_IN_PREFERENCE,
            Context.MODE_PRIVATE
        )

        //Create receivers
        val transactionReceiver = TransactionReceiver(Transaction(mutableSetOf(), mutableListOf()))
        val llTaskTypeReceiver = ViewReceiver(findViewById<LinearLayout>(R.id.ll_task_type))
        val llResultReceiver = ViewReceiver(findViewById<LinearLayout>(R.id.ll_result))
            .apply { this.hide() }
        val btnStartReceiver = TextViewReceiver(findViewById<Button>(R.id.btn_start))
        val tvTaskTypeReceiver = TextViewReceiver(findViewById<TextView>(R.id.tv_task_type))
        val llCompatReceiver = ViewReceiver(findViewById<LinearLayout>(R.id.ll_compat))
        val tvResultReceiver = TextViewReceiver(findViewById(R.id.tv_result))

        val addTypeSwitchViewReceiver = TypeSwitchViewReceiver(
            ExerciseType.ADDITION,
            findViewById(R.id.switch_add),
            findViewById<SwitchCompat>(R.id.tv_add_indicate)
        ).apply { this.load(sharedPreferences) }

        val subTypeSwitchViewReceiver = TypeSwitchViewReceiver(
            ExerciseType.SUBTRACTION,
            findViewById(R.id.switch_sub),
            findViewById<SwitchCompat>(R.id.tv_sub_indicate)
        ).apply { this.load(sharedPreferences) }
        val multiTypeSwitchViewReceiver = TypeSwitchViewReceiver(
            ExerciseType.MULTIPLICATION,
            findViewById(R.id.switch_multi),
            findViewById(R.id.tv_multi_indicate)
        ).apply { this.load(sharedPreferences) }
        val divTypeSwitchViewReceiver = TypeSwitchViewReceiver(
            ExerciseType.DIVISION,
            findViewById(R.id.switch_divide),
            findViewById(R.id.tv_divide_indicate)
        ).apply { this.load(sharedPreferences) }

        //Invokers and commands
        //Activity Launcher Invoker/Receiver
        val clipboard = Clipboard()
        val activityLauncher =
            registerForActivityResult(QuizActivityContract(), ActivityResultInvoker(
                listOf(
                    VisibilityCommand(llTaskTypeReceiver, false),
                    DisableCommand(btnStartReceiver),
                    VisibilityCommand(llResultReceiver),
                    WriteCommand(tvTaskTypeReceiver, getString(R.string.result_label)),
                    VisibilityCommand(llCompatReceiver, false),
                    PasteCommand(tvResultReceiver, clipboard)
                )
            ).apply { this.commandsBefore.add(CopyCommand(this, clipboard)) })

        val activityResultReceiver = ActivityResultReceiver(
            activityLauncher,
            transactionReceiver.transaction
        )

        //SwitchCompat Invoker
        listOf(
            addTypeSwitchViewReceiver,
            subTypeSwitchViewReceiver,
            multiTypeSwitchViewReceiver,
            divTypeSwitchViewReceiver
        )
            .forEach {
                if (it.switchCompat.isChecked) transactionReceiver.apply { this.write( it.exerciseType.name) }
                SwitchCompatInvoker(
                    it.switchCompat,
                    listOf(
                        VisibilityCommand(TextViewReceiver(it.textView)),
                        WriteCommand(transactionReceiver, it.exerciseType.name)
                    ),
                    listOf(
                        VisibilityCommand(TextViewReceiver(it.textView), false),
                        CutCommand(transactionReceiver, it.exerciseType.name)
                    ),
                    listOf(
                        SaveCommand(it, sharedPreferences)
                    )
                )
            }


        //Skip button
        ButtonInvoker(
            findViewById(R.id.btn_skip),
            listOf(),
            listOf(
                EnableCommand(btnStartReceiver),
                VisibilityCommand(llTaskTypeReceiver),
                VisibilityCommand(llResultReceiver, false),
                VisibilityCommand(llCompatReceiver),
                WriteCommand(tvTaskTypeReceiver, getString(R.string.select_tasks_type))
            )
        )
        //Start button
        val transClipboard = Clipboard()
        ButtonInvoker(
            findViewById(R.id.btn_start),
            listOf(
                CopyCommand(transactionReceiver, transClipboard)
            ),
            listOf(
                BlinkCommand(tvTaskTypeReceiver, transClipboard, Color.WHITE, Color.GREEN, 1000),
                LaunchCommand(activityResultReceiver)
            )
        )
    }
}


class QuizActivityContract : ActivityResultContract<Transaction, Transaction?>() {
    override fun createIntent(context: Context, input: Transaction): Intent {
        return Intent(context, QuizActivity::class.java)
            .putExtra(GlobalConstants.TRANSACTION_IN_INTENT, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Transaction? = when {
        resultCode == Activity.RESULT_OK && intent != null
        -> extractTransaction(intent)

        else -> null
    }
}


fun extractTransaction(intent: Intent): Transaction? { //TODO use Parcelable here
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(GlobalConstants.TRANSACTION_IN_INTENT, Transaction::class.java)
    } else {
        intent.getParcelableExtra(GlobalConstants.TRANSACTION_IN_INTENT)
    }
}


enum class ExerciseType(val s: String) {
    ADDITION(" + "), SUBTRACTION(" - "), DIVISION(" % "), MULTIPLICATION(" * ");
}

interface GlobalConstants {
    companion object {
        const val SETTINGS_IN_PREFERENCE: String = "SETTINGS_IN_PREFERENCE"
        const val TRANSACTION_IN_INTENT: String = "TRANSACTION_IN_INTENT"
    }
}

const val TASK_COUNT = 10

@Parcelize
data class Transaction(
    val exerciseTypes: MutableSet<ExerciseType>,
    var questions: MutableList<Exercise>,
    var taskNumber: Int = TASK_COUNT
) : Parcelable


data class Exercise(
    val question: String,
    val answers: List<Int>,
    val correctIndex: Int,
    var answerIndex: Int = -1
) : Serializable {
    fun getAnswerDescription(): String {
        return question
            .plus(" " + answers[answerIndex].toString() + " ")
            .plus((answerIndex == correctIndex).toString())
            .plus(if (answerIndex == correctIndex) " " else " correct is ".plus(answers[correctIndex].toString()))
            .plus("\n")
    }
}