package tutorial3;

import org.joml.*;

import static java.awt.event.KeyEvent.*;

public class Camera {

    public static final Vector3fc RIGHT = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3fc UP = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3fc FORWARD = new Vector3f(0.0f, 0.0f, -1.0f);

    private Matrix4f projMatrix;
    private Matrix4f viewMatrix;
    private Vector3f position;
    private Matrix3f rotation;
    private float moveSpeed;
    private float rotateSpeed;

    public Camera() {
        this.projMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.position = new Vector3f();
        this.rotation = new Matrix3f();
        this.moveSpeed = 0.1f;
        this.rotateSpeed = 0.025f;
    }

    public void initialize(int width, int height) {
        this.projMatrix.identity();
        this.projMatrix.perspective(45.0f, (float) width / (float) height, 0.1f, 2000.0f);
    }

    public Matrix4fc getProjMatrix() {
        return projMatrix;
    }

    private Matrix4f tempMatrix = new Matrix4f();
    public Matrix4fc getViewMatrix() {
        return viewMatrix.identity()
                    .translate(position)
                    .mul(tempMatrix.identity().set(rotation))
                    .invert();
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

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    public float getRotateSpeed() {
        return rotateSpeed;
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

    public void input(boolean[] keyDown) {
        if(keyDown[VK_W]) {
            moveForward(-moveSpeed);
        }
        else if(keyDown[VK_S]) {
            moveForward(moveSpeed);
        }
        if(keyDown[VK_SPACE]) {
            move(moveSpeed, UP);
        }
        else if(keyDown[VK_SHIFT]) {
            move(-moveSpeed, UP);
        }
        if(keyDown[VK_A]) {
            moveRight(-moveSpeed);
        }
        else if(keyDown[VK_D]) {
            moveRight(moveSpeed);
        }
        if(keyDown[VK_LEFT]) {
            yawLocal(rotateSpeed);
        }
        else if(keyDown[VK_RIGHT]) {
            yawLocal(-rotateSpeed);
        }
        if(keyDown[VK_UP]) {
            pitch(rotateSpeed);
        }
        else if(keyDown[VK_DOWN]) {
            pitch(-rotateSpeed);
        }
    }
}