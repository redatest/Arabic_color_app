package dz.redatest.arabiccolor

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private data class ColorCard(val name: String, val hex: String, val shape: Int) {
        val color: Int get() = AndroidColor.parseColor(hex)
    }

    private val colors = listOf(
        ColorCard("حْمَر", "#F04452", 0), ColorCard("زْرَق", "#3185F5", 1),
        ColorCard("صْفَر", "#FFD43B", 2), ColorCard("خْضَر", "#2DBE78", 0),
        ColorCard("بُرْتُقالي", "#FF8A32", 1), ColorCard("وَرْدي", "#F28BC8", 2),
        ColorCard("بَنَفْسَجي", "#9565D9", 0), ColorCard("كَحْل", "#343943", 1),
        ColorCard("بِيض", "#F4F1E8", 2), ColorCard("قَهْوي", "#95613D", 0)
    )

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var quizTarget = -1
    private var score = 0
    private lateinit var quizPrompt: TextView
    private lateinit var scoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(255, 248, 235)
        window.navigationBarColor = AndroidColor.rgb(255, 248, 235)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        tts = TextToSpeech(this, this)
        buildScreen()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(0.82f)
            if (!ttsReady) runOnUiThread {
                Toast.makeText(this, "فعّل صوت العربية في إعدادات الهاتف", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(22))
            setBackgroundColor(AndroidColor.rgb(255, 248, 235))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }

        page.addView(TextView(this).apply {
            text = "يَلّا نْتْعَلّمو الأَلْوان!"
            textSize = 27f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(53, 55, 72))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        })
        page.addView(TextView(this).apply {
            text = "إضغط على الشكل واسمع اسم اللون"
            textSize = 16f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(105, 105, 115))
            setPadding(0, 0, 0, dp(14))
        })

        val listenAll = makeButton("🔊  اسْمَعْني الأَلْوان", "#5B65D8")
        listenAll.setOnClickListener { speakAll() }
        page.addView(listenAll, matchWrap())

        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        colors.forEachIndexed { index, item ->
            grid.addView(makeColorCard(index, item), GridLayout.LayoutParams().apply {
                width = 0; height = dp(154)
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
                setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        val gamePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded("#FFFFFF", 22)
        }
        gamePanel.addView(TextView(this).apply {
            text = "🎈  لُعْبَة الأَلْوان"; textSize = 21f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(57, 61, 90)); typeface = Typeface.DEFAULT_BOLD
        })
        quizPrompt = TextView(this).apply {
            text = "واجْد؟ نَلْعْبو؟"; textSize = 19f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(73, 73, 83)); setPadding(0, dp(8), 0, dp(4))
        }
        gamePanel.addView(quizPrompt)
        scoreText = TextView(this).apply {
            text = "النْقاط: 0 ⭐"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(115, 102, 75)); setPadding(0, 0, 0, dp(8))
        }
        gamePanel.addView(scoreText)
        gamePanel.addView(makeButton("يَلّا نْلَعْبو!", "#F08A36").apply {
            setOnClickListener { newQuiz() }
        }, matchWrap())
        page.addView(gamePanel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })
        setContentView(scroll)
    }

    private fun makeColorCard(index: Int, item: ColorCard): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(7))
            background = rounded("#FFFFFF", 20); elevation = dp(3).toFloat()
            isClickable = true; isFocusable = true
        }
        card.addView(ColorShapeView(this, item.color, item.shape), LinearLayout.LayoutParams(dp(72), dp(72)))
        card.addView(TextView(this).apply {
            text = item.name; textSize = 21f; gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(AndroidColor.rgb(55, 58, 72)); setPadding(0, dp(2), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = "🔊 اسمع"; textSize = 13f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(82, 91, 190)); setPadding(dp(8), dp(3), dp(8), dp(2))
        })
        card.setOnClickListener {
            card.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).withEndAction {
                card.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
            }.start()
            speak(item.name)
            if (quizTarget >= 0) checkAnswer(index)
        }
        return card
    }

    private fun newQuiz() {
        quizTarget = Random.nextInt(colors.size)
        val target = colors[quizTarget]
        quizPrompt.text = "وِين راهُ اللّون ${target.name}؟"
        speak("وِين راهُ اللّون ${target.name}؟")
    }

    private fun checkAnswer(index: Int) {
        if (index == quizTarget) {
            score++
            scoreText.text = "النْقاط: $score ⭐"
            quizPrompt.text = listOf("بْرافو عَلِيك!", "شَاطْر!", "وَاعَر!").random()
            speak(listOf("برافو عليك!", "شاطر!", "واعر!").random())
            quizTarget = -1
        } else {
            quizPrompt.text = "حاوِل مَرّة أُخْرى 😊"
            speak("حاول مرة أخرى")
        }
    }

    private fun speakAll() {
        colors.forEachIndexed { i, item ->
            android.os.Handler(mainLooper).postDelayed({ speak(item.name) }, i * 1050L)
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "color-${System.nanoTime()}")
        else Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun makeButton(text: String, color: String) = Button(this).apply {
        this.text = text; textSize = 17f; isAllCaps = false
        setTextColor(AndroidColor.WHITE); background = rounded(color, 18)
        stateListAnimator = null; minHeight = dp(52)
    }

    private fun rounded(color: String, radius: Int) = GradientDrawable().apply {
        setColor(AndroidColor.parseColor(color)); cornerRadius = dp(radius).toFloat()
    }
    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    )
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); super.onDestroy()
    }

    private class ColorShapeView(
        context: android.content.Context, private val fill: Int, private val shape: Int
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = minOf(width, height) * 0.78f
            val cx = width / 2f; val cy = height / 2f
            paint.color = fill
            when (shape) {
                0 -> canvas.drawCircle(cx, cy, size / 2f, paint)
                1 -> canvas.drawRoundRect(cx-size/2, cy-size/2, cx+size/2, cy+size/2,
                    size*0.12f, size*0.12f, paint)
                else -> {
                    val path = Path().apply {
                        moveTo(cx, cy-size/2); lineTo(cx+size/2, cy+size/2)
                        lineTo(cx-size/2, cy+size/2); close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
}
