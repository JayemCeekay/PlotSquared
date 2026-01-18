rootProject.name = "PlotSquared"

pluginManagement {
    repositories {
        maven {
            name = "FabricRepo"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.toString() == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
        }
    }
}


include("Core", "Fabric")

project(":Core").name = "plotsquared-core"
//project(":Bukkit").name = "plotsquared-bukkit"
project(":Fabric").name = "plotsquared-fabric"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
