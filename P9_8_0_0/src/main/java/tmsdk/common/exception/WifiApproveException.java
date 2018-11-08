package tmsdk.common.exception;

public class WifiApproveException extends Exception {
    public WifiApproveException(String str) {
        super(str);
    }

    public WifiApproveException(String str, Throwable th) {
        super(str, th);
    }

    public WifiApproveException(Throwable th) {
        super(th.getMessage(), th);
    }

    public String getErrMsg() {
        Object -l_1_R = getMessage();
        if (-l_1_R == null) {
            Object -l_2_R = getCause();
            if (-l_2_R != null) {
                -l_1_R = -l_2_R.getMessage();
            }
        }
        return -l_1_R == null ? "" : -l_1_R;
    }
}
