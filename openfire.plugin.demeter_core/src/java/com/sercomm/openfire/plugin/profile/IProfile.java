package com.sercomm.openfire.plugin.profile;

import java.util.List;
import java.util.Map;

public interface IProfile
{
    public final static long PREFERENCE_COMMAND_INTERVAL = 7 * 1000L;

    /*
     * Get CPU, memory and storage usages by ubus command
     */
    Map<?,?> getUsage(String serial, String mac, String appName) throws Throwable;    
    /*
     * Install specific package (App) by ubus command
     */
    Map<?,?> installApp(String serial, String mac, String appName, Map<String, Object> payload) throws Throwable;
    /*
     * Patch/upgrade specific package (App) by ubus command
     */
    Map<?,?> patchApp(String serial, String mac, String appName, Map<String, Object> payload) throws Throwable;
    /*
     * Uninstalled specific package (App) by ubus command
     */
    Map<?,?> uninstallApp(String serial, String mac, String appName) throws Throwable;
    /*
     * Get details of installed packages (App) by ubus command
     */
    List<Map<?,?>> getInstalledApps(String serial, String mac) throws Throwable;
    /*
     * Get details of installed package (App) by ubus command
     */
    Map<?,?> getInstalledApp(String serial, String mac, String appName) throws Throwable;
    /*
     * Start the specific package (App) by ubus command
     */
    Map<?,?> startApp(String serial, String mac, String appName) throws Throwable;
    /*
     * Stop the specific package (App) by ubus command
     */
    Map<?,?> stopApp(String serial, String mac, String appName) throws Throwable;
    /*
     * Generate context of container configuration
     * NOTICE: reserved and TODO
     */
    String generateContainerConfiguration(String serial, String mac, String appName) throws Throwable;
}
