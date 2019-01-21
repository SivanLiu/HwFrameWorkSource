package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.SettingsEx.Systemex;
import android.provider.SettingsStringUtil;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup.LayoutParams;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ViewDebug {
    private static final int CAPTURE_TIMEOUT = 4000;
    public static final boolean DEBUG_DRAG = false;
    public static final boolean DEBUG_POSITIONING = false;
    private static final String DUMPC_CUST_FIELDSLIST = "dumpc_cust_fields";
    private static final String DUMPC_CUST_METHODSLIST = "dumpc_cust_methods";
    private static final String REMOTE_COMMAND_CAPTURE = "CAPTURE";
    private static final String REMOTE_COMMAND_CAPTURE_LAYERS = "CAPTURE_LAYERS";
    private static final String REMOTE_COMMAND_DUMP = "DUMP";
    private static String REMOTE_COMMAND_DUMP_CUST = SystemProperties.get("ro.config.autotestdump", "DUMPC");
    private static final String REMOTE_COMMAND_DUMP_THEME = "DUMP_THEME";
    private static final String REMOTE_COMMAND_INVALIDATE = "INVALIDATE";
    private static final String REMOTE_COMMAND_OUTPUT_DISPLAYLIST = "OUTPUT_DISPLAYLIST";
    private static final String REMOTE_COMMAND_REQUEST_LAYOUT = "REQUEST_LAYOUT";
    private static final String REMOTE_PROFILE = "PROFILE";
    @Deprecated
    public static final boolean TRACE_HIERARCHY = false;
    @Deprecated
    public static final boolean TRACE_RECYCLER = false;
    private static HashMap<Class<?>, Field[]> mCapturedViewFieldsForClasses = null;
    private static HashMap<Class<?>, Method[]> mCapturedViewMethodsForClasses = null;
    private static boolean mCustomizedDump = false;
    private static String[] mFieldsList = new String[]{"mText", "absolute_x", "absolute_y", "mID", "mLeft", "mTop", "mScrollX", "mScrollY", "x", "y"};
    private static boolean mInited = false;
    private static String[] mMethodsList = new String[]{"isSelected", "isClickable", "isEnabled", "getWidth", "getHeight", "getTag", "isChecked", "isActivated", "getVisibility", "getLayoutParams"};
    private static HashMap<AccessibleObject, ExportedProperty> sAnnotations;
    private static HashMap<Class<?>, Field[]> sCustFieldsForClasses = new HashMap();
    private static HashMap<Class<?>, Method[]> sCustMethodsForClasses = new HashMap();
    private static HashMap<Class<?>, Field[]> sFieldsForClasses;
    private static HashMap<Class<?>, Method[]> sMethodsForClasses;

    public interface CanvasProvider {
        Bitmap createBitmap();

        Canvas getCanvas(View view, int i, int i2);
    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CapturedViewProperty {
        boolean retrieveReturn() default false;
    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExportedProperty {
        String category() default "";

        boolean deepExport() default false;

        FlagToString[] flagMapping() default {};

        boolean formatToHexString() default false;

        boolean hasAdjacentMapping() default false;

        IntToString[] indexMapping() default {};

        IntToString[] mapping() default {};

        String prefix() default "";

        boolean resolveId() default false;
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FlagToString {
        int equals();

        int mask();

        String name();

        boolean outputIf() default true;
    }

    public interface HierarchyHandler {
        void dumpViewHierarchyWithProperties(BufferedWriter bufferedWriter, int i);

        View findHierarchyView(String str, int i);
    }

    @Deprecated
    public enum HierarchyTraceType {
        INVALIDATE,
        INVALIDATE_CHILD,
        INVALIDATE_CHILD_IN_PARENT,
        REQUEST_LAYOUT,
        ON_LAYOUT,
        ON_MEASURE,
        DRAW,
        BUILD_CACHE
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IntToString {
        int from();

        String to();
    }

    @Deprecated
    public enum RecyclerTraceType {
        NEW_VIEW,
        BIND_VIEW,
        RECYCLE_FROM_ACTIVE_HEAP,
        RECYCLE_FROM_SCRAP_HEAP,
        MOVE_TO_SCRAP_HEAP,
        MOVE_FROM_ACTIVE_TO_SCRAP_HEAP
    }

    interface ViewOperation {
        void run();

        void pre() {
        }
    }

    public static class HardwareCanvasProvider implements CanvasProvider {
        private Picture mPicture;

        public Canvas getCanvas(View view, int width, int height) {
            this.mPicture = new Picture();
            return this.mPicture.beginRecording(width, height);
        }

        public Bitmap createBitmap() {
            this.mPicture.endRecording();
            return Bitmap.createBitmap(this.mPicture);
        }
    }

    public static class SoftwareCanvasProvider implements CanvasProvider {
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private boolean mEnabledHwBitmapsInSwMode;

        public Canvas getCanvas(View view, int width, int height) {
            this.mBitmap = Bitmap.createBitmap(view.getResources().getDisplayMetrics(), width, height, Config.ARGB_8888);
            if (this.mBitmap != null) {
                this.mBitmap.setDensity(view.getResources().getDisplayMetrics().densityDpi);
                if (view.mAttachInfo != null) {
                    this.mCanvas = view.mAttachInfo.mCanvas;
                }
                if (this.mCanvas == null) {
                    this.mCanvas = new Canvas();
                }
                this.mEnabledHwBitmapsInSwMode = this.mCanvas.isHwBitmapsInSwModeEnabled();
                this.mCanvas.setBitmap(this.mBitmap);
                return this.mCanvas;
            }
            throw new OutOfMemoryError();
        }

        public Bitmap createBitmap() {
            this.mCanvas.setBitmap(null);
            this.mCanvas.setHwBitmapsInSwModeEnabled(this.mEnabledHwBitmapsInSwMode);
            return this.mBitmap;
        }
    }

    public static long getViewInstanceCount() {
        return Debug.countInstancesOfClass(View.class);
    }

    public static long getViewRootImplCount() {
        return Debug.countInstancesOfClass(ViewRootImpl.class);
    }

    @Deprecated
    public static void trace(View view, RecyclerTraceType type, int... parameters) {
    }

    @Deprecated
    public static void startRecyclerTracing(String prefix, View view) {
    }

    @Deprecated
    public static void stopRecyclerTracing() {
    }

    @Deprecated
    public static void trace(View view, HierarchyTraceType type) {
    }

    @Deprecated
    public static void startHierarchyTracing(String prefix, View view) {
    }

    @Deprecated
    public static void stopHierarchyTracing() {
    }

    static void dispatchCommand(View view, String command, String parameters, OutputStream clientStream) throws IOException {
        view = view.getRootView();
        if (REMOTE_COMMAND_DUMP_CUST.equalsIgnoreCase(command)) {
            customizedDump(view, clientStream);
        } else if (REMOTE_COMMAND_DUMP.equalsIgnoreCase(command)) {
            dump(view, false, true, clientStream);
        } else if (REMOTE_COMMAND_DUMP_THEME.equalsIgnoreCase(command)) {
            dumpTheme(view, clientStream);
        } else if (REMOTE_COMMAND_CAPTURE_LAYERS.equalsIgnoreCase(command)) {
            captureLayers(view, new DataOutputStream(clientStream));
        } else {
            String[] params = parameters.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            if (REMOTE_COMMAND_CAPTURE.equalsIgnoreCase(command)) {
                capture(view, clientStream, params[0]);
            } else if (REMOTE_COMMAND_OUTPUT_DISPLAYLIST.equalsIgnoreCase(command)) {
                outputDisplayList(view, params[0]);
            } else if (REMOTE_COMMAND_INVALIDATE.equalsIgnoreCase(command)) {
                invalidate(view, params[0]);
            } else if (REMOTE_COMMAND_REQUEST_LAYOUT.equalsIgnoreCase(command)) {
                requestLayout(view, params[0]);
            } else if (REMOTE_PROFILE.equalsIgnoreCase(command)) {
                profile(view, clientStream, params[0]);
            }
        }
    }

    public static View findView(View root, String parameter) {
        if (parameter.indexOf(64) != -1) {
            String[] ids = parameter.split("@");
            String className = ids[null];
            int hashCode = (int) Long.parseLong(ids[1], 16);
            View view = root.getRootView();
            if (view instanceof ViewGroup) {
                return findView((ViewGroup) view, className, hashCode);
            }
            return null;
        }
        return root.getRootView().findViewById(root.getResources().getIdentifier(parameter, null, null));
    }

    private static void invalidate(View root, String parameter) {
        View view = findView(root, parameter);
        if (view != null) {
            view.postInvalidate();
        }
    }

    private static void requestLayout(View root, String parameter) {
        final View view = findView(root, parameter);
        if (view != null) {
            root.post(new Runnable() {
                public void run() {
                    view.requestLayout();
                }
            });
        }
    }

    private static void profile(View root, OutputStream clientStream, String parameter) throws IOException {
        View view = findView(root, parameter);
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(clientStream), 32768);
            if (view != null) {
                profileViewAndChildren(view, out);
            } else {
                out.write("-1 -1 -1");
                out.newLine();
            }
            out.write("DONE.");
            out.newLine();
        } catch (Exception e) {
            Log.w("View", "Problem profiling the view:", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void profileViewAndChildren(View view, BufferedWriter out) throws IOException {
        RenderNode node = RenderNode.create("ViewDebug", null);
        profileViewAndChildren(view, node, out, true);
        node.destroy();
    }

    private static void profileViewAndChildren(View view, RenderNode node, BufferedWriter out, boolean root) throws IOException {
        long durationDraw = 0;
        long durationMeasure = (root || (view.mPrivateFlags & 2048) != 0) ? profileViewMeasure(view) : 0;
        long durationLayout = (root || (view.mPrivateFlags & 8192) != 0) ? profileViewLayout(view) : 0;
        if (!(!root && view.willNotDraw() && (view.mPrivateFlags & 32) == 0)) {
            durationDraw = profileViewDraw(view, node);
        }
        out.write(String.valueOf(durationMeasure));
        out.write(32);
        out.write(String.valueOf(durationLayout));
        out.write(32);
        out.write(String.valueOf(durationDraw));
        out.newLine();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                profileViewAndChildren(group.getChildAt(i), node, out, false);
            }
        }
    }

    private static long profileViewMeasure(final View view) {
        return profileViewOperation(view, new ViewOperation() {
            public void pre() {
                forceLayout(view);
            }

            private void forceLayout(View view) {
                view.forceLayout();
                if (view instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view;
                    int count = group.getChildCount();
                    for (int i = 0; i < count; i++) {
                        forceLayout(group.getChildAt(i));
                    }
                }
            }

            public void run() {
                view.measure(view.mOldWidthMeasureSpec, view.mOldHeightMeasureSpec);
            }
        });
    }

    private static long profileViewLayout(View view) {
        return profileViewOperation(view, new -$$Lambda$ViewDebug$inOytI2zZEgp1DJv8Cu4GjQVNiE(view));
    }

    private static long profileViewDraw(View view, RenderNode node) {
        DisplayMetrics dm = view.getResources().getDisplayMetrics();
        if (dm == null) {
            return 0;
        }
        if (view.isHardwareAccelerated()) {
            DisplayListCanvas canvas = node.start(dm.widthPixels, dm.heightPixels);
            try {
                long profileViewOperation = profileViewOperation(view, new -$$Lambda$ViewDebug$bI5XH5th0NpBuGdZohDPLEpO2Ek(view, canvas));
                return profileViewOperation;
            } finally {
                node.end(canvas);
            }
        } else {
            Bitmap bitmap = Bitmap.createBitmap(dm, dm.widthPixels, dm.heightPixels, Config.RGB_565);
            Canvas canvas2 = new Canvas(bitmap);
            try {
                long profileViewOperation2 = profileViewOperation(view, new -$$Lambda$ViewDebug$w986pBwzwNi77yEgLa3IWusjPNw(view, canvas2));
                return profileViewOperation2;
            } finally {
                canvas2.setBitmap(null);
                bitmap.recycle();
            }
        }
    }

    private static long profileViewOperation(View view, ViewOperation operation) {
        CountDownLatch latch = new CountDownLatch(1);
        long[] duration = new long[1];
        view.post(new -$$Lambda$ViewDebug$5rTN0pemwbr3I3IL2E-xDBeDTDg(operation, duration, latch));
        try {
            if (latch.await(4000, TimeUnit.MILLISECONDS)) {
                return duration[0];
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not complete the profiling of the view ");
            stringBuilder.append(view);
            Log.w("View", stringBuilder.toString());
            return -1;
        } catch (InterruptedException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Could not complete the profiling of the view ");
            stringBuilder2.append(view);
            Log.w("View", stringBuilder2.toString());
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    static /* synthetic */ void lambda$profileViewOperation$3(ViewOperation operation, long[] duration, CountDownLatch latch) {
        try {
            operation.pre();
            long start = Debug.threadCpuTimeNanos();
            operation.run();
            duration[0] = Debug.threadCpuTimeNanos() - start;
        } finally {
            latch.countDown();
        }
    }

    public static void captureLayers(View root, DataOutputStream clientStream) throws IOException {
        try {
            Rect outRect = new Rect();
            try {
                root.mAttachInfo.mSession.getDisplayFrame(root.mAttachInfo.mWindow, outRect);
            } catch (RemoteException e) {
            }
            clientStream.writeInt(outRect.width());
            clientStream.writeInt(outRect.height());
            captureViewLayer(root, clientStream, true);
            clientStream.write(2);
        } finally {
            clientStream.close();
        }
    }

    private static void captureViewLayer(View view, DataOutputStream clientStream, boolean visible) throws IOException {
        int id;
        int i = 0;
        boolean localVisible = view.getVisibility() == 0 && visible;
        if ((view.mPrivateFlags & 128) != 128) {
            id = view.getId();
            String name = view.getClass().getSimpleName();
            if (id != -1) {
                name = resolveId(view.getContext(), id).toString();
            }
            clientStream.write(1);
            clientStream.writeUTF(name);
            clientStream.writeByte(localVisible ? 1 : 0);
            int[] position = new int[2];
            view.getLocationInWindow(position);
            clientStream.writeInt(position[0]);
            clientStream.writeInt(position[1]);
            clientStream.flush();
            Bitmap b = performViewCapture(view, true);
            if (b != null) {
                ByteArrayOutputStream arrayOut = new ByteArrayOutputStream((b.getWidth() * b.getHeight()) * 2);
                b.compress(CompressFormat.PNG, 100, arrayOut);
                clientStream.writeInt(arrayOut.size());
                arrayOut.writeTo(clientStream);
            }
            clientStream.flush();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            id = group.getChildCount();
            while (i < id) {
                captureViewLayer(group.getChildAt(i), clientStream, localVisible);
                i++;
            }
        }
        if (view.mOverlay != null) {
            captureViewLayer(view.getOverlay().mOverlayViewGroup, clientStream, localVisible);
        }
    }

    private static void outputDisplayList(View root, String parameter) throws IOException {
        View view = findView(root, parameter);
        view.getViewRootImpl().outputDisplayList(view);
    }

    public static void outputDisplayList(View root, View target) {
        root.getViewRootImpl().outputDisplayList(target);
    }

    private static void capture(View root, OutputStream clientStream, String parameter) throws IOException {
        capture(root, clientStream, findView(root, parameter));
    }

    public static void capture(View root, OutputStream clientStream, View captureView) throws IOException {
        Bitmap b = performViewCapture(captureView, null);
        if (b == null) {
            Log.w("View", "Failed to create capture bitmap!");
            b = Bitmap.createBitmap(root.getResources().getDisplayMetrics(), 1, 1, Config.ARGB_8888);
        }
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(clientStream, 32768);
            b.compress(CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            b.recycle();
        } catch (Throwable th) {
            if (out != null) {
                out.close();
            }
            b.recycle();
        }
    }

    private static Bitmap performViewCapture(View captureView, boolean skipChildren) {
        if (captureView != null) {
            CountDownLatch latch = new CountDownLatch(1);
            Bitmap[] cache = new Bitmap[1];
            captureView.post(new -$$Lambda$ViewDebug$SYbJuwHeGrHQLha0YsHp4VI9JLg(captureView, cache, skipChildren, latch));
            try {
                latch.await(4000, TimeUnit.MILLISECONDS);
                return cache[0];
            } catch (InterruptedException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not complete the capture of the view ");
                stringBuilder.append(captureView);
                Log.w("View", stringBuilder.toString());
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    static /* synthetic */ void lambda$performViewCapture$4(View captureView, Bitmap[] cache, boolean skipChildren, CountDownLatch latch) {
        try {
            cache[0] = captureView.createSnapshot(captureView.isHardwareAccelerated() ? new HardwareCanvasProvider() : new SoftwareCanvasProvider(), skipChildren);
        } catch (OutOfMemoryError e) {
            Log.w("View", "Out of memory for bitmap");
        } catch (Throwable th) {
            latch.countDown();
        }
        latch.countDown();
    }

    @Deprecated
    public static void dump(View root, boolean skipChildren, boolean includeProperties, OutputStream clientStream) throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(clientStream, "utf-8"), 32768);
            View view = root.getRootView();
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                dumpViewHierarchy(group.getContext(), group, out, 0, skipChildren, includeProperties);
            }
            out.write("DONE.");
            out.newLine();
        } catch (Exception e) {
            Log.w("View", "Problem dumping the view:", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void dumpv2(final View view, ByteArrayOutputStream out) throws InterruptedException {
        final ViewHierarchyEncoder encoder = new ViewHierarchyEncoder(out);
        final CountDownLatch latch = new CountDownLatch(1);
        view.post(new Runnable() {
            public void run() {
                encoder.addProperty("window:left", view.mAttachInfo.mWindowLeft);
                encoder.addProperty("window:top", view.mAttachInfo.mWindowTop);
                view.encode(encoder);
                latch.countDown();
            }
        });
        latch.await(2, TimeUnit.SECONDS);
        encoder.endStream();
    }

    public static void dumpTheme(View view, OutputStream clientStream) throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(clientStream, "utf-8"), 32768);
            String[] attributes = getStyleAttributesDump(view.getContext().getResources(), view.getContext().getTheme());
            if (attributes != null) {
                for (int i = 0; i < attributes.length; i += 2) {
                    if (attributes[i] != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(attributes[i]);
                        stringBuilder.append("\n");
                        out.write(stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(attributes[i + 1]);
                        stringBuilder.append("\n");
                        out.write(stringBuilder.toString());
                    }
                }
            }
            out.write("DONE.");
            out.newLine();
        } catch (Exception e) {
            Log.w("View", "Problem dumping View Theme:", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static String[] getStyleAttributesDump(Resources resources, Theme theme) {
        TypedValue outValue = new TypedValue();
        String nullString = "null";
        int i = 0;
        int[] attributes = theme.getAllAttributes();
        String[] data = new String[(attributes.length * 2)];
        for (int attributeId : attributes) {
            try {
                data[i] = resources.getResourceName(attributeId);
                data[i + 1] = theme.resolveAttribute(attributeId, outValue, true) ? outValue.coerceToString().toString() : nullString;
                i += 2;
                if (outValue.type == 1) {
                    data[i - 1] = resources.getResourceName(outValue.resourceId);
                }
            } catch (NotFoundException e) {
            }
        }
        return data;
    }

    private static View findView(ViewGroup group, String className, int hashCode) {
        if (isRequestedView(group, className, hashCode)) {
            return group;
        }
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View found;
            View view = group.getChildAt(i);
            if (view instanceof ViewGroup) {
                found = findView((ViewGroup) view, className, hashCode);
                if (found != null) {
                    return found;
                }
            } else if (isRequestedView(view, className, hashCode)) {
                return view;
            }
            if (view.mOverlay != null) {
                found = findView(view.mOverlay.mOverlayViewGroup, className, hashCode);
                if (found != null) {
                    return found;
                }
            }
            if (view instanceof HierarchyHandler) {
                found = ((HierarchyHandler) view).findHierarchyView(className, hashCode);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean isRequestedView(View view, String className, int hashCode) {
        if (view.hashCode() != hashCode) {
            return false;
        }
        String viewClassName = view.getClass().getName();
        if (className.equals("ViewOverlay")) {
            return viewClassName.equals("android.view.ViewOverlay$OverlayViewGroup");
        }
        return className.equals(viewClassName);
    }

    private static void dumpViewHierarchy(Context context, ViewGroup group, BufferedWriter out, int level, boolean skipChildren, boolean includeProperties) {
        Context context2 = context;
        View view = group;
        BufferedWriter bufferedWriter = out;
        int i = level;
        boolean z = includeProperties;
        if (dumpView(context2, view, bufferedWriter, i, z) && !skipChildren) {
            int count = group.getChildCount();
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 >= count) {
                    break;
                }
                View view2 = view.getChildAt(i3);
                if (view2 instanceof ViewGroup) {
                    dumpViewHierarchy(context2, (ViewGroup) view2, bufferedWriter, i + 1, skipChildren, z);
                } else {
                    dumpView(context2, view2, bufferedWriter, i + 1, z);
                }
                if (view2.mOverlay != null) {
                    ViewGroup overlayContainer = view2.getOverlay().mOverlayViewGroup;
                    dumpViewHierarchy(context2, overlayContainer, bufferedWriter, i + 2, skipChildren, z);
                }
                i2 = i3 + 1;
            }
            if (view instanceof HierarchyHandler) {
                ((HierarchyHandler) view).dumpViewHierarchyWithProperties(bufferedWriter, i + 1);
            }
        }
    }

    private static boolean dumpView(Context context, View view, BufferedWriter out, int level, boolean includeProperties) {
        int i = 0;
        while (i < level) {
            try {
                out.write(32);
                i++;
            } catch (IOException e) {
                Log.w("View", "Error while dumping hierarchy tree");
                return false;
            }
        }
        IOException e2 = view.getClass().getName();
        if (e2.equals("android.view.ViewOverlay$OverlayViewGroup")) {
            e2 = "ViewOverlay";
        }
        out.write(e2);
        out.write(64);
        out.write(Integer.toHexString(view.hashCode()));
        out.write(32);
        if (includeProperties) {
            dumpViewProperties(context, view, out);
        }
        out.newLine();
        return true;
    }

    private static Field[] getExportedPropertyFields(Class<?> klass) {
        if (sFieldsForClasses == null) {
            sFieldsForClasses = new HashMap();
        }
        if (sAnnotations == null) {
            sAnnotations = new HashMap(512);
        }
        HashMap<Class<?>, Field[]> map = getFieldsMap();
        Field[] fields = (Field[]) map.get(klass);
        if (fields != null) {
            return fields;
        }
        try {
            Field[] declaredFields = getFields(klass);
            ArrayList<Field> foundFields = new ArrayList();
            for (Field field : declaredFields) {
                if (field.getType() != null && field.isAnnotationPresent(ExportedProperty.class)) {
                    field.setAccessible(true);
                    foundFields.add(field);
                    sAnnotations.put(field, (ExportedProperty) field.getAnnotation(ExportedProperty.class));
                }
            }
            fields = (Field[]) foundFields.toArray(new Field[foundFields.size()]);
            map.put(klass, fields);
            return fields;
        } catch (NoClassDefFoundError e) {
            throw new AssertionError(e);
        }
    }

    private static Method[] getExportedPropertyMethods(Class<?> klass) {
        if (sMethodsForClasses == null) {
            sMethodsForClasses = new HashMap(100);
        }
        if (sAnnotations == null) {
            sAnnotations = new HashMap(512);
        }
        HashMap<Class<?>, Method[]> map = getMethodMap();
        Method[] methods = (Method[]) map.get(klass);
        if (methods != null) {
            return methods;
        }
        int i = 0;
        methods = klass.getDeclaredMethodsUnchecked(false);
        ArrayList<Method> foundMethods = new ArrayList();
        methods = getMethod(klass);
        int length = methods.length;
        while (i < length) {
            Method method = methods[i];
            try {
                method.getReturnType();
                method.getParameterTypes();
                if (method.getParameterTypes().length == 0 && method.isAnnotationPresent(ExportedProperty.class) && method.getReturnType() != Void.class) {
                    method.setAccessible(true);
                    foundMethods.add(method);
                    sAnnotations.put(method, (ExportedProperty) method.getAnnotation(ExportedProperty.class));
                }
            } catch (NoClassDefFoundError e) {
            }
            i++;
        }
        methods = (Method[]) foundMethods.toArray(new Method[foundMethods.size()]);
        map.put(klass, methods);
        return methods;
    }

    private static void dumpViewProperties(Context context, Object view, BufferedWriter out) throws IOException {
        dumpViewProperties(context, view, out, "");
    }

    private static void dumpViewProperties(Context context, Object view, BufferedWriter out, String prefix) throws IOException {
        if (view == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("=4,null ");
            out.write(stringBuilder.toString());
            return;
        }
        Class<?> klass = view.getClass();
        do {
            exportFields(context, view, out, klass, prefix);
            exportMethods(context, view, out, klass, prefix);
            klass = klass.getSuperclass();
        } while (klass != Object.class);
    }

    private static Object callMethodOnAppropriateTheadBlocking(final Method method, Object object) throws IllegalAccessException, InvocationTargetException, TimeoutException {
        if (!(object instanceof View)) {
            return method.invoke(object, (Object[]) null);
        }
        final View view = (View) object;
        FutureTask<Object> future = new FutureTask(new Callable<Object>() {
            public Object call() throws IllegalAccessException, InvocationTargetException {
                return method.invoke(view, (Object[]) null);
            }
        });
        Handler handler = view.getHandler();
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.post(future);
        while (true) {
            try {
                return future.get(4000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t instanceof IllegalAccessException) {
                    throw ((IllegalAccessException) t);
                } else if (t instanceof InvocationTargetException) {
                    throw ((InvocationTargetException) t);
                } else {
                    throw new RuntimeException("Unexpected exception", t);
                }
            } catch (InterruptedException e2) {
            } catch (CancellationException e3) {
                throw new RuntimeException("Unexpected cancellation exception", e3);
            }
        }
    }

    private static String formatIntToHexString(int value) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(value).toUpperCase());
        return stringBuilder.toString();
    }

    private static void exportMethods(Context context, Object view, BufferedWriter out, Class<?> klass, String prefix) throws IOException {
        Context context2 = context;
        BufferedWriter bufferedWriter = out;
        String str = prefix;
        Method[] methods = getExportedPropertyMethods(klass);
        int count = methods.length;
        int i = 0;
        while (true) {
            int i2 = i;
            Method[] methods2;
            int count2;
            if (i2 < count) {
                Method method = methods[i2];
                try {
                    StringBuilder stringBuilder;
                    String stringBuilder2;
                    Object methodValue = callMethodOnAppropriateTheadBlocking(method, view);
                    Class<?> returnType = method.getReturnType();
                    ExportedProperty property = (ExportedProperty) sAnnotations.get(method);
                    if (property.category().length() != 0) {
                        try {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(property.category());
                            stringBuilder.append(SettingsStringUtil.DELIMITER);
                            stringBuilder2 = stringBuilder.toString();
                        } catch (IllegalAccessException e) {
                            methods2 = methods;
                            count2 = count;
                        } catch (InvocationTargetException e2) {
                            methods2 = methods;
                            count2 = count;
                        } catch (TimeoutException e3) {
                            methods2 = methods;
                            count2 = count;
                        }
                    } else {
                        stringBuilder2 = "";
                    }
                    String categoryPrefix = stringBuilder2;
                    ExportedProperty property2;
                    ExportedProperty property3;
                    if (returnType != Integer.TYPE) {
                        property2 = property;
                        if (returnType == int[].class) {
                            int[] array = (int[]) methodValue;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(categoryPrefix);
                            stringBuilder.append(str);
                            stringBuilder.append(method.getName());
                            stringBuilder.append('_');
                            String suffix = "()";
                            methods2 = methods;
                            methods = categoryPrefix;
                            count2 = count;
                            count = returnType;
                            try {
                                exportUnrolledArray(context2, bufferedWriter, property2, array, stringBuilder.toString(), "()");
                            } catch (IllegalAccessException | InvocationTargetException | TimeoutException e4) {
                            }
                        } else {
                            methods2 = methods;
                            count2 = count;
                            ExportedProperty property4 = property2;
                            methods = categoryPrefix;
                            count = returnType;
                            if (count == String[].class) {
                                String[] array2 = (String[]) methodValue;
                                if (property4.hasAdjacentMapping() && array2 != null) {
                                    for (int j = 0; j < array2.length; j += 2) {
                                        if (array2[j] != null) {
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append(methods);
                                            stringBuilder3.append(str);
                                            writeEntry(bufferedWriter, stringBuilder3.toString(), array2[j], "()", array2[j + 1] == null ? "null" : array2[j + 1]);
                                        }
                                    }
                                }
                            } else {
                                property3 = property4;
                                if (!count.isPrimitive() && property3.deepExport()) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(str);
                                    stringBuilder.append(property3.prefix());
                                    dumpViewProperties(context2, methodValue, bufferedWriter, stringBuilder.toString());
                                }
                            }
                        }
                        i = i2 + 1;
                        methods = methods2;
                        count = count2;
                    } else if (!property.resolveId() || context2 == null) {
                        FlagToString[] flagsMapping = property.flagMapping();
                        if (flagsMapping.length > 0) {
                            int intValue = ((Integer) methodValue).intValue();
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append(categoryPrefix);
                            stringBuilder4.append(str);
                            stringBuilder4.append(method.getName());
                            stringBuilder4.append('_');
                            exportUnrolledFlags(bufferedWriter, flagsMapping, intValue, stringBuilder4.toString());
                        }
                        IntToString[] mapping = property.mapping();
                        if (mapping.length > 0) {
                            int intValue2 = ((Integer) methodValue).intValue();
                            boolean mapped = false;
                            int mappingCount = mapping.length;
                            int j2 = 0;
                            while (true) {
                                property2 = property;
                                property = j2;
                                IntToString[] intToStringArr;
                                if (property >= mappingCount) {
                                    intToStringArr = mapping;
                                    break;
                                }
                                int mappingCount2 = mappingCount;
                                intToStringArr = mapping;
                                mappingCount = mapping[property];
                                if (mappingCount.from() == intValue2) {
                                    methodValue = mappingCount.to();
                                    mapped = true;
                                    break;
                                }
                                j2 = property + 1;
                                property = property2;
                                mappingCount = mappingCount2;
                                mapping = intToStringArr;
                            }
                            if (!mapped) {
                                methodValue = Integer.valueOf(intValue2);
                            }
                        } else {
                            property2 = property;
                        }
                        methods2 = methods;
                        count2 = count;
                        methods = categoryPrefix;
                        count = returnType;
                        property3 = property2;
                    } else {
                        methodValue = resolveId(context2, ((Integer) methodValue).intValue());
                        methods2 = methods;
                        count2 = count;
                        methods = categoryPrefix;
                        property3 = property;
                        count = returnType;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(methods);
                    stringBuilder.append(str);
                    writeEntry(bufferedWriter, stringBuilder.toString(), method.getName(), "()", methodValue);
                } catch (IllegalAccessException e5) {
                    methods2 = methods;
                    count2 = count;
                } catch (InvocationTargetException e6) {
                    methods2 = methods;
                    count2 = count;
                } catch (TimeoutException e7) {
                    methods2 = methods;
                    count2 = count;
                }
                i = i2 + 1;
                methods = methods2;
                count = count2;
            } else {
                Object obj = view;
                methods2 = methods;
                count2 = count;
                return;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:77:0x01c8 A:{Catch:{ IllegalAccessException -> 0x01e6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x01c8 A:{Catch:{ IllegalAccessException -> 0x01e6 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void exportFields(Context context, Object view, BufferedWriter out, Class<?> klass, String prefix) throws IOException {
        Context context2 = context;
        Object obj = view;
        BufferedWriter bufferedWriter = out;
        String str = prefix;
        Field[] fields = getExportedPropertyFields(klass);
        int count = fields.length;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < count) {
                Field field = fields[i2];
                Field[] fields2;
                try {
                    StringBuilder stringBuilder;
                    String stringBuilder2;
                    Object fieldValue;
                    String categoryPrefix;
                    ExportedProperty property;
                    int j;
                    StringBuilder stringBuilder3;
                    Object fieldValue2;
                    StringBuilder stringBuilder4;
                    Class<?> type = field.getType();
                    ExportedProperty property2 = (ExportedProperty) sAnnotations.get(field);
                    if (property2.category().length() != 0) {
                        try {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(property2.category());
                            stringBuilder.append(SettingsStringUtil.DELIMITER);
                            stringBuilder2 = stringBuilder.toString();
                        } catch (IllegalAccessException e) {
                            fields2 = fields;
                        }
                    } else {
                        stringBuilder2 = "";
                    }
                    String categoryPrefix2 = stringBuilder2;
                    if (type == Integer.TYPE) {
                        fieldValue = null;
                        categoryPrefix = categoryPrefix2;
                        property = property2;
                        fields2 = fields;
                        fields = type;
                    } else if (type == Byte.TYPE) {
                        fieldValue = null;
                        categoryPrefix = categoryPrefix2;
                        property = property2;
                        fields2 = fields;
                        fields = type;
                    } else {
                        if (type == int[].class) {
                            int[] array = (int[]) field.get(obj);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(categoryPrefix2);
                            stringBuilder.append(str);
                            stringBuilder.append(field.getName());
                            stringBuilder.append('_');
                            String suffix = "";
                            fieldValue = null;
                            categoryPrefix = categoryPrefix2;
                            fields2 = fields;
                            fields = type;
                            try {
                                exportUnrolledArray(context2, bufferedWriter, property2, array, stringBuilder.toString(), "");
                            } catch (IllegalAccessException e2) {
                            }
                        } else {
                            fieldValue = null;
                            categoryPrefix = categoryPrefix2;
                            ExportedProperty property3 = property2;
                            fields2 = fields;
                            fields = type;
                            if (fields == String[].class) {
                                String[] array2 = (String[]) field.get(obj);
                                if (property3.hasAdjacentMapping() && array2 != null) {
                                    for (j = 0; j < array2.length; j += 2) {
                                        if (array2[j] != null) {
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append(categoryPrefix);
                                            stringBuilder3.append(str);
                                            writeEntry(bufferedWriter, stringBuilder3.toString(), array2[j], "", array2[j + 1] == null ? "null" : array2[j + 1]);
                                        }
                                    }
                                }
                            } else {
                                property = property3;
                                if (fields.isPrimitive() || !property.deepExport()) {
                                    fieldValue2 = fieldValue;
                                    if (fieldValue2 == null) {
                                        fieldValue2 = field.get(obj);
                                    }
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append(categoryPrefix);
                                    stringBuilder4.append(str);
                                    writeEntry(bufferedWriter, stringBuilder4.toString(), field.getName(), "", fieldValue2);
                                } else {
                                    fieldValue2 = field.get(obj);
                                    property2 = new StringBuilder();
                                    property2.append(str);
                                    property2.append(property.prefix());
                                    dumpViewProperties(context2, fieldValue2, bufferedWriter, property2.toString());
                                }
                            }
                        }
                        i = i2 + 1;
                        fields = fields2;
                    }
                    if (!property.resolveId() || context2 == null) {
                        FlagToString[] flagsMapping = property.flagMapping();
                        if (flagsMapping.length > 0) {
                            j = field.getInt(obj);
                            String valuePrefix = new StringBuilder();
                            valuePrefix.append(categoryPrefix);
                            valuePrefix.append(str);
                            valuePrefix.append(field.getName());
                            valuePrefix.append('_');
                            exportUnrolledFlags(bufferedWriter, flagsMapping, j, valuePrefix.toString());
                        }
                        IntToString[] mapping = property.mapping();
                        if (mapping.length > 0) {
                            int intValue = field.getInt(obj);
                            int mappingCount = mapping.length;
                            int j2 = 0;
                            while (j2 < mappingCount) {
                                IntToString mapped = mapping[j2];
                                FlagToString[] flagsMapping2 = flagsMapping;
                                if (mapped.from() == intValue) {
                                    fieldValue2 = mapped.to();
                                    break;
                                } else {
                                    j2++;
                                    flagsMapping = flagsMapping2;
                                }
                            }
                            fieldValue2 = fieldValue;
                            if (fieldValue2 == null) {
                                fieldValue2 = Integer.valueOf(intValue);
                            }
                        } else {
                            fieldValue2 = fieldValue;
                        }
                        if (property.formatToHexString()) {
                            fieldValue2 = field.get(obj);
                            if (fields == Integer.TYPE) {
                                fieldValue2 = formatIntToHexString(((Integer) fieldValue2).intValue());
                            } else if (fields == Byte.TYPE) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("0x");
                                stringBuilder3.append(Byte.toHexString(((Byte) fieldValue2).byteValue(), true));
                                fieldValue2 = stringBuilder3.toString();
                            }
                        }
                        if (fieldValue2 == null) {
                        }
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(categoryPrefix);
                        stringBuilder4.append(str);
                        writeEntry(bufferedWriter, stringBuilder4.toString(), field.getName(), "", fieldValue2);
                        i = i2 + 1;
                        fields = fields2;
                    } else {
                        fieldValue2 = resolveId(context2, field.getInt(obj));
                        if (fieldValue2 == null) {
                        }
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(categoryPrefix);
                        stringBuilder4.append(str);
                        writeEntry(bufferedWriter, stringBuilder4.toString(), field.getName(), "", fieldValue2);
                        i = i2 + 1;
                        fields = fields2;
                    }
                } catch (IllegalAccessException e3) {
                    fields2 = fields;
                }
            } else {
                return;
            }
        }
    }

    private static void writeEntry(BufferedWriter out, String prefix, String name, String suffix, Object value) throws IOException {
        out.write(prefix);
        out.write(name);
        out.write(suffix);
        out.write("=");
        writeValue(out, value);
        out.write(32);
    }

    private static void exportUnrolledFlags(BufferedWriter out, FlagToString[] mapping, int intValue, String prefix) throws IOException {
        for (FlagToString flagMapping : mapping) {
            boolean ifTrue = flagMapping.outputIf();
            int maskResult = flagMapping.mask() & intValue;
            boolean test = maskResult == flagMapping.equals();
            if ((test && ifTrue) || !(test || ifTrue)) {
                writeEntry(out, prefix, flagMapping.name(), "", formatIntToHexString(maskResult));
            }
        }
    }

    public static String intToString(Class<?> clazz, String field, int integer) {
        IntToString[] mapping = getMapping(clazz, field);
        if (mapping == null) {
            return Integer.toString(integer);
        }
        for (IntToString map : mapping) {
            if (map.from() == integer) {
                return map.to();
            }
        }
        return Integer.toString(integer);
    }

    public static String flagsToString(Class<?> clazz, String field, int flags) {
        FlagToString[] mapping = getFlagMapping(clazz, field);
        if (mapping == null) {
            return Integer.toHexString(flags);
        }
        StringBuilder result = new StringBuilder();
        int count = mapping.length;
        int j = 0;
        while (true) {
            boolean test = true;
            if (j >= count) {
                break;
            }
            FlagToString flagMapping = mapping[j];
            boolean ifTrue = flagMapping.outputIf();
            if ((flagMapping.mask() & flags) != flagMapping.equals()) {
                test = false;
            }
            if (test && ifTrue) {
                result.append(flagMapping.name());
                result.append(' ');
            }
            j++;
        }
        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    private static FlagToString[] getFlagMapping(Class<?> clazz, String field) {
        try {
            return ((ExportedProperty) clazz.getDeclaredField(field).getAnnotation(ExportedProperty.class)).flagMapping();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static IntToString[] getMapping(Class<?> clazz, String field) {
        try {
            return ((ExportedProperty) clazz.getDeclaredField(field).getAnnotation(ExportedProperty.class)).mapping();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static void exportUnrolledArray(Context context, BufferedWriter out, ExportedProperty property, int[] array, String prefix, String suffix) throws IOException {
        Context context2 = context;
        int[] iArr = array;
        IntToString[] indexMapping = property.indexMapping();
        boolean resolveId = true;
        boolean hasIndexMapping = indexMapping.length > 0;
        IntToString[] mapping = property.mapping();
        boolean hasMapping = mapping.length > 0;
        if (!property.resolveId() || context2 == null) {
            resolveId = false;
        }
        int valuesCount = iArr.length;
        for (int j = 0; j < valuesCount; j++) {
            String value = null;
            int intValue = iArr[j];
            String name = String.valueOf(j);
            if (hasIndexMapping) {
                for (IntToString mapped : indexMapping) {
                    if (mapped.from() == j) {
                        name = mapped.to();
                        break;
                    }
                }
            }
            if (hasMapping) {
                for (IntToString mapped2 : mapping) {
                    if (mapped2.from() == intValue) {
                        value = mapped2.to();
                        break;
                    }
                }
            }
            if (!resolveId) {
                value = String.valueOf(intValue);
            } else if (value == null) {
                value = (String) resolveId(context2, intValue);
            }
            writeEntry(out, prefix, name, suffix, value);
        }
        BufferedWriter bufferedWriter = out;
        String str = prefix;
        String str2 = suffix;
    }

    static Object resolveId(Context context, int id) {
        Resources resources = context.getResources();
        if (id < 0) {
            return "NO_ID";
        }
        try {
            Object fieldValue = new StringBuilder();
            fieldValue.append(resources.getResourceTypeName(id));
            fieldValue.append('/');
            fieldValue.append(resources.getResourceEntryName(id));
            return fieldValue.toString();
        } catch (NotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("id/");
            stringBuilder.append(formatIntToHexString(id));
            return stringBuilder.toString();
        }
    }

    private static void writeValue(BufferedWriter out, Object value) throws IOException {
        if (value != null) {
            String output = "[EXCEPTION]";
            String replace;
            try {
                replace = value.toString().replace("\n", "\\n");
                output = replace;
            } finally {
                String str = replace;
                out.write(String.valueOf(output.length()));
                out.write(",");
                out.write(output);
                return;
            }
            return;
        }
        out.write("4,null");
    }

    private static Field[] capturedViewGetPropertyFields(Class<?> klass) {
        if (mCapturedViewFieldsForClasses == null) {
            mCapturedViewFieldsForClasses = new HashMap();
        }
        HashMap<Class<?>, Field[]> map = mCapturedViewFieldsForClasses;
        Field[] fields = (Field[]) map.get(klass);
        if (fields != null) {
            return fields;
        }
        ArrayList<Field> foundFields = new ArrayList();
        for (Field field : klass.getFields()) {
            if (field.isAnnotationPresent(CapturedViewProperty.class)) {
                field.setAccessible(true);
                foundFields.add(field);
            }
        }
        fields = (Field[]) foundFields.toArray(new Field[foundFields.size()]);
        map.put(klass, fields);
        return fields;
    }

    private static Method[] capturedViewGetPropertyMethods(Class<?> klass) {
        if (mCapturedViewMethodsForClasses == null) {
            mCapturedViewMethodsForClasses = new HashMap();
        }
        HashMap<Class<?>, Method[]> map = mCapturedViewMethodsForClasses;
        Method[] methods = (Method[]) map.get(klass);
        if (methods != null) {
            return methods;
        }
        ArrayList<Method> foundMethods = new ArrayList();
        for (Method method : klass.getMethods()) {
            if (method.getParameterTypes().length == 0 && method.isAnnotationPresent(CapturedViewProperty.class) && method.getReturnType() != Void.class) {
                method.setAccessible(true);
                foundMethods.add(method);
            }
        }
        methods = (Method[]) foundMethods.toArray(new Method[foundMethods.size()]);
        map.put(klass, methods);
        return methods;
    }

    private static String capturedViewExportMethods(Object obj, Class<?> klass, String prefix) {
        if (obj == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (Method method : capturedViewGetPropertyMethods(klass)) {
            try {
                Object methodValue = method.invoke(obj, (Object[]) null);
                Class<?> returnType = method.getReturnType();
                if (((CapturedViewProperty) method.getAnnotation(CapturedViewProperty.class)).retrieveReturn()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(method.getName());
                    stringBuilder.append("#");
                    sb.append(capturedViewExportMethods(methodValue, returnType, stringBuilder.toString()));
                } else {
                    sb.append(prefix);
                    sb.append(method.getName());
                    sb.append("()=");
                    if (methodValue != null) {
                        sb.append(methodValue.toString().replace("\n", "\\n"));
                    } else {
                        sb.append("null");
                    }
                    sb.append("; ");
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
            }
        }
        return sb.toString();
    }

    private static String capturedViewExportFields(Object obj, Class<?> klass, String prefix) {
        if (obj == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (Field field : capturedViewGetPropertyFields(klass)) {
            try {
                Object fieldValue = field.get(obj);
                sb.append(prefix);
                sb.append(field.getName());
                sb.append("=");
                if (fieldValue != null) {
                    sb.append(fieldValue.toString().replace("\n", "\\n"));
                } else {
                    sb.append("null");
                }
                sb.append(' ');
            } catch (IllegalAccessException e) {
            }
        }
        return sb.toString();
    }

    public static void dumpCapturedView(String tag, Object view) {
        Class<?> klass = view.getClass();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(klass.getName());
        stringBuilder.append(": ");
        StringBuilder sb = new StringBuilder(stringBuilder.toString());
        sb.append(capturedViewExportFields(view, klass, ""));
        sb.append(capturedViewExportMethods(view, klass, ""));
        Log.d(tag, sb.toString());
    }

    public static Object invokeViewMethod(View view, Method method, Object[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference();
        AtomicReference<Throwable> exception = new AtomicReference();
        final AtomicReference<Object> atomicReference = result;
        final Method method2 = method;
        final View view2 = view;
        final Object[] objArr = args;
        final AtomicReference<Throwable> atomicReference2 = exception;
        final CountDownLatch countDownLatch = latch;
        view.post(new Runnable() {
            public void run() {
                try {
                    atomicReference.set(method2.invoke(view2, objArr));
                } catch (InvocationTargetException e) {
                    atomicReference2.set(e.getCause());
                } catch (Exception e2) {
                    atomicReference2.set(e2);
                }
                countDownLatch.countDown();
            }
        });
        try {
            latch.await();
            if (exception.get() == null) {
                return result.get();
            }
            throw new RuntimeException((Throwable) exception.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setLayoutParameter(final View view, String param, int value) throws NoSuchFieldException, IllegalAccessException {
        final LayoutParams p = view.getLayoutParams();
        Field f = p.getClass().getField(param);
        if (f.getType() == Integer.TYPE) {
            f.set(p, Integer.valueOf(value));
            view.post(new Runnable() {
                public void run() {
                    view.setLayoutParams(p);
                }
            });
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Only integer layout parameters can be set. Field ");
        stringBuilder.append(param);
        stringBuilder.append(" is of type ");
        stringBuilder.append(f.getType().getSimpleName());
        throw new RuntimeException(stringBuilder.toString());
    }

    private static void initCustomizedList(Context context) {
        if (!mInited && context != null) {
            mInited = true;
            try {
                String fields = Systemex.getString(context.getContentResolver(), DUMPC_CUST_FIELDSLIST);
                String methods = Systemex.getString(context.getContentResolver(), DUMPC_CUST_METHODSLIST);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fields:");
                stringBuilder.append(fields);
                stringBuilder.append(", methods:");
                stringBuilder.append(methods);
                stringBuilder.append(", REMOTE_COMMAND_DUMP_CUST:");
                stringBuilder.append(REMOTE_COMMAND_DUMP_CUST);
                Log.i("ViewServer", stringBuilder.toString());
                mFieldsList = fields != null ? fields.split(",") : mFieldsList;
                mMethodsList = methods != null ? methods.split(",") : mMethodsList;
            } catch (Exception e) {
                Log.e("ViewDebug", "Could not load fields or methods from database.", e);
            }
        }
    }

    private static void customizedDump(View root, OutputStream clientStream) throws IOException {
        mCustomizedDump = true;
        initCustomizedList(root.getContext());
        try {
            dump(root, false, true, clientStream);
            mCustomizedDump = false;
        } catch (IOException e) {
            throw e;
        } catch (Throwable th) {
            mCustomizedDump = false;
        }
    }

    private static Field[] filterFieldsProperties(Field[] allProperty, String[] customizedList) {
        ArrayList<Field> result = new ArrayList();
        for (Field item : allProperty) {
            if (arrayContains(customizedList, item.getName())) {
                result.add(item);
            }
        }
        return (Field[]) result.toArray(new Field[result.size()]);
    }

    private static Method[] filterMethodsProperties(Method[] allProperty, String[] customizedList) {
        ArrayList<Method> result = new ArrayList();
        for (Method item : allProperty) {
            if (arrayContains(customizedList, item.getName())) {
                result.add(item);
            }
        }
        return (Method[]) result.toArray(new Method[result.size()]);
    }

    private static boolean arrayContains(String[] array, String value) {
        for (String s : array) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static HashMap<Class<?>, Field[]> getFieldsMap() {
        return mCustomizedDump ? sCustFieldsForClasses : sFieldsForClasses;
    }

    private static HashMap<Class<?>, Method[]> getMethodMap() {
        return mCustomizedDump ? sCustMethodsForClasses : sMethodsForClasses;
    }

    private static Field[] getFields(Class<?> klass) {
        Field[] fields = klass.getDeclaredFieldsUnchecked(null);
        if (mCustomizedDump) {
            return filterFieldsProperties(fields, mFieldsList);
        }
        return fields;
    }

    private static Method[] getMethod(Class<?> klass) {
        Method[] methods = klass.getDeclaredMethods();
        if (mCustomizedDump) {
            return filterMethodsProperties(methods, mMethodsList);
        }
        return methods;
    }
}
