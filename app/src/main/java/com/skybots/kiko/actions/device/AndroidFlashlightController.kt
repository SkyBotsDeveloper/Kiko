package com.skybots.kiko.actions.device

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class AndroidFlashlightController(context: Context) : FlashlightController {
    private val cameraManager = context.applicationContext
        .getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cachedFlashCameraId: String? = null

    override fun setFlashlight(enabled: Boolean): FlashlightControlResult {
        val cameraId = cachedFlashCameraId ?: findFlashCameraId()?.also { id ->
            cachedFlashCameraId = id
        } ?: return FlashlightControlResult.Unavailable

        return try {
            cameraManager.setTorchMode(cameraId, enabled)
            FlashlightControlResult.Success
        } catch (error: CameraAccessException) {
            FlashlightControlResult.Error(error.message)
        } catch (error: SecurityException) {
            FlashlightControlResult.Error(error.message)
        } catch (error: IllegalArgumentException) {
            cachedFlashCameraId = null
            FlashlightControlResult.Error(error.message)
        }
    }

    private fun findFlashCameraId(): String? =
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: CameraAccessException) {
            null
        } catch (_: SecurityException) {
            null
        }
}
