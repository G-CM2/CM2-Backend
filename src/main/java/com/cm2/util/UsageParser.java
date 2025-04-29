package com.cm2.util;

public class UsageParser {

    public static double parseCpuUsage(String cpuStr) {
        if (cpuStr == null) return 0.0;
        cpuStr = cpuStr.replace("%", "").trim();
        try {
            return Double.parseDouble(cpuStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static int parseMemoryUsage(String memStr) {
        if (memStr == null) return 0;
        memStr = memStr.toUpperCase().trim();

        double value;
        if (memStr.endsWith("GIB"))
            value = Double.parseDouble(memStr.replace("GIB", "").trim()) * 1024;
        else if (memStr.endsWith("MIB"))
            value = Double.parseDouble(memStr.replace("MIB", "").trim());
        else if (memStr.endsWith("KIB"))
            value = Double.parseDouble(memStr.replace("KIB", "").trim()) / 1024;
        else {
            try {
                value = Double.parseDouble(memStr);
            } catch (Exception e) {
                value = 0.0;
            }
        }
        return (int) Math.round(value);
    }

}
