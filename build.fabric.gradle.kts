plugins {
	java
	id("dev.kikugie.loom-back-compat")
}

val minecraftVersion = stonecutter.current.version.substringBeforeLast('-')
val isLegacy = minecraftVersion == "1.20.1"
val javaVersion = if (isLegacy) 17 else 21
val fabricLoaderVersion = if (isLegacy) "0.15.11" else project.property("fabric_loader_version") as String
val fabricLoaderMinVersion = if (isLegacy) "0.14.21" else project.property("fabric_loader_min_version") as String

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
