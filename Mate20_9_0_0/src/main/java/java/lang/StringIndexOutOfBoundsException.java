package java.lang;

public class StringIndexOutOfBoundsException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = -6762910422159637258L;

    public StringIndexOutOfBoundsException(String s) {
        super(s);
    }

    public StringIndexOutOfBoundsException(int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("String index out of range: ");
        stringBuilder.append(index);
        super(stringBuilder.toString());
    }

    StringIndexOutOfBoundsException(String s, int index) {
        this(s.length(), index);
    }

    StringIndexOutOfBoundsException(int sourceLength, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("length=");
        stringBuilder.append(sourceLength);
        stringBuilder.append("; index=");
        stringBuilder.append(index);
        super(stringBuilder.toString());
    }

    StringIndexOutOfBoundsException(String s, int offset, int count) {
        this(s.length(), offset, count);
    }

    StringIndexOutOfBoundsException(int sourceLength, int offset, int count) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("length=");
        stringBuilder.append(sourceLength);
        stringBuilder.append("; regionStart=");
        stringBuilder.append(offset);
        stringBuilder.append("; regionLength=");
        stringBuilder.append(count);
        super(stringBuilder.toString());
    }
}
