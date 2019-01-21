package com.android.server.backup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AccountSyncSettingsBackupHelper implements BackupHelper {
    private static final boolean DEBUG = false;
    private static final String JSON_FORMAT_ENCODING = "UTF-8";
    private static final String JSON_FORMAT_HEADER_KEY = "account_data";
    private static final int JSON_FORMAT_VERSION = 1;
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_ACCOUNT_AUTHORITIES = "authorities";
    private static final String KEY_ACCOUNT_NAME = "name";
    private static final String KEY_ACCOUNT_TYPE = "type";
    private static final String KEY_AUTHORITY_NAME = "name";
    private static final String KEY_AUTHORITY_SYNC_ENABLED = "syncEnabled";
    private static final String KEY_AUTHORITY_SYNC_STATE = "syncState";
    private static final String KEY_MASTER_SYNC_ENABLED = "masterSyncEnabled";
    private static final String KEY_VERSION = "version";
    private static final int MD5_BYTE_SIZE = 16;
    private static final String STASH_FILE;
    private static final int STATE_VERSION = 1;
    private static final int SYNC_REQUEST_LATCH_TIMEOUT_SECONDS = 1;
    private static final String TAG = "AccountSyncSettingsBackupHelper";
    private AccountManager mAccountManager = AccountManager.get(this.mContext);
    private Context mContext;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory());
        stringBuilder.append("/backup/unadded_account_syncsettings.json");
        STASH_FILE = stringBuilder.toString();
    }

    public AccountSyncSettingsBackupHelper(Context context) {
        this.mContext = context;
    }

    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput output, ParcelFileDescriptor newState) {
        try {
            byte[] dataBytes = serializeAccountSyncSettingsToJSON().toString().getBytes(JSON_FORMAT_ENCODING);
            byte[] oldMd5Checksum = readOldMd5Checksum(oldState);
            byte[] newMd5Checksum = generateMd5Checksum(dataBytes);
            if (Arrays.equals(oldMd5Checksum, newMd5Checksum)) {
                Log.i(TAG, "Old and new MD5 checksums match. Skipping backup.");
            } else {
                int dataSize = dataBytes.length;
                output.writeEntityHeader(JSON_FORMAT_HEADER_KEY, dataSize);
                output.writeEntityData(dataBytes, dataSize);
                Log.i(TAG, "Backup successful.");
            }
            writeNewMd5Checksum(newState, newMd5Checksum);
        } catch (IOException | NoSuchAlgorithmException | JSONException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't backup account sync settings\n");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    private JSONObject serializeAccountSyncSettingsToJSON() throws JSONException {
        Account[] accounts = this.mAccountManager.getAccounts();
        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(this.mContext.getUserId());
        HashMap<String, List<String>> accountTypeToAuthorities = new HashMap();
        int i = 0;
        for (SyncAdapterType syncAdapter : syncAdapters) {
            if (syncAdapter.isUserVisible()) {
                if (!accountTypeToAuthorities.containsKey(syncAdapter.accountType)) {
                    accountTypeToAuthorities.put(syncAdapter.accountType, new ArrayList());
                }
                ((List) accountTypeToAuthorities.get(syncAdapter.accountType)).add(syncAdapter.authority);
            }
        }
        JSONObject backupJSON = new JSONObject();
        backupJSON.put(KEY_VERSION, 1);
        backupJSON.put(KEY_MASTER_SYNC_ENABLED, ContentResolver.getMasterSyncAutomatically());
        JSONArray accountJSONArray = new JSONArray();
        int length = accounts.length;
        while (i < length) {
            Account[] accounts2;
            Account account = accounts[i];
            List<String> authorities = (List) accountTypeToAuthorities.get(account.type);
            if (authorities == null) {
                accounts2 = accounts;
            } else if (authorities.isEmpty()) {
                accounts2 = accounts;
            } else {
                JSONObject accountJSON = new JSONObject();
                accountJSON.put("name", account.name);
                accountJSON.put(KEY_ACCOUNT_TYPE, account.type);
                JSONArray authoritiesJSONArray = new JSONArray();
                for (String authority : authorities) {
                    int syncState = ContentResolver.getIsSyncable(account, authority);
                    boolean syncEnabled = ContentResolver.getSyncAutomatically(account, authority);
                    JSONObject authorityJSON = new JSONObject();
                    accounts2 = accounts;
                    authorityJSON.put("name", authority);
                    authorityJSON.put(KEY_AUTHORITY_SYNC_STATE, syncState);
                    authorityJSON.put(KEY_AUTHORITY_SYNC_ENABLED, syncEnabled);
                    authoritiesJSONArray.put(authorityJSON);
                    accounts = accounts2;
                }
                accounts2 = accounts;
                accountJSON.put(KEY_ACCOUNT_AUTHORITIES, authoritiesJSONArray);
                accountJSONArray.put(accountJSON);
            }
            i++;
            accounts = accounts2;
        }
        backupJSON.put(KEY_ACCOUNTS, accountJSONArray);
        return backupJSON;
    }

    private byte[] readOldMd5Checksum(ParcelFileDescriptor oldState) throws IOException {
        DataInputStream dataInput = new DataInputStream(new FileInputStream(oldState.getFileDescriptor()));
        byte[] oldMd5Checksum = new byte[16];
        try {
            int stateVersion = dataInput.readInt();
            if (stateVersion <= 1) {
                for (int i = 0; i < 16; i++) {
                    oldMd5Checksum[i] = dataInput.readByte();
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Backup state version is: ");
                stringBuilder.append(stateVersion);
                stringBuilder.append(" (support only up to version ");
                stringBuilder.append(1);
                stringBuilder.append(")");
                Log.i(str, stringBuilder.toString());
            }
        } catch (EOFException e) {
        }
        return oldMd5Checksum;
    }

    private void writeNewMd5Checksum(ParcelFileDescriptor newState, byte[] md5Checksum) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newState.getFileDescriptor())));
        dataOutput.writeInt(1);
        dataOutput.write(md5Checksum);
    }

    private byte[] generateMd5Checksum(byte[] data) throws NoSuchAlgorithmException {
        if (data == null) {
            return null;
        }
        return MessageDigest.getInstance("MD5").digest(data);
    }

    public void restoreEntity(BackupDataInputStream data) {
        byte[] dataBytes = new byte[data.size()];
        boolean masterSyncEnabled;
        try {
            data.read(dataBytes);
            JSONObject dataJSON = new JSONObject(new String(dataBytes, JSON_FORMAT_ENCODING));
            masterSyncEnabled = dataJSON.getBoolean(KEY_MASTER_SYNC_ENABLED);
            JSONArray accountJSONArray = dataJSON.getJSONArray(KEY_ACCOUNTS);
            if (ContentResolver.getMasterSyncAutomatically()) {
                ContentResolver.setMasterSyncAutomatically(false);
            }
            restoreFromJsonArray(accountJSONArray);
            ContentResolver.setMasterSyncAutomatically(masterSyncEnabled);
            Log.i(TAG, "Restore successful.");
        } catch (IOException | JSONException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't restore account sync settings\n");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            ContentResolver.setMasterSyncAutomatically(masterSyncEnabled);
        }
    }

    private void restoreFromJsonArray(JSONArray accountJSONArray) throws JSONException {
        HashSet<Account> currentAccounts = getAccounts();
        JSONArray unaddedAccountsJSONArray = new JSONArray();
        for (int i = 0; i < accountJSONArray.length(); i++) {
            JSONObject accountJSON = (JSONObject) accountJSONArray.get(i);
            try {
                if (currentAccounts.contains(new Account(accountJSON.getString("name"), accountJSON.getString(KEY_ACCOUNT_TYPE)))) {
                    restoreExistingAccountSyncSettingsFromJSON(accountJSON);
                } else {
                    unaddedAccountsJSONArray.put(accountJSON);
                }
            } catch (IllegalArgumentException e) {
            }
        }
        if (unaddedAccountsJSONArray.length() > 0) {
            FileOutputStream fOutput;
            try {
                fOutput = new FileOutputStream(STASH_FILE);
                new DataOutputStream(fOutput).writeUTF(unaddedAccountsJSONArray.toString());
                $closeResource(null, fOutput);
                return;
            } catch (IOException ioe) {
                Log.e(TAG, "unable to write the sync settings to the stash file", ioe);
                return;
            } catch (Throwable th) {
                $closeResource(th, fOutput);
            }
        }
        File stashFile = new File(STASH_FILE);
        if (stashFile.exists()) {
            stashFile.delete();
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private void accountAddedInternal() {
        FileInputStream fIn;
        try {
            fIn = new FileInputStream(new File(STASH_FILE));
            String jsonString = new DataInputStream(fIn).readUTF();
            $closeResource(null, fIn);
            try {
                restoreFromJsonArray(new JSONArray(jsonString));
            } catch (JSONException fIn2) {
                Log.e(TAG, "there was an error with the stashed sync settings", fIn2);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
        } catch (Throwable th) {
            $closeResource(r1, fIn2);
        }
    }

    public static void accountAdded(Context context) {
        new AccountSyncSettingsBackupHelper(context).accountAddedInternal();
    }

    private HashSet<Account> getAccounts() {
        Account[] accounts = this.mAccountManager.getAccounts();
        HashSet<Account> accountHashSet = new HashSet();
        for (Account account : accounts) {
            accountHashSet.add(account);
        }
        return accountHashSet;
    }

    private void restoreExistingAccountSyncSettingsFromJSON(JSONObject accountJSON) throws JSONException {
        JSONArray authorities = accountJSON.getJSONArray(KEY_ACCOUNT_AUTHORITIES);
        Account account = new Account(accountJSON.getString("name"), accountJSON.getString(KEY_ACCOUNT_TYPE));
        for (int i = 0; i < authorities.length(); i++) {
            JSONObject authority = (JSONObject) authorities.get(i);
            String authorityName = authority.getString("name");
            boolean wasSyncEnabled = authority.getBoolean(KEY_AUTHORITY_SYNC_ENABLED);
            int wasSyncable = authority.getInt(KEY_AUTHORITY_SYNC_STATE);
            ContentResolver.setSyncAutomaticallyAsUser(account, authorityName, wasSyncEnabled, 0);
            if (!wasSyncEnabled) {
                int i2;
                if (wasSyncable == 0) {
                    i2 = 0;
                } else {
                    i2 = 2;
                }
                ContentResolver.setIsSyncable(account, authorityName, i2);
            }
        }
    }

    public void writeNewStateDescription(ParcelFileDescriptor newState) {
    }
}
