package org.theosib.Adaptors;

import org.theosib.Utils.Priority;
import org.theosib.Utils.WindowDimensions;

public interface RenderAgent extends Priority, Disposable {
    void create(Window w);

    void destroy();

    boolean willRender(Window w, double elapsedTime);

    void render(Window w, double elapsedTime);

    void resize(Window w, WindowDimensions dim);
}
