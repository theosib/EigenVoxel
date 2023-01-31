package org.theosib.Events;

import org.theosib.Adaptors.Window;

public abstract class InputEvent {
    public Window window;
    public InputState state;

    public InputEvent(Window window, InputState state) {
        this.window = window;
        this.state = state;
    }


    static public class TimerEvent extends InputEvent {
        public TimerEvent(Window window, InputState state) {
            super(window, state);
        }
    }

    static public class KeyEvent extends InputEvent {
        public int key, scancode, action, mods;

        public KeyEvent(Window window, InputState state, int key, int scancode, int action, int mods) {
            super(window, state);
            this.key = key;
            this.scancode = scancode;
            this.action = action;
            this.mods = mods;
        }
    }

    static public class KeyTyped extends InputEvent {
        public int codepoint;

        public KeyTyped(Window window, InputState state, int codepoint) {
            super(window, state);
            this.codepoint = codepoint;
        }
    }

    static public class ButtonEvent extends InputEvent {
        public int button, action, mods;

        public ButtonEvent(Window window, InputState state, int button, int action, int mods) {
            super(window, state);
            this.button = button;
            this.action = action;
            this.mods = mods;
        }
    }

    static public class ScrollEvent extends InputEvent {
        public double xoffset, yoffset;

        public ScrollEvent(Window window, InputState state, double xoffset, double yoffset) {
            super(window, state);
            this.xoffset = xoffset;
            this.yoffset = yoffset;
        }
    }

    static public class MotionEvent extends InputEvent {
        public double xpos, ypos;

        public MotionEvent(Window window, InputState state, double xpos, double ypos) {
            super(window, state);
            this.xpos = xpos;
            this.ypos = ypos;
        }
    }
}

