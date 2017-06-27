package com.blackwoodseven.kubernetes.volume_backup

fun build_rclone_command(backupPath: String, target: String, namespace: String, pvcName: String): String {
    return "rclone sync \"$backupPath\" $target:$namespace/$pvcName"
}

fun performCommand(command: String) {
    val rcloneProcess = Runtime.getRuntime().exec(command)
    rcloneProcess.waitFor()

    val stdout = rcloneProcess.getInputStream().bufferedReader().use { it.readText() }
    val stderr = rcloneProcess.getErrorStream().bufferedReader().use { it.readText() }
}
