package android.filterfw.core;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.opengl.GLES20;
import java.nio.ByteBuffer;

public class GLFrame extends Frame {
    public static final int EXISTING_FBO_BINDING = 101;
    public static final int EXISTING_TEXTURE_BINDING = 100;
    public static final int EXTERNAL_TEXTURE = 104;
    public static final int NEW_FBO_BINDING = 103;
    public static final int NEW_TEXTURE_BINDING = 102;
    private int glFrameId = -1;
    private GLEnvironment mGLEnvironment;
    private boolean mOwnsTexture = true;

    private native boolean generateNativeMipMap();

    private native boolean getNativeBitmap(Bitmap bitmap);

    private native byte[] getNativeData();

    private native int getNativeFboId();

    private native float[] getNativeFloats();

    private native int[] getNativeInts();

    private native int getNativeTextureId();

    private native boolean nativeAllocate(GLEnvironment gLEnvironment, int i, int i2);

    private native boolean nativeAllocateExternal(GLEnvironment gLEnvironment);

    private native boolean nativeAllocateWithFbo(GLEnvironment gLEnvironment, int i, int i2, int i3);

    private native boolean nativeAllocateWithTexture(GLEnvironment gLEnvironment, int i, int i2, int i3);

    private native boolean nativeCopyFromGL(GLFrame gLFrame);

    private native boolean nativeCopyFromNative(NativeFrame nativeFrame);

    private native boolean nativeDeallocate();

    private native boolean nativeDetachTexFromFbo();

    private native boolean nativeFocus();

    private native boolean nativeReattachTexToFbo();

    private native boolean nativeResetParams();

    private native boolean setNativeBitmap(Bitmap bitmap, int i);

    private native boolean setNativeData(byte[] bArr, int i, int i2);

    private native boolean setNativeFloats(float[] fArr);

    private native boolean setNativeInts(int[] iArr);

    private native boolean setNativeTextureParam(int i, int i2);

    private native boolean setNativeViewport(int i, int i2, int i3, int i4);

    GLFrame(FrameFormat format, FrameManager frameManager) {
        super(format, frameManager);
    }

    GLFrame(FrameFormat format, FrameManager frameManager, int bindingType, long bindingId) {
        super(format, frameManager, bindingType, bindingId);
    }

    void init(GLEnvironment glEnv) {
        FrameFormat format = getFormat();
        this.mGLEnvironment = glEnv;
        if (format.getBytesPerSample() != 4) {
            throw new IllegalArgumentException("GL frames must have 4 bytes per sample!");
        } else if (format.getDimensionCount() != 2) {
            throw new IllegalArgumentException("GL frames must be 2-dimensional!");
        } else if (getFormat().getSize() >= 0) {
            int bindingType = getBindingType();
            boolean reusable = true;
            if (bindingType == 0) {
                initNew(false);
            } else if (bindingType == 104) {
                initNew(true);
                reusable = false;
            } else if (bindingType == 100) {
                initWithTexture((int) getBindingId());
            } else if (bindingType == 101) {
                initWithFbo((int) getBindingId());
            } else if (bindingType == 102) {
                initWithTexture((int) getBindingId());
            } else if (bindingType == 103) {
                initWithFbo((int) getBindingId());
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Attempting to create GL frame with unknown binding type ");
                stringBuilder.append(bindingType);
                stringBuilder.append("!");
                throw new RuntimeException(stringBuilder.toString());
            }
            setReusable(reusable);
        } else {
            throw new IllegalArgumentException("Initializing GL frame with zero size!");
        }
    }

    private void initNew(boolean isExternal) {
        if (isExternal) {
            if (!nativeAllocateExternal(this.mGLEnvironment)) {
                throw new RuntimeException("Could not allocate external GL frame!");
            }
        } else if (!nativeAllocate(this.mGLEnvironment, getFormat().getWidth(), getFormat().getHeight())) {
            throw new RuntimeException("Could not allocate GL frame!");
        }
    }

    private void initWithTexture(int texId) {
        if (nativeAllocateWithTexture(this.mGLEnvironment, texId, getFormat().getWidth(), getFormat().getHeight())) {
            this.mOwnsTexture = false;
            markReadOnly();
            return;
        }
        throw new RuntimeException("Could not allocate texture backed GL frame!");
    }

    private void initWithFbo(int fboId) {
        if (!nativeAllocateWithFbo(this.mGLEnvironment, fboId, getFormat().getWidth(), getFormat().getHeight())) {
            throw new RuntimeException("Could not allocate FBO backed GL frame!");
        }
    }

    void flushGPU(String message) {
        StopWatchMap timer = GLFrameTimer.get();
        if (timer.LOG_MFF_RUNNING_TIMES) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("glFinish ");
            stringBuilder.append(message);
            timer.start(stringBuilder.toString());
            GLES20.glFinish();
            stringBuilder = new StringBuilder();
            stringBuilder.append("glFinish ");
            stringBuilder.append(message);
            timer.stop(stringBuilder.toString());
        }
    }

    protected synchronized boolean hasNativeAllocation() {
        return this.glFrameId != -1;
    }

    protected synchronized void releaseNativeAllocation() {
        nativeDeallocate();
        this.glFrameId = -1;
    }

    public GLEnvironment getGLEnvironment() {
        return this.mGLEnvironment;
    }

    public Object getObjectValue() {
        assertGLEnvValid();
        return ByteBuffer.wrap(getNativeData());
    }

    public void setInts(int[] ints) {
        assertFrameMutable();
        assertGLEnvValid();
        if (!setNativeInts(ints)) {
            throw new RuntimeException("Could not set int values for GL frame!");
        }
    }

    public int[] getInts() {
        assertGLEnvValid();
        flushGPU("getInts");
        return getNativeInts();
    }

    public void setFloats(float[] floats) {
        assertFrameMutable();
        assertGLEnvValid();
        if (!setNativeFloats(floats)) {
            throw new RuntimeException("Could not set int values for GL frame!");
        }
    }

    public float[] getFloats() {
        assertGLEnvValid();
        flushGPU("getFloats");
        return getNativeFloats();
    }

    public void setData(ByteBuffer buffer, int offset, int length) {
        assertFrameMutable();
        assertGLEnvValid();
        byte[] bytes = buffer.array();
        if (getFormat().getSize() != bytes.length) {
            throw new RuntimeException("Data size in setData does not match GL frame size!");
        } else if (!setNativeData(bytes, offset, length)) {
            throw new RuntimeException("Could not set GL frame data!");
        }
    }

    public ByteBuffer getData() {
        assertGLEnvValid();
        flushGPU("getData");
        return ByteBuffer.wrap(getNativeData());
    }

    public void setBitmap(Bitmap bitmap) {
        assertFrameMutable();
        assertGLEnvValid();
        if (getFormat().getWidth() == bitmap.getWidth() && getFormat().getHeight() == bitmap.getHeight()) {
            Bitmap rgbaBitmap = Frame.convertBitmapToRGBA(bitmap);
            if (!setNativeBitmap(rgbaBitmap, rgbaBitmap.getByteCount())) {
                throw new RuntimeException("Could not set GL frame bitmap data!");
            }
            return;
        }
        throw new RuntimeException("Bitmap dimensions do not match GL frame dimensions!");
    }

    public Bitmap getBitmap() {
        assertGLEnvValid();
        flushGPU("getBitmap");
        Bitmap result = Bitmap.createBitmap(getFormat().getWidth(), getFormat().getHeight(), Config.ARGB_8888);
        if (getNativeBitmap(result)) {
            return result;
        }
        throw new RuntimeException("Could not get bitmap data from GL frame!");
    }

    public void setDataFromFrame(Frame frame) {
        assertGLEnvValid();
        if (getFormat().getSize() < frame.getFormat().getSize()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempting to assign frame of size ");
            stringBuilder.append(frame.getFormat().getSize());
            stringBuilder.append(" to smaller GL frame of size ");
            stringBuilder.append(getFormat().getSize());
            stringBuilder.append("!");
            throw new RuntimeException(stringBuilder.toString());
        } else if (frame instanceof NativeFrame) {
            nativeCopyFromNative((NativeFrame) frame);
        } else if (frame instanceof GLFrame) {
            nativeCopyFromGL((GLFrame) frame);
        } else if (frame instanceof SimpleFrame) {
            setObjectValue(frame.getObjectValue());
        } else {
            super.setDataFromFrame(frame);
        }
    }

    public void setViewport(int x, int y, int width, int height) {
        assertFrameMutable();
        setNativeViewport(x, y, width, height);
    }

    public void setViewport(Rect rect) {
        assertFrameMutable();
        setNativeViewport(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    public void generateMipMap() {
        assertFrameMutable();
        assertGLEnvValid();
        if (!generateNativeMipMap()) {
            throw new RuntimeException("Could not generate mip-map for GL frame!");
        }
    }

    public void setTextureParameter(int param, int value) {
        assertFrameMutable();
        assertGLEnvValid();
        if (!setNativeTextureParam(param, value)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not set texture value ");
            stringBuilder.append(param);
            stringBuilder.append(" = ");
            stringBuilder.append(value);
            stringBuilder.append(" for GLFrame!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public int getTextureId() {
        return getNativeTextureId();
    }

    public int getFboId() {
        return getNativeFboId();
    }

    public void focus() {
        if (!nativeFocus()) {
            throw new RuntimeException("Could not focus on GLFrame for drawing!");
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GLFrame id: ");
        stringBuilder.append(this.glFrameId);
        stringBuilder.append(" (");
        stringBuilder.append(getFormat());
        stringBuilder.append(") with texture ID ");
        stringBuilder.append(getTextureId());
        stringBuilder.append(", FBO ID ");
        stringBuilder.append(getFboId());
        return stringBuilder.toString();
    }

    protected void reset(FrameFormat newFormat) {
        if (nativeResetParams()) {
            super.reset(newFormat);
            return;
        }
        throw new RuntimeException("Could not reset GLFrame texture parameters!");
    }

    protected void onFrameStore() {
        if (!this.mOwnsTexture) {
            nativeDetachTexFromFbo();
        }
    }

    protected void onFrameFetch() {
        if (!this.mOwnsTexture) {
            nativeReattachTexToFbo();
        }
    }

    private void assertGLEnvValid() {
        if (!this.mGLEnvironment.isContextActive()) {
            StringBuilder stringBuilder;
            if (GLEnvironment.isAnyContextActive()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Attempting to access ");
                stringBuilder.append(this);
                stringBuilder.append(" with foreign GL context active!");
                throw new RuntimeException(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Attempting to access ");
            stringBuilder.append(this);
            stringBuilder.append(" with no GL context  active!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    static {
        System.loadLibrary("filterfw");
    }
}
