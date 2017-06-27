package com.blackwoodseven.kubernetes.volume_backup

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals

class RcloneTest : Spek ({
    describe("Rclone") {
        describe("buildRcloneCommand") {
            it("should build the correct command") {
                val backupPath = "/volumes/very-important-directory"
                val target = "s3"
                val namespace = "default"
                val pvcName = "very-important-files-volume"

                val command = build_rclone_command(backupPath=backupPath,
                        target=target, namespace=namespace, pvcName=pvcName)

                assertEquals("rclone sync \"$backupPath\" $target:$namespace/$pvcName", command)
            }

            it("should handle paths with spaces") {
                val backupPath = "/volumes/directory with spaces"
                val target = "s3"
                val namespace = "default"
                val pvcName = "very-important-files-volume"

                val command = build_rclone_command(backupPath=backupPath,
                        target=target, namespace=namespace, pvcName=pvcName)

                assertEquals("rclone sync \"$backupPath\" $target:$namespace/$pvcName", command)
            }
        }
    }
})
