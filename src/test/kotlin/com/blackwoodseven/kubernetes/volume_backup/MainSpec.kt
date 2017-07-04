package com.blackwoodseven.kubernetes.volume_backup

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.lang.reflect.Field
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals


class MainSpec : Spek({
    describe("the system should be sane") {
        it("should calculate correctly") {
            assertEquals(4, 2 + 2)
        }
    }

    describe("parseConfig") {
        it("should fetch the configuration options from environtment variable") {
            setEnvironment(mapOf(
                    "AWS_ACCESS_KEY_ID" to "key id",
                    "AWS_SECRET_ACCESS_KEY" to "secret key",
                    "AWS_DEFAULT_REGION" to "region",
                    "AWS_S3_BUCKET_NAME" to "bucket name",
                    "K8S_POD_NAME" to "pod name",
                    "K8S_NAMESPACE" to "namespace",
                    "K8S_CONTAINER_NAME" to "backup container name",
                    "BACKUP_INTERVAL" to "PT2H"
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
                    Duration.ofHours(2)
            ), config)
        }
    }
})

// Crazy function for overriding environment variables
fun setEnvironment(newenv: Map<String, String>) {
    val classes: Array<out Class<*>> = Collections::class.java.declaredClasses
    val env: MutableMap<String, String> = System.getenv()
    for (cl in classes) {
        if ("java.util.Collections\$UnmodifiableMap" == cl.name) {
            val field: Field = cl.getDeclaredField("m")
            field.isAccessible = true
            val obj: Any = field.get(env)
            @Suppress("UNCHECKED_CAST")
            val map: MutableMap<String, String> = obj as MutableMap<String, String>
            map.clear()
            map.putAll(newenv)
        }
    }
}
