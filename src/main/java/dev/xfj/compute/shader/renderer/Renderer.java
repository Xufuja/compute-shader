package dev.xfj.compute.shader.renderer;

import org.lwjgl.opengl.GL46;

public class Renderer {
    public static Texture createTexture(int width, int height) {
        Texture result = new Texture();
        result.setWidth(width);
        result.setHeight(height);

        result.setHandle(GL46.glCreateTextures(GL46.GL_TEXTURE_2D));
        GL46.glTextureStorage2D(result.getHandle(), 1, GL46.GL_RGBA32F, width, height);

        GL46.glTextureParameteri(result.getHandle(), GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        GL46.glTextureParameteri(result.getHandle(), GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);

        GL46.glTextureParameteri(result.getHandle(), GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_EDGE);
        GL46.glTextureParameteri(result.getHandle(), GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_EDGE);

        return result;
    }

    public static Framebuffer createFramebufferWithTexture(Texture texture) {
        Framebuffer result = new Framebuffer();
        result.setHandle(GL46.glCreateFramebuffers());

        if (!attachTextureToFramebuffer(result, texture)) {
            GL46.glDeleteFramebuffers(result.getHandle());

            return new Framebuffer();
        }

        return result;
    }

    public static boolean attachTextureToFramebuffer(Framebuffer framebuffer, Texture texture) {
        GL46.glNamedFramebufferTexture(
                framebuffer.getHandle(),
                GL46.GL_COLOR_ATTACHMENT0,
                texture.getHandle(),
                0
        );

        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Framebuffer is not complete!");

            return false;
        }

        framebuffer.setCollarAttachment(texture);
        return true;
    }

    public static void blitFramebufferToSwapchain(Framebuffer framebuffer) {
        GL46.glBindFramebuffer(GL46.GL_READ_FRAMEBUFFER, framebuffer.getHandle());
        GL46.glBindFramebuffer(GL46.GL_DRAW_FRAMEBUFFER, 0);
        GL46.glBlitFramebuffer(
                0,
                0,
                framebuffer.getCollarAttachment().getWidth(),
                framebuffer.getCollarAttachment().getHeight(),
                0,
                0,
                framebuffer.getCollarAttachment().getWidth(),
                framebuffer.getCollarAttachment().getHeight(),
                GL46.GL_COLOR_BUFFER_BIT,
                GL46.GL_NEAREST
        );
    }
}
