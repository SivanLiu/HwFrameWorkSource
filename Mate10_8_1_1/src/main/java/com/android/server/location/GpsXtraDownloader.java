package com.android.server.location;

import android.net.TrafficStats;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.HwServiceFactory;
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

    protected byte[] doDownload(java.lang.String r22) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x015c in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r21 = this;
        r14 = "GpsXtraDownloader";
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "Downloading XTRA data from ";
        r15 = r15.append(r16);
        r0 = r22;
        r15 = r15.append(r0);
        r15 = r15.toString();
        android.util.Log.i(r14, r15);
        r14 = 0;
        r0 = r21;
        r0.mByteCount = r14;
        r2 = 0;
        r10 = new java.net.URL;	 Catch:{ IOException -> 0x0067 }
        r0 = r22;	 Catch:{ IOException -> 0x0067 }
        r10.<init>(r0);	 Catch:{ IOException -> 0x0067 }
        r6 = r10.openConnection();	 Catch:{ IOException -> 0x0067 }
        r14 = 3000; // 0xbb8 float:4.204E-42 double:1.482E-320;	 Catch:{ IOException -> 0x0067 }
        r6.setConnectTimeout(r14);	 Catch:{ IOException -> 0x0067 }
        r14 = 3000; // 0xbb8 float:4.204E-42 double:1.482E-320;	 Catch:{ IOException -> 0x0067 }
        r6.setReadTimeout(r14);	 Catch:{ IOException -> 0x0067 }
        r14 = r6.getContentLength();	 Catch:{ IOException -> 0x0067 }
        r0 = r21;	 Catch:{ IOException -> 0x0067 }
        r0.mByteCount = r14;	 Catch:{ IOException -> 0x0067 }
        r14 = "GpsXtraDownloader";	 Catch:{ IOException -> 0x0067 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0067 }
        r15.<init>();	 Catch:{ IOException -> 0x0067 }
        r16 = "mByteCount size is ";	 Catch:{ IOException -> 0x0067 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0067 }
        r0 = r21;	 Catch:{ IOException -> 0x0067 }
        r0 = r0.mByteCount;	 Catch:{ IOException -> 0x0067 }
        r16 = r0;	 Catch:{ IOException -> 0x0067 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0067 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x0067 }
        android.util.Log.i(r14, r15);	 Catch:{ IOException -> 0x0067 }
        r0 = r21;
        r14 = r0.mByteCount;
        if (r14 > 0) goto L_0x006d;
    L_0x0065:
        r14 = 0;
        return r14;
    L_0x0067:
        r9 = move-exception;
        r9.printStackTrace();
        r14 = 0;
        return r14;
    L_0x006d:
        r0 = r21;
        r14 = r0.mByteCount;
        r0 = r21;
        r14 = r0.shouldDownload(r14);
        if (r14 != 0) goto L_0x009b;
    L_0x0079:
        r14 = "GpsXtraDownloader";
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "should not download again, download interval:";
        r15 = r15.append(r16);
        r0 = r21;
        r0 = r0.mDownLoadInterval;
        r16 = r0;
        r15 = r15.append(r16);
        r15 = r15.toString();
        android.util.Log.i(r14, r15);
        r14 = 0;
        return r14;
    L_0x009b:
        r7 = 0;
        r14 = new java.net.URL;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r22;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14.<init>(r0);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r14.openConnection();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r14;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = (java.net.HttpURLConnection) r0;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r7 = r0;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = "Accept";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r7.setRequestProperty(r14, r15);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = "x-wap-profile";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = "http://www.openmobilealliance.org/tech/profiles/UAPROF/ccppschema-20021212#";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r7.setRequestProperty(r14, r15);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = CONNECTION_TIMEOUT_MS;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r7.setConnectTimeout(r14);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = 120000; // 0x1d4c0 float:1.68156E-40 double:5.9288E-319;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r7.setReadTimeout(r14);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r7.connect();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = "GpsXtraDownloader";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15.<init>();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = "the connection timeout:";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r7.getConnectTimeout();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        android.util.Log.i(r14, r15);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r13 = r7.getResponseCode();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        if (r13 == r14) goto L_0x0112;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x00f1:
        r14 = "GpsXtraDownloader";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15.<init>();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = "HTTP error downloading gps XTRA: ";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r13);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        android.util.Log.i(r14, r15);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = 0;
        if (r7 == 0) goto L_0x0111;
    L_0x010e:
        r7.disconnect();
    L_0x0111:
        return r14;
    L_0x0112:
        r15 = 0;
        r11 = 0;
        r11 = r7.getInputStream();	 Catch:{ all -> 0x026b, all -> 0x017e }
        r5 = new java.io.ByteArrayOutputStream;	 Catch:{ all -> 0x026b, all -> 0x017e }
        r5.<init>();	 Catch:{ all -> 0x026b, all -> 0x017e }
        r14 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;	 Catch:{ all -> 0x026b, all -> 0x017e }
        r4 = new byte[r14];	 Catch:{ all -> 0x026b, all -> 0x017e }
    L_0x0121:
        r8 = r11.read(r4);	 Catch:{ all -> 0x026b, all -> 0x017e }
        r14 = -1;	 Catch:{ all -> 0x026b, all -> 0x017e }
        if (r8 == r14) goto L_0x0167;	 Catch:{ all -> 0x026b, all -> 0x017e }
    L_0x0128:
        r14 = 0;	 Catch:{ all -> 0x026b, all -> 0x017e }
        r5.write(r4, r14, r8);	 Catch:{ all -> 0x026b, all -> 0x017e }
        r14 = r5.size();	 Catch:{ all -> 0x026b, all -> 0x017e }
        r0 = (long) r14;	 Catch:{ all -> 0x026b, all -> 0x017e }
        r16 = r0;	 Catch:{ all -> 0x026b, all -> 0x017e }
        r18 = 1000000; // 0xf4240 float:1.401298E-39 double:4.940656E-318;	 Catch:{ all -> 0x026b, all -> 0x017e }
        r14 = (r16 > r18 ? 1 : (r16 == r18 ? 0 : -1));	 Catch:{ all -> 0x026b, all -> 0x017e }
        if (r14 <= 0) goto L_0x0121;	 Catch:{ all -> 0x026b, all -> 0x017e }
    L_0x013a:
        r14 = "GpsXtraDownloader";	 Catch:{ all -> 0x026b, all -> 0x017e }
        r16 = "XTRA file too large";	 Catch:{ all -> 0x026b, all -> 0x017e }
        r0 = r16;	 Catch:{ all -> 0x026b, all -> 0x017e }
        android.util.Log.d(r14, r0);	 Catch:{ all -> 0x026b, all -> 0x017e }
        if (r11 == 0) goto L_0x014a;
    L_0x0147:
        r11.close();	 Catch:{ Throwable -> 0x015e }
    L_0x014a:
        if (r15 == 0) goto L_0x0160;
    L_0x014c:
        throw r15;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x014d:
        r12 = move-exception;
        r14 = "GpsXtraDownloader";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = "Error downloading gps XTRA: ";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        android.util.Log.i(r14, r15, r12);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        if (r7 == 0) goto L_0x015c;
    L_0x0159:
        r7.disconnect();
    L_0x015c:
        r14 = 0;
        return r14;
    L_0x015e:
        r15 = move-exception;
        goto L_0x014a;
    L_0x0160:
        r14 = 0;
        if (r7 == 0) goto L_0x0166;
    L_0x0163:
        r7.disconnect();
    L_0x0166:
        return r14;
    L_0x0167:
        r3 = r5.toByteArray();	 Catch:{ all -> 0x026b, all -> 0x017e }
        if (r11 == 0) goto L_0x0170;
    L_0x016d:
        r11.close();	 Catch:{ Throwable -> 0x017a }
    L_0x0170:
        if (r15 == 0) goto L_0x019b;
    L_0x0172:
        throw r15;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x0173:
        r14 = move-exception;
        if (r7 == 0) goto L_0x0179;
    L_0x0176:
        r7.disconnect();
    L_0x0179:
        throw r14;
    L_0x017a:
        r15 = move-exception;
        goto L_0x0170;
    L_0x017c:
        r14 = move-exception;
        throw r14;	 Catch:{ all -> 0x026b, all -> 0x017e }
    L_0x017e:
        r15 = move-exception;
        r20 = r15;
        r15 = r14;
        r14 = r20;
    L_0x0184:
        if (r11 == 0) goto L_0x0189;
    L_0x0186:
        r11.close();	 Catch:{ Throwable -> 0x018c }
    L_0x0189:
        if (r15 == 0) goto L_0x019a;
    L_0x018b:
        throw r15;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x018c:
        r16 = move-exception;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        if (r15 != 0) goto L_0x0192;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x018f:
        r15 = r16;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        goto L_0x0189;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x0192:
        r0 = r16;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        if (r15 == r0) goto L_0x0189;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x0196:
        r15.addSuppressed(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        goto L_0x0189;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x019a:
        throw r14;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x019b:
        r14 = "GpsXtraDownloader";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15.<init>();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = "the getReadTimeout:";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r7.getReadTimeout();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        android.util.Log.i(r14, r15);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r0.mByteCount;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r3.length;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        if (r14 != r15) goto L_0x025b;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x01c0:
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r0.mProperties;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = "LAST_XTRA_DOWNLOAD_TIME";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = android.os.SystemClock.elapsedRealtime();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = java.lang.String.valueOf(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14.setProperty(r15, r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r0.mProperties;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = "LAST_SUCCESS_XTRA_DATA_SIZE";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r0.mByteCount;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r0;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = java.lang.String.valueOf(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14.setProperty(r15, r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r0.mDownLoadInterval;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = 600000; // 0x927c0 float:8.40779E-40 double:2.964394E-318;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r14 + r15;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0.mDownLoadInterval = r14;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r0.mDownLoadInterval;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = 7200000; // 0x6ddd00 float:1.0089349E-38 double:3.5572727E-317;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        if (r14 <= r15) goto L_0x0202;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x01fb:
        r14 = 7200000; // 0x6ddd00 float:1.0089349E-38 double:3.5572727E-317;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0.mDownLoadInterval = r14;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
    L_0x0202:
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = r0.mProperties;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = "HW_XTRA_DOWNLOAD_INTERVAL";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r0.mDownLoadInterval;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r0;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = java.lang.String.valueOf(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14.setProperty(r15, r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = "GpsXtraDownloader";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15.<init>();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = "lto downloader process ok, set download time:";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r0.mProperties;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r0;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r17 = "LAST_XTRA_DOWNLOAD_TIME";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r16.getProperty(r17);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = ", set next down interval:";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r21;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r0 = r0.mProperties;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r0;	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r17 = "HW_XTRA_DOWNLOAD_INTERVAL";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r16 = r16.getProperty(r17);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        android.util.Log.i(r14, r15);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        if (r7 == 0) goto L_0x025a;
    L_0x0257:
        r7.disconnect();
    L_0x025a:
        return r3;
    L_0x025b:
        r14 = "GpsXtraDownloader";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r15 = "lto downloader process error";	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        android.util.Log.e(r14, r15);	 Catch:{ IOException -> 0x014d, all -> 0x0173 }
        r14 = 0;
        if (r7 == 0) goto L_0x026a;
    L_0x0267:
        r7.disconnect();
    L_0x026a:
        return r14;
    L_0x026b:
        r14 = move-exception;
        goto L_0x0184;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.GpsXtraDownloader.doDownload(java.lang.String):byte[]");
    }

    GpsXtraDownloader(Properties properties) {
        this.mProperties = properties;
        int count = 0;
        String server1 = properties.getProperty("XTRA_SERVER_1");
        String server2 = properties.getProperty("XTRA_SERVER_2");
        String server3 = properties.getProperty("XTRA_SERVER_3");
        if (server1 != null) {
            count = 1;
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
            if (server1 != null) {
                this.mXtraServers[0] = server1;
                count2 = 1;
            } else {
                count2 = 0;
            }
            if (server2 != null) {
                count = count2 + 1;
                this.mXtraServers[count2] = server2;
                count2 = count;
            }
            if (server3 != null) {
                count = count2 + 1;
                this.mXtraServers[count2] = server3;
            } else {
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
        loop0:
        while (result == null) {
            int oldTag = TrafficStats.getAndSetThreadStatsTag(-188);
            try {
                result = doDownload(this.mXtraServers[this.mNextServerIndex]);
                this.mNextServerIndex++;
                if (this.mNextServerIndex == this.mXtraServers.length) {
                    this.mNextServerIndex = 0;
                }
                if (this.mNextServerIndex == startIndex) {
                    break loop0;
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

    private boolean shouldDownload(int byteCount) {
        boolean z = true;
        long currenttime = SystemClock.elapsedRealtime();
        if (byteCount == Integer.parseInt(this.mProperties.getProperty("LAST_SUCCESS_XTRA_DATA_SIZE"))) {
            if (currenttime - this.mLastDownloadTime <= ((long) this.mDownLoadInterval)) {
                z = false;
            }
            return z;
        }
        this.mDownLoadInterval = 0;
        return true;
    }
}
