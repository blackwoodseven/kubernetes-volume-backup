package com.blackwoodseven.kubernetes.volume_backup

import java.io.File

fun buildRcloneCommand(backupPath: String, target: String, namespace: String, pvcName: String, includes: List<String>? = null, excludes: List<String>? = null): List<String> {
    val includeParams = includes?.flatMap {listOf("--include", it)} ?: emptyList()
    val excludeParams = excludes?.flatMap {listOf("--exclude", it)} ?: emptyList()

    return listOf("rclone", "sync", backupPath, "$target/$namespace/$pvcName") + includeParams + excludeParams
}

fun performCommand(command: List<String>) {
    val rcloneProcess = Runtime.getRuntime().exec(command.toTypedArray())
    println(rcloneProcess)
    rcloneProcess.waitFor()

    val stdout = rcloneProcess.inputStream.bufferedReader().use { it.readText() }
    val stderr = rcloneProcess.errorStream.bufferedReader().use { it.readText() }

    println("STDOUT From rclone:\n$stdout")
    println("STDERR From rclone:\n$stderr")
}

fun setRcloneConfig(region: String) {
    val rcloneConfig = """
                       [s3]
                       type = s3
                       env_auth = true
                       region = $region
                       """.trimIndent()

    File("/root/.config/rclone").mkdirs()
    File("/root/.config/rclone/rclone.conf").writeText(rcloneConfig)
}
