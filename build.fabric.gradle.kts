plugins {
	java
	id("dev.kikugie.loom-back-compat")
	id("me.modmuss50.mod-publish-plugin")
}

val minecraftVersion = stonecutter.current.version.substringBeforeLast('-')
val isLegacy = minecraftVersion == "1.20.1"
val javaVersion = if (isLegacy) 17 else 21
val fabricLoaderVersion = if (isLegacy) "0.15.11" else project.property("fabric_loader_version") as String
val fabricLoaderMinVersion = if (isLegacy) "0.14.21" else project.property("fabric_loader_min_version") as String
val clothConfigVersion = if (isLegacy) {
	project.property("cloth_config_legacy_version") as String
} else {
	project.property("cloth_config_current_version") as String
}
val modMenuVersion = if (isLegacy) {
	project.property("modmenu_legacy_version") as String
} else {
	project.property("modmenu_current_version") as String
}
val releaseType = providers.gradleProperty("release_type").orElse("release").get()

group = "dev.vercim.ticks"
version = "${project.property("mod_version")}+$minecraftVersion-fabric"
base.archivesName = "ticks"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
	withSourcesJar()
}

repositories {
	mavenCentral()
	maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
	maven("https://maven.terraformersmc.com/releases/") { name = "Terraformers" }
}

sourceSets["main"].java.srcDir(rootProject.file("src/fabric/java"))
sourceSets["main"].resources.srcDir(rootProject.file("src/fabric/resources"))

loom {
	runs.named("client") {
		client()
		runDir = "run"
		environment = "client"
		programArgs("--username=Dev")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings(loom.layered {
		officialMojangMappings()
	})
	modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
	modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
	modCompileOnly("com.terraformersmc:modmenu:$modMenuVersion")

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
		"mod_sources" to project.property("mod_sources"),
		"mod_issues" to project.property("mod_issues"),
		"minecraft_version" to minecraftVersion,
		"fabric_loader_min_version" to fabricLoaderMinVersion,
		"java_version" to javaVersion
	)
	inputs.properties(metadata)
	filesMatching("fabric.mod.json") {
		expand(metadata)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = javaVersion
}

tasks.test {
	useJUnitPlatform()
}

publishMods {
	file.set(tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
	changelog.set(providers.environmentVariable("RELEASE_CHANGELOG").orElse("See the GitHub release notes."))
	type.set(when (releaseType) {
		"release" -> STABLE
		"beta" -> BETA
		"alpha" -> ALPHA
		else -> error("Unsupported release_type '$releaseType'. Use release, beta, or alpha.")
	})
	modLoaders.add("fabric")

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
