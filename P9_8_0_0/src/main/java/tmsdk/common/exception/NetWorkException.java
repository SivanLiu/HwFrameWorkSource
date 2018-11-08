package tmsdk.common.exception;

public class NetWorkException extends Exception {
    private int yA;

    public NetWorkException(int i, String str) {
        super(str);
        this.yA = i;
    }

    public NetWorkException(int i, String str, Throwable th) {
        super(str, th);
        this.yA = i;
    }

    public NetWorkException(int i, Throwable th) {
        super(th.getMessage(), th);
        this.yA = i;
    }

    public int getErrCode() {
        return this.yA;
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
