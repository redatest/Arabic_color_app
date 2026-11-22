package dz.redatest.arabiccolor

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private data class ColorCard(val name: String, val hex: String, val shape: Int) {
        val color: Int get() = AndroidColor.parseColor(hex)
    }

    private val colors = listOf(
        ColorCard("أَحْمَر", "#F04452", 0),
        ColorCard("أَزْرَق", "#3185F5", 1),
        ColorCard("أَصْفَر", "#FFD43B", 2),
        ColorCard("أَخْضَر", "#2DBE78", 0),
        ColorCard("بُرْتُقَالِيّ", "#FF8A32", 1),
        ColorCard("وَرْدِيّ", "#F28BC8", 2),
        ColorCard("بَنَفْسَجِيّ", "#9565D9", 0),
        ColorCard("أَسْوَد", "#343943", 1),
        ColorCard("أَبْيَض", "#F4F1E8", 2),
        ColorCard("بُنِّيّ", "#95613D", 0)
    )

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var quizTarget = -1
    private var score = 0
    private lateinit var quizPrompt: TextView
    private lateinit var scoreText: TextView
    private lateinit var soundStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(255, 248, 235)
        window.navigationBarColor = AndroidColor.rgb(255, 248, 235)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        tts = TextToSpeech(this, this)
        tts?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {
                runOnUiThread { showSoundUnavailable() }
            }
        })
        buildScreen()
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            runOnUiThread { showSoundUnavailable() }
            return
        }

        val engine = tts ?: return
        engine.setSpeechRate(0.78f)
        engine.setPitch(1.0f)

        val offlineArabicVoice = engine.voices
            ?.filter { it.locale.language == "ar" && !it.isNetworkConnectionRequired }
            ?.sortedBy {
                when (it.locale.country.uppercase(Locale.ROOT)) {
                    "SA" -> 0
                    "EG" -> 1
                    else -> 2
                }
            }
            ?.firstOrNull()

        val languageResult = if (offlineArabicVoice != null) {
            engine.voice = offlineArabicVoice
            TextToSpeech.LANG_AVAILABLE
        } else {
            listOf(Locale("ar", "SA"), Locale("ar", "EG"), Locale("ar"))
                .asSequence()
                .map { engine.setLanguage(it) }
                .firstOrNull {
                    it == TextToSpeech.LANG_AVAILABLE ||
                    it == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    it == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                } ?: TextToSpeech.LANG_NOT_SUPPORTED
        }

        ttsReady = languageResult >= TextToSpeech.LANG_AVAILABLE
        runOnUiThread {
            if (ttsReady) {
                soundStatus.text = "الصوت العربي جاهز"
                soundStatus.setTextColor(AndroidColor.rgb(35, 140, 80))
                speak("مَرْحَبًا! هَيَّا نَتَعَلَّمُ الأَلْوَانَ")
            } else {
                showSoundUnavailable()
            }
        }
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(14), dp(18), dp(22))
            setBackgroundColor(AndroidColor.rgb(255, 248, 235))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        page.addView(TextView(this).apply {
            text = "هَيَّا نَتَعَلَّمُ الأَلْوَانَ"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(53, 55, 72))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        })
        page.addView(TextView(this).apply {
            text = "اِضْغَطْ عَلَى الشَّكْلِ لِتَسْمَعَ اِسْمَ اللَّوْنِ"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(105, 105, 115))
            setPadding(0, 0, 0, dp(8))
        })

        soundStatus = TextView(this).apply {
            text = "جارٍ تَجْهِيزُ الصَّوْتِ العَرَبِيِّ..."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(105, 105, 115))
            setPadding(0, dp(2), 0, dp(8))
        }
        page.addView(soundStatus)

        page.addView(makeButton("إِعْدَادَاتُ الصَّوْتِ العَرَبِيِّ", "#6570D8").apply {
            setOnClickListener { openSpeechSettings() }
        }, matchWrap())

        page.addView(makeButton("اِسْتَمِعْ إِلَى جَمِيعِ الأَلْوَانِ", "#5B65D8").apply {
            setOnClickListener { speakAll() }
        }, matchWrap().apply { topMargin = dp(8) })

        val grid = GridLayout(this).apply {
            columnCount = 2
            useDefaultMargins = false
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        colors.forEachIndexed { index, item ->
            grid.addView(makeColorCard(index, item), GridLayout.LayoutParams().apply {
                width = 0
                height = dp(154)
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
                setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        val gamePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded("#FFFFFF", 22)
        }
        gamePanel.addView(TextView(this).apply {
            text = "لُعْبَةُ الأَلْوَانِ"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(57, 61, 90))
            typeface = Typeface.DEFAULT_BOLD
        })
        quizPrompt = TextView(this).apply {
            text = "هَلْ أَنْتَ مُسْتَعِدٌّ لِلَّعِبِ؟"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(73, 73, 83))
            setPadding(0, dp(8), 0, dp(4))
        }
        gamePanel.addView(quizPrompt)
        scoreText = TextView(this).apply {
            text = "النِّقَاطُ: ٠ ⭐"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(115, 102, 75))
            setPadding(0, 0, 0, dp(8))
        }
        gamePanel.addView(scoreText)
        gamePanel.addView(makeButton("اِبْدَأِ اللُّعْبَةَ", "#F08A36").apply {
            setOnClickListener { newQuiz() }
        }, matchWrap())
        page.addView(gamePanel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })
        setContentView(scroll)
    }

    private fun makeColorCard(index: Int, item: ColorCard): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(6), dp(8), dp(6), dp(7))
            background = rounded("#FFFFFF", 20)
            elevation = dp(3).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "اللَّوْنُ \${item.name}. اضغط للاستماع."
        }
        card.addView(ColorShapeView(this, item.color, item.shape), LinearLayout.LayoutParams(dp(72), dp(72)))
        card.addView(TextView(this).apply {
            text = item.name
            textSize = 21f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(AndroidColor.rgb(55, 58, 72))
            setPadding(0, dp(2), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = "🔊 اِسْتَمِعْ"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(82, 91, 190))
            setPadding(dp(8), dp(3), dp(8), dp(2))
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
        quizPrompt.text = "أَيْنَ اللَّوْنُ \${target.name}؟"
        speak("أَيْنَ اللَّوْنُ \${target.name}؟")
    }

    private fun checkAnswer(index: Int) {
        if (index == quizTarget) {
            score++
            scoreText.text = "النِّقَاطُ: \${score.toString().mapDigits()} ⭐"
            quizPrompt.text = listOf("أَحْسَنْتَ!", "مُمْتَازٌ!", "إِجَابَةٌ صَحِيحَةٌ!").random()
            speak(quizPrompt.text.toString())
            quizTarget = -1
        } else {
            quizPrompt.text = "حَاوِلْ مَرَّةً أُخْرَى"
            speak("حَاوِلْ مَرَّةً أُخْرَى")
        }
    }

    private fun speakAll() {
        if (!ensureSpeechReady()) return
        tts?.stop()
        colors.forEachIndexed { index, item ->
            tts?.speak(
                item.name,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "color-$index-\${System.nanoTime()}"
            )
        }
    }

    private fun speak(text: String) {
        if (!ensureSpeechReady()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speech-\${System.nanoTime()}")
    }

    private fun ensureSpeechReady(): Boolean {
        if (ttsReady && tts != null) return true
        showSoundUnavailable()
        return false
    }

    private fun showSoundUnavailable() {
        if (!::soundStatus.isInitialized) return
        soundStatus.text = "الصوت العربي غير مُثَبَّت. اضغط على إعدادات الصوت."
        soundStatus.setTextColor(AndroidColor.rgb(190, 70, 55))
    }

    private fun openSpeechSettings() {
        val intents = listOf(
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent(Settings.ACTION_SETTINGS)
        )
        val target = intents.firstOrNull { it.resolveActivity(packageManager) != null }
        if (target != null) {
            try {
                startActivity(target)
            } catch (_: Exception) {
                showSpeechHelp()
            }
        } else {
            showSpeechHelp()
        }
    }

    private fun showSpeechHelp() {
        AlertDialog.Builder(this)
            .setTitle("تَفْعِيلُ الصَّوْتِ العَرَبِيِّ")
            .setMessage("افتح إعدادات الهاتف، ثم النظام، ثم اللغات والإدخال، ثم تحويل النص إلى كلام. اختر محرّكًا يدعم العربية وثبّت بيانات الصوت العربي، ثم عُد إلى التطبيق.")
            .setPositiveButton("حَسَنًا", null)
            .show()
    }

    private fun String.mapDigits(): String = map { char ->
        if (char in '0'..'9') ('٠'.code + (char - '0')).toChar() else char
    }.joinToString("")

    private fun makeButton(text: String, color: String) = Button(this).apply {
        this.text = text
        textSize = 16f
        isAllCaps = false
        setTextColor(AndroidColor.WHITE)
        background = rounded(color, 18)
        stateListAnimator = null
        minHeight = dp(52)
    }

    private fun rounded(color: String, radius: Int) = GradientDrawable().apply {
        setColor(AndroidColor.parseColor(color))
        cornerRadius = dp(radius).toFloat()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private class ColorShapeView(
        context: android.content.Context,
        private val fill: Int,
        private val shape: Int
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = minOf(width, height) * 0.78f
            val cx = width / 2f
            val cy = height / 2f
            paint.color = fill
            when (shape) {
                0 -> canvas.drawCircle(cx, cy, size / 2f, paint)
                1 -> canvas.drawRoundRect(
                    cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2,
                    size * 0.12f, size * 0.12f, paint
                )
                else -> {
                    val path = Path().apply {
                        moveTo(cx, cy - size / 2)
                        lineTo(cx + size / 2, cy + size / 2)
                        lineTo(cx - size / 2, cy + size / 2)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
}
