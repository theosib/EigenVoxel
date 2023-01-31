package org.theosib.Adaptors;

import org.theosib.Utils.Priority;
import org.theosib.Utils.WindowDimensions;

public interface RenderAgent extends Priority, Disposable {
    void create(Window w);

    void destroy();

    boolean willRender(Window w);

    void render(Window w);

    void resize(Window w, WindowDimensions dim);
}
