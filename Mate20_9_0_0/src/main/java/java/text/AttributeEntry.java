package java.text;

import java.text.AttributedCharacterIterator.Attribute;
import java.util.Map.Entry;

/* compiled from: AttributedString */
class AttributeEntry implements Entry<Attribute, Object> {
    private Attribute key;
    private Object value;

    AttributeEntry(Attribute key, Object value) {
        this.key = key;
        this.value = value;
    }

    public boolean equals(Object o) {
        boolean z = false;
        if (!(o instanceof AttributeEntry)) {
            return false;
        }
        AttributeEntry other = (AttributeEntry) o;
        if (other.key.equals(this.key) && (this.value != null ? !other.value.equals(this.value) : other.value != null)) {
            z = true;
        }
        return z;
    }

    public Attribute getKey() {
        return this.key;
    }

    public Object getValue() {
        return this.value;
    }

    public Object setValue(Object newValue) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        return this.key.hashCode() ^ (this.value == null ? 0 : this.value.hashCode());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.key.toString());
        stringBuilder.append("=");
        stringBuilder.append(this.value.toString());
        return stringBuilder.toString();
    }
}
