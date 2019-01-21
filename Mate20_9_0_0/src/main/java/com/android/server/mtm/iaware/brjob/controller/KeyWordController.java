package com.android.server.mtm.iaware.brjob.controller;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.os.Bundle;
import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobStatus;
import com.android.server.mtm.iaware.brjob.scheduler.AwareStateChangedListener;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Arrays;

public class KeyWordController extends AwareStateController {
    private static final String CONDITION_EXTRA = "Extra";
    private static final String CONDITION_KEYWORD = "KeyWord";
    private static final String EXTRA_BOOLEAN = "boolean";
    private static final String EXTRA_CHAR = "char";
    private static final String EXTRA_DOUBLE = "double";
    private static final String EXTRA_FLOAT = "float";
    private static final String EXTRA_INT = "int";
    private static final String EXTRA_LONG = "long";
    private static final String EXTRA_OBJECT = "Object";
    private static final String EXTRA_SHORT = "short";
    private static final int EXTRA_SPLIT_LENGTH = 3;
    private static final String EXTRA_STRING = "String";
    private static final String KEYWORD_HOST = "host";
    private static final String KEYWORD_MIME = "mimeType";
    private static final String KEYWORD_PACKAGE_NAME = "packageName";
    private static final String KEYWORD_PATH = "path";
    private static final String KEYWORD_PATH_PATTERN = "pathPattern";
    private static final String KEYWORD_PATH_PREFIX = "pathPrefix";
    private static final String KEYWORD_PORT = "port";
    private static final String KEYWORD_SCHEME = "scheme";
    private static final int KEYWORD_SPLIT_LENGTH = 2;
    private static final int KEYWORD_SPLIT_PACKAGE_LENGTH = 3;
    private static final int KEYWORD_SPLIT_PACKAGE_VALUE_INDEX = 2;
    private static final String KEYWORD_SSP = "ssp";
    private static final String KEYWORD_SSP_PATTERN = "sspPattern";
    private static final String KEYWORD_SSP_PREFIX = "sspPrefix";
    private static final String TAG = "KeyWordController";
    private static KeyWordController mSingleton;
    private static Object sCreationLock = new Object();

    public static KeyWordController get(AwareJobSchedulerService jms) {
        KeyWordController keyWordController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new KeyWordController(jms, jms.getContext(), jms.getLock());
            }
            keyWordController = mSingleton;
        }
        return keyWordController;
    }

    private KeyWordController(AwareStateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
    }

    public void maybeStartTrackingJobLocked(AwareJobStatus job) {
        if (job != null) {
            if (job.hasConstraint("KeyWord") || job.hasConstraint("Extra")) {
                Intent intent = job.getIntent();
                if (this.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("iaware_brjob awarejobstatus, intent: ");
                    stringBuilder.append(intent);
                    stringBuilder.append(", extras: ");
                    stringBuilder.append(intent == null ? "null" : intent.getExtras());
                    AwareLog.i(str, stringBuilder.toString());
                }
                if (job.hasConstraint("KeyWord")) {
                    checkKeyword(job);
                } else if (job.hasConstraint("Extra")) {
                    checkExtra(job);
                }
            }
        }
    }

    private void checkKeyword(AwareJobStatus job) {
        String str;
        StringBuilder stringBuilder;
        AwareJobStatus awareJobStatus = job;
        String filterValue = awareJobStatus.getActionFilterValue("KeyWord");
        if (this.DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brjob checkKeyword: ");
            stringBuilder.append(filterValue);
            AwareLog.i(str, stringBuilder.toString());
        }
        int i = 0;
        if (TextUtils.isEmpty(filterValue)) {
            if (this.DEBUG) {
                AwareLog.w(TAG, "iaware_brjob keyword config error!");
            }
            awareJobStatus.setSatisfied("KeyWord", false);
            return;
        }
        Intent intent = job.getIntent();
        if (intent == null) {
            AwareLog.w(TAG, "iaware_brjob intent is null.");
            awareJobStatus.setSatisfied("KeyWord", false);
            return;
        }
        String[] keywords = filterValue.split("[\\[\\]]");
        IntentFilter filter = new IntentFilter(job.getAction());
        String host = null;
        int i2 = 0;
        String port = null;
        boolean specialFormat = false;
        while (true) {
            int i3 = i2;
            if (i3 < keywords.length) {
                String filterValue2;
                if (!(keywords[i3] == null || keywords[i3].trim().length() == 0)) {
                    if (!":".equals(keywords[i3].trim())) {
                        StringBuilder stringBuilder2;
                        String[] values = keywords[i3].split("[:]");
                        String[] pkgValues = keywords[i3].split("[:@]");
                        if (this.DEBUG) {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("iaware_brjob config keyword: ");
                            stringBuilder2.append(keywords[i3]);
                            AwareLog.i(str, stringBuilder2.toString());
                        }
                        String str2;
                        if (pkgValues.length == 3) {
                            if (!"packageName".equals(pkgValues[i])) {
                                str2 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("iaware_brjob KeyWord value format is wrong: ");
                                stringBuilder3.append(job.getComponentName());
                                AwareLog.e(str2, stringBuilder3.toString());
                            } else if (checkPackgeKeyword(awareJobStatus, pkgValues, intent)) {
                                return;
                            }
                            specialFormat = true;
                        } else if (values.length != 2) {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("iaware_brjob KeyWord value format is wrong: ");
                            stringBuilder2.append(job.getComponentName());
                            AwareLog.e(str, stringBuilder2.toString());
                            awareJobStatus.setSatisfied("KeyWord", i);
                            return;
                        } else {
                            String key = values[i];
                            str2 = values[1];
                            if (!TextUtils.isEmpty(key)) {
                                if (!TextUtils.isEmpty(str2)) {
                                    if (KEYWORD_SCHEME.equals(key)) {
                                        filter.addDataScheme(str2);
                                    } else if (KEYWORD_HOST.equals(key)) {
                                        host = str2;
                                    } else if (KEYWORD_PORT.equals(key)) {
                                        port = str2;
                                    } else if (KEYWORD_MIME.equals(key)) {
                                        try {
                                            filter.addDataType(str2);
                                        } catch (MalformedMimeTypeException e) {
                                            MalformedMimeTypeException malformedMimeTypeException = e;
                                            AwareLog.e(TAG, "iaware_brjob invalid mimeType!");
                                        }
                                    } else if ("path".equals(key)) {
                                        filter.addDataPath(str2, 0);
                                    } else if (KEYWORD_PATH_PREFIX.equals(key)) {
                                        filter.addDataPath(str2, 1);
                                    } else if (KEYWORD_PATH_PATTERN.equals(key)) {
                                        filter.addDataPath(str2, 2);
                                    } else if (KEYWORD_SSP.equals(key)) {
                                        filter.addDataSchemeSpecificPart(str2, 0);
                                    } else if (KEYWORD_SSP_PREFIX.equals(key)) {
                                        filter.addDataSchemeSpecificPart(str2, 1);
                                    } else if (KEYWORD_SSP_PATTERN.equals(key)) {
                                        filter.addDataSchemeSpecificPart(str2, 2);
                                    } else {
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        filterValue2 = filterValue;
                                        stringBuilder.append("iaware_brjob invalid key: ");
                                        stringBuilder.append(key);
                                        AwareLog.e(str, stringBuilder.toString());
                                        i2 = i3 + 1;
                                        filterValue = filterValue2;
                                        i = 0;
                                    }
                                }
                            }
                        }
                    }
                    filterValue2 = filterValue;
                    i2 = i3 + 1;
                    filterValue = filterValue2;
                    i = 0;
                }
                filterValue2 = filterValue;
                i2 = i3 + 1;
                filterValue = filterValue2;
                i = 0;
            } else {
                if (specialFormat) {
                    filterValue = host;
                } else {
                    if (host != null) {
                        filter.addDataAuthority(host, port);
                    }
                    i = filter.match(intent.getAction(), job.getHwBroadcastRecord().getResolvedType(), intent.getScheme(), intent.getData(), null, TAG);
                    if (this.DEBUG) {
                        String str3 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("iaware_brjob filter match: ");
                        stringBuilder4.append(intent.getAction());
                        stringBuilder4.append(", ");
                        stringBuilder4.append(job.getHwBroadcastRecord().getResolvedType());
                        stringBuilder4.append(", ");
                        stringBuilder4.append(intent.getScheme());
                        stringBuilder4.append(", ");
                        stringBuilder4.append(intent.getData());
                        stringBuilder4.append(", result: ");
                        stringBuilder4.append(i);
                        AwareLog.i(str3, stringBuilder4.toString());
                    }
                    if (i >= 0) {
                        awareJobStatus.setSatisfied("KeyWord", true);
                    } else {
                        awareJobStatus.setSatisfied("KeyWord", false);
                    }
                }
                return;
            }
        }
    }

    private boolean checkPackgeKeyword(AwareJobStatus job, String[] pkgValues, Intent intent) {
        if (intent.getData() == null) {
            AwareLog.e(TAG, "intent data is null.");
            job.setSatisfied("KeyWord", false);
            return false;
        }
        boolean result;
        String pkgName = pkgValues[2];
        String ssp = intent.getData().getSchemeSpecificPart();
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brjob checkPackgeKeyword: ");
            stringBuilder.append(Arrays.toString(pkgValues));
            stringBuilder.append(", ssp: ");
            stringBuilder.append(ssp == null ? "null" : ssp);
            AwareLog.i(str, stringBuilder.toString());
        }
        if (ssp == null || pkgName == null || !ssp.contains(pkgName)) {
            result = false;
        } else {
            result = true;
        }
        job.setSatisfied("KeyWord", result);
        return result;
    }

    private void checkExtra(AwareJobStatus job) {
        Intent intent = job.getIntent();
        if (intent == null) {
            AwareLog.e(TAG, "iaware_brjob intent is null.");
            job.setSatisfied("Extra", false);
            return;
        }
        String filterValue = job.getActionFilterValue("Extra");
        if (TextUtils.isEmpty(filterValue)) {
            if (this.DEBUG) {
                AwareLog.w(TAG, "iaware_brjob extra config error!");
            }
            job.setSatisfied("Extra", false);
            return;
        }
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brjob checkExtra: ");
            stringBuilder.append(filterValue);
            AwareLog.w(str, stringBuilder.toString());
        }
        String[] extras = filterValue.split("[\\[\\]]");
        boolean hasMatch = false;
        int i = 0;
        while (i < extras.length) {
            if (!(extras[i] == null || extras[i].trim().length() == 0 || ":".equals(extras[i].trim()))) {
                if (this.DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("iaware_brjob compare extra: ");
                    stringBuilder2.append(extras[i]);
                    AwareLog.i(str2, stringBuilder2.toString());
                }
                String[] values = extras[i].split("[:@]");
                if (values.length != 3) {
                    AwareLog.e(TAG, "iaware_brjob extra value length is wrong.");
                    job.setSatisfied("Extra", false);
                    return;
                } else if (match(values[0], values[1], values[2], intent)) {
                    hasMatch = true;
                    break;
                }
            }
            i++;
        }
        if (hasMatch) {
            job.setSatisfied("Extra", true);
        } else {
            job.setSatisfied("Extra", false);
        }
    }

    public void maybeStopTrackingJobLocked(AwareJobStatus jobStatus) {
        if (jobStatus != null && this.DEBUG) {
            AwareLog.i(TAG, "iaware_brjob no tracked jobStatus.");
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            pw.println("    KeyWordController iaware_brjob nothing to dump.");
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0016, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean matchReg(String key, String type, String value, Intent intent) {
        if (key == null || type == null || value == null || intent == null || mSingleton == null) {
            return false;
        }
        return mSingleton.match(key, type, value, intent);
    }

    private boolean match(String key, String type, String value, Intent intent) {
        if (type.equals(EXTRA_BOOLEAN)) {
            return matchBoolean(key, value, intent);
        }
        if (type.equals(EXTRA_INT)) {
            return matchInt(key, value, intent);
        }
        if (type.equals(EXTRA_STRING)) {
            return matchString(key, value, intent);
        }
        if (type.equals(EXTRA_CHAR)) {
            return matchChar(key, value, intent);
        }
        if (type.equals(EXTRA_SHORT)) {
            return matchShort(key, value, intent);
        }
        if (type.equals(EXTRA_LONG)) {
            return matchLong(key, value, intent);
        }
        if (type.equals(EXTRA_DOUBLE)) {
            return matchDouble(key, value, intent);
        }
        if (type.equals(EXTRA_FLOAT)) {
            return matchFloat(key, value, intent);
        }
        if (type.equals(EXTRA_OBJECT)) {
            return matchObject(key, value, intent);
        }
        AwareLog.e(TAG, "iaware_brjob type is error");
        return false;
    }

    private boolean matchBoolean(String key, String value, Intent intent) {
        if (value.equals("true")) {
            return intent.getBooleanExtra(key, false);
        }
        if (value.equals("false")) {
            return 1 ^ intent.getBooleanExtra(key, true);
        }
        return false;
    }

    private boolean matchInt(String key, String value, Intent intent) {
        try {
            int temp = Integer.parseInt(value);
            if (intent.getIntExtra(key, temp - 1) == temp) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "iaware_brjob value format error");
            return false;
        }
    }

    private boolean matchString(String key, String value, Intent intent) {
        if (value.equals(intent.getStringExtra(key))) {
            return true;
        }
        return false;
    }

    private boolean matchChar(String key, String value, Intent intent) {
        char[] chs = value.toCharArray();
        if (chs.length != 1) {
            return false;
        }
        char temp = chs[0];
        Bundle bundle = intent.getExtras();
        return bundle != null && bundle.getChar(key) == temp;
    }

    private boolean matchFloat(String key, String value, Intent intent) {
        try {
            float temp = Float.parseFloat(value);
            if (new BigDecimal((double) intent.getFloatExtra(key, temp - 1.0f)).equals(new BigDecimal((double) temp))) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "iaware_brjob value format error");
            return false;
        }
    }

    private boolean matchDouble(String key, String value, Intent intent) {
        try {
            double temp = Double.parseDouble(value);
            if (new BigDecimal(intent.getDoubleExtra(key, temp - 1.0d)).equals(new BigDecimal(temp))) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "iaware_brjob value format error");
            return false;
        }
    }

    private boolean matchShort(String key, String value, Intent intent) {
        try {
            short temp = Short.parseShort(value);
            if (intent.getShortExtra(key, (short) (temp - 1)) == temp) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "iaware_brjob value format error");
            return false;
        }
    }

    private boolean matchLong(String key, String value, Intent intent) {
        try {
            long temp = Long.parseLong(value);
            if (intent.getLongExtra(key, temp - 1) == temp) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "iaware_brjob value format error");
            return false;
        }
    }

    private boolean matchObject(String key, String value, Intent intent) {
        Bundle extra = intent.getExtras();
        return (extra == null ? null : extra.get(key)) != null;
    }
}
