package kr.co.logforme.studytimer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

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

    //버튼 활성화 여부
    private var controlButtons = HashMap<String, Boolean>()

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

    private val settingButton: Button by lazy {
        findViewById<Button>(R.id.settingButton)
    }

    private val setTimeHandler = SetTimeHandler()
    private lateinit var updateTimeThread: UpdateTimeThread
    private lateinit var updateCountUpThread: UpdateCountUpThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            //변수 불러오기
            countType = savedInstanceState.getSerializable("countType") as CountType
            timerStatus = savedInstanceState.getSerializable("timerStatus") as Status

            when (countType) {
                CountType.CLOCK -> {
                    countTypeButton.text = "CLOCK"
                }
                CountType.COUNT_UP -> {
                    countTypeButton.text = "COUNT-UP"
                    startTimeStamp = savedInstanceState.getLong("startTimeStamp")
                    pauseTimeStamp = savedInstanceState.getLong("pauseTimeStamp")
                    //pauseTime = savedInstanceState.getLong("pauseTime")
                }
                CountType.COUNT_DOWN -> {
                    countTypeButton.text = "COUNT-DOWN"
                    countTime = savedInstanceState.getLong("countTime")
                }
            }
        }

        initCountTypeButton()
        initTimeButton()
        initSettingButton()
        initEnabledButton()
        initThread()

        //타이머
        when (countType) {
            CountType.CLOCK -> {
                updateNowTime()
            }
            CountType.COUNT_UP -> {
                if (timerStatus == Status.RUN)
                    updateCountUp()
                else {
                    updateCountUpThread.calPauseTime()  //pause 시간 세팅
                    updateCountUpThread.setPauseTimeStamp() //pauseTimeStamp 갱신

                    val currentTimeStamp = SystemClock.elapsedRealtime()
                    val countTimeSeconds = ((currentTimeStamp - startTimeStamp - pauseTime) / 1000L)

                    val hours = countTimeSeconds / (60 * 60)
                    val minutes = (countTimeSeconds - (hours * 60 * 60)) / 60
                    val seconds = countTimeSeconds % 60

                    val time = "%02d%02d%02d".format(hours, minutes, seconds)

                    timerTextView.text = time.substring(0, 2) + ":" + time.substring(2, 4)
                    secTimerTextView.text = time.substring(4, 6)

                    //setTimer(countTimeSeconds)
                }
            }
            CountType.COUNT_DOWN -> {
                if (timerStatus == Status.RUN)
                    updateCountDown(this)
                else
                    setTimer(countTime)
            }
        }
    }

    private fun initCountTypeButton() {
        controlButtons["HR"] = false;
        controlButtons["MIN"] = false;
        controlButtons["SEC"] = false;
        controlButtons["START"] = false;
        controlButtons["PAUSE"] = false;
        controlButtons["CLEAR"] = false;

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
        hourButton.setOnClickListener {
            countTime += (60 * 60)
            setTimer(countTime)

            controlButtons["START"] = true
            controlButtons["CLEAR"] = true
            updateEnabledButton()
        }

        minuteButton.setOnClickListener {
            countTime += 60
            setTimer(countTime)

            controlButtons["START"] = true
            controlButtons["CLEAR"] = true
            updateEnabledButton()
        }

        secondButton.setOnClickListener {
            countTime += 1
            setTimer(countTime)

            controlButtons["START"] = true
            controlButtons["CLEAR"] = true
            updateEnabledButton()
        }
    }

    private fun initSettingButton() {
        startButton.setOnClickListener {
            if (timerStatus == Status.RUN) return@setOnClickListener

            when (countType) {
                CountType.CLOCK -> {
                    //Disabled
                }
                CountType.COUNT_UP -> {
                    timerStatus = Status.RUN
                    controlButtons["START"] = false
                    controlButtons["PAUSE"] = true
                    controlButtons["CLEAR"] = false
                    updateCountUp()
                }
                CountType.COUNT_DOWN -> {
                    Log.d("StartButton Click", "")
                    timerStatus = Status.RUN
                    controlButtons["HR"] = false
                    controlButtons["MIN"] = false
                    controlButtons["SEC"] = false
                    controlButtons["START"] = false
                    controlButtons["PAUSE"] = true
                    controlButtons["CLEAR"] = false
                    updateCountDown(this)
                }
            }
            updateEnabledButton()
        }

        pauseButton.setOnClickListener {
            if (timerStatus != Status.RUN) return@setOnClickListener

            when (countType) {
                CountType.CLOCK -> {
                    //Disabled
                }
                CountType.COUNT_UP -> {
                    timerStatus = Status.PAUSE
                    controlButtons["START"] = true
                    controlButtons["PAUSE"] = false
                    controlButtons["CLEAR"] = true
                    pauseCountUp()
                }
                CountType.COUNT_DOWN -> {
                    timerStatus = Status.PAUSE
                    controlButtons["HR"] = true
                    controlButtons["MIN"] = true
                    controlButtons["SEC"] = true
                    controlButtons["START"] = true
                    controlButtons["PAUSE"] = false
                    controlButtons["CLEAR"] = true
                    pauseCountDown()
                }
            }
            updateEnabledButton()
        }

        clearButton.setOnClickListener {
            initTimer()
            when (countType) {
                CountType.CLOCK -> {
                    //Disabled
                }
                CountType.COUNT_UP -> {
                    clearCountUp()
                    controlButtons["START"] = true
                    controlButtons["PAUSE"] = false
                    controlButtons["CLEAR"] = false
                }
                CountType.COUNT_DOWN -> {
                    clearCountDown()
                    controlButtons["START"] = false
                    controlButtons["PAUSE"] = false
                    controlButtons["CLEAR"] = false
                }
            }
            updateEnabledButton()
        }
    }

    private fun initEnabledButton() {
        when (countType) {
            CountType.CLOCK -> {
                controlButtons["HR"] = false
                controlButtons["MIN"] = false
                controlButtons["SEC"] = false
                controlButtons["START"] = false
                controlButtons["PAUSE"] = false
                controlButtons["CLEAR"] = false
            }
            CountType.COUNT_UP -> {
                controlButtons["HR"] = false
                controlButtons["MIN"] = false
                controlButtons["SEC"] = false
                controlButtons["START"] = true
                controlButtons["PAUSE"] = false
                controlButtons["CLEAR"] = false
            }
            CountType.COUNT_DOWN -> {
                controlButtons["HR"] = true
                controlButtons["MIN"] = true
                controlButtons["SEC"] = true
                controlButtons["START"] = false
                controlButtons["PAUSE"] = false
                controlButtons["CLEAR"] = false
            }
        }
        updateEnabledButton()
    }

    @SuppressLint("ResourceAsColor")
    private fun updateEnabledButton() {
        hourButton.isEnabled = controlButtons["HR"] == true
        minuteButton.isEnabled = controlButtons["MIN"] == true
        secondButton.isEnabled = controlButtons["SEC"] == true
        startButton.isEnabled = controlButtons["START"] == true
        pauseButton.isEnabled = controlButtons["PAUSE"] == true
        clearButton.isEnabled = controlButtons["CLEAR"] == true


        if (controlButtons["HR"] == true) hourButton.setBackgroundResource(R.drawable.time_setting_button)
        else hourButton.setBackgroundResource(R.drawable.time_setting_disabled_button)

        if (controlButtons["MIN"] == true) minuteButton.setBackgroundResource(R.drawable.time_setting_button)
        else minuteButton.setBackgroundResource(R.drawable.time_setting_disabled_button)

        if (controlButtons["SEC"] == true) secondButton.setBackgroundResource(R.drawable.time_setting_button)
        else secondButton.setBackgroundResource(R.drawable.time_setting_disabled_button)

        if (controlButtons["START"] == true) startButton.setBackgroundResource(R.drawable.time_setting_button)
        else startButton.setBackgroundResource(R.drawable.time_setting_disabled_button)

        if (controlButtons["PAUSE"] == true) pauseButton.setBackgroundResource(R.drawable.time_setting_button)
        else pauseButton.setBackgroundResource(R.drawable.time_setting_disabled_button)

        if (controlButtons["CLEAR"] == true) clearButton.setBackgroundResource(R.drawable.time_setting_clear_button)
        else clearButton.setBackgroundResource(R.drawable.time_setting_disabled_button)



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
    private fun updateCountDown(context: Context) {
        countDownTimer = object : CountDownTimer(countTime * 1000, 1000) {
            override fun onTick(p0: Long) {
                countTime -= 1
                setTimer(countTime)
            }

            override fun onFinish() {
                timerStatus = Status.WAIT
                playVibration(context)

                //TODO 소리 알림 -> 향후에 사용자가 알림 형식 선택할 수 있게
                //playNotification(context)
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

    /**
     * 타이머 알림
     * User Notification
     */
    private fun playNotification(context: Context) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.play()
    }

    private fun playVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as Vibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(500);
        }
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


    /**
     * Android LifeCycle
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //1. clock : x / up : 흘러간 시간 / down : 남은 시간
        //2. 현재 Count-type
        //3. play 여부 (Status)

        when (countType) {
            CountType.CLOCK -> {
                //Disabled
            }
            CountType.COUNT_UP -> {
                outState.putLong("startTimeStamp", startTimeStamp)
                outState.putLong("pauseTimeStamp", pauseTimeStamp)
                outState.putLong("pauseTime", pauseTime)
            }
            CountType.COUNT_DOWN -> {
                outState.putLong("countTime", countTime)
            }
        }
        outState.putSerializable("countType", countType)
        outState.putSerializable("timerStatus", timerStatus)
    }
}