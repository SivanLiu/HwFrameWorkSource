package java.util;

import java.io.Serializable;

public class EventObject implements Serializable {
    private static final long serialVersionUID = 5516075349620653480L;
    protected transient Object source;

    public EventObject(Object source) {
        if (source != null) {
            this.source = source;
            return;
        }
        throw new IllegalArgumentException("null source");
    }

    public Object getSource() {
        return this.source;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[source=");
        stringBuilder.append(this.source);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
