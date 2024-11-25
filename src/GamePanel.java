import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamePanel extends JPanel {
    private Timer timer;

    private int carX = 180;
    private int carY = 500;

    private List<Rectangle> obstacles = new ArrayList<>();
    private Map<String, String> otherPlayerPositions = new HashMap<>();

    private boolean isRunning = false;
    private PrintWriter out;

    private Image carImage;

    private String message = "";

    public GamePanel(PrintWriter out, String carImageName) {
        this.out = out;
        this.setFocusable(true);
        loadCarImage(carImageName);
        timer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isRunning) return;

                for (Rectangle obstacle : obstacles) {
                    obstacle.y += 10;
                    if (obstacle.y > 600) {
                        obstacle.y = (int) (Math.random() * -600);
                        obstacle.x = (int) (Math.random() * 300) + 50;
                    }
                }
                out.println("POS:" + carX + "," + carY);
                for (Rectangle obstacle : obstacles) {
                    if (new Rectangle(carX, carY, 40, 40).intersects(obstacle)) {
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

    private void loadCarImage(String carImageName) {
        try {
            carImage = new ImageIcon(getClass().getResource("/image/" + carImageName)).getImage();
        } catch (Exception e) {
            System.err.println("이미지를 로드할 수 없습니다: " + carImageName);
        }
    }

    private void generateObstacles() {
        obstacles.clear();
        for (int i = 0; i < 5; i++) {
            int x = (int) (Math.random() * 300) + 50;
            int y = (int) (Math.random() * -600);
            obstacles.add(new Rectangle(x, y, 40, 40));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 400, 600);

        g.setColor(Color.YELLOW);
        g.fillRect(190, 0, 10, 600);

        g.drawImage(carImage, carX, carY, 40, 60, this);

        g.setColor(Color.BLACK);
        for (Rectangle obstacle : obstacles) {
            g.fillRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);
        }

        g.setColor(Color.BLUE);
        for (String pos : otherPlayerPositions.values()) {
            String[] coords = pos.split(",");
            int otherX = Integer.parseInt(coords[0]);
            int otherY = Integer.parseInt(coords[1]);
            g.fillRect(otherX, otherY, 40, 60);
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

    public void displayWinner(String resultMessage) {
        JOptionPane.showMessageDialog(this, resultMessage, "게임 결과", JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateOtherPlayerPosition(String playerName, String position) {
        otherPlayerPositions.put(playerName, position);
        repaint();
    }
}