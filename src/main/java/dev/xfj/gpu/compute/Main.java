package dev.xfj.gpu.compute;

import dev.xfj.gpu.compute.renderer.Framebuffer;
import dev.xfj.gpu.compute.renderer.Renderer;
import dev.xfj.gpu.compute.renderer.Texture;
import dev.xfj.gpu.compute.shader.Shader;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;

import java.nio.file.Path;

import static org.joml.Math.cos;
import static org.joml.Math.sin;
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

        Texture sky = Renderer.loadTexture(Path.of("assets", "sky.png"));
        Texture ground = Renderer.loadTexture(Path.of("assets", "snes_smk_mc_2.png"));

        float fWorldX = 1000.4f;
        float fWorldY = 1000.39f;
        float fWorldA = 0.77135f;
        float fNear = 0.026492f;
        float fFar = 0.199961f;
        float fFovHalf = 3.14159f / 4.0f;

        float lastTime = (float) glfwGetTime();

        int fps = 0;
        float secondsTimer = 0.0f;

        while (!glfwWindowShouldClose(window)) {
            float currentTime = (float) glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            secondsTimer += deltaTime;

            if (secondsTimer >= 1.0f) {
                String title = String.format("Compute - %s fps", fps);

                glfwSetWindowTitle(window, title);

                secondsTimer = 0.0f;
                fps = 0;
            }

            glfwGetFramebufferSize(window, width, height);

            if (width[0] != computeShaderTexture.getWidth() || height[0] != computeShaderTexture.getHeight()) {
                GL46.glDeleteTextures(computeShaderTexture.getHandle());
                computeShaderTexture = Renderer.createTexture(width[0], height[0]);
                Renderer.attachTextureToFramebuffer(framebuffer, computeShaderTexture);
            }

            float fFarX1 = fWorldX + cos(fWorldA - fFovHalf) * fFar;
            float fFarY1 = fWorldY + sin(fWorldA - fFovHalf) * fFar;

            float fNearX1 = fWorldX + cos(fWorldA - fFovHalf) * fNear;
            float fNearY1 = fWorldY + sin(fWorldA - fFovHalf) * fNear;

            float fFarX2 = fWorldX + cos(fWorldA + fFovHalf) * fFar;
            float fFarY2 = fWorldY + sin(fWorldA + fFovHalf) * fFar;

            float fNearX2 = fWorldX + cos(fWorldA + fFovHalf) * fNear;
            float fNearY2 = fWorldY + sin(fWorldA + fFovHalf) * fNear;

            fWorldX += cos(fWorldA) * 0.2f * deltaTime;
            fWorldY += sin(fWorldA) * 0.2f * deltaTime;

            float[] near = {fNearX1, fNearY1, fNearX2, fNearY2};
            float[] far = {fFarX1, fFarY1, fFarX2, fFarY2};

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

            GL46.glBindTextureUnit(1, sky.getHandle());
            GL46.glBindTextureUnit(2, ground.getHandle());

            GL46.glUniform4fv(0, near);
            GL46.glUniform4fv(1, far);

            int workGroupSizeX = 16;
            int workGroupSizeY = 16;

            int numGroupsX = (width[0] + workGroupSizeX - 1) / workGroupSizeX;
            int numGroupsY = (height[0] + workGroupSizeY - 1) / workGroupSizeY;

            GL46.glDispatchCompute(numGroupsX, numGroupsY, 1);
            GL46.glMemoryBarrier(GL46.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            Renderer.blitFramebufferToSwapchain(framebuffer);

            glfwSwapBuffers(window);
            glfwPollEvents();

            fps++;
        }

        glfwDestroyWindow(window);
        glfwTerminate();
    }
}