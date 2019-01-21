package android.graphics;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.res.Resources;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.util.LruCache;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class AwareBitmapCacher implements IAwareBitmapCacher {
    private static final String BITMAP_CACHER_SIZE = "persist.sys.iaware.size.BitmapDeocodeCache";
    private static final String BITMAP_CACHER_SWITCH = "persist.sys.iaware.switch.BitmapDeocodeCache";
    private static final int INIT_FIAILED_STATUS = -2;
    private static final int MAX_BITMAP_POINT = 1000;
    private static final int MAX_BITMAP_SIZE = 2000000;
    private static final int MSG_BITMAP_CACHER_INIT = 1000;
    private static final int MSG_BITMAP_CACHER_RELEASE = 1002;
    private static final int MSG_CHECK_IS_BG_AND_RELEASE = 1001;
    private static final int MUTIUSER_ADD_UID = 100000;
    private static final int SYSTEM_UID = 1000;
    private static final String TAG = "AwareBitmapCacher";
    private static final int TIME_DELAY_BITMAP_CACHER_INIT = 5000;
    private static final int TIME_DELAY_CACHE_RELEASE = 60000;
    private static final int TIME_DELAY_CHECK_RELEASE = 5000;
    private static final int UNINITED_STATUS = -1;
    private static final int UNIT_K2BYTES = 10;
    private static final String groupBG = "background";
    private static volatile AwareBitmapCacher mInstance = new AwareBitmapCacher();
    private static final String matcher = "cpuset";
    private Application mApplication;
    private int mBitampCacherSize = -1;
    private boolean mBitmapCacherSwitch = false;
    private LruCache<String, Bitmap> mLruCache;
    private MyHandler mMyHandler;
    private String mProcessName;
    private ReadLock mReadLock;
    private ReentrantReadWriteLock mReadWriteLock;
    private WriteLock mWriteLock;

    public static class FileContent {
        public static void close(BufferedReader br) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e(AwareBitmapCacher.TAG, "close exception!");
                }
            }
        }

        public static void close(InputStreamReader isr) {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    Log.e(AwareBitmapCacher.TAG, "close exception!");
                }
            }
        }

        public static void close(FileInputStream fis) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(AwareBitmapCacher.TAG, "close exception!");
                }
            }
        }
    }

    class MyActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {
        MyActivityLifecycleCallbacks() {
        }

        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        public void onActivityStarted(Activity activity) {
        }

        public void onActivityResumed(Activity activity) {
            if (AwareBitmapCacher.this.mMyHandler != null) {
                AwareBitmapCacher.this.mMyHandler.removeMessages(1002);
            }
        }

        public void onActivityPaused(Activity activity) {
            if (AwareBitmapCacher.this.mMyHandler != null) {
                AwareBitmapCacher.this.mMyHandler.sendEmptyMessageDelayed(1001, 5000);
            }
        }

        public void onActivityStopped(Activity activity) {
        }

        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        public void onActivityDestroyed(Activity activity) {
        }
    }

    private class MyHandler extends Handler {
        public MyHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            if (msg == null) {
                Log.e(AwareBitmapCacher.TAG, "null == msg");
                return;
            }
            switch (msg.what) {
                case 1000:
                    AwareBitmapCacher.this.handleInit();
                    break;
                case 1001:
                    removeMessages(1002);
                    AwareBitmapCacher.this.handleCheckBgAndRelease();
                    break;
                case 1002:
                    AwareBitmapCacher.this.handleReleaseCache();
                    break;
            }
        }
    }

    public static IAwareBitmapCacher getDefault() {
        return mInstance;
    }

    public void init(String processName, Application app) {
        if (app != null && processName != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("init processName:");
            stringBuilder.append(processName);
            stringBuilder.append(" pid=");
            stringBuilder.append(Process.myPid());
            stringBuilder.append(" uid=");
            stringBuilder.append(Process.myUid());
            Log.i(str, stringBuilder.toString());
            this.mApplication = app;
            this.mProcessName = processName;
            this.mMyHandler = new MyHandler();
            this.mMyHandler.sendEmptyMessageDelayed(1000, 5000);
        }
    }

    private void initCache(int cacheSize) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("init lrucache size: ");
        stringBuilder.append(cacheSize);
        stringBuilder.append(" pid=");
        stringBuilder.append(Process.myPid());
        Log.i(str, stringBuilder.toString());
        if (cacheSize > 0) {
            this.mWriteLock.lock();
            try {
                this.mLruCache = new LruCache<String, Bitmap>(cacheSize) {
                    protected int sizeOf(String key, Bitmap value) {
                        return value.getByteCount();
                    }
                };
            } finally {
                this.mWriteLock.unlock();
            }
        }
    }

    private void handleInit() {
        try {
            String str;
            StringBuilder stringBuilder;
            if (this.mBitampCacherSize != -1) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleInit reinit pid=");
                stringBuilder2.append(Process.myPid());
                Log.d(str2, stringBuilder2.toString());
                if (this.mBitampCacherSize > 0) {
                    return;
                }
            } else if (Process.myUid() % MUTIUSER_ADD_UID <= 1000) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleInit system app disable uid=");
                stringBuilder.append(Process.myUid());
                Log.d(str, stringBuilder.toString());
                if (this.mBitampCacherSize > 0) {
                    return;
                }
            } else if (this.mProcessName == null) {
                Log.e(TAG, "handleInit disable mProcessName=null");
                if (this.mBitampCacherSize > 0) {
                    return;
                }
            } else {
                if (!(this.mProcessName.contains("com.android.") || this.mProcessName.contains("com.huawei.") || this.mProcessName.contains("com.google.") || this.mProcessName.contains("android.process."))) {
                    if (!this.mProcessName.contains(":")) {
                        this.mBitmapCacherSwitch = SystemProperties.getBoolean(BITMAP_CACHER_SWITCH, false);
                        if (this.mBitmapCacherSwitch) {
                            this.mBitampCacherSize = SystemProperties.getInt(BITMAP_CACHER_SIZE, 0);
                            if (this.mBitampCacherSize > 0) {
                                this.mBitampCacherSize <<= 10;
                                registerActivityCallback();
                                this.mReadWriteLock = new ReentrantReadWriteLock();
                                this.mReadLock = this.mReadWriteLock.readLock();
                                this.mWriteLock = this.mReadWriteLock.writeLock();
                                initCache(this.mBitampCacherSize);
                                return;
                            }
                        }
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("handleInit switch not opened pid=");
                        stringBuilder.append(Process.myPid());
                        Log.d(str, stringBuilder.toString());
                        if (this.mBitampCacherSize > 0) {
                            return;
                        }
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleInit disable ");
                stringBuilder.append(this.mProcessName);
                Log.d(str, stringBuilder.toString());
                if (this.mBitampCacherSize > 0) {
                    return;
                }
            }
        } catch (Throwable th) {
            if (this.mBitampCacherSize > 0) {
            }
        }
        this.mBitampCacherSize = -2;
        this.mApplication = null;
        this.mProcessName = null;
        this.mMyHandler = null;
    }

    private void registerActivityCallback() {
        if (this.mApplication != null) {
            this.mApplication.registerActivityLifecycleCallbacks(new MyActivityLifecycleCallbacks());
        }
    }

    public Bitmap getCachedBitmap(String pathName) {
        Bitmap bitmap = null;
        if (this.mLruCache == null) {
            return null;
        }
        if (pathName == null) {
            Log.e(TAG, "getCachedBitmap pathName null");
            return null;
        }
        this.mReadLock.lock();
        try {
            Bitmap bm = (Bitmap) this.mLruCache.get(pathName);
            if (bm != null) {
                if (!bm.isRecycled()) {
                    if (bm.getByteCount() > 0) {
                        bm.incReference();
                    }
                }
                this.mLruCache.remove(pathName);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getCachedBitmap remove for isRecycled @pathName=");
                stringBuilder.append(pathName);
                Log.i(str, stringBuilder.toString());
                return bitmap;
            }
            this.mReadLock.unlock();
            return bm;
        } finally {
            bitmap = this.mReadLock;
            bitmap.unlock();
        }
    }

    public void cacheBitmap(String pathName, Bitmap bitmap, Options opts) {
        if (this.mLruCache != null) {
            if (pathName == null) {
                Log.e(TAG, "cacheBitmap pathName null");
                return;
            }
            this.mWriteLock.lock();
            if (bitmap != null) {
                try {
                    if (!bitmap.isRecycled()) {
                        int bmByteCount = bitmap.getByteCount();
                        if (bmByteCount < MAX_BITMAP_SIZE) {
                            if (bmByteCount > 0) {
                                this.mLruCache.put(pathName, bitmap);
                                this.mWriteLock.unlock();
                                if (this.mMyHandler != null) {
                                    this.mMyHandler.removeMessages(1002);
                                    this.mMyHandler.sendEmptyMessageDelayed(1002, 60000);
                                }
                                return;
                            }
                        }
                        this.mWriteLock.unlock();
                        return;
                    }
                } catch (Throwable th) {
                    this.mWriteLock.unlock();
                }
            }
            Log.i(TAG, "cacheBitmap bitmap null");
            this.mWriteLock.unlock();
        }
    }

    public Bitmap getCachedBitmap(Resources res, int id) {
        return getCachedBitmap(getString(res, id));
    }

    public void cacheBitmap(Resources res, int id, Bitmap bitmap, Options opts) {
        cacheBitmap(getString(res, id), bitmap, opts);
    }

    private void handleReleaseCache() {
        if (this.mLruCache != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleReleaseCache: pid=");
            stringBuilder.append(Process.myPid());
            Log.i(str, stringBuilder.toString());
            initCache(this.mBitampCacherSize);
        }
    }

    private String getString(Resources res, int id) {
        if (res == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(res.toString());
        stringBuilder.append(id);
        return stringBuilder.toString();
    }

    private void handleCheckBgAndRelease() {
        int myPid = Process.myPid();
        String pathName = new StringBuilder();
        pathName.append("/proc/");
        pathName.append(myPid);
        pathName.append("/cgroup");
        pathName = pathName.toString();
        File tmpFile = new File(pathName);
        if (tmpFile.exists() && tmpFile.canRead()) {
            boolean procIsBg = true;
            FileInputStream fis = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;
            try {
                String str;
                fis = new FileInputStream(tmpFile);
                isr = new InputStreamReader(fis, "UTF-8");
                reader = new BufferedReader(isr);
                do {
                    str = reader.readLine();
                    if (str == null) {
                        break;
                    }
                } while (!str.contains(matcher));
                procIsBg = str.contains(groupBG);
            } catch (NumberFormatException e) {
                Log.e(TAG, "NumberFormatException!");
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "FileNotFoundException!");
            } catch (UnsupportedEncodingException e3) {
                Log.e(TAG, "UnsupportedEncodingException!");
            } catch (IOException e4) {
                Log.e(TAG, "IOException!");
            } catch (Throwable th) {
                FileContent.close(reader);
                FileContent.close(null);
                FileContent.close(null);
            }
            FileContent.close(reader);
            FileContent.close(isr);
            FileContent.close(fis);
            if (procIsBg) {
                handleReleaseCache();
            }
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleCheckBgAndRelease can't access:");
        stringBuilder.append(pathName);
        Log.e(str2, stringBuilder.toString());
    }
}
