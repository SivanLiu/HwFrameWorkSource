package android.net.captiveportal;

public final class CaptivePortalProbeResult {
    public static final CaptivePortalProbeResult FAILED = new CaptivePortalProbeResult(FAILED_CODE);
    public static final int FAILED_CODE = 599;
    public static final int PORTAL_CODE = 302;
    public static final CaptivePortalProbeResult SUCCESS = new CaptivePortalProbeResult(204);
    public static final int SUCCESS_CODE = 204;
    public final String detectUrl;
    public int mHttpResponseCode;
    public final CaptivePortalProbeSpec probeSpec;
    public final String redirectUrl;

    public CaptivePortalProbeResult(int httpResponseCode) {
        this(httpResponseCode, null, null);
    }

    public CaptivePortalProbeResult(int httpResponseCode, String redirectUrl, String detectUrl) {
        this(httpResponseCode, redirectUrl, detectUrl, null);
    }

    public CaptivePortalProbeResult(int httpResponseCode, String redirectUrl, String detectUrl, CaptivePortalProbeSpec probeSpec) {
        this.mHttpResponseCode = httpResponseCode;
        this.redirectUrl = redirectUrl;
        this.detectUrl = detectUrl;
        this.probeSpec = probeSpec;
    }

    public boolean isSuccessful() {
        return this.mHttpResponseCode == 204;
    }

    public boolean isPortal() {
        return !isSuccessful() && this.mHttpResponseCode >= 200 && this.mHttpResponseCode <= 399;
    }

    public boolean isFailed() {
        return (isSuccessful() || isPortal()) ? false : true;
    }
}
