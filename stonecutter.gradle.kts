@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
	id("dev.kikugie.stonecutter")
	id("net.fabricmc.fabric-loom-remap") version "1.16.3" apply false
}

val activeVersionFile = file(".sc_active_version")
if (activeVersionFile.isFile) {
	stonecutter active activeVersionFile
} else {
	stonecutter active "1.21.1-fabric"
}

stonecutter parameters {
	constants.match(current.project.substringAfterLast('-'), "fabric", "forge", "neoforge")
}

if (activeVersionFile.isFile) {
	tasks.register("runActiveClient") {
		group = "stonecutter"
		description = "Runs the client for the active Stonecutter target."
		dependsOn(stonecutter.current!!.project + ":runClient")
	}
}
