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
    private static final Path COMPUTE_SHADER_PATH = Path.of("shaders/compute.glsl");
    private static final Path VERTEX_SHADER_PATH = Path.of("shaders/vertex.glsl");
    private static final Path FRAGMENT_SHADER_PATH = Path.of("shaders/fragment.glsl");

    private static boolean compute = false;
    private static int computeShader = -1;
    private static int graphicsShader = -1;

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
                            computeShader = Shader.reloadComputeShader(computeShader, COMPUTE_SHADER_PATH);
                            graphicsShader = Shader.reloadGraphicsShader(
                                    graphicsShader,
                                    VERTEX_SHADER_PATH,
                                    FRAGMENT_SHADER_PATH
                            );
                        }

                        if (key == GLFW_KEY_S) {
                            compute = !compute;
                        }
                    }
                }
            }
        });

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        computeShader = Shader.createComputeShader(COMPUTE_SHADER_PATH);

        if (computeShader == -1) {
            throw new RuntimeException("Compute shader failed!");
        }

        graphicsShader = Shader.createGraphicsShaders(VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);

        if (graphicsShader == -1) {
            throw new RuntimeException("Graphics shader failed!");
        }

        Texture computeShaderTexture = Renderer.createTexture(width[0], height[0]);
        Framebuffer framebuffer = Renderer.createFramebufferWithTexture(computeShaderTexture);

        Texture sky = Renderer.loadTexture(Path.of("assets", "sky.png"));
        Texture ground = Renderer.loadTexture(Path.of("assets", "snes_smk_mc_2.png"));

        int vertexArray = GL46.glCreateVertexArrays();
        int vertexBuffer = GL46.glCreateBuffers();

        boolean triangle = true;

        if (!triangle) {
            int indexBuffer = GL46.glCreateBuffers();

            float[] vertices = {
                    -1.0f, -1.0f, 0.0f, 0.0f,
                    1.0f, -1.0f, 1.0f, 0.0f,
                    -1.0f, 1.0f, 0.0f, 1.0f,
                    1.0f, 1.0f, 1.0f, 1.0f,
            };

            int[] indices = {0, 1, 2, 3};

            GL46.glNamedBufferData(vertexBuffer, vertices, GL46.GL_STATIC_DRAW);
            GL46.glNamedBufferData(indexBuffer, indices, GL46.GL_STATIC_DRAW);

            GL46.glVertexArrayElementBuffer(vertexArray, indexBuffer);
        } else {
            float[] vertices = {
                    -1.0f, -1.0f, 0.0f, 0.0f,
                    3.0f, -1.0f, 2.0f, 0.0f,
                    -1.0f, 3.0f, 0.0f, 2.0f
            };

            GL46.glNamedBufferData(vertexBuffer, vertices, GL46.GL_STATIC_DRAW);
        }

        GL46.glVertexArrayVertexBuffer(vertexArray, 0, vertexBuffer, 0, Float.BYTES * 4);

        GL46.glEnableVertexArrayAttrib(vertexArray, 0);
        GL46.glEnableVertexArrayAttrib(vertexArray, 1);

        GL46.glVertexArrayAttribFormat(
                vertexArray,
                0,
                2,
                GL46.GL_FLOAT,
                false,
                0
        );
        GL46.glVertexArrayAttribFormat(
                vertexArray,
                1,
                2,
                GL46.GL_FLOAT,
                false,
                Float.BYTES * 2
        );

        GL46.glVertexArrayAttribBinding(vertexArray, 0, 0);
        GL46.glVertexArrayAttribBinding(vertexArray, 1, 0);

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
                String title = String.format("%s - %s fps", compute ? "Compute" : "Graphics", fps);

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

            float[] near = {fNearX1, fNearY1, fNearX2, fNearY2};
            float[] far = {fFarX1, fFarY1, fFarX2, fFarY2};

            fWorldX += cos(fWorldA) * 0.2f * deltaTime;
            fWorldY += sin(fWorldA) * 0.2f * deltaTime;

            if (compute) {
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
            } else {
                GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer.getHandle());
                GL46.glUseProgram(graphicsShader);

                GL46.glBindTextureUnit(1, sky.getHandle());
                GL46.glBindTextureUnit(2, ground.getHandle());

                GL46.glUniform4fv(0, near);
                GL46.glUniform4fv(1, far);

                GL46.glBindVertexArray(vertexArray);

                if (triangle) {
                    GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, 3);
                } else {
                    GL46.glDrawElements(GL46.GL_TRIANGLE_STRIP, 4, GL46.GL_UNSIGNED_INT, NULL);
                }

                GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0);
            }

            Renderer.blitFramebufferToSwapchain(framebuffer);

            glfwSwapBuffers(window);
            glfwPollEvents();

            fps++;
        }

        glfwDestroyWindow(window);
        glfwTerminate();
    }
}