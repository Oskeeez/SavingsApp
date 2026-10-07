package com.example.coolingoffjar.domain

enum class ShelfCategory(val label: String) {
    PLANTS("Plants"),
    DECOR("Decor"),
    PETS("Pets"),
    LIGHTING("Lighting"),
    ART("Art"),
    NOTES("Notes"),
}

/** Where a thing goes: standing on the bookcase, or pinned to the wall above it. */
enum class ShelfSurface { SHELF, WALL }

/**
 * Something you can put on your shelf. [artKey] names the illustration (see `ShelfArt` in the UI);
 * [artWidthPx] x [artHeightPx] is that illustration's size, used only for its aspect ratio.
 * Cost is in coins: one coin for every "Not buying" you have made.
 */
data class CatalogItem(
    val id: String,
    val name: String,
    val category: ShelfCategory,
    val cost: Int,
    val description: String,
    val artKey: String,
    val artWidthPx: Int,
    val artHeightPx: Int,
    val surface: ShelfSurface = ShelfSurface.SHELF,
) {
    val aspect: Float get() = artWidthPx.toFloat() / artHeightPx
}

object ShelfCatalog {
    private fun item(
        art: String, name: String, category: ShelfCategory, cost: Int, w: Int, h: Int, description: String,
        surface: ShelfSurface = ShelfSurface.SHELF,
    ) = CatalogItem(id = art, name = name, category = category, cost = cost, description = description, artKey = art, artWidthPx = w, artHeightPx = h, surface = surface)

    val items: List<CatalogItem> = listOf(
        // Plants (always in pots)
        item("anthurium", "Anthurium clarinervium", ShelfCategory.PLANTS, 5, 194, 213, "A striking, velvety plant with heart-shaped leaves and bright, delicate veins. Brings a calm, natural feeling to your shelf."),
        item("monstera", "Monstera adansonii", ShelfCategory.PLANTS, 5, 198, 209, "Lacy, hole-filled leaves that look a little like a green window. Happy to sit quietly and grow."),
        item("pothos_potted", "Pothos plant", ShelfCategory.PLANTS, 4, 168, 174, "Glossy, forgiving, and always pleased to see you. A good first plant."),
        item("pothos_trailing", "Trailing pothos", ShelfCategory.PLANTS, 5, 178, 237, "Long green vines spilling over the pot, one patient leaf at a time."),
        item("ivy_hanging", "English ivy", ShelfCategory.PLANTS, 4, 186, 199, "Soft little leaves that curl and trail. Plants bring a little more life to good decisions."),
        item("bonsai", "Little bonsai", ShelfCategory.PLANTS, 6, 140, 152, "A tiny tree that rewards slow, careful attention."),
        item("flower_vase", "Flowers in a vase", ShelfCategory.PLANTS, 3, 127, 206, "A few small white blossoms in a glass bottle. Simple and cheerful."),
        // Decor
        item("books_stack", "Book stack", ShelfCategory.DECOR, 3, 209, 129, "Slow Days, Brighter Days, A Kinder You. Three good books to rest a lamp on."),
        item("books_standing", "Row of books", ShelfCategory.DECOR, 4, 187, 163, "A leaning row of well-loved spines in sage, blue and rust."),
        item("wooden_house", "Wooden house", ShelfCategory.DECOR, 3, 132, 141, "A tiny cream house with a wooden roof. Someone is always home."),
        item("world_globe", "World globe", ShelfCategory.DECOR, 5, 159, 197, "Spin it slowly and pick somewhere to go one day."),
        item("snow_globe", "Snow globe", ShelfCategory.DECOR, 5, 148, 170, "A cosy cabin in a quiet, snowy forest. Give it a shake when you need a pause."),
        item("film_camera", "Film camera", ShelfCategory.DECOR, 6, 207, 135, "A little retro camera for remembering how things felt."),
        item("reed_diffuser", "Reed diffuser", ShelfCategory.DECOR, 4, 93, 199, "Amber glass and slim reeds. The shelf smells like a good evening."),
        item("storage_box_green", "Storage box", ShelfCategory.DECOR, 4, 184, 121, "A sage-green box with a brass label holder, for keeping small things safe."),
        item("storage_boxes_cream", "Paper boxes", ShelfCategory.DECOR, 4, 178, 119, "Two cream boxes, neatly stacked, with little brass pulls."),
        item("basket", "Woven basket", ShelfCategory.DECOR, 3, 189, 123, "Hand-woven and warm. Holds anything that needs a home."),
        item("ceramic_vases", "Ceramic vases", ShelfCategory.DECOR, 4, 159, 153, "A speckled cream bottle and a sage vase with a single sprig."),
        item("stacked_stones", "Balanced stones", ShelfCategory.DECOR, 3, 126, 108, "Three smooth stones, balanced just so. A reminder to go slowly."),
        item("wooden_tray", "Wooden tray", ShelfCategory.DECOR, 2, 189, 68, "A shallow tray for keys, coins and bits and bobs."),
        item("mini_mountains", "Little mountains", ShelfCategory.DECOR, 3, 184, 111, "Five snow-capped peaks, small enough to hold."),
        item("pen_cup", "Pen cup", ShelfCategory.DECOR, 2, 94, 140, "A wooden cup with a sprout on it, full of pens, pencils and scissors."),
        // Pets
        item("cat_calico", "Calico cat", ShelfCategory.PETS, 6, 203, 114, "A sleepy calico loaf. Asks for nothing and is excellent company."),
        item("rabbit", "Little rabbit", ShelfCategory.PETS, 6, 144, 188, "A soft white rabbit with pink ears and a very calm outlook."),
        item("bird_figure", "Ceramic bird", ShelfCategory.PETS, 3, 130, 101, "A plump little bird, glazed cream, perched and content."),
        // Lighting
        item("mushroom_lamp", "Cozy lamp", ShelfCategory.LIGHTING, 4, 146, 182, "A warm, glowing mushroom of a lamp on a little wooden base."),
        item("moon_lamp", "Moon light", ShelfCategory.LIGHTING, 5, 114, 143, "A golden crescent on a wooden stand, for a gentle goodnight."),
        // Art
        item("framed_landscape", "Framed print", ShelfCategory.ART, 4, 168, 174, "Rolling hills and a quiet river, in a warm wooden frame."),
        item("framed_flowers", "Flower print", ShelfCategory.ART, 4, 133, 148, "A small botanical print of white blossoms."),
        item("framed_night", "Night sky print", ShelfCategory.ART, 4, 130, 145, "A crescent moon over a sleepy field."),
        // Notes: pinned to the wall above the shelf
        item("note_take_your_time", "Take your time", ShelfCategory.NOTES, 2, 449, 479, "A little green-taped note to read whenever you feel rushed.", ShelfSurface.WALL),
        item("note_small_progress", "Small progress", ShelfCategory.NOTES, 2, 507, 382, "Small progress still counts. Every wait is one.", ShelfSurface.WALL),
        item("note_good_days", "Good days ahead", ShelfCategory.NOTES, 3, 421, 472, "A snapshot of mountains and sunshine, pinned up for later.", ShelfSurface.WALL),
        item("note_doing_great", "You're doing great", ShelfCategory.NOTES, 3, 572, 435, "A page torn from a notebook, with a very sleepy cat.", ShelfSurface.WALL),
        item("note_kinder_you", "A kinder you", ShelfCategory.NOTES, 3, 435, 480, "A scalloped sage tag with a little ribbon: a kinder you, every day.", ShelfSurface.WALL),
    )

    private val byId = items.associateBy { it.id }
    fun find(id: String): CatalogItem? = byId[id]
}
