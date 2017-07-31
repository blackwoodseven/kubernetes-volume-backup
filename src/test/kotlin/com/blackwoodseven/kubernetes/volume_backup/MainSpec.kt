package com.blackwoodseven.kubernetes.volume_backup

import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.net.InetAddress
import java.time.Duration
import kotlin.test.assertEquals


class MainSpec : Spek({
    describe("the system should be sane") {
        it("should calculate correctly") {
            assertEquals(4, 2 + 2)
        }
    }

    describe("parseConfig") {
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
                        httpStatusCode = 404
                        httpResponseMessage = "Not Found"
                    }
                }
            }
        }

        afterGroup {
            //Restore the Fuel Client
            FuelManager.instance.client = oldClient
        }

        it("should fetch the configuration options from environment variables") {
            setEnvironment(mapOf(
                    "AWS_ACCESS_KEY_ID" to "key id",
                    "AWS_SECRET_ACCESS_KEY" to "secret key",
                    "AWS_DEFAULT_REGION" to "region",
                    "AWS_S3_BUCKET_NAME" to "bucket name",
                    "K8S_POD_NAME" to "pod name",
                    "K8S_NAMESPACE" to "namespace",
                    "K8S_CONTAINER_NAME" to "backup container name",
                    "K8S_API_HOSTNAME" to "somehost",
                    "BACKUP_INTERVAL" to "PT2H",
                    "FORCED_PATHS" to "{\"some-backup-name\":\"/some/forced/volume\"}",
                    "INCLUDES" to "[\"*.csv\", \"*.xlsx\"]",
                    "EXCLUDES" to "[\"*-malformed-*\", \"*-corrupted-*\"]"
            ))

            val config = parseConfig()

            assertEquals(Config(
                    "key id",
                    "secret key",
                    "region",
                    "bucket name",
                    "pod name",
                    "namespace",
                    "backup container name",
                    "somehost",
                    Duration.ofHours(2),
                    mapOf("some-backup-name" to "/some/forced/volume"),
                    listOf("*.csv", "*.xlsx"),
                    listOf("*-malformed-*", "*-corrupted-*")
            ), config)
        }

        it("should fail with an empty environment") {
            setEnvironment(emptyMap())

            assert.that({parseConfig()}, throws<IllegalArgumentException>(
                    has(Exception::message, equalTo("No S3 bucket specified"))))
        }

        it("should fail if only bucket given") {
            setEnvironment(mapOf(
                    "AWS_S3_BUCKET_NAME" to "bucket name"
            ))

            assert.that({parseConfig()}, throws<Exception>(
                    has(Exception::message, equalTo("AWS Region auto-detection failed, is this container running in AWS?"))))
        }

        it("should work if only bucket, region given") {
            setEnvironment(mapOf(
                    "AWS_S3_BUCKET_NAME" to "bucket name",
                    "AWS_DEFAULT_REGION" to "eu-west-1"
            ))

            val config = parseConfig()

            assert.that(config, equalTo(Config(
                    null,
                    null,
                    "eu-west-1",
                    "bucket name",
                    InetAddress.getLocalHost().hostName,
                    "default",
                    "volume-backup",
                    "kubernetes.default",
                    Duration.ofHours(1),
                    null,
                    null,
                    null
            )))
        }
    }
})

