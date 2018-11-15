package android.support.v4.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParserException;

public class FileProvider extends ContentProvider {
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PATH = "path";
    private static final String[] COLUMNS = new String[]{"_display_name", "_size"};
    private static final File DEVICE_ROOT = new File("/");
    private static final String META_DATA_FILE_PROVIDER_PATHS = "android.support.FILE_PROVIDER_PATHS";
    private static final String TAG_CACHE_PATH = "cache-path";
    private static final String TAG_EXTERNAL = "external-path";
    private static final String TAG_EXTERNAL_CACHE = "external-cache-path";
    private static final String TAG_EXTERNAL_FILES = "external-files-path";
    private static final String TAG_EXTERNAL_MEDIA = "external-media-path";
    private static final String TAG_FILES_PATH = "files-path";
    private static final String TAG_ROOT_PATH = "root-path";
    @GuardedBy("sCache")
    private static HashMap<String, PathStrategy> sCache = new HashMap();
    private PathStrategy mStrategy;

    interface PathStrategy {
        File getFileForUri(Uri uri);

        Uri getUriForFile(File file);
    }

    static class SimplePathStrategy implements PathStrategy {
        private final String mAuthority;
        private final HashMap<String, File> mRoots = new HashMap();

        SimplePathStrategy(String authority) {
            this.mAuthority = authority;
        }

        void addRoot(String name, File root) {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name must not be empty");
            }
            try {
                this.mRoots.put(name, root.getCanonicalFile());
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to resolve canonical path for ");
                stringBuilder.append(root);
                throw new IllegalArgumentException(stringBuilder.toString(), e);
            }
        }

        public Uri getUriForFile(File file) {
            try {
                String path = file.getCanonicalPath();
                Entry<String, File> mostSpecific = null;
                for (Entry<String, File> root : this.mRoots.entrySet()) {
                    String rootPath = ((File) root.getValue()).getPath();
                    if (path.startsWith(rootPath) && (mostSpecific == null || rootPath.length() > ((File) mostSpecific.getValue()).getPath().length())) {
                        mostSpecific = root;
                    }
                }
                StringBuilder stringBuilder;
                if (mostSpecific != null) {
                    String rootPath2 = ((File) mostSpecific.getValue()).getPath();
                    if (rootPath2.endsWith("/")) {
                        path = path.substring(rootPath2.length());
                    } else {
                        path = path.substring(rootPath2.length() + 1);
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(Uri.encode((String) mostSpecific.getKey()));
                    stringBuilder.append('/');
                    stringBuilder.append(Uri.encode(path, "/"));
                    return new Builder().scheme("content").authority(this.mAuthority).encodedPath(stringBuilder.toString()).build();
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to find configured root that contains ");
                stringBuilder.append(path);
                throw new IllegalArgumentException(stringBuilder.toString());
            } catch (IOException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to resolve canonical path for ");
                stringBuilder2.append(file);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }

        public File getFileForUri(Uri uri) {
            String path = uri.getEncodedPath();
            int splitIndex = path.indexOf(47, 1);
            String tag = Uri.decode(path.substring(1, splitIndex));
            path = Uri.decode(path.substring(splitIndex + 1));
            File root = (File) this.mRoots.get(tag);
            if (root != null) {
                File file = new File(root, path);
                try {
                    file = file.getCanonicalFile();
                    if (file.getPath().startsWith(root.getPath())) {
                        return file;
                    }
                    throw new SecurityException("Resolved path jumped beyond configured root");
                } catch (IOException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to resolve canonical path for ");
                    stringBuilder.append(file);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to find configured root for ");
            stringBuilder2.append(uri);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    public boolean onCreate() {
        return true;
    }

    public void attachInfo(@NonNull Context context, @NonNull ProviderInfo info) {
        super.attachInfo(context, info);
        if (info.exported) {
            throw new SecurityException("Provider must not be exported");
        } else if (info.grantUriPermissions) {
            this.mStrategy = getPathStrategy(context, info.authority);
        } else {
            throw new SecurityException("Provider must grant uri permissions");
        }
    }

    public static Uri getUriForFile(@NonNull Context context, @NonNull String authority, @NonNull File file) {
        return getPathStrategy(context, authority).getUriForFile(file);
    }

    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        File file = this.mStrategy.getFileForUri(uri);
        if (projection == null) {
            projection = COLUMNS;
        }
        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int i = 0;
        for (String col : projection) {
            int i2;
            if ("_display_name".equals(col)) {
                cols[i] = "_display_name";
                i2 = i + 1;
                values[i] = file.getName();
            } else if ("_size".equals(col)) {
                cols[i] = "_size";
                i2 = i + 1;
                values[i] = Long.valueOf(file.length());
            } else {
            }
            i = i2;
        }
        cols = copyOf(cols, i);
        values = copyOf(values, i);
        MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    public String getType(@NonNull Uri uri) {
        File file = this.mStrategy.getFileForUri(uri);
        int lastDot = file.getName().lastIndexOf(46);
        if (lastDot >= 0) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(lastDot + 1));
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    public int update(@NonNull Uri uri, ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return this.mStrategy.getFileForUri(uri).delete();
    }

    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(this.mStrategy.getFileForUri(uri), modeToMode(mode));
    }

    private static PathStrategy getPathStrategy(Context context, String authority) {
        PathStrategy strat;
        synchronized (sCache) {
            strat = (PathStrategy) sCache.get(authority);
            if (strat == null) {
                try {
                    strat = parsePathStrategy(context, authority);
                    sCache.put(authority, strat);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to parse android.support.FILE_PROVIDER_PATHS meta-data", e);
                } catch (XmlPullParserException e2) {
                    throw new IllegalArgumentException("Failed to parse android.support.FILE_PROVIDER_PATHS meta-data", e2);
                }
            }
        }
        return strat;
    }

    private static PathStrategy parsePathStrategy(Context context, String authority) throws IOException, XmlPullParserException {
        SimplePathStrategy strat = new SimplePathStrategy(authority);
        XmlResourceParser in = context.getPackageManager().resolveContentProvider(authority, 128).loadXmlMetaData(context.getPackageManager(), META_DATA_FILE_PROVIDER_PATHS);
        if (in != null) {
            while (true) {
                int next = in.next();
                int type = next;
                if (next == 1) {
                    return strat;
                }
                if (type == 2) {
                    String tag = in.getName();
                    String name = in.getAttributeValue(null, ATTR_NAME);
                    String path = in.getAttributeValue(null, ATTR_PATH);
                    File target = null;
                    File[] externalFilesDirs;
                    if (TAG_ROOT_PATH.equals(tag)) {
                        target = DEVICE_ROOT;
                    } else if (TAG_FILES_PATH.equals(tag)) {
                        target = context.getFilesDir();
                    } else if (TAG_CACHE_PATH.equals(tag)) {
                        target = context.getCacheDir();
                    } else if (TAG_EXTERNAL.equals(tag)) {
                        target = Environment.getExternalStorageDirectory();
                    } else if (TAG_EXTERNAL_FILES.equals(tag)) {
                        externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
                        if (externalFilesDirs.length > 0) {
                            target = externalFilesDirs[0];
                        }
                    } else if (TAG_EXTERNAL_CACHE.equals(tag)) {
                        externalFilesDirs = ContextCompat.getExternalCacheDirs(context);
                        if (externalFilesDirs.length > 0) {
                            target = externalFilesDirs[0];
                        }
                    } else if (VERSION.SDK_INT >= 21 && TAG_EXTERNAL_MEDIA.equals(tag)) {
                        externalFilesDirs = context.getExternalMediaDirs();
                        if (externalFilesDirs.length > 0) {
                            target = externalFilesDirs[0];
                        }
                    }
                    if (target != null) {
                        strat.addRoot(name, buildPath(target, path));
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Missing android.support.FILE_PROVIDER_PATHS meta-data");
        }
    }

    private static int modeToMode(String mode) {
        if ("r".equals(mode)) {
            return 268435456;
        }
        if ("w".equals(mode) || "wt".equals(mode)) {
            return 738197504;
        }
        if ("wa".equals(mode)) {
            return 704643072;
        }
        if ("rw".equals(mode)) {
            return 939524096;
        }
        if ("rwt".equals(mode)) {
            return 1006632960;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode: ");
        stringBuilder.append(mode);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (segment != null) {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }

    private static String[] copyOf(String[] original, int newLength) {
        String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }
}
