package kr.co.logforme.studytimer

import android.annotation.SuppressLint
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    //0:CLOCK 1:COUNT-UP 2:COUNT-DOWN
    private var countType = CountType.CLOCK

    //현재 타이머 상태
    private var timerStatus = Status.WAIT

    //Count-up TimeStamp 변수
    private var startTimeStamp: Long = 0L
    private var pauseTimeStamp: Long = 0L
    private var pauseTime: Long = 0L


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
    private val pauseButton: Button by lazy {
        findViewById<Button>(R.id.pauseButton)
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
            if (timerStatus == Status.RUN) return@setOnClickListener

            when (countType) {
                CountType.CLOCK -> {
                    //PASS
                }
                CountType.COUNT_UP -> {
                    timerStatus = Status.RUN
                    updateCountUp()
                }
                CountType.COUNT_DOWN -> {
                    //TODO
                }
            }
        }

        //pauseButton
        pauseButton.setOnClickListener {
            if (timerStatus == Status.PAUSE) return@setOnClickListener

            when (countType) {
                CountType.CLOCK -> {
                    //PASS
                }
                CountType.COUNT_UP -> {
                    timerStatus = Status.PAUSE
                    pauseCountUp()
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
                    timerStatus = Status.WAIT
                    stopCountUp()
                }
                CountType.COUNT_DOWN -> {
                    //TODO
                }
            }
        }
    }

    /**
     * Thread Action
     */
    private fun initThread() {
        updateTimeThread = UpdateTimeThread()
        updateCountUpThread = UpdateCountUpThread()

        updateTimeThread.start()
        updateCountUpThread.start()
    }
    private fun stopThread() {
        updateTimeThread.threadStop(true)
        updateCountUpThread.threadStop(true)
    }

    /**
     * Now-time Action
     */
    private fun updateNowTime() {
        updateTimeThread = UpdateTimeThread()
        updateTimeThread.start()
        updateTimeThread.threadStop(false)
    }

    /**
     * Count-up Action
     */
    private fun updateCountUp() {
        updateCountUpThread = UpdateCountUpThread()
        updateCountUpThread.setStartTime()  //첫 카운트업 타임스탬프 저장
        updateCountUpThread.setPauseTime()
        updateCountUpThread.start()
        updateCountUpThread.threadStop(false)
    }

    private fun pauseCountUp() {
        updateCountUpThread.setPauseTimeStamp()
        updateCountUpThread.threadStop(true)
    }

    private fun stopCountUp() {
        initTimer()
        updateCountUpThread.threadStop(true)
    }

    /**
     * Count-up Thread
     */
    inner class UpdateCountUpThread : Thread() {
        private var stopFlag = true

        override fun run() {
            while (!stopFlag) {
                val message = setTimeHandler.obtainMessage()
                val bundle = Bundle()

                val currentTimeStamp = SystemClock.elapsedRealtime()

                System.out.println("" + currentTimeStamp + " / " + startTimeStamp + " / " + pauseTime)

                val countTimeSeconds = ((currentTimeStamp - startTimeStamp - pauseTime) / 1000L).toInt()
                // 멈춘 동안 흘러간 시간을 다시 빼주어야 함

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

        fun setStartTime() {
            if (startTimeStamp == 0L) {
                startTimeStamp = SystemClock.elapsedRealtime()
            }
        }

        fun setPauseTimeStamp() {
            pauseTimeStamp = SystemClock.elapsedRealtime()
        }

        fun setPauseTime() {
            val currentTimeStamp = SystemClock.elapsedRealtime()
            if (pauseTimeStamp != 0L) {
                pauseTime += (currentTimeStamp - pauseTimeStamp)
            }
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
        startTimeStamp = 0L
        pauseTimeStamp = 0L
        pauseTime = 0L

        timerTextView.text = "00:00"
        secTimerTextView.text = "00"
    }
}