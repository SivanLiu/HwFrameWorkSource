package android.filterfw.core;

import java.lang.reflect.Field;

public class FinalPort extends FieldPort {
    public FinalPort(Filter filter, String name, Field field, boolean hasDefault) {
        super(filter, name, field, hasDefault);
    }

    protected synchronized void setFieldFrame(Frame frame, boolean isAssignment) {
        assertPortIsOpen();
        checkFrameType(frame, isAssignment);
        if (this.mFilter.getStatus() == 0) {
            super.setFieldFrame(frame, isAssignment);
            super.transfer(null);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempting to modify ");
            stringBuilder.append(this);
            stringBuilder.append("!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("final ");
        stringBuilder.append(super.toString());
        return stringBuilder.toString();
    }
}
