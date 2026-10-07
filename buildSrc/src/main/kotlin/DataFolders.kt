import org.gradle.api.GradleException

/**
 * Minecraft 1.20.1 names these data folders in the plural; the shared files use the newer names, and the Forge build
 * renames them as it copies them. Our own folders keep their names. Any other folder fails the build, so a file is never
 * shipped where 1.20.1 won't look for it.
 */
object DataFolders {
    private val plural = mapOf(
        "tags/item" to "tags/items",
        "tags/block" to "tags/blocks",
        "recipe" to "recipes",
        "advancement" to "advancements",
        "structure" to "structures",
        "loot_table" to "loot_tables",
    )
    private val own = listOf("diamondvending/catalog")

    /** Where a resource at [path] (e.g. `data/diamondvending/tags/item/currency.json`) goes in a 1.20.1 jar. */
    fun for1201(path: String): String {
        val parts = path.split("/", limit = 3)
        if (parts.size < 3 || parts[0] != "data") return path
        val (_, namespace, rest) = parts
        for ((newer, older) in plural) {
            if (rest.startsWith("$newer/")) return "data/$namespace/$older/${rest.removePrefix("$newer/")}"
        }
        if ((plural.values + own).any { rest.startsWith("$it/") }) return path
        throw GradleException("$path: no Minecraft 1.20.1 folder is known for it; add one to buildSrc/src/main/kotlin/DataFolders.kt")
    }
}
