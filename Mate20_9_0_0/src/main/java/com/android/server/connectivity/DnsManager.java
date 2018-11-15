package com.android.server.connectivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.dns.ResolvUtil;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.INetworkManagementService;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;
import com.android.server.HwServiceFactory;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class DnsManager {
    private static final int DNS_RESOLVER_DEFAULT_MAX_SAMPLES = 64;
    private static final int DNS_RESOLVER_DEFAULT_MIN_SAMPLES = 8;
    private static final int DNS_RESOLVER_DEFAULT_SAMPLE_VALIDITY_SECONDS = 1800;
    private static final int DNS_RESOLVER_DEFAULT_SUCCESS_THRESHOLD_PERCENT = 25;
    private static final PrivateDnsConfig PRIVATE_DNS_OFF = new PrivateDnsConfig();
    private static final String TAG = DnsManager.class.getSimpleName();
    private final ContentResolver mContentResolver = this.mContext.getContentResolver();
    private final Context mContext;
    private int mMaxSamples;
    private int mMinSamples;
    private final INetworkManagementService mNMS;
    private int mNumDnsEntries;
    private final Map<Integer, PrivateDnsConfig> mPrivateDnsMap;
    private String mPrivateDnsMode;
    private String mPrivateDnsSpecifier;
    private final Map<Integer, PrivateDnsValidationStatuses> mPrivateDnsValidationMap;
    private int mSampleValidity;
    private int mSuccessThreshold;
    private final MockableSystemProperties mSystemProperties;

    public static class PrivateDnsConfig {
        public final String hostname;
        public final InetAddress[] ips;
        public final boolean useTls;

        public PrivateDnsConfig() {
            this(false);
        }

        public PrivateDnsConfig(boolean useTls) {
            this.useTls = useTls;
            this.hostname = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.ips = new InetAddress[0];
        }

        public PrivateDnsConfig(String hostname, InetAddress[] ips) {
            this.useTls = TextUtils.isEmpty(hostname) ^ 1;
            this.hostname = this.useTls ? hostname : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.ips = ips != null ? ips : new InetAddress[0];
        }

        public PrivateDnsConfig(PrivateDnsConfig cfg) {
            this.useTls = cfg.useTls;
            this.hostname = cfg.hostname;
            this.ips = cfg.ips;
        }

        public boolean inStrictMode() {
            return this.useTls && !TextUtils.isEmpty(this.hostname);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PrivateDnsConfig.class.getSimpleName());
            stringBuilder.append("{");
            stringBuilder.append(this.useTls);
            stringBuilder.append(":");
            stringBuilder.append(this.hostname);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(Arrays.toString(this.ips));
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private static class PrivateDnsValidationStatuses {
        private Map<Pair<String, InetAddress>, ValidationStatus> mValidationMap;

        enum ValidationStatus {
            IN_PROGRESS,
            FAILED,
            SUCCEEDED
        }

        private PrivateDnsValidationStatuses() {
            this.mValidationMap = new HashMap();
        }

        private boolean hasValidatedServer() {
            for (ValidationStatus status : this.mValidationMap.values()) {
                if (status == ValidationStatus.SUCCEEDED) {
                    return true;
                }
            }
            return false;
        }

        private void updateTrackedDnses(String[] ipAddresses, String hostname) {
            Set<Pair<String, InetAddress>> latestDnses = new HashSet();
            for (String ipAddress : ipAddresses) {
                try {
                    latestDnses.add(new Pair(hostname, InetAddress.parseNumericAddress(ipAddress)));
                } catch (IllegalArgumentException e) {
                }
            }
            Iterator<Entry<Pair<String, InetAddress>, ValidationStatus>> it = this.mValidationMap.entrySet().iterator();
            while (it.hasNext()) {
                if (!latestDnses.contains(((Entry) it.next()).getKey())) {
                    it.remove();
                }
            }
            for (Pair<String, InetAddress> p : latestDnses) {
                if (!this.mValidationMap.containsKey(p)) {
                    this.mValidationMap.put(p, ValidationStatus.IN_PROGRESS);
                }
            }
        }

        private void updateStatus(PrivateDnsValidationUpdate update) {
            Pair<String, InetAddress> p = new Pair(update.hostname, update.ipAddress);
            if (this.mValidationMap.containsKey(p)) {
                if (update.validated) {
                    this.mValidationMap.put(p, ValidationStatus.SUCCEEDED);
                } else {
                    this.mValidationMap.put(p, ValidationStatus.FAILED);
                }
            }
        }

        private LinkProperties fillInValidatedPrivateDns(LinkProperties lp) {
            lp.setValidatedPrivateDnsServers(Collections.EMPTY_LIST);
            this.mValidationMap.forEach(new -$$Lambda$DnsManager$PrivateDnsValidationStatuses$_X4_M08nKysv-L4hDpqAsa4SBxI(lp));
            return lp;
        }

        static /* synthetic */ void lambda$fillInValidatedPrivateDns$0(LinkProperties lp, Pair key, ValidationStatus value) {
            if (value == ValidationStatus.SUCCEEDED) {
                lp.addValidatedPrivateDnsServer((InetAddress) key.second);
            }
        }
    }

    public static class PrivateDnsValidationUpdate {
        public final String hostname;
        public final InetAddress ipAddress;
        public final int netId;
        public final boolean validated;

        public PrivateDnsValidationUpdate(int netId, InetAddress ipAddress, String hostname, boolean validated) {
            this.netId = netId;
            this.ipAddress = ipAddress;
            this.hostname = hostname;
            this.validated = validated;
        }
    }

    public static PrivateDnsConfig getPrivateDnsConfig(ContentResolver cr) {
        String mode = getPrivateDnsMode(cr);
        boolean useTls = (TextUtils.isEmpty(mode) || "off".equals(mode)) ? false : true;
        if ("hostname".equals(mode)) {
            return new PrivateDnsConfig(getStringSetting(cr, "private_dns_specifier"), null);
        }
        return new PrivateDnsConfig(useTls);
    }

    public static PrivateDnsConfig tryBlockingResolveOf(Network network, String name) {
        try {
            return new PrivateDnsConfig(name, ResolvUtil.blockingResolveAllLocally(network, name));
        } catch (UnknownHostException e) {
            return new PrivateDnsConfig(name, null);
        }
    }

    public static Uri[] getPrivateDnsSettingsUris() {
        return new Uri[]{Global.getUriFor("private_dns_default_mode"), Global.getUriFor("private_dns_mode"), Global.getUriFor("private_dns_specifier")};
    }

    public DnsManager(Context ctx, INetworkManagementService nms, MockableSystemProperties sp) {
        this.mContext = ctx;
        this.mNMS = nms;
        this.mSystemProperties = sp;
        this.mPrivateDnsMap = new HashMap();
        this.mPrivateDnsValidationMap = new HashMap();
    }

    public PrivateDnsConfig getPrivateDnsConfig() {
        return getPrivateDnsConfig(this.mContentResolver);
    }

    public void removeNetwork(Network network) {
        this.mPrivateDnsMap.remove(Integer.valueOf(network.netId));
        this.mPrivateDnsValidationMap.remove(Integer.valueOf(network.netId));
    }

    public PrivateDnsConfig updatePrivateDns(Network network, PrivateDnsConfig cfg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updatePrivateDns(");
        stringBuilder.append(network);
        stringBuilder.append(", ");
        stringBuilder.append(cfg);
        stringBuilder.append(")");
        Slog.w(str, stringBuilder.toString());
        if (cfg != null) {
            return (PrivateDnsConfig) this.mPrivateDnsMap.put(Integer.valueOf(network.netId), cfg);
        }
        return (PrivateDnsConfig) this.mPrivateDnsMap.remove(Integer.valueOf(network.netId));
    }

    public void updatePrivateDnsStatus(int netId, LinkProperties lp) {
        PrivateDnsConfig privateDnsCfg = (PrivateDnsConfig) this.mPrivateDnsMap.getOrDefault(Integer.valueOf(netId), PRIVATE_DNS_OFF);
        String tlsHostname = null;
        PrivateDnsValidationStatuses statuses = privateDnsCfg.useTls ? (PrivateDnsValidationStatuses) this.mPrivateDnsValidationMap.get(Integer.valueOf(netId)) : null;
        boolean usingPrivateDns = false;
        boolean validated = statuses != null && statuses.hasValidatedServer();
        boolean strictMode = privateDnsCfg.inStrictMode();
        if (strictMode) {
            tlsHostname = privateDnsCfg.hostname;
        }
        if (strictMode || validated) {
            usingPrivateDns = true;
        }
        lp.setUsePrivateDns(usingPrivateDns);
        lp.setPrivateDnsServerName(tlsHostname);
        if (!usingPrivateDns || statuses == null) {
            lp.setValidatedPrivateDnsServers(Collections.EMPTY_LIST);
        } else {
            statuses.fillInValidatedPrivateDns(lp);
        }
    }

    public void updatePrivateDnsValidation(PrivateDnsValidationUpdate update) {
        PrivateDnsValidationStatuses statuses = (PrivateDnsValidationStatuses) this.mPrivateDnsValidationMap.get(Integer.valueOf(update.netId));
        if (statuses != null) {
            statuses.updateStatus(update);
        }
    }

    public void setDnsConfigurationForNetwork(int netId, LinkProperties lp, boolean isDefaultNetwork) {
        String[] makeStrings;
        Exception e;
        String[] assignedServers = NetworkUtils.makeStrings(lp.getDnsServers());
        String[] domainStrs = getDomainStrings(lp.getDomains());
        updateParametersSettings();
        int[] params = new int[]{this.mSampleValidity, this.mSuccessThreshold, this.mMinSamples, this.mMaxSamples};
        PrivateDnsConfig privateDnsCfg = (PrivateDnsConfig) this.mPrivateDnsMap.getOrDefault(Integer.valueOf(netId), PRIVATE_DNS_OFF);
        int i = netId;
        boolean z = privateDnsCfg.useTls && !HwServiceFactory.getHwConnectivityManager().isBypassPrivateDns(i);
        boolean useTls = z;
        boolean strictMode = privateDnsCfg.inStrictMode();
        String tlsHostname = strictMode ? privateDnsCfg.hostname : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (strictMode) {
            makeStrings = NetworkUtils.makeStrings((Collection) Arrays.stream(privateDnsCfg.ips).filter(new -$$Lambda$DnsManager$Z_oEyRSp0wthIcVTcqKDoAJRe6Q(lp)).collect(Collectors.toList()));
        } else {
            LinkProperties linkProperties = lp;
            makeStrings = useTls ? assignedServers : new String[0];
        }
        String[] tlsServers = makeStrings;
        if (useTls) {
            if (!this.mPrivateDnsValidationMap.containsKey(Integer.valueOf(netId))) {
                this.mPrivateDnsValidationMap.put(Integer.valueOf(netId), new PrivateDnsValidationStatuses());
            }
            ((PrivateDnsValidationStatuses) this.mPrivateDnsValidationMap.get(Integer.valueOf(netId))).updateTrackedDnses(tlsServers, tlsHostname);
        } else {
            this.mPrivateDnsValidationMap.remove(Integer.valueOf(netId));
        }
        Slog.d(TAG, String.format("setDnsConfigurationForNetwork(%d, %s, %s, %s, %s, %s)", new Object[]{Integer.valueOf(netId), Arrays.toString(assignedServers), Arrays.toString(domainStrs), Arrays.toString(params), tlsHostname, Arrays.toString(tlsServers)}));
        try {
            try {
                this.mNMS.setDnsConfigurationForNetwork(i, assignedServers, domainStrs, params, tlsHostname, tlsServers);
                if (isDefaultNetwork) {
                    setDefaultDnsSystemProperties(lp.getDnsServers());
                }
                flushVmDnsCache();
            } catch (Exception e2) {
                e = e2;
            }
        } catch (Exception e3) {
            e = e3;
            String[] strArr = tlsServers;
            String str = tlsHostname;
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error setting DNS configuration: ");
            stringBuilder.append(e);
            Slog.e(str2, stringBuilder.toString());
        }
    }

    public void setDefaultDnsSystemProperties(Collection<InetAddress> dnses) {
        int last = 0;
        for (InetAddress dns : dnses) {
            last++;
            setNetDnsProperty(last, dns.getHostAddress());
        }
        for (int i = last + 1; i <= this.mNumDnsEntries; i++) {
            setNetDnsProperty(i, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        this.mNumDnsEntries = last;
    }

    private void flushVmDnsCache() {
        Intent intent = new Intent("android.intent.action.CLEAR_DNS_CACHE");
        intent.addFlags(536870912);
        intent.addFlags(67108864);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void updateParametersSettings() {
        String str;
        StringBuilder stringBuilder;
        this.mSampleValidity = getIntSetting("dns_resolver_sample_validity_seconds", 1800);
        if (this.mSampleValidity < 0 || this.mSampleValidity > NetworkConstants.ARP_HWTYPE_RESERVED_HI) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid sampleValidity=");
            stringBuilder.append(this.mSampleValidity);
            stringBuilder.append(", using default=");
            stringBuilder.append(1800);
            Slog.w(str, stringBuilder.toString());
            this.mSampleValidity = 1800;
        }
        this.mSuccessThreshold = getIntSetting("dns_resolver_success_threshold_percent", 25);
        if (this.mSuccessThreshold < 0 || this.mSuccessThreshold > 100) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid successThreshold=");
            stringBuilder.append(this.mSuccessThreshold);
            stringBuilder.append(", using default=");
            stringBuilder.append(25);
            Slog.w(str, stringBuilder.toString());
            this.mSuccessThreshold = 25;
        }
        this.mMinSamples = getIntSetting("dns_resolver_min_samples", 8);
        this.mMaxSamples = getIntSetting("dns_resolver_max_samples", 64);
        if (this.mMinSamples < 0 || this.mMinSamples > this.mMaxSamples || this.mMaxSamples > 64) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid sample count (min, max)=(");
            stringBuilder2.append(this.mMinSamples);
            stringBuilder2.append(", ");
            stringBuilder2.append(this.mMaxSamples);
            stringBuilder2.append("), using default=(");
            stringBuilder2.append(8);
            stringBuilder2.append(", ");
            stringBuilder2.append(64);
            stringBuilder2.append(")");
            Slog.w(str, stringBuilder2.toString());
            this.mMinSamples = 8;
            this.mMaxSamples = 64;
        }
    }

    private int getIntSetting(String which, int dflt) {
        return Global.getInt(this.mContentResolver, which, dflt);
    }

    private void setNetDnsProperty(int which, String value) {
        String key = new StringBuilder();
        key.append("net.dns");
        key.append(which);
        try {
            this.mSystemProperties.set(key.toString(), value);
        } catch (Exception e) {
            Slog.e(TAG, "Error setting unsupported net.dns property: ", e);
        }
    }

    private static String getPrivateDnsMode(ContentResolver cr) {
        String mode = getStringSetting(cr, "private_dns_mode");
        if (TextUtils.isEmpty(mode)) {
            mode = getStringSetting(cr, "private_dns_default_mode");
        }
        if (TextUtils.isEmpty(mode)) {
            return "off";
        }
        return mode;
    }

    private static String getStringSetting(ContentResolver cr, String which) {
        return Global.getString(cr, which);
    }

    private static String[] getDomainStrings(String domains) {
        return TextUtils.isEmpty(domains) ? new String[0] : domains.split(" ");
    }
}
