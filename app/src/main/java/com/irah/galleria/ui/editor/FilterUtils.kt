package com.irah.galleria.ui.editor

import android.graphics.ColorMatrix

enum class FilterType(val label: String) {
    NONE("Original"),
    BW("B&W"),
    SEPIA("Sepia"),
    VINTAGE("Vintage"),
    WARM("Golden"),
    COOL("Cool"),
    CINEMATIC("Cine"),
    DRAMATIC("Drama"),
    SKIN_ROSY("Rosy"),
    SKIN_GOLDEN("Gold Skin"),
    SKIN_SOFT("Soft"),
    SKIN_TAN("Tan")
}

object FilterUtils {
    fun createFilterMatrix(type: FilterType): ColorMatrix {
        val cm = ColorMatrix()
        when (type) {
            FilterType.NONE -> { /* Identity */ }
            FilterType.BW -> cm.setSaturation(0f)
            FilterType.SEPIA -> {
                cm.setScale(1f, 0.95f, 0.82f, 1f) // Base warm
                val sepia = ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                 // Sepia is strong, so we blend it or just set it
                 cm.set(sepia)
            }
            FilterType.VINTAGE -> {
                // Low saturation + Yellow tint
                cm.setSaturation(0.6f)
                val tint = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, 20f,
                    0f, 1f, 0f, 0f, 20f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(tint)
            }
            FilterType.WARM -> {
                cm.setScale(1.1f, 1.05f, 0.9f, 1f)
            }
            FilterType.COOL -> {
                cm.setScale(0.9f, 1.0f, 1.1f, 1f)
            }
            FilterType.CINEMATIC -> {
                // Teal/Orange vibe: Boost Red shadows (not easy with matrix),
                // Simplified: Contrast + Sat + Blue Tint in shadows?
                // Just use a matrix that crushes blacks and boosts midtones
                 val contrast = ColorMatrix(floatArrayOf(
                    1.2f, 0f, 0f, 0f, -20f,
                    0f, 1.2f, 0f, 0f, -20f,
                    0f, 0f, 1.2f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(contrast)
            }
            FilterType.DRAMATIC -> {
                cm.setSaturation(0.8f) // Desaturate
                 val highContrast = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, -50f,
                    0f, 1.4f, 0f, 0f, -50f,
                    0f, 0f, 1.4f, 0f, -50f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(highContrast)
            }
            FilterType.SKIN_ROSY -> {
                // Subtle Pink tint + Slight brightness
                cm.setScale(1.05f, 0.95f, 0.95f, 1f) // Push Red
                val pinkish = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, 10f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 5f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(pinkish)
            }
            FilterType.SKIN_GOLDEN -> {
                // Warm, slightly saturated
                cm.setSaturation(1.1f)
                cm.setScale(1.1f, 1.05f, 0.9f, 1f)
            }
            FilterType.SKIN_SOFT -> {
                // Low contrast, slightly bright
                 val soft = ColorMatrix(floatArrayOf(
                    0.9f, 0f, 0f, 0f, 20f,
                    0f, 0.9f, 0f, 0f, 20f,
                    0f, 0f, 0.9f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(soft)
                cm.setSaturation(0.9f)
            }
            FilterType.SKIN_TAN -> {
                // Darker, warmer, higher contrast
                 val tan = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, -10f,
                    0f, 1f, 0f, 0f, -10f,
                    0f, 0f, 1f, 0f, -20f, // Less blue = Yellow
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(tan)
                cm.setSaturation(1.15f)
            }
        }
        return cm
    }

    fun scaleFilterMatrix(cm: ColorMatrix, strength: Float): ColorMatrix {
        if (strength >= 1f) return cm
        if (strength <= 0f) return ColorMatrix() // Identity

        val target = cm.array
        val result = FloatArray(20)
        
        // Identity Matrix
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        for (i in 0 until 20) {
            result[i] = identity[i] + (target[i] - identity[i]) * strength
        }
        
        return ColorMatrix(result)
    }
}
