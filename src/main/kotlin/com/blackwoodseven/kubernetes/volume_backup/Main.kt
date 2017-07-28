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
        val awsAccessKeyId: String?,
        val awsSecretAccessKey: String?,
        val awsDefaultRegion: String,
        val awsS3BucketName: String,
        val podName: String,
        val namespace: String,
        val backupContainerName: String,
        val kubernetesHostname: String,
        val backupInterval: Duration,
        val forcedPaths: Map<String, String>?
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
    val rawAwsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID")
    val rawAwsSecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY")
    val rawAwsDefaultRegion = System.getenv("AWS_DEFAULT_REGION")
    val rawAwsS3BucketName = System.getenv("AWS_S3_BUCKET_NAME")
    val rawK8sPodName = System.getenv("K8S_POD_NAME")
    val rawK8sNamespace = System.getenv("K8S_NAMESPACE")
    val rawK8sContainerName = System.getenv("K8S_CONTAINER_NAME")
    val rawK8sApiHostname = System.getenv("K8S_API_HOSTNAME")
    val rawBackupInterval = System.getenv("BACKUP_INTERVAL")
    val rawForcedPaths = System.getenv("FORCED_PATHS")

    if (rawAwsAccessKeyId == null || rawAwsSecretAccessKey == null)
        logger.info { "No AWS Access Key specified, assuming permissions are applied by IAM Role." }
    if (rawAwsS3BucketName == null)
        throw IllegalArgumentException("No S3 bucket specified")

    val awsDefaultRegion = rawAwsDefaultRegion ?: {
        logger.info { "No AWS Default Region specified, auto-detecting..."}
        resolveAWSRegion()
    }()
    val k8sPodName = rawK8sPodName ?: {
        val hostname = InetAddress.getLocalHost().hostName
        logger.info { "No Pod Name given, defaulting to hostname \"$hostname\""}
        hostname
    }()
    val k8sNamespace = rawK8sNamespace ?: {
        logger.info { "No Namespace specified, defaulting to \"default\"" }
        "default"
    }()
    val backupContainerName = rawK8sContainerName ?: {
        logger.info { "No backup container name specified, defaulting to \"volume-backup\""}
        "volume-backup"
    }()
    val k8sApiHostname = rawK8sApiHostname ?: {
        logger.info { "No Kubernetes API Hostname specified, defaulting to \"kubernetes.default\"" }
        "kubernetes.default"
    }()

    val backupInterval = try {
        val iso8601Duration = rawBackupInterval ?: {
            logger.info { "No backup interval defined, defaulting to PT1H" }
            "PT1H"
        }()
        Duration.parse(iso8601Duration)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException(
                "The given BACKUP_INTERVAL does not conform to the ISO 8601 Duration format: " +
                        "https://en.wikipedia.org/wiki/ISO_8601#Durations", e
        )
    }

    val forcedPaths = if (rawForcedPaths == null) {
        null
    } else {
        try {
            val mapper = jacksonObjectMapper()
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            mapper.readValue<Map<String, String>>(rawForcedPaths)
        } catch (e: IllegalStateException) {
            throw IllegalArgumentException(
                    "The given FORCED_BACKUP_PATHS could not be parsed, it must be a json object string, containing only strings", e
            )
        }
    }

    return Config(
            rawAwsAccessKeyId,
            rawAwsSecretAccessKey,
            awsDefaultRegion,
            rawAwsS3BucketName,
            k8sPodName,
            k8sNamespace,
            backupContainerName,
            k8sApiHostname,
            backupInterval,
            forcedPaths
    )
}

fun resolveAWSRegion(): String {
    val url = "http://169.254.169.254/latest/dynamic/instance-identity/document"
    val req = Fuel.get(url)
    val (request, response, result) = req.responseObject(AWSInstanceIdentity.Deserializer())
    val (podDescription, error) = result
    if (error != null) {
        throw Exception("AWS Region auto-detection failed, is this container running in AWS?", error)
    }
    if (podDescription?.region == null) {
        throw Exception("AWS Region auto-detection failed, is this container running in AWS?")
    }
    return podDescription.region
}

fun logConfig(config: Config) {
    logger.info("""
            Config Contains:
            awsAccessKeyId: ${config.awsAccessKeyId}
            awsSecretAccessKey: [hidden]
            awsDefaultRegion: ${config.awsDefaultRegion}
            awsS3BucketName: ${config.awsS3BucketName}
            podName: ${config.podName}
            namespace: ${config.namespace}
            backupContainerName: ${config.backupContainerName}
            kubernetesHostName: ${config.kubernetesHostname}
            backupInterval: ${config.backupInterval}
            forcedPaths: ${config.forcedPaths}
    """.trimIndent())
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
    logConfig(config)

    logger.info { "Initializing..." }
    setRcloneConfig(config.awsDefaultRegion)
    val token = getKubernetesToken()
    val podDescription = fetchPodDescription(config.podName, config.namespace, config.kubernetesHostname, token) ?:
            throw RuntimeException("Could not fetch pod description.")
    val volumesToBackup = matchClaimNameToMountPaths(config.backupContainerName, podDescription)
    val allVolumesToBackup = (volumesToBackup.toList() + (config.forcedPaths?.toList() ?: emptyList())).toMap()

    logger.info { "Waiting for 1 minute before starting backups" }
    TimeUnit.MINUTES.sleep(1)

    while(true) {
        performBackup(config, allVolumesToBackup)

        logger.info { "Sleeping for ${config.backupInterval}" }
        TimeUnit.SECONDS.sleep(config.backupInterval.seconds)
    }
}
