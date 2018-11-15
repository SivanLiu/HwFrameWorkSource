package com.android.server.hidata.wavemapping.entity;

import com.android.server.hidata.wavemapping.cons.Constant;
import java.util.Locale;

public class RecognizeResult {
    private String allApModelName;
    private String mainApModelName;
    private String mainApRgResult = Constant.RESULT_UNKNOWN;
    private String rgResult = Constant.RESULT_UNKNOWN;

    public String getMainApRgResult() {
        return this.mainApRgResult;
    }

    public int getMainApRgResultInt() {
        if (this.mainApRgResult == null) {
            return 0;
        }
        int result = 0;
        if (Constant.PATTERN_STR2INT.matcher(this.mainApRgResult).matches()) {
            try {
                result = Integer.parseInt(this.mainApRgResult);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return result;
    }

    public void setMainApRgResult(String mainApRgResult) {
        this.mainApRgResult = mainApRgResult;
    }

    public String getRgResult() {
        return this.rgResult;
    }

    public int getRgResultInt() {
        if (this.rgResult == null) {
            return 0;
        }
        int result = 0;
        if (Constant.PATTERN_STR2INT.matcher(this.rgResult).matches()) {
            try {
                result = Integer.parseInt(this.rgResult);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return result;
    }

    public void setRgResult(String rgResult) {
        this.rgResult = rgResult;
    }

    public boolean cmpResults(RecognizeResult results) {
        if (this.rgResult.equals(results.getRgResult()) && this.mainApRgResult.equals(results.getMainApRgResult()) && !this.rgResult.toLowerCase(Locale.ENGLISH).contains(Constant.RESULT_UNKNOWN)) {
            return false;
        }
        return true;
    }

    public String getMainApModelName() {
        return this.mainApModelName;
    }

    public int getMainApModelNameInt() {
        if (this.mainApModelName == null) {
            return 0;
        }
        int model = 0;
        if (Constant.PATTERN_STR2INT.matcher(this.mainApModelName).matches()) {
            try {
                model = Integer.parseInt(this.mainApModelName);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return model;
    }

    public void setMainApModelName(String mainApModelName) {
        this.mainApModelName = mainApModelName;
    }

    public String getAllApModelName() {
        return this.allApModelName;
    }

    public int getAllApModelNameInt() {
        if (this.allApModelName == null) {
            return 0;
        }
        int model = 0;
        if (Constant.PATTERN_STR2INT.matcher(this.allApModelName).matches()) {
            try {
                model = Integer.parseInt(this.allApModelName);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return model;
    }

    public void setAllApModelName(String allApModelName) {
        this.allApModelName = allApModelName;
    }

    public RecognizeResult normalizeCopy() {
        RecognizeResult results = new RecognizeResult();
        if (this.rgResult.contains(Constant.RESULT_UNKNOWN)) {
            results.setRgResult("0");
        } else {
            results.setRgResult(this.rgResult);
        }
        if (this.mainApRgResult.contains(Constant.RESULT_UNKNOWN)) {
            results.setMainApRgResult("0");
        } else {
            results.setMainApRgResult(this.mainApRgResult);
        }
        results.setAllApModelName(this.allApModelName);
        results.setMainApModelName(this.mainApModelName);
        return results;
    }

    public String printResults() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Space ID:{ mainAp=");
        stringBuilder.append(this.mainApRgResult);
        stringBuilder.append(",  allAp=");
        stringBuilder.append(this.rgResult);
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }

    public String printResultsDemo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Space ID: ");
        stringBuilder.append(this.rgResult);
        stringBuilder.append(" ");
        return stringBuilder.toString();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RecognizeResult{mainApRgResult='");
        stringBuilder.append(this.mainApRgResult);
        stringBuilder.append('\'');
        stringBuilder.append(", rgResult='");
        stringBuilder.append(this.rgResult);
        stringBuilder.append('\'');
        stringBuilder.append(", mainApModelName='");
        stringBuilder.append(this.mainApModelName);
        stringBuilder.append('\'');
        stringBuilder.append(", allApModelName='");
        stringBuilder.append(this.allApModelName);
        stringBuilder.append('\'');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
