package android.icu.util;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.number.Padder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Region implements Comparable<Region> {
    private static final String OUTLYING_OCEANIA_REGION_ID = "QO";
    private static final String UNKNOWN_REGION_ID = "ZZ";
    private static final String WORLD_ID = "001";
    private static ArrayList<Set<Region>> availableRegions = null;
    private static Map<Integer, Region> numericCodeMap = null;
    private static Map<String, Region> regionAliases = null;
    private static boolean regionDataIsLoaded = false;
    private static Map<String, Region> regionIDMap = null;
    private static ArrayList<Region> regions = null;
    private int code;
    private Set<Region> containedRegions = new TreeSet();
    private Region containingRegion = null;
    private String id;
    private List<Region> preferredValues = null;
    private RegionType type;

    public enum RegionType {
        UNKNOWN,
        TERRITORY,
        WORLD,
        CONTINENT,
        SUBCONTINENT,
        GROUPING,
        DEPRECATED
    }

    private Region() {
    }

    private static synchronized void loadRegionData() {
        synchronized (Region.class) {
            if (regionDataIsLoaded) {
                return;
            }
            String r;
            UResourceBundle regionRegular;
            UResourceBundle regionMacro;
            UResourceBundle regionUnknown;
            String newRegion;
            String aliasFrom;
            UResourceBundle territoryAlias;
            Iterator it;
            Region r2;
            regionAliases = new HashMap();
            regionIDMap = new HashMap();
            numericCodeMap = new HashMap();
            availableRegions = new ArrayList(RegionType.values().length);
            UResourceBundle metadataAlias = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "metadata", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("alias");
            UResourceBundle territoryAlias2 = metadataAlias.get("territory");
            UResourceBundle supplementalData = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle codeMappings = supplementalData.get("codeMappings");
            UResourceBundle idValidity = supplementalData.get("idValidity");
            UResourceBundle regionList = idValidity.get("region");
            UResourceBundle regionRegular2 = regionList.get("regular");
            UResourceBundle regionMacro2 = regionList.get("macroregion");
            UResourceBundle regionUnknown2 = regionList.get("unknown");
            UResourceBundle territoryContainment = supplementalData.get("territoryContainment");
            UResourceBundle worldContainment = territoryContainment.get(WORLD_ID);
            UResourceBundle groupingContainment = territoryContainment.get("grouping");
            List<String> continents = Arrays.asList(worldContainment.getStringArray());
            String[] groupingArr = groupingContainment.getStringArray();
            List<String> groupings = Arrays.asList(groupingArr);
            List<String> regionCodes = new ArrayList();
            List<String> allRegions = new ArrayList();
            allRegions.addAll(Arrays.asList(regionRegular2.getStringArray()));
            allRegions.addAll(Arrays.asList(regionMacro2.getStringArray()));
            allRegions.add(regionUnknown2.getString());
            Iterator it2 = allRegions.iterator();
            while (it2.hasNext()) {
                List<String> allRegions2 = allRegions;
                Iterator it3 = it2;
                r = (String) it2.next();
                allRegions = r.indexOf("~");
                if (allRegions > null) {
                    regionRegular = regionRegular2;
                    regionRegular2 = new StringBuilder(r);
                    regionMacro = regionMacro2;
                    char endRange = regionRegular2.charAt(allRegions + 1);
                    regionRegular2.setLength(allRegions);
                    regionUnknown = regionUnknown2;
                    char lastChar = regionRegular2.charAt(allRegions - 1);
                    while (lastChar <= endRange) {
                        char endRange2 = endRange;
                        newRegion = regionRegular2.toString();
                        regionCodes.add(newRegion);
                        lastChar = (char) (lastChar + 1);
                        regionRegular2.setCharAt(allRegions - 1, lastChar);
                        endRange = endRange2;
                    }
                } else {
                    regionRegular = regionRegular2;
                    regionMacro = regionMacro2;
                    regionUnknown = regionUnknown2;
                    regionCodes.add(r);
                }
                allRegions = allRegions2;
                it2 = it3;
                regionRegular2 = regionRegular;
                regionMacro2 = regionMacro;
                regionUnknown2 = regionUnknown;
            }
            regionRegular = regionRegular2;
            regionMacro = regionMacro2;
            regionUnknown = regionUnknown2;
            regions = new ArrayList(regionCodes.size());
            for (String r3 : regionCodes) {
                Region r4 = new Region();
                r4.id = r3;
                r4.type = RegionType.TERRITORY;
                regionIDMap.put(r3, r4);
                if (r3.matches("[0-9]{3}")) {
                    r4.code = Integer.valueOf(r3).intValue();
                    numericCodeMap.put(Integer.valueOf(r4.code), r4);
                    r4.type = RegionType.SUBCONTINENT;
                } else {
                    r4.code = -1;
                }
                regions.add(r4);
            }
            int i = 0;
            while (i < territoryAlias2.getSize()) {
                List<String> regionCodes2;
                regionMacro2 = territoryAlias2.get(i);
                aliasFrom = regionMacro2.getKey();
                String aliasTo = regionMacro2.get("replacement").getString();
                if (!regionIDMap.containsKey(aliasTo) || regionIDMap.containsKey(aliasFrom)) {
                    regionCodes2 = regionCodes;
                    if (regionIDMap.containsKey(aliasFrom) != null) {
                        regionCodes = (Region) regionIDMap.get(aliasFrom);
                        territoryAlias = territoryAlias2;
                    } else {
                        regionCodes = new Region();
                        regionCodes.id = aliasFrom;
                        regionIDMap.put(aliasFrom, regionCodes);
                        if (aliasFrom.matches("[0-9]{3}")) {
                            regionCodes.code = Integer.valueOf(aliasFrom).intValue();
                            territoryAlias = territoryAlias2;
                            numericCodeMap.put(Integer.valueOf(regionCodes.code), regionCodes);
                        } else {
                            territoryAlias = territoryAlias2;
                            regionCodes.code = -1;
                        }
                        regions.add(regionCodes);
                    }
                    regionCodes.type = RegionType.DEPRECATED;
                    List<String> aliasToRegionStrings = Arrays.asList(aliasTo.split(Padder.FALLBACK_PADDING_STRING));
                    regionCodes.preferredValues = new ArrayList();
                    it = aliasToRegionStrings.iterator();
                    while (it.hasNext()) {
                        Region r5;
                        Iterator it4 = it;
                        String aliasTo2 = aliasTo;
                        aliasTo = (String) it.next();
                        if (regionIDMap.containsKey(aliasTo)) {
                            r5 = regionCodes;
                            regionCodes.preferredValues.add((Region) regionIDMap.get(aliasTo));
                        } else {
                            r5 = regionCodes;
                        }
                        it = it4;
                        aliasTo = aliasTo2;
                        Object regionCodes3 = r5;
                    }
                } else {
                    regionCodes2 = regionCodes;
                    regionAliases.put(aliasFrom, (Region) regionIDMap.get(aliasTo));
                    territoryAlias = territoryAlias2;
                }
                i++;
                regionCodes = regionCodes2;
                territoryAlias2 = territoryAlias;
            }
            territoryAlias = territoryAlias2;
            int i2 = 0;
            while (i2 < codeMappings.getSize()) {
                UResourceBundle codeMappings2;
                territoryAlias2 = codeMappings.get(i2);
                if (territoryAlias2.getType() == 8) {
                    String[] codeMappingStrings = territoryAlias2.getStringArray();
                    newRegion = codeMappingStrings[0];
                    Integer codeMappingNumber = Integer.valueOf(codeMappingStrings[1]);
                    aliasFrom = codeMappingStrings[2];
                    if (regionIDMap.containsKey(newRegion)) {
                        r2 = (Region) regionIDMap.get(newRegion);
                        r2.code = codeMappingNumber.intValue();
                        codeMappings2 = codeMappings;
                        numericCodeMap.put(Integer.valueOf(r2.code), r2);
                        regionAliases.put(aliasFrom, r2);
                        i2++;
                        codeMappings = codeMappings2;
                    }
                }
                codeMappings2 = codeMappings;
                i2++;
                codeMappings = codeMappings2;
            }
            if (regionIDMap.containsKey(WORLD_ID)) {
                ((Region) regionIDMap.get(WORLD_ID)).type = RegionType.WORLD;
            }
            if (regionIDMap.containsKey(UNKNOWN_REGION_ID)) {
                ((Region) regionIDMap.get(UNKNOWN_REGION_ID)).type = RegionType.UNKNOWN;
            }
            for (String continent : continents) {
                if (regionIDMap.containsKey(continent)) {
                    ((Region) regionIDMap.get(continent)).type = RegionType.CONTINENT;
                }
            }
            regionCodes = groupings;
            for (String grouping : regionCodes) {
                if (regionIDMap.containsKey(grouping)) {
                    ((Region) regionIDMap.get(grouping)).type = RegionType.GROUPING;
                }
            }
            if (regionIDMap.containsKey(OUTLYING_OCEANIA_REGION_ID)) {
                ((Region) regionIDMap.get(OUTLYING_OCEANIA_REGION_ID)).type = RegionType.SUBCONTINENT;
            }
            int i3 = 0;
            while (i3 < territoryContainment.getSize()) {
                List<String> groupings2;
                codeMappings = territoryContainment.get(i3);
                r3 = codeMappings.getKey();
                if (!r3.equals("containedGroupings")) {
                    if (r3.equals("deprecated")) {
                        groupings2 = regionCodes;
                        i3++;
                        regionCodes = groupings2;
                    } else {
                        r2 = (Region) regionIDMap.get(r3);
                        int j = 0;
                        while (j < codeMappings.getSize()) {
                            UResourceBundle mapping;
                            Region childRegion = (Region) regionIDMap.get(codeMappings.getString(j));
                            if (r2 == null || childRegion == null) {
                                groupings2 = regionCodes;
                                mapping = codeMappings;
                            } else {
                                groupings2 = regionCodes;
                                r2.containedRegions.add(childRegion);
                                mapping = codeMappings;
                                if (r2.getType() != RegionType.GROUPING) {
                                    childRegion.containingRegion = r2;
                                }
                            }
                            j++;
                            regionCodes = groupings2;
                            codeMappings = mapping;
                        }
                    }
                }
                groupings2 = regionCodes;
                i3++;
                regionCodes = groupings2;
            }
            int i4 = 0;
            while (true) {
                i2 = i4;
                if (i2 >= RegionType.values().length) {
                    break;
                }
                availableRegions.add(new TreeSet());
                i4 = i2 + 1;
            }
            Iterator it5 = regions.iterator();
            while (it5.hasNext()) {
                Region ar = (Region) it5.next();
                Set<Region> currentSet = (Set) availableRegions.get(ar.type.ordinal());
                currentSet.add(ar);
                availableRegions.set(ar.type.ordinal(), currentSet);
            }
            regionDataIsLoaded = true;
        }
    }

    public static Region getInstance(String id) {
        if (id != null) {
            loadRegionData();
            Region r = (Region) regionIDMap.get(id);
            if (r == null) {
                r = (Region) regionAliases.get(id);
            }
            if (r == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown region id: ");
                stringBuilder.append(id);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (r.type == RegionType.DEPRECATED && r.preferredValues.size() == 1) {
                return (Region) r.preferredValues.get(0);
            } else {
                return r;
            }
        }
        throw new NullPointerException();
    }

    public static Region getInstance(int code) {
        loadRegionData();
        Region r = (Region) numericCodeMap.get(Integer.valueOf(code));
        if (r == null) {
            String pad = "";
            if (code < 10) {
                pad = "00";
            } else if (code < 100) {
                pad = AndroidHardcodedSystemProperties.JAVA_VERSION;
            }
            String id = new StringBuilder();
            id.append(pad);
            id.append(Integer.toString(code));
            r = (Region) regionAliases.get(id.toString());
        }
        if (r == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown region code: ");
            stringBuilder.append(code);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (r.type == RegionType.DEPRECATED && r.preferredValues.size() == 1) {
            return (Region) r.preferredValues.get(0);
        } else {
            return r;
        }
    }

    public static Set<Region> getAvailable(RegionType type) {
        loadRegionData();
        return Collections.unmodifiableSet((Set) availableRegions.get(type.ordinal()));
    }

    public Region getContainingRegion() {
        loadRegionData();
        return this.containingRegion;
    }

    public Region getContainingRegion(RegionType type) {
        loadRegionData();
        if (this.containingRegion == null) {
            return null;
        }
        if (this.containingRegion.type.equals(type)) {
            return this.containingRegion;
        }
        return this.containingRegion.getContainingRegion(type);
    }

    public Set<Region> getContainedRegions() {
        loadRegionData();
        return Collections.unmodifiableSet(this.containedRegions);
    }

    public Set<Region> getContainedRegions(RegionType type) {
        loadRegionData();
        Set<Region> result = new TreeSet();
        for (Region r : getContainedRegions()) {
            if (r.getType() == type) {
                result.add(r);
            } else {
                result.addAll(r.getContainedRegions(type));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public List<Region> getPreferredValues() {
        loadRegionData();
        if (this.type == RegionType.DEPRECATED) {
            return Collections.unmodifiableList(this.preferredValues);
        }
        return null;
    }

    public boolean contains(Region other) {
        loadRegionData();
        if (this.containedRegions.contains(other)) {
            return true;
        }
        for (Region cr : this.containedRegions) {
            if (cr.contains(other)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return this.id;
    }

    public int getNumericCode() {
        return this.code;
    }

    public RegionType getType() {
        return this.type;
    }

    public int compareTo(Region other) {
        return this.id.compareTo(other.id);
    }
}
