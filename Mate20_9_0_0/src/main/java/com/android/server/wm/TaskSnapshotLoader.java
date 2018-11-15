package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.util.Slog;
import com.android.server.wm.nano.WindowManagerProtos.TaskSnapshotProto;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

class TaskSnapshotLoader {
    private static final String TAG = "WindowManager";
    private final TaskSnapshotPersister mPersister;

    TaskSnapshotLoader(TaskSnapshotPersister persister) {
        this.mPersister = persister;
    }

    TaskSnapshot loadTask(int taskId, int userId, boolean reducedResolution) {
        File reducedResolutionBitmapFile;
        File file;
        int i = taskId;
        int i2 = userId;
        File protoFile = this.mPersister.getProtoFile(i, i2);
        if (reducedResolution) {
            reducedResolutionBitmapFile = this.mPersister.getReducedResolutionBitmapFile(i, i2);
        } else {
            reducedResolutionBitmapFile = this.mPersister.getBitmapFile(i, i2);
        }
        File bitmapFile = reducedResolutionBitmapFile;
        if (bitmapFile == null || !protoFile.exists()) {
        } else if (bitmapFile.exists()) {
            String str;
            StringBuilder stringBuilder;
            try {
                byte[] bytes = Files.readAllBytes(protoFile.toPath());
                TaskSnapshotProto proto = TaskSnapshotProto.parseFrom(bytes);
                Options options = new Options();
                options.inPreferredConfig = Config.HARDWARE;
                Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getPath(), options);
                if (bitmap == null) {
                    try {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to load bitmap: ");
                        stringBuilder.append(bitmapFile.getPath());
                        Slog.w(str, stringBuilder.toString());
                        return null;
                    } catch (IOException e) {
                        file = bitmapFile;
                    }
                } else {
                    GraphicBuffer buffer = bitmap.createGraphicBufferHandle();
                    if (buffer == null) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to retrieve gralloc buffer for bitmap: ");
                        stringBuilder.append(bitmapFile.getPath());
                        Slog.w(str, stringBuilder.toString());
                        return null;
                    }
                    int i3 = proto.orientation;
                    Rect rect = new Rect(proto.insetLeft, proto.insetTop, proto.insetRight, proto.insetBottom);
                    float f = reducedResolution ? TaskSnapshotPersister.REDUCED_SCALE : 1.0f;
                    boolean z = proto.isRealSnapshot;
                    int i4 = proto.windowingMode;
                    int i5 = proto.systemUiVisibility;
                    boolean z2 = proto.isTranslucent;
                    int i6 = i5;
                    TaskSnapshot taskSnapshot = taskSnapshot;
                    int i7 = i6;
                    try {
                        return new TaskSnapshot(buffer, i3, rect, reducedResolution, f, z, i4, i7, z2);
                    } catch (IOException e2) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to load task snapshot data for taskId=");
                        stringBuilder.append(i);
                        Slog.w(str, stringBuilder.toString());
                        return null;
                    }
                }
            } catch (IOException e3) {
                file = bitmapFile;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to load task snapshot data for taskId=");
                stringBuilder.append(i);
                Slog.w(str, stringBuilder.toString());
                return null;
            }
        } else {
            file = bitmapFile;
        }
        return null;
    }
}
