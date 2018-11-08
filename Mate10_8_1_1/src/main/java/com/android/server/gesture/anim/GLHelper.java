package com.android.server.gesture.anim;

import android.opengl.GLES20;

public class GLHelper {
    private static final String TAG = "GLHelper";

    private static int compileShader(int type, String shaderCode) {
        int shaderObjectId = GLES20.glCreateShader(type);
        while (true) {
            int error = GLES20.glGetError();
            if (error == 0) {
                break;
            }
            GLLogUtils.logE(TAG, "error " + error);
        }
        if (shaderObjectId == 0) {
            GLLogUtils.logW(TAG, "Could not create new shader.");
            return 0;
        }
        GLES20.glShaderSource(shaderObjectId, shaderCode);
        GLES20.glCompileShader(shaderObjectId);
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shaderObjectId, 35713, compileStatus, 0);
        GLLogUtils.logD(TAG, "Results of compiling source:\n" + shaderCode + "\n:" + GLES20.glGetShaderInfoLog(shaderObjectId));
        if (compileStatus[0] != 0) {
            return shaderObjectId;
        }
        GLES20.glDeleteShader(shaderObjectId);
        GLLogUtils.logW(TAG, "Compilation of shader failed.");
        return 0;
    }

    private static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        int programObjectId = GLES20.glCreateProgram();
        if (programObjectId == 0) {
            GLLogUtils.logW(TAG, "Could not create new program");
            return 0;
        }
        GLES20.glAttachShader(programObjectId, vertexShaderId);
        GLES20.glAttachShader(programObjectId, fragmentShaderId);
        GLES20.glLinkProgram(programObjectId);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, 35714, linkStatus, 0);
        GLLogUtils.logV(TAG, "Results of linking program:\n" + GLES20.glGetProgramInfoLog(programObjectId));
        if (linkStatus[0] != 0) {
            return programObjectId;
        }
        GLES20.glDeleteProgram(programObjectId);
        GLLogUtils.logW(TAG, "Linking of program failed.");
        return 0;
    }

    private static void validateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, 35715, validateStatus, 0);
        GLLogUtils.logV(TAG, "Results of validating program: " + validateStatus[0] + "\nLog:" + GLES20.glGetProgramInfoLog(programObjectId));
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int program = linkProgram(compileShader(35633, vertexShaderSource), compileShader(35632, fragmentShaderSource));
        validateProgram(program);
        return program;
    }
}
