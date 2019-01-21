package java.io;

public class FileNotFoundException extends IOException {
    private static final long serialVersionUID = -897856973823710492L;

    public FileNotFoundException(String s) {
        super(s);
    }

    private FileNotFoundException(String path, String reason) {
        String str;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(path);
        if (reason == null) {
            str = "";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" (");
            stringBuilder2.append(reason);
            stringBuilder2.append(")");
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        super(stringBuilder.toString());
    }
}
