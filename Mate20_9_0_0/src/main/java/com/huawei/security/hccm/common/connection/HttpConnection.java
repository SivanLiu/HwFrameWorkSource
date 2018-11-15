package com.huawei.security.hccm.common.connection;

import android.support.annotation.NonNull;
import android.util.Log;
import com.huawei.security.hccm.EnrollmentException;
import com.huawei.security.hccm.common.connection.exception.MalFormedPKIMessageException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Iterator;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.x509.DisplayText;
import org.json.JSONObject;

public class HttpConnection {
    private static final String TAG = "HttpConnection";
    private HttpURLConnection mHttpConn = null;
    private String methodType;

    public static class HttpHeaders {
        public static final String AUTHENTICATION = "Authorization";
        public static final String CONNECTION = "Connection";
        public static final String CONNECTION_CLOSE = "close";
        public static final String CONNECTION_KEEP_ALIVE = "Keep-Alive";
        public static final String CONNECT_TIMEOUT = "ConnectTimeout";
        public static final String CONTENT_LANGUAGE = "Content-Language";
        public static final String CONTENT_LANGUAGE_DEFAULT = "en-US";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final int DEFAULT_TIMEOUT = 15000;
        public static final String DO_INPUT = "DoInput";
        public static final String DO_OUTPUT = "DoOutput";
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String READ_TIMEOUT = "ReadTimeout";
        public static final String X_REQUEST_ID = "X-Request-ID";
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0049  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HttpConnection(@NonNull String type) {
        Object obj;
        int hashCode = type.hashCode();
        if (hashCode == 70454) {
            if (type.equals(HttpHeaders.GET)) {
                obj = 1;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 2461856 && type.equals(HttpHeaders.POST)) {
            obj = null;
            switch (obj) {
                case null:
                    this.methodType = HttpHeaders.POST;
                    return;
                case 1:
                    this.methodType = HttpHeaders.GET;
                    return;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Supported request type are GET and POST. Invalid requested type is [");
                    stringBuilder.append(type);
                    stringBuilder.append("].");
                    throw new InvalidParameterException(stringBuilder.toString());
            }
        }
        obj = -1;
        switch (obj) {
            case null:
                break;
            case 1:
                break;
            default:
                break;
        }
    }

    public void initialize(@NonNull URL url, JSONObject config) throws IOException {
        this.mHttpConn = (HttpURLConnection) url.openConnection();
        this.mHttpConn.setRequestMethod(this.methodType);
        if (config != null) {
            setGeneralConfig(config);
        }
    }

    private void setGeneralConfig(@NonNull JSONObject config) throws ProtocolException {
        this.mHttpConn.setRequestProperty(HttpHeaders.CONTENT_TYPE, config.optString(HttpHeaders.CONTENT_TYPE));
        this.mHttpConn.setRequestProperty(HttpHeaders.CONTENT_LANGUAGE, config.optString(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LANGUAGE_DEFAULT));
        this.mHttpConn.setRequestProperty(HttpHeaders.X_REQUEST_ID, randomID(64));
        this.mHttpConn.setRequestProperty(HttpHeaders.CONNECTION, config.optString(HttpHeaders.CONNECTION, HttpHeaders.CONNECTION_CLOSE));
        this.mHttpConn.setDoInput(config.optBoolean(HttpHeaders.DO_INPUT));
        this.mHttpConn.setDoOutput(config.optBoolean(HttpHeaders.DO_OUTPUT));
        this.mHttpConn.setConnectTimeout(config.optInt(HttpHeaders.CONNECT_TIMEOUT, HttpHeaders.DEFAULT_TIMEOUT));
        this.mHttpConn.setReadTimeout(config.optInt(HttpHeaders.READ_TIMEOUT, HttpHeaders.DEFAULT_TIMEOUT));
    }

    public void setUserConfig(@NonNull JSONObject config) {
        if (config != null) {
            Iterator<?> keys = config.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (!config.optString(key).isEmpty()) {
                    this.mHttpConn.setRequestProperty(key, config.optString(key));
                }
            }
        }
    }

    public void setHeaderProperty(String key, String value) {
        this.mHttpConn.setRequestProperty(key, value);
    }

    public byte[] send(byte[] request) throws MalFormedPKIMessageException, EnrollmentException {
        InputStream is = null;
        OutputStream os = null;
        String str;
        StringBuilder stringBuilder;
        try {
            os = this.mHttpConn.getOutputStream();
            os.write(request);
            os.close();
            int status = this.mHttpConn.getResponseCode();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("connection status is ");
            stringBuilder.append(status);
            Log.d(str, stringBuilder.toString());
            if (status == 403) {
                throw new MalFormedPKIMessageException("the public key of the TBSCertificate is not the same as that of the attestation certificate!", status);
            } else if (status == 401) {
                throw new MalFormedPKIMessageException("Invalid AT in HTTP header!", status);
            } else if (status == DisplayText.DISPLAY_TEXT_MAXIMUM_SIZE) {
                is = this.mHttpConn.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[PKIFailureInfo.certRevoked];
                while (true) {
                    int read = is.read(buffer);
                    int len = read;
                    if (read == -1) {
                        break;
                    }
                    bos.write(buffer, 0, len);
                }
                is.close();
                byte[] response = bos.toByteArray();
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Fail to close a stream.");
                    }
                }
                if (is != null) {
                    is.close();
                }
                return response;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown HTTP status: ");
                stringBuilder.append(status);
                throw new MalFormedPKIMessageException(stringBuilder.toString(), status);
            }
        } catch (IOException ioe) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("connect failed");
            stringBuilder.append(ioe.getMessage());
            Log.e(str, stringBuilder.toString());
            throw new EnrollmentException("Connect failed", -11);
        } catch (Throwable th) {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e2) {
                    Log.w(TAG, "Fail to close a stream.");
                }
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private String randomID(int length) {
        String ALPHA_NUMERIC_STRING = new StringBuilder();
        ALPHA_NUMERIC_STRING.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        ALPHA_NUMERIC_STRING.append("abcdefghijklmnopqrstuvwxyz");
        ALPHA_NUMERIC_STRING.append("0123456789");
        ALPHA_NUMERIC_STRING.append(".-_");
        ALPHA_NUMERIC_STRING = ALPHA_NUMERIC_STRING.toString();
        StringBuilder builder = new StringBuilder();
        while (true) {
            int length2 = length - 1;
            if (length == 0) {
                return builder.toString();
            }
            builder.append(ALPHA_NUMERIC_STRING.charAt((int) (Math.random() * ((double) ALPHA_NUMERIC_STRING.length()))));
            length = length2;
        }
    }
}
