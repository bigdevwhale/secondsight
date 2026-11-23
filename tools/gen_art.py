"""Generates Second Sight's art with the Codex CLI image tool (the Leyline pipeline).

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py                 # everything missing
    python tools/gen_art.py hall people        # only these
    python tools/gen_art.py --ref t1_1 t1_2 # pass art/sheets/t1_1.png as style reference
    python tools/gen_art.py --tales         # every vision, each tale with its first vision as reference
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

HEAD = "Use your image generation tool to create ONE image. Do nothing else (no file copying).\n\n"

STYLE = (
    "Style: an East Asian ink-and-wash painting with soft watercolour on silk: misty layered mountains, old pines, bamboo, curved roofs of a "
    "small mountain monastery, flowing brush lines in dark ink, delicate washes of indigo, jade green, soft rose and warm lantern amber; lots of "
    "empty misty space, calm and quiet. Night scenes are deep indigo with warm golden lantern light. Hand-painted, gentle, no photo realism, "
    "no 3D render, no anime, no manga, no pixel art."
)
NOTEXT = "IMPORTANT: NO text, NO letters, NO numbers, NO signatures anywhere."
OBJ_STYLE = STYLE + (
    " Plain single objects drawn clearly and readably like inventory icons of a cozy game. "
    "IMPORTANT: NO faces, NO eyes, NO mouths on objects, NO people, NO text.")
GRID = ("on a flat solid pure magenta (#FF00FF) background. One item per cell, centered, filling most of the cell, "
        "with clear margin; nothing touches the cell borders; no text, no letters, no numbers, no grid lines, no shadows on the background.")


def sheet(what, cols, rows, items, style=OBJ_STYLE):
    lines = "\n".join(f"{i + 1}) {t}" for i, t in enumerate(items))
    return HEAD + f"Image: a sprite sheet of {len(items)} {what}.\n{style}\nLayout: {cols} columns x {rows} rows of equal square cells {GRID}\nItems, left to right, top to bottom:\n{lines}"


VISION = HEAD + ("Image: a SQUARE 1:1 round painting for a fairy tale, a 'vision seen in a crystal orb'. The painted scene fills a big "
                 "CIRCLE centred in the square and touching its edges; everything outside the circle is plain deep indigo-black. No border "
                 "ornament, no frame (a frame is added later).\n" + STYLE + "\n" + NOTEXT + "\nScene: ")

# The tales: same order as domain/Tales.kt. Each list is the 8 visions.
GIRL = "a little tea-picking girl in a simple rose-coloured robe with a straw basket on her back and two small hair buns"
BOATMAN = "an old grey-bearded boatman in a wide straw hat and a blue robe holding a paper lantern, a white red-crowned crane beside him"
BADGER = "a kind round badger walking upright in a little indigo jacket and a straw hat"
POTTER = "a young potter in a grey work robe with rolled sleeves, a cloth band on his forehead"
BOY = "a boy in a green robe with a topknot"
WEAVER = "a tall serene woman of the sky, the Cloud Weaver, in long flowing white-and-pale-blue robes"
SEER = "a kind old woman oracle with silver hair in a bun with a wooden pin, in a dark indigo robe"
TALES = {
    "t1": [
        "night over terraced tea hills and a small village: a bright golden star falls from the sky with a long golden trail into a lotus pond",
        f"dawn mist; {GIRL} kneels by the lotus pond and looks into the water, where a soft golden glow shines among the lotus leaves",
        f"{GIRL} holds in her palms a small pale star taken from the pond, dim and cold, water dripping from it",
        f"{GIRL} walks along a misty village street carrying the small star inside a paper lantern; villagers peek from round windows",
        f"an old potter by his glowing kiln spreads his hands in wonder while {GIRL} shows him the dim star in the lantern",
        f"dusk; {GIRL} climbs a long stone stairway up a steep mountain peak through the clouds, the lantern in her hands, first stars above",
        f"night on the mountain top: {GIRL} sits with closed eyes in deep darkness, and the star in her cupped palms glows brighter and brighter",
        f"the star rises back into the night sky from {GIRL}'s raised hands above a sea of clouds; a tiny golden spark shines in her eyes",
    ],
    "t2": [
        f"a wide misty river at night between steep mountains, a small wooden boat at the shore and {BOATMAN}",
        f"thick white fog all around; {BOATMAN.replace('holding a paper lantern, ', '')} stands in his boat and cannot see the oar in his own hands, he rubs his eyes",
        "on a misty river bank travellers wait for the boat: a woman with a baby, a peddler with a box on his back, a boy with a little goat, a lantern on a pole",
        "a white red-crowned crane flies high above a sea of white fog under a starry sky, wings wide, the moon behind",
        f"{BOATMAN} steers his boat through the fog with his eyes closed, listening; far away the crane calls from the other shore",
        f"the boat full of travellers glides calmly through the fog, a paper lantern glowing on its bow, {BOATMAN} at the oar",
        "dawn at the far shore: the travellers climb out of the boat and walk up a path to their village, the fog lifting, pink sky",
        f"{BOATMAN} and the crane rest by a small fire on the river bank at night under the moon",
    ],
    "t3": [
        f"{BADGER} tends rows of green tea bushes on a terraced mountain slope in spring; mist below, cherry blossom",
        f"{BADGER} stands on a terrace shading his eyes with his paw, watching rain clouds far away over the distant mountains",
        f"{BADGER} walks down a mountain path carrying bundles of tea leaves on a bamboo pole over his shoulder, autumn maples in red and gold",
        f"a lively mountain market with lanterns and stalls; {BADGER} trades his tea for a warm quilted blanket and a paper lantern",
        f"a snowstorm on the mountain; {BADGER} covers the tea bushes with straw mats, snowflakes swirling",
        f"{BADGER} sleeps curled up in a cozy burrow under the snow; above him a dream bubble shows green tea terraces in spring",
        f"spring: {BADGER} comes out of his burrow and squints, covering his eyes from the bright sun; snow melting, streams",
        f"{BADGER} smiles among the tea bushes covered with fresh bright green shoots, plum blossom around",
    ],
    "t4": [
        "a winter night in the mountains: the full moon catches on the top of an ancient pine and cracks; a few pale pieces fall",
        "a snowy mountain village in deep darkness; villagers with small lanterns stumble and look for their gates",
        f"{POTTER} kneels in the snow and finds a glowing pale piece of the moon",
        f"{POTTER} climbs a very tall old pine into the night sky with a small pot of liquid gold and a brush tied to his belt",
        f"{POTTER} sits on a cloud and mends the cracked moon, painting its cracks with shining gold like kintsugi; golden drops become stars",
        f"the full moon whole again in the sky with thin shining golden seams; {POTTER} rests on a cloud",
        "villagers come out of their houses onto a snowy street and look up happily at the mended moon",
        f"{POTTER} rests at home on a low mat by a brazier; his wife gently covers his eyes with her warm palms",
    ],
    "t5": [
        f"a tall wooden watchtower on a mountain ridge above a vast valley of forests; {BOY} stands at the top under a wide sky",
        f"{BOY} sits in the watchtower reading a scroll held very close to his face by an oil lamp",
        f"{BOY} looks up from his scroll at the horizon, squinting: the far mountains are painted blurry and hazy",
        f"{SEER} climbs up into the watchtower and points {BOY} far away to the horizon, teaching him",
        f"{BOY} on the watchtower watches a line of cranes and clouds far, far away over the distant mountains",
        f"{BOY} on the watchtower points excitedly at a thin wisp of smoke rising over the far forest",
        "villagers run with wooden buckets and put out a small fire at the edge of a pine forest; the boy watches from the tower",
        f"{BOY} receives a brass spyglass from the village elders, but he looks into the distance with his own eyes, the spyglass at his side",
    ],
    "t6": [
        f"high above the mountain peaks stands a little pavilion made of cloud; inside {WEAVER} weaves white clouds on a loom",
        "big snowflakes fall over a mountain village with curved roofs; children play in the snow, smoke from the chimneys",
        f"{WEAVER} sits tired at the stopped loom, rubbing her eyes; below, the mountains are grey and bare without snow",
        f"{SEER} flies up into the clouds on the back of a big white crane, a lantern in her hand",
        f"{SEER} and {WEAVER} stand together at a round moon window of the cloud pavilion and look at the far stars",
        f"{WEAVER} sits in the dark with her palms covering her closed eyes, resting; a lantern glows softly",
        f"{WEAVER} weaves again with a smile, and big fluffy snowflakes pour down from her loom over the mountains",
        f"{SEER} walks home along a snowy mountain path at night, and on her sleeve lies a single big beautiful star-shaped snowflake",
    ],
}

PEOPLE = HEAD + ("Image: a sprite sheet of 9 round portrait paintings, 3 columns x 3 rows of equal square cells. In each cell one ROUND "
                 "portrait (head and shoulders, three-quarter view, kind warm face) fills a circle centred in the cell; outside the circles "
                 "everything is plain deep indigo-black. No frames.\n" + STYLE + "\n" + NOTEXT + "\nPortraits, left to right, top to bottom:\n" +
                 "\n".join(f"{i + 1}) {t}" for i, t in enumerate([
                     f"{SEER}, wise and smiling, a small glowing crystal orb in her hands, a white crane behind her",
                     "a young goatherd boy with tousled hair and a bamboo flute, a little white goat beside him",
                     "a cheerful plump teahouse keeper woman with her hair up, holding a steaming clay teapot",
                     "a strong potter man with a short black beard, clay on his hands, holding a small bowl",
                     "a young bride with a red flower in her hair and a red silk shawl",
                     "an old fisherman with a long white beard, a wide straw hat and a fishing net over his shoulder",
                     "a merchant in a rich patterned robe and a small black cap, holding an abacus",
                     "an old weaver woman with a sly smile in a patterned shawl, holding a shuttle",
                     "a little girl with two braids holding a paper kite shaped like a fish",
                 ])))

ICONS = [
    "a small round moon window looking out at distant blue mountains and a crescent moon (look far away)",
    "a golden spark of light leaving a curling spiral trail (follow with the eyes)",
    "a paper lantern in front with a far golden star behind it (near and far)",
    "two small paper lanterns hanging apart from a branch (two lanterns)",
    "three clear dew drops falling from a bamboo leaf (dew, blinking)",
    "a glowing golden point of light with two thin golden rays converging on it from both sides (third eye)",
    "a ring of tiny golden lights around a dark round centre (side vision)",
    "two cupped hands held together holding a piece of the deep blue starry night (palming, darkness)",
    "an open hand making a graceful gesture with thumb and finger touching, a gold ring of light around it (mudra)",
    "a straight green bamboo stem with a few leaves (straight back)",
    "a round celadon bowl of calm water with one gentle ripple (breathing)",
    "a paper lantern just put out with a thin curl of smoke (put out the lantern)",
    "a crystal orb on a small dark wooden stand, softly glowing inside",
    "a golden sun rising over mountain peaks (daylight)",
    "a small bamboo basket of tea leaves and flowers (herbs)",
    "a rolled scroll with jade ends and a silk tie (the book of visions)",
]
HERBS = [
    "fresh green tea leaves on a short twig with a bud", "a sprig of white jasmine flowers", "a yellow chrysanthemum flower with leaves",
    "a sprig of tiny orange osmanthus flowers", "a branch of pink plum blossom", "a pink lotus flower with a seed pod",
    "a knobbly ginger root with a green shoot", "a sprig of fresh green mint", "a sprig of red goji berries",
    "a few long bamboo leaves", "a green pine twig with a small cone", "a red camellia flower with glossy leaves",
]
GIFTS = [
    "a small brass goat bell on a woven cord",
    "a round clay teapot with a bamboo handle",
    "a small tea bowl mended with shining gold seams",
    "a red silk folding fan with a painted plum branch",
    "a carved wooden fish painted with gold scales",
    "a little bronze coin charm on a red tasselled cord",
    "a rolled piece of patterned indigo silk tied with a ribbon",
    "a small paper kite shaped like a carp, colourful",
]
# The seer's gifts for walking the apprentice's path to the end: one per course, in this order.
MASTER = [
    "an ink brush with a bamboo handle resting on a small black inkstone carved with clouds",
    "a round jade pendant carved as a crescent moon on a green silk cord with a tassel",
    "a small round bronze mirror with a pine pattern on its back, on a red cord",
    "a little glass crystal orb glowing softly gold, on a carved dark wooden stand",
]
TABS = [
    "a small mountain pavilion with a curved roof and a glowing round window",
    "a glowing paper lantern",
    "a rolled scroll with jade ends",
    "a winding stone stairway going up a mountain with a little gate",
]

JOBS = {
    "hall": HEAD + ("Image: an illustration for an app, PORTRAIT 2:3, full-bleed, no frame, no border.\n" + STYLE + "\n" + NOTEXT +
                    "\nNO people, NO animals.\nScene: the quiet inside of a small wooden mountain monastery pavilion at night, seen straight from the front. "
                    "Upper part: a big round moon window showing misty mountain peaks, a sea of clouds and a crescent moon; paper lanterns hanging from "
                    "the beams, a scroll with a painted pine on the wall, a shelf with small tea jars. In the lower half stands a low dark wooden table, "
                    "and on the table in the CENTRE stands an EMPTY small carved wooden stand for a crystal orb with NOTHING on it, a little incense "
                    "holder and a tea cup beside. The area just above the stand (the middle of the picture) is calm, dark and simple "
                    "so a glass orb can be drawn there later."),
    "people": PEOPLE,
    "icons": sheet("small icons for a calm magic app", 4, 4, ICONS),
    "herbs": sheet("tea herbs, flowers and berries", 4, 3, HERBS),
    "gifts": sheet("small gifts from village folk", 4, 2, GIFTS),
    "master": sheet("precious gifts of an old mountain oracle to her apprentice", 4, 1, MASTER),
    "path_end": VISION + f"high on a mountain terrace at sunrise, {SEER} stands beside her young apprentice (seen from behind, a slim figure in a simple "
                         "grey-blue robe); both look out over a vast golden sea of clouds with distant peaks; a red-crowned crane flies past; warm, "
                         "quiet, the end of a long journey",
    "tabs": sheet("navigation icons for the bottom menu of a calm magic app, bold simple silhouettes that read well at small size", 4, 1, TABS),
    **{f"{t}_{i + 1}": VISION + s for t, vs in TALES.items() for i, s in enumerate(vs)},
}


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job, ref=None):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    cmd = [shutil.which("codex.cmd") or shutil.which("codex") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only"]
    if ref is not None and ref.exists():
        cmd += ["-i", str(ref)]
        prompt = prompt.replace(HEAD, HEAD + "The attached image is the style reference: match its painting style, line, palette and the look "
                                                "of the recurring characters exactly (not its scene).\n\n")
    r = None
    for _ in range(2):
        r = subprocess.run(cmd + ["-"], input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
        p = image_of((r.stdout or "") + (r.stderr or ""))
        if p is not None:
            shutil.copy(p, dst)
            print("ok", name, flush=True)
            return
    print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    args = sys.argv[1:]
    if args[:1] == ["--tales"]:
        firsts = [(f"{t}_1", JOBS[f"{t}_1"]) for t in TALES]
        style = OUT / "t1_1.png"
        with ThreadPoolExecutor(4) as ex:
            list(ex.map(lambda j: run(j, style if j[0] != "t1_1" else None), firsts))
        rest = [(f"{t}_{i}", JOBS[f"{t}_{i}"], OUT / f"{t}_1.png") for t in TALES for i in range(2, 9)]
        with ThreadPoolExecutor(4) as ex:
            list(ex.map(lambda j: run(j[:2], j[2]), rest))
        return
    ref = None
    if args[:1] == ["--ref"]:
        ref = OUT / f"{args[1]}.png"
        args = args[2:]
    jobs = [j for j in JOBS.items() if not args or j[0] in args]
    with ThreadPoolExecutor(4) as ex:
        list(ex.map(lambda j: run(j, ref), jobs))


if __name__ == "__main__":
    main()
