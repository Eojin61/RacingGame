import java.awt.*;

public class Obstacle {
    public int x, y;
    public Image image;

    public Obstacle(int x, int y, Image image) {
        this.x = x;
        this.y = y;
        this.image = image;
    }

    public Obstacle(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 40, 40);
    }
}
