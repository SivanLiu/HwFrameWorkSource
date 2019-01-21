package java.time.zone;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ZoneRules implements Serializable {
    private static final ZoneOffsetTransitionRule[] EMPTY_LASTRULES = new ZoneOffsetTransitionRule[0];
    private static final LocalDateTime[] EMPTY_LDT_ARRAY = new LocalDateTime[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final int LAST_CACHED_YEAR = 2100;
    private static final long serialVersionUID = 3044319355680032515L;
    private final ZoneOffsetTransitionRule[] lastRules;
    private final transient ConcurrentMap<Integer, ZoneOffsetTransition[]> lastRulesCache;
    private final long[] savingsInstantTransitions;
    private final LocalDateTime[] savingsLocalTransitions;
    private final ZoneOffset[] standardOffsets;
    private final long[] standardTransitions;
    private final ZoneOffset[] wallOffsets;

    public static ZoneRules of(ZoneOffset baseStandardOffset, ZoneOffset baseWallOffset, List<ZoneOffsetTransition> standardOffsetTransitionList, List<ZoneOffsetTransition> transitionList, List<ZoneOffsetTransitionRule> lastRules) {
        Objects.requireNonNull((Object) baseStandardOffset, "baseStandardOffset");
        Objects.requireNonNull((Object) baseWallOffset, "baseWallOffset");
        Objects.requireNonNull((Object) standardOffsetTransitionList, "standardOffsetTransitionList");
        Objects.requireNonNull((Object) transitionList, "transitionList");
        Objects.requireNonNull((Object) lastRules, "lastRules");
        return new ZoneRules(baseStandardOffset, baseWallOffset, (List) standardOffsetTransitionList, (List) transitionList, (List) lastRules);
    }

    public static ZoneRules of(ZoneOffset offset) {
        Objects.requireNonNull((Object) offset, "offset");
        return new ZoneRules(offset);
    }

    ZoneRules(ZoneOffset baseStandardOffset, ZoneOffset baseWallOffset, List<ZoneOffsetTransition> standardOffsetTransitionList, List<ZoneOffsetTransition> transitionList, List<ZoneOffsetTransitionRule> lastRules) {
        this.lastRulesCache = new ConcurrentHashMap();
        this.standardTransitions = new long[standardOffsetTransitionList.size()];
        this.standardOffsets = new ZoneOffset[(standardOffsetTransitionList.size() + 1)];
        int i = 0;
        this.standardOffsets[0] = baseStandardOffset;
        for (int i2 = 0; i2 < standardOffsetTransitionList.size(); i2++) {
            this.standardTransitions[i2] = ((ZoneOffsetTransition) standardOffsetTransitionList.get(i2)).toEpochSecond();
            this.standardOffsets[i2 + 1] = ((ZoneOffsetTransition) standardOffsetTransitionList.get(i2)).getOffsetAfter();
        }
        List<LocalDateTime> localTransitionList = new ArrayList();
        List<ZoneOffset> localTransitionOffsetList = new ArrayList();
        localTransitionOffsetList.add(baseWallOffset);
        for (ZoneOffsetTransition trans : transitionList) {
            if (trans.isGap()) {
                localTransitionList.add(trans.getDateTimeBefore());
                localTransitionList.add(trans.getDateTimeAfter());
            } else {
                localTransitionList.add(trans.getDateTimeAfter());
                localTransitionList.add(trans.getDateTimeBefore());
            }
            localTransitionOffsetList.add(trans.getOffsetAfter());
        }
        this.savingsLocalTransitions = (LocalDateTime[]) localTransitionList.toArray(new LocalDateTime[localTransitionList.size()]);
        this.wallOffsets = (ZoneOffset[]) localTransitionOffsetList.toArray(new ZoneOffset[localTransitionOffsetList.size()]);
        this.savingsInstantTransitions = new long[transitionList.size()];
        while (i < transitionList.size()) {
            this.savingsInstantTransitions[i] = ((ZoneOffsetTransition) transitionList.get(i)).toEpochSecond();
            i++;
        }
        if (lastRules.size() <= 16) {
            this.lastRules = (ZoneOffsetTransitionRule[]) lastRules.toArray(new ZoneOffsetTransitionRule[lastRules.size()]);
            return;
        }
        throw new IllegalArgumentException("Too many transition rules");
    }

    private ZoneRules(long[] standardTransitions, ZoneOffset[] standardOffsets, long[] savingsInstantTransitions, ZoneOffset[] wallOffsets, ZoneOffsetTransitionRule[] lastRules) {
        this.lastRulesCache = new ConcurrentHashMap();
        this.standardTransitions = standardTransitions;
        this.standardOffsets = standardOffsets;
        this.savingsInstantTransitions = savingsInstantTransitions;
        this.wallOffsets = wallOffsets;
        this.lastRules = lastRules;
        if (savingsInstantTransitions.length == 0) {
            this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
            return;
        }
        List<LocalDateTime> localTransitionList = new ArrayList();
        for (int i = 0; i < savingsInstantTransitions.length; i++) {
            ZoneOffsetTransition trans = new ZoneOffsetTransition(savingsInstantTransitions[i], wallOffsets[i], wallOffsets[i + 1]);
            if (trans.isGap()) {
                localTransitionList.add(trans.getDateTimeBefore());
                localTransitionList.add(trans.getDateTimeAfter());
            } else {
                localTransitionList.add(trans.getDateTimeAfter());
                localTransitionList.add(trans.getDateTimeBefore());
            }
        }
        this.savingsLocalTransitions = (LocalDateTime[]) localTransitionList.toArray(new LocalDateTime[localTransitionList.size()]);
    }

    private ZoneRules(ZoneOffset offset) {
        this.lastRulesCache = new ConcurrentHashMap();
        this.standardOffsets = new ZoneOffset[1];
        this.standardOffsets[0] = offset;
        this.standardTransitions = EMPTY_LONG_ARRAY;
        this.savingsInstantTransitions = EMPTY_LONG_ARRAY;
        this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
        this.wallOffsets = this.standardOffsets;
        this.lastRules = EMPTY_LASTRULES;
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private Object writeReplace() {
        return new Ser((byte) 1, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(this.standardTransitions.length);
        int i = 0;
        for (long trans : this.standardTransitions) {
            Ser.writeEpochSec(trans, out);
        }
        for (ZoneOffset offset : this.standardOffsets) {
            Ser.writeOffset(offset, out);
        }
        out.writeInt(this.savingsInstantTransitions.length);
        for (long trans2 : this.savingsInstantTransitions) {
            Ser.writeEpochSec(trans2, out);
        }
        for (ZoneOffset offset2 : this.wallOffsets) {
            Ser.writeOffset(offset2, out);
        }
        out.writeByte(this.lastRules.length);
        ZoneOffsetTransitionRule[] zoneOffsetTransitionRuleArr = this.lastRules;
        int length = zoneOffsetTransitionRuleArr.length;
        while (i < length) {
            zoneOffsetTransitionRuleArr[i].writeExternal(out);
            i++;
        }
    }

    static ZoneRules readExternal(DataInput in) throws IOException, ClassNotFoundException {
        long[] stdTrans;
        int i;
        long[] jArr;
        int stdSize = in.readInt();
        if (stdSize == 0) {
            stdTrans = EMPTY_LONG_ARRAY;
        } else {
            stdTrans = new long[stdSize];
        }
        int i2 = 0;
        for (i = 0; i < stdSize; i++) {
            stdTrans[i] = Ser.readEpochSec(in);
        }
        ZoneOffset[] stdOffsets = new ZoneOffset[(stdSize + 1)];
        for (i = 0; i < stdOffsets.length; i++) {
            stdOffsets[i] = Ser.readOffset(in);
        }
        int savSize = in.readInt();
        if (savSize == 0) {
            jArr = EMPTY_LONG_ARRAY;
        } else {
            jArr = new long[savSize];
        }
        long[] savTrans = jArr;
        for (i = 0; i < savSize; i++) {
            savTrans[i] = Ser.readEpochSec(in);
        }
        ZoneOffset[] savOffsets = new ZoneOffset[(savSize + 1)];
        for (i = 0; i < savOffsets.length; i++) {
            savOffsets[i] = Ser.readOffset(in);
        }
        int ruleSize = in.readByte();
        ZoneOffsetTransitionRule[] rules = ruleSize == 0 ? EMPTY_LASTRULES : new ZoneOffsetTransitionRule[ruleSize];
        while (i2 < ruleSize) {
            rules[i2] = ZoneOffsetTransitionRule.readExternal(in);
            i2++;
        }
        return new ZoneRules(stdTrans, stdOffsets, savTrans, savOffsets, rules);
    }

    public boolean isFixedOffset() {
        return this.savingsInstantTransitions.length == 0;
    }

    public ZoneOffset getOffset(Instant instant) {
        int i = 0;
        if (this.savingsInstantTransitions.length == 0) {
            return this.standardOffsets[0];
        }
        long epochSec = instant.getEpochSecond();
        if (this.lastRules.length <= 0 || epochSec <= this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1]) {
            int index = Arrays.binarySearch(this.savingsInstantTransitions, epochSec);
            if (index < 0) {
                index = (-index) - 2;
            }
            return this.wallOffsets[index + 1];
        }
        ZoneOffsetTransition[] transArray = findTransitionArray(findYear(epochSec, this.wallOffsets[this.wallOffsets.length - 1]));
        ZoneOffsetTransition trans = null;
        while (i < transArray.length) {
            trans = transArray[i];
            if (epochSec < trans.toEpochSecond()) {
                return trans.getOffsetBefore();
            }
            i++;
        }
        return trans.getOffsetAfter();
    }

    public ZoneOffset getOffset(LocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        if (info instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) info).getOffsetBefore();
        }
        return (ZoneOffset) info;
    }

    public List<ZoneOffset> getValidOffsets(LocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        if (info instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) info).getValidOffsets();
        }
        return Collections.singletonList((ZoneOffset) info);
    }

    public ZoneOffsetTransition getTransition(LocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        return info instanceof ZoneOffsetTransition ? (ZoneOffsetTransition) info : null;
    }

    private Object getOffsetInfo(LocalDateTime dt) {
        int i = 0;
        if (this.savingsInstantTransitions.length == 0) {
            return this.standardOffsets[0];
        }
        if (this.lastRules.length <= 0 || !dt.isAfter(this.savingsLocalTransitions[this.savingsLocalTransitions.length - 1])) {
            int index = Arrays.binarySearch(this.savingsLocalTransitions, (Object) dt);
            if (index == -1) {
                return this.wallOffsets[0];
            }
            if (index < 0) {
                index = (-index) - 2;
            } else if (index < this.savingsLocalTransitions.length - 1 && this.savingsLocalTransitions[index].equals(this.savingsLocalTransitions[index + 1])) {
                index++;
            }
            if ((index & 1) != 0) {
                return this.wallOffsets[(index / 2) + 1];
            }
            LocalDateTime dtBefore = this.savingsLocalTransitions[index];
            LocalDateTime dtAfter = this.savingsLocalTransitions[index + 1];
            ZoneOffset offsetBefore = this.wallOffsets[index / 2];
            ZoneOffset offsetAfter = this.wallOffsets[(index / 2) + 1];
            if (offsetAfter.getTotalSeconds() > offsetBefore.getTotalSeconds()) {
                return new ZoneOffsetTransition(dtBefore, offsetBefore, offsetAfter);
            }
            return new ZoneOffsetTransition(dtAfter, offsetBefore, offsetAfter);
        }
        ZoneOffsetTransition[] transArray = findTransitionArray(dt.getYear());
        Object info = null;
        int length = transArray.length;
        while (i < length) {
            ZoneOffsetTransition trans = transArray[i];
            info = findOffsetInfo(dt, trans);
            if ((info instanceof ZoneOffsetTransition) || info.equals(trans.getOffsetBefore())) {
                return info;
            }
            i++;
        }
        return info;
    }

    private Object findOffsetInfo(LocalDateTime dt, ZoneOffsetTransition trans) {
        LocalDateTime localTransition = trans.getDateTimeBefore();
        if (trans.isGap()) {
            if (dt.isBefore(localTransition)) {
                return trans.getOffsetBefore();
            }
            if (dt.isBefore(trans.getDateTimeAfter())) {
                return trans;
            }
            return trans.getOffsetAfter();
        } else if (!dt.isBefore(localTransition)) {
            return trans.getOffsetAfter();
        } else {
            if (dt.isBefore(trans.getDateTimeAfter())) {
                return trans.getOffsetBefore();
            }
            return trans;
        }
    }

    private ZoneOffsetTransition[] findTransitionArray(int year) {
        Integer yearObj = Integer.valueOf(year);
        ZoneOffsetTransition[] transArray = (ZoneOffsetTransition[]) this.lastRulesCache.get(yearObj);
        if (transArray != null) {
            return transArray;
        }
        ZoneOffsetTransitionRule[] ruleArray = this.lastRules;
        transArray = new ZoneOffsetTransition[ruleArray.length];
        for (int i = 0; i < ruleArray.length; i++) {
            transArray[i] = ruleArray[i].createTransition(year);
        }
        if (year < LAST_CACHED_YEAR) {
            this.lastRulesCache.putIfAbsent(yearObj, transArray);
        }
        return transArray;
    }

    public ZoneOffset getStandardOffset(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return this.standardOffsets[0];
        }
        int index = Arrays.binarySearch(this.standardTransitions, instant.getEpochSecond());
        if (index < 0) {
            index = (-index) - 2;
        }
        return this.standardOffsets[index + 1];
    }

    public Duration getDaylightSavings(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds((long) (getOffset(instant).getTotalSeconds() - getStandardOffset(instant).getTotalSeconds()));
    }

    public boolean isDaylightSavings(Instant instant) {
        return getStandardOffset(instant).equals(getOffset(instant)) ^ 1;
    }

    public boolean isValidOffset(LocalDateTime localDateTime, ZoneOffset offset) {
        return getValidOffsets(localDateTime).contains(offset);
    }

    public ZoneOffsetTransition nextTransition(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return null;
        }
        long epochSec = instant.getEpochSecond();
        int index;
        if (epochSec < this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1]) {
            int index2;
            index = Arrays.binarySearch(this.savingsInstantTransitions, epochSec);
            if (index < 0) {
                index2 = (-index) - 1;
            } else {
                index2 = index + 1;
            }
            return new ZoneOffsetTransition(this.savingsInstantTransitions[index2], this.wallOffsets[index2], this.wallOffsets[index2 + 1]);
        } else if (this.lastRules.length == 0) {
            return null;
        } else {
            index = findYear(epochSec, this.wallOffsets[this.wallOffsets.length - 1]);
            for (ZoneOffsetTransition trans : findTransitionArray(index)) {
                if (epochSec < trans.toEpochSecond()) {
                    return trans;
                }
            }
            if (index < Year.MAX_VALUE) {
                return findTransitionArray(index + 1)[0];
            }
            return null;
        }
    }

    public ZoneOffsetTransition previousTransition(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return null;
        }
        long epochSec = instant.getEpochSecond();
        if (instant.getNano() > 0 && epochSec < Long.MAX_VALUE) {
            epochSec++;
        }
        long lastHistoric = this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1];
        if (this.lastRules.length > 0 && epochSec > lastHistoric) {
            ZoneOffset lastHistoricOffset = this.wallOffsets[this.wallOffsets.length - 1];
            int year = findYear(epochSec, lastHistoricOffset);
            ZoneOffsetTransition[] transArray = findTransitionArray(year);
            for (int i = transArray.length - 1; i >= 0; i--) {
                if (epochSec > transArray[i].toEpochSecond()) {
                    return transArray[i];
                }
            }
            year--;
            if (year > findYear(lastHistoric, lastHistoricOffset)) {
                ZoneOffsetTransition[] transArray2 = findTransitionArray(year);
                return transArray2[transArray2.length - 1];
            }
        }
        int index = Arrays.binarySearch(this.savingsInstantTransitions, epochSec);
        if (index < 0) {
            index = (-index) - 1;
        }
        if (index <= 0) {
            return null;
        }
        return new ZoneOffsetTransition(this.savingsInstantTransitions[index - 1], this.wallOffsets[index - 1], this.wallOffsets[index]);
    }

    private int findYear(long epochSecond, ZoneOffset offset) {
        return LocalDate.ofEpochDay(Math.floorDiv(((long) offset.getTotalSeconds()) + epochSecond, 86400)).getYear();
    }

    public List<ZoneOffsetTransition> getTransitions() {
        List<ZoneOffsetTransition> list = new ArrayList();
        for (int i = 0; i < this.savingsInstantTransitions.length; i++) {
            list.add(new ZoneOffsetTransition(this.savingsInstantTransitions[i], this.wallOffsets[i], this.wallOffsets[i + 1]));
        }
        return Collections.unmodifiableList(list);
    }

    public List<ZoneOffsetTransitionRule> getTransitionRules() {
        return Collections.unmodifiableList(Arrays.asList(this.lastRules));
    }

    public boolean equals(Object otherRules) {
        boolean z = true;
        if (this == otherRules) {
            return true;
        }
        if (!(otherRules instanceof ZoneRules)) {
            return false;
        }
        ZoneRules other = (ZoneRules) otherRules;
        if (!(Arrays.equals(this.standardTransitions, other.standardTransitions) && Arrays.equals(this.standardOffsets, other.standardOffsets) && Arrays.equals(this.savingsInstantTransitions, other.savingsInstantTransitions) && Arrays.equals(this.wallOffsets, other.wallOffsets) && Arrays.equals(this.lastRules, other.lastRules))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (((Arrays.hashCode(this.standardTransitions) ^ Arrays.hashCode(this.standardOffsets)) ^ Arrays.hashCode(this.savingsInstantTransitions)) ^ Arrays.hashCode(this.wallOffsets)) ^ Arrays.hashCode(this.lastRules);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ZoneRules[currentStandardOffset=");
        stringBuilder.append(this.standardOffsets[this.standardOffsets.length - 1]);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
