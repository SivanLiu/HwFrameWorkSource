package android.filterfw.core;

import java.lang.reflect.Field;

public class FieldPort extends InputPort {
    protected Field mField;
    protected boolean mHasFrame;
    protected Object mValue;
    protected boolean mValueWaiting = false;

    public FieldPort(Filter filter, String name, Field field, boolean hasDefault) {
        super(filter, name);
        this.mField = field;
        this.mHasFrame = hasDefault;
    }

    public void clear() {
    }

    public void pushFrame(Frame frame) {
        setFieldFrame(frame, false);
    }

    public void setFrame(Frame frame) {
        setFieldFrame(frame, true);
    }

    public Object getTarget() {
        try {
            return this.mField.get(this.mFilter);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public synchronized void transfer(FilterContext context) {
        if (this.mValueWaiting) {
            try {
                this.mField.set(this.mFilter, this.mValue);
                this.mValueWaiting = false;
                if (context != null) {
                    this.mFilter.notifyFieldPortValueUpdated(this.mName, context);
                }
            } catch (IllegalAccessException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Access to field '");
                stringBuilder.append(this.mField.getName());
                stringBuilder.append("' was denied!");
                throw new RuntimeException(stringBuilder.toString());
            }
        }
    }

    public synchronized Frame pullFrame() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot pull frame on ");
        stringBuilder.append(this);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public synchronized boolean hasFrame() {
        return this.mHasFrame;
    }

    public synchronized boolean acceptsFrame() {
        return this.mValueWaiting ^ 1;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("field ");
        stringBuilder.append(super.toString());
        return stringBuilder.toString();
    }

    protected synchronized void setFieldFrame(Frame frame, boolean isAssignment) {
        assertPortIsOpen();
        checkFrameType(frame, isAssignment);
        Object value = frame.getObjectValue();
        if ((value == null && this.mValue != null) || !value.equals(this.mValue)) {
            this.mValue = value;
            this.mValueWaiting = true;
        }
        this.mHasFrame = true;
    }
}
