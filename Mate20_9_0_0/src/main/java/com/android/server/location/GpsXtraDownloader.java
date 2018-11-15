package com.android.server.location;

import android.net.TrafficStats;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.HwServiceFactory;
import com.android.server.display.DisplayTransformManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GpsXtraDownloader {
    private static final int CONNECTION_TIMEOUT_MS = ((int) TimeUnit.SECONDS.toMillis(30));
    private static final String DEFAULT_USER_AGENT = "Android";
    private static final long MAXIMUM_CONTENT_LENGTH_BYTES = 1000000;
    private static final int READ_TIMEOUT_MS = ((int) TimeUnit.SECONDS.toMillis(60));
    private static final String TAG = "GpsXtraDownloader";
    private int mByteCount;
    private int mDownLoadInterval;
    private IHwGpsLogServices mHwGpsLogServices;
    private long mLastDownloadTime;
    private int mNextServerIndex;
    private Properties mProperties;
    private final String mUserAgent;
    private final String[] mXtraServers;

    GpsXtraDownloader(Properties properties) {
        this.mProperties = properties;
        int count = 0;
        String server1 = properties.getProperty("XTRA_SERVER_1");
        String server2 = properties.getProperty("XTRA_SERVER_2");
        String server3 = properties.getProperty("XTRA_SERVER_3");
        if (server1 != null) {
            count = 0 + 1;
        }
        if (server2 != null) {
            count++;
        }
        if (server3 != null) {
            count++;
        }
        String agent = properties.getProperty("XTRA_USER_AGENT");
        if (TextUtils.isEmpty(agent)) {
            this.mUserAgent = DEFAULT_USER_AGENT;
        } else {
            this.mUserAgent = agent;
        }
        if (count == 0) {
            Log.e(TAG, "No XTRA servers were specified in the GPS configuration");
            this.mXtraServers = null;
        } else {
            int count2;
            this.mXtraServers = new String[count];
            count = 0;
            if (server1 != null) {
                count2 = 0 + 1;
                this.mXtraServers[0] = server1;
                count = count2;
            }
            if (server2 != null) {
                count2 = count + 1;
                this.mXtraServers[count] = server2;
                count = count2;
            }
            if (server3 != null) {
                count2 = count + 1;
                this.mXtraServers[count] = server3;
                count = count2;
            }
            this.mNextServerIndex = new Random().nextInt(count);
        }
        this.mDownLoadInterval = Integer.parseInt(this.mProperties.getProperty("HW_XTRA_DOWNLOAD_INTERVAL"));
        this.mLastDownloadTime = Long.parseLong(this.mProperties.getProperty("LAST_XTRA_DOWNLOAD_TIME"));
    }

    byte[] downloadXtraData() {
        byte[] result = null;
        int startIndex = this.mNextServerIndex;
        if (this.mXtraServers == null) {
            return null;
        }
        loop1:
        while (result == null) {
            int oldTag = TrafficStats.getAndSetThreadStatsTag(-188);
            try {
                String[] strArr = this.mXtraServers;
                int i = this.mNextServerIndex;
                byte[] doDownload = doDownload(strArr[i]);
                result = doDownload;
                if (doDownload == i) {
                    this.mNextServerIndex = 0;
                }
                if (this.mNextServerIndex == startIndex) {
                    break loop1;
                }
            } finally {
                TrafficStats.setThreadStatsTag(oldTag);
            }
        }
        this.mHwGpsLogServices = HwServiceFactory.getNewHwGpsLogService();
        if (this.mHwGpsLogServices != null) {
            boolean xtraStatus = true;
            if (result == null && this.mByteCount != -1) {
                xtraStatus = false;
            }
            this.mHwGpsLogServices.updateXtraDloadStatus(xtraStatus);
            if (xtraStatus) {
                this.mHwGpsLogServices.injectExtraParam("extra_data");
            }
        }
        return result;
    }

    protected byte[] doDownload(String url) {
        Throwable th;
        String str = url;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Downloading XTRA data from ");
        stringBuilder.append(str);
        Log.i(str2, stringBuilder.toString());
        this.mByteCount = 0;
        int blockSize = 0;
        try {
            InputStream in;
            Throwable th2;
            URLConnection conn = new URL(str).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            this.mByteCount = conn.getContentLength();
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mByteCount size is ");
            stringBuilder2.append(this.mByteCount);
            Log.i(str3, stringBuilder2.toString());
            if (this.mByteCount <= 0) {
                return null;
            }
            if (shouldDownload(this.mByteCount)) {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(str).openConnection();
                    connection.setRequestProperty("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
                    connection.setRequestProperty("x-wap-profile", "http://www.openmobilealliance.org/tech/profiles/UAPROF/ccppschema-20021212#");
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                    connection.setReadTimeout(120000);
                    connection.connect();
                    String str4 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("the connection timeout:");
                    stringBuilder3.append(connection.getConnectTimeout());
                    Log.i(str4, stringBuilder3.toString());
                    int statusCode = connection.getResponseCode();
                    if (statusCode != DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE) {
                        str2 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("HTTP error downloading gps XTRA: ");
                        stringBuilder3.append(statusCode);
                        Log.i(str2, stringBuilder3.toString());
                        if (connection != null) {
                            connection.disconnect();
                        }
                        return null;
                    }
                    in = connection.getInputStream();
                    try {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        do {
                            int read = in.read(buffer);
                            int count = read;
                            if (read != -1) {
                                bytes.write(buffer, 0, count);
                            } else {
                                byte[] body = bytes.toByteArray();
                                if (in != null) {
                                    in.close();
                                }
                                str3 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("the getReadTimeout:");
                                stringBuilder2.append(connection.getReadTimeout());
                                Log.i(str3, stringBuilder2.toString());
                                if (this.mByteCount == body.length) {
                                    this.mProperties.setProperty("LAST_XTRA_DOWNLOAD_TIME", String.valueOf(SystemClock.elapsedRealtime()));
                                    this.mProperties.setProperty("LAST_SUCCESS_XTRA_DATA_SIZE", String.valueOf(this.mByteCount));
                                    this.mDownLoadInterval += 600000;
                                    if (this.mDownLoadInterval > 7200000) {
                                        this.mDownLoadInterval = 7200000;
                                    }
                                    this.mProperties.setProperty("HW_XTRA_DOWNLOAD_INTERVAL", String.valueOf(this.mDownLoadInterval));
                                    str3 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("lto downloader process ok, set download time:");
                                    stringBuilder2.append(this.mProperties.getProperty("LAST_XTRA_DOWNLOAD_TIME"));
                                    stringBuilder2.append(", set next down interval:");
                                    stringBuilder2.append(this.mProperties.getProperty("HW_XTRA_DOWNLOAD_INTERVAL"));
                                    Log.i(str3, stringBuilder2.toString());
                                    if (connection != null) {
                                        connection.disconnect();
                                    }
                                    return body;
                                }
                                Log.e(TAG, "lto downloader process error");
                                if (connection != null) {
                                    connection.disconnect();
                                }
                                return null;
                            }
                        } while (((long) bytes.size()) <= MAXIMUM_CONTENT_LENGTH_BYTES);
                        Log.d(TAG, "XTRA file too large");
                        if (in != null) {
                            in.close();
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                        return null;
                    } catch (Throwable th3) {
                        th2 = th;
                        th = th3;
                    }
                } catch (IOException ioe) {
                    Log.i(TAG, "Error downloading gps XTRA: ", ioe);
                    if (connection != null) {
                        connection.disconnect();
                    }
                    return null;
                } catch (Throwable th32) {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    throw th32;
                }
            }
            str2 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("should not download again, download interval:");
            stringBuilder4.append(this.mDownLoadInterval);
            Log.i(str2, stringBuilder4.toString());
            return null;
            if (in != null) {
                if (th2 != null) {
                    try {
                        in.close();
                    } catch (Throwable th322) {
                        th2.addSuppressed(th322);
                    }
                } else {
                    in.close();
                }
            }
            throw th;
            throw th;
        } catch (IOException ioe2) {
            ioe2.printStackTrace();
            return null;
        }
    }

    private boolean shouldDownload(int byteCount) {
        long currenttime = SystemClock.elapsedRealtime();
        boolean z = false;
        if (byteCount == Integer.parseInt(this.mProperties.getProperty("LAST_SUCCESS_XTRA_DATA_SIZE"))) {
            if (currenttime - this.mLastDownloadTime > ((long) this.mDownLoadInterval)) {
                z = true;
            }
            return z;
        }
        this.mDownLoadInterval = 0;
        return true;
    }
}
