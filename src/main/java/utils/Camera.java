package utils;

import org.joml.*;

import static java.awt.event.KeyEvent.*;

public class Camera {

    private Matrix4f projMatrix;
    private Matrix4f viewMatrix;
    private Vector3f position;
    private Quaternionf rotation;

    public Camera(int width, int height) {
        this.projMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.position = new Vector3f();
        this.rotation = new Quaternionf();
        this.projMatrix.identity();
        this.projMatrix.perspective(45.0f, (float) width / (float) height, 0.1f, 1000.0f);
    }

    public Matrix4fc getProjMatrix() {
        return projMatrix;
    }

    public Matrix4fc getViewMatrix() {
        return viewMatrix;
    }

    public void setPosition(Vector3fc position) {
        this.position.set(position);
    }

    public Vector3fc getPosition() {
        return position;
    }

    public void pitch(float pitch) {
        rotation.rotateX(pitch);
    }

    public void yaw(float yaw) {
        rotation.rotateY(yaw);
    }

    public Vector3fc getForward() {
        return rotation.conjugate(new Quaternionf()).transform(new Vector3f(0.0f, 0.0f, 1.0f));
    }

    public Vector3fc getLeft() {
        return rotation.conjugate(new Quaternionf()).transform(new Vector3f(1.0f, 0.0f, 0.0f));
    }

    public Vector3fc getUp() {
        return rotation.conjugate(new Quaternionf()).transform(new Vector3f(0.0f, -1.0f, 0.0f));
    }

    public void moveForward(float amount) {
        position.add(getForward().mul(amount, new Vector3f()));
    }

    public void moveLeft(float amount) {
        position.add(getLeft().mul(amount, new Vector3f()));
    }

    public void moveUp(float amount) {
        position.add(getUp().mul(amount, new Vector3f()));
    }

    public void update() {
        viewMatrix.identity();
        viewMatrix.rotate(rotation);
        viewMatrix.translate(position);
    }

    private static final float speed = 0.1f;
    private static final float rotSpeed = 0.025f;
    public void input(boolean[] keyDown) {
        if(keyDown[VK_W]) {
            moveForward(speed);
        }
        if(keyDown[VK_S]) {
            moveForward(-speed);
        }
        if(keyDown[VK_SPACE]) {
            moveUp(speed);
        }
        if(keyDown[VK_SHIFT]) {
            moveUp(-speed);
        }
        if(keyDown[VK_Q]) {
            moveLeft(speed);
        }
        if(keyDown[VK_E]) {
            moveLeft(-speed);
        }
        if(keyDown[VK_A]) {
            yaw(-rotSpeed);
        }
        if(keyDown[VK_D]) {
            yaw(rotSpeed);
        }
    }
}
