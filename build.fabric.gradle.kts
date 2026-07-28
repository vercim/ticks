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
val releaseType = providers.gradleProperty("release_type").orElse("release").get()
val displayVersion = (project.property("mod_version") as String).substringBefore('-')

extra["ticksMinecraftVersion"] = minecraftVersion
extra["ticksPlatform"] = "fabric"
apply(from = rootProject.file("gradle/preprocess-sources.gradle.kts"))

group = "dev.vercim.ticks"
version = "${project.property("mod_version")}+$minecraftVersion-fabric"
base.archivesName = "ticks"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
	withSourcesJar()
}

repositories {
	mavenCentral()
}

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

tasks.register<Copy>("buildAndCollect") {
	group = "build"
	description = "Builds this target and copies its distributable JAR to the root collection directory."
	from(tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
	into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod_version")}"))
	dependsOn("build")
}

publishMods {
	file.set(tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
	displayName.set("$displayVersion+$minecraftVersion")
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
		displayName.set("Ticks $displayVersion Fabric")
		version.set("$displayVersion+$minecraftVersion")
		projectId.set(providers.gradleProperty("modrinth_project_id"))
		accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
		minecraftVersions.add(minecraftVersion)
	}
}
