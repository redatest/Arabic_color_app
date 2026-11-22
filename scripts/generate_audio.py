import asyncio
from pathlib import Path
import edge_tts

VOICE = "ar-SA-ZariyahNeural"
RATE = "-18%"
OUT = Path("app/src/main/assets/audio")

CLIPS = {
    "intro": "مَرْحَبًا! هَيَّا نَتَعَلَّمُ الأَلْوَانَ مَعًا.",
    "all_colors_intro": "اِسْتَمِعْ إِلَى أَسْمَاءِ الأَلْوَانِ.",
    "praise": "أَحْسَنْتَ! إِجَابَةٌ صَحِيحَةٌ! أَنْتَ مُمْتَازٌ!",
    "try_again": "حَاوِلْ مَرَّةً أُخْرَى. أَنْتَ تَسْتَطِيعُ!",
    "surprise_reveal": "مَا أَجْمَلَ هَذِهِ الهَدِيَّةَ! أَحْسَنْتَ!",
    "game_balloon": "هَيَّا نَفْقَعُ البَالُونَ الصَّحِيحَ!",
    "game_find": "اِبْحَثْ عَنِ الشَّيْءِ الَّذِي يَحْمِلُ هَذَا اللَّوْنَ!",
    "game_surprise": "مَا الَّذِي تُخْفِيهِ البَيْضَةُ السِّحْرِيَّةُ؟",
    "game_train": "هَيَّا نُكْمِلُ قِطَارَ الأَلْوَانِ!",
    "game_listen": "اِسْتَمِعْ جَيِّدًا وَاخْتَرِ اللَّوْنَ الصَّحِيحَ!",
    "color_red": "أَحْمَرُ.",
    "color_blue": "أَزْرَقُ.",
    "color_yellow": "أَصْفَرُ.",
    "color_green": "أَخْضَرُ.",
    "color_orange": "بُرْتُقَالِيٌّ.",
    "color_pink": "وَرْدِيٌّ.",
    "color_purple": "بَنَفْسَجِيٌّ.",
    "color_black": "أَسْوَدُ.",
    "color_white": "أَبْيَضُ.",
    "color_brown": "بُنِّيٌّ.",
    "question_red": "أَيْنَ اللَّوْنُ الأَحْمَرُ؟",
    "question_blue": "أَيْنَ اللَّوْنُ الأَزْرَقُ؟",
    "question_yellow": "أَيْنَ اللَّوْنُ الأَصْفَرُ؟",
    "question_green": "أَيْنَ اللَّوْنُ الأَخْضَرُ؟",
    "question_orange": "أَيْنَ اللَّوْنُ البُرْتُقَالِيُّ؟",
    "question_pink": "أَيْنَ اللَّوْنُ الوَرْدِيُّ؟",
    "question_purple": "أَيْنَ اللَّوْنُ البَنَفْسَجِيُّ؟",
    "question_black": "أَيْنَ اللَّوْنُ الأَسْوَدُ؟",
    "question_white": "أَيْنَ اللَّوْنُ الأَبْيَضُ؟",
    "question_brown": "أَيْنَ اللَّوْنُ البُنِّيُّ؟",
    "find_red": "أَيْنَ الشَّيْءُ الأَحْمَرُ؟",
    "find_blue": "أَيْنَ الشَّيْءُ الأَزْرَقُ؟",
    "find_yellow": "أَيْنَ الشَّيْءُ الأَصْفَرُ؟",
    "find_green": "أَيْنَ الشَّيْءُ الأَخْضَرُ؟",
    "find_orange": "أَيْنَ الشَّيْءُ البُرْتُقَالِيُّ؟",
    "find_pink": "أَيْنَ الشَّيْءُ الوَرْدِيُّ؟",
    "find_purple": "أَيْنَ الشَّيْءُ البَنَفْسَجِيُّ؟",
    "find_black": "أَيْنَ الشَّيْءُ الأَسْوَدُ؟",
    "find_white": "أَيْنَ الشَّيْءُ الأَبْيَضُ؟",
    "find_brown": "أَيْنَ الشَّيْءُ البُنِّيُّ؟",
}

async def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, text in CLIPS.items():
        target = OUT / f"{name}.mp3"
        print(f"Generating {name} with {VOICE}")
        await edge_tts.Communicate(text, voice=VOICE, rate=RATE).save(str(target))
        if not target.exists() or target.stat().st_size < 1000:
            raise RuntimeError(f"Invalid audio output: {target}")
    print(f"Generated {len(CLIPS)} Arabic audio clips.")

if __name__ == "__main__":
    asyncio.run(main())
