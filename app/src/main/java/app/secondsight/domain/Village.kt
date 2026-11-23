package app.secondsight.domain

/** What a question asks of the apprentice: [what] is a practice id, "herb" (any herbs) or "sun" (daylight minutes). */
data class Need(val what: String, val n: Int) {
    val label: String get() = when (what) {
        "herb" -> count(n, "пучок трав", "пучка трав", "пучков трав", "bunch of herbs", "bunches of herbs")
        "sun" -> tr("$n мин при дневном свете", "$n min of daylight")
        else -> Practice.of(what)!!.let { p -> if (n == 1) p.title else "${p.title} × $n" }
    }
}

/** One visit: the question, what to do for the answer, and the answer the orb gives. */
class Ask(val q: String, val needs: List<Need>, val answer: String)

/** A pilgrim who climbs up to the pavilion. Portrait: drawable face_<id>; after the last visit leaves gift_<id>. */
class Villager(val id: String, private val ru: String, private val en: String, val asks: List<Ask>, private val giftRu: String, private val giftEn: String) {
    val name get() = tr(ru, en)
    val gift get() = tr(giftRu, giftEn)
}

private fun n(vararg p: Pair<String, Int>) = p.map { Need(it.first, it.second) }

object Village {
    val all = listOf(
        Villager("goatherd", "Пастушок", "The goatherd boy", listOf(
            Ask("Моя белая коза пропала! Где её искать?|My white goat is gone! Where do I look?",
                n("far" to 3),
                "В шаре старая ива над ручьём и колокольчик в траве. Иди к ручью, коза там щиплет клевер.|The orb shows an old willow over a brook and a bell in the grass. Go to the brook – your goat is there, eating clover."),
            Ask("Можно мне ночью пасти стадо вместе со взрослыми?|Can I watch the herd at night with the grown-ups?",
                n("dark" to 1, "track" to 1),
                "Вижу костёр, стадо и тебя у огня. Отпустят, только возьми тёплую накидку: к утру будет зябко.|I see a fire, the herd and you by the flames. They'll let you go – just take a warm cloak, it gets chilly by dawn."),
            Ask("Кем я стану, когда вырасту?|What will I be when I grow up?",
                n("far" to 5, "herb" to 1),
                "Вижу горные тропы, много троп, и ты на них не заблудишься. Будешь водить людей туда, куда другие не знают дороги.|I see mountain paths, many of them, and you never get lost. You'll lead people where others don't know the way."),
        ), "Колокольчик белой козы", "The white goat's bell"),
        Villager("teahouse", "Хозяйка чайной", "The teahouse keeper", listOf(
            Ask("Чай у меня горчит третий день. Что не так?|My tea has been bitter for three days. What's wrong?",
                n("blink" to 2, "far" to 2),
                "Вижу, как ты льёшь крутой кипяток на нежный лист. Дай воде немного остыть, и горечь уйдёт.|I see you pouring boiling water on tender leaves. Let the water cool a little, and the bitterness will go."),
            Ask("Гостей стало мало. Может, чайную сглазили?|Fewer guests come now. Has someone cursed my teahouse?",
                n("nearfar" to 1, "sun" to 15),
                "Никто её не сглазил. Над твоей дверью погас фонарь, и вечером путники проходят мимо. Повесь новый.|Nobody did. The lantern over your door has gone out, and in the evening travellers walk past. Hang a new one."),
            Ask("Сын ушёл учиться в город. Как он там?|My son went to study in the city. How is he?",
                n("dark" to 2, "side" to 1),
                "Вижу его в светлой комнате над книгами. Он сыт и здоров, а к осени жди от него письма.|I see him in a bright room over his books. He's fed and well, and expect a letter from him by autumn."),
        ), "Глиняный чайник", "A clay teapot"),
        Villager("potter", "Гончар", "The potter", listOf(
            Ask("К вечеру всё плывёт перед глазами. Это от жара печи?|By evening everything swims before my eyes. Is it the kiln's heat?",
                n("far" to 4),
                "И от жара, и от того, что весь день смотришь в одну точку. Каждый час выходи из мастерской и смотри на дальние горы.|The heat, and staring at one spot all day. Every hour step out of the workshop and look at the far mountains."),
            Ask("Руки к ночи ноют. Чем им помочь?|My hands ache by night. What will help?",
                n("hands" to 2),
                "Вижу тебя у ручья: руки сначала в холодной воде, потом в тёплой. И разминай кисти, как я тебе показала.|I see you at the brook, hands in cold water, then in warm. And stretch your wrists the way I showed you."),
            Ask("Мне предлагают большой заказ. Браться?|I've been offered a big order. Should I take it?",
                n("dark" to 1, "jump" to 2, "back" to 1),
                "Берись, только возьми ученика. Одному не управиться, а вдвоём успеете к сроку, и спину побережёшь.|Take it, but take an apprentice too. You won't manage alone, but two of you will finish on time and spare your back."),
        ), "Чаша с золотыми швами", "A bowl with golden seams"),
        Villager("bride", "Невеста", "The bride", listOf(
            Ask("Скажи, он правда меня любит?|Tell me, does he really love me?",
                n("dark" to 1),
                "Вижу его ночью под твоим окном, он смотрит на твой фонарик. Любит, не сомневайся.|I see him under your window at night, looking at your lantern. He loves you, have no doubt."),
            Ask("Когда играть свадьбу, весной или осенью?|When should we have the wedding, spring or autumn?",
                n("far" to 3, "herb" to 2),
                "Вижу красные клёны и полные амбары. Играйте осенью, когда соберёте урожай.|I see red maples and full barns. Have it in autumn, when the harvest is in."),
            Ask("Будет ли в нашем доме лад?|Will there be peace in our home?",
                n("breath" to 2, "side" to 1, "sun" to 20),
                "Вижу стол, за ним смеются двое, а под столом спит кот. Лад будет, если не оставлять ссору на ночь.|I see a table, two people laughing at it and a cat asleep under it. There'll be peace if you never leave a quarrel for the night."),
        ), "Красный веер", "A red fan"),
        Villager("fisher", "Старый рыбак", "The old fisherman", listOf(
            Ask("Где нынче рыба? Третий день возвращаюсь пустой.|Where are the fish these days? Three days I've come home empty.",
                n("far" to 2, "track" to 1),
                "Вижу омут за старой ивой, где тёмная вода. Рыба стоит там и прячется от жары.|I see a deep pool past the old willow, where the water is dark. The fish are there, hiding from the heat."),
            Ask("Глаза стали не те, поплавок не вижу.|My eyes aren't what they were, I can't see the float.",
                n("nearfar" to 2, "blink" to 2),
                "Глаза у тебя ещё крепкие, просто устают. Посмотри на поплавок, потом на дальний берег, и так несколько раз. И моргай почаще.|Your eyes are still strong, they just tire. Look at the float, then at the far bank, a few times over. And blink more."),
            Ask("Жена моя хворает. Поправится?|My wife is ill. Will she get better?",
                n("dark" to 2, "herb" to 2),
                "Вижу её у окна, она пьёт хризантемовый чай. Поправится. Только сам сиди с ней почаще.|I see her by the window, drinking chrysanthemum tea. She'll get better. Just sit with her more often."),
        ), "Резная рыбка", "A carved fish"),
        Villager("merchant", "Купец", "The merchant", listOf(
            Ask("Ехать мне с товаром за перевал или нет?|Should I take my goods over the pass or not?",
                n("far" to 3, "jump" to 1),
                "Вижу размытую дорогу и сломанное колесо. Подожди неделю, пока она подсохнет, тогда и поезжай.|I see a washed-out road and a broken wheel. Wait a week for it to dry, then go."),
            Ask("Счёт не сходится, а вора я не вижу.|My accounts don't add up, and I see no thief.",
                n("side" to 2, "dark" to 1),
                "Вора и нет. Вижу страницу, где ты ошибся сам. Пересчитай медленно, и не на ночь глядя.|There's no thief. I see a page where you made the mistake yourself. Count again slowly, and not late at night."),
            Ask("Кому оставить лавку, сыну или дочери?|Who should get the shop, my son or my daughter?",
                n("dark" to 1, "far" to 3, "breath" to 1),
                "Вижу, как дочь торгуется и смеётся, а сын сидит над свитками. Лавку отдай дочери, а сына отпусти учиться.|I see your daughter bargaining and laughing, and your son bent over scrolls. Give the shop to her, and let him study."),
        ), "Монетка на красном шнурке", "A coin charm"),
        Villager("weaver", "Старая ткачиха", "The old weaver", listOf(
            Ask("Соседка, небось, на меня порчу навела?|My neighbour must have cursed me, surely?",
                n("far" to 2, "blink" to 1),
                "Порчи не вижу. Вижу, как соседка несёт тебе пирожки, а ты не открываешь ей дверь.|I see no curse. I see your neighbour bringing you dumplings, and you not opening the door."),
            Ask("Спина болит, ночью не могу уснуть.|My back hurts, I can't sleep at night.",
                n("back" to 2, "sun" to 15),
                "Вижу, как ты гуляешь у реки, днём, на солнце. Ходи понемногу каждый день, и спина отпустит.|I see you walking by the river in the daytime sun. Walk a little every day, and your back will ease."),
            Ask("Научи меня видеть, как ты.|Teach me to see the way you do.",
                n("dark" to 2, "track" to 1, "nearfar" to 1),
                "Никакого секрета тут нет. Смотри вдаль, давай глазам темноту и не держи зла. Остальное придёт само.|There's no secret. Look far away, give your eyes darkness and don't hold grudges. The rest comes by itself."),
        ), "Отрез узорного шёлка", "A length of patterned silk"),
        Villager("girl", "Девочка со змеем", "The girl with the kite", listOf(
            Ask("У меня есть котёнок, а как его назвать, не знаю.|I have a kitten, and I don't know what to call it.",
                n("blink" to 1, "far" to 1),
                "Вижу, как он сидит на окне и ловит снежинки. Назови его Снежком.|I see it sitting on the windowsill catching snowflakes. Call it Snowball."),
            Ask("Почему у меня в школе буквы расплываются?|Why do the letters blur at school?",
                n("far" to 3, "nearfar" to 1, "sun" to 20),
                "Ты много сидишь над книжкой и мало гуляешь. Каждый день запускай змея, пока светло, и смотри на облака. А маму попроси сводить тебя к глазному врачу.|You sit over books a lot and play outside too little. Fly your kite every day while it's light, and look at the clouds. And ask your mum to take you to an eye doctor."),
            Ask("А мне можно стать провидицей?|Can I become a seer too?",
                n("dark" to 1, "side" to 1, "herb" to 1),
                "Вижу тебя взрослой, с корзинкой трав. Паломники поднимаются сюда, а встречаешь их ты. Приходи, научу.|I see you grown up, with a basket of herbs. Pilgrims climb up here, and you're the one who greets them. Come, I'll teach you."),
        ), "Бумажный змей", "A paper kite"),
    )

    fun of(id: String) = all.firstOrNull { it.id == id }
}

/** Tea herbs: picked in daylight, in their season. Art: herb_<id>. */
enum class Herb(val id: String, val months: Set<Int>, private val ru: String, private val en: String) {
    TEA("tea", setOf(4, 5), "Чайный лист", "Tea leaves"),
    JASMINE("jasmine", setOf(6, 7, 8), "Жасмин", "Jasmine"),
    CHRYSANTHEMUM("chrys", setOf(9, 10, 11), "Хризантема", "Chrysanthemum"),
    OSMANTHUS("osmanthus", setOf(9, 10), "Османтус", "Osmanthus"),
    PLUM("plum", setOf(2, 3), "Цвет сливы", "Plum blossom"),
    LOTUS("lotus", setOf(7, 8), "Лотос", "Lotus"),
    GINGER("ginger", setOf(10, 11, 12), "Имбирь", "Ginger"),
    MINT("mint", setOf(5, 6, 7, 8, 9), "Мята", "Mint"),
    GOJI("goji", setOf(8, 9, 10), "Ягоды годжи", "Goji berries"),
    BAMBOO("bamboo", setOf(11, 12, 1, 4, 5, 6), "Лист бамбука", "Bamboo leaves"),
    PINE("pine", setOf(12, 1, 2), "Сосновые иглы", "Pine needles"),
    CAMELLIA("camellia", setOf(1, 2, 3), "Камелия", "Camellia");

    val title get() = tr(ru, en)
    val art get() = "herb_$id"

    companion object {
        /** Minutes of daylight for one bunch. */
        const val MINUTES = 15
        fun of(id: String) = entries.firstOrNull { it.id == id }
        /** The herbs of this month; a picked bunch is one of them, in turn. */
        fun season(month: Int) = entries.filter { month in it.months }.ifEmpty { listOf(PINE) }
    }
}

/** The apprentice's rank by the clarity gathered (one glimpse = one point, a vision = 5). */
enum class Rank(val from: Int, private val mRu: String, private val fRu: String, private val en: String) {
    NOVICE(0, "Послушник", "Послушница", "Novice"),
    APPRENTICE(150, "Ученик", "Ученица", "Apprentice"),
    SEEING(600, "Видящий", "Видящая", "Seeing one"),
    SEER(1800, "Провидец", "Провидица", "Seer");

    val title get() = if (Lang.ru) g(mRu, fRu) else en
    val next get() = entries.getOrNull(ordinal + 1)

    companion object { fun of(points: Int) = entries.last { points >= it.from } }
}
