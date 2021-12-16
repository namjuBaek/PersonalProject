package kr.co.logforme.studytimer

import android.annotation.SuppressLint
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    //0:CLOCK 1:COUNT-UP 2:COUNT-DOWN
    private var countType = CountType.CLOCK

    private val countTypeButton: Button by lazy {
        findViewById<Button>(R.id.countTypeButton)
    }

    private val timerTextView: TextView by lazy {
        findViewById<TextView>(R.id.timerTextView)
    }
    private val secTimerTextView: TextView by lazy {
        findViewById<TextView>(R.id.secTimerTextView)
    }

    private val startButton: Button by lazy {
        findViewById<Button>(R.id.startButton)
    }
    private val stopButton: Button by lazy {
        findViewById<Button>(R.id.stopButton)
    }

    private val setTimeHandler = SetTimeHandler()
    private lateinit var updateTimeThread: UpdateTimeThread
    private lateinit var updateCountUpThread: UpdateCountUpThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCountTypeButton()
        initSettingButton()
        initThread()

        //시계 구동
        updateNowTime()
    }

    private fun initCountTypeButton() {
        countTypeButton.setOnClickListener {
            stopThread()
            initTimer()

            when (countType) {
                CountType.CLOCK -> {
                    countType = CountType.COUNT_UP
                    countTypeButton.text = "COUNT-UP"
                }
                CountType.COUNT_UP -> {
                    countType = CountType.COUNT_DOWN
                    countTypeButton.text = "COUNT-DOWN"
                }
                CountType.COUNT_DOWN -> {
                    countType = CountType.CLOCK
                    countTypeButton.text = "CLOCK"

                    updateNowTime()
                }
            }
        }
    }

    private fun initSettingButton() {
        //Start Button
        startButton.setOnClickListener {
            when (countType) {
                CountType.CLOCK -> {
                    //PASS
                }
                CountType.COUNT_UP -> {
                    updateCountUp()
                }
                CountType.COUNT_DOWN -> {
                    //TODO
                }
            }
        }

        //StopButton
        stopButton.setOnClickListener {
            when (countType) {
                CountType.CLOCK -> {
                    //PASS
                }
                CountType.COUNT_UP -> {
                    stopCountUp()
                }
                CountType.COUNT_DOWN -> {
                    //TODO
                }
            }
        }
    }

    private fun initThread() {
        updateTimeThread = UpdateTimeThread()
        updateCountUpThread = UpdateCountUpThread()

        updateTimeThread.start()
        updateCountUpThread.start()
    }

    private fun updateNowTime() {
        updateTimeThread = UpdateTimeThread()
        updateTimeThread.start()
        updateTimeThread.threadStop(false)
    }

    private fun updateCountUp() {
        updateCountUpThread = UpdateCountUpThread()
        updateCountUpThread.start()
        updateCountUpThread.threadStop(false)
    }

    private fun stopThread() {
        updateTimeThread.threadStop(true)
        updateCountUpThread.threadStop(true)
    }

    private fun stopCountUp() {
        initTimer()
        updateCountUpThread.threadStop(true)
    }

    /**
     * Count-up
     */
    inner class UpdateCountUpThread : Thread() {
        private var stopFlag = true
        private var startTimeStamp: Long = SystemClock.elapsedRealtime()

        override fun run() {
            while (!stopFlag) {
                val message = setTimeHandler.obtainMessage()
                val bundle = Bundle()

                val currentTimeStamp = SystemClock.elapsedRealtime()
                val countTimeSeconds = ((currentTimeStamp - startTimeStamp) / 1000L).toInt()

                val hours = countTimeSeconds / (60 * 60)
                val minutes = countTimeSeconds / 60
                val seconds = countTimeSeconds % 60

                val time = "%02d%02d%02d".format(hours, minutes, seconds)
                bundle.putString("time", time)

                message.data = bundle
                setTimeHandler.sendMessage(message)
                sleep(1000)
            }
        }

        fun initTimeStamp() {
            startTimeStamp = SystemClock.elapsedRealtime()
        }

        fun threadStop(flag: Boolean) {
            this.stopFlag = flag
        }
    }

    /**
     * Now Clock
     */
    inner class UpdateTimeThread : Thread() {
        private var stopFlag = true

        override fun run() {
            while (!stopFlag) {
                val message = setTimeHandler.obtainMessage()
                val bundle: Bundle = Bundle()

                val time = getNowTime()
                bundle.putString("time", time)

                message.data = bundle
                setTimeHandler.sendMessage(message)
                sleep(1000)
            }
        }

        fun threadStop(flag: Boolean) {
            this.stopFlag = flag
        }
    }

    /**
     * 시간 세팅하는 UI 핸들러
     */
    inner class SetTimeHandler : Handler(Looper.getMainLooper()) {
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

    private fun initTimer() {
        timerTextView.text = "00:00"
        secTimerTextView.text = "00"
    }
}