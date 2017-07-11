package com.blackwoodseven.kubernetes.volume_backup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.ResponseDeserializable
import mu.KotlinLogging
import java.net.InetAddress
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

data class AWSInstanceIdentity(
        val region: String
) {
    class Deserializer: ResponseDeserializable<AWSInstanceIdentity> {
        override fun deserialize(content: String): AWSInstanceIdentity {
            val mapper = jacksonObjectMapper()
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            return mapper.readValue<AWSInstanceIdentity>(content)
        }
    }
}

fun parseConfig(): Config {
    val backupInterval = try {
        val iso8601Duration = System.getenv("BACKUP_INTERVAL") ?: "PT1H"
        Duration.parse(iso8601Duration)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException(
                "The given BACKUP_INTERVAL does not conform to the ISO 8601 Duration format: " +
                        "https://en.wikipedia.org/wiki/ISO_8601#Durations", e
        )
    }

    return Config(
            System.getenv("AWS_ACCESS_KEY_ID"),
            System.getenv("AWS_SECRET_ACCESS_KEY"),
            System.getenv("AWS_DEFAULT_REGION") ?: resolveAWSRegion(),
            System.getenv("AWS_S3_BUCKET_NAME"),
            System.getenv("K8S_POD_NAME") ?: InetAddress.getLocalHost().getHostName(),
            System.getenv("K8S_NAMESPACE") ?: "default",
            System.getenv("K8S_CONTAINER_NAME") ?: "volume-backup",
            System.getenv("K8S_API_HOSTNAME") ?: "kubernetes.default",
            backupInterval
    )
}

fun resolveAWSRegion(): String {
    val url = "http://169.254.169.254/latest/dynamic/instance-identity/document"
    val req = Fuel.get(url)
    val (request, response, result) = req.responseObject(AWSInstanceIdentity.Deserializer())
    val (podDescription, error) = result
    if (error != null) {
        throw error
    }
    if (podDescription?.region == null) {
        throw Exception("AWS Region auto-detection failed, is this container running in AWS?")
    }
    return podDescription.region
}

fun performBackup(config: Config, volumesToBackup: Map<String, String>) {
    logger.info { "Performing backup" }
    for ((volumeName, path) in volumesToBackup) {
        val command = buildRcloneCommand(path, "s3:${config.awsS3BucketName}", config.namespace, volumeName)
        logger.info { "Executing command: $command" }
        performCommand(command)
    }
}

fun main(args : Array<String>) {
    logger.info { "Parsing Configuration" }
    val config = parseConfig()


    logger.info { "Initializing..."}
    setRcloneConfig(config.awsDefaultRegion)
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
