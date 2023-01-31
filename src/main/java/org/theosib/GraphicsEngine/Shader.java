package org.theosib.GraphicsEngine;

import org.theosib.Adaptors.Disposable;
import org.theosib.Utils.FileLocator;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;

public class Shader implements Disposable {
    private int shaderProgram = 0;
    private String fragmentCode = null;
    private String vertexCode = null;

    protected static int compileShader(int shaderType, String code) {
//        System.out.println("Compiling type " + shaderType + " code:" + code);
        int shaderID = GL33.glCreateShader(shaderType);
        GL33.glShaderSource(shaderID, code);
        GL33.glCompileShader(shaderID);
        if (GL33.glGetShaderi(shaderID, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Failed to compile shader: " + code);
        }
        return shaderID;
    }

    protected static int setupShaders(String vertexCode, String fragmentCode) {
        int vertexShader = compileShader(GL33.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = compileShader(GL33.GL_FRAGMENT_SHADER, fragmentCode);

        int shaderProgram = GL33.glCreateProgram();
        GL33.glAttachShader(shaderProgram, vertexShader);
        GL33.glAttachShader(shaderProgram, fragmentShader);

        GL33.glLinkProgram(shaderProgram);
        if (GL33.glGetProgrami(shaderProgram, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Program Linking: " + GL33.glGetProgramInfoLog(shaderProgram));
        }

//        GL33.glValidateProgram(shaderProgram);
//        if (GL33.glGetProgrami(shaderProgram, GL33.GL_VALIDATE_STATUS) == GL33.GL_FALSE) {
//            throw new RuntimeException("Program Validation: " + GL33.glGetProgramInfoLog(shaderProgram));
//        }

        GL33.glDeleteShader(vertexShader);
        GL33.glDeleteShader(fragmentShader);

        return shaderProgram;
    }

    private void compile() {
        if (shaderProgram != 0) return;
        shaderProgram = setupShaders(vertexCode, fragmentCode);
    }

    @Override
    public void destroy() {
        if (shaderProgram != 0) {
            GL33.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }

    public Shader setFragmentCode(String fragmentCode) {
        this.fragmentCode = fragmentCode;
        return this;
    }

    public Shader setFragmentCodeFile(String name) throws IOException {
        String data = FileLocator.getFileAsString(FileLocator.FileCategory.Shader, name);
        return setFragmentCode(data);
    }

    public Shader setVertexCode(String vertexCode) {
        this.vertexCode = vertexCode;
        return this;
    }

    public Shader setVertexCodeFile(String name) throws IOException {
        String data = FileLocator.getFileAsString(FileLocator.FileCategory.Shader, name);
        return setVertexCode(data);
    }

    public void bind() {
        compile();
        GL30.glUseProgram(shaderProgram);
    }

    public static void unbind() {
        GL30.glUseProgram(0);
    }

    public Shader setBool(String name, boolean value) {
        bind();
        GL33.glUniform1i(GL33.glGetUniformLocation(shaderProgram, name), value ? 1 : 0);
        return this;
    }

    public Shader setInt(String name, int value) {
        bind();
        GL33.glUniform1i(GL33.glGetUniformLocation(shaderProgram, name), value);
        return this;
    }

    public Shader setFloat(String name, float value) {
        bind();
        GL33.glUniform1f(GL33.glGetUniformLocation(shaderProgram, name), value);
        return this;
    }

    public Shader setMat4(String name, Matrix4fc value) {
        bind();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL33.glUniformMatrix4fv(GL33.glGetUniformLocation(shaderProgram, name), false,
                    value.get(stack.mallocFloat(16)));
        }
        return this;
    }
}
