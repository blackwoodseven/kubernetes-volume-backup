package com.blackwoodseven.kubernetes.volume_backup

import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.net.URL
import kotlin.test.assertEquals

class KubernetesApiSpec : Spek({
    val podJson = """{
  "apiVersion": "v1",
  "kind": "Pod",
  "metadata": {
    "annotations": {
      "kubernetes.io/created-by": "{\"kind\":\"SerializedReference\",\"apiVersion\":\"v1\",\"reference\":{\"kind\":\"ReplicaSet\",\"namespace\":\"monitoring\",\"name\":\"grafana-3558654421\",\"uid\":\"a3988031-529c-11e7-bab1-0abf805b923e\",\"apiVersion\":\"extensions\",\"resourceVersion\":\"19840246\"}}\n"
    },
    "creationTimestamp": "2017-06-19T12:53:12Z",
    "generateName": "grafana-3558654421-",
    "labels": {
      "app": "grafana",
      "pod-template-hash": "3558654421"
    },
    "name": "grafana-3558654421-f4nzp",
    "namespace": "monitoring",
    "ownerReferences": [
      {
        "apiVersion": "extensions/v1beta1",
        "blockOwnerDeletion": true,
        "controller": true,
        "kind": "ReplicaSet",
        "name": "grafana-3558654421",
        "uid": "a3988031-529c-11e7-bab1-0abf805b923e"
      }
    ],
    "resourceVersion": "19843085",
    "selfLink": "/api/v1/namespaces/monitoring/pods/grafana-3558654421-f4nzp",
    "uid": "40a2a4ad-54ee-11e7-b175-0a74ef784b3a"
  },
  "spec": {
    "containers": [
      {
        "env": [
          {
            "name": "GF_SERVER_DOMAIN",
            "value": "grafana.dev.blackwoodseven.com"
          },
          {
            "name": "GF_SERVER_ROOT_URL",
            "value": "https://grafana.dev.blackwoodseven.com"
          },
          {
            "name": "GF_SERVER_PROTOCOL",
            "value": "https"
          },
          {
            "name": "GF_SERVER_CERT_FILE",
            "value": "/certs/fullchain.pem"
          },
          {
            "name": "GF_SERVER_CERT_KEY",
            "value": "/certs/key.pem"
          },
          {
            "name": "GF_AUTH_DISABLE_LOGIN_FORM",
            "value": "true"
          },
          {
            "name": "GF_AUTH_ANONYMOUS_ENABLED",
            "value": "false"
          },
          {
            "name": "GF_AUTH_BASIC_ENABLED",
            "value": "false"
          },
          {
            "name": "GF_AUTH_GITHUB_ENABLED",
            "value": "true"
          },
          {
            "name": "GF_AUTH_GITHUB_CLIENT_ID",
            "value": "ec683fb63b12776814af"
          },
          {
            "name": "GF_AUTH_GITHUB_CLIENT_SECRET",
            "value": "a1f18e172f642ea6ee20701fc4a98ccd4af3246a"
          },
          {
            "name": "GF_AUTH_GITHUB_SCOPES",
            "value": "user:email,read:org"
          },
          {
            "name": "GF_AUTH_GITHUB_ALLOWED_ORGANIZATIONS",
            "value": "blackwoodseven"
          },
          {
            "name": "GF_SECURITY_ADMIN_USER",
            "valueFrom": {
              "secretKeyRef": {
                "key": "user",
                "name": "grafana-credentials"
              }
            }
          },
          {
            "name": "GF_SECURITY_ADMIN_PASSWORD",
            "valueFrom": {
              "secretKeyRef": {
                "key": "password",
                "name": "grafana-credentials"
              }
            }
          }
        ],
        "image": "grafana/grafana:4.1.1",
        "imagePullPolicy": "IfNotPresent",
        "name": "grafana",
        "ports": [
          {
            "containerPort": 3000,
            "name": "web",
            "protocol": "TCP"
          }
        ],
        "resources": {
          "limits": {
            "cpu": "200m",
            "memory": "200Mi"
          },
          "requests": {
            "cpu": "100m",
            "memory": "100Mi"
          }
        },
        "terminationMessagePath": "/dev/termination-log",
        "terminationMessagePolicy": "File",
        "volumeMounts": [
          {
            "mountPath": "/certs",
            "name": "cert-volume"
          },
          {
            "mountPath": "/var/lib/grafana",
            "name": "grafana-storage"
          },
          {
            "mountPath": "/var/run/secrets/kubernetes.io/serviceaccount",
            "name": "default-token-7wqfm",
            "readOnly": true
          }
        ]
      },
      {
        "command": [
          "sh",
          "-c"
        ],
        "env": [
          {
            "name": "VOLUME_TO_MONITOR",
            "value": "/certs"
          }
        ],
        "image": "alpine",
        "imagePullPolicy": "Always",
        "livenessProbe": {
          "exec": {
            "command": [
              "sh",
              "-c"
            ]
          },
          "failureThreshold": 1,
          "initialDelaySeconds": 5,
          "periodSeconds": 5,
          "successThreshold": 1,
          "timeoutSeconds": 1
        },
        "name": "configmap-watcher",
        "resources": {},
        "terminationMessagePath": "/dev/termination-log",
        "terminationMessagePolicy": "File",
        "volumeMounts": [
          {
            "mountPath": "/certs",
            "name": "cert-volume",
            "readOnly": true
          },
          {
            "mountPath": "/var/run/secrets/kubernetes.io/serviceaccount",
            "name": "default-token-7wqfm",
            "readOnly": true
          }
        ]
      }
    ],
    "dnsPolicy": "ClusterFirst",
    "nodeName": "ip-172-20-32-131.eu-west-1.compute.internal",
    "restartPolicy": "Always",
    "schedulerName": "default-scheduler",
    "securityContext": {},
    "serviceAccount": "default",
    "serviceAccountName": "default",
    "terminationGracePeriodSeconds": 30,
    "tolerations": [
      {
        "effect": "NoSchedule",
        "key": "team",
        "operator": "Equal",
        "value": "sre"
      },
      {
        "effect": "NoExecute",
        "key": "node.alpha.kubernetes.io/notReady",
        "operator": "Exists",
        "tolerationSeconds": 300
      },
      {
        "effect": "NoExecute",
        "key": "node.alpha.kubernetes.io/unreachable",
        "operator": "Exists",
        "tolerationSeconds": 300
      }
    ],
    "volumes": [
      {
        "name": "cert-volume",
        "secret": {
          "defaultMode": 420,
          "secretName": "grafana-dev-blackwoodseven-com-tls"
        }
      },
      {
        "name": "grafana-storage",
        "persistentVolumeClaim": {
          "claimName": "grafana-volume"
        }
      },
      {
        "name": "default-token-7wqfm",
        "secret": {
          "defaultMode": 420,
          "secretName": "default-token-7wqfm"
        }
      }
    ]
  },
  "status": {
    "conditions": [
      {
        "lastProbeTime": null,
        "lastTransitionTime": "2017-06-19T12:53:12Z",
        "status": "True",
        "type": "Initialized"
      },
      {
        "lastProbeTime": null,
        "lastTransitionTime": "2017-06-19T12:54:53Z",
        "status": "True",
        "type": "Ready"
      },
      {
        "lastProbeTime": null,
        "lastTransitionTime": "2017-06-19T12:53:12Z",
        "status": "True",
        "type": "PodScheduled"
      }
    ],
    "containerStatuses": [
      {
        "containerID": "docker://79ceb8a700a3fb7cfd416147044c01568780f7cd2954d7186896de595fa4870b",
        "image": "alpine:latest",
        "imageID": "docker-pullable://alpine@sha256:0b94d1d1b5eb130dd0253374552445b39470653fb1a1ec2d81490948876e462c",
        "lastState": {},
        "name": "configmap-watcher",
        "ready": true,
        "restartCount": 0,
        "state": {
          "running": {
            "startedAt": "2017-06-19T12:54:53Z"
          }
        }
      },
      {
        "containerID": "docker://2ed0359a4301ad69728919ef4c24fa5b9517486083247f0375c4cde01f1da41a",
        "image": "grafana/grafana:4.1.1",
        "imageID": "docker-pullable://grafana/grafana@sha256:68fe8467f5e7cb87fcace36c5517364c7195b30cbc73451294e7bbe9312db4ff",
        "lastState": {},
        "name": "grafana",
        "ready": true,
        "restartCount": 0,
        "state": {
          "running": {
            "startedAt": "2017-06-19T12:54:45Z"
          }
        }
      }
    ],
    "hostIP": "172.20.32.131",
    "phase": "Running",
    "podIP": "100.96.1.7",
    "qosClass": "Burstable",
    "startTime": "2017-06-19T12:53:12Z"
  }
}
"""
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
    describe("parsePodJson") {
        it("should parse the podJson into the expected PodDescription") {
            val actualPodDescription = PodDescription.Deserializer().deserialize(podJson)
            assertEquals(podDescription, actualPodDescription)
        }
    }

    describe("fetchPodDescription") {
        var sentRequest: Request? = null
        val oldClient = FuelManager.instance.client

        beforeEachTest {
            sentRequest = null
        }

        beforeGroup {
            FuelManager.instance.client = object : Client {
                override fun executeRequest(request: Request): Response {
                    sentRequest = request
                    return Response().apply {
                        data = podJson.toByteArray()
                        httpStatusCode = 200
                        httpResponseMessage = "OK"
                    }
                }
            }
        }

        afterGroup {
            //Restore the Fuel Client
            FuelManager.instance.client = oldClient
        }

        it("should send the correct request, and return the PodDescription structure") {
            val result = fetchPodDescription("some-pod-name", "some-namespace")
            assertEquals(podDescription, result)
            assertEquals(
                    sentRequest?.url,
                    URL("https://kubernetes.default/api/v1/namespaces/some-namespace/pods/some-pod-name/")
            )
        }

        it("should be possible to override the kubernetes hostname") {
            val result = fetchPodDescription("some-pod-name", "some-namespace", "some.kubernetes.host")
            assertEquals(podDescription, result)
            assertEquals(
                    sentRequest?.url,
                    URL("https://some.kubernetes.host/api/v1/namespaces/some-namespace/pods/some-pod-name/")
            )
        }
    }
})
