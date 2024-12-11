import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel {
    private Timer timer;

    private int carX = 180;
    private int carY = 500;

    private List<Obstacle> obstacles = new ArrayList<>(); // 장애물 리스트
    private boolean isRunning = false;
    private PrintWriter out;

    private Image carImage;
    private Image roadImage; // 로드 이미지

    private List<Image> obstacleImages = new ArrayList<>(); // 장애물 이미지 리스트

    private String message = "";

    public GamePanel(PrintWriter out, String carImageName) {
        this.out = out;
        this.setFocusable(true);
        loadImages(carImageName);
        timer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isRunning) return;

                for (Obstacle obstacle : obstacles) {
                    obstacle.y += 10;
                    if (obstacle.y > 600) {
                        obstacle.y = (int) (Math.random() * -600);
                        obstacle.x = (int) (Math.random() * 300) + 50;
                        obstacle.image = getRandomObstacleImage(); // 새로운 장애물 이미지 설정
                    }
                }
                out.println("POS:" + carX + "," + carY);
                for (Obstacle obstacle : obstacles) {
                    if (new Rectangle(carX, carY, 40, 40).intersects(obstacle.getBounds())) {
                        isRunning = false;
                        out.println("COLLISION");
                        break;
                    }
                }
                repaint();
            }
        });
        timer.start();
        generateObstacles();
        JButton startButton = new JButton("게임 시작");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.println("START");
                isRunning = true;
                startButton.setEnabled(false);
                GamePanel.this.requestFocusInWindow();
            }
        });
        this.setLayout(new BorderLayout());
        this.add(startButton, BorderLayout.SOUTH);
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isRunning) return;
                if (e.getKeyCode() == KeyEvent.VK_LEFT && carX > 50) {
                    carX -= 10;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && carX < 310) {
                    carX += 10;
                }
                repaint();
            }
        });
    }




    private void loadImages(String carImageName) {
        try {
            // 자동차 이미지 로드
            carImage = new ImageIcon(getClass().getResource("/image/" + carImageName)).getImage();

            // 도로 이미지 로드
            roadImage = new ImageIcon(getClass().getResource("/image/road.png")).getImage();

            // 장애물 이미지 로드
            obstacleImages.add(new ImageIcon(getClass().getResource("/image/obstacle1.png")).getImage());
            obstacleImages.add(new ImageIcon(getClass().getResource("/image/obstacle2.png")).getImage());
        } catch (Exception e) {
            System.err.println("이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }


    private Image getRandomObstacleImage() {
        Random rand = new Random();
        return obstacleImages.get(rand.nextInt(obstacleImages.size()));
    }

    private void generateObstacles() {
        obstacles.clear();
        for (int i = 0; i < 5; i++) {
            int x = (int) (Math.random() * 300) + 50;
            int y = (int) (Math.random() * -600);
            Image randomImage = getRandomObstacleImage();
            obstacles.add(new Obstacle(x, y, randomImage));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 도로 이미지 그리기
        if (roadImage != null) {
            g.drawImage(roadImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // 기본 배경색으로 도로를 채우기
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, 400, 600);
        }

        // 중앙선 그리기
        g.setColor(Color.YELLOW);
        g.fillRect(190, 0, 10, 600);

        // 자동차 그리기
        g.drawImage(carImage, carX, carY, 40, 60, this);

        // 장애물 이미지 그리기
        for (Obstacle obstacle : obstacles) {
            g.drawImage(obstacle.image, obstacle.x, obstacle.y, 40, 40, this);
        }

        // 메시지 출력
        g.setColor(Color.WHITE);
        g.drawString(message, 10, 20);
    }


    public void displayMessage(String message) {
        this.message = message;
        repaint();
    }

    public void startGame() {
        isRunning = true;
        this.requestFocusInWindow();
    }

    public void displayWinner(String resultMessage) {
        JOptionPane.showMessageDialog(this, resultMessage, "게임 결과", JOptionPane.INFORMATION_MESSAGE);
    }
}

class Obstacle {
    public int x, y;
    public Image image;

    public Obstacle(int x, int y, Image image) {
        this.x = x;
        this.y = y;
        this.image = image;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 40, 40);
    }
}
