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

fun matchClaimNameToMountPaths(thisContainerName: String, podDescription: PodDescription): Map<String, String> {
    val spec = podDescription.spec

    // Extract this container's volume mounts
    val volumeMounts = spec.containers.first { it.name == thisContainerName }.volumeMounts

    // Map volumeMounts into "name -> path"
    val nameToMountPath = volumeMounts.associate { (name, mountPath) -> name to mountPath }

    // Map volumes into "claimName -> name"
    val claimNameToName = spec.volumes.mapNotNull {
        if (it.persistentVolumeClaim != null) it.persistentVolumeClaim.claimName to it.name else null
    }.toMap()

    // Join "claimName -> name" and "name -> path" into "claimName -> path"
    return claimNameToName.mapNotNull { (claimName, name) ->
        if (nameToMountPath.contains(name)) claimName to nameToMountPath[name]!! else null
    }.toMap()
}

fun fetchPodDescription(podName: String, namespace: String): String {
    val token = File("/var/run/secrets/kubernetes.io/serviceaccount/token").readText()
    val cert = File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt").readText()
    Fuel.get("https://kubernetes/api/v1/namespaces/$namespace/pods/$podName/").header(Pair("Authentication", "token $token"))
    return "Hej"
}
