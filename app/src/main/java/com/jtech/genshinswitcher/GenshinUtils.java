package com.jtech.genshinswitcher;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class GenshinUtils {
    public static final int INTERNATIONAL_VERSION = 0;
    public static final int CNMAINLAND_VERSION = 1;
    public static final int BILIBILI_VERSION = 3;
    public static final int XIAOMI_VERSION = 3;

    public static HashMap<String, String> versionStringMap = new HashMap<String, String>() {{
        put("com.miHoYo.GenshinImpact", "原神国际服");
        put("com.miHoYo.Yuanshen", "原神中国大陆官服");
        put("com.miHoYo.ys.bilibili", "原神渠道服-Bilibili");
        put("com.miHoYo.ys.mi", "原神渠道服-小米");
    }};

    public static ArrayList<String> getCurrentGenshinPackageNames(Set<String> allApps) {
        ArrayList<String> currentInstalled = new ArrayList<>();
        for (String v : versionStringMap.keySet()) {
            if (allApps.contains(v)) {
                currentInstalled.add(v);
            }
        }
        return currentInstalled;
    }

    public static ArrayList<String> getGenshinVersionsWithData(String[] dataFolder) {
        ArrayList<String> versions = new ArrayList<>();
        Set<String> packageNames = versionStringMap.keySet();
        for (String f : dataFolder) {
            if (!f.startsWith("com.miHoYo")) {
                continue;
            }
            for(String p:packageNames){
                if(f.equals(p)){
                    versions.add(f);
                }
            }
        }
        return versions;
    }

    public static String[] getVersionNameFromPackageNames(String[] genshinPackage) {
        int len = genshinPackage.length;
        String[] installed = new String[len];
        for (int i = 0; i < len; i++) {
            installed[i] = versionStringMap.get(genshinPackage[i]);
        }
        return installed;
    }
}


