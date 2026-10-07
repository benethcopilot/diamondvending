plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1-neoforge"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Applies version- and loader-specific tables from stonecutter.properties.toml
    properties {
        tags(version, loader)
    }

    // Enables //? if fabric / neoforge / forge in source comments
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        // Mojang renamed ResourceLocation to Identifier in 1.21.11; write `Identifier` in source.
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
