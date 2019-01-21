package java.util.logging;

public class ErrorManager {
    public static final int CLOSE_FAILURE = 3;
    public static final int FLUSH_FAILURE = 2;
    public static final int FORMAT_FAILURE = 5;
    public static final int GENERIC_FAILURE = 0;
    public static final int OPEN_FAILURE = 4;
    public static final int WRITE_FAILURE = 1;
    private boolean reported = false;

    /* JADX WARNING: Missing block: B:15:0x003d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void error(String msg, Exception ex, int code) {
        if (!this.reported) {
            this.reported = true;
            String text = new StringBuilder();
            text.append("java.util.logging.ErrorManager: ");
            text.append(code);
            text = text.toString();
            if (msg != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(text);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                text = stringBuilder.toString();
            }
            System.err.println(text);
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    }
}
