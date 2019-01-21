package sun.security.provider.certpath;

import java.security.cert.X509Certificate;

public class BuildStep {
    public static final int BACK = 2;
    public static final int FAIL = 4;
    public static final int FOLLOW = 3;
    public static final int POSSIBLE = 1;
    public static final int SUCCEED = 5;
    private X509Certificate cert;
    private int result;
    private Throwable throwable;
    private Vertex vertex;

    public BuildStep(Vertex vtx, int res) {
        this.vertex = vtx;
        if (this.vertex != null) {
            this.cert = this.vertex.getCertificate();
            this.throwable = this.vertex.getThrowable();
        }
        this.result = res;
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    public X509Certificate getCertificate() {
        return this.cert;
    }

    public String getIssuerName() {
        return getIssuerName(null);
    }

    public String getIssuerName(String defaultName) {
        if (this.cert == null) {
            return defaultName;
        }
        return this.cert.getIssuerX500Principal().toString();
    }

    public String getSubjectName() {
        return getSubjectName(null);
    }

    public String getSubjectName(String defaultName) {
        if (this.cert == null) {
            return defaultName;
        }
        return this.cert.getSubjectX500Principal().toString();
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

    public int getResult() {
        return this.result;
    }

    public String resultToString(int res) {
        String resultString = "";
        switch (res) {
            case 1:
                return "Certificate to be tried.\n";
            case 2:
                return "Certificate backed out since path does not satisfy build requirements.\n";
            case 3:
                return "Certificate satisfies conditions.\n";
            case 4:
                return "Certificate backed out since path does not satisfy conditions.\n";
            case 5:
                return "Certificate satisfies conditions.\n";
            default:
                return "Internal error: Invalid step result value.\n";
        }
    }

    public String toString() {
        String out = "Internal Error\n";
        switch (this.result) {
            case 1:
            case 3:
            case 5:
                return resultToString(this.result);
            case 2:
            case 4:
                out = resultToString(this.result);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(out);
                stringBuilder.append(this.vertex.throwableToString());
                return stringBuilder.toString();
            default:
                return "Internal Error: Invalid step result\n";
        }
    }

    public String verboseToString() {
        StringBuilder stringBuilder;
        String out = resultToString(getResult());
        switch (this.result) {
            case 2:
            case 4:
                stringBuilder = new StringBuilder();
                stringBuilder.append(out);
                stringBuilder.append(this.vertex.throwableToString());
                out = stringBuilder.toString();
                break;
            case 3:
            case 5:
                stringBuilder = new StringBuilder();
                stringBuilder.append(out);
                stringBuilder.append(this.vertex.moreToString());
                out = stringBuilder.toString();
                break;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("Certificate contains:\n");
        stringBuilder.append(this.vertex.certToString());
        return stringBuilder.toString();
    }

    public String fullToString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(resultToString(getResult()));
        stringBuilder.append(this.vertex.toString());
        return stringBuilder.toString();
    }
}
