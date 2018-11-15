package junit.framework;

public class ComparisonCompactor {
    private static final String DELTA_END = "]";
    private static final String DELTA_START = "[";
    private static final String ELLIPSIS = "...";
    private String fActual;
    private int fContextLength;
    private String fExpected;
    private int fPrefix;
    private int fSuffix;

    public ComparisonCompactor(int contextLength, String expected, String actual) {
        this.fContextLength = contextLength;
        this.fExpected = expected;
        this.fActual = actual;
    }

    public String compact(String message) {
        if (this.fExpected == null || this.fActual == null || areStringsEqual()) {
            return Assert.format(message, this.fExpected, this.fActual);
        }
        findCommonPrefix();
        findCommonSuffix();
        return Assert.format(message, compactString(this.fExpected), compactString(this.fActual));
    }

    private String compactString(String source) {
        StringBuilder stringBuilder;
        String result = new StringBuilder();
        result.append(DELTA_START);
        result.append(source.substring(this.fPrefix, (source.length() - this.fSuffix) + 1));
        result.append(DELTA_END);
        result = result.toString();
        if (this.fPrefix > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(computeCommonPrefix());
            stringBuilder.append(result);
            result = stringBuilder.toString();
        }
        if (this.fSuffix <= 0) {
            return result;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(result);
        stringBuilder.append(computeCommonSuffix());
        return stringBuilder.toString();
    }

    private void findCommonPrefix() {
        this.fPrefix = 0;
        int end = Math.min(this.fExpected.length(), this.fActual.length());
        while (this.fPrefix < end && this.fExpected.charAt(this.fPrefix) == this.fActual.charAt(this.fPrefix)) {
            this.fPrefix++;
        }
    }

    private void findCommonSuffix() {
        int expectedSuffix = this.fExpected.length() - 1;
        int actualSuffix = this.fActual.length() - 1;
        while (actualSuffix >= this.fPrefix && expectedSuffix >= this.fPrefix && this.fExpected.charAt(expectedSuffix) == this.fActual.charAt(actualSuffix)) {
            actualSuffix--;
            expectedSuffix--;
        }
        this.fSuffix = this.fExpected.length() - expectedSuffix;
    }

    private String computeCommonPrefix() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.fPrefix > this.fContextLength ? ELLIPSIS : "");
        stringBuilder.append(this.fExpected.substring(Math.max(0, this.fPrefix - this.fContextLength), this.fPrefix));
        return stringBuilder.toString();
    }

    private String computeCommonSuffix() {
        int end = Math.min(((this.fExpected.length() - this.fSuffix) + 1) + this.fContextLength, this.fExpected.length());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.fExpected.substring((this.fExpected.length() - this.fSuffix) + 1, end));
        stringBuilder.append((this.fExpected.length() - this.fSuffix) + 1 < this.fExpected.length() - this.fContextLength ? ELLIPSIS : "");
        return stringBuilder.toString();
    }

    private boolean areStringsEqual() {
        return this.fExpected.equals(this.fActual);
    }
}
