package flooferland.showbiz.lowlevel.types;

import flooferland.chirp.safety.Result;
import flooferland.chirp.types.math.TimeLength;
import flooferland.chirp.types.math.TimePoint;
import flooferland.showbiz.lowlevel.MidiSignalManager;

import javax.annotation.Nonnull;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;

// TODO: FIX ME. URGENT:
//        BitEvents hold a duration, and they're currently using the classic event type,
//        meaning a "BIT_ON" state can have a length, and a "BIT_OFF" state can have a length,
//        when instead they should both either store their time position or be combined into one "BIT" state and hold the start and length

public class SignalEvents {
    /** Converts a signal event to MIDI */
    public static Result<MidiEvent, String> toMidi(@Nonnull SignalEvent event, @Nonnull final Sequence sequence, double bpm) {
        try {
            Result<ShortMessage, String> result;
            switch (event) {
                case BitEvent ev -> {
                    result = Result.ok(new ShortMessage(
                            /* Command  */ ev.State ? ShortMessage.NOTE_ON : ShortMessage.NOTE_OFF,
                            /* Channel  */ (int) ev.Bit.Drawer.Number,
                            /* Value    */ ev.Bit.Id,
                            /* Velocity */ MidiSignalManager.defaultVelocity
                    ));
                }
                case OtherMidiEvent ev -> {
                    result = Result.ok(new ShortMessage(
                            ev.Message[0],
                            ev.Message.length > 1 ? ev.Message[1] : 0x00,
                            ev.Message.length > 2 ? ev.Message[2] : 0x00
                    ));
                }
                default -> {
                    result = Result.err("Unexpected value: " + event);
                }
            };
            
            return result.mapOk(message -> new MidiEvent(message, event.TimeStamp.asMidiTicks(sequence, bpm)));
        } catch (Exception e) {
            return Result.err(e.toString());
        }
    }

    /** Converts a MIDI event to a signal event */
    public static Result<SignalEvent, String> fromMidi(@Nonnull MidiEvent event, @Nonnull TimePoint time) {
        MidiMessage message = event.getMessage();

        SignalEvent signalEvent;
        int msg = message.getStatus() & 0xF0;
        switch (msg) {
            case ShortMessage.NOTE_ON:
            case ShortMessage.NOTE_OFF:
                short channel = (short) (message.getStatus() & 0x0F);
                byte note = message.getMessage()[1];
                byte velocity = message.getMessage()[2];
                BitInfo bit = BitInfo.ofGuaranteed(note, DrawerInfo.of(channel));

                signalEvent = new BitEvent(time, msg == ShortMessage.NOTE_ON, bit);
                break;
            default:
                return Result.err("MIDI event \"status:%s data:%s\" is not a bit event", message.getStatus(), Arrays.toString(message.getMessage()));
        };

        return Result.ok(signalEvent);
    }
    
    // region | Events
    /** Event containing miscellaneous MIDI data */
    public static class OtherMidiEvent extends SignalEvent {
        public byte[] Message;
        
        public OtherMidiEvent(TimePoint time, byte[] message, HashMap<String, Object> extraData) {
            super(EventType.OTHER, time);
            Message = message;
        }
        
        public OtherMidiEvent(TimePoint time, byte[] message) {
            super(EventType.OTHER, time);
            Message = message;
        }
    }
    
    /** Event containing signal bit data */
    public static class BitEvent extends SignalEvent {
        public boolean State;
        public BitInfo Bit;
        public BitEvent(TimePoint time, boolean state, BitInfo bit, HashMap<String, Object> extraData) {
            super(EventType.BIT, time, extraData);
            State = state;
            Bit = bit;
        }
        public BitEvent(TimePoint time, boolean state, BitInfo bit) {
            super(EventType.BIT, time);
            State = state;
            Bit = bit;
        }
    }
    // endregion
}
