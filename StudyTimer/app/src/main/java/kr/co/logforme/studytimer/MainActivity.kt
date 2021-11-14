package kr.co.logforme.studytimer

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Button
import android.widget.TextView
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var countType: Int = 0  //0:CLOCK 1:COUNT-UP 2:COUNT-DOWN
    private val countTypeButton: Button by lazy {
        findViewById<Button>(R.id.countTypeButton)
    }

    private val timerTextView: TextView by lazy {
        findViewById<TextView>(R.id.timerTextView)
    }
    private val secTimerTextView: TextView by lazy {
        findViewById<TextView>(R.id.secTimerTextView)
    }

    private val nowTimeHandler = NowTimeHandler()
    private lateinit var timeThread: UpdateTimeThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCountTypeButton()
        updateNowTime()
    }

    private fun initCountTypeButton() {
        countTypeButton.setOnClickListener {
            when (countType) {
                0 -> {
                    countType = 1
                    countTypeButton.text = "COUNT-UP"
                    stopNowTime()
                }
                1 -> {
                    countType = 2
                    countTypeButton.text = "COUNT-DOWN"
                    stopNowTime()
                }
                2 -> {
                    countType = 0
                    countTypeButton.text = "CLOCK"
                    updateNowTime()
                }
            }
        }
    }

    private fun updateNowTime() {
        timeThread = UpdateTimeThread()
        timeThread.threadStop(false)
        timeThread.start()
    }

    private fun stopNowTime() {
        timeThread.threadStop(true)
    }

    inner class UpdateTimeThread : Thread() {
        var stopFlag = false

        override fun run() {
            while (!stopFlag) {
                val message = nowTimeHandler.obtainMessage()
                val bundle: Bundle = Bundle()

                val time = getNowTime()
                bundle.putString("time", time)

                message.data = bundle
                nowTimeHandler.sendMessage(message)
                sleep(1000)
            }
        }

        fun threadStop(flag: Boolean) {
            this.stopFlag = flag
        }
    }

    inner class NowTimeHandler : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val bundle: Bundle = msg.data
            if (!bundle.isEmpty) {
                //UI Task
                val time: String = bundle.getString("time").toString()
                timerTextView.text = time.substring(0, 2) + ":" + time.substring(2, 4)
                secTimerTextView.text = time.substring(4, 6)
            }
        }
    }

    private fun getNowTime(): String {
        val mNow = System.currentTimeMillis()
        val date = Date(mNow)

        val dateFormat = SimpleDateFormat("HHmmss")
        return dateFormat.format(date)
    }
}