package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.CursorTrack;

import java.util.HashMap;
import java.util.Map;

/**
 * BWS Track Discovery Service
 * 
 * Discovers and manages tracks tagged with <BWS:0> to <BWS:5> for one-button track cycling.
 * Uses a large TrackBank (128 tracks) for project-wide discovery while maintaining 
 * Track references that survive track reordering.
 * 
 * Features:
 * - Real-time track name observation and BWS tag detection
 * - Round-robin cycling through available BWS tracks (0→1→2→3→4→5→0)
 * - Track reference storage (survives project changes)
 * - Integration with hardware LED feedback system
 */
public class BwsTrackDiscoveryService {
    
    private static final int DISCOVERY_TRACK_BANK_SIZE = 128;  // Large bank for discovery
    private static final int BWS_SLOT_COUNT = 6;  // BWS slots 0-5
    
    private final ControllerHost host;
    private final TrackBank allTracksBank;  // Large bank for discovery
    private final CursorTrack cursorTrack;   // Track current Bitwig selection
    
    // BWS track storage
    private final Map<Integer, Track> bwsTrackReferences = new HashMap<>();  // BWS slot → Track reference
    private final Map<Integer, Integer> bwsTrackPositions = new HashMap<>();  // BWS slot → Bank position (for debugging)
    private final Map<Track, Integer> trackToBwsSlot = new HashMap<>();       // Track reference → BWS slot (for selection detection)
    private int currentBwsSlot = 0;  // Current cycle position
    
    // Discovery state
    private boolean initialized = false;
    private int discoveredBwsTracks = 0;
    
    // LED update callback
    private LedUpdateCallback ledUpdateCallback;
    
    public BwsTrackDiscoveryService(ControllerHost host) {
        this.host = host;
        
        // Create large TrackBank for project-wide discovery
        this.allTracksBank = host.createMainTrackBank(DISCOVERY_TRACK_BANK_SIZE, 0, 0);
        
        // Create cursor track to observe current selection
        this.cursorTrack = host.createCursorTrack("BWS_CURSOR", "BWS Cursor", 0, 0, true);
        
        host.println("BwsTrackDiscoveryService: Initialized with " + DISCOVERY_TRACK_BANK_SIZE + " track discovery bank");
    }
    
    /**
     * Initializes track name observation for BWS discovery.
     * Should be called after controller setup is complete.
     */
    public void initialize() {
        if (initialized) {
            host.println("BwsTrackDiscoveryService: Already initialized, skipping");
            return;
        }
        
        host.println("BwsTrackDiscoveryService: Setting up track name observers...");
        
        // Observe track count changes to trigger rediscovery when tracks become available
        allTracksBank.itemCount().markInterested();
        allTracksBank.itemCount().addValueObserver(trackCount -> {
            host.println("BwsTrackDiscoveryService: Track count changed to " + trackCount + " - triggering rediscovery");
            if (trackCount > 0 && initialized) {
                // Rediscover BWS tracks when new tracks become available
                host.scheduleTask(() -> {
                    host.println("BwsTrackDiscoveryService: Auto-rediscovery triggered by track count change");
                    rediscoverBwsTracks();
                }, 500); // Short delay to let track data populate
            }
        });
        
        // Set up track name observation for all tracks in discovery bank
        for (int trackIndex = 0; trackIndex < DISCOVERY_TRACK_BANK_SIZE; trackIndex++) {
            final int finalTrackIndex = trackIndex;  // For lambda capture
            Track track = allTracksBank.getItemAt(trackIndex);
            
            // Mark track properties as interested
            track.exists().markInterested();
            track.name().markInterested();
            
            // Observe track name changes for BWS tag detection
            track.name().addValueObserver(trackName -> {
                host.println(String.format("BWS Observer: Track %d name changed to: \"%s\"", finalTrackIndex, trackName));
                reparseTrackForBwsTags(finalTrackIndex, track, trackName);
                
                // Update LED after track name changes
                updateBwsLedFeedback();
            });
        }
        
        // Set up cursor track selection observer for LED feedback
        cursorTrack.name().markInterested();
        cursorTrack.name().addValueObserver(trackName -> {
            updateLedForCurrentSelection(trackName);
        });
        
        initialized = true;
        host.println("BwsTrackDiscoveryService: Initialization complete");
        
        // Delay initial discovery to allow TrackBank to populate
        host.scheduleTask(() -> {
            host.println("BwsTrackDiscoveryService: Starting delayed initial discovery...");
            performInitialDiscovery();
        }, 2000); // 2 second delay
    }
    
    /**
     * Performs initial discovery of all existing BWS tracks.
     */
    private void performInitialDiscovery() {
        host.println("BwsTrackDiscoveryService: Performing initial BWS track discovery...");
        
        int existingTracks = 0;
        int totalTracks = 0;
        
        for (int trackIndex = 0; trackIndex < DISCOVERY_TRACK_BANK_SIZE; trackIndex++) {
            Track track = allTracksBank.getItemAt(trackIndex);
            totalTracks++;
            
            boolean trackExists = track.exists().get();
            String trackName = track.name().get();
            
            host.println(String.format("BWS Discovery: Scanning track %d - exists: %s, name: \"%s\"", 
                trackIndex, trackExists, trackName));
            
            if (trackExists) {
                existingTracks++;
                if (!trackName.isEmpty()) {
                    reparseTrackForBwsTags(trackIndex, track, trackName);
                } else {
                    host.println(String.format("BWS Discovery: Track %d exists but has empty name", trackIndex));
                }
            }
        }
        
        host.println(String.format("BWS Discovery: Scanned %d tracks, %d exist, processing complete", 
            totalTracks, existingTracks));
        logDiscoveryResults();
        
        // Update LED feedback after discovery
        updateBwsLedFeedback();
    }
    
    /**
     * Parses a track name for BWS tags and updates the BWS track mapping.
     * Called both during initial discovery and when track names change.
     * 
     * @param trackIndex The position of the track in the discovery bank
     * @param track The Track reference
     * @param trackName The current track name to parse
     */
    private void reparseTrackForBwsTags(int trackIndex, Track track, String trackName) {
        // Remove any existing BWS mappings for this track position
        removeTrackFromBwsMapping(trackIndex);
        
        // Skip empty or non-existent tracks
        if (trackName == null || trackName.trim().isEmpty() || !track.exists().get()) {
            return;
        }
        
        // Check for BWS tags anywhere in track name - format: <BWS:N>
        for (int bwsSlot = 0; bwsSlot < BWS_SLOT_COUNT; bwsSlot++) {
            String bwsTag = "<BWS:" + bwsSlot + ">";
            
            if (trackName.contains(bwsTag)) {
                // Found BWS tag - store track reference and position
                bwsTrackReferences.put(bwsSlot, track);
                bwsTrackPositions.put(bwsSlot, trackIndex);
                trackToBwsSlot.put(track, bwsSlot);  // Reverse mapping for selection detection
                discoveredBwsTracks = bwsTrackReferences.size();
                
                host.println(String.format("BWS Discovery: Found %s at position %d: \"%s\"", 
                    bwsTag, trackIndex, trackName));
                
                break; // Each track can only have one BWS slot
            }
        }
        
        // Debug: Log all track names being checked
        host.println(String.format("BWS Discovery: Checking track %d: \"%s\" (exists: %s)", 
            trackIndex, trackName, track.exists().get()));
    }
    
    /**
     * Removes a track from BWS mapping (used when track names change or tracks are deleted).
     * 
     * @param trackIndex The track position to remove from mapping
     */
    private void removeTrackFromBwsMapping(int trackIndex) {
        // Find and remove any BWS slot that was mapped to this track position
        bwsTrackPositions.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(trackIndex)) {
                // Remove from reverse mapping too
                Integer bwsSlot = entry.getKey();
                Track track = bwsTrackReferences.get(bwsSlot);
                if (track != null) {
                    trackToBwsSlot.remove(track);
                }
                return true;
            }
            return false;
        });
        
        // Also remove from track references (check if track reference matches position)
        bwsTrackReferences.entrySet().removeIf(entry -> {
            Integer mappedPosition = bwsTrackPositions.get(entry.getKey());
            if (mappedPosition != null && mappedPosition.equals(trackIndex)) {
                trackToBwsSlot.remove(entry.getValue());  // Remove from reverse mapping
                return true;
            }
            return false;
        });
        
        discoveredBwsTracks = bwsTrackReferences.size();
    }
    
    /**
     * Cycles to the next available BWS track in sequence.
     * Navigation order: BWS:0 → BWS:1 → BWS:2 → BWS:3 → BWS:4 → BWS:5 → BWS:0
     * 
     * @return true if a BWS track was found and navigated to, false if no BWS tracks available
     */
    public boolean cycleToNextBwsTrack() {
        if (!initialized) {
            host.println("BwsTrackDiscoveryService: Not initialized, cannot cycle tracks");
            return false;
        }
        
        if (discoveredBwsTracks == 0) {
            host.println("BWS Cycle: No BWS tracks found in project");
            return false;
        }
        
        // Find next available BWS track starting from current position
        for (int attempts = 0; attempts < BWS_SLOT_COUNT; attempts++) {
            Track bwsTrack = bwsTrackReferences.get(currentBwsSlot);
            
            if (bwsTrack != null && bwsTrack.exists().get()) {
                // Navigate to the BWS track
                bwsTrack.selectInEditor();
                bwsTrack.selectInMixer();
                
                String trackName = bwsTrack.name().get();
                Integer trackPosition = bwsTrackPositions.get(currentBwsSlot);
                
                host.println(String.format("BWS Cycle: Navigated to BWS:%d (position %d) - \"%s\"", 
                    currentBwsSlot, trackPosition != null ? trackPosition : -1, trackName));
                
                // Advance to next BWS slot for next cycle
                int previousBwsSlot = currentBwsSlot;
                currentBwsSlot = (currentBwsSlot + 1) % BWS_SLOT_COUNT;
                
                return true;
            }
            
            // Try next BWS slot if current one doesn't exist
            currentBwsSlot = (currentBwsSlot + 1) % BWS_SLOT_COUNT;
        }
        
        host.println("BWS Cycle: No valid BWS tracks found (all references invalid)");
        return false;
    }
    
    /**
     * Gets the current BWS slot that will be navigated to on next cycle.
     * Used for LED feedback to show which BWS track is next.
     * 
     * @return Current BWS slot (0-5), or -1 if no BWS tracks available
     */
    public int getCurrentBwsSlot() {
        if (!initialized || discoveredBwsTracks == 0) {
            return -1;
        }
        
        // Return the slot that would be used on next cycle
        for (int attempts = 0; attempts < BWS_SLOT_COUNT; attempts++) {
            int checkSlot = (currentBwsSlot + attempts) % BWS_SLOT_COUNT;
            Track bwsTrack = bwsTrackReferences.get(checkSlot);
            
            if (bwsTrack != null && bwsTrack.exists().get()) {
                return checkSlot;
            }
        }
        
        return -1; // No valid BWS tracks available
    }
    
    /**
     * Gets the number of discovered BWS tracks.
     * 
     * @return Number of BWS tracks (0-6)
     */
    public int getBwsTrackCount() {
        return discoveredBwsTracks;
    }
    
    /**
     * Checks if BWS track discovery has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Logs current BWS discovery results to console.
     */
    private void logDiscoveryResults() {
        host.println("=== BWS Track Discovery Results ===");
        host.println("Discovered " + discoveredBwsTracks + " BWS tracks:");
        
        for (int bwsSlot = 0; bwsSlot < BWS_SLOT_COUNT; bwsSlot++) {
            Track bwsTrack = bwsTrackReferences.get(bwsSlot);
            Integer position = bwsTrackPositions.get(bwsSlot);
            
            if (bwsTrack != null && position != null) {
                String trackName = bwsTrack.name().get();
                host.println(String.format("  BWS:%d → Position %d: \"%s\"", bwsSlot, position, trackName));
            }
        }
        
        if (discoveredBwsTracks == 0) {
            host.println("  No BWS tracks found. Add <BWS:0> to <BWS:5> tags to track names for navigation.");
        }
        
        host.println("=== End BWS Discovery Results ===");
    }
    
    /**
     * Forces a rediscovery of all BWS tracks.
     * Useful for debugging or if manual refresh is needed.
     */
    public void rediscoverBwsTracks() {
        host.println("BwsTrackDiscoveryService: Manual rediscovery requested");
        
        // Clear existing mapping
        bwsTrackReferences.clear();
        bwsTrackPositions.clear();
        trackToBwsSlot.clear();  // Clear reverse mapping too
        discoveredBwsTracks = 0;
        currentBwsSlot = 0;
        
        // Perform fresh discovery
        performInitialDiscovery();
    }
    
    /**
     * Updates BWS LED feedback on PAD4 to show current BWS slot.
     * Called after track discovery or track name changes.
     */
    private void updateBwsLedFeedback() {
        // Update LED based on currently selected track, not next cycle target
        String currentTrackName = cursorTrack.name().get();
        updateLedForCurrentSelection(currentTrackName);
    }
    
    /**
     * Updates LED feedback based on the currently selected track in Bitwig.
     * 
     * @param currentTrackName The name of the currently selected track
     */
    private void updateLedForCurrentSelection(String currentTrackName) {
        if (!initialized || ledUpdateCallback == null) {
            return;
        }
        
        // Check if current track is a BWS track
        Integer bwsSlot = null;
        for (int slot = 0; slot < BWS_SLOT_COUNT; slot++) {
            String bwsTag = "<BWS:" + slot + ">";
            if (currentTrackName != null && currentTrackName.contains(bwsTag)) {
                bwsSlot = slot;
                break;
            }
        }
        
        if (bwsSlot != null) {
            // Current track is a BWS track - show corresponding LED state
            host.println(String.format("BWS LED: Currently selected BWS:%d track (\"%s\")", bwsSlot, currentTrackName));
            ledUpdateCallback.updateBwsLed(bwsSlot);
        } else {
            // Current track is not a BWS track - show blinking green
            host.println(String.format("BWS LED: Currently selected non-BWS track (\"%s\") - showing green blink", currentTrackName));
            ledUpdateCallback.updateBwsLed(-2); // Special value for non-BWS track
        }
    }
    
    /**
     * Sets the LED update callback for track selection feedback.
     * 
     * @param callback The callback to handle LED updates
     */
    public void setLedUpdateCallback(LedUpdateCallback callback) {
        this.ledUpdateCallback = callback;
    }
    
    /**
     * Interface for LED update callbacks.
     */
    public interface LedUpdateCallback {
        /**
         * Updates the BWS LED based on current selection.
         * 
         * @param bwsSlot BWS slot (0-5) if BWS track selected, -1 if no BWS tracks available, -2 if non-BWS track selected
         */
        void updateBwsLed(int bwsSlot);
    }
}