package de.davidrival.softstep.debug;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.controller.PadConfigurationManager;

/**
 * Centralized debug logging system with granular control over different subsystems.
 * 
 * Debug Categories:
 * - DEBUG_COMMON: Startup, initialization, BWS discovery, general system events
 * - DEBUG_PERF: PERF mode functionality, PAD4 track cycling, LED updates
 * - DEBUG_USER: USER mode pad functionality, pressure, toggles, long press  
 * - DEBUG_CLIP: CLIP mode functionality, arm/unarm, clip launching, mute
 * 
 * Usage:
 * DebugLogger.common(host, padConfigManager, "Startup message");
 * DebugLogger.perf(host, padConfigManager, "PERF mode message");
 * DebugLogger.user(host, padConfigManager, "USER mode message"); 
 * DebugLogger.clip(host, padConfigManager, "CLIP mode message");
 */
public class DebugLogger {
    
    /**
     * Logs a common/startup/general system message if DEBUG_COMMON is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param message The debug message to log
     */
    public static void common(ControllerHost host, PadConfigurationManager padConfigManager, String message) {
        if (padConfigManager != null && padConfigManager.isDebugCommon()) {
            host.println("[DEBUG_COMMON] " + message);
        }
    }
    
    /**
     * Logs a PERF mode message if DEBUG_PERF is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param message The debug message to log
     */
    public static void perf(ControllerHost host, PadConfigurationManager padConfigManager, String message) {
        if (padConfigManager != null && padConfigManager.isDebugPerf()) {
            host.println("[DEBUG_PERF] " + message);
        }
    }
    
    /**
     * Logs a PERF2 mode message if DEBUG_PERF2 is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param message The debug message to log
     */
    public static void perf2(ControllerHost host, PadConfigurationManager padConfigManager, String message) {
        if (padConfigManager != null && padConfigManager.isDebugPerf2()) {
            host.println("[DEBUG_PERF2] " + message);
        }
    }
    
    /**
     * Logs a USER mode message if DEBUG_USER is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param message The debug message to log
     */
    public static void user(ControllerHost host, PadConfigurationManager padConfigManager, String message) {
        if (padConfigManager != null && padConfigManager.isDebugUser()) {
            host.println("[DEBUG_USER] " + message);
        }
    }
    
    /**
     * Logs a CLIP mode message if DEBUG_CLIP is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param message The debug message to log
     */
    public static void clip(ControllerHost host, PadConfigurationManager padConfigManager, String message) {
        if (padConfigManager != null && padConfigManager.isDebugClip()) {
            host.println("[DEBUG_CLIP] " + message);
        }
    }
    
    /**
     * Logs a message with printf-style formatting if DEBUG_COMMON is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param format The format string
     * @param args The arguments for formatting
     */
    public static void commonf(ControllerHost host, PadConfigurationManager padConfigManager, String format, Object... args) {
        if (padConfigManager != null && padConfigManager.isDebugCommon()) {
            host.println("[DEBUG_COMMON] " + String.format(format, args));
        }
    }
    
    /**
     * Logs a message with printf-style formatting if DEBUG_PERF is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param format The format string
     * @param args The arguments for formatting
     */
    public static void perff(ControllerHost host, PadConfigurationManager padConfigManager, String format, Object... args) {
        if (padConfigManager != null && padConfigManager.isDebugPerf()) {
            host.println("[DEBUG_PERF] " + String.format(format, args));
        }
    }
    
    /**
     * Logs a message with printf-style formatting if DEBUG_PERF2 is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param format The format string
     * @param args The arguments for formatting
     */
    public static void perf2f(ControllerHost host, PadConfigurationManager padConfigManager, String format, Object... args) {
        if (padConfigManager != null && padConfigManager.isDebugPerf2()) {
            host.println("[DEBUG_PERF2] " + String.format(format, args));
        }
    }
    
    /**
     * Logs a message with printf-style formatting if DEBUG_USER is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param format The format string
     * @param args The arguments for formatting
     */
    public static void userf(ControllerHost host, PadConfigurationManager padConfigManager, String format, Object... args) {
        if (padConfigManager != null && padConfigManager.isDebugUser()) {
            host.println("[DEBUG_USER] " + String.format(format, args));
        }
    }
    
    /**
     * Logs a message with printf-style formatting if DEBUG_CLIP is enabled.
     * 
     * @param host The controller host for logging
     * @param padConfigManager The configuration manager containing debug flags
     * @param format The format string
     * @param args The arguments for formatting
     */
    public static void clipf(ControllerHost host, PadConfigurationManager padConfigManager, String format, Object... args) {
        if (padConfigManager != null && padConfigManager.isDebugClip()) {
            host.println("[DEBUG_CLIP] " + String.format(format, args));
        }
    }
    
    /**
     * Always logs a message regardless of debug flags (for critical errors/warnings).
     * 
     * @param host The controller host for logging
     * @param message The message to log
     */
    public static void always(ControllerHost host, String message) {
        host.println("[BWS_CONTROLLER] " + message);
    }
}