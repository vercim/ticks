plugins {
	java
	id("net.neoforged.moddev")
}

group = "dev.skuto.smoothtime"
version = "0.1.0-alpha.1+1.21.1-neoforge"
base.archivesName = "smooth-time"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(21)
	withSourcesJar()
}

neoForge {
	version = "21.1.213"

	runs {
		register("client") {
			client()
			gameDirectory = file("run")
			programArgument("--username=Dev")
		}
	}

	mods {
		register("smooth_time") {
			sourceSet(sourceSets["main"])
		}
	}
}

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
	inputs.property("version", project.version)
	filesMatching("META-INF/neoforge.mods.toml") {
		expand("version" to project.version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

tasks.test {
	useJUnitPlatform()
}
