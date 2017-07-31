package com.blackwoodseven.kubernetes.volume_backup

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals

class RcloneSpec : Spek ({
    describe("Rclone") {
        describe("buildRcloneCommand") {
            it("should build the correct command") {
                val backupPath = "/volumes/very-important-directory"
                val target = "s3:some-bucket"
                val namespace = "default"
                val pvcName = "very-important-files-volume"

                val command = buildRcloneCommand(backupPath, target, namespace, pvcName)

                assertEquals(listOf("rclone", "sync", backupPath, "$target/$namespace/$pvcName"), command)
            }

            it("should handle paths with spaces") {
                val backupPath = "/volumes/directory with spaces"
                val target = "s3:some-bucket"
                val namespace = "default"
                val pvcName = "very-important-files-volume"

                val command = buildRcloneCommand(backupPath, target, namespace, pvcName)

                assertEquals(listOf("rclone", "sync", backupPath, "$target/$namespace/$pvcName"), command)
            }

            it("should handle includes") {
                val backupPath = "/volumes/very-important-directory"
                val target = "s3:some-bucket"
                val namespace = "default"
                val pvcName = "very-important-files-volume"
                val includes = listOf("*.csv", "*.xlsx")

                val command = buildRcloneCommand(backupPath, target, namespace, pvcName, includes)

                assertEquals(listOf("rclone", "sync", backupPath, "$target/$namespace/$pvcName", "--include", "*.csv", "--include", "*.xlsx"), command)
            }

            it("should handle excludes") {
                val backupPath = "/volumes/very-important-directory"
                val target = "s3:some-bucket"
                val namespace = "default"
                val pvcName = "very-important-files-volume"
                val excludes = listOf("*.csv", "*.xlsx")

                val command = buildRcloneCommand(backupPath, target, namespace, pvcName, excludes = excludes)

                assertEquals(listOf("rclone", "sync", backupPath, "$target/$namespace/$pvcName", "--exclude", "*.csv", "--exclude", "*.xlsx"), command)
            }

            it("should handle includes and excludes at the same time") {
                val backupPath = "/volumes/very-important-directory"
                val target = "s3:some-bucket"
                val namespace = "default"
                val pvcName = "very-important-files-volume"
                val includes = listOf("*.txt", "*.jpg")
                val excludes = listOf("*.csv", "*.xlsx")

                val command = buildRcloneCommand(backupPath, target, namespace, pvcName, includes, excludes)

                assertEquals(listOf("rclone", "sync", backupPath, "$target/$namespace/$pvcName", "--include", "*.txt", "--include", "*.jpg", "--exclude", "*.csv", "--exclude", "*.xlsx"), command)
            }
        }
    }
})
