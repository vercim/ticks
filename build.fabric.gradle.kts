plugins {
	java
	id("dev.kikugie.loom-back-compat")
}

group = "dev.skuto.smoothtime"
version = "0.1.0-alpha.1+1.21.1-fabric"
base.archivesName = "smooth-time"

java {
	toolchain.languageVersion = JavaLanguageVersion.of(21)
	withSourcesJar()
}

repositories {
	mavenCentral()
}

sourceSets["main"].resources.srcDir("src/fabric/resources")

loom {
	runs.named("client") {
		client()
		runDir = "run"
		environment = "client"
		programArgs("--username=Dev")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:1.21.1")
	mappings(loom.layered {
		officialMojangMappings()
	})
	modImplementation("net.fabricmc:fabric-loader:0.16.10")

	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
	inputs.property("version", project.version)
	filesMatching("fabric.mod.json") {
		expand("version" to project.version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

tasks.test {
	useJUnitPlatform()
}
