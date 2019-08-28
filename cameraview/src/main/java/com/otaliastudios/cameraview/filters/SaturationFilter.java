package com.otaliastudios.cameraview.filters;

import android.opengl.GLES20;

import android.support.annotation.NonNull;
import com.otaliastudios.cameraview.filter.BaseFilter;
import com.otaliastudios.cameraview.filter.OneParameterFilter;
import com.otaliastudios.cameraview.internal.GlUtils;

/**
 * Adjusts color saturation.
 */
public class SaturationFilter extends BaseFilter implements OneParameterFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "uniform float scale;\n"
            + "uniform vec3 exponents;\n"
            + "float shift;\n"
            + "vec3 weights;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  weights[0] = " + 2f / 8f + ";\n"
            + "  weights[1] = " + 5f / 8f + ";\n"
            + "  weights[2] = " + 1f / 8f + ";\n"
            + "  shift = " + 1.0f / 255.0f + ";\n"
            + "  vec4 oldcolor = texture2D(sTexture, vTextureCoord);\n"
            + "  float kv = dot(oldcolor.rgb, weights) + shift;\n"
            + "  vec3 new_color = scale * oldcolor.rgb + (1.0 - scale) * kv;\n"
            + "  gl_FragColor = vec4(new_color, oldcolor.a);\n"
            + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
            + "  float de = dot(color.rgb, weights);\n"
            + "  float inv_de = 1.0 / de;\n"
            + "  vec3 verynew_color = de * pow(color.rgb * inv_de, exponents);\n"
            + "  float max_color = max(max(max(verynew_color.r, verynew_color.g), verynew_color.b), 1.0);\n"
            + "  gl_FragColor = gl_FragColor+vec4(verynew_color / max_color, color.a);\n"
            + "}\n";

    private float scale = 1F; // -1...1
    private int scaleLocation = -1;
    private int exponentsLocation = -1;

    public SaturationFilter() { }

    /**
     * Sets the saturation correction value:
     * -1.0: fully desaturated, grayscale.
     * 0.0: no change.
     * +1.0: fully saturated.
     *
     * @param value new value
     */
    @SuppressWarnings("WeakerAccess")
    public void setSaturation(float value) {
        if (value < -1F) value = -1F;
        if (value > 1F) value = 1F;
        scale = value;
    }

    /**
     * Returns the current saturation.
     *
     * @see #setSaturation(float)
     * @return saturation
     */
    @SuppressWarnings("WeakerAccess")
    public float getSaturation() {
        return scale;
    }

    @Override
    public void setParameter1(float value) {
        setSaturation(2F * value - 1F);
    }

    @Override
    public float getParameter1() {
        return (getSaturation() + 1F) / 2F;
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onCreate(int programHandle) {
        super.onCreate(programHandle);
        scaleLocation = GLES20.glGetUniformLocation(programHandle, "scale");
        GlUtils.checkLocation(scaleLocation, "scale");
        exponentsLocation = GLES20.glGetUniformLocation(programHandle, "exponents");
        GlUtils.checkLocation(exponentsLocation, "exponents");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scaleLocation = -1;
        exponentsLocation = -1;
    }

    @Override
    protected void onPreDraw(float[] transformMatrix) {
        super.onPreDraw(transformMatrix);
        if (scale > 0.0f) {
            GLES20.glUniform1f(scaleLocation, 0F);
            GlUtils.checkError("glUniform1f");
            GLES20.glUniform3f(exponentsLocation,
                    (0.9f * scale) + 1.0f,
                    (2.1f * scale) + 1.0f,
                    (2.7f * scale) + 1.0f
            );
            GlUtils.checkError("glUniform3f");
        } else {
            GLES20.glUniform1f(scaleLocation, 1.0F + scale);
            GlUtils.checkError("glUniform1f");
            GLES20.glUniform3f(exponentsLocation, 0F, 0F, 0F);
            GlUtils.checkError("glUniform3f");
        }
    }
}
