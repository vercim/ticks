plugins {
	java
	id("net.neoforged.moddev")
	id("me.modmuss50.mod-publish-plugin")
}

group = "dev.vercim.ticks"
val minecraftVersion = stonecutter.current.version.substringBeforeLast('-')
val clothConfigVersion = project.property("cloth_config_current_version") as String
val releaseType = providers.gradleProperty("release_type").orElse("release").get()
version = "${project.property("mod_version")}+$minecraftVersion-neoforge"
base.archivesName = "ticks"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(21)
	withSourcesJar()
}

neoForge {
	version = project.property("neoforge_version") as String

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
	maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
}

dependencies {
	compileOnly("me.shedaniel.cloth:cloth-config-neoforge:$clothConfigVersion")
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
		"neoforge_version" to project.property("neoforge_version"),
		"neoforge_loader_version_range" to project.property("neoforge_loader_version_range")
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
		projectId.set("ticks")
		accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
		minecraftVersions.add(minecraftVersion)
	}
}
