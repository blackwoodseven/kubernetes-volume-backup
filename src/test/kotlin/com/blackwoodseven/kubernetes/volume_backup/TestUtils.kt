package com.blackwoodseven.kubernetes.volume_backup

import java.lang.reflect.Field
import java.util.*

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
