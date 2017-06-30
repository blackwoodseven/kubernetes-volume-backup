package com.blackwoodseven.kubernetes.volume_backup

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
