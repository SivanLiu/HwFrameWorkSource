package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.FileUtils;
import android.provider.Settings.Secure;
import android.util.Slog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import libcore.io.IoUtils;

public class CertBlacklister extends Binder {
    private static final String BLACKLIST_ROOT;
    public static final String PUBKEY_BLACKLIST_KEY = "pubkey_blacklist";
    public static final String PUBKEY_PATH;
    public static final String SERIAL_BLACKLIST_KEY = "serial_blacklist";
    public static final String SERIAL_PATH;
    private static final String TAG = "CertBlacklister";

    private static class BlacklistObserver extends ContentObserver {
        private final ContentResolver mContentResolver;
        private final String mKey;
        private final String mName;
        private final String mPath;
        private final File mTmpDir = new File(this.mPath).getParentFile();

        public BlacklistObserver(String key, String name, String path, ContentResolver cr) {
            super(null);
            this.mKey = key;
            this.mName = name;
            this.mPath = path;
            this.mContentResolver = cr;
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            writeBlacklist();
        }

        public String getValue() {
            return Secure.getString(this.mContentResolver, this.mKey);
        }

        private void writeBlacklist() {
            new Thread("BlacklistUpdater") {
                public void run() {
                    synchronized (BlacklistObserver.this.mTmpDir) {
                        String blacklist = BlacklistObserver.this.getValue();
                        if (blacklist != null) {
                            Slog.i(CertBlacklister.TAG, "Certificate blacklist changed, updating...");
                            FileOutputStream out = null;
                            try {
                                File tmp = File.createTempFile("journal", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BlacklistObserver.this.mTmpDir);
                                tmp.setReadable(true, false);
                                out = new FileOutputStream(tmp);
                                out.write(blacklist.getBytes());
                                FileUtils.sync(out);
                                tmp.renameTo(new File(BlacklistObserver.this.mPath));
                                Slog.i(CertBlacklister.TAG, "Certificate blacklist updated");
                                IoUtils.closeQuietly(out);
                            } catch (IOException e) {
                                try {
                                    Slog.e(CertBlacklister.TAG, "Failed to write blacklist", e);
                                } finally {
                                    IoUtils.closeQuietly(out);
                                }
                            }
                        }
                    }
                }
            }.start();
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.getenv("ANDROID_DATA"));
        stringBuilder.append("/misc/keychain/");
        BLACKLIST_ROOT = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(BLACKLIST_ROOT);
        stringBuilder.append("pubkey_blacklist.txt");
        PUBKEY_PATH = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(BLACKLIST_ROOT);
        stringBuilder.append("serial_blacklist.txt");
        SERIAL_PATH = stringBuilder.toString();
    }

    public CertBlacklister(Context context) {
        registerObservers(context.getContentResolver());
    }

    private BlacklistObserver buildPubkeyObserver(ContentResolver cr) {
        return new BlacklistObserver(PUBKEY_BLACKLIST_KEY, "pubkey", PUBKEY_PATH, cr);
    }

    private BlacklistObserver buildSerialObserver(ContentResolver cr) {
        return new BlacklistObserver(SERIAL_BLACKLIST_KEY, "serial", SERIAL_PATH, cr);
    }

    private void registerObservers(ContentResolver cr) {
        cr.registerContentObserver(Secure.getUriFor(PUBKEY_BLACKLIST_KEY), true, buildPubkeyObserver(cr));
        cr.registerContentObserver(Secure.getUriFor(SERIAL_BLACKLIST_KEY), true, buildSerialObserver(cr));
    }
}
