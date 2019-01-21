package android.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;

public final class SmartLog {
    private static final int MAX_CACHE_LINES = 20;
    private static final int MAX_LOG_LINES = 1000;
    private static final int MAX_MESSAGE_ID = 100000;
    private static final String TAG = "SmartLog";
    private static final Singleton<SmartLog> gDefault = new Singleton<SmartLog>() {
        protected SmartLog create() {
            return new SmartLog();
        }
    };
    private LinkedList<String> mCacheLog;
    private Handler mHandler;
    private LinkedList<String> mLog;
    private int mMessageId;

    private class LogHandler extends Handler {
        LogHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            SmartLog.this.moveCacheToLog();
            SmartLog smartLog = SmartLog.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("timeout(>1s):");
            stringBuilder.append(String.valueOf(msg.obj));
            smartLog.addToLog(stringBuilder.toString());
        }
    }

    public static class ReadOnlyLocalLog {
        private final SmartLog smartLog;

        ReadOnlyLocalLog(SmartLog log) {
            this.smartLog = log;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            this.smartLog.dump(fd, pw, args);
        }
    }

    /* synthetic */ SmartLog(AnonymousClass1 x0) {
        this();
    }

    private SmartLog() {
        this.mMessageId = -1;
        this.mLog = new LinkedList();
        this.mCacheLog = new LinkedList();
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new LogHandler(handlerThread.getLooper());
    }

    public static SmartLog getInstance() {
        return (SmartLog) gDefault.get();
    }

    public int startRecord(String log, int waitMillis) {
        int messageId = getMessageId();
        String msgLog = getLog(messageId, log);
        Message msg = new Message();
        msg.what = messageId;
        msg.obj = msgLog;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cache:");
        stringBuilder.append(msgLog);
        addToCache(stringBuilder.toString());
        this.mHandler.sendMessageDelayed(msg, (long) waitMillis);
        return messageId;
    }

    public void endRecord(int messageId, String log) {
        String msgLog = getLog(messageId, log);
        StringBuilder stringBuilder;
        if (this.mHandler.hasMessages(messageId)) {
            this.mHandler.removeMessages(messageId);
            stringBuilder = new StringBuilder();
            stringBuilder.append("cache:");
            stringBuilder.append(msgLog);
            addToCache(stringBuilder.toString());
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("timeout:");
        stringBuilder.append(msgLog);
        addToLog(stringBuilder.toString());
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Iterator<String> itr = this.mLog.listIterator(0);
        while (itr.hasNext()) {
            pw.println((String) itr.next());
        }
    }

    public synchronized void reverseDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        for (int i = this.mLog.size() - 1; i >= 0; i--) {
            pw.println((String) this.mLog.get(i));
        }
    }

    public ReadOnlyLocalLog readOnlyLocalLog() {
        return new ReadOnlyLocalLog(this);
    }

    private synchronized void addToLog(String msg) {
        this.mLog.add(msg);
        while (this.mLog.size() > 1000) {
            Log.e(TAG, (String) this.mLog.remove());
        }
    }

    private synchronized void addToCache(String msg) {
        this.mCacheLog.add(msg);
        while (this.mCacheLog.size() > 20) {
            this.mCacheLog.remove();
        }
    }

    private synchronized void moveCacheToLog() {
        while (this.mCacheLog.size() > 0) {
            addToLog((String) this.mCacheLog.remove());
        }
    }

    private synchronized String getLog(int msgId, String msg) {
        StringBuilder stringBuilder;
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        Calendar.getInstance().setTimeInMillis(now);
        sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", new Object[]{c, c, c, c, c, c}));
        stringBuilder = new StringBuilder();
        stringBuilder.append(msgId);
        stringBuilder.append(":");
        stringBuilder.append(sb.toString());
        stringBuilder.append(msg);
        return stringBuilder.toString();
    }

    private synchronized int getMessageId() {
        int i;
        i = this.mMessageId + 1;
        this.mMessageId = i;
        return i / MAX_MESSAGE_ID;
    }
}
