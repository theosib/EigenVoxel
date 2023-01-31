package org.theosib.GraphicsEngine;

import org.theosib.Adaptors.Disposable;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class RenderDataBuffer implements Disposable {
    FloatBuffer floats = null;

    public FloatBuffer getBuffer() {
        return floats;
    }

    public void flip() {
        floats.flip();
    }

    public FloatBuffer reserveFloats(int num_floats) {
        if (floats == null) {
            floats = MemoryUtil.memAllocFloat(num_floats);
            return floats;
        }
        if (floats.capacity() < num_floats) {
            MemoryUtil.memFree(floats);
            floats = MemoryUtil.memAllocFloat(num_floats);
            return floats;
        }
        floats.limit(num_floats);
        floats.position(0);
        return floats;
    }

    @Override
    public void destroy() {
        if (floats != null) {
            MemoryUtil.memFree(floats);
            floats = null;
        }
    }
}
