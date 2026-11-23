package app.secondsight.domain

/** A tale the orb shows, one vision at a time. Vision art: drawable v_<id>_<1..8>. */
class Tale(val id: String, private val ru: String, private val en: String, val visions: List<String>) {
    val title get() = tr(ru, en)
    fun line(i: Int) = bi(visions[i])
    fun art(i: Int) = "v_${id}_${i + 1}"
}

object Tales {
    /** Glimpses a vision needs before the Darkness can show it. */
    const val GLIMPSES = 6

    val all = listOf(
        Tale("t1", "Звезда в пруду", "The Star in the Pond", listOf(
            "Ночью над чайными холмами сорвалась звезда и упала в пруд с лотосами.|One night a star fell over the tea hills, into the lotus pond.",
            "На рассвете маленькая сборщица чая увидела, что среди листьев лотоса что-то светится.|At dawn a little tea picker saw something glowing among the lotus leaves.",
            "Она достала звезду из воды. Звезда была маленькая, холодная и почти погасшая.|She lifted the star out of the water. It was small and cold, and almost out.",
            "Девочка спрятала её в бумажный фонарик и пошла по деревне спрашивать, как вернуть звезду на небо.|She hid it in a paper lantern and went through the village asking how to put a star back.",
            "Старый гончар только развёл руками: «Её огонь не наш. Ей нужны высота и темнота».|The old potter just spread his hands: “Its fire isn't ours. It needs height and darkness.”",
            "К вечеру она поднялась по тысяче ступеней на самую вершину, выше облаков.|By evening she had climbed a thousand steps to the very top, above the clouds.",
            "Там она закрыла глаза и долго сидела в темноте, а звезда в её ладонях разгоралась.|There she closed her eyes and sat in the dark for a long time, and the star grew brighter in her palms.",
            "Звезда поднялась на своё место. А у девочки с тех пор в глазах горит искорка.|The star rose back to its place. And ever since, a little spark shines in the girl's eyes.",
        )),
        Tale("t2", "Лодочник и журавль", "The Boatman and the Crane", listOf(
            "На туманной реке между горами жил старый лодочник, и с ним белый журавль.|An old boatman lived on a misty river between the mountains, and a white crane lived with him.",
            "Однажды туман лёг такой, что лодочник не видел собственного весла.|One day the fog was so thick that the boatman couldn't see his own oar.",
            "А на берегу ждали люди: им нужно было домой, на тот берег.|And people were waiting on the bank: they had to get home, across the river.",
            "Тогда журавль поднялся над туманом, туда, где видно звёзды.|So the crane rose above the fog, up to where the stars could be seen.",
            "Он кричал с того берега, и лодочник правил на голос, закрыв глаза.|He called from the far shore, and the boatman steered by his voice with his eyes closed.",
            "Лодка шла сквозь туман, и никто в ней не боялся.|The boat went through the fog, and nobody in it was afraid.",
            "К рассвету все были дома.|By dawn everyone was home.",
            "С тех пор лодочник говорит: «Глаза глазами, а видеть надо учиться».|Since then the boatman says: “Eyes are eyes, but seeing is something you learn.”",
        )),
        Tale("t3", "Барсук и чай", "The Badger's Tea", listOf(
            "На горном склоне барсук растил чай, и лучше его чая не было во всей долине.|A badger grew tea on a mountain slope, and there was no better tea in the whole valley.",
            "Каждое утро он смотрел на дальние горы: оттуда приходят дождевые тучи.|Every morning he watched the far mountains: that's where the rain clouds came from.",
            "Осенью он понёс чай вниз, на рынок.|In autumn he carried his tea down to the market.",
            "Там он выменял чай на тёплое одеяло и бумажный фонарь.|There he traded it for a warm quilt and a paper lantern.",
            "Зимой ударили морозы, и барсук укрыл кусты соломенными циновками.|Then the frosts came, and the badger covered the bushes with straw mats.",
            "Сам он уснул в норе и всю зиму видел во сне зелёные склоны.|He fell asleep in his burrow and dreamt of green slopes all winter.",
            "Весной он вышел наружу и зажмурился: так ярко светило солнце.|In spring he came out and squinted: the sun was that bright.",
            "А на кустах уже проклюнулись свежие листочки.|And fresh young leaves were already showing on the bushes.",
        )),
        Tale("t4", "Гончар и луна", "The Potter and the Moon", listOf(
            "Зимней ночью луна зацепилась за верхушку старой сосны и треснула.|One winter night the moon caught on the top of an old pine and cracked.",
            "В деревне стало так темно, что люди не могли найти свои ворота.|It grew so dark in the village that people couldn't find their own gates.",
            "Молодой гончар нашёл в снегу светящийся осколок луны.|A young potter found a glowing piece of the moon in the snow.",
            "Он взял горшочек жидкого золота, кисть и полез на самую высокую сосну.|He took a little pot of liquid gold and a brush and climbed the tallest pine.",
            "Всю ночь он склеивал луну золотом, как склеивают разбитую чашу.|All night he mended the moon with gold, the way you mend a broken bowl.",
            "К утру луна снова была целой, только с тонкими золотыми швами.|By morning the moon was whole again, with thin golden seams.",
            "Люди вышли на улицу и долго смотрели вверх.|People came out into the street and looked up for a long time.",
            "А гончар лёг отдохнуть, и жена накрыла ему глаза тёплыми ладонями.|And the potter lay down to rest, and his wife covered his eyes with her warm palms.",
        )),
        Tale("t5", "Дозорный", "The Watch Boy", listOf(
            "На горном хребте стояла сторожевая башня, и на ней нёс дозор мальчик.|A watchtower stood on a mountain ridge, and a boy kept watch there.",
            "Мальчик любил читать и целыми днями сидел над свитками.|The boy loved to read and sat over his scrolls all day long.",
            "Однажды он поднял глаза, а дальние горы расплываются.|One day he looked up, and the far mountains were a blur.",
            "Пришла старая провидица и сказала: «Смотри вдаль. Там глазам отдых».|The old seer came and said: “Look far away. That's where your eyes rest.”",
            "С тех пор мальчик каждый час смотрел, как над дальними горами летят журавли.|After that, every hour the boy watched the cranes flying over the far mountains.",
            "И однажды первым заметил далёкий дымок.|And one day he was the first to spot a wisp of smoke far away.",
            "Пожар потушили, пока он не разгорелся.|The fire was put out before it could spread.",
            "Мальчику подарили подзорную трубу, но вдаль он смотрит и без неё.|The boy was given a spyglass, but he looks far away without it.",
        )),
        Tale("t6", "Ткачиха облаков", "The Cloud Weaver", listOf(
            "Высоко над вершинами живёт Ткачиха и ткёт облака.|High above the peaks lives the Weaver, and she weaves the clouds.",
            "Когда она ткёт, на горы падает снег.|While she weaves, snow falls on the mountains.",
            "Но однажды у Ткачихи устали глаза, и станок остановился.|But one day the Weaver's eyes grew tired, and the loom stopped.",
            "Старая провидица прилетела к ней на журавле.|The old seer flew up to her on a crane.",
            "«Поработала – посмотри на дальние звёзды», – сказала она.|“When you've worked a while, look at the far stars,” she said.",
            "«А вечером закрой глаза ладонями и посиди в темноте».|“And in the evening, cover your eyes with your palms and sit in the dark.”",
            "Ткачиха снова села за станок, и повалил пушистый снег.|The Weaver sat down at her loom again, and fluffy snow came pouring down.",
            "А провидице на рукав упала снежинка – самая красивая из всех.|And a snowflake fell on the seer's sleeve – the most beautiful of them all.",
        )),
    )

    fun of(id: String) = all.firstOrNull { it.id == id }
}
