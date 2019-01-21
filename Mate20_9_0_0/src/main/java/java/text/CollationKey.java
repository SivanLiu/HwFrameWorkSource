package java.text;

public abstract class CollationKey implements Comparable<CollationKey> {
    private final String source;

    public abstract int compareTo(CollationKey collationKey);

    public abstract byte[] toByteArray();

    public String getSourceString() {
        return this.source;
    }

    protected CollationKey(String source) {
        if (source != null) {
            this.source = source;
            return;
        }
        throw new NullPointerException();
    }
}
