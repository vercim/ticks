plugins {
	java
	id("net.neoforged.moddev.legacyforge")
}

val minecraftVersion = stonecutter.current.version.substringBeforeLast('-')
val javaVersion = 17

group = "dev.vercim.ticks"
version = "${project.property("mod_version")}+$minecraftVersion-forge"
base.archivesName = "ticks"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
	withSourcesJar()
}

legacyForge {
	version = project.property("forge_version") as String

	runs {
		register("client") {
			client()
			gameDirectory = file("run")
			programArgument("--username=Dev")
		}
	}

	mods {
		register("ticks") {
			sourceSet(sourceSets["main"])
		}
	}
}

mixin {
	add(sourceSets["main"], "ticks.forge.refmap.json")
	config("ticks.forge.mixins.json")
}

sourceSets["main"].resources.srcDir(rootProject.file("src/forge/resources"))

repositories {
	mavenCentral()
}

dependencies {
	annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
	val metadata = mapOf(
		"version" to project.version,
		"mod_id" to project.property("mod_id"),
		"mod_name" to project.property("mod_name"),
		"mod_authors" to project.property("mod_authors"),
		"mod_license" to project.property("mod_license"),
		"mod_description" to project.property("mod_description"),
		"mod_homepage" to project.property("mod_homepage"),
		"mod_issues" to project.property("mod_issues"),
		"minecraft_version" to minecraftVersion,
		"forge_version" to project.property("forge_version"),
		"forge_loader_version_range" to project.property("forge_loader_version_range")
	)
	inputs.properties(metadata)
	filesMatching("META-INF/mods.toml") {
		expand(metadata)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = javaVersion
}

tasks.jar {
	manifest.attributes["MixinConfigs"] = "ticks.forge.mixins.json"
}

tasks.test {
	useJUnitPlatform()
}
