package com.blackwoodseven.kubernetes.volume_backup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.ResponseDeserializable
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
) {
    class Deserializer: ResponseDeserializable<PodDescription> {
        override fun deserialize(content: String): PodDescription {
            val mapper = jacksonObjectMapper()
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            return mapper.readValue<PodDescription>(content)
        }
    }
}

fun getKubernetesToken(): String? {
    val tokenFile = File("/var/run/secrets/kubernetes.io/serviceaccount/token")

    return if (tokenFile.isFile && tokenFile.canRead()) {
        tokenFile.readText()
    } else {
        null
    }
}

fun fetchPodDescription(podName: String, namespace: String, kubernetesHostName: String = "kubernetes.default", token: String? = null): PodDescription? {
    var req = Fuel.get("https://$kubernetesHostName/api/v1/namespaces/$namespace/pods/$podName/")

    if (token != null) {
        req = req.header("Authorization" to "Bearer $token")
    }
    val (request, response, result) = req.responseObject(PodDescription.Deserializer())
    val (podDescription, error) = result
    if (error != null) {
        throw error
    }
    return podDescription
}
