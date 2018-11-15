package com.android.server.radar;

public class RadarHeader {
    private static final int HEAD_MAX_SIZE = 256;
    private int mBugType;
    private int mLevel;
    private String mPackageName;
    private int mScene;
    private String mVersion;

    public RadarHeader(String packageName, String version, int bugType, int scene, int level) {
        this.mPackageName = packageName;
        this.mVersion = version;
        this.mLevel = level;
        this.mBugType = bugType;
        this.mScene = scene;
    }

    public RadarHeader(String packageName, String version, int scene, int level) {
        this(packageName, version, 100, scene, level);
    }

    public String getRadarHeader() {
        String header = new StringBuilder(256);
        header.append("Package: ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mPackageName);
        stringBuilder.append("\n");
        header.append(stringBuilder.toString());
        header.append("APK version: ");
        stringBuilder = new StringBuilder();
        stringBuilder.append(this.mVersion);
        stringBuilder.append("\n");
        header.append(stringBuilder.toString());
        header.append("Bug type: ");
        header.append(this.mBugType);
        header.append("\n");
        header.append("Scene def: ");
        header.append(this.mScene);
        header.append("\n");
        return header.toString();
    }

    public int getScene() {
        return this.mScene;
    }

    public int getLevel() {
        return this.mLevel;
    }
}
