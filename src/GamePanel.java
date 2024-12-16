import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private Timer timer;

    private int carX = 180;
    private int carY = 500;

    private List<Obstacle> obstacles = new ArrayList<>(); // 장애물 리스트
    private boolean isRunning = false;
    private PrintWriter out;

    private Image carImage; // 자신의 자동차 이미지
    private Image opponentCarImage; // 상대방 자동차 이미지
    private Image roadImage; // 도로 이미지

    private String message = "";

    public GamePanel(PrintWriter out, String carImageName) {
        this.out = out;
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        loadImages();

        timer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isRunning) return;

                for (Obstacle obstacle : obstacles) {
                    obstacle.y += 10; // 장애물 이동
                    if (obstacle.y > 600) {
                        obstacle.y = (int) (Math.random() * -600); // 새로운 위치로 리셋
                        obstacle.x = (int) (Math.random() * 300) + 50;
                    }
                }

                out.println("POS:" + carX + "," + carY);

                for (Obstacle obstacle : obstacles) {
                    Rectangle carBounds = new Rectangle(carX, carY, 40, 60); // 자동차 크기: 40x60
                    if (carBounds.intersects(obstacle.getBounds())) {
                        isRunning = false;
                        out.println("COLLISION");
                        displayMessage("충돌 발생! 게임 종료");
                        break;
                    }
                }
                repaint();
            }
        });

        timer.start();
        generateObstacles();

        // 게임 시작 버튼 추가
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

    public synchronized void updateObstacles(List<Obstacle> newObstacles) {
        this.obstacles.clear();
        this.obstacles.addAll(newObstacles);
        repaint();
    }

    public void setOpponentCarImage(String opponentCarImageName) {
        try {
            opponentCarImage = new ImageIcon(getClass().getResource("/image/Player2.png")).getImage();
        } catch (Exception e) {
            System.err.println("상대방 자동차 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }

    private void loadImages() {
        try {
            carImage = new ImageIcon(getClass().getResource("/image/Player1.png")).getImage();

            opponentCarImage = new ImageIcon(getClass().getResource("/image/Player2.png")).getImage();

            roadImage = new ImageIcon(getClass().getResource("/image/road.png")).getImage();
        } catch (Exception e) {
            System.err.println("이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }

    private void generateObstacles() {
        obstacles.clear();
        for (int i = 0; i < 5; i++) {
            int x = (int) (Math.random() * 300) + 50;
            int y = (int) (Math.random() * -600);
        }
    }

    private int opponentX = 0, opponentY = 0; // 상대방 자동차 위치

    public void updateOpponentPosition(int x, int y) {
        this.opponentX = x;
        this.opponentY = y;
        repaint();
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (roadImage != null) {
            g.drawImage(roadImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, 400, 600);
        }

        g.drawImage(carImage, carX, carY, 40, 60, this);

        if (opponentCarImage != null) {
            g.drawImage(opponentCarImage, opponentX, opponentY, 40, 60, this);
        } else {
            g.setColor(Color.RED);
            g.fillRect(opponentX, opponentY, 40, 60);
        }

        for (Obstacle obstacle : obstacles) {
            Image img = new ImageIcon(getClass().getResource("/image/" + obstacle.imageName)).getImage();
            g.drawImage(img, obstacle.x, obstacle.y, 40, 40, this);
        }

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
}