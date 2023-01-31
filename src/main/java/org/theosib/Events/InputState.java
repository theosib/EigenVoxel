package org.theosib.Events;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.theosib.Adaptors.Window;

public class InputState {
    private Window window;
    private IntSet buttonsDown = new IntArraySet();

    public InputState(Window w) {
        window = w;
    }
    public boolean isKeyPressed(int key) {
        return window.isKeyPressed(key);
    }

    public boolean isButtonPressed(int button) {
        return buttonsDown.contains(button);
    }

    public void buttonDown(int key) {
        buttonsDown.add(key);
    }

    public void buttonUp(int key) {
        buttonsDown.remove(key);
    }
}
