package de.davidrival.softstep.hardware;

import com.bitwig.extension.controller.api.MidiOut;
import de.davidrival.softstep.controller.LedStates;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SoftstepHardwareBase {

    private final MidiOut midiOut;

    public static final int STATUS_BYTE = 176;
    public static final int NAV_LEFT_DATA1 = 80;
    public static final int NAV_RIGHT_DATA1 = 81;
    public static final int NAV_PAD_PUSHED_DOWN_TRESHOLD = 2;




    public SoftstepHardwareBase(MidiOut midiOut) {
        this.midiOut = midiOut;
        init();
    }

    public void init() {
        midiOut.sendSysex("f0 00 1b 48 7a 01 00 00 00 00 00 00 00 00 00 00 00 01 00 09 00 0b 2b 3a 00 10 04 01 00 00 00 00 00 00 00 2f 7e 00 00 00 00 02 f7"); //Standalone
        midiOut.sendSysex("f0 00 1b 48 7a 01 00 00 00 00 00 00 00 00 00 00 00 01 00 09 00 0b 2b 3a 00 10 03 00 00 00 00 00 00 00 00 50 07 00 00 00 00 00 f7"); // Tether
        midiOut.sendSysex("f0 00 1b 48 7a 01 00 00 00 00 00 00 00 00 00 00 00 01 00 04 00 05 08 25 01 20 00 00 7b 2c 00 00 00 0c f7"); // backlight
    }

    public void exit() {
        midiOut.sendSysex("f0 00 1b 48 7a 01 00 00 00 00 00 00 00 00 00 00 00 01 00 04 00 05 08 25 00 20 00 00 4c 1c 00 00 00 0c f7"); // backlight
        displayText("   ");
        resetLeds();

        midiOut.sendSysex("f0 00 1b 48 7a 01 00 00 00 00 00 00 00 00 00 00 00 01 00 09 00 0b 2b 3a 00 10 04 00 00 00 00 00 00 00 00 17 1f 00 00 00 00 00 f7"); // standalone
        midiOut.sendSysex("f0 00 1b 48 7a 01 00 00 00 00 00 00 00 00 00 00 00 01 00 09 00 0b 2b 3a 00 10 03 01 00 00 00 00 00 00 00 68 66 00 00 00 00 00 f7"); // tether
    }

    public void drawLedAt(int index, LedStates ledStates) {
        // strange.. needs to turn of on all colors before drawing
        setLed(index, LedColor.GREEN.data2ForLed, LedLight.OFF.data2ForLed);
        setLed(index, LedColor.YELLOW.data2ForLed, LedLight.OFF.data2ForLed);
        setLed(index, LedColor.RED.data2ForLed, LedLight.OFF.data2ForLed);
        setLed(index
                , ledStates.ledColor.data2ForLed
                , ledStates.ledFlashing.data2ForLed);
    }

    /**
     * Sets led number <led> (numbered from 1 to 10) to given color and mode
     *
     * @param number  select led, numbered from 0
     * @param color  green = 0, red = 1, yellow = 2
     * @param mode  range(x) = (off, on, blink, fast, flash)
     */
    public void setLed(int number, int color, int mode) {
        midiOut.sendMidi(0xB0,40,number); // select led, numbered from 0
        midiOut.sendMidi(0xB0,41,color); // green = 0, red = 1, yellow = 2
        midiOut.sendMidi(0xB0,42,mode);// range(x) = (off, on, blink, fast, flash)
        midiOut.sendMidi(0xB0,0,0);
        midiOut.sendMidi(0xB0,0,0);
        midiOut.sendMidi(0xB0,0,0);
    }

    /**
     * Switch all leds off
     */
    public void resetLeds() {
        for( int l= 0; l<10; l++) {
            for( int c=0; c<3; c++) {
                setLed(l,c, LedLight.OFF.data2ForLed);
            }
        }
    }

//    public void  updateLeds() {
//        ls = ledstates[current_page];
//        for( int l= 0; l<10; l++) {
//            for( int c=0; c<3; c++) {
//                setLed(l,c,Led.OFF);
//            }
//            setLed(l,ls[l][0],ls[l][1]);
//        }
//    }
    /**
     * Sets the text on the device's display. The text gets truncated to 4 chars
     */
    public void displayText(String text) {
        for(int i=0; i<4; i++) {
            int cc = i < text.length() ? text.charAt(i) : 0x20;
            midiOut.sendMidi(176,50+i,cc);
        }
    }

}
