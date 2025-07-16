# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Bitwig Studio Controller Extension** that provides advanced project control features including:

1. **Clip Launcher Control**: a lane of 4 slots where loops can be recorded, erased, stopped and started 
2. **Selected Track navigation**: selecting current track by footcotroller, mute/unmute the current track

The project is built with Java 21 and the Bitwig Controller API v18, using both Maven and Gradle build systems.

## Build Systems

The project supports **dual build systems**:

### Maven (Primary)
- Build: `mvn clean compile` 
- Package and deploy: `mvn package install`
- The Maven build automatically copies the generated `.bwextension` to the Bitwig Extensions folder

### Gradle (Alternative)
- Build: `./gradlew build`
- Deploy: `./gradlew copyBwextension` (includes automatic deployment to Bitwig)
- The Gradle build includes tasks for both copying the extension and deploying to Bitwig

### Quick Build Scripts
- **Unix/macOS**: `./build.sh` - runs Maven clean compile
- **Windows**: `build.bat` - runs Maven clean compile

## Architecture

### Core Classes

1. **SysexProjectControlExtension** (`src/main/java/de/systemexklusiv/sysexprojectcontrol/`)
   - Main extension class extending ControllerExtension
   - Handles MIDI input/output and coordinates between controllers and cue point management
   - Routes CC messages to CustomRemoteControlsPage and Note messages to CuePointsManager

2. **CuePointsManager** (`src/main/java/de/systemexklusiv/sysexprojectcontrol/cuepoints/`)
   - Complete cue point navigation system
   - Handles 128 cue markers with observers for position/name changes
   - Implements looping functionality (between cues, around playhead, time-signature aware)

3. **CustomRemoteControlsPage** (`src/main/java/de/systemexklusiv/sysexprojectcontrol/controller/`)
   - Manages remote control mappings to project-wide parameters
   - Currently configured for CC 35-42 (Bank 1) and CC 44-51 (Bank 2) on MIDI channel 14

### MIDI Configuration

- **CC Channel**: 14 (configurable via `FIRST_BYTE = 190` in SysexProjectControlExtension)
- **Note Messages**: Any MIDI channel for cue point navigation (Notes 0-127)
- **SysEx**: MMC transport commands

### Key Constants

- `PARAMTERS_SIZE = 8`: Number of parameters per remote control page
- `CUE_MARKER_BANK_SIZE = 128`: Maximum number of cue markers tracked
- Debug logging can be controlled via `DEBUG = true` in SysexProjectControlExtension

### User Configuration

The extension provides comprehensive user-configurable settings through Bitwig's Preferences panel:

#### Global Settings
- **Number of Pages** (1-8): Configure how many remote control pages to create

#### Per-Page Settings
For each page (1-8), users can configure:
- **MIDI Channel** (1-16): Which MIDI channel this page listens on
- **Start CC** (0-120): Starting CC number for the 8 controls on this page

#### Default Configuration
- **Page 1**: Channel 15, Start CC 35 (CCs 35-42)
- **Page 2**: Channel 1, Start CC 44 (CCs 44-51)
- **Additional pages**: Channel 1, Start CC at multiples of 8

#### Notes
- All messages are Control Change (CC) type
- Each page has 8 controls (PARAMTERS_SIZE = 8)
- Status bytes are automatically calculated as 0xB0 + (channel - 1)
- Changes take effect immediately without restart
- Settings are accessible in Bitwig Studio under Controller Preferences

### Cue Point MIDI Mapping Configuration

The extension provides configurable MIDI note mappings for all cue point functions:

#### Configurable Functions
- **Launch Current Cue** (default: Note 0)
- **Jump to Previous Cue** (default: Note 1) 
- **Jump to Next Cue** (default: Note 2)
- **Jump to Previous Cue and Launch** (default: Note 3)
- **Jump to Next Cue and Launch** (default: Note 4)
- **Loop Between Surrounding Cues** (default: Note 5)
- **Loop Around Playhead (Bar)** (default: Note 6)
- **Loop Around Playhead (Last Half)** (default: Note 7)
- **Loop Around Playhead (Quarter)** (default: Note 8)
- **Loop Around Playhead (First Half)** (default: Note 9)
- **Loop Around Playhead (Two Bars)** (default: Note 10)
- **Loop Around Playhead (Four Bars)** (default: Note 11)
- **Toggle Loop** (default: Note 12)
- **Jump to Cue Min/Max Range** (default: Notes 20-127)

#### Implementation Details
- Uses MidiMapping record for type-safe configuration
- Dynamic MIDI note assignment through Bitwig Preferences
- Real-time updates without extension restart
- Maintains backwards compatibility with original note assignments

## Architecture Notes

### Page Management
- **Pre-allocation Strategy**: All 8 pages are created during initialization (required by Bitwig API)
- **Dynamic Configuration**: Page settings (channel, CC range) are updated without recreating objects
- **Active Page Filtering**: Only processes MIDI for the number of pages specified by user
- **Memory Efficient**: Minimal overhead from inactive pages

### API Constraints
- Bitwig Studio Controller API requires all objects to be created during `init()` phase
- Cannot create new `CursorRemoteControlsPage` objects after initialization
- Solution: Pre-create maximum pages, then configure dynamically based on user preferences

## Dependencies

- **Bitwig Extension API v18**: Core controller framework
- **Lombok 1.18.30**: Code generation for cleaner Java (builders, getters, etc.)
- **Java 21**: Required runtime version

## Development Notes

- The extension generates `.bwextension` files (renamed JAR files) for Bitwig Studio
- The API documentation is here: file:///Applications/Bitwig%20Studio.app/Contents/Resources/Documentation/control-surface/api/index.html
- Both build systems handle the renaming and deployment automatically
- Debug output goes to Bitwig's console when DEBUG flag is enabled
- The project includes the source .java files in the repository. The compiled classes ignored in .gitignore