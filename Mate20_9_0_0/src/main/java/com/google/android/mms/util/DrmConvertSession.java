package com.google.android.mms.util;

import android.content.Context;
import android.drm.DrmConvertedStatus;
import android.drm.DrmManagerClient;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DrmConvertSession {
    private static final String TAG = "DrmConvertSession";
    private int mConvertSessionId;
    private DrmManagerClient mDrmClient;

    private DrmConvertSession(DrmManagerClient drmClient, int convertSessionId) {
        this.mDrmClient = drmClient;
        this.mConvertSessionId = convertSessionId;
    }

    public static DrmConvertSession open(Context context, String mimeType) {
        DrmManagerClient drmClient = null;
        int convertSessionId = -1;
        if (!(context == null || mimeType == null || mimeType.equals(""))) {
            try {
                drmClient = new DrmManagerClient(context);
                try {
                    convertSessionId = drmClient.openConvertSession(mimeType);
                } catch (IllegalArgumentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Conversion of Mimetype: ");
                    stringBuilder.append(mimeType);
                    stringBuilder.append(" is not supported.");
                    Log.w(str, stringBuilder.toString(), e);
                } catch (IllegalStateException e2) {
                    Log.w(TAG, "Could not access Open DrmFramework.", e2);
                }
            } catch (IllegalArgumentException e3) {
                Log.w(TAG, "DrmManagerClient instance could not be created, context is Illegal.");
            } catch (IllegalStateException e4) {
                Log.w(TAG, "DrmManagerClient didn't initialize properly.");
            }
        }
        if (drmClient == null || convertSessionId < 0) {
            return null;
        }
        return new DrmConvertSession(drmClient, convertSessionId);
    }

    public byte[] convert(byte[] inBuffer, int size) {
        String str;
        StringBuilder stringBuilder;
        if (inBuffer != null) {
            try {
                DrmConvertedStatus convertedStatus;
                if (size != inBuffer.length) {
                    byte[] buf = new byte[size];
                    System.arraycopy(inBuffer, 0, buf, 0, size);
                    convertedStatus = this.mDrmClient.convertData(this.mConvertSessionId, buf);
                } else {
                    convertedStatus = this.mDrmClient.convertData(this.mConvertSessionId, inBuffer);
                }
                if (convertedStatus == null || convertedStatus.statusCode != 1 || convertedStatus.convertedData == null) {
                    return null;
                }
                return convertedStatus.convertedData;
            } catch (IllegalArgumentException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Buffer with data to convert is illegal. Convertsession: ");
                stringBuilder.append(this.mConvertSessionId);
                Log.w(str, stringBuilder.toString(), e);
                return null;
            } catch (IllegalStateException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not convert data. Convertsession: ");
                stringBuilder.append(this.mConvertSessionId);
                Log.w(str, stringBuilder.toString(), e2);
                return null;
            }
        }
        throw new IllegalArgumentException("Parameter inBuffer is null");
    }

    public int close(String filename) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        String stringBuilder2;
        int result = 491;
        if (this.mDrmClient == null || this.mConvertSessionId < 0) {
            return 491;
        }
        RandomAccessFile rndAccessFile;
        try {
            DrmConvertedStatus convertedStatus = this.mDrmClient.closeConvertSession(this.mConvertSessionId);
            if (convertedStatus == null || convertedStatus.statusCode != 1 || convertedStatus.convertedData == null) {
                return 406;
            }
            rndAccessFile = null;
            try {
                rndAccessFile = new RandomAccessFile(filename, "rw");
                rndAccessFile.seek((long) convertedStatus.offset);
                rndAccessFile.write(convertedStatus.convertedData);
                try {
                    rndAccessFile.close();
                    return 200;
                } catch (IOException e2) {
                    e = e2;
                    result = 492;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to close File:");
                    stringBuilder.append(filename);
                    stringBuilder.append(".");
                    stringBuilder2 = stringBuilder.toString();
                    Log.w(str, stringBuilder2, e);
                    return result;
                }
            } catch (FileNotFoundException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("File: ");
                stringBuilder.append(filename);
                stringBuilder.append(" could not be found.");
                Log.w(str, stringBuilder.toString(), e3);
                if (rndAccessFile == null) {
                    return 492;
                }
                try {
                    rndAccessFile.close();
                    return 492;
                } catch (IOException e4) {
                    e = e4;
                    result = 492;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to close File:");
                    stringBuilder.append(filename);
                    stringBuilder.append(".");
                    stringBuilder2 = stringBuilder.toString();
                    Log.w(str, stringBuilder2, e);
                    return result;
                }
            } catch (IOException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not access File: ");
                stringBuilder.append(filename);
                stringBuilder.append(" .");
                Log.w(str, stringBuilder.toString(), e5);
                if (rndAccessFile == null) {
                    return 492;
                }
                try {
                    rndAccessFile.close();
                    return 492;
                } catch (IOException e6) {
                    e5 = e6;
                    result = 492;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to close File:");
                    stringBuilder.append(filename);
                    stringBuilder.append(".");
                    stringBuilder2 = stringBuilder.toString();
                    Log.w(str, stringBuilder2, e5);
                    return result;
                }
            } catch (IllegalArgumentException e7) {
                Log.w(TAG, "Could not open file in mode: rw", e7);
                if (rndAccessFile == null) {
                    return 492;
                }
                try {
                    rndAccessFile.close();
                    return 492;
                } catch (IOException e8) {
                    e5 = e8;
                    result = 492;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to close File:");
                    stringBuilder.append(filename);
                    stringBuilder.append(".");
                    stringBuilder2 = stringBuilder.toString();
                    Log.w(str, stringBuilder2, e5);
                    return result;
                }
            } catch (SecurityException e9) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Access to File: ");
                stringBuilder.append(filename);
                stringBuilder.append(" was denied denied by SecurityManager.");
                Log.w(str, stringBuilder.toString(), e9);
                if (rndAccessFile == null) {
                    return 491;
                }
                try {
                    rndAccessFile.close();
                    return 491;
                } catch (IOException e10) {
                    e5 = e10;
                    result = 492;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to close File:");
                    stringBuilder.append(filename);
                    stringBuilder.append(".");
                    stringBuilder2 = stringBuilder.toString();
                    Log.w(str, stringBuilder2, e5);
                    return result;
                }
            }
        } catch (IllegalStateException e11) {
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Could not close convertsession. Convertsession: ");
            stringBuilder3.append(this.mConvertSessionId);
            Log.w(str2, stringBuilder3.toString(), e11);
            return result;
        } catch (Throwable th) {
            if (rndAccessFile != null) {
                try {
                    rndAccessFile.close();
                } catch (IOException e12) {
                    result = 492;
                    stringBuilder2 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Failed to close File:");
                    stringBuilder4.append(filename);
                    stringBuilder4.append(".");
                    Log.w(stringBuilder2, stringBuilder4.toString(), e12);
                }
            }
        }
    }
}
