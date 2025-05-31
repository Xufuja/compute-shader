package dev.xfj.gpu.compute.renderer;

public class Framebuffer {
    private int handle;
    private Texture collarAttachment;

    public Framebuffer() {
        this.handle = 0;
        this.collarAttachment = new Texture();
    }

    public int getHandle() {
        return handle;
    }

    public void setHandle(int handle) {
        this.handle = handle;
    }

    public Texture getCollarAttachment() {
        return collarAttachment;
    }

    public void setCollarAttachment(Texture collarAttachment) {
        this.collarAttachment = collarAttachment;
    }
}
