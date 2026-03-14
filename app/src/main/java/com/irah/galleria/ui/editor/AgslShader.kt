package com.irah.galleria.ui.editor

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

object AgslShader {
    
    // Minimum API is 33 (Tiramisu) for full AGSL RenderEffect support on Compose Modifier
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    const val ADJUSTMENT_SHADER = """
        uniform shader composable;
        
        // Light Adjustments
        uniform float exposure;
        uniform float brightness;
        uniform float contrast;
        uniform float highlights;
        uniform float shadows;
        uniform float whites;
        uniform float blacks;
        
        // Color Adjustments
        uniform float saturation;
        uniform float temperature;
        uniform float tint;
        uniform float vibrance;
        
        // Vignette and Geometry
        uniform float vignette;
        uniform float2 center;     // Image center, normalized 0.0 to 1.0 or pixel space
        uniform float maxRadius;   // Max radius of the image bounding box
        uniform float layerScale;  // Scale applied to the image layer
        uniform float2 layerOffset;// Translation applied to the image layer

        vec3 applyCurveMath(vec3 color, float shadowAmt, float highlightAmt, float whiteAmt, float blackAmt) {
            vec3 result = color;
            
            // Per-channel highlights and shadows approximation
            // Shadows: Boost darks. influence = (1.0 - color)^3
            vec3 shadowInfluence = pow(vec3(1.0) - result, vec3(3.0));
            result += shadowAmt * shadowInfluence * 0.5;
            
            // Highlights: Boost lights. influence = color^3
            vec3 hlInfluence = pow(result, vec3(3.0));
            result += highlightAmt * hlInfluence * 0.5;
            
            // Whites
            vec3 wMask = clamp((result - 0.7) / 0.3, 0.0, 1.0);
            result += whiteAmt * wMask * 0.2;
            
            // Blacks
            vec3 bMask = clamp((0.3 - result) / 0.3, 0.0, 1.0);
            result += blackAmt * bMask * 0.2;
            
            return clamp(result, 0.0, 1.0);
        }

        vec4 main(float2 coords) {
            vec4 color = composable.eval(coords);
            if (color.a == 0.0) return color; // Skip fully transparent
            
            // Un-premultiply alpha
            vec3 rgb = color.rgb / color.a;
            
            // 1. Exposure (2^exposure)
            float expScale = pow(2.0, exposure);
            rgb *= expScale;
            
            // 2. Brightness (Additive)
            rgb += brightness;
            
            // 3. Contrast (Scale around 0.5)
            // (color - 0.5) * contrast + 0.5
            rgb = (rgb - 0.5) * contrast + 0.5;
            rgb = clamp(rgb, 0.0, 1.0); // Clamp before complex curves
            
            // 4. Shadows / Highlights / Whites / Blacks
            if (shadows != 0.0 || highlights != 0.0 || whites != 0.0 || blacks != 0.0) {
                 rgb = applyCurveMath(rgb, shadows, highlights, whites, blacks);
            }
            
            // 5. Temperature (Blue <-> Yellow) and Tint (Green <-> Magenta)
            // Approx map -1..1 to reasonable RGB shifts
            // temp > 0 means add R, sub B
            // tint > 0 means add G
            float tempAmt = temperature * 0.15; // scalar
            float tintAmt = tint * 0.15;
            rgb.r = clamp(rgb.r + tempAmt, 0.0, 1.0);
            rgb.b = clamp(rgb.b - tempAmt, 0.0, 1.0);
            rgb.g = clamp(rgb.g + tintAmt, 0.0, 1.0);
            
            // 6. Saturation & Vibrance
            // Luminance standard
            float lum = dot(rgb, vec3(0.299, 0.587, 0.114));
            
            if (saturation != 1.0) {
                rgb = mix(vec3(lum), rgb, saturation);
            }
            
            if (vibrance != 0.0) {
                float maxRgb = max(rgb.r, max(rgb.g, rgb.b));
                float minRgb = min(rgb.r, min(rgb.g, rgb.b));
                float sat = maxRgb == 0.0 ? 0.0 : (maxRgb - minRgb) / maxRgb;
                float vibAmt = vibrance * 0.5 * (1.0 - sat);
                rgb = rgb + (rgb - vec3(lum)) * vibAmt;
            }
            
            rgb = clamp(rgb, 0.0, 1.0);
            
            // 7. Vignette
            if (vignette > 0.0) {
                 // coords are in physical screen space. 
                 // Map them back to the image's untransformed space so vignette scales with zoom.
                 float2 centeredCoords = coords - center;
                 centeredCoords -= layerOffset;
                 centeredCoords /= layerScale;
                 
                 float dist = sqrt(dot(centeredCoords, centeredCoords)) / maxRadius;
                 // edge0 = 0.3, edge1 = 1.0
                 float t = clamp((dist - 0.3) / 0.7, 0.0, 1.0);
                 float smoothT = t * t * (3.0 - 2.0 * t);
                 float darkness = smoothT * vignette;
                 rgb *= (1.0 - darkness);
            }
            
            // Re-premultiply alpha before returning to Compose
            return vec4(rgb * color.a, color.a);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun buildAdjustmentRenderEffect(
        adjustments: BitmapUtils.Adjustments,
        viewWidth: Float,
        viewHeight: Float,
        layerScale: Float = 1f,
        layerOffset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
    ): RenderEffect {
        val shader = RuntimeShader(ADJUSTMENT_SHADER)
        
        // Bind Light Properties
        shader.setFloatUniform("exposure", adjustments.exposure)
        shader.setFloatUniform("brightness", adjustments.brightness)
        shader.setFloatUniform("contrast", adjustments.contrast)
        shader.setFloatUniform("highlights", adjustments.highlights)
        shader.setFloatUniform("shadows", adjustments.shadows)
        shader.setFloatUniform("whites", adjustments.whites)
        shader.setFloatUniform("blacks", adjustments.blacks)
        
        // Bind Color Properties
        shader.setFloatUniform("saturation", adjustments.saturation)
        shader.setFloatUniform("temperature", adjustments.temperature)
        shader.setFloatUniform("tint", adjustments.tint)
        shader.setFloatUniform("vibrance", adjustments.vibrance)
        
        // Bind Vignette
        shader.setFloatUniform("vignette", adjustments.vignette)
        
        // Center of the Compose Box in pixel space
        val cw = viewWidth / 2f
        val ch = viewHeight / 2f
        shader.setFloatUniform("center", cw, ch)
        
        val maxRad = kotlin.math.sqrt(cw * cw + ch * ch)
        shader.setFloatUniform("maxRadius", if (maxRad == 0f) 1f else maxRad)
        
        // Geometry bindings
        shader.setFloatUniform("layerScale", layerScale)
        shader.setFloatUniform("layerOffset", layerOffset.x, layerOffset.y)
        
        return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    const val OUTLINE_SHADER = """
        uniform shader composable;
        uniform shader mask;
        uniform float time;
        uniform float2 size;     // Viewport size
        uniform float4 imageRect; // [left, top, right, bottom] in viewport space

        vec4 main(float2 coords) {
            vec4 color = composable.eval(coords);
            
            // Check if within image bounds
            if (coords.x < imageRect.x || coords.x > imageRect.z || 
                coords.y < imageRect.y || coords.y > imageRect.w) {
                return color;
            }

            float rawM = mask.eval(coords).a;
            
            // Threshold the mask internally for a clean edge
            float m = smoothstep(0.4, 0.6, rawM);
            
            // Edge detection logic: sample neighbors
            float edge = 0.0;
            float radius = 3.0; // Reduced for precision
            
            float mLeft = smoothstep(0.4, 0.6, mask.eval(coords + float2(-radius, 0.0)).a);
            float mRight = smoothstep(0.4, 0.6, mask.eval(coords + float2(radius, 0.0)).a);
            float mTop = smoothstep(0.4, 0.6, mask.eval(coords + float2(0.0, -radius)).a);
            float mBottom = smoothstep(0.4, 0.6, mask.eval(coords + float2(0.0, radius)).a);
            
            float diff = abs(m - mLeft) + abs(m - mRight) + abs(m - mTop) + abs(m - mBottom);
            edge = clamp(diff * 3.0, 0.0, 1.0);
            
            if (edge > 0.01) {
                float angle = atan(coords.y - (imageRect.y + imageRect.w) / 2.0, 
                                   coords.x - (imageRect.x + imageRect.z) / 2.0);
                float flow = sin(angle * 12.0 + time * 6.0) * 0.5 + 0.5;
                
                vec3 glowColor = mix(vec3(0.0, 0.8, 1.0), vec3(1.0, 1.0, 1.0), flow);
                float glowIntensity = edge * (0.8 + 0.2 * flow);
                
                return vec4(mix(color.rgb, glowColor, glowIntensity), color.a);
            }
            
            return color;
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun buildOutlineRenderEffect(
        mask: android.graphics.Bitmap,
        time: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        imageLeft: Float,
        imageTop: Float,
        imageRight: Float,
        imageBottom: Float
    ): RenderEffect {
        val shader = RuntimeShader(OUTLINE_SHADER)
        
        // Calculate matrix to map viewport coordinates to mask coordinates
        // Mask is always [0, 0] to [mask.width, mask.height]
        // Image is drawn in viewport at [imageLeft, imageTop] to [imageRight, imageBottom]
        val matrix = android.graphics.Matrix()
        val imageWidth = imageRight - imageLeft
        val imageHeight = imageBottom - imageTop
        
        val scaleX = mask.width.toFloat() / imageWidth
        val scaleY = mask.height.toFloat() / imageHeight
        
        matrix.setTranslate(-imageLeft, -imageTop)
        matrix.postScale(scaleX, scaleY)
        
        val bitmapShader = android.graphics.BitmapShader(mask, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        bitmapShader.setLocalMatrix(matrix)
        
        shader.setInputBuffer("mask", bitmapShader)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("size", viewportWidth, viewportHeight)
        shader.setFloatUniform("imageRect", imageLeft, imageTop, imageRight, imageBottom)
        
        return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
    }

    /**
     * Determines if the current adjustments can be entirely handled by the hardware AGSL shader.
     * We exclude complex operations like Skin Tones, HSL Curves, Spatial convolutions (Sharpen/Denoise), 
     * Background Masks, or specific predefined Image Filters unless we port them all.
     */
    fun canUseHardwareAcceleration(adj: BitmapUtils.Adjustments): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        
        // Check if any complex features that aren't in the shader are active
        if (adj.clarity != 0f || adj.sharpen != 0f || adj.denoise != 0f || adj.blur != 0f || adj.dehaze != 0f) return false
        if (adj.filter != FilterType.NONE) return false
        if (adj.backgroundMode != BitmapUtils.BackgroundMode.NONE) return false
        if (adj.skinTone != 0f || adj.skinColor != 0f) return false
        if (adj.hsl.isNotEmpty()) return false
        
        // Check Curves (If they differ from default 0,0->1,1 linear mapping, AGSL can't handle them without a LUT texture binding)
        val defCurve = listOf(0f to 0f, 1f to 1f)
        if (adj.curveRGB != defCurve || adj.curveRed != defCurve || adj.curveGreen != defCurve || adj.curveBlue != defCurve || adj.curveLuminance != defCurve) return false

        return true
    }
}
