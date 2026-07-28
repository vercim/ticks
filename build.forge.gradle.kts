plugins {
	java
	id("net.neoforged.moddev.legacyforge")
	id("me.modmuss50.mod-publish-plugin")
}

val minecraftVersion = stonecutter.current.version.substringBeforeLast('-')
val javaVersion = 17
val releaseType = providers.gradleProperty("release_type").orElse("release").get()
val displayVersion = (project.property("mod_version") as String).substringBefore('-')

extra["ticksMinecraftVersion"] = minecraftVersion
extra["ticksPlatform"] = "forge"
apply(from = rootProject.file("gradle/preprocess-sources.gradle.kts"))

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

tasks.register<Copy>("buildAndCollect") {
	group = "build"
	description = "Builds this target and copies its distributable JAR to the root collection directory."
	from(tasks.named<org.gradle.api.tasks.bundling.Jar>("jar").flatMap { it.archiveFile })
	into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod_version")}"))
	dependsOn("build")
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
	modLoaders.add("forge")

	curseforge {
		projectId.set(providers.gradleProperty("curseforge_project_id"))
		accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
		minecraftVersions.add(minecraftVersion)
		client.set(true)
		server.set(false)
	}

	modrinth {
		displayName.set("Ticks $displayVersion Forge")
		version.set("$displayVersion+$minecraftVersion")
		projectId.set(providers.gradleProperty("modrinth_project_id"))
		accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
		minecraftVersions.add(minecraftVersion)
	}
}
