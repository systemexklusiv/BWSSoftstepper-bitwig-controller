package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.Parameter;
import de.davidrival.softstep.controller.Page;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static de.davidrival.softstep.api.ApiManager.USER_CONTROL_PARAMETER_RESOLUTION;

public class ApiControllerToHost extends SimpleConsolePrinter{

    private final ApiManager api;

    public ApiControllerToHost(ApiManager api) {
        super(api.getHost());
        this.api = api;
    }

    public void fireSlotAt(int number) {
        api.getSlotBank()
                .launch(number);
    }

    public void deleteSlotAt(int number) {
        api.getSlotBank()
                .getItemAt(number)
                .deleteObject();
    }

    public void setValueOfUserControl(int index, int value) {
        Parameter parameter = api.getUserControls()
                .getControl(index);
        
        // Use original method to maintain UserControl identity for mapping
        parameter.set(value, USER_CONTROL_PARAMETER_RESOLUTION);

        // update LEDs only for pad UserControls (0-9), not for long press (10-19)
        if (index < 10) {
            api.getSoftstepController().getSoftstepHardware().drawFastAt( index, value > 0
                            ? Page.USER_LED_STATES.FOOT_ON
                            : Page.USER_LED_STATES.FOOT_OFF);
        }
    }
    
    /**
     * Sets UserControl value immediately, bypassing takeover mode completely.
     * This method uses Parameter.setImmediately() which ignores the user's takeover mode settings
     * and applies the value change immediately. Use this for parameter control after mapping,
     * not for mapping detection itself.
     * 
     * @param index UserControl index (0-19)
     * @param value Raw value (0-127) - will be normalized to 0.0-1.0 internally
     */
    public void setValueOfUserControlImmediately(int index, int value) {
        Parameter parameter = api.getUserControls()
                .getControl(index);
                
        // Convert 0-127 value to normalized 0.0-1.0 range for setImmediately()
        double normalizedValue = Math.max(0.0, Math.min(1.0, value / 127.0));
        
        // Apply immediately - bypasses takeover mode completely
        parameter.setImmediately(normalizedValue);

        // update LEDs only for pad UserControls (0-9), not for long press (10-19)
        if (index < 10) {
            api.getSoftstepController().getSoftstepHardware().drawFastAt( index, value > 0
                            ? Page.USER_LED_STATES.FOOT_ON
                            : Page.USER_LED_STATES.FOOT_OFF);
        }
    }

    public void clipSlotBankDown() {
//        p("clipSlotBankUp");
        api.getTrackBank().scrollForwards();
        api.getTrack().selectInMixer();
    }

    public void clipSlotBankUp() {
//        p("clipSlotBankDown");
        api.getTrackBank().scrollBackwards();
        api.getTrack().selectInMixer();
    }

    public void clipSlotBankLeft() {
//        p("clipSlotBankLeft");
        api.getSceneBank().scrollByPages(-1);

    }

    public void clipSlotBankRight() {
//        p("clipSlotBankRight");
        api.getSceneBank().scrollByPages(1);
    }

    public void armTrack() {
        api.getTrackCurser().arm().toggle();
    }

    public void muteTrack() {
        api.getTrackCurser().mute().toggle();
    }

    public void stopTrack() {
        api.getTrackCurser().stop();
    }

    public void deleteAllSlots() {
        int size = api.getSlotBank().getSizeOfBank();
        for (int i = 0; i < size; i++) {
            deleteSlotAt(i);
        }
    }
    
    /**
     * Sends repeated UserControl signals in a burst pattern for mapping detection.
     * This method is used by both hardware long press and Studio I/O Panel triggers
     * to ensure Bitwig recognizes the UserControl as a continuous control signal.
     * 
     * @param userControlIndex The UserControl index to send to (0-19)
     * @param value The value to send (0-127) 
     * @param burstCount Number of signals to send
     * @param burstDelayMs Delay between signals in milliseconds
     * @param description Description for logging and notifications (e.g., "Hardware Long Press Pad 0", "Studio I/O Panel Pad 3")
     * @param onProgress Optional callback for progress updates (can be null)
     * @param onComplete Optional callback when burst is complete (can be null)
     */
    public void sendUserControlBurst(int userControlIndex, int value, int burstCount, int burstDelayMs, 
                                   String description, Runnable onProgress, Runnable onComplete) {
        
        // Validate parameters
        if (userControlIndex < 0 || userControlIndex >= 20) {
            api.getHost().println("ERROR: Invalid UserControl index: " + userControlIndex + " (must be 0-19)");
            return;
        }
        if (burstCount <= 0 || burstCount > 50) {
            api.getHost().println("ERROR: Invalid burst count: " + burstCount + " (must be 1-50)");
            return;
        }
        if (burstDelayMs < 10 || burstDelayMs > 1000) {
            api.getHost().println("ERROR: Invalid burst delay: " + burstDelayMs + "ms (must be 10-1000ms)");
            return;
        }
        
        // Show start notification  
        api.getHost().showPopupNotification(String.format(
            "%s → UserControl%d (sending %d signals...)", 
            description, userControlIndex, burstCount
        ));
        
        // Create timer for burst signals
        Timer signalTimer = new Timer();
        AtomicInteger signalsSent = new AtomicInteger(0);
        
        signalTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int currentSignal = signalsSent.incrementAndGet();
                
                // Send the UserControl value
                setValueOfUserControl(userControlIndex, value);
                
                // Progress callback
                if (onProgress != null) {
                    onProgress.run();
                }
                
                // Debug output (reduce spam by only showing key signals)
                if (currentSignal % 3 == 0 || currentSignal == 1 || currentSignal == burstCount) {
                    api.getHost().println(String.format(
                        "UserControlBurst: Signal %d/%d → UserControl%d (value: %d) [%s]",
                        currentSignal, burstCount, userControlIndex, value, description
                    ));
                }
                
                // Stop after sending all signals
                if (currentSignal >= burstCount) {
                    this.cancel();
                    signalTimer.cancel();
                    
                    // Completion callback
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    
                    // Final notification and logging
                    api.getHost().showPopupNotification(String.format(
                        "%s / UserControl%d Complete",
                        description, userControlIndex
                    ));
                    
                    api.getHost().println(String.format(
                        "UserControlBurst: Completed for UserControl%d (%d signals sent) [%s]",
                        userControlIndex, burstCount, description
                    ));
                }
            }
        }, 0, burstDelayMs); // Start immediately, repeat every delayMs
    }
    
    /**
     * Convenience method for simple burst sending without callbacks.
     * Uses the reusable burst method with null callbacks.
     */
    public void sendUserControlBurst(int userControlIndex, int value, int burstCount, int burstDelayMs, String description) {
        sendUserControlBurst(userControlIndex, value, burstCount, burstDelayMs, description, null, null);
    }
    
    /**
     * Sends ramped UserControl signals that simulate user interaction (like turning a knob).
     * Instead of sending the same value repeatedly, this method ramps up to the target value
     * to simulate how a user would gradually turn a control to the desired position.
     * This should trigger parameter changes that static bursts might not.
     * 
     * @param userControlIndex The UserControl index to send to (0-19)
     * @param targetValue The final target value (0-127)
     * @param rampSteps Number of steps to ramp up (uses Global Long Press Settings burst count)
     * @param burstDelayMs Delay between signals in milliseconds
     * @param description Description for logging and notifications
     */
    public void sendUserControlRampedBurst(int userControlIndex, int targetValue, int rampSteps, int burstDelayMs, String description) {
        // Validate parameters
        if (userControlIndex < 0 || userControlIndex >= 20) {
            api.getHost().println("ERROR: Invalid UserControl index: " + userControlIndex + " (must be 0-19)");
            return;
        }
        if (rampSteps <= 0 || rampSteps > 50) {
            api.getHost().println("ERROR: Invalid ramp steps: " + rampSteps + " (must be 1-50)");
            return;
        }
        if (burstDelayMs < 10 || burstDelayMs > 1000) {
            api.getHost().println("ERROR: Invalid burst delay: " + burstDelayMs + "ms (must be 10-1000ms)");
            return;
        }
        
        // Calculate ramp starting point
        // Start from a lower value and ramp up to target (minimum 5 steps below target)
        int startValue = Math.max(0, targetValue - Math.max(5, rampSteps - 1));
        int valueRange = targetValue - startValue;
        
        // Show start notification with ramp info
        api.getHost().showPopupNotification(String.format(
            "%s → UserControl%d (ramping %d→%d in %d steps)", 
            description, userControlIndex, startValue, targetValue, rampSteps
        ));
        
        // Create timer for ramped signals
        Timer rampTimer = new Timer();
        AtomicInteger stepsSent = new AtomicInteger(0);
        
        rampTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int currentStep = stepsSent.incrementAndGet();
                
                // Calculate current value based on ramp progress
                int currentValue;
                if (currentStep >= rampSteps) {
                    // Ensure final step is exactly the target value
                    currentValue = targetValue;
                } else {
                    // Linear interpolation from startValue to targetValue
                    double progress = (double) currentStep / rampSteps;
                    currentValue = (int) Math.round(startValue + (valueRange * progress));
                }
                
                // Send the ramped UserControl value
                setValueOfUserControl(userControlIndex, currentValue);
                
                // Debug output
                api.getHost().println(String.format(
                    "UserControlRamp: Step %d/%d → UserControl%d (value: %d) [%s]",
                    currentStep, rampSteps, userControlIndex, currentValue, description
                ));
                
                // Stop after sending all steps
                if (currentStep >= rampSteps) {
                    this.cancel();
                    rampTimer.cancel();
                    
                    // Final notification and logging
                    api.getHost().showPopupNotification(String.format(
                        "%s / UserControl%d Ramp Complete (final: %d)",
                        description, userControlIndex, targetValue
                    ));
                    
                    api.getHost().println(String.format(
                        "UserControlRamp: Completed for UserControl%d (%d steps, final value: %d) [%s]",
                        userControlIndex, rampSteps, targetValue, description
                    ));
                }
            }
        }, 0, burstDelayMs); // Start immediately, repeat every delayMs
    }
}
