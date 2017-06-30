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

fun getKubernetesCert(): String? {
    val certFile = File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt")

    return if (certFile.isFile && certFile.canRead()) {
        certFile.readText()
    } else {
        null
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

fun fetchPodDescription(podName: String, namespace: String, token: String? = null, cert: String? = null): PodDescription? {
    var req = Fuel.get("https://api.k8s.dev.blackwoodseven.com/api/v1/namespaces/$namespace/pods/$podName/")

    if (token != null) {
        req = req.header(Pair("Authentication", "token $token"))
    }
    val (request, response, result) = req.responseObject(PodDescription.Deserializer())
    val (podDescription, error) = result
    if (error != null) {
        throw error
    }
    return podDescription
}
