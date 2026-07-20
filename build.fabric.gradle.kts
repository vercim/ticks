plugins {
	java
	id("dev.kikugie.loom-back-compat")
}

group = "dev.vercim.ticks"
version = "${project.property("mod_version")}+${project.property("minecraft_version")}-fabric"
base.archivesName = "ticks"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(21)
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
	minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
	mappings(loom.layered {
		officialMojangMappings()
	})
	modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")

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
		"minecraft_version" to project.property("minecraft_version"),
		"fabric_loader_min_version" to project.property("fabric_loader_min_version")
	)
	inputs.properties(metadata)
	filesMatching("fabric.mod.json") {
		expand(metadata)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

tasks.test {
	useJUnitPlatform()
}
