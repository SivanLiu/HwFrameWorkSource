package com.android.internal.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore.Files;
import android.provider.MetadataReader;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Protocol;
import com.android.internal.widget.MessagingMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import libcore.io.IoUtils;

public abstract class FileSystemProvider extends DocumentsProvider {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final boolean LOG_INOTIFY = false;
    private static final String MIMETYPE_JPEG = "image/jpeg";
    private static final String MIMETYPE_JPG = "image/jpg";
    private static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";
    private static final String TAG = "FileSystemProvider";
    private String[] mDefaultProjection;
    private Handler mHandler;
    @GuardedBy("mObservers")
    private final ArrayMap<File, DirectoryObserver> mObservers = new ArrayMap();

    private static class DirectoryObserver extends FileObserver {
        private static final int NOTIFY_EVENTS = 4044;
        private final File mFile;
        private final Uri mNotifyUri;
        private int mRefCount = 0;
        private final ContentResolver mResolver;

        public DirectoryObserver(File file, ContentResolver resolver, Uri notifyUri) {
            super(file.getAbsolutePath(), NOTIFY_EVENTS);
            this.mFile = file;
            this.mResolver = resolver;
            this.mNotifyUri = notifyUri;
        }

        public void onEvent(int event, String path) {
            if ((event & NOTIFY_EVENTS) != 0) {
                this.mResolver.notifyChange(this.mNotifyUri, null, false);
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DirectoryObserver{file=");
            stringBuilder.append(this.mFile.getAbsolutePath());
            stringBuilder.append(", ref=");
            stringBuilder.append(this.mRefCount);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private class DirectoryCursor extends MatrixCursor {
        private final File mFile;

        public DirectoryCursor(String[] columnNames, String docId, File file) {
            super(columnNames);
            Uri notifyUri = FileSystemProvider.this.buildNotificationUri(docId);
            setNotificationUri(FileSystemProvider.this.getContext().getContentResolver(), notifyUri);
            this.mFile = file;
            FileSystemProvider.this.startObserving(this.mFile, notifyUri);
        }

        public void close() {
            super.close();
            FileSystemProvider.this.stopObserving(this.mFile);
        }
    }

    protected abstract Uri buildNotificationUri(String str);

    protected abstract String getDocIdForFile(File file) throws FileNotFoundException;

    protected abstract File getFileForDocId(String str, boolean z) throws FileNotFoundException;

    protected void onDocIdChanged(String docId) {
    }

    public boolean onCreate() {
        throw new UnsupportedOperationException("Subclass should override this and call onCreate(defaultDocumentProjection)");
    }

    protected void onCreate(String[] defaultProjection) {
        this.mHandler = new Handler();
        this.mDefaultProjection = defaultProjection;
    }

    public boolean isChildDocument(String parentDocId, String docId) {
        try {
            return FileUtils.contains(getFileForDocId(parentDocId).getCanonicalFile(), getFileForDocId(docId).getCanonicalFile());
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to determine if ");
            stringBuilder.append(docId);
            stringBuilder.append(" is child of ");
            stringBuilder.append(parentDocId);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public Bundle getDocumentMetadata(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        if (!file.exists()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't find the file for documentId: ");
            stringBuilder.append(documentId);
            throw new FileNotFoundException(stringBuilder.toString());
        } else if (!file.isFile()) {
            Log.w(TAG, "Can't stream non-regular file. Returning empty metadata.");
            return null;
        } else if (file.canRead()) {
            String mimeType = getTypeForFile(file);
            if (!MetadataReader.isSupportedMimeType(mimeType)) {
                return null;
            }
            InputStream stream = null;
            Bundle metadata;
            try {
                metadata = new Bundle();
                stream = new FileInputStream(file.getAbsolutePath());
                MetadataReader.getMetadata(metadata, stream, mimeType, null);
                return metadata;
            } catch (IOException e) {
                metadata = e;
                Log.e(TAG, "An error occurred retrieving the metadata", metadata);
                return null;
            } finally {
                IoUtils.closeQuietly(stream);
            }
        } else {
            Log.w(TAG, "Can't stream non-readable file. Returning empty metadata.");
            return null;
        }
    }

    protected final List<String> findDocumentPath(File parent, File doc) throws FileNotFoundException {
        StringBuilder stringBuilder;
        if (doc != null && !doc.exists()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(doc);
            stringBuilder.append(" is not found.");
            throw new FileNotFoundException(stringBuilder.toString());
        } else if (FileUtils.contains(parent, doc)) {
            LinkedList<String> path = new LinkedList();
            while (doc != null && FileUtils.contains(parent, doc)) {
                path.addFirst(getDocIdForFile(doc));
                doc = doc.getParentFile();
            }
            return path;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(doc);
            stringBuilder.append(" is not found under ");
            stringBuilder.append(parent);
            throw new FileNotFoundException(stringBuilder.toString());
        }
    }

    public String createDocument(String docId, String mimeType, String displayName) throws FileNotFoundException {
        displayName = FileUtils.buildValidFatFilename(displayName);
        File parent = getFileForDocId(docId);
        if (parent.isDirectory()) {
            String childId;
            File file = FileUtils.buildUniqueFile(parent, mimeType, displayName);
            StringBuilder stringBuilder;
            if (!"vnd.android.document/directory".equals(mimeType)) {
                try {
                    if (file.createNewFile()) {
                        childId = getDocIdForFile(file);
                        onDocIdChanged(childId);
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to touch ");
                        stringBuilder.append(file);
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                } catch (IOException e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to touch ");
                    stringBuilder2.append(file);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(e);
                    throw new IllegalStateException(stringBuilder2.toString());
                }
            } else if (file.mkdir()) {
                childId = getDocIdForFile(file);
                onDocIdChanged(childId);
                addFolderToMediaStore(getFileForDocId(childId, true));
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to mkdir ");
                stringBuilder.append(file);
                throw new IllegalStateException(stringBuilder.toString());
            }
            return childId;
        }
        throw new IllegalArgumentException("Parent document isn't a directory");
    }

    private void addFolderToMediaStore(File visibleFolder) {
        if (visibleFolder != null) {
            long token = Binder.clearCallingIdentity();
            try {
                ContentResolver resolver = getContext().getContentResolver();
                Uri uri = Files.getDirectoryUri("external");
                ContentValues values = new ContentValues();
                values.put("_data", visibleFolder.getAbsolutePath());
                resolver.insert(uri, values);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public String renameDocument(String docId, String displayName) throws FileNotFoundException {
        displayName = FileUtils.buildValidFatFilename(displayName);
        File before = getFileForDocId(docId);
        File after = FileUtils.buildUniqueFile(before.getParentFile(), displayName);
        if (before.renameTo(after)) {
            String afterDocId = getDocIdForFile(after);
            onDocIdChanged(docId);
            onDocIdChanged(afterDocId);
            File beforeVisibleFile = getFileForDocId(docId, true);
            File afterVisibleFile = getFileForDocId(afterDocId, true);
            moveInMediaStore(beforeVisibleFile, afterVisibleFile);
            if (TextUtils.equals(docId, afterDocId)) {
                return null;
            }
            scanFile(afterVisibleFile);
            return afterDocId;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to rename to ");
        stringBuilder.append(after);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        File before = getFileForDocId(sourceDocumentId);
        File after = new File(getFileForDocId(targetParentDocumentId), before.getName());
        File visibleFileBefore = getFileForDocId(sourceDocumentId, true);
        StringBuilder stringBuilder;
        if (after.exists()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Already exists ");
            stringBuilder.append(after);
            throw new IllegalStateException(stringBuilder.toString());
        } else if (before.renameTo(after)) {
            String docId = getDocIdForFile(after);
            onDocIdChanged(sourceDocumentId);
            onDocIdChanged(docId);
            moveInMediaStore(visibleFileBefore, getFileForDocId(docId, true));
            return docId;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to move to ");
            stringBuilder.append(after);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private void moveInMediaStore(File oldVisibleFile, File newVisibleFile) {
        if (oldVisibleFile != null && newVisibleFile != null) {
            long token = Binder.clearCallingIdentity();
            try {
                Uri externalUri;
                ContentResolver resolver = getContext().getContentResolver();
                if (newVisibleFile.isDirectory()) {
                    externalUri = Files.getDirectoryUri("external");
                } else {
                    externalUri = Files.getContentUri("external");
                }
                ContentValues values = new ContentValues();
                values.put("_data", newVisibleFile.getAbsolutePath());
                String path = oldVisibleFile.getAbsolutePath();
                resolver.update(externalUri, values, "_data LIKE ? AND lower(_data)=lower(?)", new String[]{path, path});
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public void deleteDocument(String docId) throws FileNotFoundException {
        File file = getFileForDocId(docId);
        File visibleFile = getFileForDocId(docId, true);
        boolean isDirectory = file.isDirectory();
        if (isDirectory) {
            FileUtils.deleteContents(file);
        }
        if (file.delete()) {
            onDocIdChanged(docId);
            removeFromMediaStore(visibleFile, isDirectory);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to delete ");
        stringBuilder.append(file);
        throw new IllegalStateException(stringBuilder.toString());
    }

    private void removeFromMediaStore(File visibleFile, boolean isFolder) throws FileNotFoundException {
        if (visibleFile != null) {
            long token = Binder.clearCallingIdentity();
            try {
                String path;
                ContentResolver resolver = getContext().getContentResolver();
                Uri externalUri = Files.getContentUri("external");
                if (isFolder) {
                    path = new StringBuilder();
                    path.append(visibleFile.getAbsolutePath());
                    path.append("/");
                    path = path.toString();
                    String[] strArr = new String[3];
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(path);
                    stringBuilder.append("%");
                    strArr[0] = stringBuilder.toString();
                    strArr[1] = Integer.toString(path.length());
                    strArr[2] = path;
                    resolver.delete(externalUri, "_data LIKE ?1 AND lower(substr(_data,1,?2))=lower(?3)", strArr);
                }
                path = visibleFile.getAbsolutePath();
                resolver.delete(externalUri, "_data LIKE ?1 AND lower(_data)=lower(?2)", new String[]{path, path});
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveProjection(projection));
        includeFile(result, documentId, null);
        return result;
    }

    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        File parent = getFileForDocId(parentDocumentId);
        MatrixCursor result = new DirectoryCursor(resolveProjection(projection), parentDocumentId, parent);
        File[] fsList = parent.listFiles();
        if (fsList != null) {
            for (File file : fsList) {
                includeFile(result, null, file);
            }
        }
        return result;
    }

    protected final Cursor querySearchDocuments(File folder, String query, String[] projection, Set<String> exclusion) throws FileNotFoundException {
        query = query.toLowerCase();
        MatrixCursor result = new MatrixCursor(resolveProjection(projection));
        LinkedList<File> pending = new LinkedList();
        pending.add(folder);
        while (!pending.isEmpty() && result.getCount() < 24) {
            File file = (File) pending.removeFirst();
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    pending.add(child);
                }
            }
            if (file.getName().toLowerCase().contains(query) && !exclusion.contains(file.getAbsolutePath())) {
                includeFile(result, null, file);
            }
        }
        return result;
    }

    public String getDocumentType(String documentId) throws FileNotFoundException {
        return getTypeForFile(getFileForDocId(documentId));
    }

    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        File visibleFile = getFileForDocId(documentId, true);
        int pfdMode = ParcelFileDescriptor.parseMode(mode);
        if (pfdMode == 268435456 || visibleFile == null) {
            return ParcelFileDescriptor.open(file, pfdMode);
        }
        try {
            return ParcelFileDescriptor.open(file, pfdMode, this.mHandler, new -$$Lambda$FileSystemProvider$y9rjeYFpkvVjwD2Whw-ujCM-C7Y(this, documentId, visibleFile));
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to open for writing: ");
            stringBuilder.append(e);
            throw new FileNotFoundException(stringBuilder.toString());
        }
    }

    public static /* synthetic */ void lambda$openDocument$0(FileSystemProvider fileSystemProvider, String documentId, File visibleFile, IOException e) {
        fileSystemProvider.onDocIdChanged(documentId);
        fileSystemProvider.scanFile(visibleFile);
    }

    private void scanFile(File visibleFile) {
        Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        intent.setData(Uri.fromFile(visibleFile));
        getContext().sendBroadcast(intent);
    }

    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        return DocumentsContract.openImageThumbnail(getFileForDocId(documentId));
    }

    protected RowBuilder includeFile(MatrixCursor result, String docId, File file) throws FileNotFoundException {
        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }
        int flags = 0;
        if (file.canWrite()) {
            if (file.isDirectory()) {
                flags = (((0 | 8) | 4) | 64) | 256;
            } else {
                flags = (((0 | 2) | 4) | 64) | 256;
            }
        }
        String mimeType = getTypeForFile(file);
        String displayName = file.getName();
        if (mimeType.startsWith(MessagingMessage.IMAGE_MIME_TYPE_PREFIX)) {
            flags |= 1;
        }
        if (typeSupportsMetadata(mimeType)) {
            flags |= Protocol.BASE_WIFI;
        }
        RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", displayName);
        row.add("_size", Long.valueOf(file.length()));
        row.add("mime_type", mimeType);
        row.add("flags", Integer.valueOf(flags));
        long lastModified = file.lastModified();
        if (lastModified > 31536000000L) {
            row.add("last_modified", Long.valueOf(lastModified));
        }
        return row;
    }

    private static String getTypeForFile(File file) {
        if (file.isDirectory()) {
            return "vnd.android.document/directory";
        }
        return getTypeForName(file.getName());
    }

    protected boolean typeSupportsMetadata(String mimeType) {
        return MetadataReader.isSupportedMimeType(mimeType);
    }

    private static String getTypeForName(String name) {
        int lastDot = name.lastIndexOf(46);
        if (lastDot >= 0) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(lastDot + 1).toLowerCase());
            if (mime != null) {
                return mime;
            }
        }
        return MIMETYPE_OCTET_STREAM;
    }

    protected final File getFileForDocId(String docId) throws FileNotFoundException {
        return getFileForDocId(docId, false);
    }

    private String[] resolveProjection(String[] projection) {
        return projection == null ? this.mDefaultProjection : projection;
    }

    private void startObserving(File file, Uri notifyUri) {
        synchronized (this.mObservers) {
            DirectoryObserver observer = (DirectoryObserver) this.mObservers.get(file);
            if (observer == null) {
                observer = new DirectoryObserver(file, getContext().getContentResolver(), notifyUri);
                observer.startWatching();
                this.mObservers.put(file, observer);
            }
            observer.mRefCount = observer.mRefCount + 1;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0021, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void stopObserving(File file) {
        synchronized (this.mObservers) {
            DirectoryObserver observer = (DirectoryObserver) this.mObservers.get(file);
            if (observer == null) {
                return;
            }
            observer.mRefCount = observer.mRefCount - 1;
            if (observer.mRefCount == 0) {
                this.mObservers.remove(file);
                observer.stopWatching();
            }
        }
    }
}
