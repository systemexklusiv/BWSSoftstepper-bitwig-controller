package de.davirival.hardware;

import com.bitwig.extension.api.MemoryBlock;
import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.api.graphics.*;
import com.bitwig.extension.api.opensoundcontrol.OscModule;
import com.bitwig.extension.callback.ConnectionEstablishedCallback;
import com.bitwig.extension.callback.DataReceivedCallback;
import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.Softstep1Controls;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TestSoftstep1 {

    @Test
    public void isInitWorking() {
        Softstep1Controls softstep1 = new Softstep1Controls(host);

        Assert.assertEquals(10, softstep1.getPads().size());
        Assert.assertEquals(Optional.of(44).get(), softstep1
                .getPads()
                .get(0)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(0));

        Assert.assertEquals(Optional.of(83).get(), softstep1
                .getPads()
                .get(9)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(3));
    }


    ControllerHost host = new ControllerHost() {
        @Override
        public void restart() {

        }

        @Override
        public void loadAPI(int version) {

        }

        @Override
        public void useBetaApi() {

        }

        @Override
        public boolean shouldFailOnDeprecatedUse() {
            return false;
        }

        @Override
        public void setShouldFailOnDeprecatedUse(boolean value) {

        }

        @Override
        public void load(String path) {

        }

        @Override
        public boolean platformIsWindows() {
            return false;
        }

        @Override
        public boolean platformIsMac() {
            return false;
        }

        @Override
        public boolean platformIsLinux() {
            return false;
        }

        @Override
        public void defineController(String vendor, String name, String version, String uuid, String author) {

        }

        @Override
        public void defineMidiPorts(int numInports, int numOutports) {

        }

        @Override
        public MidiIn getMidiInPort(int index) {
            return null;
        }

        @Override
        public MidiOut getMidiOutPort(int index) {
            return null;
        }

        @Override
        public HardwareDevice hardwareDevice(int index) {
            return null;
        }

        @Override
        public void addDeviceNameBasedDiscoveryPair(String[] inputs, String[] outputs) {

        }

        @Override
        public void defineSysexIdentityReply(String reply) {

        }

        @Override
        public Preferences getPreferences() {
            return null;
        }

        @Override
        public DocumentState getDocumentState() {
            return null;
        }

        @Override
        public NotificationSettings getNotificationSettings() {
            return null;
        }

        @Override
        public Project getProject() {
            return null;
        }

        @Override
        public Transport createTransport() {
            return null;
        }

        @Override
        public Groove createGroove() {
            return null;
        }

        @Override
        public Application createApplication() {
            return null;
        }

        @Override
        public Arranger createArranger() {
            return null;
        }

        @Override
        public Arranger createArranger(int window) {
            return null;
        }

        @Override
        public Mixer createMixer() {
            return null;
        }

        @Override
        public Mixer createMixer(String panelLayout) {
            return null;
        }

        @Override
        public Mixer createMixer(int window) {
            return null;
        }

        @Override
        public Mixer createMixer(String panelLayout, int window) {
            return null;
        }

        @Override
        public DetailEditor createDetailEditor() {
            return null;
        }

        @Override
        public DetailEditor createDetailEditor(int window) {
            return null;
        }

        @Override
        public TrackBank createTrackBank(int numTracks, int numSends, int numScenes) {
            return null;
        }

        @Override
        public TrackBank createTrackBank(int numTracks, int numSends, int numScenes, boolean hasFlatTrackList) {
            return null;
        }

        @Override
        public TrackBank createMainTrackBank(int numTracks, int numSends, int numScenes) {
            return null;
        }

        @Override
        public TrackBank createEffectTrackBank(int numTracks, int numScenes) {
            return null;
        }

        @Override
        public MasterTrack createMasterTrack(int numScenes) {
            return null;
        }

        @Override
        public CursorTrack createArrangerCursorTrack(int numSends, int numScenes) {
            return null;
        }

        @Override
        public CursorTrack createCursorTrack(String name, int numSends, int numScenes) {
            return null;
        }

        @Override
        public CursorTrack createCursorTrack(String id, String name, int numSends, int numScenes, boolean shouldFollowSelection) {
            return null;
        }

        @Override
        public SceneBank createSceneBank(int numScenes) {
            return null;
        }

        @Override
        public CursorDevice createEditorCursorDevice() {
            return null;
        }

        @Override
        public CursorDevice createEditorCursorDevice(int numSends) {
            return null;
        }

        @Override
        public Clip createCursorClip(int gridWidth, int gridHeight) {
            return null;
        }

        @Override
        public Clip createLauncherCursorClip(int gridWidth, int gridHeight) {
            return null;
        }

        @Override
        public Clip createArrangerCursorClip(int gridWidth, int gridHeight) {
            return null;
        }

        @Override
        public UserControlBank createUserControls(int numControllers) {
            return null;
        }

        @Override
        public void scheduleTask(Object callback, Object[] args, long delay) {

        }

        @Override
        public void scheduleTask(Runnable callback, long delay) {

        }

        @Override
        public void requestFlush() {

        }

        @Override
        public void println(String s) {

        }

        @Override
        public void errorln(String s) {

        }

        @Override
        public void showPopupNotification(String text) {

        }

        @Override
        public RemoteSocket createRemoteConnection(String name, int defaultPort) {
            return null;
        }

        @Override
        public void connectToRemoteHost(String host, int port, ConnectionEstablishedCallback callback) {

        }

        @Override
        public void sendDatagramPacket(String host, int port, byte[] data) {

        }

        @Override
        public boolean addDatagramPacketObserver(String name, int port, DataReceivedCallback callback) {
            return false;
        }

        @Override
        public void defineController(String vendor, String name, String version, String uuid) {

        }

        @Override
        public Transport createTransportSection() {
            return null;
        }

        @Override
        public CursorTrack createCursorTrack(int numSends, int numScenes) {
            return null;
        }

        @Override
        public Groove createGrooveSection() {
            return null;
        }

        @Override
        public Application createApplicationSection() {
            return null;
        }

        @Override
        public Arranger createArrangerSection(int screenIndex) {
            return null;
        }

        @Override
        public Mixer createMixerSection(String perspective, int screenIndex) {
            return null;
        }

        @Override
        public TrackBank createTrackBankSection(int numTracks, int numSends, int numScenes) {
            return null;
        }

        @Override
        public TrackBank createMainTrackBankSection(int numTracks, int numSends, int numScenes) {
            return null;
        }

        @Override
        public TrackBank createEffectTrackBankSection(int numTracks, int numScenes) {
            return null;
        }

        @Override
        public CursorTrack createCursorTrackSection(int numSends, int numScenes) {
            return null;
        }

        @Override
        public Track createMasterTrackSection(int numScenes) {
            return null;
        }

        @Override
        public Clip createCursorClipSection(int gridWidth, int gridHeight) {
            return null;
        }

        @Override
        public CursorDevice createCursorDeviceSection(int numControllers) {
            return null;
        }

        @Override
        public CursorDevice createCursorDevice() {
            return null;
        }

        @Override
        public UserControlBank createUserControlsSection(int numControllers) {
            return null;
        }

        @Override
        public void defineSysexDiscovery(String request, String reply) {

        }

        @Override
        public PopupBrowser createPopupBrowser() {
            return null;
        }

        @Override
        public BeatTimeFormatter defaultBeatTimeFormatter() {
            return null;
        }

        @Override
        public void setDefaultBeatTimeFormatter(BeatTimeFormatter formatter) {

        }

        @Override
        public BeatTimeFormatter createBeatTimeFormatter(String separator, int barsLen, int beatsLen, int subdivisionLen, int ticksLen) {
            return null;
        }

        @Override
        public HardwareSurface createHardwareSurface() {
            return null;
        }

        @Override
        public HardwareActionMatcher createOrHardwareActionMatcher(HardwareActionMatcher matcher1, HardwareActionMatcher matcher2) {
            return null;
        }

        @Override
        public RelativeHardwareValueMatcher createOrRelativeHardwareValueMatcher(RelativeHardwareValueMatcher matcher1, RelativeHardwareValueMatcher matcher2) {
            return null;
        }

        @Override
        public AbsoluteHardwareValueMatcher createOrAbsoluteHardwareValueMatcher(AbsoluteHardwareValueMatcher matcher1, AbsoluteHardwareValueMatcher matcher2) {
            return null;
        }

        @Override
        public MidiExpressions midiExpressions() {
            return null;
        }

        @Override
        public HardwareActionBindable createAction(Runnable runnable, Supplier<String> descriptionProvider) {
            return null;
        }

        @Override
        public HardwareActionBindable createAction(DoubleConsumer actionPressureConsumer, Supplier<String> descriptionProvider) {
            return null;
        }

        @Override
        public RelativeHardwarControlBindable createRelativeHardwareControlStepTarget(HardwareActionBindable stepForwardsAction, HardwareActionBindable stepBackwardsAction) {
            return null;
        }

        @Override
        public RelativeHardwarControlBindable createRelativeHardwareControlAdjustmentTarget(DoubleConsumer adjustmentConsumer) {
            return null;
        }

        @Override
        public AbsoluteHardwarControlBindable createAbsoluteHardwareControlAdjustmentTarget(DoubleConsumer adjustmentConsumer) {
            return null;
        }

        @Override
        public void deleteObjects(String undoName, DeleteableObject... objects) {

        }

        @Override
        public void deleteObjects(DeleteableObject... objects) {

        }

        @Override
        public DeviceMatcher createInstrumentMatcher() {
            return null;
        }

        @Override
        public DeviceMatcher createAudioEffectMatcher() {
            return null;
        }

        @Override
        public DeviceMatcher createNoteEffectMatcher() {
            return null;
        }

        @Override
        public DeviceMatcher createBitwigDeviceMatcher(UUID id) {
            return null;
        }

        @Override
        public DeviceMatcher createVST2DeviceMatcher(int id) {
            return null;
        }

        @Override
        public DeviceMatcher createVST3DeviceMatcher(String id) {
            return null;
        }

        @Override
        public DeviceMatcher createActiveDeviceMatcher() {
            return null;
        }

        @Override
        public DeviceMatcher createFirstDeviceInChainMatcher() {
            return null;
        }

        @Override
        public DeviceMatcher createLastDeviceInChainMatcher() {
            return null;
        }

        @Override
        public DeviceMatcher createOrDeviceMatcher(DeviceMatcher... deviceMatchers) {
            return null;
        }

        @Override
        public DeviceMatcher createAndDeviceMatcher(DeviceMatcher... deviceMatchers) {
            return null;
        }

        @Override
        public DeviceMatcher createNotDeviceMatcher(DeviceMatcher deviceMatcher) {
            return null;
        }

        @Override
        public int getHostApiVersion() {
            return 0;
        }

        @Override
        public String getHostVendor() {
            return null;
        }

        @Override
        public String getHostProduct() {
            return null;
        }

        @Override
        public String getHostVersion() {
            return null;
        }

        @Override
        public PlatformType getPlatformType() {
            return null;
        }

        @Override
        public void setErrorReportingEMail(String address) {

        }

        @Override
        public OscModule getOscModule() {
            return null;
        }

        @Override
        public MemoryBlock allocateMemoryBlock(int size) {
            return null;
        }

        @Override
        public Bitmap createBitmap(int width, int height, BitmapFormat format) {
            return null;
        }

        @Override
        public FontFace loadFontFace(String path) {
            return null;
        }

        @Override
        public FontOptions createFontOptions() {
            return null;
        }

        @Override
        public Image loadPNG(String path) {
            return null;
        }

        @Override
        public Image loadSVG(String path, double scale) {
            return null;
        }
    };
}
