package kr.co.logforme.studytimer

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCountTypeButton()
        settingNowTime()
    }

    @SuppressLint("SetTextI18n")
    private fun settingNowTime() {
        val time = getTime()
        timerTextView.text = time.substring(0, 2) + ":" + time.substring(2, 4)
        secTimerTextView.text = time.substring(4, 6)
    }

    private fun getTime(): String {
        val mNow = System.currentTimeMillis()
        val date = Date(mNow)

        val dateFormat = SimpleDateFormat("HH:mm:ss")

        return dateFormat.format(date)
    }

    private fun initCountTypeButton() {
        countTypeButton.setOnClickListener {
            when (countType) {
                0 -> {
                    countType = 1
                    countTypeButton.text = "COUNT-UP"
                }
                1 -> {
                    countType = 2
                    countTypeButton.text = "COUNT-DOWN"
                }
                2 -> {
                    countType = 0
                    countTypeButton.text = "CLOCK"
                }
            }
        }
    }
}