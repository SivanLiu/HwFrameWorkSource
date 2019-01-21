package android.filterpacks.ui;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.GLEnvironment;
import android.filterfw.core.GLFrame;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.util.Log;
import android.view.Surface;

public class SurfaceTargetFilter extends Filter {
    private static final String TAG = "SurfaceRenderFilter";
    private final int RENDERMODE_FILL_CROP = 2;
    private final int RENDERMODE_FIT = 1;
    private final int RENDERMODE_STRETCH = 0;
    private float mAspectRatio = 1.0f;
    private GLEnvironment mGlEnv;
    private boolean mLogVerbose = Log.isLoggable(TAG, 2);
    private ShaderProgram mProgram;
    private int mRenderMode = 1;
    @GenerateFieldPort(hasDefault = true, name = "renderMode")
    private String mRenderModeString;
    private GLFrame mScreen;
    @GenerateFieldPort(name = "oheight")
    private int mScreenHeight;
    @GenerateFieldPort(name = "owidth")
    private int mScreenWidth;
    @GenerateFinalPort(name = "surface")
    private Surface mSurface;
    private int mSurfaceId = -1;

    public SurfaceTargetFilter(String name) {
        super(name);
    }

    public void setupPorts() {
        if (this.mSurface != null) {
            addMaskedInputPort("frame", ImageFormat.create(3));
            return;
        }
        throw new RuntimeException("NULL Surface passed to SurfaceTargetFilter");
    }

    public void updateRenderMode() {
        if (this.mRenderModeString != null) {
            if (this.mRenderModeString.equals("stretch")) {
                this.mRenderMode = 0;
            } else if (this.mRenderModeString.equals("fit")) {
                this.mRenderMode = 1;
            } else if (this.mRenderModeString.equals("fill_crop")) {
                this.mRenderMode = 2;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown render mode '");
                stringBuilder.append(this.mRenderModeString);
                stringBuilder.append("'!");
                throw new RuntimeException(stringBuilder.toString());
            }
        }
        updateTargetRect();
    }

    public void prepare(FilterContext context) {
        this.mGlEnv = context.getGLEnvironment();
        this.mProgram = ShaderProgram.createIdentity(context);
        this.mProgram.setSourceRect(0.0f, 1.0f, 1.0f, -1.0f);
        this.mProgram.setClearsOutput(true);
        this.mProgram.setClearColor(0.0f, 0.0f, 0.0f);
        this.mScreen = (GLFrame) context.getFrameManager().newBoundFrame(ImageFormat.create(this.mScreenWidth, this.mScreenHeight, 3, 3), 101, 0);
        updateRenderMode();
    }

    public void open(FilterContext context) {
        registerSurface();
    }

    public void process(FilterContext context) {
        Frame gpuFrame;
        if (this.mLogVerbose) {
            Log.v(TAG, "Starting frame processing");
        }
        Frame input = pullInput("frame");
        boolean createdFrame = false;
        float currentAspectRatio = ((float) input.getFormat().getWidth()) / ((float) input.getFormat().getHeight());
        if (currentAspectRatio != this.mAspectRatio) {
            if (this.mLogVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("New aspect ratio: ");
                stringBuilder.append(currentAspectRatio);
                stringBuilder.append(", previously: ");
                stringBuilder.append(this.mAspectRatio);
                Log.v(str, stringBuilder.toString());
            }
            this.mAspectRatio = currentAspectRatio;
            updateTargetRect();
        }
        if (this.mLogVerbose) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Got input format: ");
            stringBuilder2.append(input.getFormat());
            Log.v(str2, stringBuilder2.toString());
        }
        if (input.getFormat().getTarget() != 3) {
            gpuFrame = context.getFrameManager().duplicateFrameToTarget(input, 3);
            createdFrame = true;
        } else {
            gpuFrame = input;
        }
        this.mGlEnv.activateSurfaceWithId(this.mSurfaceId);
        this.mProgram.process(gpuFrame, (Frame) this.mScreen);
        this.mGlEnv.swapBuffers();
        if (createdFrame) {
            gpuFrame.release();
        }
    }

    public void fieldPortValueUpdated(String name, FilterContext context) {
        this.mScreen.setViewport(0, 0, this.mScreenWidth, this.mScreenHeight);
        updateTargetRect();
    }

    public void close(FilterContext context) {
        unregisterSurface();
    }

    public void tearDown(FilterContext context) {
        if (this.mScreen != null) {
            this.mScreen.release();
        }
    }

    private void updateTargetRect() {
        if (this.mScreenWidth > 0 && this.mScreenHeight > 0 && this.mProgram != null) {
            float relativeAspectRatio = (((float) this.mScreenWidth) / ((float) this.mScreenHeight)) / this.mAspectRatio;
            switch (this.mRenderMode) {
                case 0:
                    this.mProgram.setTargetRect(0.0f, 0.0f, 1.0f, 1.0f);
                    return;
                case 1:
                    if (relativeAspectRatio > 1.0f) {
                        this.mProgram.setTargetRect(0.5f - (0.5f / relativeAspectRatio), 0.0f, 1.0f / relativeAspectRatio, 1.0f);
                        return;
                    } else {
                        this.mProgram.setTargetRect(0.0f, 0.5f - (0.5f * relativeAspectRatio), 1.0f, relativeAspectRatio);
                        return;
                    }
                case 2:
                    if (relativeAspectRatio > 1.0f) {
                        this.mProgram.setTargetRect(0.0f, 0.5f - (0.5f * relativeAspectRatio), 1.0f, relativeAspectRatio);
                        return;
                    } else {
                        this.mProgram.setTargetRect(0.5f - (0.5f / relativeAspectRatio), 0.0f, 1.0f / relativeAspectRatio, 1.0f);
                        return;
                    }
                default:
                    return;
            }
        }
    }

    private void registerSurface() {
        this.mSurfaceId = this.mGlEnv.registerSurface(this.mSurface);
        if (this.mSurfaceId < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not register Surface: ");
            stringBuilder.append(this.mSurface);
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    private void unregisterSurface() {
        if (this.mSurfaceId > 0) {
            this.mGlEnv.unregisterSurfaceId(this.mSurfaceId);
        }
    }
}
