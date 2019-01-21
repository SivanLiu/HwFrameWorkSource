package android.database;

public class CursorIndexOutOfBoundsException extends IndexOutOfBoundsException {
    public CursorIndexOutOfBoundsException(int index, int size) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Index ");
        stringBuilder.append(index);
        stringBuilder.append(" requested, with a size of ");
        stringBuilder.append(size);
        super(stringBuilder.toString());
    }

    public CursorIndexOutOfBoundsException(String message) {
        super(message);
    }
}
