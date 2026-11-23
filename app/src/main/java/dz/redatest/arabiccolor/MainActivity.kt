package dz.redatest.arabiccolor

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.view.MotionEvent
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

class MainActivity : Activity() {
    private data class ColorCard(val name: String, val hex: String, val audio: String) {
        val color: Int get() = AColor.parseColor(hex)
    }
    private data class Toy(val emoji: String, val colorIndex: Int)
    private data class MenuItem(val emoji: String, val label: String, val color: String, val action: () -> Unit)
    private val colors = listOf(
        ColorCard("أَحْمَر", "#F04452", "red"),
        ColorCard("أَزْرَق", "#3185F5", "blue"),
        ColorCard("أَصْفَر", "#FFD43B", "yellow"),
        ColorCard("أَخْضَر", "#2DBE78", "green"),
        ColorCard("بُرْتُقَالِيّ", "#FF8A32", "orange"),
        ColorCard("وَرْدِيّ", "#F28BC8", "pink"),
        ColorCard("بَنَفْسَجِيّ", "#9565D9", "purple"),
        ColorCard("أَسْوَد", "#343943", "black"),
        ColorCard("أَبْيَض", "#F4F1E8", "white"),
        ColorCard("بُنِّيّ", "#95613D", "brown")
    )
    private val toys = listOf(
        Toy("🍎", 0), Toy("🚙", 1), Toy("☀️", 2), Toy("🐸", 3), Toy("🦊", 4),
        Toy("🌸", 5), Toy("🍇", 6), Toy("🐈‍⬛", 7), Toy("☁️", 8), Toy("🐻", 9)
    )
    private val stickers = listOf("🦁", "🐼", "🦄", "🐸", "🐻", "🐯", "🐧", "🐵", "🐘", "🦋", "🐨", "🦊")
    private var player: MediaPlayer? = null
    private val audioQueue = ArrayDeque<String>()
    private val prefs by lazy { getSharedPreferences("little_colors_progress", MODE_PRIVATE) }
    private var stars = 0
    private val collected = linkedSetOf<String>()
    private lateinit var root: LinearLayout
    private lateinit var page: LinearLayout
    private lateinit var overlay: FrameLayout
    private var roundTarget = 0
    private var roundObjects: List<Toy> = emptyList()
    private var trainSequence = mutableListOf<Int>()
    private var trainStep = 0
    private var surpriseEmoji = "🦄"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stars = prefs.getInt("stars", 0)
        collected.addAll(prefs.getStringSet("collected", emptySet()) ?: emptySet())
        window.statusBarColor = AColor.rgb(13, 20, 43)
        window.navigationBarColor = AColor.rgb(13, 20, 43)
        window.decorView.systemUiVisibility = 0
        createRoot()
        showHome()
        playClip("intro")
    }

    private fun createRoot() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = gradient("#111A36", "#293D68", 0)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(12))
            addView(root)
        }
        overlay = FrameLayout(this)
        overlay.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(overlay)
    }

    private fun resetPage(title: String, subtitle: String = "") {
        root.removeAllViews()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(10))
        }
        header.addView(TextView(this).apply {
            text = title
            textSize = 27f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(AColor.WHITE)
        })
        if (subtitle.isNotBlank()) header.addView(TextView(this).apply {
            text = subtitle
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(AColor.rgb(202, 213, 242))
            setPadding(0, dp(3), 0, dp(5))
        })
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(4), dp(16), dp(12))
        }
        root.addView(page, LinearLayout.LayoutParams(-1, -2))
        page.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            private var index = 0
            override fun onChildViewAdded(parent: View?, child: View?) {
                child ?: return
                val delay = (index++ * 55L).coerceAtMost(440L)
                child.alpha = 0f
                child.translationY = dp(22).toFloat()
                child.scaleX = .96f
                child.scaleY = .96f
                child.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(delay).setDuration(360).setInterpolator(android.view.animation.OvershootInterpolator(0.8f)).start()
            }
            override fun onChildViewRemoved(parent: View?, child: View?) {}
        })
        addBottomNavigation()
    }

    private fun addBottomNavigation() {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = gradient("#4656A1", "#202D57", 24)
            elevation = dp(12).toFloat()
        }
        nav.addView(navButton("🏠  الرَّئِيسِيَّةُ") { showHome() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(navButton("🎁  مَجْمُوعَتِي") { showCollection() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(nav, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(12), dp(4), dp(12), dp(10))
        })
    }

    private fun showHome() {
        resetPage("مَرْحَبًا بِكَ فِي عَالَمِ الأَلْوَانِ", "هَيَّا نَلْعَبْ وَنَتَعَلَّمْ مَعَ الأَرْنَبِ!")
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = gradient("#5269B4", "#26375F", 28)
            elevation = dp(12).toFloat()
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        hero.addView(BunnyView(this), LinearLayout.LayoutParams(dp(126), dp(142)))
        val welcome = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        welcome.addView(TextView(this).apply {
            text = "أَهْلًا يَا صَدِيقِي!"
            textSize = 22f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(AColor.WHITE); gravity = Gravity.CENTER
        })
        welcome.addView(TextView(this).apply {
            text = "اِخْتَرْ لُعْبَةً وَهَيَّا نَمْرَحْ"
            textSize = 15f; setTextColor(AColor.rgb(218, 226, 255)); gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, 0)
        })
        hero.addView(welcome, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val score = TextView(this).apply {
            text = "⭐  " + stars.toString().mapDigits() + "     🎁  " + collected.size.toString().mapDigits()
            textSize = 19f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            setTextColor(AColor.WHITE); background = gradient("#4D65A7", "#25365F", 18)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        page.addView(score, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        page.addView(sectionTitle("مَاذَا تُرِيدُ أَنْ تَفْعَلَ؟"))
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        val items = listOf(
            MenuItem("🎨", "تَعَلَّمِ الأَلْوَانَ", "#7A83E8") { showLearn() },
            MenuItem("🎈", "فَقِّعِ البَالُونَ", "#F06B86") { startBalloonGame() },
            MenuItem("🔎", "اِبْحَثْ عَنِ اللَّوْنِ", "#4BBE9B") { startFindGame() },
            MenuItem("🥚", "المُفَاجَأَةُ", "#A77BE8") { showSurprise() },
            MenuItem("🚂", "قِطَارُ الأَلْوَانِ", "#F2A343") { startTrainGame() },
            MenuItem("👂", "اِسْمَعْ وَاخْتَرْ", "#4CA6D8") { startListenGame() },
            MenuItem("🧩", "اِخْتَرِ اللَّوْنَ", "#D78B58") { startClassicQuiz() }
        )
        items.forEachIndexed { index, item ->
            grid.addView(menuTile(item.emoji, item.label, item.color, item.action), GridLayout.LayoutParams().apply {
                width = 0; height = dp(116)
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
                setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid)
        page.addView(makeButton("🎁  اِفْتَحْ مَجْمُوعَةَ المُفَاجَآتِ", "#8D73D8") { showCollection() },
            LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(10) })
    }

    private fun showLearn() {
        resetPage("هَيَّا نَتَعَلَّمُ الأَلْوَانَ", "اِضْغَطْ عَلَى كُلِّ لَوْنٍ لِتَسْمَعَ اسْمَهُ")
        page.addView(makeButton("🔊  اِسْتَمِعْ إِلَى جَمِيعِ الأَلْوَانِ", "#6572D9") { playAllColors() },
            LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(10) })
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        colors.forEachIndexed { index, item ->
            val tile = colorTile(item, true) { playClip("color_" + item.audio); bounce(it) }
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0; height = dp(126); columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2); setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid)
    }

    private fun startClassicQuiz() {
        resetPage("لُعْبَةُ اِخْتِيَارِ اللَّوْنِ", "اِسْمَعِ السُّؤَالَ ثُمَّ اِلْمَسِ اللَّوْنَ الصَّحِيحَ")
        roundTarget = Random.nextInt(colors.size)
        addRabbitHint("هَيَّا نَخْتَبِرْ ذَاكِرَتَكَ!")
        addTargetCard(roundTarget, "question_")
        val indices = (colors.indices.filter { it != roundTarget }.shuffled().take(5) + roundTarget).shuffled()
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        indices.forEachIndexed { i, index ->
            val tile = colorTile(colors[index], true) { }
            tile.setOnClickListener {
                if (index == roundTarget) {
                    reward("classic")
                    roundSuccess { startClassicQuiz() }
                } else {
                    playClip("try_again")
                    bounce(tile)
                }
            }
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0; height = dp(112); columnSpec = GridLayout.spec(i % 2, 1f)
                rowSpec = GridLayout.spec(i / 2); setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid)
    }

    private fun startBalloonGame() {
        resetPage("فَقِّعِ البَالُونَ!", "اِسْمَعِ اللَّوْنَ ثُمَّ اِلْمَسِ البَالُونَ الصَّحِيحَ")
        roundTarget = Random.nextInt(colors.size)
        addRabbitHint("أَيْنَ البَالُونُ؟")
        addTargetCard(roundTarget, "question_")
        val indices = (colors.indices.filter { it != roundTarget }.shuffled().take(5) + roundTarget).shuffled()
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        indices.forEachIndexed { i, colorIndex ->
            val balloon = BalloonView(this, colors[colorIndex].color)
            balloon.setOnClickListener {
                if (colorIndex == roundTarget) {
                    bounce(balloon); reward("balloon")
                    roundSuccess { startBalloonGame() }
                } else {
                    balloon.animate().translationY(dp(7).toFloat()).setDuration(100).withEndAction {
                        balloon.animate().translationY(0f).setDuration(100).start()
                    }.start()
                    playClip("try_again")
                }
            }
            grid.addView(balloon, GridLayout.LayoutParams().apply {
                width = 0; height = dp(142); columnSpec = GridLayout.spec(i % 2, 1f)
                rowSpec = GridLayout.spec(i / 2); setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid)
        playClips(listOf("game_balloon", "question_" + colors[roundTarget].audio))
    }

    private fun startFindGame() {
        resetPage("اِبْحَثْ عَنِ اللَّوْنِ", "أَيْنَ الشَّيْءُ الَّذِي لَوْنُهُ مِثْلُ اللَّوْنِ المَطْلُوبِ؟")
        roundTarget = Random.nextInt(colors.size)
        addRabbitHint("هَلْ تَجِدُهُ؟")
        addTargetCard(roundTarget, "find_")
        roundObjects = (toys.filter { it.colorIndex != roundTarget }.shuffled().take(5) +
            toys.first { it.colorIndex == roundTarget }).shuffled()
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        roundObjects.forEachIndexed { i, toy ->
            val tile = objectTile(toy)
            tile.setOnClickListener {
                if (toy.colorIndex == roundTarget) {
                    bounce(tile); reward("find"); roundSuccess { startFindGame() }
                } else playClip("try_again")
            }
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0; height = dp(112); columnSpec = GridLayout.spec(i % 2, 1f)
                rowSpec = GridLayout.spec(i / 2); setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid)
        playClips(listOf("game_find", "find_" + colors[roundTarget].audio))
    }

    private fun showSurprise() {
        resetPage("صُنْدُوقُ المُفَاجَآتِ", "اِلْمَسِ البَيْضَةَ السِّحْرِيَّةَ لِتَكْتَشِفَ هَدِيَّتَكَ!")
        addRabbitHint("مَاذَا تُخْفِي البَيْضَةُ؟")
        surpriseEmoji = stickers.filter { it !in collected }.randomOrNull() ?: stickers.random()
        playClip("game_surprise")
        val egg = EggView(this)
        egg.setOnClickListener {
            if (surpriseEmoji !in collected) {
                collected.add(surpriseEmoji)
                saveProgress()
                playClip("surprise_reveal")
            } else playClip("praise")
            page.removeView(egg)
            val reveal = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = gradient("#FFF0B8", "#FFD9EC", 28)
                setPadding(dp(20), dp(22), dp(20), dp(22))
            }
            reveal.addView(TextView(this).apply {
                text = "تَـا دَا!"; textSize = 28f; gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD; setTextColor(AColor.rgb(111, 77, 142))
            })
            reveal.addView(TextView(this).apply { text = surpriseEmoji; textSize = 100f; gravity = Gravity.CENTER })
            reveal.addView(TextView(this).apply {
                text = "هَدِيَّةٌ جَدِيدَةٌ فِي مَجْمُوعَتِكَ!"
                textSize = 18f; gravity = Gravity.CENTER; setTextColor(AColor.rgb(91, 72, 105))
            })
            page.addView(reveal, 2, LinearLayout.LayoutParams(-1, dp(260)).apply { topMargin = dp(12) })
            page.addView(makeButton("🎁  شَاهِدْ مَجْمُوعَتَكَ", "#8D73D8") { showCollection() },
                LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
        }
        page.addView(egg, LinearLayout.LayoutParams(-1, dp(260)).apply { topMargin = dp(8) })
        page.addView(makeButton("مُفَاجَأَةٌ أُخْرَى", "#F19A47") { showSurprise() },
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
    }

    private fun startTrainGame() {
        trainSequence = colors.indices.shuffled().take(3).toMutableList()
        trainStep = 0
        renderTrainStep()
    }

    private fun renderTrainStep() {
        resetPage("قِطَارُ الأَلْوَانِ", "أَكْمِلِ القِطَارَ بِثَلَاثَةِ أَلْوَانٍ مُخْتَلِفَةٍ")
        roundTarget = trainSequence[trainStep]
        addRabbitHint("هَيَّا نَمْلَأُ العَرَبَةَ رَقْمَ " + (trainStep + 1).toString().mapDigits())
        addTargetCard(roundTarget, "question_")
        val train = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = rounded("#FFFFFF", 22); setPadding(dp(8), dp(16), dp(8), dp(16))
        }
        train.addView(TextView(this).apply { text = "🚂"; textSize = 43f; gravity = Gravity.CENTER })
        repeat(3) { slot ->
            val wagon = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                background = rounded(if (slot < trainStep) "#E7F8EC" else "#EDEDF2", 12)
            }
            if (slot < trainStep) {
                wagon.addView(ColorDot(this@MainActivity, colors[trainSequence[slot]].color),
                    LinearLayout.LayoutParams(dp(48), dp(48)))
            } else {
                wagon.addView(TextView(this@MainActivity).apply {
                    text = "؟"; textSize = 26f; gravity = Gravity.CENTER
                    setTextColor(AColor.GRAY)
                })
            }
            train.addView(wagon, LinearLayout.LayoutParams(0, dp(62), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            })
        }
        page.addView(train, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10); bottomMargin = dp(12)
        })
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        colors.indices.shuffled().take(6).forEachIndexed { i, index ->
            val tile = colorTile(colors[index], false) { }
            tile.setOnClickListener {
                if (index == roundTarget) {
                    trainStep++
                    if (trainStep >= trainSequence.size) {
                        reward("train")
                        roundSuccess { startTrainGame() }
                    } else {
                        renderTrainStep()
                    }
                } else {
                    playClip("try_again")
                    bounce(tile)
                }
            }
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0; height = dp(92); columnSpec = GridLayout.spec(i % 2, 1f)
                rowSpec = GridLayout.spec(i / 2); setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid)
        if (trainStep == 0) playClips(listOf("game_train", "question_" + colors[roundTarget].audio))
    }

    private fun startListenGame() {
        resetPage("اِسْمَعْ وَاخْتَرْ", "اِسْتَمِعْ جَيِّدًا، ثُمَّ اِلْمَسِ اللَّوْنَ الَّذِي سَمِعْتَهُ")
        roundTarget = Random.nextInt(colors.size)
        addRabbitHint("هَلْ تَسْمَعُ؟")
        page.addView(makeButton("🔊  اِسْمَعْ مَرَّةً أُخْرَى", "#6572D9") {
            playClip("color_" + colors[roundTarget].audio)
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(10) })
        playClip("color_" + colors[roundTarget].audio)
        val indices = (colors.indices.filter { it != roundTarget }.shuffled().take(5) + roundTarget).shuffled()
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = false }
        indices.forEachIndexed { i, index ->
            val tile = colorTile(colors[index], false) { }
            tile.setOnClickListener {
                if (index == roundTarget) {
                    reward("listen"); roundSuccess { startListenGame() }
                } else playClip("try_again")
            }
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0; height = dp(104); columnSpec = GridLayout.spec(i % 2, 1f)
                rowSpec = GridLayout.spec(i / 2); setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        page.addView(grid)
        playClips(listOf("game_listen", "color_" + colors[roundTarget].audio))
    }

    private fun showCollection() {
        resetPage("مَجْمُوعَةُ المُفَاجَآتِ", "كُلُّ هَدِيَّةٍ تَجْمَعُهَا تَبْقَى مَعَكَ!")
        page.addView(TextView(this).apply {
            text = "⭐  " + stars.toString().mapDigits() + "     🎁  " + collected.size.toString().mapDigits() + " / " + stickers.size.toString().mapDigits()
            textSize = 20f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            setTextColor(AColor.rgb(126, 89, 35)); background = rounded("#FFF1C9", 18)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val grid = GridLayout(this).apply { columnCount = 3; useDefaultMargins = false }
        stickers.forEachIndexed { i, sticker ->
            val unlocked = sticker in collected
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = rounded(if (unlocked) "#FFF0C7" else "#EDECF1", 18)
                setPadding(dp(4), dp(9), dp(4), dp(9))
            }
            tile.addView(TextView(this).apply {
                text = if (unlocked) sticker else "🔒"; textSize = 38f; gravity = Gravity.CENTER
            })
            tile.addView(TextView(this).apply {
                text = if (unlocked) "⭐" else "؟"; textSize = 14f; gravity = Gravity.CENTER
                setTextColor(AColor.rgb(120, 106, 128))
            })
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0; height = dp(100); columnSpec = GridLayout.spec(i % 3, 1f)
                rowSpec = GridLayout.spec(i / 3); setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        page.addView(grid)
        page.addView(makeButton("🥚  اِفْتَحْ بَيْضَةً سِحْرِيَّةً", "#A77BE8") { showSurprise() },
            LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
    }

    private fun addTargetCard(index: Int, prefix: String) {
        val target = colors[index]
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = gradient("#526AAE", "#25365D", 20); setPadding(dp(12), dp(12), dp(12), dp(12)); elevation = dp(8).toFloat()
        }
        panel.addView(ColorDot(this, target.color), LinearLayout.LayoutParams(dp(64), dp(64)))
        panel.addView(TextView(this).apply {
            text = target.name; textSize = 25f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(AColor.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(10) })
        playClip(prefix + target.audio)
    }

    private fun addRabbitHint(message: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = gradient("#586EB2", "#293A67", 20); setPadding(dp(8), dp(5), dp(8), dp(5)); elevation = dp(8).toFloat()
        }
        row.addView(BunnyView(this), LinearLayout.LayoutParams(dp(78), dp(82)))
        row.addView(TextView(this).apply {
            text = message; textSize = 19f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(AColor.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun objectTile(toy: Toy): View {
        val item = colors[toy.colorIndex]
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = gradient("#5067A8", "#26365E", 20)
            elevation = dp(9).toFloat()
            addView(TextView(this@MainActivity).apply { text = toy.emoji; textSize = 43f; gravity = Gravity.CENTER })
            addView(TextView(this@MainActivity).apply {
                text = item.name; textSize = 16f; gravity = Gravity.CENTER
                setTextColor(AColor.WHITE)
            })
        }
    }

    private fun colorTile(item: ColorCard, withLabel: Boolean, action: (View) -> Unit): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = gradient("#536CB2", "#25375F", 20)
            elevation = dp(12).toFloat(); setPadding(dp(5), dp(6), dp(5), dp(6))
        }
        tile.addView(ColorDot(this, item.color), LinearLayout.LayoutParams(dp(52), dp(52)))
        if (withLabel) tile.addView(TextView(this).apply {
            text = item.name; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(AColor.WHITE)
        })
        tile.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) playThumbPop()
            false
        }
        tile.setOnClickListener { action(tile) }
        return tile
    }

    private fun menuTile(emoji: String, label: String, color: String, action: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = gradient(lighten(color), color, 22)
            elevation = dp(14).toFloat(); isClickable = true; isFocusable = true
            addView(TextView(this@MainActivity).apply { text = emoji; textSize = 37f; gravity = Gravity.CENTER })
            addView(TextView(this@MainActivity).apply {
                text = label; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER; setTextColor(AColor.WHITE); setPadding(dp(2), dp(3), dp(2), 0)
            })
            setOnClickListener { action(); bounce(this) }
        }
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value; textSize = 19f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(AColor.rgb(226, 233, 255)); gravity = Gravity.CENTER
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun navButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 13f; isAllCaps = false
        setTextColor(AColor.WHITE); background = gradient("#6479C7", "#344779", 16)
        stateListAnimator = null; setOnClickListener { action() }
    }

    private fun makeButton(label: String, color: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 16f; isAllCaps = false; minHeight = dp(48)
        setTextColor(AColor.WHITE); background = gradient(lighten(color), color, 18)
        stateListAnimator = null; setOnClickListener { action(); bounce(this) }
    }

    private fun roundSuccess(next: () -> Unit) {
        resetPage("أَحْسَنْتَ!", "إِجَابَةٌ رَائِعَةٌ! لَقَدْ كَسَبْتَ نَجْمَةً")
        addRabbitHint("أَنْتَ بَطَلٌ!")
        page.addView(TextView(this).apply {
            text = "⭐  ⭐  ⭐"
            textSize = 54f
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(18))
        })
        page.addView(makeButton("هَيَّا نَلْعَبْ مَرَّةً أُخْرَى", "#F08A45", next),
            LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(10) })
        playClip("praise")
    }

    private fun reward(game: String) {
        stars++
        if (stars % 2 == 0) {
            val next = stickers.firstOrNull { it !in collected }
            if (next != null) collected.add(next)
        }
        saveProgress()
    }

    private fun saveProgress() {
        prefs.edit().putInt("stars", stars).putStringSet("collected", collected.toSet()).apply()
    }

    private fun playClips(names: List<String>) {
        audioQueue.clear()
        audioQueue.addAll(names)
        playNext()
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
                    playNext()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            playNext()
        }
    }

    private fun bounce(view: View) {
        view.animate().cancel()
        view.scaleX = 1f; view.scaleY = 1f
        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(85).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
        }.start()
    }

    private fun lighten(hex: String): String {
        val c = AColor.parseColor(hex)
        fun ch(v: Int) = (v + 255) / 2
        return String.format("#%02X%02X%02X", ch(AColor.red(c)), ch(AColor.green(c)), ch(AColor.blue(c)))
    }

    private fun gradient(first: String, second: String, radius: Int) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(AColor.parseColor(first), AColor.parseColor(second), AColor.parseColor(second))
    ).apply { cornerRadius = dp(radius).toFloat(); setStroke(dp(1), AColor.argb(95, 210, 224, 255)) }

    private fun rounded(color: String, radius: Int) = GradientDrawable().apply {
        setColor(AColor.parseColor(color)); cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), AColor.argb(100, 220, 230, 255))
    }

    private fun playThumbPop() {
        val hand = ThumbPopView(this)
        overlay.addView(hand, FrameLayout.LayoutParams(dp(220), dp(240), Gravity.CENTER))
        hand.alpha = 0f
        hand.scaleX = .22f; hand.scaleY = .22f
        hand.translationY = dp(150).toFloat()
        hand.animate().alpha(1f).scaleX(1.12f).scaleY(1.12f).translationY(dp(-28).toFloat())
            .setDuration(420).setInterpolator(android.view.animation.OvershootInterpolator(1.0f))
            .withEndAction {
                hand.animate().scaleX(1f).scaleY(1f).translationY(dp(-45).toFloat())
                    .setDuration(180).withEndAction {
                        hand.animate().alpha(0f).translationY(dp(-150).toFloat()).setStartDelay(320)
                            .setDuration(260).withEndAction { overlay.removeView(hand) }.start()
                    }.start()
            }.start()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun String.mapDigits(): String = map { c ->
        if (c in '0'..'9') ('٠'.code + (c - '0')).toChar() else c
    }.joinToString("")

    override fun onDestroy() {
        audioQueue.clear()
        player?.release(); player = null
        super.onDestroy()
    }

    // Opaque, child-friendly cartoon glove. The thumb is visibly joined to the palm;
    // four curled fingers form a fist, avoiding any ambiguous raised-finger silhouette.
    private class ThumbPopView(context: Context) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun grad(x1: Float, y1: Float, x2: Float, y2: Float, c1: Int, c2: Int) {
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(x1, y1, x2, y2, c1, c2, Shader.TileMode.CLAMP)
        }
        private fun solid(color: Int) { p.shader = null; p.style = Paint.Style.FILL; p.color = color; p.clearShadowLayer() }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            canvas.scale(width / 220f, height / 240f)
            canvas.rotate(-7f, 110f, 125f)

            solid(AColor.argb(95, 5, 12, 35))
            canvas.drawOval(30f, 205f, 190f, 232f, p)

            // Blue sleeve and white cuff.
            grad(72f, 184f, 148f, 238f, AColor.rgb(115, 177, 255), AColor.rgb(34, 73, 170))
            canvas.drawRoundRect(66f, 179f, 157f, 239f, 22f, 22f, p)
            solid(AColor.rgb(231, 241, 255))
            canvas.drawRoundRect(67f, 178f, 157f, 195f, 8f, 8f, p)

            // Single continuous hand silhouette with curled fingers and a side thumb.
            val palm = Path().apply {
                moveTo(76f, 190f)
                cubicTo(62f, 184f, 55f, 171f, 59f, 154f)
                lineTo(68f, 119f)
                cubicTo(71f, 104f, 85f, 99f, 96f, 108f)
                cubicTo(101f, 114f, 100f, 122f, 97f, 132f)
                lineTo(94f, 143f)
                cubicTo(104f, 137f, 115f, 138f, 121f, 147f)
                cubicTo(133f, 136f, 149f, 140f, 153f, 153f)
                cubicTo(169f, 148f, 181f, 161f, 178f, 175f)
                cubicTo(175f, 194f, 151f, 205f, 123f, 205f)
                lineTo(94f, 202f)
                cubicTo(85f, 201f, 80f, 197f, 76f, 190f)
                close()
            }
            p.setShadowLayer(12f, 3f, 8f, AColor.argb(125, 5, 12, 35))
            grad(65f, 105f, 170f, 205f, AColor.WHITE, AColor.rgb(183, 202, 235))
            canvas.drawPath(palm, p)
            p.clearShadowLayer(); p.shader = null

            // Thumb: a separate rounded shape, rooted into the left edge of the palm.
            val thumb = Path().apply {
                moveTo(79f, 157f)
                cubicTo(70f, 151f, 67f, 142f, 69f, 131f)
                lineTo(76f, 73f)
                cubicTo(78f, 55f, 91f, 46f, 104f, 51f)
                cubicTo(117f, 56f, 120f, 69f, 116f, 84f)
                lineTo(104f, 134f)
                cubicTo(101f, 146f, 94f, 156f, 86f, 162f)
                close()
            }
            p.setShadowLayer(6f, 1f, 4f, AColor.argb(75, 20, 35, 70))
            grad(76f, 52f, 117f, 158f, AColor.WHITE, AColor.rgb(190, 208, 239))
            canvas.drawPath(thumb, p)
            p.clearShadowLayer(); p.shader = null

            // Four rounded knuckles make the folded fingers readable.
            val knuckles = arrayOf(
                floatArrayOf(112f, 145f, 153f, 160f),
                floatArrayOf(108f, 158f, 166f, 175f),
                floatArrayOf(101f, 172f, 160f, 189f),
                floatArrayOf(92f, 184f, 145f, 200f)
            )
            knuckles.forEachIndexed { i, k ->
                p.setShadowLayer(2f, 0f, 2f, AColor.argb(45, 30, 50, 90))
                grad(k[0], k[1], k[2], k[3], AColor.WHITE,
                    AColor.rgb(194 - i * 3, 211 - i * 3, 239 - i * 2))
                canvas.drawRoundRect(k[0], k[1], k[2], k[3], 10f, 10f, p)
                p.clearShadowLayer(); p.shader = null
            }

            // Blue-gray contour, glove highlights and simple seam details.
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f
            p.color = AColor.rgb(112, 139, 190)
            canvas.drawPath(palm, p)
            canvas.drawPath(thumb, p)
            p.style = Paint.Style.FILL
            solid(AColor.WHITE)
            canvas.drawOval(85f, 59f, 97f, 87f, p)
            canvas.drawOval(120f, 148f, 137f, 153f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            p.color = AColor.rgb(163, 184, 220)
            canvas.drawArc(82f, 110f, 101f, 137f, 35f, 105f, false, p)
            canvas.drawArc(111f, 159f, 150f, 179f, 195f, 125f, false, p)
            p.style = Paint.Style.FILL
            canvas.restore()
        }
    }

    private class ColorDot(context: Context, private val fill: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            val s = minOf(width, height).toFloat()
            val cx = width / 2f; val cy = height / 2f
            paint.shader = LinearGradient(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2,
                lighten(fill), fill, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, s * 0.39f, paint)
            paint.shader = null
            paint.color = AColor.argb(70, 255, 255, 255)
            canvas.drawOval(cx - s * .22f, cy - s * .27f, cx + s * .02f, cy - s * .12f, paint)
        }
        private fun lighten(color: Int): Int = AColor.rgb(
            (AColor.red(color) + 255) / 2, (AColor.green(color) + 255) / 2, (AColor.blue(color) + 255) / 2
        )
    }

    private class BalloonView(context: Context, private val fill: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f; val cy = height * .43f
            val r = minOf(width * .31f, height * .31f)
            paint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r,
                AColor.rgb(255, 255, 255), fill, Shader.TileMode.CLAMP)
            canvas.drawOval(cx - r, cy - r * 1.1f, cx + r, cy + r * 1.1f, paint)
            paint.shader = null
            paint.color = fill
            val knot = Path().apply { moveTo(cx - 8, cy + r * 1.05f); lineTo(cx + 8, cy + r * 1.05f); lineTo(cx, cy + r * 1.25f); close() }
            canvas.drawPath(knot, paint)
            paint.color = AColor.GRAY
            paint.strokeWidth = dp(1.5f)
            canvas.drawLine(cx, cy + r * 1.25f, cx + 4, height.toFloat() - dp(6f), paint)
            paint.color = AColor.argb(110, 255, 255, 255)
            canvas.drawOval(cx - r * .48f, cy - r * .65f, cx - r * .2f, cy - r * .32f, paint)
        }
        private fun dp(v: Float) = v * resources.displayMetrics.density
    }

    private class EggView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f; val cy = height / 2f
            val w = minOf(width * .48f, height * .72f)
            val h = w * 1.28f
            paint.setShadowLayer(16f, 0f, 8f, AColor.argb(45, 90, 60, 90))
            paint.shader = LinearGradient(cx - w, cy - h, cx + w, cy + h,
                AColor.rgb(255, 255, 255), AColor.rgb(255, 205, 224), Shader.TileMode.CLAMP)
            canvas.drawOval(cx - w * .78f, cy - h, cx + w * .78f, cy + h, paint)
            paint.shader = null; paint.clearShadowLayer()
            val dots = listOf(
                Triple(-.35f, -.45f, "#F2A343"), Triple(.32f, -.52f, "#7E8AE8"),
                Triple(-.4f, .05f, "#54C49B"), Triple(.38f, .12f, "#F28BC8"),
                Triple(0f, .52f, "#A77BE8")
            )
            dots.forEach { (x, y, color) ->
                paint.color = AColor.parseColor(color)
                canvas.drawCircle(cx + w * x, cy + h * y, w * .12f, paint)
            }
            paint.color = AColor.argb(100, 255, 255, 255)
            canvas.drawOval(cx - w * .48f, cy - h * .62f, cx - w * .23f, cy - h * .4f, paint)
        }
    }

    private class BunnyView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val base = height * .57f
            val scale = minOf(width / 150f, height / 150f)
            canvas.save()
            canvas.translate(cx, base)
            canvas.scale(scale, scale)
            paint.color = AColor.rgb(255, 255, 255)
            paint.setShadowLayer(5f, 0f, 4f, AColor.argb(40, 80, 60, 90))
            canvas.drawOval(-37f, -25f, 37f, 48f, paint)
            paint.clearShadowLayer()
            canvas.drawOval(-28f, -108f, -5f, -30f, paint)
            canvas.drawOval(5f, -108f, 28f, -30f, paint)
            paint.color = AColor.rgb(250, 190, 211)
            canvas.drawOval(-22f, -99f, -11f, -39f, paint)
            canvas.drawOval(11f, -99f, 22f, -39f, paint)
            paint.color = AColor.WHITE
            canvas.drawCircle(0f, -27f, 39f, paint)
            paint.color = AColor.rgb(65, 55, 70)
            canvas.drawCircle(-14f, -32f, 3.8f, paint)
            canvas.drawCircle(14f, -32f, 3.8f, paint)
            paint.color = AColor.rgb(245, 140, 170)
            canvas.drawCircle(0f, -20f, 4.5f, paint)
            paint.color = AColor.rgb(130, 90, 95)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
            canvas.drawArc(-9f, -22f, 0f, -10f, 0f, 90f, false, paint)
            canvas.drawArc(0f, -22f, 9f, -10f, 90f, 90f, false, paint)
            paint.style = Paint.Style.FILL
            paint.color = AColor.rgb(250, 190, 211)
            canvas.drawCircle(-28f, -17f, 7f, paint); canvas.drawCircle(28f, -17f, 7f, paint)
            canvas.restore()
        }
    }
}