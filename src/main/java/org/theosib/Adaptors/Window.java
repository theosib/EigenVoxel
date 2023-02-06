package org.theosib.Adaptors;

import org.theosib.Events.InputEvent;
import org.theosib.Events.InputState;
import org.theosib.Utils.CursorPos;
import org.theosib.Utils.PriorityList;
import org.theosib.Utils.WindowDimensions;
import org.joml.Matrix4f;

public abstract class Window implements Disposable {
    protected Window(int w, int h) {
        dim.win.w = w;
        dim.win.h = h;
    }

    protected WindowDimensions dim = new WindowDimensions();
    protected InputState inputState = new InputState(this);

    public WindowDimensions getDim() {
        return dim;
    }

    public InputState getInputState() {
        return inputState;
    }

    PriorityList<RenderAgent> renderers = new PriorityList<>();
    PriorityList<InputReceiver> inputters = new PriorityList<>();

    public void addRenderer(RenderAgent r) {
        renderers.add(r);
        r.resize(this, dim);
    }

    public void removeRenderer(RenderAgent r) {
        renderers.remove(r);
    }

    public void addInputter(InputReceiver i) {
        inputters.add(i);
    }

    public void removeInputter(InputReceiver i) {
        inputters.remove(i);
    }

    abstract public void toggleFullScreen();
    abstract public void setFullScreen(boolean full);
    abstract public void setRawMouseInput(boolean raw);
    abstract public void create();
    abstract public void destroy();

    abstract public void swapBuffers();
    abstract public void clearBackground();
    abstract public void pollInputs();
    abstract public boolean shouldClose();

    public boolean renderAll() {
        boolean anyUpdates = false;
        for (RenderAgent r : renderers) {
            if (r.willRender(this)) {
                anyUpdates = true;
                break;
            }
        }
        if (!anyUpdates) return false;

        clearBackground();
        for (RenderAgent r : renderers) {
            r.render(this);
        }

        return true;
    }

    protected double lastProcessTime = -1;
    public static double getCurrentTime() {
        return System.nanoTime() * 1.0e-9;
    }

    public void processInputsAll(InputState state) {
        double currentTime = getCurrentTime();
        if (lastProcessTime < 0) lastProcessTime = currentTime;
        double deltaTime = Math.min(currentTime - lastProcessTime, 0.008);
        for (InputReceiver i : inputters) {
            i.process(state, deltaTime);
        }
        lastProcessTime = currentTime;
    }

    boolean do_quit = false;
    public void setQuit(boolean q) {
        do_quit = q;
    }

    public void renderPass() {
        pollInputs();
        processInputsAll(getInputState());

        if (renderAll()) {
            swapBuffers();
        } else {
            try {
                Thread.sleep(16);
            } catch (Exception x) {}
        }
    }

    public void renderLoop() {
        while (!do_quit && !shouldClose()) {
            renderPass();
        }
    }


    abstract public CursorPos getCursorPos();
    abstract public float getAspectRatio();
    abstract public Matrix4f getProjectionMatrix();

    protected double FOV = 0;
    public void setFOV(double fov) { FOV = fov; }
    public double getFOV() { return FOV; }

    protected void destroyRenderers() {
        for (RenderAgent r : renderers) {
            r.destroy();
        }
        renderers.clear();
    }

    protected void destroyInputters() {
        inputters.clear();
    }

    protected void resizeRenderers(WindowDimensions dim) {
        for (RenderAgent r : renderers) {
            r.resize(this, dim);
        }
    }

    protected void deliverInputEvent(InputEvent ev) {
        for (InputReceiver i : inputters) {
            boolean consumed = i.event(ev);
            if (consumed) return;
        }
    }

    abstract public void grabCursor(boolean grab);
    abstract public boolean cursorIsGrabbed();

    abstract public boolean isKeyPressed(int key);
}
