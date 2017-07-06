package com.blackwoodseven.kubernetes.volume_backup

fun buildRcloneCommand(backupPath: String, target: String, namespace: String, pvcName: String): String {
    return "rclone sync \"$backupPath\" $target:$namespace/$pvcName"
}

fun performCommand(command: String) {
    val rcloneProcess = Runtime.getRuntime().exec(command)
    rcloneProcess.waitFor()

    val stdout = rcloneProcess.inputStream.bufferedReader().use { it.readText() }
    val stderr = rcloneProcess.errorStream.bufferedReader().use { it.readText() }

    println("STDOUT From rclone:\n$stdout")
    println("STDERR From rclone:\n$stderr")
}
