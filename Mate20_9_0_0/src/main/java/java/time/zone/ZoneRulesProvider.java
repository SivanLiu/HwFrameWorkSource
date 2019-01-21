package java.time.zone;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ZoneRulesProvider {
    private static final CopyOnWriteArrayList<ZoneRulesProvider> PROVIDERS = new CopyOnWriteArrayList();
    private static final ConcurrentMap<String, ZoneRulesProvider> ZONES = new ConcurrentHashMap(512, 0.75f, 2);

    protected abstract ZoneRules provideRules(String str, boolean z);

    protected abstract NavigableMap<String, ZoneRules> provideVersions(String str);

    protected abstract Set<String> provideZoneIds();

    static {
        registerProvider(new IcuZoneRulesProvider());
    }

    public static Set<String> getAvailableZoneIds() {
        return new HashSet(ZONES.keySet());
    }

    public static ZoneRules getRules(String zoneId, boolean forCaching) {
        Objects.requireNonNull((Object) zoneId, "zoneId");
        return getProvider(zoneId).provideRules(zoneId, forCaching);
    }

    public static NavigableMap<String, ZoneRules> getVersions(String zoneId) {
        Objects.requireNonNull((Object) zoneId, "zoneId");
        return getProvider(zoneId).provideVersions(zoneId);
    }

    private static ZoneRulesProvider getProvider(String zoneId) {
        ZoneRulesProvider provider = (ZoneRulesProvider) ZONES.get(zoneId);
        if (provider != null) {
            return provider;
        }
        if (ZONES.isEmpty()) {
            throw new ZoneRulesException("No time-zone data files registered");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown time-zone ID: ");
        stringBuilder.append(zoneId);
        throw new ZoneRulesException(stringBuilder.toString());
    }

    public static void registerProvider(ZoneRulesProvider provider) {
        Objects.requireNonNull((Object) provider, "provider");
        registerProvider0(provider);
        PROVIDERS.add(provider);
    }

    private static void registerProvider0(ZoneRulesProvider provider) {
        for (String zoneId : provider.provideZoneIds()) {
            Objects.requireNonNull((Object) zoneId, "zoneId");
            if (((ZoneRulesProvider) ZONES.putIfAbsent(zoneId, provider)) != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to register zone as one already registered with that ID: ");
                stringBuilder.append(zoneId);
                stringBuilder.append(", currently loading from provider: ");
                stringBuilder.append((Object) provider);
                throw new ZoneRulesException(stringBuilder.toString());
            }
        }
    }

    public static boolean refresh() {
        boolean changed = false;
        Iterator it = PROVIDERS.iterator();
        while (it.hasNext()) {
            changed |= ((ZoneRulesProvider) it.next()).provideRefresh();
        }
        return changed;
    }

    protected ZoneRulesProvider() {
    }

    protected boolean provideRefresh() {
        return false;
    }
}
