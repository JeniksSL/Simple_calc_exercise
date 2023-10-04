package com.iba.simplecalcexercise

import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.widget.Button
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



data class Clipboard(public var text: String = StringUtils.EMPTY)
interface Command {
    fun execute()
}


interface Receiver {
    fun hide() {}
    fun show() {}
    fun write(text: String) {}
    fun load() {}
    fun save() {}
    fun clear(text: String) {}
    fun copy(clipboard: Clipboard) {
        clipboard.text = StringUtils.EMPTY
    }
    fun coloring(color: Int) {}
    fun enabled(isEnabled: Boolean) {}
    fun cut(text: String) {}
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

class BlinkCommand(private var receiver: Receiver, private val clipboard: Clipboard) : Command {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        if (StringUtils.isEmpty(clipboard.text))
            GlobalScope.launch(Dispatchers.Main) {
                receiver.coloring(Color.GREEN)
                delay(1000L)
                receiver.coloring(Color.WHITE)
            }
    }

}

class VisibilityCommand(private val receiver: Receiver, private val isVisible: Boolean = true) :
    Command {
    override fun execute() {
        if (isVisible) receiver.show() else receiver.hide()
    }
}

class SaveCommand(private var receiver: Receiver) : Command {
    override fun execute() {
        receiver.save()
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

class LoadCommand(private var receiver: Receiver) : Command {
    override fun execute() {
        receiver.load()
    }
}

class CopyCommand(private var receiver: Receiver, private val clipboard: Clipboard) : Command {
    override fun execute() {
        receiver.copy(clipboard)
    }
}

class PasteCommand(private var receiver: Receiver, private val clipboard: Clipboard) : Command {
    override fun execute() {
        receiver.write(clipboard.text)
    }
}



class ActivityResultReceiver(
    private val activityResultLauncher: ActivityResultLauncher<Transaction>,
    private val transaction: Transaction
) : Receiver {
    override fun load() {
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
    val beforeList = mutableListOf<Command>()
    override fun onActivityResult(result: Transaction?) {

        transaction = result
        beforeList.forEach { it.execute() }
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

    override fun clear(text: String) {
        textView.text = StringUtils.EMPTY
    }

    override fun copy(clipboard: Clipboard) {
        clipboard.text = textView.text.toString()
    }

    override fun cut(text: String) {
        textView.text = textView.text.removePrefix(text)
    }
}

class CompatPreferenceReceiver(
    private val preferences: SharedPreferences,
    private val compat: SwitchCompat
) : Receiver {
    override fun save() {
        preferences.edit().putBoolean(compat.id.toString(), compat.isChecked).apply()
    }

    override fun load() {
        compat.isChecked = preferences.getBoolean(compat.id.toString(), false)
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
    private val commandsOnClick: List<Command>,
) {
    init {
        button.setOnClickListener { commandsOnClick.forEach { it.execute() } }
    }
}