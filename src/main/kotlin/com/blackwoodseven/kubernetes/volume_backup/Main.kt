package com.blackwoodseven.kubernetes.volume_backup

import mu.KotlinLogging
import java.time.Duration
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

data class Config(
    val awsAccessKeyId: String,
    val awsSecretAccessKey: String,
    val awsDefaultRegion: String,
    val awsS3BucketName: String,
    val podName: String,
    val namespace: String,
    val backupContainerName: String,
    val kubernetesHostname: String,
    val backupInterval: Duration
)

private val logger = KotlinLogging.logger {}

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
            System.getenv("K8S_API_HOSTNAME"),
            backupInterval
    )
}

fun performBackup(config: Config, volumesToBackup: Map<String, String>) {
    logger.info { "Performing backup" }
    for ((volumeName, path) in volumesToBackup) {
        val command = buildRcloneCommand(path, "s3:${config.awsS3BucketName}", config.namespace, volumeName)
        logger.info { "Executing command: \"$command\"" }
        performCommand(command)
    }
}

fun main(args : Array<String>) {
    logger.info { "Parsing Configuration" }
    val config = parseConfig()

    logger.info { "Initializing..."}
    val token = getKubernetesToken()
    val podDescription = fetchPodDescription(config.podName, config.namespace, config.kubernetesHostname, token)
    if (podDescription == null) throw RuntimeException("Could not fetch pod description.")
    val volumesToBackup = matchClaimNameToMountPaths(config.backupContainerName, podDescription)

    logger.info { "Waiting for 1 minute before starting backups" }
    TimeUnit.MINUTES.sleep(1)

    while(true) {
        performBackup(config, volumesToBackup)

        logger.info { "Sleeping for ${config.backupInterval}" }
        TimeUnit.SECONDS.sleep(config.backupInterval.seconds)
    }
}
