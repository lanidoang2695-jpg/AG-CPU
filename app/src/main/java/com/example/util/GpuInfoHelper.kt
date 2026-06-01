package com.example.util

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.util.Log

object GpuInfoHelper {
    data class GpuData(
        val vendor: String,
        val renderer: String,
        val version: String
    )

    fun getGpuDetails(): GpuData {
        var vendor = "Unknown Vendor"
        var renderer = "Unknown Renderer (Software Render)"
        var version = "Unknown OpenGL ES version"

        var eglDisplay: EGLDisplay? = null
        var eglContext: EGLContext? = null
        var eglSurface: EGLSurface? = null

        try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                return GpuData(vendor, renderer, version)
            }

            val versionMajorMinor = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, versionMajorMinor, 0, versionMajorMinor, 1)) {
                return GpuData(vendor, renderer, version)
            }

            val configSpec = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configSpec, 0, configs, 0, 1, numConfigs, 0)

            if (numConfigs[0] > 0 && configs[0] != null) {
                val config = configs[0]!!
                val contextSpec = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
                )
                eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextSpec, 0)
                
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    val pbufferSpec = intArrayOf(
                        EGL14.EGL_WIDTH, 1,
                        EGL14.EGL_HEIGHT, 1,
                        EGL14.EGL_NONE
                    )
                    eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, pbufferSpec, 0)
                    if (eglSurface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                        
                        vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown Vendor"
                        renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown Renderer"
                        version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GpuInfoHelper", "Error probing OpenGL details for GPU hardware profile", e)
        } finally {
            try {
                if (eglDisplay != null && eglDisplay != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(
                        eglDisplay,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT
                    )
                    if (eglSurface != null && eglSurface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    }
                    if (eglContext != null && eglContext != EGL14.EGL_NO_CONTEXT) {
                        EGL14.eglDestroyContext(eglDisplay, eglContext)
                    }
                    EGL14.eglTerminate(eglDisplay)
                }
            } catch (ex: Exception) {
                Log.e("GpuInfoHelper", "EGL cleanup exception", ex)
            }
        }

        return GpuData(vendor, renderer, version)
    }
}
