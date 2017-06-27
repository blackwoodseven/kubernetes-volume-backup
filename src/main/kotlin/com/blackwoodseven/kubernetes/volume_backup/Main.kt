package com.blackwoodseven.kubernetes.volume_backup

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties

object backup : PropertyGroup() {
    val directories by stringType
    val cron by stringType
    val pod_name by stringType
    val pod_namespace by stringType
    val container_name by stringType
}

object aws : PropertyGroup() {
    val default_region by stringType
    val bucket by stringType
    val access_key_id by stringType
    val secret_access_key by stringType
}



fun main(args : Array<String>) {
    val config = systemProperties() overriding
                 EnvironmentVariables()

    println(config[backup.directories])
    println(config[aws.default_region])

}
