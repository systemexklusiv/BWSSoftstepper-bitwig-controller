package de.davidrival.softstep;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.controller.ControllerPages;
import de.davidrival.softstep.controller.Page;
import de.davidrival.softstep.controller.SoftstepController;
import de.davidrival.softstep.hardware.SoftstepHardware;

public class SoftstepperExtension extends ControllerExtension
{
   MidiIn midiIn;
   MidiOut midiOut;

   Transport transport;

   SoftstepController softstepController;


   protected SoftstepperExtension(final SoftstepperExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      String[] options = {"option1", "option2"};
      Preferences preferences = host.getPreferences();

      SettableEnumValue enumSetting = preferences.getEnumSetting("Mode", "Global", options, options[0]);
      enumSetting.addValueObserver(newValue -> host.println(" Enum Setting Changed to :" + newValue));

      transport = host.createTransport();
      midiIn = host.getMidiInPort(0);
      midiOut = host.getMidiOutPort(0);

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
      midiIn.setSysexCallback(this::onSysex0);

      SoftstepHardware softstepHardware = new SoftstepHardware(midiOut);

      ControllerPages controllerPages = new ControllerPages(Page.CLIP);
      softstepController = new SoftstepController(controllerPages
              , softstepHardware
              , host
      );

      softstepController.display();

      host.showPopupNotification("BWSSoftstepper Initialized");
      getHost().println("BWSSoftstepper Initialized!");
   }


   @Override
   public void exit()
   {
      softstepController.exit();

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
//      getHost().println("--- incomming msg! ---");
//      getHost().println(msg.toString());
//      getHost().println("^^^^^^^^^^^^^^^^^^^^^^");
      softstepController.handleMidi(msg);
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data)
   {
      // MMC Transport Controls:
      switch (data) {
         case "f07f7f0605f7":
            transport.rewind();
            break;
         case "f07f7f0604f7":
            transport.fastForward();
            break;
         case "f07f7f0601f7":
            transport.stop();
            break;
         case "f07f7f0602f7":
            transport.play();
            break;
         case "f07f7f0606f7":
            transport.record();
            break;
      }
   }
}
