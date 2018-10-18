package tutorial0;

import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;
import graphicslib3D.Point3D;
import graphicslib3D.Vector3D;

import static java.awt.event.KeyEvent.*;
import static java.awt.event.KeyEvent.VK_RIGHT;

public class Camera {

    public static final Vector3D FORWARD = new Vector3D(0.0, 0.0, -1.0);
    public static final Vector3D RIGHT = new Vector3D(1.0, 0.0, 0.0);
    public static final Vector3D UP = new Vector3D(0.0, 1.0, 0.0);

    private Point3D position;
    private Matrix3D rotation;

    private Matrix3D projectionMatrix;

    public Camera(float fov, float aspectRatio, float zNear, float zFar) {
        this.position = new Point3D();
        this.rotation = new Matrix3D();
        this.rotation.setToIdentity();

        double q = 1.0 / Math.tan(fov / 2.0);
        double A = q / aspectRatio;
        double B = (zNear + zFar) / (zNear - zFar);
        double C = (2 * zNear * zFar) / (zNear - zFar);

        this.projectionMatrix = new Matrix3D();
        this.projectionMatrix.setToIdentity();
        this.projectionMatrix.setElementAt(0, 0, A);
        this.projectionMatrix.setElementAt(1, 1, q);
        this.projectionMatrix.setElementAt(2, 2, B);
        this.projectionMatrix.setElementAt(2, 3, C);
        this.projectionMatrix.setElementAt(3, 2, -1.0);
    }

    public Matrix3D getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix3D getViewMatrix() {
        Matrix3D matrix = new Matrix3D();
        matrix.setToIdentity();
        matrix.translate(position.getX(), position.getY(), position.getZ());
        matrix.concatenate(rotation);
        return matrix.inverse();
    }

    public Vector3D getForward() {
        return rotation.getCol(2);
    }

    public Vector3D getRight() {
        return rotation.getCol(0);
    }

    public Vector3D getUp() {
        return rotation.getCol(1);
    }

    public Matrix3D getRotation() {
        return rotation;
    }

    public Point3D getPosition() {
        return position;
    }

    public void rotate(float amount, Vector3D axis) {
        rotation.rotate(amount, axis);
    }

    public void move(float amount, Vector3D direction) {
        Point3D point = new Point3D(direction.getX(), direction.getY(), direction.getZ());
        point = point.mult(amount);
        position = position.add(point);
    }

    public void input(boolean[] keyDown) {
        final float moveSpeed = 1.0f;
        final float rotateSpeed = 1.0f;
        if(keyDown[VK_W]) {
            move(-moveSpeed, getForward());
        }
        else if(keyDown[VK_S]) {
            move(moveSpeed, getForward());
        }
        if(keyDown[VK_A]) {
            move(-moveSpeed, getRight());
        }
        else if(keyDown[VK_D]) {
            move(moveSpeed, getRight());
        }
        if(keyDown[VK_SPACE]) {
            move(moveSpeed, getUp());
        }
        else if(keyDown[VK_SHIFT]) {
            move(-moveSpeed, getUp());
        }
        if(keyDown[VK_LEFT]) {
            rotate(rotateSpeed, Camera.UP);
        }
        else if(keyDown[VK_RIGHT]) {
            rotate(-rotateSpeed, Camera.UP);
        }
        if(keyDown[VK_UP]) {
            rotate(rotateSpeed, getRight());
        }
        else if(keyDown[VK_DOWN]) {
            rotate(-rotateSpeed, getRight());
        }
    }
}
