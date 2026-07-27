plugins {
	java
	id("net.neoforged.moddev")
	id("me.modmuss50.mod-publish-plugin")
}

group = "dev.vercim.ticks"
val minecraftVersion = stonecutter.current.version.substringBeforeLast('-')
val neoForgeVersion = when (minecraftVersion) {
	"1.21.1" -> project.property("neoforge_version") as String
	"1.21.4" -> project.property("neoforge_1_21_4_version") as String
	else -> error("No NeoForge version is configured for Minecraft $minecraftVersion")
}
val minecraftVersionUpperBound = minecraftVersion.split(".").let { parts ->
	"${parts[0]}.${parts[1]}.${parts[2].toInt() + 1}"
}
val releaseType = providers.gradleProperty("release_type").orElse("release").get()
val displayVersion = (project.property("mod_version") as String).substringBefore('-')

extra["ticksMinecraftVersion"] = minecraftVersion
extra["ticksPlatform"] = "neoforge"
apply(from = rootProject.file("gradle/preprocess-sources.gradle.kts"))

version = "${project.property("mod_version")}+$minecraftVersion-neoforge"
base.archivesName = "ticks"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(21)
	withSourcesJar()
}

neoForge {
	version = neoForgeVersion

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

sourceSets["main"].resources.srcDir(rootProject.file("src/neoforge/resources"))

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
	testImplementation("com.google.code.gson:gson:2.10.1")
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
		"neoforge_version" to neoForgeVersion,
		"neoforge_loader_version_range" to project.property("neoforge_loader_version_range"),
		"minecraft_version_upper_bound" to minecraftVersionUpperBound
	)
	inputs.properties(metadata)
	filesMatching("META-INF/neoforge.mods.toml") {
		expand(metadata)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

tasks.test {
	useJUnitPlatform()
}

publishMods {
	file.set(tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile })
	displayName.set("$displayVersion+$minecraftVersion")
	changelog.set(providers.environmentVariable("RELEASE_CHANGELOG").orElse("See the GitHub release notes."))
	type.set(when (releaseType) {
		"release" -> STABLE
		"beta" -> BETA
		"alpha" -> ALPHA
		else -> error("Unsupported release_type '$releaseType'. Use release, beta, or alpha.")
	})
	modLoaders.add("neoforge")

	curseforge {
		projectId.set(providers.gradleProperty("curseforge_project_id"))
		accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
		minecraftVersions.add(minecraftVersion)
		client.set(true)
		server.set(false)
	}

	modrinth {
		displayName.set("Ticks $displayVersion NeoForge")
		version.set("$displayVersion+$minecraftVersion")
		projectId.set(providers.gradleProperty("modrinth_project_id"))
		accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
		minecraftVersions.add(minecraftVersion)
	}
}
