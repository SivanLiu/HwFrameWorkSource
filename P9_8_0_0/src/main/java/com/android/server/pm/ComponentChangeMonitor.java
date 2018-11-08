package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ComponentChangeMonitor {
    static final String COMCHANGE_REPORT_FILE_DIR = (Environment.getDataDirectory() + "/log/com_change/");
    static final String COM_CHANGE_REPORT_FILE_LATEST = "com_change_report.1";
    static final String COM_CHANGE_REPORT_FILE_PREFIX = "com_change_report.";
    static final boolean DEBUG = false;
    static final int MAX_FILE_COUNT = 5;
    static final int MAX_FILE_SIZE = 102400;
    static final int MSG_WIRTE_FILE = 0;
    static final String TAG = "ComponentChangeMonitor";
    private Context mContext;
    private Looper mHandlerLooper = null;
    private List<String> mMonitorList = new ArrayList();
    private WriteFileHandler mWriteHandler = null;

    private class WriteFileHandler extends Handler {
        public WriteFileHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    ComponentChangeMonitor.this.writeLogToFile((String) msg.obj);
                    return;
                default:
                    Log.w(ComponentChangeMonitor.TAG, "WriteFileHandler unsupport operator " + msg.what);
                    return;
            }
        }
    }

    public ComponentChangeMonitor(Context context, Looper looper) {
        this.mContext = context;
        this.mHandlerLooper = looper;
        init();
    }

    private void init() {
        parseComChangeMonitorsXml();
        if (!this.mMonitorList.isEmpty()) {
            initComChangeDir();
            if (this.mHandlerLooper == null) {
                HandlerThread handlerThread = new HandlerThread(TAG);
                handlerThread.start();
                this.mHandlerLooper = handlerThread.getLooper();
            }
            this.mWriteHandler = new WriteFileHandler(this.mHandlerLooper);
        }
    }

    private void initComChangeDir() {
        File gmsDir = new File(COMCHANGE_REPORT_FILE_DIR);
        if (!gmsDir.exists()) {
            Log.i(TAG, "comChange directory not exist, make it: " + gmsDir.mkdir());
        }
    }

    protected void writeComponetChangeLogToFile(ComponentName componentName, int newState, int userId) {
        if (componentName != null && (isMonitorComponent(componentName.getPackageName()) ^ 1) == 0 && this.mWriteHandler != null) {
            String enable;
            String callPkgName = getAppNameByPid(Binder.getCallingPid());
            if (newState <= 1) {
                enable = "enable";
            } else {
                enable = "disable";
            }
            String writeLine = "User{" + userId + "}: " + callPkgName + " " + enable + " " + componentName + " at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())) + ".\n";
            Log.i(TAG, "writeComponetChangeLogToFile writeLine: " + writeLine);
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(0, writeLine));
        }
    }

    private boolean isMonitorComponent(String appName) {
        if (this.mMonitorList.isEmpty()) {
            return false;
        }
        return this.mMonitorList.contains(appName);
    }

    private void writeLogToFile(String oneLine) {
        OutputStreamWriter osw;
        Exception e;
        Throwable th;
        BufferedWriter bufferedWriter = null;
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileOutputStream = null;
        try {
            FileOutputStream fos = new FileOutputStream(COMCHANGE_REPORT_FILE_DIR + COM_CHANGE_REPORT_FILE_LATEST, true);
            try {
                osw = new OutputStreamWriter(fos, "utf-8");
            } catch (FileNotFoundException e2) {
                fileOutputStream = fos;
                Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST FileNotFoundException");
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST out IOException");
                    }
                }
                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST osw IOException");
                    }
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST fos IOException");
                    }
                }
                checkFilesStatus();
            } catch (IOException e6) {
                fileOutputStream = fos;
                Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST IOException");
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e7) {
                        Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST out IOException");
                    }
                }
                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e8) {
                        Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST osw IOException");
                    }
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e9) {
                        Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST fos IOException");
                    }
                }
                checkFilesStatus();
            } catch (Exception e10) {
                e = e10;
                fileOutputStream = fos;
                try {
                    Log.e(TAG, "writeLogToFile Exception: ", e);
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e11) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST out IOException");
                        }
                    }
                    if (outputStreamWriter != null) {
                        try {
                            outputStreamWriter.close();
                        } catch (IOException e12) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST osw IOException");
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e13) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST fos IOException");
                        }
                    }
                    checkFilesStatus();
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e14) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST out IOException");
                        }
                    }
                    if (outputStreamWriter != null) {
                        try {
                            outputStreamWriter.close();
                        } catch (IOException e15) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST osw IOException");
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e16) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST fos IOException");
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileOutputStream = fos;
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                throw th;
            }
            try {
                BufferedWriter out = new BufferedWriter(osw);
                try {
                    out.write(oneLine);
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e17) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST out IOException");
                        }
                    } else {
                        bufferedWriter = out;
                    }
                    if (osw != null) {
                        try {
                            osw.close();
                        } catch (IOException e18) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST osw IOException");
                        }
                    } else {
                        outputStreamWriter = osw;
                    }
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e19) {
                            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST fos IOException");
                        }
                        checkFilesStatus();
                    }
                    checkFilesStatus();
                } catch (FileNotFoundException e20) {
                    fileOutputStream = fos;
                    outputStreamWriter = osw;
                    bufferedWriter = out;
                    Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST FileNotFoundException");
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    checkFilesStatus();
                } catch (IOException e21) {
                    fileOutputStream = fos;
                    outputStreamWriter = osw;
                    bufferedWriter = out;
                    Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST IOException");
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    checkFilesStatus();
                } catch (Exception e22) {
                    e = e22;
                    fileOutputStream = fos;
                    outputStreamWriter = osw;
                    bufferedWriter = out;
                    Log.e(TAG, "writeLogToFile Exception: ", e);
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    checkFilesStatus();
                } catch (Throwable th4) {
                    th = th4;
                    fileOutputStream = fos;
                    outputStreamWriter = osw;
                    bufferedWriter = out;
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    throw th;
                }
            } catch (FileNotFoundException e23) {
                fileOutputStream = fos;
                outputStreamWriter = osw;
                Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST FileNotFoundException");
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                checkFilesStatus();
            } catch (IOException e24) {
                fileOutputStream = fos;
                outputStreamWriter = osw;
                Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST IOException");
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                checkFilesStatus();
            } catch (Exception e25) {
                e = e25;
                fileOutputStream = fos;
                outputStreamWriter = osw;
                Log.e(TAG, "writeLogToFile Exception: ", e);
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                checkFilesStatus();
            } catch (Throwable th5) {
                th = th5;
                fileOutputStream = fos;
                outputStreamWriter = osw;
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e26) {
            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST FileNotFoundException");
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            checkFilesStatus();
        } catch (IOException e27) {
            Log.e(TAG, "COM_CHANGE_REPORT_FILE_LATEST IOException");
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            checkFilesStatus();
        } catch (Exception e28) {
            e = e28;
            Log.e(TAG, "writeLogToFile Exception: ", e);
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            checkFilesStatus();
        }
    }

    private void checkFilesStatus() {
        File[] subFiles = new File(COMCHANGE_REPORT_FILE_DIR).listFiles();
        if (subFiles != null) {
            int dirFileCount = subFiles.length;
            File tempFile;
            if (dirFileCount > 5) {
                Log.w(TAG, "deleteOldAndCreateNewFile abnormal file Count: " + dirFileCount);
                for (int fileIndex = 0; fileIndex < dirFileCount; fileIndex++) {
                    tempFile = subFiles[fileIndex];
                    if (!(tempFile == null || !tempFile.exists() || tempFile.delete())) {
                        Log.w(TAG, "checkFilesStatus delete failed: " + fileIndex);
                    }
                }
                return;
            }
            File latestFile = new File(COMCHANGE_REPORT_FILE_DIR + COM_CHANGE_REPORT_FILE_LATEST);
            if (latestFile.exists() && latestFile.length() > MemoryConstant.RECLAIM_KILL_GAP_MEMORY) {
                int i = dirFileCount;
                while (i > 0) {
                    tempFile = new File(COMCHANGE_REPORT_FILE_DIR + COM_CHANGE_REPORT_FILE_PREFIX + i);
                    if (tempFile.exists() && !tempFile.renameTo(new File(COMCHANGE_REPORT_FILE_DIR + COM_CHANGE_REPORT_FILE_PREFIX + (i + 1)))) {
                        Log.w(TAG, "checkFilesStatus rename failed: " + i);
                    }
                    i--;
                }
                if (5 == dirFileCount) {
                    File delFile = new File(COMCHANGE_REPORT_FILE_DIR + COM_CHANGE_REPORT_FILE_PREFIX + 6);
                    if (delFile.exists() && !delFile.delete()) {
                        Log.w(TAG, "checkFilesStatus delete last file failed");
                    }
                }
            }
        }
    }

    private String getAppNameByPid(int pid) {
        List<RunningAppProcessInfo> processes = getRunningProcesses();
        if (processes == null) {
            Log.d(TAG, "get app name, get running process failed");
            return null;
        }
        for (RunningAppProcessInfo processInfo : processes) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return null;
    }

    private List<RunningAppProcessInfo> getRunningProcesses() {
        if (this.mContext == null) {
            Log.d(TAG, "getRunningProcesses, mContext is null");
            return null;
        }
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (activityManager != null) {
            return activityManager.getRunningAppProcesses();
        }
        Log.d(TAG, "get process status, get ams service failed");
        return null;
    }

    private void parseComChangeMonitorsXml() {
        Throwable th;
        InputStream inputStream = null;
        File monitorFile = null;
        try {
            monitorFile = HwCfgFilePolicy.getCfgFile("xml/com_change_monitors.xml", 0);
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (monitorFile == null) {
            try {
                Log.i(TAG, "com_change_monitors.xml is not exist");
                return;
            } catch (FileNotFoundException e2) {
                try {
                    Log.e(TAG, "com_change_monitors.xml FileNotFoundException");
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e3) {
                            Log.e(TAG, "inputstream IOException");
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e4) {
                            Log.e(TAG, "inputstream IOException");
                        }
                    }
                    throw th;
                }
            } catch (XmlPullParserException e5) {
                Log.e(TAG, "com_change_monitors.xml XmlPullParserException");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e6) {
                        Log.e(TAG, "inputstream IOException");
                    }
                }
            } catch (IOException e7) {
                Log.e(TAG, "com_change_monitors.xml IOException");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e8) {
                        Log.e(TAG, "inputstream IOException");
                    }
                }
            } catch (Exception e9) {
                Exception e10 = e9;
                Log.e(TAG, "parseComChangeMonitorsXml e: ", e10);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e11) {
                        Log.e(TAG, "inputstream IOException");
                    }
                }
            }
        }
        InputStream inputstream = new FileInputStream(monitorFile);
        try {
            parseComChangeData(inputstream);
            if (inputstream != null) {
                try {
                    inputstream.close();
                } catch (IOException e12) {
                    Log.e(TAG, "inputstream IOException");
                }
            }
        } catch (FileNotFoundException e13) {
            inputStream = inputstream;
            Log.e(TAG, "com_change_monitors.xml FileNotFoundException");
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (XmlPullParserException e14) {
            inputStream = inputstream;
            Log.e(TAG, "com_change_monitors.xml XmlPullParserException");
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e15) {
            inputStream = inputstream;
            Log.e(TAG, "com_change_monitors.xml IOException");
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e16) {
            e10 = e16;
            inputStream = inputstream;
            Log.e(TAG, "parseComChangeMonitorsXml e: ", e10);
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Throwable th3) {
            th = th3;
            inputStream = inputstream;
            if (inputStream != null) {
                inputStream.close();
            }
            throw th;
        }
    }

    private void parseComChangeData(InputStream inputstream) throws XmlPullParserException, IOException {
        XmlPullParser pullParser = Xml.newPullParser();
        pullParser.setInput(inputstream, "UTF-8");
        for (int event = pullParser.getEventType(); event != 1; event = pullParser.next()) {
            switch (event) {
                case 2:
                    if (!"component".equals(pullParser.getName())) {
                        break;
                    }
                    this.mMonitorList.add(pullParser.nextText());
                    break;
                default:
                    break;
            }
        }
    }
}
