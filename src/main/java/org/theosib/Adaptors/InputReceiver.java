package org.theosib.Adaptors;

import org.theosib.Events.InputEvent;
import org.theosib.Events.InputState;
import org.theosib.Utils.Priority;

public interface InputReceiver extends Priority {
    boolean event(InputEvent ev);
    void process(InputState state, double timeDelta);
}
