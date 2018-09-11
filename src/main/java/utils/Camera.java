package utils;

import org.joml.*;
import org.joml.Math;

import static java.awt.event.KeyEvent.*;

public class Camera {

    public static final Vector3fc FORWARD = new Vector3f(0.0f, 0.0f, -1.0f);
    public static final Vector3fc RIGHT = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3fc UP = new Vector3f(0.0f, 1.0f, 0.0f);

    private Matrix4f projMatrix;
    private Matrix4f viewMatrix;
    private Vector3f position;
    private Matrix3f rotation;

    public Camera(int width, int height) {
        this.projMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.position = new Vector3f();
        this.rotation = new Matrix3f();
        this.projMatrix.identity();
        this.projMatrix.perspective(45.0f, (float) width / (float) height, 0.1f, 1000.0f);
    }

    public Matrix4fc getProjMatrix() {
        return projMatrix;
    }

    public Matrix4fc getViewMatrix() {
        return viewMatrix;
    }

    public void setRotation(Matrix3fc rotation) {
        this.rotation.set(rotation);
    }

    public Matrix3fc getRotation() {
        return rotation;
    }

    public void setPosition(Vector3fc position) {
        this.position.set(position);
    }

    public Vector3fc getPosition() {
        return position;
    }

    public void pitch(float angle) {
        rotation.rotate(angle, RIGHT);
    }

    public void pitchLocal(float angle) {
        rotation.rotateLocal(angle, RIGHT.x(), RIGHT.y(), RIGHT.z());
    }

    public void yaw(float angle) {
        rotation.rotate(angle, UP);
    }

    public void yawLocal(float angle) {
        rotation.rotateLocal(angle, UP.x(), UP.y(), UP.z());
    }

    public void rotate(float amount, Vector3fc axis) {
        rotation.rotate(amount, axis);
    }

    public Vector3fc getForward() {
        return rotation.getColumn(2, new Vector3f());
    }

    public Vector3fc getRight() {
        return rotation.getColumn(0, new Vector3f());
    }

    public Vector3fc getUp() {
        return rotation.getColumn(1, new Vector3f());
    }

    public void moveForward(float amount) {
        position.add(getForward().mul(amount, new Vector3f()));
    }

    public void moveRight(float amount) {
        position.add(getRight().mul(amount, new Vector3f()));
    }

    public void moveUp(float amount) {
        position.add(getUp().mul(amount, new Vector3f()));
    }

    public void move(float amount, Vector3fc direction) {
        position.add(direction.mul(amount, new Vector3f()));
    }

    public void update() {
        viewMatrix.identity()
                .translate(position)
                .mul(new Matrix4f().identity().set(rotation))
                .invert();
    }

    private static final float speed = 0.1f;
    private static final float rotSpeed = 0.025f;
    public void input(boolean[] keyDown) {
        if(keyDown[VK_W]) {
            moveForward(-speed);
        }
        if(keyDown[VK_S]) {
            moveForward(speed);
        }
        if(keyDown[VK_SPACE]) {
            move(speed, UP);
        }
        if(keyDown[VK_SHIFT]) {
            move(-speed, UP);
        }
        if(keyDown[VK_A]) {
            moveRight(-speed);
        }
        if(keyDown[VK_D]) {
            moveRight(speed);
        }
        if(keyDown[VK_LEFT]) {
            yawLocal(rotSpeed);
        }
        if(keyDown[VK_RIGHT]) {
            yawLocal(-rotSpeed);
        }
        if(keyDown[VK_UP]) {
            pitch(rotSpeed);
        }
        if(keyDown[VK_DOWN]) {
            pitch(-rotSpeed);
        }
    }
}