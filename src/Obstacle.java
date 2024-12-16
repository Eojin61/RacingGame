import java.awt.*;

public class Obstacle {
    public int x, y;
    public Image image;
    public String imageName; // 이미지 파일 이름

    public Obstacle(int x, int y, String imageName) {
        this.x = x;
        this.y = y;
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 40, 40);
    }
}
