package uk.ac.dmu.koffeecraft.util.images

object ProductImageAssignments {

    private val keyByProductName: Map<String, String> = mapOf(
        "Espresso" to "coffee_espresso",
        "Cappuccino" to "coffee_cappuccino",
        "Latte" to "coffee_latte",
        "Flat White" to "coffee_flat_white",
        "Pistachio Latte" to "coffee_pistachio_latte",
        "Salted Caramel Mocha" to "coffee_salted_caramel_mocha",

        "Cheesecake" to "cake_cheesecake",
        "Chocolate Brownie" to "cake_chocolate_brownie",
        "Carrot Cake" to "cake_carrot_cake",
        "Tiramisu" to "cake_tiramisu",
        "Victoria Sponge Cake" to "cake_victoria_sponge_cake",
        "Lemon Drizzle Cake" to "cake_lemon_drizzle_cake",
        "Lotus Biscoff Cheesecake" to "cake_lotus_biscoff_cheesecake",
        "Red Velvet Slice" to "cake_red_velvet_slice",
        "Basque Cheesecake" to "cake_basque_cheesecake",

        "KoffeeCraft Mug" to "reward_mug",
        "KoffeeCraft Teddy Bear" to "reward_teddy",
        "1kg Crafted Coffee Beans" to "reward_beans_1kg"
    )

    fun imageKeyForProductName(name: String): String? {
        return keyByProductName[name.trim()]
    }
}