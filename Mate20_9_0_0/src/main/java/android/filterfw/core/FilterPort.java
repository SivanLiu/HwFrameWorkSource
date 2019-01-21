package android.filterfw.core;

import android.util.Log;

public abstract class FilterPort {
    private static final String TAG = "FilterPort";
    protected boolean mChecksType = false;
    protected Filter mFilter;
    protected boolean mIsBlocking = true;
    protected boolean mIsOpen = false;
    private boolean mLogVerbose;
    protected String mName;
    protected FrameFormat mPortFormat;

    public abstract void clear();

    public abstract boolean filterMustClose();

    public abstract boolean hasFrame();

    public abstract boolean isReady();

    public abstract Frame pullFrame();

    public abstract void pushFrame(Frame frame);

    public abstract void setFrame(Frame frame);

    public FilterPort(Filter filter, String name) {
        this.mName = name;
        this.mFilter = filter;
        this.mLogVerbose = Log.isLoggable(TAG, 2);
    }

    public boolean isAttached() {
        return this.mFilter != null;
    }

    public FrameFormat getPortFormat() {
        return this.mPortFormat;
    }

    public void setPortFormat(FrameFormat format) {
        this.mPortFormat = format;
    }

    public Filter getFilter() {
        return this.mFilter;
    }

    public String getName() {
        return this.mName;
    }

    public void setBlocking(boolean blocking) {
        this.mIsBlocking = blocking;
    }

    public void setChecksType(boolean checksType) {
        this.mChecksType = checksType;
    }

    public void open() {
        if (!this.mIsOpen && this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Opening ");
            stringBuilder.append(this);
            Log.v(str, stringBuilder.toString());
        }
        this.mIsOpen = true;
    }

    public void close() {
        if (this.mIsOpen && this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Closing ");
            stringBuilder.append(this);
            Log.v(str, stringBuilder.toString());
        }
        this.mIsOpen = false;
    }

    public boolean isOpen() {
        return this.mIsOpen;
    }

    public boolean isBlocking() {
        return this.mIsBlocking;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("port '");
        stringBuilder.append(this.mName);
        stringBuilder.append("' of ");
        stringBuilder.append(this.mFilter);
        return stringBuilder.toString();
    }

    protected void assertPortIsOpen() {
        if (!isOpen()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal operation on closed ");
            stringBuilder.append(this);
            stringBuilder.append("!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    protected void checkFrameType(Frame frame, boolean forceCheck) {
        if ((this.mChecksType || forceCheck) && this.mPortFormat != null && !frame.getFormat().isCompatibleWith(this.mPortFormat)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Frame passed to ");
            stringBuilder.append(this);
            stringBuilder.append(" is of incorrect type! Expected ");
            stringBuilder.append(this.mPortFormat);
            stringBuilder.append(" but got ");
            stringBuilder.append(frame.getFormat());
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    protected void checkFrameManager(Frame frame, FilterContext context) {
        if (frame.getFrameManager() != null && frame.getFrameManager() != context.getFrameManager()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Frame ");
            stringBuilder.append(frame);
            stringBuilder.append(" is managed by foreign FrameManager! ");
            throw new RuntimeException(stringBuilder.toString());
        }
    }
}
