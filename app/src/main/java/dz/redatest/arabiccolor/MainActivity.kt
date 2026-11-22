package dz.redatest.arabiccolor

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

class MainActivity : Activity() {
    private data class ColorCard(val name: String, val hex: String, val shape: Int, val audio: String) {
        val color: Int get() = AndroidColor.parseColor(hex)
    }
    private val colors = listOf(
        ColorCard("أَحْمَر", "#F04452", 0, "red"),
        ColorCard("أَزْرَق", "#3185F5", 1, "blue"),
        ColorCard("أَصْفَر", "#FFD43B", 2, "yellow"),
        ColorCard("أَخْضَر", "#2DBE78", 0, "green"),
        ColorCard("بُرْتُقَالِيّ", "#FF8A32", 1, "orange"),
        ColorCard("وَرْدِيّ", "#F28BC8", 2, "pink"),
        ColorCard("بَنَفْسَجِيّ", "#9565D9", 0, "purple"),
        ColorCard("أَسْوَد", "#343943", 1, "black"),
        ColorCard("أَبْيَض", "#F4F1E8", 2, "white"),
        ColorCard("بُنِّيّ", "#95613D", 0, "brown")
    )
    private var player: MediaPlayer? = null
    private val audioQueue = ArrayDeque<String>()
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
        buildScreen()
        soundStatus.text = "الأَصْوَاتُ العَرَبِيَّةُ مُدْمَجَةٌ فِي التَّطْبِيقِ"
        soundStatus.setTextColor(AndroidColor.rgb(35, 140, 80))
        playClip("intro")
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(14), dp(18), dp(22))
            setBackgroundColor(AndroidColor.rgb(255, 248, 235))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }
        page.addView(TextView(this).apply {
            text = "هَيَّا نَتَعَلَّمُ الأَلْوَانَ"; textSize = 28f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(53, 55, 72)); typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, dp(4))
        })
        page.addView(TextView(this).apply {
            text = "اِضْغَطْ عَلَى الشَّكْلِ لِتَسْمَعَ اِسْمَ اللَّوْنِ"
            textSize = 16f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(105, 105, 115)); setPadding(0, 0, 0, dp(8))
        })
        soundStatus = TextView(this).apply {
            text = "الأَصْوَاتُ العَرَبِيَّةُ مُدْمَجَةٌ فِي التَّطْبِيقِ"
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(35, 140, 80)); setPadding(0, dp(2), 0, dp(8))
        }
        page.addView(soundStatus)
        page.addView(makeButton("اِسْتَمِعْ إِلَى جَمِيعِ الأَلْوَانِ", "#5B65D8").apply {
            setOnClickListener { playAllColors() }
        }, matchWrap())

        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        colors.forEachIndexed { index, item ->
            grid.addView(makeColorCard(index, item), GridLayout.LayoutParams().apply {
                width = 0; height = dp(154); columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2); setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val gamePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded("#FFFFFF", 22)
        }
        gamePanel.addView(TextView(this).apply {
            text = "لُعْبَةُ الأَلْوَانِ"; textSize = 21f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(57, 61, 90)); typeface = Typeface.DEFAULT_BOLD
        })
        quizPrompt = TextView(this).apply {
            text = "هَلْ أَنْتَ مُسْتَعِدٌّ لِلَّعِبِ؟"; textSize = 19f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(73, 73, 83)); setPadding(0, dp(8), 0, dp(4))
        }
        gamePanel.addView(quizPrompt)
        scoreText = TextView(this).apply {
            text = "النِّقَاطُ: ٠ ⭐"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(115, 102, 75)); setPadding(0, 0, 0, dp(8))
        }
        gamePanel.addView(scoreText)
        gamePanel.addView(makeButton("اِبْدَأِ اللُّعْبَةَ", "#F08A36").apply { setOnClickListener { newQuiz() } }, matchWrap())
        page.addView(gamePanel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        setContentView(scroll)
    }

    private fun makeColorCard(index: Int, item: ColorCard): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(6), dp(8), dp(6), dp(7)); background = rounded("#FFFFFF", 20)
            elevation = dp(3).toFloat(); isClickable = true; isFocusable = true
            contentDescription = "اللَّوْنُ " + item.name
        }
        card.addView(ColorShapeView(this, item.color, item.shape), LinearLayout.LayoutParams(dp(72), dp(72)))
        card.addView(TextView(this).apply {
            text = item.name; textSize = 21f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            setTextColor(AndroidColor.rgb(55, 58, 72)); setPadding(0, dp(2), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = "🔊 اِسْتَمِعْ"; textSize = 13f; gravity = Gravity.CENTER
            setTextColor(AndroidColor.rgb(82, 91, 190)); setPadding(dp(8), dp(3), dp(8), dp(2))
        })
        card.setOnClickListener {
            card.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).withEndAction {
                card.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
            }.start()
            playClip("color_" + item.audio)
            if (quizTarget >= 0) checkAnswer(index)
        }
        return card
    }

    private fun newQuiz() {
        quizTarget = Random.nextInt(colors.size)
        val target = colors[quizTarget]
        quizPrompt.text = "أَيْنَ اللَّوْنُ " + target.name + "؟"
        playClip("question_" + target.audio)
    }

    private fun checkAnswer(index: Int) {
        if (index == quizTarget) {
            score++
            scoreText.text = "النِّقَاطُ: " + score.toString().mapDigits() + " ⭐"
            quizPrompt.text = listOf("أَحْسَنْتَ!", "مُمْتَازٌ!", "إِجَابَةٌ صَحِيحَةٌ!").random()
            playClip("praise")
            quizTarget = -1
        } else {
            quizPrompt.text = "حَاوِلْ مَرَّةً أُخْرَى"
            playClip("try_again")
        }
    }

    private fun playAllColors() {
        audioQueue.clear()
        audioQueue.add("all_colors_intro")
        colors.forEach { audioQueue.add("color_" + it.audio) }
        playNext()
    }

    private fun playClip(name: String) {
        audioQueue.clear()
        audioQueue.add(name)
        playNext()
    }

    private fun playNext() {
        if (audioQueue.isEmpty()) return
        val name = audioQueue.removeFirst()
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                val fd = assets.openFd("audio/" + name + ".mp3")
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    if (player === it) player = null
                    playNext()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    if (player === mp) player = null
                    soundStatus.text = "تَعَذَّرَ تَشْغِيلُ مِلَفِّ الصَّوْتِ"
                    playNext()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            soundStatus.text = "مِلَفَّاتُ الصَّوْتِ غَيْرُ مُتَوَفِّرَةٍ"
            playNext()
        }
    }

    private fun String.mapDigits(): String = map { c ->
        if (c in '0'..'9') ('٠'.code + (c - '0')).toChar() else c
    }.joinToString("")

    private fun makeButton(text: String, color: String) = Button(this).apply {
        this.text = text; textSize = 16f; isAllCaps = false; setTextColor(AndroidColor.WHITE)
        background = rounded(color, 18); stateListAnimator = null; minHeight = dp(52)
    }
    private fun rounded(color: String, radius: Int) = GradientDrawable().apply {
        setColor(AndroidColor.parseColor(color)); cornerRadius = dp(radius).toFloat()
    }
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        audioQueue.clear(); player?.release(); player = null
        super.onDestroy()
    }

    private class ColorShapeView(context: Context, private val fill: Int, private val shape: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = minOf(width, height) * 0.78f
            val cx = width / 2f; val cy = height / 2f
            paint.color = fill
            when (shape) {
                0 -> canvas.drawCircle(cx, cy, size / 2f, paint)
                1 -> canvas.drawRoundRect(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, size * 0.12f, size * 0.12f, paint)
                else -> {
                    val path = Path().apply {
                        moveTo(cx, cy - size / 2); lineTo(cx + size / 2, cy + size / 2)
                        lineTo(cx - size / 2, cy + size / 2); close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
}
