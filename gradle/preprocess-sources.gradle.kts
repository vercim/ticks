import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.tasks.Jar

val minecraftVersion = extra["ticksMinecraftVersion"] as String
val platform = extra["ticksPlatform"] as String
val sourceDirectories = buildList {
	add(rootProject.file("src/main/java").toPath())
	add(rootProject.file("src/$platform/java").toPath())
}
val processedSourcesDirectory = layout.buildDirectory.dir("generated/sources/ticks/main")

fun compareVersions(first: String, second: String): Int {
	val firstParts = first.split('.').map(String::toInt)
	val secondParts = second.split('.').map(String::toInt)
	val partCount = maxOf(firstParts.size, secondParts.size)

	for (index in 0 until partCount) {
		val comparison = firstParts.getOrElse(index) { 0 }.compareTo(secondParts.getOrElse(index) { 0 })
		if (comparison != 0) {
			return comparison
		}
	}

	return 0
}

fun matchesCondition(condition: String): Boolean {
	return when {
		condition.startsWith(">=") -> compareVersions(minecraftVersion, condition.removePrefix(">=").trim()) >= 0
		condition.startsWith("<") -> compareVersions(minecraftVersion, condition.removePrefix("<").trim()) < 0
		else -> platform == condition
	}
}

fun preprocessSource(source: String): String {
	val activeConditions = ArrayDeque<Boolean>()
	val output = StringBuilder()

	for (line in source.lineSequence()) {
		val trimmed = line.trim()
		if (trimmed.startsWith("//?") && trimmed.endsWith("{") && !trimmed.contains("}")) {
			activeConditions.addLast(matchesCondition(trimmed.removePrefix("//?").removeSuffix("{").trim()))
			continue
		}
		if (trimmed == "//?}" || trimmed == "*///?}") {
			activeConditions.removeLast()
			continue
		}
		if (activeConditions.all { it }) {
			val processedLine = if (trimmed.startsWith("/*") && !trimmed.startsWith("/**")) {
				line.replaceFirst("/*", "")
			} else {
				line
			}
			output.append(processedLine).append('\n')
		}
	}

	return output.toString()
}

val preprocessMainSources = tasks.register("preprocessMainSources") {
	inputs.files(sourceDirectories)
	outputs.dir(processedSourcesDirectory)

	doLast {
		val outputDirectory = processedSourcesDirectory.get().asFile.toPath()
		project.delete(outputDirectory)

		for (sourceDirectory in sourceDirectories) {
			if (!Files.exists(sourceDirectory)) {
				continue
			}
			Files.walk(sourceDirectory).use { paths ->
				paths.filter(Files::isRegularFile).forEach { sourceFile ->
					val targetFile = outputDirectory.resolve(sourceDirectory.relativize(sourceFile))
					Files.createDirectories(targetFile.parent)
					Files.writeString(targetFile, preprocessSource(Files.readString(sourceFile)), StandardCharsets.UTF_8)
				}
			}
		}
	}
}

extensions.getByType<JavaPluginExtension>().sourceSets["main"].java.setSrcDirs(listOf(processedSourcesDirectory))
tasks.named("compileJava") {
	dependsOn(preprocessMainSources)
}
tasks.withType<Jar>().configureEach {
	if (name == "sourcesJar") {
		dependsOn(preprocessMainSources)
	}
}
