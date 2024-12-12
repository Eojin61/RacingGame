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
    private Image obstacleImage; // 고정된 장애물 이미지

    private String message = "";

    public GamePanel(PrintWriter out, String carImageName) {
        this.out = out;
        this.setDoubleBuffered(true); // 더블 버퍼링 활성화
        this.setFocusable(true);

        loadImages(); // 자동차 및 도로 이미지 로드

        timer = new Timer(100, new ActionListener() { // 장애물 업데이트 주기: 100ms
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

                // 위치 정보 전송
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
                repaint(); // 화면 갱신
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

        // 키 입력 리스너 추가
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
        this.obstacles.addAll(newObstacles); // 서버에서 받은 장애물 업데이트
        repaint();
    }

    public void setOpponentCarImage(String opponentCarImageName) {
        // 상대방 자동차 이미지를 고정된 Player2.png로 설정
        try {
            opponentCarImage = new ImageIcon(getClass().getResource("/image/Player2.png")).getImage();
        } catch (Exception e) {
            System.err.println("상대방 자동차 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }

    private void loadImages() {
        try {
            // 자신의 자동차 이미지를 고정된 Player1.png로 설정
            carImage = new ImageIcon(getClass().getResource("/image/Player1.png")).getImage();

            // 상대방 자동차 이미지는 기본적으로 Player2.png로 설정
            opponentCarImage = new ImageIcon(getClass().getResource("/image/Player2.png")).getImage();

            // 도로 이미지 로드
            roadImage = new ImageIcon(getClass().getResource("/image/road.png")).getImage();

            // 고정된 장애물 이미지 로드
            obstacleImage = new ImageIcon(getClass().getResource("/image/obstacle1.png")).getImage();
        } catch (Exception e) {
            System.err.println("이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }

    private void generateObstacles() {
        obstacles.clear();
        for (int i = 0; i < 5; i++) {
            int x = (int) (Math.random() * 300) + 50;
            int y = (int) (Math.random() * -600);
            obstacles.add(new Obstacle(x, y, obstacleImage));
        }
    }

    private int opponentX = 0, opponentY = 0; // 상대방 자동차 위치

    public void updateOpponentPosition(int x, int y) {
        this.opponentX = x;
        this.opponentY = y;
        repaint(); // 화면 다시 그리기
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 도로 이미지 그리기
        if (roadImage != null) {
            g.drawImage(roadImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // 기본 배경색으로 도로를 채우기
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, 400, 600);
        }

        // 자동차 그리기
        g.drawImage(carImage, carX, carY, 40, 60, this);

        // 상대방 자동차 그리기 (이미지로 표시)
        if (opponentCarImage != null) {
            g.drawImage(opponentCarImage, opponentX, opponentY, 40, 60, this);
        } else {
            // 상대방 자동차 이미지가 없는 경우 기본 빨간색 사각형으로 표시
            g.setColor(Color.RED);
            g.fillRect(opponentX, opponentY, 40, 60);
        }

        // 장애물 이미지 그리기
        for (Obstacle obstacle : obstacles) {
            if (obstacle.image != null) {
                g.drawImage(obstacle.image, obstacle.x, obstacle.y, 40, 40, this);
            } else {
                // 이미지가 없는 경우 기본 사각형으로 표시
                g.setColor(Color.RED);
                g.fillRect(obstacle.x, obstacle.y, 40, 40);
            }
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