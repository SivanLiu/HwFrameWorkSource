package libcore.internal;

public final class StringPool {
    private final String[] pool = new String[512];

    private static boolean contentEquals(String s, char[] chars, int start, int length) {
        if (s.length() != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (chars[start + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public String get(char[] array, int start, int length) {
        int i;
        int hashCode = 0;
        for (i = start; i < start + length; i++) {
            hashCode = (hashCode * 31) + array[i];
        }
        i = ((hashCode >>> 20) ^ (hashCode >>> 12)) ^ hashCode;
        hashCode = (this.pool.length - 1) & (i ^ ((i >>> 7) ^ (i >>> 4)));
        String pooled = this.pool[hashCode];
        if (pooled != null && contentEquals(pooled, array, start, length)) {
            return pooled;
        }
        String result = new String(array, start, length);
        this.pool[hashCode] = result;
        return result;
    }
}
