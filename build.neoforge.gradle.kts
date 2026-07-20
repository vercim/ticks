plugins {
	java
	id("net.neoforged.moddev")
}

group = "dev.vercim.ticks"
version = "${project.property("mod_version")}+${project.property("minecraft_version")}-neoforge"
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
}

dependencies {
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
		"minecraft_version" to project.property("minecraft_version"),
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
