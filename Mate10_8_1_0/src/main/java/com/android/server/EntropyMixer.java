package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class EntropyMixer extends Binder {
    private static final int ENTROPY_WHAT = 1;
    private static final int ENTROPY_WRITE_PERIOD = 10800000;
    private static final long START_NANOTIME = System.nanoTime();
    private static final long START_TIME = System.currentTimeMillis();
    private static final String TAG = "EntropyMixer";
    private final String entropyFile;
    private final String hwRandomDevice;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Handler mHandler;
    private final String randomDevice;

    public EntropyMixer(Context context) {
        this(context, getSystemDir() + "/entropy.dat", "/dev/urandom", "/dev/hw_random");
    }

    public EntropyMixer(Context context, String entropyFile, String randomDevice, String hwRandomDevice) {
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    Slog.e(EntropyMixer.TAG, "Will not process invalid message");
                    return;
                }
                EntropyMixer.this.addHwRandomEntropy();
                EntropyMixer.this.writeEntropy();
                EntropyMixer.this.scheduleEntropyWriter();
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                EntropyMixer.this.writeEntropy();
            }
        };
        if (randomDevice == null) {
            throw new NullPointerException("randomDevice");
        } else if (hwRandomDevice == null) {
            throw new NullPointerException("hwRandomDevice");
        } else if (entropyFile == null) {
            throw new NullPointerException("entropyFile");
        } else {
            this.randomDevice = randomDevice;
            this.hwRandomDevice = hwRandomDevice;
            this.entropyFile = entropyFile;
            loadInitialEntropy();
            addDeviceSpecificEntropy();
            addHwRandomEntropy();
            writeEntropy();
            scheduleEntropyWriter();
            IntentFilter broadcastFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
            broadcastFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            broadcastFilter.addAction("android.intent.action.REBOOT");
            context.registerReceiver(this.mBroadcastReceiver, broadcastFilter);
        }
    }

    private void scheduleEntropyWriter() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 10800000);
    }

    private void loadInitialEntropy() {
        try {
            RandomBlock.fromFile(this.entropyFile).toFile(this.randomDevice, false);
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "No existing entropy file -- first boot?");
        } catch (IOException e2) {
            Slog.w(TAG, "Failure loading existing entropy file", e2);
        }
    }

    private void writeEntropy() {
        try {
            Slog.i(TAG, "Writing entropy...");
            RandomBlock.fromFile(this.randomDevice).toFile(this.entropyFile, true);
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write entropy", e);
        }
    }

    private void addDeviceSpecificEntropy() {
        IOException e;
        Throwable th;
        PrintWriter printWriter = null;
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(this.randomDevice));
            try {
                out.println("Copyright (C) 2009 The Android Open Source Project");
                out.println("All Your Randomness Are Belong To Us");
                out.println(START_TIME);
                out.println(START_NANOTIME);
                out.println(SystemProperties.get("ro.serialno"));
                out.println(SystemProperties.get("ro.bootmode"));
                out.println(SystemProperties.get("ro.baseband"));
                out.println(SystemProperties.get("ro.carrier"));
                out.println(SystemProperties.get("ro.bootloader"));
                out.println(SystemProperties.get("ro.hardware"));
                out.println(SystemProperties.get("ro.revision"));
                out.println(SystemProperties.get("ro.build.fingerprint"));
                out.println(new Object().hashCode());
                out.println(System.currentTimeMillis());
                out.println(System.nanoTime());
                if (out != null) {
                    out.close();
                }
                printWriter = out;
            } catch (IOException e2) {
                e = e2;
                printWriter = out;
                try {
                    Slog.w(TAG, "Unable to add device specific data to the entropy pool", e);
                    if (printWriter != null) {
                        printWriter.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (printWriter != null) {
                        printWriter.close();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                printWriter = out;
                if (printWriter != null) {
                    printWriter.close();
                }
                throw th;
            }
        } catch (IOException e3) {
            e = e3;
            Slog.w(TAG, "Unable to add device specific data to the entropy pool", e);
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    private void addHwRandomEntropy() {
        try {
            RandomBlock.fromFile(this.hwRandomDevice).toFile(this.randomDevice, false);
            Slog.i(TAG, "Added HW RNG output to entropy pool");
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            Slog.w(TAG, "Failed to add HW RNG output to entropy pool", e2);
        }
    }

    private static String getSystemDir() {
        File systemDir = new File(Environment.getDataDirectory(), "system");
        systemDir.mkdirs();
        return systemDir.toString();
    }
}
