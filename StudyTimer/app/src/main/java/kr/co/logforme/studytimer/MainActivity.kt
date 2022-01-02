package kr.co.logforme.studytimer

import android.annotation.SuppressLint
import android.os.*
import android.util.Log
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
    private var startTimeStamp: Long = 0L   //시작 시점
    private var pauseTimeStamp: Long = 0L   //일시정지 시점
    private var pauseTime: Long = 0L    //일시정지된 후 흘러간 시간

    //Count-down 변수
    private lateinit var countDownTimer: CountDownTimer //count-down 타이머
    private var countTime: Long = 0L


    private val countTypeButton: Button by lazy {
        findViewById<Button>(R.id.countTypeButton)
    }

    private val timerTextView: TextView by lazy {
        findViewById<TextView>(R.id.timerTextView)
    }
    private val secTimerTextView: TextView by lazy {
        findViewById<TextView>(R.id.secTimerTextView)
    }

    private val hourButton: Button by lazy {
        findViewById<Button>(R.id.hourButton)
    }
    private val minuteButton: Button by lazy {
        findViewById<Button>(R.id.minuteButton)
    }
    private val secondButton: Button by lazy {
        findViewById<Button>(R.id.secondButton)
    }


    private val startButton: Button by lazy {
        findViewById<Button>(R.id.startButton)
    }
    private val pauseButton: Button by lazy {
        findViewById<Button>(R.id.pauseButton)
    }
    private val clearButton: Button by lazy {
        findViewById<Button>(R.id.clearButton)
    }

    private val setTimeHandler = SetTimeHandler()
    private lateinit var updateTimeThread: UpdateTimeThread
    private lateinit var updateCountUpThread: UpdateCountUpThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCountTypeButton()
        initTimeButton()
        initSettingButton()
        initEnabledButton()
        initThread()

        //시계 구동
        updateNowTime()
    }

    private fun initCountTypeButton() {
        countTypeButton.setOnClickListener {
            stopThread()
            pauseCountDown()

            when (countType) {
                CountType.CLOCK -> {
                    initTimer()
                    countType = CountType.COUNT_UP
                    countTypeButton.text = "COUNT-UP"
                }
                CountType.COUNT_UP -> {
                    initTimer()
                    countType = CountType.COUNT_DOWN
                    countTypeButton.text = "COUNT-DOWN"
                }
                CountType.COUNT_DOWN -> {
                    countType = CountType.CLOCK
                    countTypeButton.text = "CLOCK"

                    //현재 시각 세팅
                    updateNowTime()
                }
            }
            initEnabledButton()
        }
    }

    /**
     * Timer Button Setting
     */
    private fun initTimeButton() {
        //Hour Button
        hourButton.setOnClickListener {
            countTime += (60 * 60)
            setTimer(countTime)
        }

        //Minute Button
        minuteButton.setOnClickListener {
            countTime += 60
            setTimer(countTime)
        }

        //Second Button
        secondButton.setOnClickListener {
            countTime += 1
            setTimer(countTime)
        }
    }

    private fun initSettingButton() {
        //Start Button
        startButton.setOnClickListener {
            if (timerStatus == Status.RUN) return@setOnClickListener

            when (countType) {
                CountType.CLOCK -> {
                    //Disabled
                }
                CountType.COUNT_UP -> {
                    timerStatus = Status.RUN
                    updateCountUp()
                }
                CountType.COUNT_DOWN -> {
                    //Count-down 시작
                    timerStatus = Status.RUN
                    updateCountDown()
                }
            }
        }

        //pauseButton
        pauseButton.setOnClickListener {
            if (timerStatus != Status.RUN) return@setOnClickListener

            when (countType) {
                CountType.CLOCK -> {
                    //Disabled
                }
                CountType.COUNT_UP -> {
                    timerStatus = Status.PAUSE
                    pauseCountUp()
                }
                CountType.COUNT_DOWN -> {
                    timerStatus = Status.PAUSE
                    pauseCountDown()
                }
            }
        }

        //clearButton
        clearButton.setOnClickListener {
            initTimer()
            when (countType) {
                CountType.CLOCK -> {
                    //Disabled
                }
                CountType.COUNT_UP -> {
                    clearCountUp()
                }
                CountType.COUNT_DOWN -> {
                    clearCountDown()
                }
            }
        }
    }

    private fun initEnabledButton() {
        when (countType) {
            CountType.CLOCK -> {
                hourButton.isEnabled = false
                minuteButton.isEnabled = false
                secondButton.isEnabled = false
                startButton.isEnabled = false
                pauseButton.isEnabled = false
                clearButton.isEnabled = false
            }
            CountType.COUNT_UP -> {
                hourButton.isEnabled = false
                minuteButton.isEnabled = false
                secondButton.isEnabled = false
                startButton.isEnabled = true
                pauseButton.isEnabled = true
                clearButton.isEnabled = true
            }
            CountType.COUNT_DOWN -> {
                hourButton.isEnabled = true
                minuteButton.isEnabled = true
                secondButton.isEnabled = true
                startButton.isEnabled = true
                pauseButton.isEnabled = true
                clearButton.isEnabled = true
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
        updateCountUpThread.calPauseTime()
        updateCountUpThread.start()
        updateCountUpThread.threadStop(false)
    }

    private fun pauseCountUp() {
        updateCountUpThread.setPauseTimeStamp()
        updateCountUpThread.threadStop(true)
    }

    private fun clearCountUp() {
        updateCountUpThread.threadStop(true)
    }

    /**
     * Count-down Action
     */
    private fun updateCountDown() {
        countDownTimer = object : CountDownTimer(countTime * 1000, 1000) {
            override fun onTick(p0: Long) {
                countTime -= 1
                setTimer(countTime)
            }

            override fun onFinish() {
                Log.d("MainActivity", "Count-down Finish")

                //TODO 알림 띄우기
            }
        }.start()
    }

    private fun pauseCountDown() {
        if (this::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }

    private fun clearCountDown() {
        countTime = 0L
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

                val hours = countTimeSeconds / (60 * 60)
                val minutes = (countTimeSeconds - (hours * 60 * 60)) / 60
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

        fun calPauseTime() {
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
     * - UI를 변경하기 위해 핸들러 사용
     */
    inner class SetTimeHandler : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val bundle: Bundle = msg.data
            if (!bundle.isEmpty) {
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

    /**
     * 타이머 시간 세팅
     */
    private fun setTimer(countTimeSeconds: Long) {
        val hours = countTimeSeconds / (60 * 60)
        val minutes = (countTimeSeconds - (hours * 60 * 60)) / 60
        val seconds = countTimeSeconds % 60

        val time = "%02d%02d%02d".format(hours, minutes, seconds)

        timerTextView.text = time.substring(0, 2) + ":" + time.substring(2, 4)
        secTimerTextView.text = time.substring(4, 6)
    }

    private fun initTimer() {
        timerStatus = Status.WAIT

        startTimeStamp = 0L
        pauseTimeStamp = 0L
        pauseTime = 0L
        countTime = 0L

        timerTextView.text = "00:00"
        secTimerTextView.text = "00"
    }
}