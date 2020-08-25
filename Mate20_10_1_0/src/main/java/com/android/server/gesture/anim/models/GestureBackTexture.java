package com.android.server.gesture.anim.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.display.HwFoldScreenState;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import com.android.server.gesture.anim.GLHelper;
import com.android.server.gesture.anim.GLLogUtils;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GestureBackTexture implements GLModel {
    private static final int BASE_VIEW_HEIGHT = 2340;
    private static final int BASE_VIEW_WIDTH = 1080;
    private static final int DOCK_TEXTURE_H = 62;
    private static final int DOCK_TEXTURE_W = 62;
    private static final String FRAGMENT_SHADER_CODE = "#version 300 es\nprecision mediump float;\n\nuniform float uAlpha;\nuniform sampler2D uTexture;\n\nin vec2 vTexCoord;\n\nout vec4 oFragColor;\n\nvoid main() {\n    vec4 vt = texture(uTexture, vTexCoord);\n    oFragColor = vec4(vt.rgb, vt.a * uAlpha);\n}";
    private static final int POINT_DESCRIBE_SIZE = 2;
    private static final String TAG = "GestureBackTexture";
    private static final int TEXTURE_H = 62;
    private static final int TEXTURE_W = 38;
    private static final String VERTEX_SHADER_CODE = "#version 300 es\n\nlayout (location = 0) in vec4 aPosition;\nlayout (location = 1) in vec2 aTexCoordinate;\n\nout vec2 vTexCoord;\n\nvoid main() {\n    gl_Position = aPosition;\n    vTexCoord = aTexCoordinate;\n}";
    private int mAlphaLoc;
    private float mAnimProcess;
    private int mAttrTextureLoc;
    private int mAttrVertexLoc;
    private int mCenterY;
    private Context mContext;
    private float mDockAlpha;
    private float mFoldMainScreenRatio = 0.0f;
    private int mHeight;
    private int mIcon;
    private boolean mIsDraw;
    private boolean mIsFoldScreenDevice = false;
    private boolean mIsLeft;
    private float mMaxTextureOffset;
    private int mProgram;
    private FloatBuffer mRectBuffer;
    private float[] mRectData = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    private float mTextureAlpha;
    private FloatBuffer mTextureBuffer;
    private final float[] mTextureData = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    private int mTextureH;
    private int mTextureLoc;
    private int mTextureW;
    private int mWidth;

    public GestureBackTexture(Context context, int icon) {
        this.mContext = context;
        this.mIsDraw = true;
        this.mMaxTextureOffset = 0.15f;
        this.mAnimProcess = 0.0f;
        this.mIsLeft = true;
        this.mTextureAlpha = 0.0f;
        this.mCenterY = 0;
        this.mIcon = icon;
        this.mDockAlpha = 1.0f;
        if (this.mIcon == 33751903) {
            this.mTextureW = 38;
            this.mTextureH = 62;
        } else {
            this.mTextureW = 62;
            this.mTextureH = 62;
        }
        if (HwFoldScreenState.isFoldScreenDevice()) {
            this.mIsFoldScreenDevice = true;
            Rect foldScreenSize = HwFoldScreenState.getScreenPhysicalRect(2);
            this.mFoldMainScreenRatio = ((float) foldScreenSize.width()) / ((float) foldScreenSize.height());
        }
    }

    @Override // com.android.server.gesture.anim.models.GLModel
    public void prepare() {
        this.mProgram = GLHelper.buildProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);
        this.mRectBuffer = ByteBuffer.allocateDirect(this.mRectData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(this.mRectData);
        createTexture(this.mContext, this.mIcon);
        this.mAttrVertexLoc = GLES30.glGetAttribLocation(this.mProgram, "aPosition");
        this.mAttrTextureLoc = GLES30.glGetAttribLocation(this.mProgram, "aTexCoordinate");
        this.mAlphaLoc = GLES30.glGetUniformLocation(this.mProgram, "uAlpha");
        this.mTextureLoc = GLES30.glGetUniformLocation(this.mProgram, "uTexture");
    }

    @Override // com.android.server.gesture.anim.models.GLModel
    public void onSurfaceViewChanged(int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        this.mWidth = width;
        this.mHeight = height;
        if (this.mCenterY == 0) {
            this.mCenterY = height / 2;
        }
    }

    @Override // com.android.server.gesture.anim.models.GLModel
    public void drawSelf() {
        if (this.mIsDraw) {
            int centerY = changeCenterToGl(this.mCenterY);
            if (this.mIsFoldScreenDevice) {
                int longSide = this.mWidth;
                int i = this.mHeight;
                if (longSide <= i) {
                    longSide = i;
                }
                int viewportWidth = (int) (this.mFoldMainScreenRatio * ((float) longSide));
                float viewRatio = ((float) viewportWidth) / 1080.0f;
                if (this.mIsLeft) {
                    int i2 = this.mTextureH;
                    GLES30.glViewport(0, centerY - ((int) ((((float) i2) * viewRatio) / 2.0f)), viewportWidth, (int) (((float) i2) * viewRatio));
                } else {
                    int i3 = this.mTextureH;
                    GLES30.glViewport(this.mWidth - viewportWidth, centerY - ((int) ((((float) i3) * viewRatio) / 2.0f)), viewportWidth, (int) (((float) i3) * viewRatio));
                }
                refreshVertexLoc((float) viewportWidth, viewRatio);
            } else {
                int i4 = this.mWidth;
                int i5 = this.mHeight;
                if (i4 < i5) {
                    float viewRatio2 = ((float) i4) / 1080.0f;
                    int i6 = this.mTextureH;
                    GLES30.glViewport(0, centerY - ((int) ((((float) i6) * viewRatio2) / 2.0f)), i4, (int) (((float) i6) * viewRatio2));
                    refreshVertexLoc((float) this.mWidth, viewRatio2);
                } else if (this.mIsLeft) {
                    float viewRatio3 = ((float) i5) / 1080.0f;
                    int i7 = this.mTextureH;
                    GLES30.glViewport(0, centerY - ((int) ((((float) i7) * viewRatio3) / 2.0f)), i5, (int) (((float) i7) * viewRatio3));
                    refreshVertexLoc((float) this.mHeight, viewRatio3);
                } else {
                    float viewRatio4 = ((float) i5) / 1080.0f;
                    int i8 = this.mTextureH;
                    GLES30.glViewport(i4 - i5, centerY - ((int) ((((float) i8) * viewRatio4) / 2.0f)), i5, (int) (((float) i8) * viewRatio4));
                    refreshVertexLoc((float) this.mHeight, viewRatio4);
                }
            }
            GLES30.glUseProgram(this.mProgram);
            GLES30.glUniform1f(this.mAlphaLoc, this.mTextureAlpha * this.mDockAlpha);
            int mTextureValue = 0;
            if (this.mIcon == 33751957) {
                mTextureValue = 1;
            }
            GLES30.glUniform1i(this.mTextureLoc, mTextureValue);
            this.mTextureBuffer = ByteBuffer.allocateDirect(this.mTextureData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(this.mTextureData);
            this.mRectBuffer.flip();
            GLES30.glEnableVertexAttribArray(this.mAttrVertexLoc);
            GLES30.glVertexAttribPointer(this.mAttrVertexLoc, 2, 5126, false, 8, (Buffer) this.mRectBuffer);
            this.mTextureBuffer.flip();
            GLES30.glEnableVertexAttribArray(this.mAttrTextureLoc);
            GLES30.glVertexAttribPointer(this.mAttrTextureLoc, 2, 5126, false, 8, (Buffer) this.mTextureBuffer);
            GLES30.glDrawArrays(5, 0, this.mRectData.length / 2);
        }
    }

    private int changeCenterToGl(int positionY) {
        return this.mHeight - positionY;
    }

    private void refreshVertexLoc(float drawWidth, float viewRatio) {
        float textureW = (((float) this.mTextureW) * viewRatio) / drawWidth;
        float startOffset = ((38.0f * viewRatio) / drawWidth) * 2.0f;
        float textureCenterX = (-1.0f - startOffset) + ((this.mMaxTextureOffset + startOffset) * this.mAnimProcess);
        if (!this.mIsLeft) {
            textureCenterX *= -1.0f;
        }
        this.mRectData = new float[]{textureCenterX - textureW, 1.0f, textureCenterX + textureW, 1.0f, textureCenterX - textureW, -1.0f, textureCenterX + textureW, -1.0f};
        this.mRectBuffer = ByteBuffer.allocateDirect(this.mRectData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(this.mRectData);
    }

    private void createTexture(Context context, int resourceId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        if (bitmap == null) {
            GLLogUtils.logD(TAG, ">> load bitmap fail");
            return;
        }
        int[] texture = new int[1];
        GLES30.glGenTextures(1, texture, 0);
        if (texture[0] == 0) {
            GLLogUtils.logD(TAG, ">> create texture fail");
            return;
        }
        if (resourceId == 33751903) {
            GLES30.glActiveTexture(33984);
            GLES30.glBindTexture(3553, texture[0]);
        } else {
            GLES30.glActiveTexture(33985);
            GLES30.glBindTexture(3553, texture[0]);
        }
        GLES30.glTexParameterf(3553, 10241, 9728.0f);
        GLES30.glTexParameterf(3553, 10240, 9729.0f);
        GLES30.glTexParameterf(3553, 10243, 33071.0f);
        GLES30.glTexParameterf(3553, 10242, 33071.0f);
        GLUtils.texImage2D(3553, 0, 6408, bitmap, 5121, 0);
        bitmap.recycle();
        GLES30.glGenerateMipmap(3553);
    }

    private void setAlpha(float alpha) {
        this.mTextureAlpha = alpha;
    }

    public void setDockAlpha(float alpha) {
        this.mDockAlpha = alpha;
    }

    public void setScaleRate(float scaleRate) {
        float scaleValue = 1.0f / scaleRate;
        float[] fArr = this.mTextureData;
        fArr[0] = (1.0f - scaleValue) / 2.0f;
        fArr[1] = (1.0f - scaleValue) / 2.0f;
        fArr[2] = (scaleValue + 1.0f) / 2.0f;
        fArr[3] = (1.0f - scaleValue) / 2.0f;
        fArr[4] = (1.0f - scaleValue) / 2.0f;
        fArr[5] = (scaleValue + 1.0f) / 2.0f;
        fArr[6] = (scaleValue + 1.0f) / 2.0f;
        fArr[7] = (1.0f + scaleValue) / 2.0f;
    }

    public void setProcess(float process) {
        this.mAnimProcess = process;
        float f = this.mAnimProcess;
        if (f >= 0.55f) {
            setAlpha(1.0f);
        } else if (f < 0.275f) {
            setAlpha(0.0f);
        } else {
            setAlpha((f - 0.275f) / (0.55f - 0.275f));
        }
    }

    public void setSide(boolean isLeft) {
        this.mIsLeft = isLeft;
    }

    public void setCenter(int positionY) {
        this.mCenterY = positionY;
    }

    public void setDraw(boolean isDraw) {
        this.mIsDraw = isDraw;
    }
}
