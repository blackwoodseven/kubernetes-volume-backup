package com.blackwoodseven.kubernetes.volume_backup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.Fuel
import java.io.File

data class volumeMount(
        val name: String,
        val mountPath: String,
        val readOnly: Boolean
)

data class Container(
        val name: String,
        val volumeMounts: List<volumeMount>

)

data class PersistentVolumeClaim(
        val claimName: String
)

data class Volume(
        val name: String,
        val persistentVolumeClaim: PersistentVolumeClaim?
)

data class PodSpec(
        val volumes: List<Volume>,
        val containers: List<Container>
)

data class PodDescription(
        val spec: PodSpec
)

fun GetVolumeNames(podName: String, namespace: String): String {
    fetchPodDescription(podName, namespace)
    return "hej"
}

fun findVolumeNames(containerName: String, podDescription: String): Map<String, String> {
    val mapper = jacksonObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val parsedPodDescription = mapper.readValue<PodDescription>(podDescription)

    val spec = parsedPodDescription.spec

    // Extract this container's volume mounts
    val volumeMounts = spec.containers.first { it.name == containerName }.volumeMounts

    // Join volume mounts with their volumes
    val volumeMountsToVolumes = volumeMounts.associateBy(
            {volumeMount -> volumeMount},
            {volumeMount ->
                spec.volumes.first {
                    volume -> volumeMount.name == volume.name
                }
            }
    )

    // Pick only volumes which are PersistentVolumeClaims
    val filteredMountsToVolumes = volumeMountsToVolumes.filter {
        (_, volume) -> volume.persistentVolumeClaim != null
    }

    // Map VolumeMount:Volume to mountPath:claimName
    val mountPathToClaimName = filteredMountsToVolumes.map {
        (volumeMount, volume) -> volumeMount.mountPath to volume.persistentVolumeClaim!!.claimName
    }.toMap()

    return mountPathToClaimName
}

fun fetchPodDescription(podName: String, namespace: String) {
    val token = File("/var/run/secrets/kubernetes.io/serviceaccount/token").readText()
    val cert = File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt").readText()
    Fuel.get("https://kubernetes/").header(Pair("Authentication", "token $token"))
}
