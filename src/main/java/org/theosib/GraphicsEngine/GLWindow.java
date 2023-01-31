package org.theosib.GraphicsEngine;

import org.theosib.Adaptors.Window;
import org.theosib.Events.InputEvent;
import org.theosib.Utils.*;
import org.joml.Matrix4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;

public class GLWindow extends Window {
    private long window, monitor;
    String title;

    public GLWindow(String title, int w, int h) {
        super(w, h);
        this.title = title;
    }

    @Override
    public void toggleFullScreen() {
        setFullScreen(!inFullScreen);
    }

    private boolean inFullScreen = false;
    @Override
    public void setFullScreen(boolean full) {
        if (full == inFullScreen) return;

        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        if (full) {
            savedWindowSize = getGLWindowSize();
            GLFW.glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), GLFW.GLFW_DONT_CARE);
        } else {
            GLFW.glfwSetWindowMonitor(window, 0, savedWindowSize.x, savedWindowSize.y, savedWindowSize.w, savedWindowSize.h,
                    GLFW.GLFW_DONT_CARE);
        }
        inFullScreen = full;
    }

    @Override
    public void setRawMouseInput(boolean raw) {
        if (GLFW.glfwRawMouseMotionSupported())
            GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, raw ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }

    CursorScale cursorScale = null;
    IntBox savedWindowSize = null;

    @Override
    public void create() {
        if (window != 0) return;

        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Could not init window");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL33.GL_TRUE);

        window = GLFW.glfwCreateWindow(dim.win.w, dim.win.h, title, 0, 0);
        if (window == 0) {
            throw new RuntimeException("Window creation fail");
        }

        monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
        GLFW.glfwSetWindowPos(window, (videoMode.width() - dim.win.w) / 2, (videoMode.height() - dim.win.h) / 2);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GL33.glEnable(GL33.GL_DEPTH_TEST);
        GL33.glDisable(GL33.GL_BLEND);
        GL33.glEnable(GL33.GL_CULL_FACE);
        GL33.glCullFace(GL33.GL_BACK);

        long cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
        GLFW.glfwSetCursor(window, cursor);
        createCallbacks();
        setRawMouseInput(true);

        GLFW.glfwShowWindow(window);
        GLFW.glfwSwapInterval(1);

        dim.fb = fetchFBSize();
        GL33.glViewport(0, 0, dim.fb.w, dim.fb.h);

        cursorScale = computeCursorScale(dim);
        savedWindowSize = getGLWindowSize();
    }

    private CursorScale computeCursorScale(WindowDimensions dim) {
        double scale_x = (double)dim.fb.w / (double)dim.win.w;
        double scale_y = (double)dim.fb.h / (double)dim.win.h;
        return new CursorScale(scale_x, scale_y);
    }

    private WindowDimensions.IntSize fetchFBSize() {
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetFramebufferSize(window, w, h);
        return new WindowDimensions.IntSize(w[0], h[0]);
    }

    private IntBox getGLWindowSize() {
        int[] x = new int[1];
        int[] y = new int[1];
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowPos(window, x, y);
        GLFW.glfwGetWindowSize(window, w, h);
        return new IntBox(x[0], y[0], w[0], h[0]);
    }

    private GLFWWindowSizeCallback sizeCallback = new GLFWWindowSizeCallback() {
        @Override
        public void invoke(long window, int iw, int ih) {
        dim.win.w = iw;
        dim.win.h = ih;
        dim.fb = fetchFBSize();
        cursorScale = computeCursorScale(dim);
        GL33.glViewport(0, 0, dim.fb.w, dim.fb.h);
        resizeRenderers(dim);
        }
    };

    private GLFWCharCallback charCallback = new GLFWCharCallback() {
        @Override
        public void invoke(long window, int codepoint) {
            deliverInputEvent(new InputEvent.KeyTyped(GLWindow.this, inputState, codepoint));
        }
    };

    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
//            if (action == GLFW.GLFW_PRESS) {
//                System.out.println("Pressed " + key);
//            } else {
//                System.out.println("Released " + key);
//            }
            deliverInputEvent(new InputEvent.KeyEvent(GLWindow.this, inputState, key, scancode, action, mods));
        }
    };

    private GLFWMouseButtonCallback buttonCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
        deliverInputEvent(new InputEvent.ButtonEvent(GLWindow.this, inputState, button, action, mods));
        }
    };

    private GLFWCursorPosCallback cursorPosCallback = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long widow, double xpos, double ypos) {
            deliverInputEvent(new InputEvent.MotionEvent(GLWindow.this, inputState,
                    xpos * cursorScale.sx, ypos * cursorScale.sy));
        }
    };

    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double xoffset, double yoffset) {
            deliverInputEvent(new InputEvent.ScrollEvent(GLWindow.this, inputState, xoffset, yoffset));
        }
    };

    private boolean callbackSet = false;
    private void createCallbacks() {
        if (callbackSet) return;
        GLFW.glfwSetWindowSizeCallback(window, sizeCallback);
        GLFW.glfwSetKeyCallback(window, keyCallback);
        GLFW.glfwSetCursorPosCallback(window, cursorPosCallback);
        GLFW.glfwSetMouseButtonCallback(window, buttonCallback);
        GLFW.glfwSetCharCallback(window, charCallback);
        GLFW.glfwSetScrollCallback(window, scrollCallback);
        callbackSet = true;
    }

    private void freeCallbacks() {
        if (!callbackSet) return;
        sizeCallback.free();
        keyCallback.free();
        cursorPosCallback.free();
        buttonCallback.free();
        charCallback.free();
        scrollCallback.free();
        callbackSet = false;
    }



    @Override
    public void destroy() {
        destroyRenderers();
        destroyInputters();
        freeCallbacks();
        if (window != 0) {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
            window = 0;
        }
    }



    @Override
    public void swapBuffers() {
        GLFW.glfwSwapBuffers(window);
    }

    @Override
    public void clearBackground() {
        GL33.glClearColor(1, 0, 0, 1);
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void pollInputs() {
        // Call 10 to 100 times per second
        GLFW.glfwPollEvents();
    }

    @Override
    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    @Override
    public CursorPos getCursorPos() {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window, x, y);
        return new CursorPos(x[0] * cursorScale.sx, y[0] * cursorScale.sy);
    }

    @Override
    public float getAspectRatio() {
        return (float)dim.fb.w / (float)dim.fb.h;
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        float NEAR_PLANE = 0.1f;
        float FAR_PLANE = 300.0f;

        System.out.println("fov=" + Math.toRadians(getFOV()) + " aspect=" + getAspectRatio());

        return new Matrix4f().perspective((float)Math.toRadians(getFOV()), getAspectRatio(), NEAR_PLANE, FAR_PLANE);
    }

    private boolean cursorGrabbed = false;
    @Override
    public void grabCursor(boolean grab) {
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, grab ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
        cursorGrabbed = grab;
    }

    @Override
    public boolean cursorIsGrabbed() {
        return cursorGrabbed;
    }

    @Override
    public boolean isKeyPressed(int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}

