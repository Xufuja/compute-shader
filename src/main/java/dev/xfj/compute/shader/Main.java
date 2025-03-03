package dev.xfj.compute.shader;

import dev.xfj.compute.shader.renderer.Framebuffer;
import dev.xfj.compute.shader.renderer.Renderer;
import dev.xfj.compute.shader.renderer.Texture;
import dev.xfj.compute.shader.shader.Shader;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;

import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {
    private static int computeShader = -1;
    private static Path computeShaderPath = Path.of("shaders/compute.glsl");

    public static void main(String[] args) {
        glfwSetErrorCallback(new GLFWErrorCallback() {
            @Override
            public void invoke(int error, long description) {
                System.err.println(String.format("Error: %s", description));
            }
        });

        if (!glfwInit()) {
            throw new RuntimeException("Could not initialize GLFW!");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);

        int[] width = {1280};
        int[] height = {720};

        long window = glfwCreateWindow(width[0], height[0], "Compute", NULL, NULL);

        if (window == 0) {
            glfwTerminate();
            throw new RuntimeException("Could not initialize window!");
        }

        glfwSetKeyCallback(window, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scanCode, int action, int mods) {
                switch (action) {
                    case GLFW_PRESS -> {
                        if (key == GLFW_KEY_ESCAPE) {
                            glfwSetWindowShouldClose(window, true);
                        }
                    }
                    case GLFW_RELEASE -> {
                        if (key == GLFW_KEY_R) {
                            computeShader = Shader.reloadComputeShader(computeShader, computeShaderPath);
                        }
                    }
                }
            }
        });

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        computeShader = Shader.createComputeShader(computeShaderPath);

        if (computeShader == -1) {
            throw new RuntimeException("Could not initialize shader!");
        }

        Texture computeShaderTexture = Renderer.createTexture(width[0], height[0]);
        Framebuffer framebuffer = Renderer.createFramebufferWithTexture(computeShaderTexture);

        while (!glfwWindowShouldClose(window)) {
            glfwGetFramebufferSize(window, width, height);

            if (width[0] != computeShaderTexture.getWidth() || height[0] != computeShaderTexture.getHeight()) {
                GL46.glDeleteTextures(computeShaderTexture.getHandle());
                computeShaderTexture = Renderer.createTexture(width[0], height[0]);
                Renderer.attachTextureToFramebuffer(framebuffer, computeShaderTexture);
            }

            GL46.glUseProgram(computeShader);
            GL46.glBindImageTexture(
                    0,
                    framebuffer.getCollarAttachment().getHandle(),
                    0,
                    false,
                    0,
                    GL46.GL_WRITE_ONLY,
                    GL46.GL_RGBA32F
            );

            int workGroupSizeX = 16;
            int workGroupSizeY = 16;

            int numGroupsX = (width[0] + workGroupSizeX - 1) / workGroupSizeX;
            int numGroupsY = (height[0] + workGroupSizeY - 1) / workGroupSizeY;

            GL46.glDispatchCompute(numGroupsX, numGroupsY, 1);
            GL46.glMemoryBarrier(GL46.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            Renderer.blitFramebufferToSwapchain(framebuffer);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        glfwDestroyWindow(window);
        glfwTerminate();
    }
}