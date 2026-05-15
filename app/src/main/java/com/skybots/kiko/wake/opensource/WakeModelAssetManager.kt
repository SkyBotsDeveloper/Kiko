package com.skybots.kiko.wake.opensource

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import java.io.FileNotFoundException

class WakeModelAssetManager private constructor(
    private val assetManager: AssetManager?,
    private val modelPath: String,
    private val existsProvider: (() -> Boolean)?,
) {
    constructor(
        context: Context,
        modelPath: String = OpenSourceWakeConfig.MODEL_ASSET_PATH,
    ) : this(
        assetManager = context.applicationContext.assets,
        modelPath = modelPath,
        existsProvider = null,
    )

    constructor(
        modelExists: () -> Boolean,
        modelPath: String = OpenSourceWakeConfig.MODEL_ASSET_PATH,
    ) : this(
        assetManager = null,
        modelPath = modelPath,
        existsProvider = modelExists,
    )

    fun health(): WakeEngineHealth =
        when {
            existsProvider != null -> if (existsProvider.invoke()) {
                WakeEngineHealth.ready()
            } else {
                WakeEngineHealth.modelMissing()
            }
            assetManager == null -> WakeEngineHealth.modelMissing()
            else -> runCatching {
                val manager = assetManager ?: return WakeEngineHealth.modelMissing()
                manager.openFd(modelPath).use { descriptor ->
                    if (descriptor.length > 0L) {
                        WakeEngineHealth.ready()
                    } else {
                        WakeEngineHealth.modelInvalid("Open-source Hey Kiko model is empty.")
                    }
                }
            }.getOrElse { error ->
                if (error is FileNotFoundException) {
                    WakeEngineHealth.modelMissing()
                } else {
                    WakeEngineHealth.modelInvalid(
                        error.message ?: "Open-source Hey Kiko model could not be opened.",
                    )
                }
            }
        }

    fun openFd(): AssetFileDescriptor? =
        assetManager?.openFd(modelPath)
}
