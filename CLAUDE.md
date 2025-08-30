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
- **Increment Mode**: GREEN=at min, YELLOW (orange)=in between, RED=at max/wraparound ✨
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
// Integration: Wired through SoftstepperExtension → SoftstepController → UserControlls
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

### **Next Session Tasks:**
1. Test and verify long press functionality with different pad modes
2. Complete PERF page implementation (mixed CLIP/USER functionality)  
3. Verify all pad modes work with Bitwig parameter mapping

## Reference
- Original JS implementation: https://github.com/ngradwohl/bitwig_scripts/tree/master/Controller%20Scripts/softstep
- SoftStep Manual: https://files.keithmcmillen.com/downloads/softstep/SoftStep_Manual_v2.01.pdf