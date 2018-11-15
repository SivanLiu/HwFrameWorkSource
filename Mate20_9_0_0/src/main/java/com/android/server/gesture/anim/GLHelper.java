package com.android.server.gesture.anim;

import android.opengl.GLES30;

public class GLHelper {
    private static final String TAG = "GLHelper";

    private static int compileShader(int type, String shaderCode) {
        int shaderObjectId = GLES30.glCreateShader(type);
        while (true) {
            int glGetError = GLES30.glGetError();
            int error = glGetError;
            if (glGetError == 0) {
                break;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error ");
            stringBuilder.append(error);
            GLLogUtils.logE(str, stringBuilder.toString());
        }
        if (shaderObjectId == 0) {
            GLLogUtils.logW(TAG, "Could not create new shader.");
            return 0;
        }
        GLES30.glShaderSource(shaderObjectId, shaderCode);
        GLES30.glCompileShader(shaderObjectId);
        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shaderObjectId, 35713, compileStatus, 0);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Results of compiling source:\n");
        stringBuilder2.append(shaderCode);
        stringBuilder2.append("\n:");
        stringBuilder2.append(GLES30.glGetShaderInfoLog(shaderObjectId));
        GLLogUtils.logD(str2, stringBuilder2.toString());
        if (compileStatus[0] != 0) {
            return shaderObjectId;
        }
        GLES30.glDeleteShader(shaderObjectId);
        GLLogUtils.logW(TAG, "Compilation of shader failed.");
        return 0;
    }

    private static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        int programObjectId = GLES30.glCreateProgram();
        if (programObjectId == 0) {
            GLLogUtils.logW(TAG, "Could not create new program");
            return 0;
        }
        GLES30.glAttachShader(programObjectId, vertexShaderId);
        GLES30.glAttachShader(programObjectId, fragmentShaderId);
        GLES30.glLinkProgram(programObjectId);
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(programObjectId, 35714, linkStatus, 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Results of linking program:\n");
        stringBuilder.append(GLES30.glGetProgramInfoLog(programObjectId));
        GLLogUtils.logV(str, stringBuilder.toString());
        if (linkStatus[0] != 0) {
            return programObjectId;
        }
        GLES30.glDeleteProgram(programObjectId);
        GLLogUtils.logW(TAG, "Linking of program failed.");
        return 0;
    }

    private static void validateProgram(int programObjectId) {
        GLES30.glValidateProgram(programObjectId);
        int[] validateStatus = new int[1];
        GLES30.glGetProgramiv(programObjectId, 35715, validateStatus, 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Results of validating program: ");
        stringBuilder.append(validateStatus[0]);
        stringBuilder.append("\nLog:");
        stringBuilder.append(GLES30.glGetProgramInfoLog(programObjectId));
        GLLogUtils.logV(str, stringBuilder.toString());
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int program = linkProgram(compileShader(35633, vertexShaderSource), compileShader(35632, fragmentShaderSource));
        validateProgram(program);
        return program;
    }
}
