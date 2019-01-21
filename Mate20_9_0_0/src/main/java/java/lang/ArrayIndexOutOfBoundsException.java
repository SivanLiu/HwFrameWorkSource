package java.lang;

public class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = -5116101128118950844L;

    public ArrayIndexOutOfBoundsException(int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Array index out of range: ");
        stringBuilder.append(index);
        super(stringBuilder.toString());
    }

    public ArrayIndexOutOfBoundsException(String s) {
        super(s);
    }

    public ArrayIndexOutOfBoundsException(int sourceLength, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("length=");
        stringBuilder.append(sourceLength);
        stringBuilder.append("; index=");
        stringBuilder.append(index);
        super(stringBuilder.toString());
    }

    public ArrayIndexOutOfBoundsException(int sourceLength, int offset, int count) {
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
