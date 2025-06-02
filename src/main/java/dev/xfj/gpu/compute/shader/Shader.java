package dev.xfj.gpu.compute.shader;

import org.lwjgl.opengl.GL46;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

public class Shader {
    public static String readTextFile(Path path) {
        String shaderSource = "";

        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] bytes = inputStream.readAllBytes();
            shaderSource = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to open file: " + path);
            return shaderSource;
        }

        return shaderSource;
    }

    public static int createComputeShader(Path path) {
        String shaderSource = readTextFile(path);

        int shaderHandle = GL46.glCreateShader(GL46.GL_COMPUTE_SHADER);

        GL46.glShaderSource(shaderHandle, shaderSource);
        GL46.glCompileShader(shaderHandle);

        int[] isCompiled = new int[1];
        GL46.glGetShaderiv(shaderHandle, GL46.GL_COMPILE_STATUS, isCompiled);

        if (isCompiled[0] == GL46.GL_FALSE) {
            int[] maxLength = new int[1];
            GL46.glGetShaderiv(shaderHandle, GL_INFO_LOG_LENGTH, maxLength);

            String infoLog = GL46.glGetShaderInfoLog(shaderHandle, maxLength[0]);
            System.err.println(infoLog);

            GL46.glDeleteShader(shaderHandle);
            return -1;
        }

        int program = GL46.glCreateProgram();

        GL46.glAttachShader(program, shaderHandle);
        GL46.glLinkProgram(program);

        int[] isLinked = new int[1];
        GL46.glGetProgramiv(program, GL_LINK_STATUS, isLinked);

        if (isLinked[0] == GL_FALSE) {
            int[] maxLength = new int[1];
            GL46.glGetProgramiv(program, GL_INFO_LOG_LENGTH, maxLength);

            String infoLog = glGetProgramInfoLog(program, maxLength[0]);
            System.err.println(infoLog);

            GL46.glDeleteProgram(program);
            GL46.glDeleteShader(shaderHandle);

            return -1;
        }

        GL46.glDetachShader(program, shaderHandle);
        return program;
    }

    public static int reloadComputeShader(int shaderHandle, Path path) {
        int newShaderHandle = createComputeShader(path);

        if (newShaderHandle == -1) {
            return shaderHandle;
        }

        GL46.glDeleteProgram(shaderHandle);
        return newShaderHandle;
    }

    public static int createGraphicsShaders(Path vertexPath, Path fragmentPath) {
        String vertexShaderSource = readTextFile(vertexPath);
        String fragmentShaderSource = readTextFile(fragmentPath);

        int vertexShaderHandle = GL46.glCreateShader(GL46.GL_VERTEX_SHADER);

        GL46.glShaderSource(vertexShaderHandle, vertexShaderSource);
        GL46.glCompileShader(vertexShaderHandle);

        int[] isCompiled = new int[1];
        GL46.glGetShaderiv(vertexShaderHandle, GL46.GL_COMPILE_STATUS, isCompiled);

        if (isCompiled[0] == GL46.GL_FALSE) {
            int[] maxLength = new int[1];
            GL46.glGetShaderiv(vertexShaderHandle, GL_INFO_LOG_LENGTH, maxLength);

            String infoLog = GL46.glGetShaderInfoLog(vertexShaderHandle, maxLength[0]);
            System.err.println(infoLog);

            GL46.glDeleteShader(vertexShaderHandle);
            return -1;
        }

        int fragmentShaderHandle = GL46.glCreateShader(GL46.GL_FRAGMENT_SHADER);

        GL46.glShaderSource(fragmentShaderHandle, fragmentShaderSource);
        GL46.glCompileShader(fragmentShaderHandle);

        isCompiled[0] = 0;
        GL46.glGetShaderiv(fragmentShaderHandle, GL46.GL_COMPILE_STATUS, isCompiled);

        if (isCompiled[0] == GL46.GL_FALSE) {
            int[] maxLength = new int[1];
            GL46.glGetShaderiv(fragmentShaderHandle, GL_INFO_LOG_LENGTH, maxLength);

            String infoLog = GL46.glGetShaderInfoLog(fragmentShaderHandle, maxLength[0]);
            System.err.println(infoLog);

            GL46.glDeleteShader(fragmentShaderHandle);
            return -1;
        }

        int program = GL46.glCreateProgram();

        GL46.glAttachShader(program, vertexShaderHandle);
        GL46.glAttachShader(program, fragmentShaderHandle);
        GL46.glLinkProgram(program);

        int[] isLinked = new int[1];
        GL46.glGetProgramiv(program, GL_LINK_STATUS, isLinked);

        if (isLinked[0] == GL_FALSE) {
            int[] maxLength = new int[1];
            GL46.glGetProgramiv(program, GL_INFO_LOG_LENGTH, maxLength);

            String infoLog = glGetProgramInfoLog(program, maxLength[0]);
            System.err.println(infoLog);

            GL46.glDeleteProgram(program);
            GL46.glDeleteShader(vertexShaderHandle);
            GL46.glDeleteShader(fragmentShaderHandle);

            return -1;
        }

        GL46.glDetachShader(program, vertexShaderHandle);
        return program;
    }

    public static int reloadGraphicsShader(int shaderHandle, Path vertexPath, Path fragmentPath) {
        int newShaderHandle = createGraphicsShaders(vertexPath, fragmentPath);

        if (newShaderHandle == -1) {
            return shaderHandle;
        }

        GL46.glDeleteProgram(shaderHandle);
        return newShaderHandle;
    }
}
