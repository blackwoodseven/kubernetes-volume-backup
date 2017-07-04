package com.blackwoodseven.kubernetes.volume_backup

import java.time.Duration
import java.time.format.DateTimeParseException

data class Config(
    val awsAccessKeyId: String,
    val awsSecretAccessKey: String,
    val awsDefaultRegion: String,
    val awsS3BucketName: String,
    val podName: String,
    val namespace: String,
    val backupContainerName: String,
    val backupInterval: Duration
)

fun parseConfig(): Config {
    val backupInterval = try {
        Duration.parse(System.getenv("BACKUP_INTERVAL"))
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException(
                "The given BACKUP_INTERVAL does not conform to the ISO 8601 Duration format: " +
                        "https://en.wikipedia.org/wiki/ISO_8601#Durations", e
        )
    }

    return Config(
            System.getenv("AWS_ACCESS_KEY_ID"),
            System.getenv("AWS_SECRET_ACCESS_KEY"),
            System.getenv("AWS_DEFAULT_REGION"),
            System.getenv("AWS_S3_BUCKET_NAME"),
            System.getenv("K8S_POD_NAME"),
            System.getenv("K8S_NAMESPACE"),
            System.getenv("K8S_CONTAINER_NAME"),
            backupInterval
    )
}

fun main(args : Array<String>) {
    val config = parseConfig()
    while(true) {
//        sleep()
    }
}
