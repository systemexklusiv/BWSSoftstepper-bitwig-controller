# BWSSoftstepper Native Mode Implementation

## Project Overview
Java-based Bitwig Studio controller extension for Keith McMillen Softstep foot controller. **Native mode is now fully implemented** - the controller is configured entirely via SysEx from the script with no preset dependencies. **No preset file is needed.**

## Native Mode Implementation (COMPLETED)

### Current Features
- **No preset needed**: Controller configured entirely via SysEx from script ✅
- **Automatic setup**: No manual preset loading required ✅
- **Dynamic configuration**: Can change mappings on-the-fly during runtime ✅

### Previous Legacy Setup (No Longer Used)
- **Preset required**: `BWSSoftstepper.softsteppreset` had to be loaded on Softstep
- **Manual step**: User had to load preset via SoftStep Editor
- **Static configuration**: Button mappings were defined in preset

## Technical Implementation Details

### SysEx Initialization Commands (IMPLEMENTED)
```java
// 1. Standalone Mode
byte[] standaloneMode = {0xF0, 0x00, 0x1B, 0x48, 0x7A, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x09, 0x00, 0x0B, 0x2B, 0x3A, 0x00, 0x10, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2F, 0x7E, 0x00, 0x00, 0x00, 0x00, 0x02, 0xF7};

// 2. Backlight On
byte[] backlightOn = {0xF0, 0x00, 0x1B, 0x48, 0x7A, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x04, 0x00, 0x05, 0x08, 0x25, 0x01, 0x20, 0x00, 0x00, 0x7B, 0x2C, 0x00, 0x00, 0x00, 0x0C, 0xF7};

// 3. Tether Mode (if needed)
byte[] tetherMode = {0xF0, 0x00, 0x1B, 0x48, 0x7A, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x09, 0x00, 0x0B, 0x2B, 0x3A, 0x00, 0x10, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x50, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF7};
```

### MIDI Control Mapping
```java
// LED Control
CC40: Select LED (0-9)
CC41: Color (0=Green, 1=Red, 2=Yellow)  
CC42: Mode (0=Off, 1=On, 2=Blink, 3=Fast Blink, 4=Flash)

// Display Control
CC50-53: 4-character display text

// Button Input Addresses
int[] buttonAddresses = {44, 52, 60, 68, 76, 40, 48, 56, 64, 72};
// 4 pressure zones per button (0-127 values)
```

### Architecture Changes Required

1. **SoftstepHardware.java Updates**
   - Add SysEx initialization methods
   - Implement LED control via CC 40-42
   - Add 4-character display control via CC 50-53
   - Remove preset dependency

2. **Button Mapping Redesign**
   - Handle 10 buttons with pressure sensitivity
   - Map to current CLIP/USER mode functions
   - Dynamic button reconfiguration for mode switching

3. **LED State Management**
   - Real-time LED updates based on clip states
   - Color coding (Green=ready, Red=recording, Yellow=playing)
   - Blink patterns for different states

## Current Controller Layout

### CLIP Mode (default)
- Pads 1-4: Play/record clips (long press to delete)
- Pad 5: Track down, Pad 10: Track up
- Pads 8-9: Bank navigation  
- Pad 6: Mute track (long press stops clip)
- Pad 7: Arm track (long press deletes all clips in bank)
- Big nav control: Mode switching

### USER Mode
- All pads become freely assignable controls
- Supports both press and pressure sensitivity

## Benefits of Native Mode
- **User-friendly**: No manual preset loading
- **Dynamic**: Can change mappings during performance
- **Robust**: No dependency on external preset files
- **Integrated**: All configuration lives in Java code
- **Lower latency**: Direct MIDI communication
- **More reliable**: Fewer failure points

## Configurable Pad System (IN PROGRESS)

### **Current Implementation Status:**
✅ **Architecture Complete**: PadConfigurationManager and UserControlls integration
✅ **UI Framework**: Bitwig preferences with 10-pad configuration
✅ **Gesture Integration**: Proper press/release detection using existing Gestures system
❌ **Value Reading Issue**: SettableRangedValue returns normalized 0.0-1.0 instead of configured ranges

### **Configurable Pad Features:**
- **4 Control Modes**: Pressure, Momentary, Toggle, Increment
- **10 Individual Pads**: Each pad independently configurable
- **Per-Pad Settings**:
  - Mode selection (radio buttons)
  - Min Value (0-127 range)
  - Max Value (0-127 range) 
  - Step Size (context-sensitive):
    - **INCREMENT mode**: Integer 1-64 (how much each stomp increments)
    - **PRESSURE mode**: Double 0.1-10.0 (pressure sensitivity multiplier)
  - Inverted checkbox (inverts output values)
- **Project Persistence**: Settings saved with Bitwig projects automatically

### **Hardware LED Feedback Logic (Enhanced):**
- **Toggle Mode**: RED=ON state, YELLOW (orange)=OFF state ✨
- **Momentary Mode**: RED=pressed, GREEN=released
- **Pressure Mode**: RED when value > min, GREEN when value = min
- **Increment Mode**: GREEN=at min, YELLOW (orange)=safe to increment, RED=next step will wraparound ✨
- **Long Press**: Brief YELLOW flash when triggered
- **Centralized Constants**: All LED states defined in `Page.USER_LED_STATES` for consistency
- **Dynamic Assignment**: LEDs dynamically update based on pad mode and current state
- **No Software Feedback**: USER controls don't send updates back from software interactions

### **Technical Architecture:**

#### **PadConfigurationManager Class:**
```java
// Manages all preference UI and configuration storage
// Located: /src/main/java/de/davidrival/softstep/controller/PadConfigurationManager.java
// Features: 10-pad configuration with mode/min/max/step/inverted settings
// Integration: Wired through SoftstepperExtension → SoftstepController → UserControls
```

#### **Updated UserControlls Class:**
```java
// Uses Gestures system for proper press/release detection:
gestures.isFootOn()    // Clean press detection with debouncing
gestures.isFootOff()   // Clean release detection  
gestures.getPressure() // Continuous pressure values

// Processes each pad based on its individual configuration
// Handles: toggle state tracking, increment value tracking, value inversion
// Hardware feedback: Updates LED states based on control mode and values
```

### **Current Critical Issue:**
**Problem**: `SettableRangedValue.get()` returns normalized values (0.0-1.0) regardless of configured range (0-127)

**Debug Evidence**:
```
DEBUG: Raw preference doubles for Pad 0 - minDouble:0.0 maxDouble:1.0
DEBUG: Raw preference doubles for Pad 1 - minDouble:0.0 maxDouble:1.0
```

**Impact**: 
- Toggle mode sends value 1 instead of 127
- All ranges show 0-1 instead of configured 0-127
- Pressure and momentary modes unusable with proper ranges

### **Planned Solution: String Input Fields**
**Approach**: Replace `SettableRangedValue[]` with `SettableStringValue[]` for min/max/step settings

**Benefits**:
- Full control over value validation and parsing
- User-friendly error handling with popups for invalid inputs
- No normalization issues from Bitwig's SettableRangedValue
- Exact integer values as intended

**Implementation Plan**:
1. Replace numeric settings with string settings in PadConfigurationManager
2. Add custom parsing with validation (0-127 range enforcement)
3. Show error popups via `host.showPopupNotification()` for invalid inputs
4. Update value observers to parse strings instead of casting doubles

### **Files Modified for Configurable Pads:**
- `PadConfigurationManager.java` - New preference management class
- `UserControlls.java` - Updated to use configurations and Gestures system
- `SoftstepController.java` - Updated constructor to pass PadConfigurationManager
- `SoftstepperExtension.java` - Integration of PadConfigurationManager

## **CRITICAL BUG FIX: UserControl Mapping Issue**

### **Problem Identified:**
- **TOGGLE mode**: Showed as "CC54 (Ch. 1)" in Mapping Panel instead of "UserControl"
- **Mappings ineffective**: Bitwig didn't recognize controls as persistent UserControls
- **Index confusion**: UI showed "U5" but Mapping Panel showed "UserControl4"

### **Root Cause:**
- **TOGGLE/MOMENTARY modes**: Only sent values on press/release events
- **Bitwig interpretation**: Saw sporadic messages as MIDI CC instead of continuous UserControl
- **Missing persistence**: Controls didn't maintain their state between events

### **Solution Applied:**
- **TOGGLE mode**: Now continuously sends current toggle state (like PRESSURE mode)
- **MOMENTARY mode**: Now continuously sends current pressed/released state  
- **Consistent behavior**: All modes now send values continuously for proper UserControl mapping

### **Code Changes:**
```java
// BEFORE (problematic):
case TOGGLE:
    if (gestures.isFootOn()) {  // Only sent once on press!
        toggleStates[padIndex] = !toggleStates[padIndex];
        sendValue = true;
    }
    break;

// AFTER (fixed):
case TOGGLE:
    if (gestures.isFootOn()) {
        toggleStates[padIndex] = !toggleStates[padIndex];
    }
    outputValue = toggleStates[padIndex] ? config.max : config.min;
    sendValue = true;  // Always send current state
    break;
```

### **Expected Result:**
- **Mapping Panel**: Should now show "UserControl0", "UserControl1", etc.
- **Bitwig Integration**: Controls should work properly with mapped parameters
- **Index Consistency**: "U5" should correspond to "UserControl4" (0-based indexing)

## **NEW FEATURE: Separate UserControls for Long Press (IMPLEMENTED)**

### **UserControl Architecture:**
```
UserControl 0-9:   Normal pad operations (Pad 0-9)
UserControl 10-19: Long press operations (Pad 0-9 long press) 
UserControl 20:    Expression pedal (reserved for future)
Total: 21 UserControls
```

### **Long Press Configuration:**
- **Per-Pad Enable**: "Long Press Enabled" checkbox per pad in Bitwig preferences
- **Per-Pad Value**: "Long Press Value" field (0-127) - no range/inversion scaling
- **Separate Mapping**: Each long press maps to its own UserControl (pad + 10)
- **No Conflicts**: Long press and normal operation are completely independent
- **Works with All Modes**: Pressure, momentary, toggle, and increment all work perfectly

### **Use Cases:**
```
Example 1 - Filter Cutoff with Reset:
- Pad 0 Normal (UserControl0): Pressure mode 0-127 → Filter Cutoff
- Pad 0 Long Press (UserControl10): Value 64 → Reset to middle

Example 2 - Volume with Bypass:
- Pad 1 Normal (UserControl1): Toggle mode → Volume On/Off  
- Pad 1 Long Press (UserControl11): Value 0 → Instant Mute

Example 3 - Multiple Effect Controls:
- Pad 2 Normal (UserControl2): Pressure mode → Effect Send Amount
- Pad 2 Long Press (UserControl12): Value 0 → Effect Bypass
```

### **Bitwig Integration:**
- **Mapping Panel**: Shows "UserControl0-9" for pads, "UserControl10-19" for long press
- **Independent Mapping**: Each can map to different software parameters  
- **1-to-Many Support**: Each UserControl supports multiple parameter mappings
- **Project Persistence**: All mappings save with project files

### **Implementation Details:**
- **Preference UI**: "Long Press Enabled" checkbox + "Long Press Value" field per pad
- **Priority Processing**: Long press actions processed before normal pad operations
- **Hardware Feedback**: Brief yellow FLASH, then returns to normal LED state
- **No Timer Complexity**: Separate UserControls eliminate data conflicts
- **Debug Logging**: Shows long press triggers with target UserControl index

## **NEW FEATURE: Studio I/O Panel Long Press Testing (IMPLEMENTED)**

### **Studio I/O Panel Integration:**
- **Location**: Appears at top of Bitwig's main window when controller is active
- **Purpose**: Easy mapping of long press UserControls without hardware gestures
- **UI Elements**: 10 "Assign Longpress 0-9" trigger buttons (one per pad)
- **Persistent**: Always visible when controller is connected

### **How It Works:**
```
1. User clicks "Assign Longpress N" button in Studio I/O Panel
2. Sends 15 rapid signals (50ms apart) to long press UserControl (UserControl10-19)
3. Bitwig's mapping mode detects the continuous signal stream
4. User can now map this UserControl to any Bitwig parameter
5. Real hardware long press gestures also trigger the same UserControl
```

### **Benefits:**
✅ **No Hardware Required**: Map long press functions without physical gestures  
✅ **No Conflicts**: Test triggers don't interfere with normal pad operation  
✅ **Instant Access**: Always visible in Studio I/O Panel at top of main window  
✅ **Clear Feedback**: Popup notifications show progress and completion  
✅ **Configuration Aware**: Only works if long press is enabled for that pad  
✅ **Mapping Detection**: Signal burst ensures Bitwig recognizes UserControls for mapping  

### **Technical Implementation:**
- **StudioIOPanelManager Class**: Separate class for Studio I/O Panel management
- **DocumentState Integration**: Uses Bitwig's DocumentState API for persistent UI
- **API Integration**: Leverages existing ApiManager for UserControl sending
- **Configuration Integration**: Respects PadConfigurationManager settings

### **User Workflow:**
```
1. Configure pad in Bitwig preferences (enable long press, set value)
2. Click "Assign Longpress N" button in Studio I/O Panel
3. Right-click Bitwig parameter → "Map to UserControl10-19"
4. Hardware long press now controls the mapped parameter
```

## **CONFIGURABLE BURST SYSTEM FOR LONG PRESS (COMPLETED)**

### **Implementation Summary:**
✅ **Global Burst Settings**: User-configurable burst count (1-50) and delay (10-1000ms) in Bitwig preferences  
✅ **Unified Burst Method**: Centralized burst logic in ApiControllerToHost with validation and callbacks  
✅ **Hardware Integration**: Hardware long press now uses configurable burst sending  
✅ **Studio I/O Panel Integration**: UI triggers use same global settings for consistency  
✅ **Code Cleanup**: Removed duplicate burst logic, improved maintainability  

### **Benefits:**
- **Consistency**: Hardware and UI triggers behave identically
- **User Control**: Configurable parameters for different mapping needs  
- **Reliability**: Better UserControl mapping detection with consistent burst patterns
- **Maintainability**: Single burst implementation reduces code duplication

---

## **TECHNICAL IMPLEMENTATION GUIDE: STUDIO I/O PANEL & PREFERENCES**

### **Studio I/O Panel Implementation:**

#### **1. Basic Setup (StudioIOPanelManager.java):**
```java
// Use DocumentState API for persistent UI controls
private final DocumentState documentState;
private final SettableEnumValue[] longPressTestButtons;

// In constructor:
this.documentState = host.getDocumentState();

// Create controls that appear in Studio I/O Panel
longPressTestButtons[i] = documentState.getEnumSetting(
    "Assign Longpress " + padIndex,     // Control name (appears as button)
    "Pad " + padIndex,                  // Category name  
    new String[]{IDLE_VALUE, TRIGGER_VALUE},  // Options (must have 2+)
    IDLE_VALUE                          // Default value
);
```

#### **2. Observer Pattern with Initialization Safety:**
```java
private final boolean[] initializationComplete;

// Prevent startup triggers
longPressTestButtons[i].addValueObserver(value -> {
    if (TRIGGER_VALUE.equals(value) && initializationComplete[padIndex]) {
        triggerLongPressUserControl(padIndex);
        
        // Auto-reset to idle after trigger
        new Timer().schedule(new TimerTask() {
            public void run() {
                longPressTestButtons[padIndex].set(IDLE_VALUE);
            }
        }, 100);
    }
});

// Mark as ready after delay (prevents startup bursts)
new Timer().schedule(new TimerTask() {
    public void run() {
        initializationComplete[padIndex] = true;
    }
}, 1000);
```

#### **3. Signal Burst for Mapping Detection:**
```java
// Use centralized burst method with global settings
apiManager.getApiToHost().sendUserControlBurst(
    userControlIndex, 
    value, 
    padConfigManager.getBurstCount(),
    padConfigManager.getBurstDelayMs(),
    "Studio I/O Panel Pad " + padIndex
);
```

### **Preferences Implementation (PadConfigurationManager.java):**

#### **1. String-Based Settings (Critical for Value Control):**
```java
// NEVER use SettableRangedValue - it normalizes to 0.0-1.0!
// Always use SettableStringValue for precise integer control
this.padMinSettings = new SettableStringValue[NUM_PADS];
this.padMaxSettings = new SettableStringValue[NUM_PADS];

// Setup with validation
padMinSettings[i] = preferences.getStringSetting(
    "Min Value",        // Setting name
    "Pad " + (i + 1),  // Category (use 1-based for user display)
    8,                  // Max length
    "0"                // Default value as string
);
```

#### **2. Custom Parsing with User-Friendly Validation:**
```java
private int parseIntegerValue(String value, int min, int max, int defaultValue, String fieldName, int padIndex) {
    try {
        int parsedValue = Integer.parseInt(value.trim());
        if (parsedValue < min || parsedValue > max) {
            // Show popup notification for user feedback
            host.showPopupNotification("Pad " + (padIndex + 1) + " " + fieldName + 
                " must be between " + min + " and " + max + ". Using default: " + defaultValue);
            return defaultValue;
        }
        return parsedValue;
    } catch (NumberFormatException e) {
        host.showPopupNotification("Pad " + (padIndex + 1) + " " + fieldName + 
            " invalid format: '" + value + "'. Using default: " + defaultValue);
        return defaultValue;
    }
}
```

#### **3. Observer Pattern for Live Updates:**
```java
// Mark all settings as interested
padMinSettings[i].markInterested();

// Add observers for live configuration updates
padMinSettings[i].addValueObserver(value -> {
    updateConfigFromSettings(padIndex);
    host.println("Pad " + (padIndex + 1) + " min value changed to: '" + value + "'");
});
```

#### **4. Global Settings Organization:**
```java
// Create separate category for global settings
Preferences preferences = host.getPreferences();
this.burstCountSetting = preferences.getStringSetting(
    "Burst Count",                        // Setting name
    "Global Long Press Settings",         // Category name (groups related settings)
    8,                                   // Max length
    "15"                                 // Default value
);
```

### **CRITICAL PITFALLS & SOLUTIONS:**

#### **🚨 PITFALL 1: SettableRangedValue Normalization**
**Problem**: `SettableRangedValue.get()` always returns 0.0-1.0, regardless of configured range
```java
// WRONG - Always returns normalized values!
SettableRangedValue rangeSetting = preferences.getRangedSetting("Value", "Pad", 0, 127, 0.1, "", 64);
double value = rangeSetting.get(); // Always 0.0-1.0!
```
**Solution**: Use `SettableStringValue` with custom parsing
```java
// CORRECT - Full control over values
SettableStringValue stringSetting = preferences.getStringSetting("Value", "Pad", 8, "64");
int value = parseIntegerValue(stringSetting.get(), 0, 127, 64, "Value", padIndex);
```

#### **🚨 PITFALL 2: UserControl Mapping Detection**
**Problem**: Single UserControl events show as "CC54 (Ch. 1)" instead of "UserControl" in mapping
**Solution**: Send continuous/burst signals for proper detection
```java
// WRONG - Single event not recognized as UserControl
apiManager.getApiToHost().setValueOfUserControl(index, value);

// CORRECT - Burst pattern ensures UserControl recognition
apiManager.getApiToHost().sendUserControlBurst(index, value, burstCount, delayMs, description);
```

#### **🚨 PITFALL 3: Enum Settings Red Warning**
**Problem**: Single-option enum causes Bitwig console warnings
```java
// WRONG - Causes "enum settings should have at least two options" warning
new String[]{"Trigger"}

// CORRECT - Always provide 2+ options
new String[]{"Ready", "Trigger"}
```

#### **🚨 PITFALL 4: Startup Observer Triggers**
**Problem**: Observers fire during initialization, causing unwanted startup bursts
**Solution**: Implement initialization delay mechanism
```java
private final boolean[] initializationComplete = new boolean[NUM_PADS];

// In observer:
if (TRIGGER_VALUE.equals(value) && initializationComplete[padIndex]) {
    // Only trigger after initialization complete
}

// Set ready after delay
new Timer().schedule(new TimerTask() {
    public void run() { initializationComplete[padIndex] = true; }
}, 1000);
```

#### **🚨 PITFALL 5: UserControl Index Conflicts**
**Problem**: Expression pedal or other reserved UserControls cause index mismatches
**Solution**: Carefully manage UserControl allocation
```java
// Document UserControl allocation clearly:
// UserControl 0-9:   Normal pad operations
// UserControl 10-19: Long press operations  
// UserControl 20+:   Reserved for future expansion

// Ensure total count matches usage
public static final int AMOUNT_USER_CONTROLS = 20; // 10 pads + 10 longpress
```

### **BEST PRACTICES:**

1. **Always Use String Settings** for numeric values requiring precision
2. **Implement Custom Validation** with user-friendly error messages
3. **Use Burst Patterns** for reliable UserControl mapping detection
4. **Document UserControl Allocation** to prevent index conflicts
5. **Implement Initialization Delays** to prevent startup side effects
6. **Group Related Settings** using category names for better UX
7. **Provide Real-time Feedback** through console logging and popup notifications

## **CRITICAL BUG: Studio I/O Panel Trigger Ineffective After Mapping**

### **Problem Description:**
- **Mapping Phase Works**: "Assign Longpress N" buttons successfully trigger burst signals for UserControl mapping
- **Mapping Establishes**: Bitwig shows mapped connection (e.g., "UserControl/Control10 mapped to Track1 Volume")
- **Post-Mapping Issue**: Same trigger button no longer affects the mapped parameter
- **Console Evidence**: Burst signals are still being sent and logged correctly
- **Parameter Unchanged**: Track volume (or other mapped parameter) doesn't respond to triggers

### **Technical Analysis:**
```
✅ Burst signal generation: Working (console confirms)
✅ UserControl index calculation: Correct (UserControl10-19)
✅ Value transmission: Working (apiManager.getApiToHost().sendUserControlBurst())
❌ Parameter response: Bitwig mapped parameter doesn't change
```

### **Potential Root Causes:**
1. **Burst vs Single Value Issue**: Mapping phase needs bursts, but parameter control might need single values
2. **Value Range Mismatch**: Burst sends raw 0-127 values, but Bitwig expects normalized 0.0-1.0 values
3. **UserControl State Confusion**: Bitwig might not recognize burst patterns as valid parameter changes
4. **API Method Mismatch**: `sendUserControlBurst()` vs `setValueOfUserControl()` behavior differences

### **Current Workaround:**
- **"Trigger Once N" buttons**: These use `setValueOfUserControl()` instead of burst - test if these work for parameter control
- **Hardware Long Press**: Test if actual hardware long press gestures work correctly after mapping

### **Investigation Plan:**
1. Test "Trigger Once N" buttons after mapping to see if single values work
2. Compare burst method vs single value method effectiveness
3. Check if hardware long press works correctly after Studio I/O mapping
4. Investigate if value normalization is needed for parameter control

### **SOLUTION ATTEMPT: Ramped User Interaction Simulation**

**Implementation Date**: Current Session
**Hypothesis**: Bitwig expects **value changes** rather than **repeated identical values** for parameter updates

#### **New API Method Added:**
```java
// ApiControllerToHost.sendUserControlRampedBurst()
// Simulates turning a knob by sending gradually increasing values
// Example: For target value 33 with 5 ramp steps → sends 29,30,31,32,33
```

#### **New Studio I/O Panel Button:**
- **"Ramp Test N" buttons** - Third button per pad for ramped value testing
- Uses same configuration (long press enabled, target value, global burst settings)
- Sends ramped sequence instead of repeated identical values

#### **Technical Implementation:**
```java
// Calculates starting value (minimum 5 steps below target)
int startValue = Math.max(0, targetValue - Math.max(5, rampSteps - 1));

// Linear interpolation for smooth progression
double progress = (double) currentStep / rampSteps;
int currentValue = startValue + (valueRange * progress);
```

#### **Studio I/O Panel Layout (Updated):**
Each pad now has **3 test buttons**:
1. **"Assign Longpress N"** - Static burst for mapping detection (unchanged)
2. **"Trigger Once N"** - Single value for basic testing
3. **"Ramp Test N"** - Ramped values simulating user interaction ✨ **NEW**

#### **Expected Behavior:**
1. Map long press using "Assign Longpress" (existing functionality)
2. Test parameter control using "Ramp Test" button
3. Ramped values should trigger mapped parameter changes successfully

#### **Files Modified:**
- `ApiControllerToHost.java` - Added `sendUserControlRampedBurst()` method
- `StudioIOPanelManager.java` - Added ramp test buttons and `triggerRampedUserControl()` method

#### **Testing Status:**
✅ **TESTED** - Ramping works but revealed deeper issue

#### **CRITICAL DISCOVERY: Takeover Mode Behavior**

**Date**: Current Session  
**Finding**: UserControls exhibit takeover mode behavior when mapped to Bitwig parameters

**Evidence**:
1. **Fader at Max (127)** + Ramp Test to value 11 → **NO EFFECT** (values 0-11 never cross 127)
2. **Fader at Min (0)** + Ramp Test to value 11 → **WORKS** (values cross 0 and continue upward)  
3. **Fader at 99** + Target 127 → **WORKS** (values cross 99 and continue to 127)

**Log Analysis**:
```
Pad 0 Long Press Value: 11, Burst Count: 30
Ramp sequence: 0→1→1→1→2→2→3→3→3→4→4→4→5→5→6→6→6→7→7→7→8→8→8→9→9→10→10→10→11→11
Result: Only works when ramp crosses current fader position
```

**Root Cause**: 
- **Takeover Mode Active**: UserControls require value crossover before taking control
- **Parameter Protection**: Prevents accidental jumps by requiring smooth transitions
- **Crossover Threshold**: Values only affect parameter after crossing current position

#### **SOLUTION RESEARCH: Disable Takeover Mode**

**Potential API Solutions**:
1. **RemoteControl.setIndication(true)** - May disable takeover for specific controls
2. **Parameter.setIndication(false)** - Could force immediate control without takeover
3. **UserControl specific settings** - Check if UserControls have takeover disable options
4. **ControllerHost preferences** - Global takeover mode settings

**Alternative Approaches**:
1. **Smart Ramping**: Always start ramp from current parameter value (query before sending)
2. **Crossover Ramp**: Send values from 0→127 to guarantee crossover regardless of current position
3. **Bidirectional Ramp**: Ramp both directions to ensure crossover (current±range)

**SOLUTION FOUND: DrivenByMoss Research Results**

**Key Findings from DrivenByMoss Framework:**

1. **Parameter.setImmediately()**: Bypasses takeover mode completely
   ```java
   parameter.setImmediately(normalizedValue); // 0.0-1.0 range
   ```

2. **AbsoluteHardwareControl.disableTakeOver()**: For hardware controls
   ```java
   // Used by APC, MCU, Maschine JAM controllers
   hardwareControl.disableTakeOver();
   ```

3. **Usage Patterns in DrivenByMoss:**
   - **Launchpad UserView**: `parameter.setValueImmediatly(value)` 
   - **APC Controllers**: Disable takeover for LED feedback knobs
   - **MCU Controllers**: Disable takeover for motor faders
   - **Initialization Pattern**: Set initial values to prevent catch mode

**Final Hybrid Solution Implemented:**

#### **Two-Method Approach:**
1. **`setValueOfUserControl()`** - Uses `parameter.set()` for UserControl mapping identity
2. **`setValueOfUserControlImmediately()`** - Uses `parameter.setImmediately()` for takeover bypass

#### **Method Usage:**
- **UserControl Mapping**: Keep `setValueOfUserControl()` to maintain "UserControl0" identity
- **Parameter Control**: Use `setValueOfUserControlImmediately()` to bypass takeover mode
- **Studio I/O Panel**: "Assign Longpress" uses mapping method, test buttons can use immediate method

#### **Critical Discovery:**
- **`setImmediately()` breaks UserControl identity**: Shows as "CC46 (Ch. 1)" instead of "UserControl0"  
- **Mapping vs Control phases**: Different methods needed for different purposes
- **Foot controller rationale**: Takeover mode inappropriate for foot-operated controls

#### **User Configuration Required:**
**IMPORTANT**: In Bitwig Studio → Settings → Controllers → [Controller Name] → Takeover Mode:
- Change from **"Catch"** to **"Immediately"** for best user experience
- This eliminates the need for value crossover on foot-operated controls
- Provides immediate response without requiring precise foot positioning

#### **Current Status:**
✅ **UserControl Mapping**: Works correctly (shows as "UserControl0", "UserControl1", etc.)  
✅ **Long Press Mapping**: Studio I/O Panel "Assign Longpress" buttons work for mapping  
✅ **Takeover Solution**: User can set global takeover mode to "Immediately" in Bitwig settings  
✅ **Ramped Burst System**: Successfully implemented for user interaction simulation as fallback

### **Session Summary: Takeover Mode Investigation & Solution**

**Major Accomplishments:**
1. ✅ **Identified takeover mode as root cause** of Studio I/O Panel trigger failures
2. ✅ **Researched DrivenByMoss framework** for professional controller solutions  
3. ✅ **Implemented ramped burst system** to simulate realistic user interaction
4. ✅ **Added third Studio I/O Panel button** ("Ramp Test") for ramped value testing
5. ✅ **Discovered `parameter.setImmediately()`** method that bypasses takeover completely
6. ✅ **Solved UserControl identity issue** by maintaining hybrid approach
7. ✅ **Documented complete solution** including user configuration requirements

**Key Technical Insights:**
- Takeover mode requires value crossover - inappropriate for foot controllers
- `setImmediately()` breaks UserControl mapping identity (shows as CC46)
- Two-method approach needed: mapping vs parameter control
- User can disable takeover globally in Bitwig controller settings

**Files Modified:**
- `ApiControllerToHost.java`: Added `setValueOfUserControlImmediately()` method
- `StudioIOPanelManager.java`: Added "Ramp Test" buttons with ramped burst functionality

## **FUTURE PLAN: Animated Control Movement via Ramps**

### **Concept: Per-Control-Type Ramping Animation**
**Vision**: Smooth animated transitions for pad control changes instead of instant jumps

### **Implementation Strategy:**
**Core Method**: Leverage existing `sendUserControlRampedBurst()` architecture from `ApiControllerToHost.java`

### **Per-Pad-Mode Ramp Settings:**
Each control mode gets individual ramp configuration in Bitwig preferences:

#### **1. Toggle Ramp Settings:**
- **"Toggle Ramp Enabled"** (checkbox) 
- **"Toggle Ramp Time"** (string field, parsed to milliseconds)
- **Effect**: Animate from current state to new toggle state over ramp time

#### **2. Increment Ramp Settings:**
- **"Increment Ramp Enabled"** (checkbox)
- **"Increment Ramp Time"** (string field, parsed to milliseconds) 
- **Effect**: Animate from last value to next increment over ramp time

#### **3. Momentary Ramp Settings:**
- **"Momentary Ramp Enabled"** (checkbox)
- **"Momentary Ramp Time"** (string field, parsed to milliseconds)
- **Effect**: Animate press/release transitions over ramp time

### **Example User Experience:**
```
User Setting: Increment Ramp Time = "500"
Current Value: 64
User Action: Stomp Pad (increment by step size 10)
Target Value: 74
Animation: Smooth ramp 64→65→66→67→68→69→70→71→72→73→74 over 500ms
```

### **Technical Implementation Notes:**
- **Reuse ramping infrastructure**: `sendUserControlRampedBurst()` method already handles timing and interpolation
- **Remove global settings**: Eliminate Global Long Press Settings, use per-control-type settings instead
- **Studio I/O Panel cleanup**: 
  - **Keep**: "Assign Longpress N" buttons (still needed for long press UserControl mapping)
  - **Remove**: "Trigger Once N" and "Ramp Test N" buttons (no longer needed with user takeover setting)
- **Keep core ramping code**: `sendUserControlRampedBurst()` becomes foundation for animated control movement

### **RAMPED BURST ARCHITECTURE (Preserved for Future Animation System)**

#### **Core Method: `ApiControllerToHost.sendUserControlRampedBurst()`**
```java
public void sendUserControlRampedBurst(int userControlIndex, int targetValue, 
                                     int rampSteps, int burstDelayMs, String description)
```

#### **Key Features:**
- **Smart Value Calculation**: Automatically calculates starting value (minimum 5 steps below target)
- **Linear Interpolation**: Smooth progression using `double progress = (double) currentStep / rampSteps`
- **Timer-Based Execution**: Uses `Timer.scheduleAtFixedRate()` for precise timing control
- **Value Normalization**: Handles 0-127 to 0.0-1.0 conversion internally
- **Debug Integration**: Comprehensive logging and notifications
- **Parameter Validation**: Input validation for userControlIndex, rampSteps, and delayMs

#### **Animation Logic:**
```java
// Calculate ramp starting point (minimum 5 steps below target)
int startValue = Math.max(0, targetValue - Math.max(5, rampSteps - 1));

// Linear interpolation for smooth progression  
double progress = (double) currentStep / rampSteps;
int currentValue = startValue + (valueRange * progress);

// Send using setValueOfUserControl() for proper UserControl identity
setValueOfUserControl(userControlIndex, currentValue);
```

#### **Future Adaptation for Animated Controls:**
- **Toggle Animation**: Ramp from current toggle state to new state over configured time
- **Increment Animation**: Ramp from current value to current+stepSize over configured time  
- **Momentary Animation**: Ramp press/release transitions over configured time
- **Per-Mode Timing**: Each control type gets individual ramp time configuration

### **Next Session Tasks:**
1. Clean up Studio I/O Panel (remove "Trigger Once" and "Ramp Test" buttons only)
2. Remove Global Long Press Settings from preferences  
3. Complete PERF page implementation (mixed CLIP/USER functionality)  
4. Plan animated ramp settings implementation for future session

## **BASEPERFORMANCEMODECONTROLS ARCHITECTURE REFACTORING (COMPLETED)**

### **Problem Solved:**
The original PERF and PERF2 implementations had severe architectural issues:
- **Pad Assignment Conflicts**: Perf2Controls had `REC_ARM_PAD = 0` conflicting with `ARM_PAD = 6`
- **Code Duplication**: ~70% duplicate code between PERF/PERF2 upper row functionality
- **Inconsistent Layouts**: PERF2 pad assignments were completely wrong
- **LED Update Issues**: Inconsistent LED handling approaches

### **Hardware Layout (CRITICAL):**
**Lower Row (Pads 0-4):** Bottom row on physical controller
**Upper Row (Pads 5-9):** Top row on physical controller

### **Solution: BasePerformanceModeControls Architecture**

#### **BasePerformanceModeControls** (Abstract Base Class)
```java
// Common functionality for both PERF and PERF2 modes
// Handles upper row (pads 5-9) + BWS track cycle (pad 4)
// Location: /src/main/java/de/davidrival/softstep/controller/BasePerformanceModeControls.java

// Common Upper Row Assignments:
protected static final int TRACK_CYCLE_PAD = 4;  // BWS track cycling (lower row)
protected static final int MUTE_PAD = 5;          // Mute/stop clip (CLIP functionality)
protected static final int ARM_PAD = 6;           // Record arm/delete all (CLIP functionality) 
// Pads 7-9: User controls (USER functionality)
```

#### **PerfControls** (extends BasePerformanceModeControls)
```java
// PERF Mode Layout:
// Lower Row (0-4): Standard clip slots 0-3 + BWS cycle (4)
// Upper Row (5-9): Inherited from base class (mute/arm/user)

@Override
protected void processModeSpecificPads(List<Softstep1Pad> modeSpecificPads, ShortMidiMessage msg) {
    // Pads 0-3 → ClipControls (standard clip functionality)
    clipControls.processControlls(modeSpecificPads, msg);
}
```

#### **Perf2Controls** (extends BasePerformanceModeControls)
```java
// PERF2 Mode Layout:
// Lower Row (0-4): Focused clip (0) + Smart assistant (1) + User (2-3) + BWS cycle (4)
// Upper Row (5-9): Inherited from base class (mute/arm/user)

private static final int FOCUSED_CLIP_PAD = 0;        // Focused clip slot at cursor position
private static final int SMART_ASSISTANT_PAD = 1;     // Smart recording assistant
// Pads 2-3: USER mode (handled by overridden isUserPad method)

@Override
protected boolean isUserPad(int padIndex) {
    // Include pads 2-3 (lower row) in addition to 7-9 (upper row)
    return (padIndex == 2 || padIndex == 3) || super.isUserPad(padIndex);
}
```

### **Final Corrected Layouts:**

#### **PERF Mode:**
| Lower Row | Function | Type |
|-----------|----------|---------|
| 0-3 | Clip Slots 0-3 | CLIP mode |
| 4 | BWS Track Cycle | Base class |

| Upper Row | Function | Type |
|-----------|----------|---------|
| 5 | Mute/Stop Clip | CLIP mode |
| 6 | Record Arm/Delete All | CLIP mode |
| 7-9 | User Controls | USER mode |

#### **PERF2 Mode:**
| Lower Row | Function | Type |
|-----------|----------|---------|
| 0 | Focused Clip Slot | PERF2-specific |
| 1 | Smart Recording Assistant | PERF2-specific |
| 2-3 | User Controls | USER mode |
| 4 | BWS Track Cycle | Base class |

| Upper Row | Function | Type |
|-----------|----------|---------|
| 5 | Mute/Stop Clip | CLIP mode |
| 6 | **Record Arm/Delete All** | CLIP mode ✨ |
| 7-9 | User Controls | USER mode |

### **Key Benefits Achieved:**
✅ **No More Conflicts**: Pad 6 consistently provides CLIP arm/delete functionality in both modes
✅ **Code Reuse**: ~70% reduction in duplicate code between PERF/PERF2
✅ **Consistency**: Upper row (5-9) identical between modes
✅ **Maintainability**: Single source of truth for common functionality
✅ **Correct Assignments**: PERF2 now has proper pad layout (focused clip on 0, smart assistant on 1)
✅ **LED Unification**: Consistent LED handling approach may resolve update issues

### **Files Created/Modified:**
- **NEW**: `BasePerformanceModeControls.java` - Abstract base class with common functionality
- **REFACTORED**: `PerfControls.java` - Now extends base class, minimal code
- **REFACTORED**: `Perf2Controls.java` - Now extends base class, correct pad assignments

## **STUDIO I/O PANEL RESEARCH & CAPABILITIES**

### **Advanced UI Elements Available (DocumentState API)**

#### **Core UI Element Types:**
- **SettableEnumValue**: Dropdowns/button groups for mode selection
- **SettableBooleanValue**: Checkboxes/toggle buttons for on/off controls  
- **SettableRangedValue**: Sliders/number fields with custom ranges and units
- **SettableStringValue**: Text input fields with character limits
- **SettableColorValue**: RGB color picker interface (API v5+)
- **Signal**: Push buttons for one-time actions

#### **Advanced Use Cases for SoftStep Controller:**

**1. Virtual Hardware Interface**
```java
// Virtual faders replicating hardware functionality
SettableRangedValue virtualPadValue = documentState.getNumberSetting(
    "Virtual Pad " + i + " Value", "Live Control", 
    0.0, 127.0, 1.0, "", 0.0);

// Color coding to match hardware LED states
SettableColorValue padColor = documentState.getColorSetting(
    "Pad " + i + " Color", "Visual Feedback", Color.GREEN);
```

**2. Live Configuration Center**
```java
// Real-time pad mode switching without preferences
SettableEnumValue liveMode = documentState.getEnumSetting(
    "Live Pad " + i + " Mode", "Runtime Config",
    new String[]{"Pressure", "Toggle", "Momentary", "Increment"}, 
    "Pressure");
```

**3. Performance & Debug Interface**
- Signal monitors showing MIDI I/O
- State displays for toggle/increment values
- Hardware calibration controls
- Performance metrics (latency, message rates)

#### **Potential SoftStep Panel Features:**
1. **Virtual Pressure Faders**: Mouse control instead of foot pressure
2. **LED State Simulator**: Visual representation of hardware LED states
3. **Live Pad Configuration**: Change modes without going to preferences
4. **Preset Switcher**: Quick switching between pad configurations
5. **Performance Monitor**: Real-time display of pad values and states
6. **Hardware Calibration**: Live adjustment of sensitivity and ranges

#### **Best Practices Learned:**
- **Initialization Safety**: Use delays to prevent startup triggers
- **Auto-Reset Buttons**: Timer-based reset for trigger buttons
- **Category Organization**: Group related controls logically
- **Bidirectional Sync**: Keep UI and hardware/software in sync
- **Value Validation**: Validate inputs with user-friendly error messages

#### **Implementation Patterns:**
```java
// Initialization safety pattern
private boolean initializationComplete = false;
setting.addValueObserver(value -> {
    if (initializationComplete) { /* handle change */ }
});

// Auto-reset trigger pattern
button.addValueObserver(pressed -> {
    if (pressed && initializationComplete) {
        performAction();
        host.scheduleTask(() -> button.set(false), 100);
    }
});
```

## Reference
- **Original JS implementation**: https://github.com/ngradwohl/bitwig_scripts/tree/master/Controller%20Scripts/softstep
- **SoftStep Manual**: https://files.keithmcmillen.com/downloads/softstep/SoftStep_Manual_v2.01.pdf
- **DrivenByMoss Framework**: https://github.com/git-moss/DrivenByMoss (Advanced Studio I/O Panel examples)
- **BitwigSnapshotManager**: OSC settings and snapshot management via Studio I/O Panel
- **Push Controller Configuration**: Comprehensive hardware parameter control examples
- **ISettingsUI Interface**: Abstraction layer for advanced UI element management
- **Bitwig Controller API**: DocumentState vs Preferences for project-specific vs global settings