package com.huawei.nb.coordinator.helper.http;

import android.text.TextUtils;
import com.huawei.nb.coordinator.helper.DataRequestListener;
import com.huawei.nb.coordinator.helper.FileDataRequestListener;
import com.huawei.nb.coordinator.helper.RefreshDataRequestListener;
import com.huawei.nb.coordinator.helper.RefreshResult;
import com.huawei.nb.coordinator.helper.verify.IVerifyVar;
import com.huawei.nb.coordinator.json.PackageMeta;
import com.huawei.nb.coordinator.json.PackageMeta.PackagesBean;
import com.huawei.nb.io.IOUtils;
import com.huawei.nb.io.LineIterator;
import com.huawei.nb.model.coordinator.CoordinatorAudit;
import com.huawei.nb.utils.JsonUtils;
import com.huawei.nb.utils.file.FileUtils;
import com.huawei.nb.utils.logger.DSLog;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;

public class HttpClient {
    private static final int BUFFER_SIZE = 1024;
    public static final int DEFAULT_REQUEST = 0;
    public static final String DELETE_TYPE = "DELETE";
    public static final int FRAGMENT_LOAD_REQUEST = 1;
    public static final String GET_TYPE = "GET";
    private static final String HTTPS_TYPE = "https";
    private static final String HTTP_TYPE = "http";
    private static final long MAX_DEFAULT_SIZE = 524288000;
    private static final int MAX_REQUESTBODY_LENGTH = 819200;
    private static final int MAX_URL_LENGTH = 8192;
    public static final String POST_TYPE = "POST";
    private static final String TAG = "HttpClient";
    public static final int TRANSFER_FILE_REQUEST = 2;
    private String baseURL;
    private Long dataTrafficSize;
    private String fileName;
    private String fileSavePath;
    private DataRequestListener mDataRequestListener;
    private HttpResponse mResponse;
    private HttpRequest request;
    private String requestBody;
    private Map<String, String> requestHeaders;
    private String requestMethod;
    private int requestMode;

    public HttpClient() {
        this.baseURL = null;
        this.requestBody = null;
        this.requestMethod = null;
        this.request = null;
        this.requestHeaders = null;
        this.mResponse = new HttpResponse();
        this.dataTrafficSize = Long.valueOf(MAX_DEFAULT_SIZE);
        this.requestMode = 0;
    }

    public HttpClient(int requestMode, String fileSavePath, String fileName) {
        this.baseURL = null;
        this.requestBody = null;
        this.requestMethod = null;
        this.request = null;
        this.requestHeaders = null;
        this.mResponse = new HttpResponse();
        this.dataTrafficSize = Long.valueOf(MAX_DEFAULT_SIZE);
        this.requestMode = requestMode;
        this.fileSavePath = fileSavePath;
        this.fileName = fileName;
    }

    public HttpClient setDataTrafficSize(Long dataTrafficSize) {
        this.dataTrafficSize = dataTrafficSize;
        return this;
    }

    public HttpClient setDataRequestListener(DataRequestListener dataRequestListener) {
        this.mDataRequestListener = dataRequestListener;
        return this;
    }

    private boolean isFileParamsValid() throws IOException {
        if (TextUtils.isEmpty(this.fileName) || TextUtils.isEmpty(this.fileSavePath)) {
            setErrorHttpResponse(-5, "HttpClient FileSavePath or fileName is empty !");
            return false;
        }
        File savePath = new File(this.fileSavePath);
        if (savePath.exists() && savePath.isDirectory()) {
            return createFile(this.fileSavePath, this.fileName);
        }
        if (savePath.exists()) {
            setErrorHttpResponse(-5, "HttpClient FileSavePath is error !");
            return false;
        } else if (savePath.mkdirs()) {
            return createFile(this.fileSavePath, this.fileName);
        } else {
            setErrorHttpResponse(-5, "HttpClient Create Save Path error !");
            return false;
        }
    }

    private boolean createDirectory(String path) throws IOException {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = FileUtils.getFile(path);
        if (file.exists()) {
            if (!file.isDirectory()) {
                setErrorHttpResponse(-5, "HttpClient FileSavePath is error");
                return false;
            }
        } else if (!file.mkdirs()) {
            setErrorHttpResponse(-5, "HttpClient Create Save Path error !");
            return false;
        }
        return true;
    }

    private boolean createFile(String parentPath, String fileName) throws IOException {
        if (!TextUtils.isEmpty(fileName)) {
            return createFile(FileUtils.getFile(parentPath, fileName));
        }
        DSLog.e("HttpClient fileName is empty!", new Object[0]);
        return false;
    }

    private boolean deleteDir(File dir) {
        if (dir == null) {
            return false;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                int i = 0;
                while (i < children.length) {
                    if (deleteDir(new File(dir, children[i]))) {
                        i++;
                    } else {
                        DSLog.e(children[i] + " delete failed.", new Object[0]);
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    private boolean createFile(File file) {
        if (file == null) {
            DSLog.e("HttpClient file is null.", new Object[0]);
            return false;
        }
        if (file.exists()) {
            boolean deleteResult;
            if (file.isDirectory()) {
                deleteResult = deleteDir(file);
            } else {
                deleteResult = file.delete();
            }
            if (!deleteResult) {
                DSLog.e("HttpClient Delete old file error !", new Object[0]);
                return false;
            }
        }
        try {
            if (file.createNewFile()) {
                return true;
            }
            DSLog.e("HttpClient Create file error !", new Object[0]);
            return false;
        } catch (IOException e) {
            this.mResponse.setHttpExceptionMsg(e.getMessage());
            DSLog.e("HttpClient Create file error:" + e.getMessage() + "!", new Object[0]);
            return false;
        }
    }

    public HttpResponse syncExecute() {
        return syncExecute(null);
    }

    public HttpResponse syncExecute(CoordinatorAudit coordinatorAudit) {
        HttpURLConnection connection = null;
        try {
            URL url = getUrl();
            HttpResponse errorHttpResponse;
            if (url == null) {
                errorHttpResponse = setErrorHttpResponse(-2, "HttpClient url may be too long. Request stop.");
                if (connection == null) {
                    return errorHttpResponse;
                }
                connection.disconnect();
                return errorHttpResponse;
            } else if (isRequestBodyValid()) {
                this.mResponse.setUrl(url.toString());
                URLConnection urlConnection = url.openConnection();
                if (this.baseURL.startsWith(HTTPS_TYPE)) {
                    if (urlConnection instanceof HttpsURLConnection) {
                        connection = (HttpsURLConnection) urlConnection;
                    }
                } else if (!this.baseURL.startsWith(HTTP_TYPE)) {
                    errorHttpResponse = setErrorHttpResponse(-2, " connection type is illegal. Request stop.");
                    if (connection == null) {
                        return errorHttpResponse;
                    }
                    connection.disconnect();
                    return errorHttpResponse;
                } else if (urlConnection instanceof HttpURLConnection) {
                    connection = (HttpURLConnection) urlConnection;
                }
                if (connection == null) {
                    errorHttpResponse = setErrorHttpResponse(-2, " connection is illegal. Request stop.");
                    if (connection == null) {
                        return errorHttpResponse;
                    }
                    connection.disconnect();
                    return errorHttpResponse;
                }
                initConnection(connection);
                if (this.requestMethod.equals(POST_TYPE)) {
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    addPostRequestBody(connection);
                }
                switch (this.requestMode) {
                    case 0:
                        executeResponse(connection, coordinatorAudit);
                        break;
                    case 1:
                        executeResponseForFragmentLoad(connection, coordinatorAudit);
                        break;
                    case 2:
                        if (isFileParamsValid()) {
                            executeResponseForTransferFile(connection, coordinatorAudit);
                            break;
                        }
                        break;
                    default:
                        DSLog.e("HttpClient requestMode:" + this.requestMode + " is not exist!", new Object[0]);
                        break;
                }
                if (connection != null) {
                    connection.disconnect();
                }
                return this.mResponse;
            } else {
                errorHttpResponse = setErrorHttpResponse(-2, "HttpClient request body may be too long. Request stop.");
                if (connection == null) {
                    return errorHttpResponse;
                }
                connection.disconnect();
                return errorHttpResponse;
            }
        } catch (IOException e) {
            DSLog.e("HttpClient executeResponse errorMsg:" + e.getMessage(), new Object[0]);
            this.mResponse.setHttpExceptionMsg(e.getMessage());
            setErrorHttpResponse(-5, "HttpClient caught IOException, error message:" + e.getMessage());
            if (connection != null) {
                connection.disconnect();
            }
            if (connection != null) {
                connection.disconnect();
            }
        } catch (Throwable th) {
            if (connection != null) {
                connection.disconnect();
            }
            throw th;
        }
    }

    public HttpClient newCall(HttpRequest request) {
        this.baseURL = request.getUrl();
        this.request = request;
        this.requestBody = request.getRequestBodyString();
        this.requestHeaders = request.getRequestHeaders();
        this.requestMethod = request.getRequestMethod();
        return this;
    }

    private URL getUrl() throws MalformedURLException {
        if (!GET_TYPE.equals(this.requestMethod) || TextUtils.isEmpty(this.requestBody)) {
            return new URL(this.baseURL);
        }
        if ((this.baseURL + "?" + this.requestBody).length() > MAX_URL_LENGTH) {
            return null;
        }
        return new URL(this.baseURL + "?" + this.requestBody);
    }

    private void initConnection(HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod(this.requestMethod);
        connection.setConnectTimeout(this.request.getConnectTimeout());
        connection.setReadTimeout(this.request.getReadTimeout());
        for (Entry<String, String> entry : this.requestHeaders.entrySet()) {
            connection.setRequestProperty(checkHeader((String) entry.getKey()), checkHeader((String) entry.getValue()));
        }
    }

    private String checkHeader(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value.replace(IOUtils.LINE_SEPARATOR_UNIX, "").replace("\r", "");
    }

    private void addPostRequestBody(HttpURLConnection connection) {
        IOException e;
        Throwable th;
        OutputStream bos = null;
        OutputStream outputStream = null;
        if (!TextUtils.isEmpty(this.requestBody)) {
            try {
                outputStream = connection.getOutputStream();
                if (isOutputStreamEmpty(outputStream)) {
                    IOUtils.closeQuietly(outputStream);
                    IOUtils.closeQuietly(null);
                    return;
                }
                OutputStream bos2 = new BufferedOutputStream(outputStream);
                try {
                    bos2.write(this.requestBody.getBytes("utf-8"));
                    bos2.flush();
                    IOUtils.closeQuietly(outputStream);
                    IOUtils.closeQuietly(bos2);
                    bos = bos2;
                } catch (IOException e2) {
                    e = e2;
                    bos = bos2;
                    try {
                        DSLog.e("HttpClient addPostRequestBody IOException : " + e.getMessage(), new Object[0]);
                        this.mResponse.setHttpExceptionMsg(e.getMessage());
                        IOUtils.closeQuietly(outputStream);
                        IOUtils.closeQuietly(bos);
                    } catch (Throwable th2) {
                        th = th2;
                        IOUtils.closeQuietly(outputStream);
                        IOUtils.closeQuietly(bos);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    bos = bos2;
                    IOUtils.closeQuietly(outputStream);
                    IOUtils.closeQuietly(bos);
                    throw th;
                }
            } catch (IOException e3) {
                e = e3;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:50:0x0101  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void executeResponse(HttpURLConnection connection, CoordinatorAudit coordinatorAudit) {
        IOException e;
        Throwable th;
        InputStream is = null;
        Reader reader = null;
        try {
            int responseCode = connection.getResponseCode();
            String responseMessage = fullfillEmptyMessage(connection.getResponseMessage());
            this.mResponse.setResponseMsg(responseMessage);
            if (responseCode != 200) {
                setErrorHttpResponse(responseCode, responseMessage);
            } else if (isContentLengthValid(connection, coordinatorAudit)) {
                is = connection.getInputStream();
                if (isInputStreamEmpty(0, is)) {
                    IOUtils.closeQuietly(null);
                    IOUtils.closeQuietly(is);
                    if (connection != null) {
                        connection.disconnect();
                        return;
                    }
                    return;
                }
                Reader reader2 = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                try {
                    StringBuilder responseStr = new StringBuilder();
                    LineIterator lineIterator = new LineIterator(reader2);
                    long amountLengthByte = 0;
                    while (lineIterator.hasNext()) {
                        String line = lineIterator.next();
                        responseStr.append(line);
                        amountLengthByte += (long) line.getBytes(StandardCharsets.UTF_8).length;
                        if (amountLengthByte > this.dataTrafficSize.longValue()) {
                            throw new IOException(" response length is larger than dataTrafficeSize. dataTrafficeSize = " + this.dataTrafficSize + ", response length =" + amountLengthByte);
                        }
                    }
                    this.mResponse.setResponseSize(amountLengthByte);
                    String responseString = responseStr.toString();
                    this.mResponse.setStatusCode(responseCode);
                    this.mResponse.setResponseString(responseString);
                    reader = reader2;
                } catch (IOException e2) {
                    e = e2;
                    reader = reader2;
                    try {
                        if (TextUtils.isEmpty(e.getMessage())) {
                            setErrorHttpResponse(-5, "IO Exception: message is empty.");
                        } else if (e.getMessage().startsWith("Unable to resolve host")) {
                            setErrorHttpResponse(-6, e.getMessage());
                        } else if (e.getMessage().startsWith("failed to connect to") || e.getMessage().startsWith("Failed to connect to")) {
                            setErrorHttpResponse(-9, " connect cloud error in response : " + e.getMessage());
                        } else if (e.getMessage().startsWith(IVerifyVar.TIME_OUT_HEADER)) {
                            setErrorHttpResponse(-13, " timeout in response : " + e.getMessage());
                        } else {
                            setErrorHttpResponse(-5, "executeResponse IOException :" + e.getMessage());
                        }
                        IOUtils.closeQuietly(reader);
                        IOUtils.closeQuietly(is);
                        if (connection != null) {
                            connection.disconnect();
                            return;
                        }
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        IOUtils.closeQuietly(reader);
                        IOUtils.closeQuietly(is);
                        if (connection != null) {
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    reader = reader2;
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(is);
                    if (connection != null) {
                        connection.disconnect();
                    }
                    throw th;
                }
            } else {
                IOUtils.closeQuietly(null);
                IOUtils.closeQuietly(null);
                if (connection != null) {
                    connection.disconnect();
                    return;
                }
                return;
            }
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(is);
            if (connection != null) {
                connection.disconnect();
            }
        } catch (IOException e3) {
            e = e3;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:93:0x0439  */
    /* JADX WARNING: Removed duplicated region for block: B:165:0x07a0 A:{SYNTHETIC, Splitter:B:165:0x07a0} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x044e  */
    /* JADX WARNING: Missing block: B:99:?, code skipped:
            r26.mResponse.setResponseSize((long) r19);
            r6.flush();
            r26.mResponse.setStatusCode(r17);
     */
    /* JADX WARNING: Missing block: B:100:0x0484, code skipped:
            if (r26.mDataRequestListener == null) goto L_0x04b8;
     */
    /* JADX WARNING: Missing block: B:102:0x0492, code skipped:
            if ((r26.mDataRequestListener instanceof com.huawei.nb.coordinator.helper.RefreshDataRequestListener) == false) goto L_0x04b8;
     */
    /* JADX WARNING: Missing block: B:103:0x0494, code skipped:
            r16.setDownloadedSize((long) r19);
            r16.setFinished(true);
            ((com.huawei.nb.coordinator.helper.RefreshDataRequestListener) r26.mDataRequestListener).onRefresh(r16);
     */
    /* JADX WARNING: Missing block: B:104:0x04b8, code skipped:
            r5 = r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void executeResponseForTransferFile(HttpURLConnection connection, CoordinatorAudit coordinatorAudit) {
        IOException e;
        Throwable th;
        InputStream is = null;
        OutputStream os = null;
        OutputStream bos = null;
        int totalLength = 0;
        RefreshResult refreshResult = new RefreshResult();
        try {
            String lengthValue;
            File file = FileUtils.getFile(this.fileSavePath, this.fileName);
            int responseCode = connection.getResponseCode();
            String responseMessage = fullfillEmptyMessage(connection.getResponseMessage());
            DSLog.d("HttpClient response code of file's transfer : " + responseCode, new Object[0]);
            DSLog.d("HttpClient response message of file's transfer : " + responseMessage, new Object[0]);
            this.mResponse.setResponseMsg(responseMessage);
            if (responseCode != 200) {
                this.mResponse.setStatusCode(responseCode);
            } else if (isContentLengthValid(connection, coordinatorAudit)) {
                is = connection.getInputStream();
                if (isInputStreamEmpty(2, is)) {
                    IOUtils.closeQuietly(null);
                    IOUtils.closeQuietly(null);
                    IOUtils.closeQuietly(is);
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (this.mResponse.getStatusCode() == -7) {
                        DSLog.w("HttpClient do not check file's length in executeResponseForTransferFile-finally with interrupt by user.", new Object[0]);
                        return;
                    }
                    try {
                        lengthValue = this.mResponse.getHeaderValue("Content-Length");
                        if (TextUtils.isEmpty(lengthValue)) {
                            DSLog.e("HttpClient Response content-length is empty.", new Object[0]);
                            return;
                        }
                        file = FileUtils.getFile(this.fileSavePath, this.fileName);
                        if (file.exists()) {
                            try {
                                if (file.length() != Long.parseLong(lengthValue)) {
                                    this.mResponse.setStatusCode(-5);
                                    this.mResponse.setResponseMsg("File size error!File size:" + file.length() + ",Total size:" + lengthValue);
                                    return;
                                }
                                this.mResponse.setResponseString("{\"data\":\"File download success!File size:" + file.length() + ",Total size:" + lengthValue + "\"}");
                                return;
                            } catch (NumberFormatException e2) {
                                setErrorHttpResponse(-5, " Fail to parse content-length. error: " + e2.getMessage());
                                return;
                            }
                        }
                        return;
                    } catch (IOException e3) {
                        setErrorHttpResponse(-5, " Get File IOException : " + e3.getMessage());
                        return;
                    }
                }
                this.mResponse.setDownloadStart(true);
                this.mResponse.setHeaderFields(connection.getHeaderFields());
                os = getSafeOutputStream(file, false);
                OutputStream bos2 = new BufferedOutputStream(os);
                try {
                    refreshResult.setFinished(false);
                    refreshResult.setDeltaSize(0);
                    refreshResult.setIndex(0);
                    byte[] bb = new byte[BUFFER_SIZE];
                    while (true) {
                        int len = is.read(bb);
                        if (len == -1) {
                            break;
                        }
                        bos2.write(bb, 0, len);
                        totalLength += len;
                        if (((long) totalLength) > this.dataTrafficSize.longValue()) {
                            throw new IOException("file'size is overlarge than " + this.dataTrafficSize + " totalLength = " + totalLength);
                        } else if (this.mDataRequestListener != null && (this.mDataRequestListener instanceof RefreshDataRequestListener)) {
                            refreshResult.setDeltaSize((long) len);
                            refreshResult.increaseIndex();
                            refreshResult.setDownloadedSize((long) totalLength);
                            if (!((RefreshDataRequestListener) this.mDataRequestListener).onRefresh(refreshResult)) {
                                String msg = "downloading will be stop from refresh. Downloaded length:" + totalLength;
                                setErrorHttpResponse(-7, msg);
                                throw new IOException(msg);
                            }
                        }
                    }
                } catch (IOException e4) {
                    e3 = e4;
                    bos = bos2;
                    try {
                        if (TextUtils.isEmpty(e3.getMessage())) {
                            setErrorHttpResponse(-2, " IOException, msg is empty.");
                        } else if (e3.getMessage().startsWith("Unable to resolve host")) {
                            setErrorHttpResponse(-6, " Network error: Unable to resolve host. ");
                        } else if (e3.getMessage().startsWith("failed to connect to") || e3.getMessage().startsWith("Failed to connect to")) {
                            setErrorHttpResponse(-9, " connect cloud error for transfer file : " + e3.getMessage());
                        } else if (e3.getMessage().startsWith(IVerifyVar.TIME_OUT_HEADER)) {
                            setErrorHttpResponse(-13, "timeout for transfer file : " + e3.getMessage());
                        } else {
                            this.mResponse.setHttpExceptionMsg(e3.getMessage());
                            DSLog.e("HttpClient executeResponseForTransferFile IOException : " + e3.getMessage(), new Object[0]);
                        }
                        IOUtils.closeQuietly(bos);
                        IOUtils.closeQuietly(os);
                        IOUtils.closeQuietly(is);
                        if (connection != null) {
                            connection.disconnect();
                        }
                        if (this.mResponse.getStatusCode() == -7) {
                            DSLog.w("HttpClient do not check file's length in executeResponseForTransferFile-finally with interrupt by user.", new Object[0]);
                            return;
                        }
                        try {
                            lengthValue = this.mResponse.getHeaderValue("Content-Length");
                            if (TextUtils.isEmpty(lengthValue)) {
                                DSLog.e("HttpClient Response content-length is empty.", new Object[0]);
                                return;
                            }
                            file = FileUtils.getFile(this.fileSavePath, this.fileName);
                            if (file.exists()) {
                                try {
                                    if (file.length() != Long.parseLong(lengthValue)) {
                                        this.mResponse.setStatusCode(-5);
                                        this.mResponse.setResponseMsg("File size error!File size:" + file.length() + ",Total size:" + lengthValue);
                                        return;
                                    }
                                    this.mResponse.setResponseString("{\"data\":\"File download success!File size:" + file.length() + ",Total size:" + lengthValue + "\"}");
                                    return;
                                } catch (NumberFormatException e22) {
                                    setErrorHttpResponse(-5, " Fail to parse content-length. error: " + e22.getMessage());
                                    return;
                                }
                            }
                            return;
                        } catch (IOException e32) {
                            setErrorHttpResponse(-5, " Get File IOException : " + e32.getMessage());
                            return;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        IOUtils.closeQuietly(bos);
                        IOUtils.closeQuietly(os);
                        IOUtils.closeQuietly(is);
                        if (connection != null) {
                            connection.disconnect();
                        }
                        if (this.mResponse.getStatusCode() != -7) {
                            DSLog.w("HttpClient do not check file's length in executeResponseForTransferFile-finally with interrupt by user.", new Object[0]);
                        } else {
                            try {
                                lengthValue = this.mResponse.getHeaderValue("Content-Length");
                                if (TextUtils.isEmpty(lengthValue)) {
                                    DSLog.e("HttpClient Response content-length is empty.", new Object[0]);
                                } else {
                                    file = FileUtils.getFile(this.fileSavePath, this.fileName);
                                    if (file.exists()) {
                                        try {
                                            if (file.length() != Long.parseLong(lengthValue)) {
                                                this.mResponse.setStatusCode(-5);
                                                this.mResponse.setResponseMsg("File size error!File size:" + file.length() + ",Total size:" + lengthValue);
                                            } else {
                                                this.mResponse.setResponseString("{\"data\":\"File download success!File size:" + file.length() + ",Total size:" + lengthValue + "\"}");
                                            }
                                        } catch (NumberFormatException e222) {
                                            setErrorHttpResponse(-5, " Fail to parse content-length. error: " + e222.getMessage());
                                        }
                                    }
                                }
                            } catch (IOException e322) {
                                setErrorHttpResponse(-5, " Get File IOException : " + e322.getMessage());
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    bos = bos2;
                    IOUtils.closeQuietly(bos);
                    IOUtils.closeQuietly(os);
                    IOUtils.closeQuietly(is);
                    if (connection != null) {
                    }
                    if (this.mResponse.getStatusCode() != -7) {
                    }
                    throw th;
                }
            } else {
                IOUtils.closeQuietly(null);
                IOUtils.closeQuietly(null);
                IOUtils.closeQuietly(null);
                if (connection != null) {
                    connection.disconnect();
                }
                if (this.mResponse.getStatusCode() == -7) {
                    DSLog.w("HttpClient do not check file's length in executeResponseForTransferFile-finally with interrupt by user.", new Object[0]);
                    return;
                }
                try {
                    lengthValue = this.mResponse.getHeaderValue("Content-Length");
                    if (TextUtils.isEmpty(lengthValue)) {
                        DSLog.e("HttpClient Response content-length is empty.", new Object[0]);
                        return;
                    }
                    file = FileUtils.getFile(this.fileSavePath, this.fileName);
                    if (file.exists()) {
                        try {
                            if (file.length() != Long.parseLong(lengthValue)) {
                                this.mResponse.setStatusCode(-5);
                                this.mResponse.setResponseMsg("File size error!File size:" + file.length() + ",Total size:" + lengthValue);
                                return;
                            }
                            this.mResponse.setResponseString("{\"data\":\"File download success!File size:" + file.length() + ",Total size:" + lengthValue + "\"}");
                            return;
                        } catch (NumberFormatException e2222) {
                            setErrorHttpResponse(-5, " Fail to parse content-length. error: " + e2222.getMessage());
                            return;
                        }
                    }
                    return;
                } catch (IOException e3222) {
                    setErrorHttpResponse(-5, " Get File IOException : " + e3222.getMessage());
                    return;
                }
            }
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
            if (connection != null) {
                connection.disconnect();
            }
            if (this.mResponse.getStatusCode() == -7) {
                DSLog.w("HttpClient do not check file's length in executeResponseForTransferFile-finally with interrupt by user.", new Object[0]);
                return;
            }
            try {
                lengthValue = this.mResponse.getHeaderValue("Content-Length");
                if (TextUtils.isEmpty(lengthValue)) {
                    DSLog.e("HttpClient Response content-length is empty.", new Object[0]);
                    return;
                }
                file = FileUtils.getFile(this.fileSavePath, this.fileName);
                if (file.exists()) {
                    try {
                        if (file.length() != Long.parseLong(lengthValue)) {
                            this.mResponse.setStatusCode(-5);
                            this.mResponse.setResponseMsg("File size error!File size:" + file.length() + ",Total size:" + lengthValue);
                            return;
                        }
                        this.mResponse.setResponseString("{\"data\":\"File download success!File size:" + file.length() + ",Total size:" + lengthValue + "\"}");
                    } catch (NumberFormatException e22222) {
                        setErrorHttpResponse(-5, " Fail to parse content-length. error: " + e22222.getMessage());
                    }
                }
            } catch (IOException e32222) {
                setErrorHttpResponse(-5, " Get File IOException : " + e32222.getMessage());
            }
        } catch (IOException e5) {
            e32222 = e5;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:148:0x037e  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0261  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void executeResponseForFragmentLoad(HttpURLConnection connection, CoordinatorAudit coordinatorAudit) {
        IOException e;
        Throwable th;
        InputStream is = null;
        InputStream dis = null;
        try {
            if (createDirectory(this.fileSavePath)) {
                int responseCode = connection.getResponseCode();
                this.mResponse.setResponseMsg(fullfillEmptyMessage(connection.getResponseMessage()));
                if (responseCode != 200) {
                    this.mResponse.setStatusCode(responseCode);
                } else if (isContentLengthValid(connection, coordinatorAudit)) {
                    is = connection.getInputStream();
                    if (isInputStreamEmpty(1, is)) {
                        IOUtils.closeQuietly(null);
                        IOUtils.closeQuietly(is);
                        if (connection != null) {
                            connection.disconnect();
                            return;
                        }
                        return;
                    }
                    InputStream dis2 = new DataInputStream(is);
                    try {
                        int metaLength = dis2.readInt();
                        if (metaLength > 4096) {
                            setErrorHttpResponse(-5, " meta length is too long! " + metaLength);
                            IOUtils.closeQuietly(dis2);
                            IOUtils.closeQuietly(is);
                            if (connection != null) {
                                connection.disconnect();
                            }
                            dis = dis2;
                            return;
                        }
                        byte[] metaBytes = new byte[metaLength];
                        int readLength = is.read(metaBytes);
                        if (readLength == -1) {
                            setErrorHttpResponse(-5, "HttpClient can not get metaString info from connection!");
                            IOUtils.closeQuietly(dis2);
                            IOUtils.closeQuietly(is);
                            if (connection != null) {
                                connection.disconnect();
                            }
                            dis = dis2;
                            return;
                        }
                        long totalLength = 0 + ((long) readLength);
                        String str = new String(metaBytes, StandardCharsets.UTF_8);
                        if (JsonUtils.isValidJson(str)) {
                            PackageMeta packageMeta = (PackageMeta) JsonUtils.parse(str, PackageMeta.class);
                            if (packageMeta == null) {
                                setErrorHttpResponse(-5, "HttpClient Can not get PackageMeta info from metaString!");
                                IOUtils.closeQuietly(dis2);
                                IOUtils.closeQuietly(is);
                                if (connection != null) {
                                    connection.disconnect();
                                }
                                dis = dis2;
                                return;
                            }
                            List<PackagesBean> packageList = packageMeta.getPackages();
                            if (packageList == null) {
                                setErrorHttpResponse(-5, "HttpClient can not get packageList from PackageMeta!");
                                IOUtils.closeQuietly(dis2);
                                IOUtils.closeQuietly(is);
                                if (connection != null) {
                                    connection.disconnect();
                                }
                                dis = dis2;
                                return;
                            }
                            this.mResponse.setStatusCode(responseCode);
                            int size = packageList.size();
                            int i = 0;
                            while (i < size) {
                                PackagesBean packageBean = (PackagesBean) packageList.get(i);
                                String fileName = packageBean.getName();
                                File file = FileUtils.getFile(this.fileSavePath, fileName);
                                if (createFile(file)) {
                                    int fileSize = Integer.parseInt(packageBean.getSize());
                                    OutputStream os = null;
                                    OutputStream bos = null;
                                    try {
                                        os = getSafeOutputStream(file, false);
                                        OutputStream bos2 = new BufferedOutputStream(os);
                                        try {
                                            byte[] bb = new byte[BUFFER_SIZE];
                                            int readPosition = 0;
                                            while (readPosition < fileSize) {
                                                int len;
                                                if (readPosition + BUFFER_SIZE <= fileSize) {
                                                    len = is.read(bb);
                                                    if (len != -1) {
                                                        bos2.write(bb, 0, len);
                                                        totalLength += (long) len;
                                                    }
                                                } else {
                                                    byte[] lastbb = new byte[(fileSize - readPosition)];
                                                    len = is.read(lastbb);
                                                    if (len != -1) {
                                                        bos2.write(lastbb, 0, len);
                                                        totalLength += (long) len;
                                                    }
                                                }
                                                if (totalLength > this.dataTrafficSize.longValue()) {
                                                    throw new IOException(" file size is overlarge than " + this.dataTrafficSize);
                                                } else if (len != -1) {
                                                    readPosition += len;
                                                }
                                            }
                                            bos2.flush();
                                            if (this.mDataRequestListener != null && (this.mDataRequestListener instanceof FileDataRequestListener)) {
                                                ((FileDataRequestListener) this.mDataRequestListener).onDownloadSuccess(fileName, String.valueOf(i + 1), str);
                                            }
                                            IOUtils.closeQuietly(bos2);
                                            IOUtils.closeQuietly(os);
                                            i++;
                                        } catch (IOException e2) {
                                            e = e2;
                                            bos = bos2;
                                            try {
                                                this.mResponse.setHttpExceptionMsg(e.getMessage());
                                                setErrorHttpResponse(-5, "HttpClient MultiPackage IOException : " + e.getMessage());
                                                IOUtils.closeQuietly(bos);
                                                IOUtils.closeQuietly(os);
                                                IOUtils.closeQuietly(dis2);
                                                IOUtils.closeQuietly(is);
                                                if (connection != null) {
                                                    connection.disconnect();
                                                }
                                                dis = dis2;
                                                return;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                IOUtils.closeQuietly(bos);
                                                IOUtils.closeQuietly(os);
                                                throw th;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                            bos = bos2;
                                            IOUtils.closeQuietly(bos);
                                            IOUtils.closeQuietly(os);
                                            throw th;
                                        }
                                    } catch (IOException e3) {
                                        e = e3;
                                        this.mResponse.setHttpExceptionMsg(e.getMessage());
                                        setErrorHttpResponse(-5, "HttpClient MultiPackage IOException : " + e.getMessage());
                                        IOUtils.closeQuietly(bos);
                                        IOUtils.closeQuietly(os);
                                        IOUtils.closeQuietly(dis2);
                                        IOUtils.closeQuietly(is);
                                        if (connection != null) {
                                        }
                                        dis = dis2;
                                        return;
                                    }
                                }
                                setErrorHttpResponse(-5, "HttpClient create file fail!");
                                IOUtils.closeQuietly(dis2);
                                IOUtils.closeQuietly(is);
                                if (connection != null) {
                                    connection.disconnect();
                                }
                                dis = dis2;
                                return;
                            }
                            this.mResponse.setResponseSize(totalLength);
                            this.mResponse.setResponseString("{\"data\":\"Fragment load success!\"}");
                            dis = dis2;
                        } else {
                            setErrorHttpResponse(-5, "HttpClient json string is not valid!");
                            IOUtils.closeQuietly(dis2);
                            IOUtils.closeQuietly(is);
                            if (connection != null) {
                                connection.disconnect();
                            }
                            dis = dis2;
                            return;
                        }
                    } catch (IOException e4) {
                        e = e4;
                        dis = dis2;
                        try {
                            if (TextUtils.isEmpty(e.getMessage())) {
                                setErrorHttpResponse(-5, " IOException, error msg is empty.");
                            } else {
                                this.mResponse.setHttpExceptionMsg(e.getMessage());
                                String errMsg = "HttpClient executeResponseForMultiPackage IOException : " + e.getMessage();
                                if (e.getMessage().startsWith("Unable to resolve host")) {
                                    setErrorHttpResponse(-6, errMsg);
                                } else if (e.getMessage().startsWith("failed to connect to") || e.getMessage().startsWith("Failed to connect to")) {
                                    setErrorHttpResponse(-9, errMsg);
                                } else if (e.getMessage().startsWith(IVerifyVar.TIME_OUT_HEADER)) {
                                    setErrorHttpResponse(-13, errMsg);
                                } else {
                                    setErrorHttpResponse(-5, errMsg);
                                }
                            }
                            IOUtils.closeQuietly(dis);
                            IOUtils.closeQuietly(is);
                            if (connection != null) {
                                connection.disconnect();
                                return;
                            }
                            return;
                        } catch (Throwable th4) {
                            th = th4;
                            IOUtils.closeQuietly(dis);
                            IOUtils.closeQuietly(is);
                            if (connection != null) {
                            }
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        dis = dis2;
                        IOUtils.closeQuietly(dis);
                        IOUtils.closeQuietly(is);
                        if (connection != null) {
                            connection.disconnect();
                        }
                        throw th;
                    }
                } else {
                    IOUtils.closeQuietly(null);
                    IOUtils.closeQuietly(null);
                    if (connection != null) {
                        connection.disconnect();
                        return;
                    }
                    return;
                }
                IOUtils.closeQuietly(dis);
                IOUtils.closeQuietly(is);
                if (connection != null) {
                    connection.disconnect();
                    return;
                }
                return;
            }
            IOUtils.closeQuietly(null);
            IOUtils.closeQuietly(null);
            if (connection != null) {
                connection.disconnect();
            }
        } catch (IOException e5) {
            e = e5;
        }
    }

    private static OutputStream getSafeOutputStream(File safeFile, boolean isGroupReadShare) throws IOException {
        FileAttribute<Set<PosixFilePermission>> attr = FileUtils.getDefaultFileAttribute(safeFile, isGroupReadShare);
        Path path = safeFile.toPath();
        Files.newByteChannel(path, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE), new FileAttribute[]{attr}).close();
        return Files.newOutputStream(path, new OpenOption[0]);
    }

    private boolean isInputStreamEmpty(int requestMode, InputStream inputStream) {
        if (inputStream != null) {
            return false;
        }
        setErrorHttpResponse(-5, ": requestMode= " + requestMode + ". InputStream is empty.");
        return true;
    }

    private boolean isOutputStreamEmpty(OutputStream outputStream) {
        if (outputStream != null) {
            return false;
        }
        setErrorHttpResponse(-5, ": OutputStream is empty.");
        return true;
    }

    private String fullfillEmptyMessage(String message) {
        if (message != null) {
            return message;
        }
        setErrorHttpResponse(-2, " Response msg is empty.");
        return "";
    }

    private boolean isContentLengthValid(HttpURLConnection connection, CoordinatorAudit coordinatorAudit) {
        this.mResponse.setHeaderFields(connection.getHeaderFields());
        String contentLengthStr = this.mResponse.getHeaderValue("Content-Length");
        if (TextUtils.isEmpty(contentLengthStr)) {
            return true;
        }
        try {
            long contentLength = Long.parseLong(contentLengthStr);
            if (contentLength > this.dataTrafficSize.longValue()) {
                setErrorHttpResponse(-5, "HttpClient content-length = " + contentLength + ", dataTrafficSize = " + this.dataTrafficSize + ". Download suspended.");
                setErrorHttpResponse(-5, "HttpClient: Content-Length is invalid");
                return false;
            } else if (coordinatorAudit == null) {
                return true;
            } else {
                coordinatorAudit.setDataSize(Long.valueOf(contentLength));
                return true;
            }
        } catch (NumberFormatException e) {
            setErrorHttpResponse(-5, "HttpClient: Parse Content-Length fail! Content-Length" + contentLengthStr + ", Error:" + e.getMessage());
            return false;
        }
    }

    private HttpResponse setErrorHttpResponse(int code, String errMsg) {
        DSLog.e(errMsg, new Object[0]);
        this.mResponse.setStatusCode(code);
        this.mResponse.setResponseMsg(errMsg);
        return this.mResponse;
    }

    private boolean isRequestBodyValid() {
        return this.requestBody.length() <= MAX_REQUESTBODY_LENGTH;
    }
}
