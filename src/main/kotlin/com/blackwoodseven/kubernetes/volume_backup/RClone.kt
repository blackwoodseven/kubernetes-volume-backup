package com.blackwoodseven.kubernetes.volume_backup

fun buildRcloneCommand(backupPath: String, target: String, namespace: String, pvcName: String): List<String> {
    return listOf("rclone", "sync", backupPath, "$target/$namespace/$pvcName")
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
