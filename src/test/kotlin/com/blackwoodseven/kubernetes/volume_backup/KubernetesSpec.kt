package com.blackwoodseven.kubernetes.volume_backup

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@RunWith(JUnit4::class)
class KubernetesSpec : Spek({
    describe("Kubernetes") {
        val podDescription = PodDescription(
                PodSpec(
                        listOf(
                                Container(
                                        "grafana",
                                        listOf(
                                                VolumeMount("cert-volume", "/certs"),
                                                VolumeMount("grafana-storage", "/var/lib/grafana"),
                                                VolumeMount("default-token-7wqfm", "/var/run/secrets/kubernetes.io/serviceaccount")
                                        )
                                ),
                                Container(
                                        "configmap-watcher",
                                        listOf(
                                                VolumeMount("cert-volume", "/certs"),
                                                VolumeMount("default-token-7wqfm", "/var/run/secrets/kubernetes.io/serviceaccount")
                                        )
                                )
                        ),
                        listOf(
                                Volume(
                                        "cert-volume",
                                        null
                                ),
                                Volume(
                                        "grafana-storage",
                                        PersistentVolumeClaim(
                                                "grafana-volume"
                                        )
                                ),
                                Volume(
                                        "default-token-7wqfm",
                                        null
                                )
                        )
                )
        )

        describe("matchClaimNameToMountPaths") {
            it("should extract the relevant information about backup volumes") {
                val volumesToBackup = matchClaimNameToMountPaths("grafana", podDescription)
                assertEquals(hashMapOf(
                    "grafana-volume" to "/var/lib/grafana"
                ), volumesToBackup)
            }
        }
    }
})
