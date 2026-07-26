pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.7"
	id("dev.kikugie.loom-back-compat") version "0.4.1" apply false
	id("net.neoforged.moddev") version "2.0.142" apply false
}

stonecutter {
	create(rootProject) {
		version("1.20.1-fabric", "1.20.1").buildscript = "build.fabric.gradle.kts"
		version("1.20.1-forge", "1.20.1").buildscript = "build.forge.gradle.kts"
		version("1.21.1-fabric", "1.21.1").buildscript = "build.fabric.gradle.kts"
		version("1.21.1-neoforge", "1.21.1").buildscript = "build.neoforge.gradle.kts"
		vcsVersion = "1.21.1-fabric"
	}
}
