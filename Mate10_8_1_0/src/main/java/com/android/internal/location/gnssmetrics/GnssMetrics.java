package com.android.internal.location.gnssmetrics;

import android.os.SystemClock;
import android.util.Base64;
import android.util.TimeUtils;
import com.android.framework.protobuf.nano.MessageNano;
import com.android.internal.location.nano.GnssLogsProto.GnssLog;
import java.util.Arrays;

public class GnssMetrics {
    private static final int DEFAULT_TIME_BETWEEN_FIXES_MILLISECS = 1000;
    private Statistics locationFailureStatistics = new Statistics();
    private String logStartInElapsedRealTime;
    private Statistics positionAccuracyMeterStatistics = new Statistics();
    private Statistics timeToFirstFixSecStatistics = new Statistics();
    private Statistics topFourAverageCn0Statistics = new Statistics();

    private class Statistics {
        private int count;
        private double sum;
        private double sumSquare;

        private Statistics() {
        }

        public void reset() {
            this.count = 0;
            this.sum = 0.0d;
            this.sumSquare = 0.0d;
        }

        public void addItem(double item) {
            this.count++;
            this.sum += item;
            this.sumSquare += item * item;
        }

        public int getCount() {
            return this.count;
        }

        public double getMean() {
            return this.sum / ((double) this.count);
        }

        public double getStandardDeviation() {
            double m = this.sum / ((double) this.count);
            m *= m;
            double v = this.sumSquare / ((double) this.count);
            if (v > m) {
                return Math.sqrt(v - m);
            }
            return 0.0d;
        }
    }

    public GnssMetrics() {
        reset();
    }

    public void logReceivedLocationStatus(boolean isSuccessful) {
        if (isSuccessful) {
            this.locationFailureStatistics.addItem(0.0d);
        } else {
            this.locationFailureStatistics.addItem(1.0d);
        }
    }

    public void logMissedReports(int desiredTimeBetweenFixesMilliSeconds, int actualTimeBetweenFixesMilliSeconds) {
        int numReportMissed = (actualTimeBetweenFixesMilliSeconds / Math.max(1000, desiredTimeBetweenFixesMilliSeconds)) - 1;
        if (numReportMissed > 0) {
            for (int i = 0; i < numReportMissed; i++) {
                this.locationFailureStatistics.addItem(1.0d);
            }
        }
    }

    public void logTimeToFirstFixMilliSecs(int timeToFirstFixMilliSeconds) {
        this.timeToFirstFixSecStatistics.addItem((double) (timeToFirstFixMilliSeconds / 1000));
    }

    public void logPositionAccuracyMeters(float positionAccuracyMeters) {
        this.positionAccuracyMeterStatistics.addItem((double) positionAccuracyMeters);
    }

    public void logCn0(float[] cn0s, int numSv) {
        if (numSv >= 4) {
            float[] cn0Array = Arrays.copyOf(cn0s, numSv);
            Arrays.sort(cn0Array);
            if (((double) cn0Array[numSv - 4]) > 0.0d) {
                double top4AvgCn0 = 0.0d;
                for (int i = numSv - 4; i < numSv; i++) {
                    top4AvgCn0 += (double) cn0Array[i];
                }
                this.topFourAverageCn0Statistics.addItem(top4AvgCn0 / 4.0d);
            }
        }
    }

    public String dumpGnssMetricsAsProtoString() {
        GnssLog msg = new GnssLog();
        if (this.locationFailureStatistics.getCount() > 0) {
            msg.numLocationReportProcessed = this.locationFailureStatistics.getCount();
            msg.percentageLocationFailure = (int) (this.locationFailureStatistics.getMean() * 100.0d);
        }
        if (this.timeToFirstFixSecStatistics.getCount() > 0) {
            msg.numTimeToFirstFixProcessed = this.timeToFirstFixSecStatistics.getCount();
            msg.meanTimeToFirstFixSecs = (int) this.timeToFirstFixSecStatistics.getMean();
            msg.standardDeviationTimeToFirstFixSecs = (int) this.timeToFirstFixSecStatistics.getStandardDeviation();
        }
        if (this.positionAccuracyMeterStatistics.getCount() > 0) {
            msg.numPositionAccuracyProcessed = this.positionAccuracyMeterStatistics.getCount();
            msg.meanPositionAccuracyMeters = (int) this.positionAccuracyMeterStatistics.getMean();
            msg.standardDeviationPositionAccuracyMeters = (int) this.positionAccuracyMeterStatistics.getStandardDeviation();
        }
        if (this.topFourAverageCn0Statistics.getCount() > 0) {
            msg.numTopFourAverageCn0Processed = this.topFourAverageCn0Statistics.getCount();
            msg.meanTopFourAverageCn0DbHz = this.topFourAverageCn0Statistics.getMean();
            msg.standardDeviationTopFourAverageCn0DbHz = this.topFourAverageCn0Statistics.getStandardDeviation();
        }
        String s = Base64.encodeToString(MessageNano.toByteArray(msg), 0);
        reset();
        return s;
    }

    public String dumpGnssMetricsAsText() {
        StringBuilder s = new StringBuilder();
        s.append("GNSS_KPI_START").append('\n');
        s.append("  KPI logging start time: ").append(this.logStartInElapsedRealTime).append("\n");
        s.append("  KPI logging end time: ");
        TimeUtils.formatDuration(SystemClock.elapsedRealtimeNanos() / TimeUtils.NANOS_PER_MS, s);
        s.append("\n");
        s.append("  Number of location reports: ").append(this.locationFailureStatistics.getCount()).append("\n");
        if (this.locationFailureStatistics.getCount() > 0) {
            s.append("  Percentage location failure: ").append(this.locationFailureStatistics.getMean() * 100.0d).append("\n");
        }
        s.append("  Number of TTFF reports: ").append(this.timeToFirstFixSecStatistics.getCount()).append("\n");
        if (this.timeToFirstFixSecStatistics.getCount() > 0) {
            s.append("  TTFF mean (sec): ").append(this.timeToFirstFixSecStatistics.getMean()).append("\n");
            s.append("  TTFF standard deviation (sec): ").append(this.timeToFirstFixSecStatistics.getStandardDeviation()).append("\n");
        }
        s.append("  Number of position accuracy reports: ").append(this.positionAccuracyMeterStatistics.getCount()).append("\n");
        if (this.positionAccuracyMeterStatistics.getCount() > 0) {
            s.append("  Position accuracy mean (m): ").append(this.positionAccuracyMeterStatistics.getMean()).append("\n");
            s.append("  Position accuracy standard deviation (m): ").append(this.positionAccuracyMeterStatistics.getStandardDeviation()).append("\n");
        }
        s.append("  Number of CN0 reports: ").append(this.topFourAverageCn0Statistics.getCount()).append("\n");
        if (this.topFourAverageCn0Statistics.getCount() > 0) {
            s.append("  Top 4 Avg CN0 mean (dB-Hz): ").append(this.topFourAverageCn0Statistics.getMean()).append("\n");
            s.append("  Top 4 Avg CN0 standard deviation (dB-Hz): ").append(this.topFourAverageCn0Statistics.getStandardDeviation()).append("\n");
        }
        s.append("GNSS_KPI_END").append("\n");
        return s.toString();
    }

    private void reset() {
        StringBuilder s = new StringBuilder();
        TimeUtils.formatDuration(SystemClock.elapsedRealtimeNanos() / TimeUtils.NANOS_PER_MS, s);
        this.logStartInElapsedRealTime = s.toString();
        this.locationFailureStatistics.reset();
        this.timeToFirstFixSecStatistics.reset();
        this.positionAccuracyMeterStatistics.reset();
        this.topFourAverageCn0Statistics.reset();
    }
}
