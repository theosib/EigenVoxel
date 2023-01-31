package org.theosib.Utils;

public class WindowDimensions {
    public static class IntSize {
        public int w, h;
        public IntSize(int w, int h) {
            this.w = w;
            this.h = h;
        }
        IntSize() {}
    }

    public IntSize fb = new IntSize();
    public IntSize win = new IntSize();
}
