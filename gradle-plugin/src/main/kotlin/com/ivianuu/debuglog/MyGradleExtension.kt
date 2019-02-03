package com.ivianuu.debuglog

open class MyGradleExtension {
    /** If [false], this plugin won't actually be applied */
    var enabled: Boolean = true

    /** FQ names of annotations that should count as debuglog annotations */
    var annotations: List<String> = emptyList()
}
