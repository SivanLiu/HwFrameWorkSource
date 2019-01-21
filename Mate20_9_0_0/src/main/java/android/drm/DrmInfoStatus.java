package android.drm;

public class DrmInfoStatus {
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_OK = 1;
    public final ProcessedData data;
    public final int infoType;
    public final String mimeType;
    public final int statusCode;

    public DrmInfoStatus(int statusCode, int infoType, ProcessedData data, String mimeType) {
        StringBuilder stringBuilder;
        if (!DrmInfoRequest.isValidType(infoType)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("infoType: ");
            stringBuilder.append(infoType);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!isValidStatusCode(statusCode)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported status code: ");
            stringBuilder.append(statusCode);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (mimeType == null || mimeType == "") {
            throw new IllegalArgumentException("mimeType is null or an empty string");
        } else {
            this.statusCode = statusCode;
            this.infoType = infoType;
            this.data = data;
            this.mimeType = mimeType;
        }
    }

    private boolean isValidStatusCode(int statusCode) {
        return statusCode == 1 || statusCode == 2;
    }
}
