package com.huawei.iimagekit.common.agp.graphics.porting.gl;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLUtils;
import com.huawei.iimagekit.common.agp.graphics.porting.gl.GFX.FramebufferTarget;

public class GFXUtil {
    private static native void updateBitmapFn(Bitmap bitmap);

    public static int initTexture(Bitmap bitmap) {
        int id = GFX.initTexture(bitmap.getWidth(), bitmap.getHeight());
        updateTexture(id, bitmap);
        return id;
    }

    public static void updateTexture(int textureID, Bitmap bitmap) {
        GFX.bindTexture(textureID);
        GLUtils.texSubImage2D(3553, 0, 0, 0, bitmap);
    }

    public static Bitmap initBitmap(int textureID) {
        int[] textureSize = new int[]{0, 0};
        GFX.getTextureSize(textureID, textureSize);
        Bitmap bitmap = Bitmap.createBitmap(textureSize[0], textureSize[1], Config.ARGB_8888);
        updateBitmap(bitmap, textureID);
        return bitmap;
    }

    public static void updateBitmap(Bitmap bitmap, int textureID) {
        int fbID = GFX.initFramebuffer();
        GFX.bindFramebufferToTexture(fbID, textureID, FramebufferTarget.FRAMEBUFFER);
        updateBitmapFn(bitmap);
        GFX.destroyFramebuffer(fbID);
        GFX.bindFramebuffer(0);
    }
}
