package com.android.internal.telephony.dataconnection;

import android.database.Cursor;
import android.net.NetworkUtils;
import android.provider.HwTelephony.NumMatchs;
import android.telephony.Rlog;

public class HwApnSetting extends ApnSetting {
    public static final int APN_PRESET = 1;
    public static final String DB_PRESET = "visible";
    protected static final String LOG_TAG = "HwApnSetting";
    protected static final boolean VDBG = true;
    public int preset;

    public HwApnSetting(Cursor cursor, String[] types) {
        Cursor cursor2 = cursor;
        super(cursor2.getInt(cursor2.getColumnIndexOrThrow("_id")), cursor2.getString(cursor2.getColumnIndexOrThrow("numeric")), cursor2.getString(cursor2.getColumnIndexOrThrow(NumMatchs.NAME)), cursor2.getString(cursor2.getColumnIndexOrThrow("apn")), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("proxy"))), cursor2.getString(cursor2.getColumnIndexOrThrow("port")), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("mmsc"))), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("mmsproxy"))), cursor2.getString(cursor2.getColumnIndexOrThrow("mmsport")), cursor2.getString(cursor2.getColumnIndexOrThrow("user")), cursor2.getString(cursor2.getColumnIndexOrThrow("password")), cursor2.getInt(cursor2.getColumnIndexOrThrow("authtype")), types, cursor2.getString(cursor2.getColumnIndexOrThrow("protocol")), cursor2.getString(cursor2.getColumnIndexOrThrow("roaming_protocol")), cursor2.getInt(cursor2.getColumnIndexOrThrow("carrier_enabled")) == 1, cursor2.getInt(cursor2.getColumnIndexOrThrow("bearer")), cursor2.getInt(cursor2.getColumnIndexOrThrow("bearer_bitmask")), cursor2.getInt(cursor2.getColumnIndexOrThrow("profile_id")), cursor2.getInt(cursor2.getColumnIndexOrThrow("modem_cognitive")) == 1, cursor2.getInt(cursor2.getColumnIndexOrThrow("max_conns")), cursor2.getInt(cursor2.getColumnIndexOrThrow("wait_time")), cursor2.getInt(cursor2.getColumnIndexOrThrow("max_conns_time")), cursor2.getInt(cursor2.getColumnIndexOrThrow("mtu")), cursor2.getString(cursor2.getColumnIndexOrThrow("mvno_type")), cursor2.getString(cursor2.getColumnIndexOrThrow("mvno_match_data")));
        this.preset = 1;
        int apn_preset = 1;
        Cursor cursor3;
        try {
            cursor3 = cursor;
            try {
                apn_preset = cursor3.getInt(cursor3.getColumnIndexOrThrow(DB_PRESET));
            } catch (Exception e) {
                log("query apn_preset/visible column got exception");
                this.preset = apn_preset;
            }
        } catch (Exception e2) {
            cursor3 = cursor;
            log("query apn_preset/visible column got exception");
            this.preset = apn_preset;
        }
        this.preset = apn_preset;
    }

    public boolean isPreset() {
        return this.preset == 1;
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
