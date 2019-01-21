package javax.sip;

public enum DialogState {
    EARLY,
    CONFIRMED,
    TERMINATED;
    
    public static final int _CONFIRMED = 0;
    public static final int _EARLY = 0;
    public static final int _TERMINATED = 0;

    static {
        _EARLY = EARLY.ordinal();
        _CONFIRMED = CONFIRMED.ordinal();
        _TERMINATED = TERMINATED.ordinal();
    }

    public static DialogState getObject(int state) {
        try {
            return values()[state];
        } catch (IndexOutOfBoundsException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid dialog state: ");
            stringBuilder.append(state);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public int getValue() {
        return ordinal();
    }
}
