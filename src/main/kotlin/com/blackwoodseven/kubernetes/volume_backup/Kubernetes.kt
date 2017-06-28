package com.blackwoodseven.kubernetes.volume_backup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.Fuel
import java.io.File

data class VolumeMount(
        val name: String,
        val mountPath: String
)

data class Container(
        val name: String,
        val volumeMounts: List<VolumeMount>

)

data class PersistentVolumeClaim(
        val claimName: String
)

data class Volume(
        val name: String,
        val persistentVolumeClaim: PersistentVolumeClaim?
)

data class PodSpec(
        val containers: List<Container>,
        val volumes: List<Volume>
)

data class PodDescription(
        val spec: PodSpec
)

fun GetVolumeNames(podName: String, namespace: String): String {
    val podJson = fetchPodDescription(podName, namespace)
    val podDescription = parsePodJson(podJson)


    return "hej"
}

fun parsePodJson(podDescription: String): PodDescription {
    val mapper = jacksonObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    return mapper.readValue<PodDescription>(podDescription)
}

fun matchMountPathsToPVCs(containerName: String, podDescription: PodDescription): Map<String, String> {
    val spec = podDescription.spec

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

fun fetchPodDescription(podName: String, namespace: String): String {
    val token = File("/var/run/secrets/kubernetes.io/serviceaccount/token").readText()
    val cert = File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt").readText()
    Fuel.get("https://kubernetes/api/v1/namespaces/$namespace/pods/$podName/").header(Pair("Authentication", "token $token"))
    return "Hej"
}
