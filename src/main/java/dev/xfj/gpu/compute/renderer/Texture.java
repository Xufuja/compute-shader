package dev.xfj.gpu.compute.renderer;

public class Texture {
    private int handle;
    private int width;
    private int height;

    public Texture() {
        this.handle = 0;
        this.width = 0;
        this.height = 0;
    }

    public int getHandle() {
        return handle;
    }

    public void setHandle(int handle) {
        this.handle = handle;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
