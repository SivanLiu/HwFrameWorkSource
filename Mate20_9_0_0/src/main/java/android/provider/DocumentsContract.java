package android.provider;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.provider.CalendarContract.CalendarCache;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.DataUnit;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;

public final class DocumentsContract {
    public static final String ACTION_DOCUMENT_ROOT_SETTINGS = "android.provider.action.DOCUMENT_ROOT_SETTINGS";
    public static final String ACTION_DOCUMENT_SETTINGS = "android.provider.action.DOCUMENT_SETTINGS";
    public static final String ACTION_MANAGE_DOCUMENT = "android.provider.action.MANAGE_DOCUMENT";
    public static final String EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_EXCLUDE_SELF = "android.provider.extra.EXCLUDE_SELF";
    public static final String EXTRA_INFO = "info";
    public static final String EXTRA_INITIAL_URI = "android.provider.extra.INITIAL_URI";
    public static final String EXTRA_LOADING = "loading";
    public static final String EXTRA_OPTIONS = "options";
    public static final String EXTRA_ORIENTATION = "android.provider.extra.ORIENTATION";
    public static final String EXTRA_PACKAGE_NAME = "android.content.extra.PACKAGE_NAME";
    public static final String EXTRA_PARENT_URI = "parentUri";
    public static final String EXTRA_PROMPT = "android.provider.extra.PROMPT";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_SHOW_ADVANCED = "android.content.extra.SHOW_ADVANCED";
    public static final String EXTRA_TARGET_URI = "android.content.extra.TARGET_URI";
    public static final String EXTRA_URI = "uri";
    public static final String METADATA_EXIF = "android:documentExif";
    public static final String METADATA_TYPES = "android:documentMetadataType";
    public static final String METHOD_COPY_DOCUMENT = "android:copyDocument";
    public static final String METHOD_CREATE_DOCUMENT = "android:createDocument";
    public static final String METHOD_CREATE_WEB_LINK_INTENT = "android:createWebLinkIntent";
    public static final String METHOD_DELETE_DOCUMENT = "android:deleteDocument";
    public static final String METHOD_EJECT_ROOT = "android:ejectRoot";
    public static final String METHOD_FIND_DOCUMENT_PATH = "android:findDocumentPath";
    public static final String METHOD_GET_DOCUMENT_METADATA = "android:getDocumentMetadata";
    public static final String METHOD_IS_CHILD_DOCUMENT = "android:isChildDocument";
    public static final String METHOD_MOVE_DOCUMENT = "android:moveDocument";
    public static final String METHOD_REMOVE_DOCUMENT = "android:removeDocument";
    public static final String METHOD_RENAME_DOCUMENT = "android:renameDocument";
    public static final String PACKAGE_DOCUMENTS_UI = "com.android.documentsui";
    private static final String PARAM_MANAGE = "manage";
    private static final String PARAM_QUERY = "query";
    private static final String PATH_CHILDREN = "children";
    private static final String PATH_DOCUMENT = "document";
    private static final String PATH_RECENT = "recent";
    private static final String PATH_ROOT = "root";
    private static final String PATH_SEARCH = "search";
    public static final String PATH_TREE = "tree";
    public static final String PROVIDER_INTERFACE = "android.content.action.DOCUMENTS_PROVIDER";
    private static final String TAG = "DocumentsContract";
    private static final int THUMBNAIL_BUFFER_SIZE = ((int) DataUnit.KIBIBYTES.toBytes(128));

    public static final class Document {
        public static final String COLUMN_DISPLAY_NAME = "_display_name";
        public static final String COLUMN_DOCUMENT_ID = "document_id";
        public static final String COLUMN_FLAGS = "flags";
        public static final String COLUMN_ICON = "icon";
        public static final String COLUMN_LAST_MODIFIED = "last_modified";
        public static final String COLUMN_MIME_TYPE = "mime_type";
        public static final String COLUMN_SIZE = "_size";
        public static final String COLUMN_SUMMARY = "summary";
        public static final int FLAG_DIR_PREFERS_GRID = 16;
        public static final int FLAG_DIR_PREFERS_LAST_MODIFIED = 32;
        public static final int FLAG_DIR_SUPPORTS_CREATE = 8;
        public static final int FLAG_PARTIAL = 65536;
        public static final int FLAG_SUPPORTS_COPY = 128;
        public static final int FLAG_SUPPORTS_DELETE = 4;
        public static final int FLAG_SUPPORTS_METADATA = 131072;
        public static final int FLAG_SUPPORTS_MOVE = 256;
        public static final int FLAG_SUPPORTS_REMOVE = 1024;
        public static final int FLAG_SUPPORTS_RENAME = 64;
        public static final int FLAG_SUPPORTS_SETTINGS = 2048;
        public static final int FLAG_SUPPORTS_THUMBNAIL = 1;
        public static final int FLAG_SUPPORTS_WRITE = 2;
        public static final int FLAG_VIRTUAL_DOCUMENT = 512;
        public static final int FLAG_WEB_LINKABLE = 4096;
        public static final String MIME_TYPE_DIR = "vnd.android.document/directory";

        private Document() {
        }
    }

    public static final class Root {
        public static final String COLUMN_AVAILABLE_BYTES = "available_bytes";
        public static final String COLUMN_CAPACITY_BYTES = "capacity_bytes";
        public static final String COLUMN_DOCUMENT_ID = "document_id";
        public static final String COLUMN_FLAGS = "flags";
        public static final String COLUMN_ICON = "icon";
        public static final String COLUMN_MIME_TYPES = "mime_types";
        public static final String COLUMN_ROOT_ID = "root_id";
        public static final String COLUMN_SUMMARY = "summary";
        public static final String COLUMN_TITLE = "title";
        public static final int FLAG_ADVANCED = 131072;
        public static final int FLAG_EMPTY = 65536;
        public static final int FLAG_HAS_SETTINGS = 262144;
        public static final int FLAG_LOCAL_ONLY = 2;
        public static final int FLAG_REMOVABLE_SD = 524288;
        public static final int FLAG_REMOVABLE_USB = 1048576;
        public static final int FLAG_SUPPORTS_CREATE = 1;
        public static final int FLAG_SUPPORTS_EJECT = 32;
        public static final int FLAG_SUPPORTS_IS_CHILD = 16;
        public static final int FLAG_SUPPORTS_RECENTS = 4;
        public static final int FLAG_SUPPORTS_SEARCH = 8;
        public static final String MIME_TYPE_ITEM = "vnd.android.document/root";

        private Root() {
        }
    }

    public static final class Path implements Parcelable {
        public static final Creator<Path> CREATOR = new Creator<Path>() {
            public Path createFromParcel(Parcel in) {
                return new Path(in.readString(), in.createStringArrayList());
            }

            public Path[] newArray(int size) {
                return new Path[size];
            }
        };
        private final List<String> mPath;
        private final String mRootId;

        public Path(String rootId, List<String> path) {
            Preconditions.checkCollectionNotEmpty(path, "path");
            Preconditions.checkCollectionElementsNotNull(path, "path");
            this.mRootId = rootId;
            this.mPath = path;
        }

        public String getRootId() {
            return this.mRootId;
        }

        public List<String> getPath() {
            return this.mPath;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || !(o instanceof Path)) {
                return false;
            }
            Path path = (Path) o;
            if (!(Objects.equals(this.mRootId, path.mRootId) && Objects.equals(this.mPath, path.mPath))) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.mRootId, this.mPath});
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DocumentsContract.Path{");
            stringBuilder.append("rootId=");
            stringBuilder.append(this.mRootId);
            stringBuilder.append(", path=");
            stringBuilder.append(this.mPath);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mRootId);
            dest.writeStringList(this.mPath);
        }

        public int describeContents() {
            return 0;
        }
    }

    private DocumentsContract() {
    }

    public static Uri buildRootsUri(String authority) {
        return new Builder().scheme("content").authority(authority).appendPath(PATH_ROOT).build();
    }

    public static Uri buildRootUri(String authority, String rootId) {
        return new Builder().scheme("content").authority(authority).appendPath(PATH_ROOT).appendPath(rootId).build();
    }

    public static Uri buildHomeUri() {
        return buildRootUri(EXTERNAL_STORAGE_PROVIDER_AUTHORITY, CalendarCache.TIMEZONE_TYPE_HOME);
    }

    public static Uri buildRecentDocumentsUri(String authority, String rootId) {
        return new Builder().scheme("content").authority(authority).appendPath(PATH_ROOT).appendPath(rootId).appendPath(PATH_RECENT).build();
    }

    public static Uri buildTreeDocumentUri(String authority, String documentId) {
        return new Builder().scheme("content").authority(authority).appendPath(PATH_TREE).appendPath(documentId).build();
    }

    public static Uri buildDocumentUri(String authority, String documentId) {
        return new Builder().scheme("content").authority(authority).appendPath(PATH_DOCUMENT).appendPath(documentId).build();
    }

    public static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId) {
        return new Builder().scheme("content").authority(treeUri.getAuthority()).appendPath(PATH_TREE).appendPath(getTreeDocumentId(treeUri)).appendPath(PATH_DOCUMENT).appendPath(documentId).build();
    }

    public static Uri buildDocumentUriMaybeUsingTree(Uri baseUri, String documentId) {
        if (isTreeUri(baseUri)) {
            return buildDocumentUriUsingTree(baseUri, documentId);
        }
        return buildDocumentUri(baseUri.getAuthority(), documentId);
    }

    public static Uri buildChildDocumentsUri(String authority, String parentDocumentId) {
        return new Builder().scheme("content").authority(authority).appendPath(PATH_DOCUMENT).appendPath(parentDocumentId).appendPath(PATH_CHILDREN).build();
    }

    public static Uri buildChildDocumentsUriUsingTree(Uri treeUri, String parentDocumentId) {
        return new Builder().scheme("content").authority(treeUri.getAuthority()).appendPath(PATH_TREE).appendPath(getTreeDocumentId(treeUri)).appendPath(PATH_DOCUMENT).appendPath(parentDocumentId).appendPath(PATH_CHILDREN).build();
    }

    public static Uri buildSearchDocumentsUri(String authority, String rootId, String query) {
        return new Builder().scheme("content").authority(authority).appendPath(PATH_ROOT).appendPath(rootId).appendPath("search").appendQueryParameter("query", query).build();
    }

    public static boolean isDocumentUri(Context context, Uri uri) {
        boolean z = false;
        if (isContentUri(uri) && isDocumentsProvider(context, uri.getAuthority())) {
            List<String> paths = uri.getPathSegments();
            if (paths.size() == 2) {
                return PATH_DOCUMENT.equals(paths.get(0));
            }
            if (paths.size() == 4) {
                if (PATH_TREE.equals(paths.get(0)) && PATH_DOCUMENT.equals(paths.get(2))) {
                    z = true;
                }
                return z;
            }
        }
        return false;
    }

    public static boolean isRootUri(Context context, Uri uri) {
        boolean z = false;
        if (!isContentUri(uri) || !isDocumentsProvider(context, uri.getAuthority())) {
            return false;
        }
        List<String> paths = uri.getPathSegments();
        if (paths.size() == 2 && PATH_ROOT.equals(paths.get(0))) {
            z = true;
        }
        return z;
    }

    public static boolean isContentUri(Uri uri) {
        return uri != null && "content".equals(uri.getScheme());
    }

    public static boolean isTreeUri(Uri uri) {
        List<String> paths = uri.getPathSegments();
        return paths.size() >= 2 && PATH_TREE.equals(paths.get(0));
    }

    private static boolean isDocumentsProvider(Context context, String authority) {
        for (ResolveInfo info : context.getPackageManager().queryIntentContentProviders(new Intent(PROVIDER_INTERFACE), 0)) {
            if (authority.equals(info.providerInfo.authority)) {
                return true;
            }
        }
        return false;
    }

    public static String getRootId(Uri rootUri) {
        List<String> paths = rootUri.getPathSegments();
        if (paths.size() >= 2 && PATH_ROOT.equals(paths.get(0))) {
            return (String) paths.get(1);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid URI: ");
        stringBuilder.append(rootUri);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static String getDocumentId(Uri documentUri) {
        List<String> paths = documentUri.getPathSegments();
        if (paths.size() >= 2 && PATH_DOCUMENT.equals(paths.get(0))) {
            return (String) paths.get(1);
        }
        if (paths.size() >= 4 && PATH_TREE.equals(paths.get(0)) && PATH_DOCUMENT.equals(paths.get(2))) {
            return (String) paths.get(3);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid URI: ");
        stringBuilder.append(documentUri);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static String getTreeDocumentId(Uri documentUri) {
        List<String> paths = documentUri.getPathSegments();
        if (paths.size() >= 2 && PATH_TREE.equals(paths.get(0))) {
            return (String) paths.get(1);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid URI: ");
        stringBuilder.append(documentUri);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static String getSearchDocumentsQuery(Uri searchDocumentsUri) {
        return searchDocumentsUri.getQueryParameter("query");
    }

    public static Uri setManageMode(Uri uri) {
        return uri.buildUpon().appendQueryParameter(PARAM_MANAGE, "true").build();
    }

    public static boolean isManageMode(Uri uri) {
        return uri.getBooleanQueryParameter(PARAM_MANAGE, false);
    }

    public static Bitmap getDocumentThumbnail(ContentResolver resolver, Uri documentUri, Point size, CancellationSignal signal) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(documentUri.getAuthority());
        try {
            Bitmap documentThumbnail = getDocumentThumbnail(client, documentUri, size, signal);
            ContentProviderClient.releaseQuietly(client);
            return documentThumbnail;
        } catch (Exception e) {
            if (!(e instanceof OperationCanceledException)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to load thumbnail for ");
                stringBuilder.append(documentUri);
                stringBuilder.append(": ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
            }
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static Bitmap getDocumentThumbnail(ContentProviderClient client, Uri documentUri, Point size, CancellationSignal signal) throws RemoteException, IOException {
        Throwable th;
        Bundle bundle;
        Point point = size;
        Bundle openOpts = new Bundle();
        openOpts.putParcelable("android.content.extra.SIZE", point);
        AssetFileDescriptor afd = null;
        Bitmap bitmap = null;
        try {
            FileDescriptor fd;
            BufferedInputStream is;
            try {
                afd = client.openTypedAssetFileDescriptor(documentUri, "image/*", openOpts, signal);
                if (afd == null) {
                    IoUtils.closeQuietly(afd);
                    return null;
                }
                Bitmap bitmap2;
                fd = afd.getFileDescriptor();
                long offset = afd.getStartOffset();
                is = null;
                Os.lseek(fd, offset, OsConstants.SEEK_SET);
                Options opts = new Options();
                opts.inJustDecodeBounds = true;
                if (is != null) {
                    try {
                        BitmapFactory.decodeStream(is, null, opts);
                    } catch (Throwable th2) {
                        th = th2;
                        bundle = openOpts;
                    }
                } else {
                    BitmapFactory.decodeFileDescriptor(fd, null, opts);
                }
                int widthSample = opts.outWidth / point.x;
                int heightSample = opts.outHeight / point.y;
                opts.inThumbnailMode = true;
                opts.inJustDecodeBounds = false;
                opts.inSampleSize = Math.min(widthSample, heightSample);
                if (is != null) {
                    is.reset();
                    bitmap2 = BitmapFactory.decodeStream(is, null, opts);
                } else {
                    try {
                        Os.lseek(fd, offset, OsConstants.SEEK_SET);
                    } catch (ErrnoException e) {
                        e.rethrowAsIOException();
                    }
                    bitmap2 = BitmapFactory.decodeFileDescriptor(fd, null, opts);
                }
                bitmap = bitmap2;
                Bundle extras = afd.getExtras();
                int orientation = extras != null ? extras.getInt(EXTRA_ORIENTATION, 0) : 0;
                if (orientation != 0) {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    Matrix m = new Matrix();
                    try {
                        m.setRotate((float) orientation, (float) (width / 2), (float) (height / 2));
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, false);
                    } catch (Throwable th3) {
                        th = th3;
                        IoUtils.closeQuietly(afd);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(afd);
                return bitmap;
            } catch (ErrnoException e2) {
                is = new BufferedInputStream(new FileInputStream(fd), THUMBNAIL_BUFFER_SIZE);
                is.mark(THUMBNAIL_BUFFER_SIZE);
            } catch (Throwable th4) {
                th = th4;
                bundle = openOpts;
                IoUtils.closeQuietly(afd);
                throw th;
            }
        } catch (Throwable th5) {
            th = th5;
            ContentProviderClient contentProviderClient = client;
            Uri uri = documentUri;
            CancellationSignal cancellationSignal = signal;
            bundle = openOpts;
            IoUtils.closeQuietly(afd);
            throw th;
        }
    }

    public static Uri createDocument(ContentResolver resolver, Uri parentDocumentUri, String mimeType, String displayName) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(parentDocumentUri.getAuthority());
        try {
            Uri createDocument = createDocument(client, parentDocumentUri, mimeType, displayName);
            ContentProviderClient.releaseQuietly(client);
            return createDocument;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create document", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static Uri createDocument(ContentProviderClient client, Uri parentDocumentUri, String mimeType, String displayName) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", parentDocumentUri);
        in.putString("mime_type", mimeType);
        in.putString("_display_name", displayName);
        return (Uri) client.call(METHOD_CREATE_DOCUMENT, null, in).getParcelable("uri");
    }

    public static boolean isChildDocument(ContentProviderClient client, Uri parentDocumentUri, Uri childDocumentUri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", parentDocumentUri);
        in.putParcelable(EXTRA_TARGET_URI, childDocumentUri);
        Bundle out = client.call(METHOD_IS_CHILD_DOCUMENT, null, in);
        if (out == null) {
            throw new RemoteException("Failed to get a reponse from isChildDocument query.");
        } else if (out.containsKey(EXTRA_RESULT)) {
            return out.getBoolean(EXTRA_RESULT);
        } else {
            throw new RemoteException("Response did not include result field..");
        }
    }

    public static Uri renameDocument(ContentResolver resolver, Uri documentUri, String displayName) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(documentUri.getAuthority());
        try {
            Uri renameDocument = renameDocument(client, documentUri, displayName);
            ContentProviderClient.releaseQuietly(client);
            return renameDocument;
        } catch (Exception e) {
            Log.w(TAG, "Failed to rename document", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static Uri renameDocument(ContentProviderClient client, Uri documentUri, String displayName) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", documentUri);
        in.putString("_display_name", displayName);
        Uri outUri = (Uri) client.call(METHOD_RENAME_DOCUMENT, null, in).getParcelable("uri");
        return outUri != null ? outUri : documentUri;
    }

    public static boolean deleteDocument(ContentResolver resolver, Uri documentUri) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(documentUri.getAuthority());
        try {
            deleteDocument(client, documentUri);
            ContentProviderClient.releaseQuietly(client);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete document", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return false;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static void deleteDocument(ContentProviderClient client, Uri documentUri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", documentUri);
        client.call(METHOD_DELETE_DOCUMENT, null, in);
    }

    public static Uri copyDocument(ContentResolver resolver, Uri sourceDocumentUri, Uri targetParentDocumentUri) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(sourceDocumentUri.getAuthority());
        try {
            Uri copyDocument = copyDocument(client, sourceDocumentUri, targetParentDocumentUri);
            ContentProviderClient.releaseQuietly(client);
            return copyDocument;
        } catch (Exception e) {
            Log.w(TAG, "Failed to copy document", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static Uri copyDocument(ContentProviderClient client, Uri sourceDocumentUri, Uri targetParentDocumentUri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", sourceDocumentUri);
        in.putParcelable(EXTRA_TARGET_URI, targetParentDocumentUri);
        return (Uri) client.call(METHOD_COPY_DOCUMENT, null, in).getParcelable("uri");
    }

    public static Uri moveDocument(ContentResolver resolver, Uri sourceDocumentUri, Uri sourceParentDocumentUri, Uri targetParentDocumentUri) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(sourceDocumentUri.getAuthority());
        try {
            Uri moveDocument = moveDocument(client, sourceDocumentUri, sourceParentDocumentUri, targetParentDocumentUri);
            ContentProviderClient.releaseQuietly(client);
            return moveDocument;
        } catch (Exception e) {
            Log.w(TAG, "Failed to move document", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static Uri moveDocument(ContentProviderClient client, Uri sourceDocumentUri, Uri sourceParentDocumentUri, Uri targetParentDocumentUri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", sourceDocumentUri);
        in.putParcelable(EXTRA_PARENT_URI, sourceParentDocumentUri);
        in.putParcelable(EXTRA_TARGET_URI, targetParentDocumentUri);
        return (Uri) client.call(METHOD_MOVE_DOCUMENT, null, in).getParcelable("uri");
    }

    public static boolean removeDocument(ContentResolver resolver, Uri documentUri, Uri parentDocumentUri) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(documentUri.getAuthority());
        try {
            removeDocument(client, documentUri, parentDocumentUri);
            ContentProviderClient.releaseQuietly(client);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to remove document", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return false;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static void removeDocument(ContentProviderClient client, Uri documentUri, Uri parentDocumentUri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", documentUri);
        in.putParcelable(EXTRA_PARENT_URI, parentDocumentUri);
        client.call(METHOD_REMOVE_DOCUMENT, null, in);
    }

    public static void ejectRoot(ContentResolver resolver, Uri rootUri) {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(rootUri.getAuthority());
        try {
            ejectRoot(client, rootUri);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
        }
        ContentProviderClient.releaseQuietly(client);
    }

    public static void ejectRoot(ContentProviderClient client, Uri rootUri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", rootUri);
        client.call(METHOD_EJECT_ROOT, null, in);
    }

    public static Bundle getDocumentMetadata(ContentResolver resolver, Uri documentUri) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(documentUri.getAuthority());
        try {
            Bundle documentMetadata = getDocumentMetadata(client, documentUri);
            ContentProviderClient.releaseQuietly(client);
            return documentMetadata;
        } catch (Exception e) {
            Log.w(TAG, "Failed to get document metadata");
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static Bundle getDocumentMetadata(ContentProviderClient client, Uri documentUri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", documentUri);
        Bundle out = client.call(METHOD_GET_DOCUMENT_METADATA, null, in);
        if (out != null) {
            return out;
        }
        throw new RemoteException("Failed to get a response from getDocumentMetadata");
    }

    public static Path findDocumentPath(ContentResolver resolver, Uri treeUri) throws FileNotFoundException {
        boolean isTreeUri = isTreeUri(treeUri);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(treeUri);
        stringBuilder.append(" is not a tree uri.");
        Preconditions.checkArgument(isTreeUri, stringBuilder.toString());
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(treeUri.getAuthority());
        try {
            Path findDocumentPath = findDocumentPath(client, treeUri);
            ContentProviderClient.releaseQuietly(client);
            return findDocumentPath;
        } catch (Exception e) {
            Log.w(TAG, "Failed to find path", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static Path findDocumentPath(ContentProviderClient client, Uri uri) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", uri);
        return (Path) client.call(METHOD_FIND_DOCUMENT_PATH, null, in).getParcelable(EXTRA_RESULT);
    }

    public static IntentSender createWebLinkIntent(ContentResolver resolver, Uri uri, Bundle options) throws FileNotFoundException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(uri.getAuthority());
        try {
            IntentSender createWebLinkIntent = createWebLinkIntent(client, uri, options);
            ContentProviderClient.releaseQuietly(client);
            return createWebLinkIntent;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create a web link intent", e);
            rethrowIfNecessary(resolver, e);
            ContentProviderClient.releaseQuietly(client);
            return null;
        } catch (Throwable th) {
            ContentProviderClient.releaseQuietly(client);
            throw th;
        }
    }

    public static IntentSender createWebLinkIntent(ContentProviderClient client, Uri uri, Bundle options) throws RemoteException {
        Bundle in = new Bundle();
        in.putParcelable("uri", uri);
        if (options != null) {
            in.putBundle(EXTRA_OPTIONS, options);
        }
        return (IntentSender) client.call(METHOD_CREATE_WEB_LINK_INTENT, null, in).getParcelable(EXTRA_RESULT);
    }

    public static AssetFileDescriptor openImageThumbnail(File file) throws FileNotFoundException {
        Bundle extras;
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, 268435456);
        Bundle extras2 = null;
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int attributeInt = exif.getAttributeInt("Orientation", -1);
            if (attributeInt == 3) {
                extras2 = new Bundle(1);
                extras2.putInt(EXTRA_ORIENTATION, 180);
            } else if (attributeInt == 6) {
                extras2 = new Bundle(1);
                extras2.putInt(EXTRA_ORIENTATION, 90);
            } else if (attributeInt == 8) {
                extras2 = new Bundle(1);
                extras2.putInt(EXTRA_ORIENTATION, 270);
            }
            extras = extras2;
            try {
                long[] thumb = exif.getThumbnailRange();
                if (thumb != null) {
                    return new AssetFileDescriptor(pfd, thumb[0], thumb[1], extras);
                }
            } catch (IOException e) {
                extras2 = extras;
                extras = extras2;
                return new AssetFileDescriptor(pfd, 0, -1, extras);
            }
        } catch (IOException e2) {
            extras = extras2;
            return new AssetFileDescriptor(pfd, 0, -1, extras);
        }
        return new AssetFileDescriptor(pfd, 0, -1, extras);
    }

    private static void rethrowIfNecessary(ContentResolver resolver, Exception e) throws FileNotFoundException {
        if (resolver.getTargetSdkVersion() < 26) {
            return;
        }
        if (e instanceof ParcelableException) {
            ((ParcelableException) e).maybeRethrow(FileNotFoundException.class);
        } else if (e instanceof RemoteException) {
            ((RemoteException) e).rethrowAsRuntimeException();
        } else if (e instanceof RuntimeException) {
            throw ((RuntimeException) e);
        }
    }
}
