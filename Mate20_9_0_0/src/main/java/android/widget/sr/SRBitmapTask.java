package android.widget.sr;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.os.SystemClock;
import android.util.Log;

public class SRBitmapTask {
    private static final boolean DB = true;
    private static final String TAG = "SRBitmapTask";
    private static final int WAIT_TIME_OUT = 400;
    private Bitmap mAshmemBitmap;
    private Bitmap mBitmap;
    private volatile boolean mCondition = false;
    private boolean mSRSuccess = false;

    public SRBitmapTask(Bitmap bitmap) {
        this.mBitmap = bitmap;
        this.mAshmemBitmap = bitmap.createAshmemBitmap(Config.ARGB_8888);
    }

    public synchronized void setAshmemBitmap(Bitmap src, Bitmap dst) {
        if (this.mAshmemBitmap.equals(src)) {
            this.mSRSuccess = true;
            this.mAshmemBitmap = dst;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAshmemBitmap: src changed !!!! ");
        stringBuilder.append(src);
        stringBuilder.append(", ");
        stringBuilder.append(this.mBitmap);
        Log.w(str, stringBuilder.toString());
    }

    public synchronized Bitmap getAshmemBitmap() {
        return this.mAshmemBitmap;
    }

    public synchronized void waitTask(SRBitmapManagerImpl manager) {
        String str;
        StringBuilder stringBuilder;
        long startTime = SystemClock.elapsedRealtime();
        long startTimeForDebug = SystemClock.elapsedRealtime();
        long endMillis = 400 + startTime;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mCondition is ");
        stringBuilder2.append(this.mCondition);
        Log.i(str2, stringBuilder2.toString());
        while (!this.mCondition && startTime < endMillis) {
            try {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" start wait: ");
                stringBuilder2.append(this.mBitmap);
                Log.i(str2, stringBuilder2.toString());
                wait(endMillis - startTime);
            } catch (InterruptedException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("srBitmap: ");
                stringBuilder.append(e.toString());
                Log.e(str, stringBuilder.toString());
            }
            startTime = SystemClock.elapsedRealtime();
        }
        boolean z = true;
        if (SystemClock.elapsedRealtime() >= endMillis) {
            manager.removeTaskFromQueue(this);
            Bitmap bitmap = this.mBitmap;
            if (manager.getSRStatus() != 3) {
                z = false;
            }
            DebugUtil.debugTimeout(bitmap, z);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("time out : ");
            stringBuilder2.append(this.mBitmap);
            Log.w(str2, stringBuilder2.toString());
        } else if (this.mSRSuccess) {
            Canvas canvas = new Canvas(this.mBitmap);
            Paint paint = new Paint();
            canvas.drawColor(0, Mode.CLEAR);
            canvas.drawBitmap(this.mAshmemBitmap, 0.0f, 0.0f, paint);
            DebugUtil.debugDone(this.mBitmap, 3.0f, startTimeForDebug, true);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" success: ");
            stringBuilder.append(this.mBitmap);
            Log.i(str, stringBuilder.toString());
        } else {
            DebugUtil.debugDone(this.mBitmap, 5.0f, startTimeForDebug, false);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("quit : ");
            stringBuilder2.append(this.mBitmap);
            Log.i(str2, stringBuilder2.toString());
        }
    }

    public synchronized void notifyTask() {
        this.mCondition = true;
        notifyAll();
    }
}
