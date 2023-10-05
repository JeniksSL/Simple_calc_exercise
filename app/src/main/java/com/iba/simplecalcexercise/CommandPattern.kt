package com.iba.simplecalcexercise

import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.SwitchCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils



data class Clipboard(var text: String=StringUtils.EMPTY)
interface Command {
    fun execute()
}


interface Receiver {
    fun hide() {}
    fun show() {}
    fun write(text: String) {}
    fun load(sharedPreferences: SharedPreferences) {}
    fun save(sharedPreferences: SharedPreferences) {}
    fun copyFromIndex(clipboard: Clipboard, index:Int) {}
    fun copy(clipboard: Clipboard) {
    }
    fun paste(clipboard: Clipboard) {
    }
    fun coloring(color: Int) {}
    fun enabled(isEnabled: Boolean) {}
    fun cut(text: String) {}

    fun launch(){ }
}



class WriteCommand(private var receiver: Receiver, private val text: String) : Command {
    override fun execute() {
        receiver.write(text)
    }
}

class CutCommand(private var receiver: Receiver, private val text: String) : Command {
    override fun execute() {
        receiver.cut(text)
    }
}

class BlinkCommand(
    private var receiver: Receiver,
    private val clipboard: Clipboard,
    private val fromColor: Int,
    private val toColor: Int,
    private val delay:Long=250L) : Command {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        if (StringUtils.isEmpty(clipboard.text))
            GlobalScope.launch(Dispatchers.Main) {
                receiver.coloring(toColor)
                delay(delay)
                receiver.coloring(fromColor)
            }
    }

}

class VisibilityCommand(private val receiver: Receiver, private val isVisible: Boolean = true) :
    Command {
    override fun execute() {
        if (isVisible) receiver.show() else receiver.hide()
    }
}

class SaveCommand(private var receiver: Receiver, private val sharedPreferences: SharedPreferences) : Command {
    override fun execute() {
        receiver.save(sharedPreferences)
    }
}

class EnableCommand(private var receiver: Receiver) : Command {
    override fun execute() {
        receiver.enabled(true)
    }
}

class DisableCommand(private var receiver: Receiver) : Command {
    override fun execute() {
        receiver.enabled(false)
    }
}

class LoadCommand(private var receiver: Receiver, private val sharedPreferences: SharedPreferences) : Command {
    override fun execute() {
        receiver.load(sharedPreferences)
    }
}

class CopyCommand(private var receiver:Receiver, private val clipboard: Clipboard) : Command {
    override fun execute() {
        receiver.copy(clipboard)
    }
}

class PasteCommand(private var receiver: Receiver, private val clipboard: Clipboard) : Command {
    override fun execute() {
        receiver.paste(clipboard)
    }
}

class LaunchCommand(private var receiver: Receiver) :Command {    override fun execute() {
        receiver.launch()
    }
}

class TypeSwitchViewReceiver(
    val exerciseType: ExerciseType,
    val switchCompat: SwitchCompat,
    val textView: TextView
): Receiver{
    init{
        textView.text = exerciseType.s
        switchCompat.text = exerciseType.name
    }
    override fun save(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().putBoolean(switchCompat.id.toString(), switchCompat.isChecked).apply()
    }

    override fun load(sharedPreferences: SharedPreferences) {
        switchCompat.isChecked = sharedPreferences.getBoolean(switchCompat.id.toString(), false)
        if (switchCompat.isChecked) textView.visibility = View.VISIBLE
        else textView.visibility = View.INVISIBLE
    }
}



class ActivityResultReceiver(
    private val activityResultLauncher: ActivityResultLauncher<Transaction>,
    private val transaction: Transaction
) : Receiver {
    override fun launch() {
        if (transaction.exerciseTypes.isNotEmpty()) activityResultLauncher.launch(transaction)
    }
}

class TransactionReceiver(val transaction: Transaction) : Receiver {
    override fun write(text: String) {
        transaction.exerciseTypes.add(ExerciseType.valueOf(text))
    }

    override fun cut(text: String) {
        transaction.exerciseTypes.remove(ExerciseType.valueOf(text))
    }

    override fun copy(clipboard: Clipboard) {
        clipboard.text = transaction.exerciseTypes.joinToString { it.name }
    }
}

class ActivityResultInvoker(private val commands: List<Command>) :
    ActivityResultCallback<Transaction?>,
    Receiver {
    private var transaction: Transaction? = null
    val commandsBefore = mutableListOf<Command>()
    override fun onActivityResult(result: Transaction?) {
        transaction = result
        commandsBefore.forEach { it.execute() }
        commands.forEach { it.execute() }
    }
    override fun copy(clipboard: Clipboard) {
        clipboard.text =
            transaction?.questions?.joinToString(separator = "") { it.getAnswerDescription() }
                ?: StringUtils.EMPTY
    }
}


open class ViewReceiver(private val view: View) : Receiver {
    override fun hide() {
        view.visibility = View.INVISIBLE
    }

    override fun show() {
        view.visibility = View.VISIBLE
    }

    override fun enabled(isEnabled: Boolean) {
        view.isEnabled = isEnabled
    }

    override fun coloring(color: Int) {
        view.setBackgroundColor(color)
    }
}

class TextViewReceiver(private val textView: TextView) : ViewReceiver(textView) {
    override fun write(text: String) {
        textView.text = text
    }

    override fun copy(clipboard: Clipboard) {
        clipboard.text = textView.text.toString()
    }

    override fun cut(text: String) {
        textView.text = textView.text.removePrefix(text)
    }

    override fun paste(clipboard: Clipboard) {
        textView.text=clipboard.text
    }
}

class SwitchCompatInvoker(
    private val switchCompat: SwitchCompat,
    private val commandsOnChecked: List<Command>,
    private val commandsOnUnChecked: List<Command>,
    private val commandsOnCheckChanged: List<Command>
) {
    init {
        switchCompat.setOnCheckedChangeListener { button, _ ->
            commandsOnCheckChanged.forEach { it.execute() }
            if (button.isChecked) commandsOnChecked.forEach { it.execute() }
            else commandsOnUnChecked.forEach { it.execute() }
        }
    }
}

class ButtonInvoker(
    private val button: Button,
    private val commandsOnClickBefore: List<Command>,
    private val commandsOnClick: List<Command>

) {
    init {

        button.setOnClickListener {
            commandsOnClickBefore.forEach { it.execute() }
            commandsOnClick.forEach { it.execute() } }
    }
}