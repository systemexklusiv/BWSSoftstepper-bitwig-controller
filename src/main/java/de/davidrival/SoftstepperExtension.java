package de.davidrival;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import de.davidrival.hardware.Softstep1;

import java.util.ArrayList;
import java.util.List;

public class SoftstepperExtension extends ControllerExtension
{

   List<AbsoluteHardwareControl> sliders = new ArrayList<>(10);
   MidiIn midiIn;
   MidiOut midiOut;
   static final int CHANNEL = 0;
   HardwareSurface hardwareSurface;

   Softstep1 sstep = new Softstep1();

   protected SoftstepperExtension(final SoftstepperExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();      

      mTransport = host.createTransport();
      midiIn = host.getMidiInPort(0);
      midiOut = host.getMidiOutPort(0);

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi0(msg));
      midiIn.setSysexCallback((String data) -> onSysex0(data));

      hardwareSurface = host.createHardwareSurface();

      TrackBank trackBank = host.createMainTrackBank(sstep.getPads().size() / 2,0,0);

      final Transport transport = host.createTransport();

      // OnOffHardwareLight
      final OnOffHardwareLight light = this.hardwareSurface.createOnOffHardwareLight ("PLAY_BUTTON_LIGHT");

      light.isOn ().onUpdateHardware (isOn -> {

                 host.println ("Updating LED: " + (isOn.booleanValue () ? "On" : "Off"));

                 // TODO Send the LED light state to the display, normally this is a CC or note command
                 // midiOutPort.sendMidi (int status, int data1, int data2)
              });

      transport.isPlaying().markInterested();
      light.isOn().setValueSupplier(() -> transport.isPlaying().get());

      light.setStateToVisualStateFunction (onOffState -> {

      final Color backgroundColor = onOffState.booleanValue () ? Color.fromRGB (0, 1, 0) : Color.fromRGB (0, 0, 0);
      final Color labelColor = onOffState.booleanValue () ? Color.fromRGB (0, 0, 0) : Color.fromRGB (1, 1, 1);
      return HardwareLightVisualState.createForColor (backgroundColor, labelColor);

   });



      for (int i = 0; i < sstep.getPads().size() / 2; i++) {

//         AbsoluteHardwareControl slider = hardwareSurface.createHardwareSlider("SLIDER_" + i);
//         slider.setAdjustValueMatcher(midiIn
//                 .createAbsoluteCCValueMatcher (sstep.CHANNEL, sstep.getPads().get(i)));
//
//         slider.setBinding(trackBank.getItemAt(i).volume());
//
//         slider.setBackgroundLight(light);
//
//         sliders.add(slider);

         HardwareButton button = hardwareSurface.createHardwareButton("BUTTON_" + i);
         button.pressedAction ().setActionMatcher (midiIn.createCCActionMatcher (CHANNEL, i, 60));
         button.releasedAction ().setActionMatcher (midiIn.createCCActionMatcher (CHANNEL, i, 0));

         button.pressedAction().setBinding(trackBank.getItemAt(i).volume().set(0.0));

         button.setBackgroundLight(light);

      }

      hardwareSurface.setPhysicalSize (200, 200);

      hardwareSurface.hardwareElementWithId("SLIDER_0").setBounds(13.25, 137.0, 10.0, 50.0);
      hardwareSurface.hardwareElementWithId("SLIDER_1").setBounds(25.25, 137.0, 10.0, 50.0);
      hardwareSurface.hardwareElementWithId("SLIDER_2").setBounds(37.25, 137.0, 10.0, 50.0);
      hardwareSurface.hardwareElementWithId("SLIDER_3").setBounds(49.25, 137.0, 10.0, 50.0);
      hardwareSurface.hardwareElementWithId("SLIDER_4").setBounds(61.25, 137.0, 10.0, 50.0);

      host.showPopupNotification("BWSSoftstepper Initialized");
      getHost().println("BWSSoftstepper Initialized!");
   }


   @Override
   public void exit()
   {
      // TODO: Perform any cleanup once the driver exits
      // For now just show a popup notification for verification that it is no longer running.
      getHost().showPopupNotification("BWSSoftstepper Exited");
   }

   @Override
   public void flush()
   {
      // TODO Send any updates you need here.
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg) 
   {
      getHost().println(msg.toString());
      hardwareSurface.updateHardware();

   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data) 
   {
      // MMC Transport Controls:
      if (data.equals("f07f7f0605f7"))
            mTransport.rewind();
      else if (data.equals("f07f7f0604f7"))
            mTransport.fastForward();
      else if (data.equals("f07f7f0601f7"))
            mTransport.stop();
      else if (data.equals("f07f7f0602f7"))
            mTransport.play();
      else if (data.equals("f07f7f0606f7"))
            mTransport.record();
   }

   private Transport mTransport;
}
