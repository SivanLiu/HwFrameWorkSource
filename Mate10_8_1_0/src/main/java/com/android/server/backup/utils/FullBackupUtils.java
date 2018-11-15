package com.android.server.backup.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build.VERSION;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import android.util.StringBuilderPrinter;
import com.android.server.backup.RefactoredBackupManagerService;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FullBackupUtils {
    public static void routeSocketDataToOutput(ParcelFileDescriptor inPipe, OutputStream out) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(inPipe.getFileDescriptor()));
        byte[] buffer = new byte[32768];
        while (true) {
            int chunkTotal = in.readInt();
            if (chunkTotal > 0) {
                while (chunkTotal > 0) {
                    int nRead = in.read(buffer, 0, chunkTotal > buffer.length ? buffer.length : chunkTotal);
                    if (nRead < 0) {
                        Slog.e(RefactoredBackupManagerService.TAG, "Unexpectedly reached end of file while reading data");
                        throw new EOFException();
                    } else {
                        out.write(buffer, 0, nRead);
                        chunkTotal -= nRead;
                    }
                }
            } else {
                return;
            }
        }
    }

    public static void writeAppManifest(PackageInfo pkg, PackageManager packageManager, File manifestFile, boolean withApk, boolean withWidgets) throws IOException {
        StringBuilder builder = new StringBuilder(4096);
        StringBuilderPrinter printer = new StringBuilderPrinter(builder);
        printer.println(Integer.toString(1));
        printer.println(pkg.packageName);
        printer.println(Integer.toString(pkg.versionCode));
        printer.println(Integer.toString(VERSION.SDK_INT));
        String installerName = packageManager.getInstallerPackageName(pkg.packageName);
        if (installerName == null) {
            installerName = "";
        }
        printer.println(installerName);
        printer.println(withApk ? "1" : "0");
        if (pkg.signatures == null) {
            printer.println("0");
        } else {
            printer.println(Integer.toString(pkg.signatures.length));
            for (Signature sig : pkg.signatures) {
                printer.println(sig.toCharsString());
            }
        }
        FileOutputStream outstream = new FileOutputStream(manifestFile);
        outstream.write(builder.toString().getBytes());
        outstream.close();
        manifestFile.setLastModified(0);
    }
}
