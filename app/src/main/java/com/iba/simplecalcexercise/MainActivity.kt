package com.iba.simplecalcexercise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.SwitchCompat
import kotlinx.parcelize.Parcelize
import java.io.Serializable

class MainActivity : AppCompatActivity() {


    private lateinit var taskTypeView: TextView
    private lateinit var startButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        taskTypeView = findViewById(R.id.task_type)
        startButton = findViewById(R.id.button)
        val transactionReceiver = TransactionReceiver(Transaction(mutableSetOf(), mutableListOf()))
        val sharedPreferences = applicationContext.getSharedPreferences(
            GlobalConstants.SETTINGS_IN_PREFERENCE,
            Context.MODE_PRIVATE
        )
        val activityLauncher = initializeActivityLauncher()




        initializeSwitchCompats(sharedPreferences, transactionReceiver)
        initializeSkipButton()
        initializeStartButton(activityLauncher, transactionReceiver)
        applicationInvoker(
            VisibilityCommand(
                ViewReceiver(findViewById<LinearLayout>(R.id.ll_result)),
                false
            )
        )
    }

    private fun initializeStartButton(
        activityLauncher: ActivityResultLauncher<Transaction>,
        transactionReceiver: TransactionReceiver
    ) {
        val clipboard = Clipboard()
        ButtonInvoker(
            startButton,
            listOf(
                LoadCommand(
                    ActivityResultReceiver(
                        activityLauncher,
                        transactionReceiver.transaction
                    )
                ), CopyCommand(transactionReceiver, clipboard),
                BlinkCommand(TextViewReceiver(taskTypeView), clipboard)
            )
        )
    }

    private fun initializeSkipButton() {
        ButtonInvoker(
            findViewById(R.id.btn_skip),
            listOf(
                VisibilityCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_task_type))),
                EnableCommand(ViewReceiver(startButton)),
                VisibilityCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_result)), false),
                WriteCommand(TextViewReceiver(taskTypeView), getString(R.string.select_tasks_type)),
                VisibilityCommand(
                    ViewReceiver(findViewById<LinearLayout>(R.id.ll_compat))
                )
            )
        )
    }

    private fun initializeActivityLauncher(): ActivityResultLauncher<Transaction> {
        val clipboard = Clipboard()
        return registerForActivityResult(QuizActivityContract(), ActivityResultInvoker(
            listOf(
                VisibilityCommand(
                    ViewReceiver(findViewById<LinearLayout>(R.id.ll_task_type)),
                    false
                ),
                DisableCommand(TextViewReceiver(startButton)),
                VisibilityCommand(ViewReceiver(findViewById<LinearLayout>(R.id.ll_result))),
                WriteCommand(
                    TextViewReceiver(taskTypeView),
                    getString(R.string.result_label)
                ),
                VisibilityCommand(
                    ViewReceiver(findViewById<LinearLayout>(R.id.ll_compat)),
                    false
                ),
                PasteCommand(TextViewReceiver(findViewById(R.id.tv_result)), clipboard)
            )
        ).apply { this.beforeList.add(CopyCommand(this, clipboard)) })
    }

    private fun initializeSwitchCompats(
        sharedPreferences: SharedPreferences,
        transactionReceiver: TransactionReceiver
    ) {
        listOf(
            TypeSwitchView(
                ExerciseType.ADDITION,
                findViewById(R.id.switch_add),
                findViewById<SwitchCompat>(R.id.tv_add_indicate)
            ),
            TypeSwitchView(
                ExerciseType.SUBTRACTION,
                findViewById(R.id.switch_sub),
                findViewById<SwitchCompat>(R.id.tv_sub_indicate)
            ),
            TypeSwitchView(
                ExerciseType.MULTIPLICATION,
                findViewById(R.id.switch_multi),
                findViewById(R.id.tv_multi_indicate)
            ),
            TypeSwitchView(
                ExerciseType.DIVISION,
                findViewById(R.id.switch_divide),
                findViewById(R.id.tv_divide_indicate)
            )
        )
            .forEach {
                applicationInvoker(
                    LoadCommand(
                        CompatPreferenceReceiver(
                            sharedPreferences,
                            it.switchCompat
                        )
                    )
                )
                applicationInvoker(
                    VisibilityCommand(
                        TextViewReceiver(it.textView),
                        it.switchCompat.isChecked
                    )

                )
                if (it.switchCompat.isChecked) applicationInvoker(
                    WriteCommand(
                        transactionReceiver,
                        it.exerciseType.name
                    )
                )
                it.textView.text = it.exerciseType.s
                it.switchCompat.text = it.exerciseType.name

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
                        SaveCommand(
                            CompatPreferenceReceiver(
                                sharedPreferences,
                                it.switchCompat
                            )
                        )
                    )
                )
            }
    }


    private fun applicationInvoker(vararg commands: Command) {
        commands.forEach { it.execute() }
    }


}



data class TypeSwitchView(
    val exerciseType: ExerciseType,
    val switchCompat: SwitchCompat,
    val textView: TextView
)



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