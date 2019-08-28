package com.otaliastudios.cameraview.filters;

import android.support.annotation.NonNull;
import com.otaliastudios.cameraview.filter.BaseFilter;

/**
 * Inverts the input colors. This is also known as negative effect.
 */
public class InvertColorsFilter extends BaseFilter {

    private final static String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 vTextureCoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
            + "  float colorR = (1.0 - color.r) / 1.0;\n"
            + "  float colorG = (1.0 - color.g) / 1.0;\n"
            + "  float colorB = (1.0 - color.b) / 1.0;\n"
            + "  gl_FragColor = vec4(colorR, colorG, colorB, color.a);\n"
            + "}\n";

    public InvertColorsFilter() { }

    @NonNull
    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }
}
