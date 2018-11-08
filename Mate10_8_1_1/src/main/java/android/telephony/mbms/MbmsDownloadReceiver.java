package android.telephony.mbms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.MbmsDownloadSession;
import android.telephony.mbms.vendor.VendorUtils;
import android.util.Log;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MbmsDownloadReceiver extends BroadcastReceiver {
    public static final String DOWNLOAD_TOKEN_SUFFIX = ".download_token";
    private static final String LOG_TAG = "MbmsDownloadReceiver";
    private static final int MAX_TEMP_FILE_RETRIES = 5;
    public static final String MBMS_FILE_PROVIDER_META_DATA_KEY = "mbms-file-provider-authority";
    public static final int RESULT_APP_NOTIFICATION_ERROR = 6;
    public static final int RESULT_BAD_TEMP_FILE_ROOT = 3;
    public static final int RESULT_DOWNLOAD_FINALIZATION_ERROR = 4;
    public static final int RESULT_INVALID_ACTION = 1;
    public static final int RESULT_MALFORMED_INTENT = 2;
    public static final int RESULT_OK = 0;
    public static final int RESULT_TEMP_FILE_GENERATION_ERROR = 5;
    private static final String TEMP_FILE_STAGING_LOCATION = "staged_completed_files";
    private static final String TEMP_FILE_SUFFIX = ".embms.temp";
    private String mFileProviderAuthorityCache = null;
    private String mMiddlewarePackageNameCache = null;

    public void onReceive(Context context, Intent intent) {
        if (!verifyIntentContents(context, intent)) {
            setResultCode(2);
        } else if (Objects.equals(intent.getStringExtra(VendorUtils.EXTRA_TEMP_FILE_ROOT), MbmsTempFileProvider.getEmbmsTempFileDir(context).getPath())) {
            if (VendorUtils.ACTION_DOWNLOAD_RESULT_INTERNAL.equals(intent.getAction())) {
                moveDownloadedFile(context, intent);
                cleanupPostMove(context, intent);
            } else if (VendorUtils.ACTION_FILE_DESCRIPTOR_REQUEST.equals(intent.getAction())) {
                generateTempFiles(context, intent);
            } else if (VendorUtils.ACTION_CLEANUP.equals(intent.getAction())) {
                cleanupTempFiles(context, intent);
            } else {
                setResultCode(1);
            }
        } else {
            setResultCode(3);
        }
    }

    private boolean verifyIntentContents(Context context, Intent intent) {
        if (VendorUtils.ACTION_DOWNLOAD_RESULT_INTERNAL.equals(intent.getAction())) {
            if (!intent.hasExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_RESULT)) {
                Log.w(LOG_TAG, "Download result did not include a result code. Ignoring.");
                return false;
            } else if (1 != intent.getIntExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_RESULT, 2)) {
                return true;
            } else {
                if (!intent.hasExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_REQUEST)) {
                    Log.w(LOG_TAG, "Download result did not include the associated request. Ignoring.");
                    return false;
                } else if (!intent.hasExtra(VendorUtils.EXTRA_TEMP_FILE_ROOT)) {
                    Log.w(LOG_TAG, "Download result did not include the temp file root. Ignoring.");
                    return false;
                } else if (!intent.hasExtra(MbmsDownloadSession.EXTRA_MBMS_FILE_INFO)) {
                    Log.w(LOG_TAG, "Download result did not include the associated file info. Ignoring.");
                    return false;
                } else if (intent.hasExtra(VendorUtils.EXTRA_FINAL_URI)) {
                    DownloadRequest request = (DownloadRequest) intent.getParcelableExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_REQUEST);
                    File expectedTokenFile = new File(MbmsUtils.getEmbmsTempFileDirForService(context, request.getFileServiceId()), request.getHash() + DOWNLOAD_TOKEN_SUFFIX);
                    if (!expectedTokenFile.exists()) {
                        Log.w(LOG_TAG, "Supplied download request does not match a token that we have. Expected " + expectedTokenFile);
                        return false;
                    }
                } else {
                    Log.w(LOG_TAG, "Download result did not include the path to the final temp file. Ignoring.");
                    return false;
                }
            }
        } else if (VendorUtils.ACTION_FILE_DESCRIPTOR_REQUEST.equals(intent.getAction())) {
            if (!intent.hasExtra(VendorUtils.EXTRA_SERVICE_ID)) {
                Log.w(LOG_TAG, "Temp file request did not include the associated service id. Ignoring.");
                return false;
            } else if (!intent.hasExtra(VendorUtils.EXTRA_TEMP_FILE_ROOT)) {
                Log.w(LOG_TAG, "Download result did not include the temp file root. Ignoring.");
                return false;
            }
        } else if (VendorUtils.ACTION_CLEANUP.equals(intent.getAction())) {
            if (!intent.hasExtra(VendorUtils.EXTRA_SERVICE_ID)) {
                Log.w(LOG_TAG, "Cleanup request did not include the associated service id. Ignoring.");
                return false;
            } else if (!intent.hasExtra(VendorUtils.EXTRA_TEMP_FILE_ROOT)) {
                Log.w(LOG_TAG, "Cleanup request did not include the temp file root. Ignoring.");
                return false;
            } else if (!intent.hasExtra(VendorUtils.EXTRA_TEMP_FILES_IN_USE)) {
                Log.w(LOG_TAG, "Cleanup request did not include the list of temp files in use. Ignoring.");
                return false;
            }
        }
        return true;
    }

    private void moveDownloadedFile(Context context, Intent intent) {
        DownloadRequest request = (DownloadRequest) intent.getParcelableExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_REQUEST);
        Intent intentForApp = request.getIntentForApp();
        if (intentForApp == null) {
            Log.i(LOG_TAG, "Malformed app notification intent");
            setResultCode(6);
            return;
        }
        int result = intent.getIntExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_RESULT, 2);
        intentForApp.putExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_RESULT, result);
        if (result != 1) {
            Log.i(LOG_TAG, "Download request indicated a failed download. Aborting.");
            context.sendBroadcast(intentForApp);
            return;
        }
        Uri finalTempFile = (Uri) intent.getParcelableExtra(VendorUtils.EXTRA_FINAL_URI);
        if (verifyTempFilePath(context, request.getFileServiceId(), finalTempFile)) {
            FileInfo completedFileInfo = (FileInfo) intent.getParcelableExtra(MbmsDownloadSession.EXTRA_MBMS_FILE_INFO);
            try {
                intentForApp.putExtra(MbmsDownloadSession.EXTRA_MBMS_COMPLETED_FILE_URI, stageTempFile(finalTempFile, FileSystems.getDefault().getPath(MbmsTempFileProvider.getEmbmsTempFileDir(context).getPath(), new String[]{TEMP_FILE_STAGING_LOCATION})));
                intentForApp.putExtra(MbmsDownloadSession.EXTRA_MBMS_FILE_INFO, completedFileInfo);
                intentForApp.putExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_REQUEST, request);
                context.sendBroadcast(intentForApp);
                setResultCode(0);
                return;
            } catch (IOException e) {
                Log.w(LOG_TAG, "Failed to move temp file to final destination");
                setResultCode(4);
                return;
            }
        }
        Log.w(LOG_TAG, "Download result specified an invalid temp file " + finalTempFile);
        setResultCode(4);
    }

    private void cleanupPostMove(Context context, Intent intent) {
        DownloadRequest request = (DownloadRequest) intent.getParcelableExtra(MbmsDownloadSession.EXTRA_MBMS_DOWNLOAD_REQUEST);
        if (request == null) {
            Log.w(LOG_TAG, "Intent does not include a DownloadRequest. Ignoring.");
            return;
        }
        List<Uri> tempFiles = intent.getParcelableArrayListExtra(VendorUtils.EXTRA_TEMP_LIST);
        if (tempFiles != null) {
            for (Uri tempFileUri : tempFiles) {
                if (verifyTempFilePath(context, request.getFileServiceId(), tempFileUri)) {
                    new File(tempFileUri.getSchemeSpecificPart()).delete();
                }
            }
        }
    }

    private void generateTempFiles(Context context, Intent intent) {
        String serviceId = intent.getStringExtra(VendorUtils.EXTRA_SERVICE_ID);
        if (serviceId == null) {
            Log.w(LOG_TAG, "Temp file request did not include the associated service id. Ignoring.");
            setResultCode(2);
            return;
        }
        int fdCount = intent.getIntExtra(VendorUtils.EXTRA_FD_COUNT, 0);
        List<Uri> pausedList = intent.getParcelableArrayListExtra(VendorUtils.EXTRA_PAUSED_LIST);
        if (fdCount == 0 && (pausedList == null || pausedList.size() == 0)) {
            Log.i(LOG_TAG, "No temp files actually requested. Ending.");
            setResultCode(0);
            setResultExtras(Bundle.EMPTY);
            return;
        }
        ArrayList<UriPathPair> freshTempFiles = generateFreshTempFiles(context, serviceId, fdCount);
        ArrayList<UriPathPair> pausedFiles = generateUrisForPausedFiles(context, serviceId, pausedList);
        Bundle result = new Bundle();
        result.putParcelableArrayList(VendorUtils.EXTRA_FREE_URI_LIST, freshTempFiles);
        result.putParcelableArrayList(VendorUtils.EXTRA_PAUSED_URI_LIST, pausedFiles);
        setResultCode(0);
        setResultExtras(result);
    }

    private ArrayList<UriPathPair> generateFreshTempFiles(Context context, String serviceId, int freshFdCount) {
        File tempFileDir = MbmsUtils.getEmbmsTempFileDirForService(context, serviceId);
        if (!tempFileDir.exists()) {
            tempFileDir.mkdirs();
        }
        ArrayList<UriPathPair> result = new ArrayList(freshFdCount);
        for (int i = 0; i < freshFdCount; i++) {
            File tempFile = generateSingleTempFile(tempFileDir);
            if (tempFile == null) {
                setResultCode(5);
                Log.w(LOG_TAG, "Failed to generate a temp file. Moving on.");
            } else {
                Uri fileUri = Uri.fromFile(tempFile);
                Uri contentUri = MbmsTempFileProvider.getUriForFile(context, getFileProviderAuthorityCached(context), tempFile);
                context.grantUriPermission(getMiddlewarePackageCached(context), contentUri, 3);
                result.add(new UriPathPair(fileUri, contentUri));
            }
        }
        return result;
    }

    private static File generateSingleTempFile(File tempFileDir) {
        int numTries = 0;
        while (numTries < 5) {
            numTries++;
            File tempFile = new File(tempFileDir, UUID.randomUUID() + TEMP_FILE_SUFFIX);
            try {
                if (tempFile.createNewFile()) {
                    return tempFile.getCanonicalFile();
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    private ArrayList<UriPathPair> generateUrisForPausedFiles(Context context, String serviceId, List<Uri> pausedFiles) {
        if (pausedFiles == null) {
            return new ArrayList(0);
        }
        ArrayList<UriPathPair> result = new ArrayList(pausedFiles.size());
        for (Uri fileUri : pausedFiles) {
            if (verifyTempFilePath(context, serviceId, fileUri)) {
                File tempFile = new File(fileUri.getSchemeSpecificPart());
                if (tempFile.exists()) {
                    Uri contentUri = MbmsTempFileProvider.getUriForFile(context, getFileProviderAuthorityCached(context), tempFile);
                    context.grantUriPermission(getMiddlewarePackageCached(context), contentUri, 3);
                    result.add(new UriPathPair(fileUri, contentUri));
                } else {
                    Log.w(LOG_TAG, "Supplied file " + fileUri + " does not exist.");
                    setResultCode(5);
                }
            } else {
                Log.w(LOG_TAG, "Supplied file " + fileUri + " is not a valid temp file to resume");
                setResultCode(5);
            }
        }
        return result;
    }

    private void cleanupTempFiles(Context context, Intent intent) {
        File tempFileDir = MbmsUtils.getEmbmsTempFileDirForService(context, intent.getStringExtra(VendorUtils.EXTRA_SERVICE_ID));
        final List<Uri> filesInUse = intent.getParcelableArrayListExtra(VendorUtils.EXTRA_TEMP_FILES_IN_USE);
        File[] filesToDelete = tempFileDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                try {
                    File canonicalFile = file.getCanonicalFile();
                    if (!canonicalFile.getName().endsWith(MbmsDownloadReceiver.TEMP_FILE_SUFFIX)) {
                        return false;
                    }
                    return filesInUse.contains(Uri.fromFile(canonicalFile)) ^ 1;
                } catch (IOException e) {
                    Log.w(MbmsDownloadReceiver.LOG_TAG, "Got IOException canonicalizing " + file + ", not deleting.");
                    return false;
                }
            }
        });
        for (File fileToDelete : filesToDelete) {
            fileToDelete.delete();
        }
    }

    private static Uri stageTempFile(Uri fromPath, Path stagingDirectory) throws IOException {
        if ("file".equals(fromPath.getScheme())) {
            Path fromFile = FileSystems.getDefault().getPath(fromPath.getPath(), new String[0]);
            if (!Files.isDirectory(stagingDirectory, new LinkOption[0])) {
                Files.createDirectory(stagingDirectory, new FileAttribute[0]);
            }
            return Uri.fromFile(Files.move(fromFile, stagingDirectory.resolve(fromFile.getFileName()), new CopyOption[0]).toFile());
        }
        Log.w(LOG_TAG, "Moving source uri " + fromPath + " does not have a file scheme");
        return null;
    }

    private static boolean verifyTempFilePath(Context context, String serviceId, Uri filePath) {
        if ("file".equals(filePath.getScheme())) {
            String path = filePath.getSchemeSpecificPart();
            File tempFile = new File(path);
            if (!tempFile.exists()) {
                Log.w(LOG_TAG, "File at " + path + " does not exist.");
                return false;
            } else if (MbmsUtils.isContainedIn(MbmsUtils.getEmbmsTempFileDirForService(context, serviceId), tempFile)) {
                return true;
            } else {
                return false;
            }
        }
        Log.w(LOG_TAG, "Uri " + filePath + " does not have a file scheme");
        return false;
    }

    private String getFileProviderAuthorityCached(Context context) {
        if (this.mFileProviderAuthorityCache != null) {
            return this.mFileProviderAuthorityCache;
        }
        this.mFileProviderAuthorityCache = getFileProviderAuthority(context);
        return this.mFileProviderAuthorityCache;
    }

    private static String getFileProviderAuthority(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
            if (appInfo.metaData == null) {
                throw new RuntimeException("App must declare the file provider authority as metadata in the manifest.");
            }
            String authority = appInfo.metaData.getString(MBMS_FILE_PROVIDER_META_DATA_KEY);
            if (authority != null) {
                return authority;
            }
            throw new RuntimeException("App must declare the file provider authority as metadata in the manifest.");
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Package manager couldn't find " + context.getPackageName());
        }
    }

    private String getMiddlewarePackageCached(Context context) {
        if (this.mMiddlewarePackageNameCache == null) {
            this.mMiddlewarePackageNameCache = MbmsUtils.getMiddlewareServiceInfo(context, MbmsDownloadSession.MBMS_DOWNLOAD_SERVICE_ACTION).packageName;
        }
        return this.mMiddlewarePackageNameCache;
    }

    private static boolean manualMove(File src, File dst) {
        IOException e;
        Throwable th;
        InputStream in = null;
        OutputStream outputStream = null;
        try {
            if (!dst.exists()) {
                dst.createNewFile();
            }
            InputStream in2 = new FileInputStream(src);
            try {
                OutputStream out = new FileOutputStream(dst);
                try {
                    byte[] buffer = new byte[2048];
                    int len;
                    do {
                        len = in2.read(buffer);
                        out.write(buffer, 0, len);
                    } while (len > 0);
                    if (in2 != null) {
                        try {
                            in2.close();
                        } catch (IOException e2) {
                            Log.w(LOG_TAG, "Error closing streams: " + e2);
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                    return true;
                } catch (IOException e3) {
                    e2 = e3;
                    outputStream = out;
                    in = in2;
                    try {
                        Log.w(LOG_TAG, "Manual file move failed due to exception " + e2);
                        if (dst.exists()) {
                            dst.delete();
                        }
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e22) {
                                Log.w(LOG_TAG, "Error closing streams: " + e22);
                                return false;
                            }
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        return false;
                    } catch (Throwable th2) {
                        th = th2;
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e222) {
                                Log.w(LOG_TAG, "Error closing streams: " + e222);
                                throw th;
                            }
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    outputStream = out;
                    in = in2;
                    if (in != null) {
                        in.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    throw th;
                }
            } catch (IOException e4) {
                e222 = e4;
                in = in2;
                Log.w(LOG_TAG, "Manual file move failed due to exception " + e222);
                if (dst.exists()) {
                    dst.delete();
                }
                if (in != null) {
                    in.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                return false;
            } catch (Throwable th4) {
                th = th4;
                in = in2;
                if (in != null) {
                    in.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                throw th;
            }
        } catch (IOException e5) {
            e222 = e5;
            Log.w(LOG_TAG, "Manual file move failed due to exception " + e222);
            if (dst.exists()) {
                dst.delete();
            }
            if (in != null) {
                in.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            return false;
        }
    }
}
