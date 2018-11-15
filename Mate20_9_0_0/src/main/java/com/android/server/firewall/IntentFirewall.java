package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.EventLogTags;
import com.android.server.IntentResolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class IntentFirewall {
    private static final int LOG_PACKAGES_MAX_LENGTH = 150;
    private static final int LOG_PACKAGES_SUFFICIENT_LENGTH = 125;
    private static final File RULES_DIR = new File(Environment.getDataSystemDirectory(), "ifw");
    static final String TAG = "IntentFirewall";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_BROADCAST = "broadcast";
    private static final String TAG_RULES = "rules";
    private static final String TAG_SERVICE = "service";
    private static final int TYPE_ACTIVITY = 0;
    private static final int TYPE_BROADCAST = 1;
    private static final int TYPE_SERVICE = 2;
    private static final HashMap<String, FilterFactory> factoryMap;
    private FirewallIntentResolver mActivityResolver = new FirewallIntentResolver();
    private final AMSInterface mAms;
    private FirewallIntentResolver mBroadcastResolver = new FirewallIntentResolver();
    final FirewallHandler mHandler;
    private final RuleObserver mObserver;
    private FirewallIntentResolver mServiceResolver = new FirewallIntentResolver();

    public interface AMSInterface {
        int checkComponentPermission(String str, int i, int i2, int i3, boolean z);

        Object getAMSLock();
    }

    private final class FirewallHandler extends Handler {
        public FirewallHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            IntentFirewall.this.readRulesDir(IntentFirewall.getRulesDir());
        }
    }

    private static class FirewallIntentFilter extends IntentFilter {
        private final Rule rule;

        public FirewallIntentFilter(Rule rule) {
            this.rule = rule;
        }
    }

    private class RuleObserver extends FileObserver {
        private static final int MONITORED_EVENTS = 968;

        public RuleObserver(File monitoredDir) {
            super(monitoredDir.getAbsolutePath(), MONITORED_EVENTS);
        }

        public void onEvent(int event, String path) {
            if (path.endsWith(".xml")) {
                IntentFirewall.this.mHandler.removeMessages(0);
                IntentFirewall.this.mHandler.sendEmptyMessageDelayed(0, 250);
            }
        }
    }

    private static class FirewallIntentResolver extends IntentResolver<FirewallIntentFilter, Rule> {
        private final ArrayMap<ComponentName, Rule[]> mRulesByComponent;

        private FirewallIntentResolver() {
            this.mRulesByComponent = new ArrayMap(0);
        }

        protected boolean allowFilterResult(FirewallIntentFilter filter, List<Rule> dest) {
            return dest.contains(filter.rule) ^ 1;
        }

        protected boolean isPackageForFilter(String packageName, FirewallIntentFilter filter) {
            return true;
        }

        protected FirewallIntentFilter[] newArray(int size) {
            return new FirewallIntentFilter[size];
        }

        protected Rule newResult(FirewallIntentFilter filter, int match, int userId) {
            return filter.rule;
        }

        protected void sortResults(List<Rule> list) {
        }

        public void queryByComponent(ComponentName componentName, List<Rule> candidateRules) {
            Rule[] rules = (Rule[]) this.mRulesByComponent.get(componentName);
            if (rules != null) {
                candidateRules.addAll(Arrays.asList(rules));
            }
        }

        public void addComponentFilter(ComponentName componentName, Rule rule) {
            this.mRulesByComponent.put(componentName, (Rule[]) ArrayUtils.appendElement(Rule.class, (Rule[]) this.mRulesByComponent.get(componentName), rule));
        }
    }

    private static class Rule extends AndFilter {
        private static final String ATTR_BLOCK = "block";
        private static final String ATTR_LOG = "log";
        private static final String ATTR_NAME = "name";
        private static final String TAG_COMPONENT_FILTER = "component-filter";
        private static final String TAG_INTENT_FILTER = "intent-filter";
        private boolean block;
        private boolean log;
        private final ArrayList<ComponentName> mComponentFilters;
        private final ArrayList<FirewallIntentFilter> mIntentFilters;

        private Rule() {
            this.mIntentFilters = new ArrayList(1);
            this.mComponentFilters = new ArrayList(0);
        }

        public Rule readFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            this.block = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_BLOCK));
            this.log = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_LOG));
            super.readFromXml(parser);
            return this;
        }

        protected void readChild(XmlPullParser parser) throws IOException, XmlPullParserException {
            String currentTag = parser.getName();
            if (currentTag.equals(TAG_INTENT_FILTER)) {
                FirewallIntentFilter intentFilter = new FirewallIntentFilter(this);
                intentFilter.readFromXml(parser);
                this.mIntentFilters.add(intentFilter);
            } else if (currentTag.equals(TAG_COMPONENT_FILTER)) {
                String componentStr = parser.getAttributeValue(null, "name");
                if (componentStr != null) {
                    ComponentName componentName = ComponentName.unflattenFromString(componentStr);
                    if (componentName != null) {
                        this.mComponentFilters.add(componentName);
                        return;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid component name: ");
                    stringBuilder.append(componentStr);
                    throw new XmlPullParserException(stringBuilder.toString());
                }
                throw new XmlPullParserException("Component name must be specified.", parser, null);
            } else {
                super.readChild(parser);
            }
        }

        public int getIntentFilterCount() {
            return this.mIntentFilters.size();
        }

        public FirewallIntentFilter getIntentFilter(int index) {
            return (FirewallIntentFilter) this.mIntentFilters.get(index);
        }

        public int getComponentFilterCount() {
            return this.mComponentFilters.size();
        }

        public ComponentName getComponentFilter(int index) {
            return (ComponentName) this.mComponentFilters.get(index);
        }

        public boolean getBlock() {
            return this.block;
        }

        public boolean getLog() {
            return this.log;
        }
    }

    static {
        factories = new FilterFactory[18];
        int i = 0;
        factories[0] = AndFilter.FACTORY;
        factories[1] = OrFilter.FACTORY;
        factories[2] = NotFilter.FACTORY;
        factories[3] = StringFilter.ACTION;
        factories[4] = StringFilter.COMPONENT;
        factories[5] = StringFilter.COMPONENT_NAME;
        factories[6] = StringFilter.COMPONENT_PACKAGE;
        factories[7] = StringFilter.DATA;
        factories[8] = StringFilter.HOST;
        factories[9] = StringFilter.MIME_TYPE;
        factories[10] = StringFilter.SCHEME;
        factories[11] = StringFilter.PATH;
        factories[12] = StringFilter.SSP;
        factories[13] = CategoryFilter.FACTORY;
        factories[14] = SenderFilter.FACTORY;
        factories[15] = SenderPackageFilter.FACTORY;
        factories[16] = SenderPermissionFilter.FACTORY;
        factories[17] = PortFilter.FACTORY;
        factoryMap = new HashMap((factories.length * 4) / 3);
        while (true) {
            int i2 = i;
            if (i2 < factories.length) {
                FilterFactory factory = factories[i2];
                factoryMap.put(factory.getTagName(), factory);
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    public IntentFirewall(AMSInterface ams, Handler handler) {
        this.mAms = ams;
        this.mHandler = new FirewallHandler(handler.getLooper());
        File rulesDir = getRulesDir();
        rulesDir.mkdirs();
        readRulesDir(rulesDir);
        this.mObserver = new RuleObserver(rulesDir);
        this.mObserver.startWatching();
    }

    public boolean checkStartActivity(Intent intent, int callerUid, int callerPid, String resolvedType, ApplicationInfo resolvedApp) {
        return checkIntent(this.mActivityResolver, intent.getComponent(), 0, intent, callerUid, callerPid, resolvedType, resolvedApp.uid);
    }

    public boolean checkService(ComponentName resolvedService, Intent intent, int callerUid, int callerPid, String resolvedType, ApplicationInfo resolvedApp) {
        return checkIntent(this.mServiceResolver, resolvedService, 2, intent, callerUid, callerPid, resolvedType, resolvedApp.uid);
    }

    public boolean checkBroadcast(Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        return checkIntent(this.mBroadcastResolver, intent.getComponent(), 1, intent, callerUid, callerPid, resolvedType, receivingUid);
    }

    public boolean checkIntent(FirewallIntentResolver resolver, ComponentName resolvedComponent, int intentType, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        FirewallIntentResolver firewallIntentResolver = resolver;
        Intent intent2 = intent;
        String str = resolvedType;
        List<Rule> candidateRules = firewallIntentResolver.queryIntent(intent2, str, false, 0);
        if (candidateRules == null) {
            candidateRules = new ArrayList();
        }
        List<Rule> candidateRules2 = candidateRules;
        ComponentName componentName = resolvedComponent;
        firewallIntentResolver.queryByComponent(componentName, candidateRules2);
        boolean log = false;
        boolean block = false;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= candidateRules2.size()) {
                break;
            }
            Rule rule = (Rule) candidateRules2.get(i2);
            Rule rule2 = rule;
            int i3 = i2;
            if (rule.matches(this, componentName, intent2, callerUid, callerPid, str, receivingUid)) {
                block |= rule2.getBlock();
                log |= rule2.getLog();
                if (block && log) {
                    break;
                }
            }
            i = i3 + 1;
        }
        if (log) {
            logIntent(intentType, intent2, callerUid, str);
        } else {
            i = intentType;
            int i4 = callerUid;
        }
        return !block;
    }

    private static void logIntent(int intentType, Intent intent, int callerUid, String resolvedType) {
        int i;
        ComponentName cn = intent.getComponent();
        String shortComponent = null;
        if (cn != null) {
            shortComponent = cn.flattenToShortString();
        }
        String shortComponent2 = shortComponent;
        String callerPackages = null;
        int callerPackageCount = 0;
        IPackageManager pm = AppGlobals.getPackageManager();
        if (pm != null) {
            i = callerUid;
            try {
                String[] callerPackagesArray = pm.getPackagesForUid(i);
                if (callerPackagesArray != null) {
                    callerPackageCount = callerPackagesArray.length;
                    callerPackages = joinPackages(callerPackagesArray);
                }
            } catch (int callerPackageCount2) {
                Slog.e(TAG, "Remote exception while retrieving packages", callerPackageCount2);
            }
        } else {
            i = callerUid;
        }
        String callerPackages2 = callerPackages;
        int callerPackageCount22 = callerPackageCount;
        EventLogTags.writeIfwIntentMatched(intentType, shortComponent2, i, callerPackageCount22, callerPackages2, intent.getAction(), resolvedType, intent.getDataString(), intent.getFlags());
    }

    private static String joinPackages(String[] packages) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String pkg : packages) {
            if ((sb.length() + pkg.length()) + 1 < 150) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(pkg);
            } else if (sb.length() >= LOG_PACKAGES_SUFFICIENT_LENGTH) {
                return sb.toString();
            }
        }
        if (sb.length() != 0 || packages.length <= 0) {
            return null;
        }
        String pkg2 = packages[0];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pkg2.substring((pkg2.length() - 150) + 1));
        stringBuilder.append('-');
        return stringBuilder.toString();
    }

    public static File getRulesDir() {
        return RULES_DIR;
    }

    private void readRulesDir(File rulesDir) {
        FirewallIntentResolver[] resolvers = new FirewallIntentResolver[3];
        for (int i = 0; i < resolvers.length; i++) {
            resolvers[i] = new FirewallIntentResolver();
        }
        File[] files = rulesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".xml")) {
                    readRules(file, resolvers);
                }
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Read new rules (A:");
        stringBuilder.append(resolvers[0].filterSet().size());
        stringBuilder.append(" B:");
        stringBuilder.append(resolvers[1].filterSet().size());
        stringBuilder.append(" S:");
        stringBuilder.append(resolvers[2].filterSet().size());
        stringBuilder.append(")");
        Slog.i(str, stringBuilder.toString());
        synchronized (this.mAms.getAMSLock()) {
            this.mActivityResolver = resolvers[0];
            this.mBroadcastResolver = resolvers[1];
            this.mServiceResolver = resolvers[2];
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:54:0x00f8 A:{Splitter: B:6:0x0020, ExcHandler: java.io.IOException (r0_27 'e' java.io.IOException)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:28:0x0077, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:29:0x0078, code:
            r12 = r0;
            r12 = TAG;
            r13 = new java.lang.StringBuilder();
            r13.append("Error reading an intent firewall rule from ");
            r13.append(r1);
            android.util.Slog.e(r12, r13.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:52:0x00f5, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:53:0x00f6, code:
            r4 = r0;
     */
    /* JADX WARNING: Missing block: B:54:0x00f8, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:55:0x00f9, code:
            r4 = r0;
     */
    /* JADX WARNING: Missing block: B:57:?, code:
            r0 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Error reading intent firewall rules from ");
            r5.append(r1);
            android.util.Slog.e(r0, r5.toString(), r4);
     */
    /* JADX WARNING: Missing block: B:59:?, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:60:0x0114, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:61:0x0115, code:
            r5 = r0;
            r5 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("Error while closing ");
            r7.append(r1);
            android.util.Slog.e(r5, r7.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:73:?, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:74:0x0167, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:75:0x0168, code:
            r5 = r0;
            r5 = new java.lang.StringBuilder();
            r5.append("Error while closing ");
            r5.append(r1);
            android.util.Slog.e(TAG, r5.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:76:0x017f, code:
            throw r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readRules(File rulesFile, FirewallIntentResolver[] resolvers) {
        int i;
        IOException iOException;
        String str;
        File file = rulesFile;
        ArrayList rulesByType = new ArrayList(3);
        for (i = 0; i < 3; i++) {
            rulesByType.add(new ArrayList());
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, null);
                XmlUtils.beginDocument(parser, TAG_RULES);
                i = parser.getDepth();
                while (true) {
                    int outerDepth = i;
                    if (XmlUtils.nextElementWithin(parser, outerDepth)) {
                        i = -1;
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ACTIVITY)) {
                            i = 0;
                        } else if (tagName.equals(TAG_BROADCAST)) {
                            i = 1;
                        } else if (tagName.equals(TAG_SERVICE)) {
                            i = 2;
                        }
                        int ruleType = i;
                        if (ruleType != -1) {
                            Rule rule = new Rule();
                            List<Rule> rules = (List) rulesByType.get(ruleType);
                            rule.readFromXml(parser);
                            rules.add(rule);
                        }
                        i = outerDepth;
                    } else {
                        try {
                            fis.close();
                        } catch (IOException ex) {
                            iOException = ex;
                            str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error while closing ");
                            stringBuilder.append(file);
                            Slog.e(str, stringBuilder.toString(), ex);
                        }
                        for (i = 0; i < rulesByType.size(); i++) {
                            List<Rule> rules2 = (List) rulesByType.get(i);
                            FirewallIntentResolver resolver = resolvers[i];
                            for (int ruleIndex = 0; ruleIndex < rules2.size(); ruleIndex++) {
                                int i2;
                                Rule rule2 = (Rule) rules2.get(ruleIndex);
                                for (i2 = 0; i2 < rule2.getIntentFilterCount(); i2++) {
                                    resolver.addFilter(rule2.getIntentFilter(i2));
                                }
                                for (i2 = 0; i2 < rule2.getComponentFilterCount(); i2++) {
                                    resolver.addComponentFilter(rule2.getComponentFilter(i2), rule2);
                                }
                            }
                        }
                        return;
                    }
                }
            } catch (XmlPullParserException e) {
                XmlPullParserException ex2 = e;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading intent firewall rules from ");
                stringBuilder2.append(file);
                Slog.e(str2, stringBuilder2.toString(), ex2);
                try {
                    fis.close();
                } catch (IOException ex3) {
                    iOException = ex3;
                    str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Error while closing ");
                    stringBuilder3.append(file);
                    Slog.e(str, stringBuilder3.toString(), ex3);
                }
            } catch (IOException e2) {
            }
        } catch (FileNotFoundException e3) {
        }
    }

    static Filter parseFilter(XmlPullParser parser) throws IOException, XmlPullParserException {
        String elementName = parser.getName();
        FilterFactory factory = (FilterFactory) factoryMap.get(elementName);
        if (factory != null) {
            return factory.newFilter(parser);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown element in filter list: ");
        stringBuilder.append(elementName);
        throw new XmlPullParserException(stringBuilder.toString());
    }

    boolean checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
        return this.mAms.checkComponentPermission(permission, pid, uid, owningUid, exported) == 0;
    }

    boolean signaturesMatch(int uid1, int uid2) {
        boolean z = false;
        try {
            if (AppGlobals.getPackageManager().checkUidSignatures(uid1, uid2) == 0) {
                z = true;
            }
            return z;
        } catch (RemoteException ex) {
            Slog.e(TAG, "Remote exception while checking signatures", ex);
            return false;
        }
    }
}
