import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer timer; // 게임의 주기적 업데이트를 위한 타이머
    private int carX = 180, carY = 500; // 플레이어 자동차의 초기 위치
    private List<Rectangle> obstacles = new ArrayList<>(); // 장애물 리스트
    private Map<String, String> otherPlayerPositions = new HashMap<>(); // 다른 플레이어 위치
    private boolean running = false; // 게임이 진행 중인지 여부
    private PrintWriter out; // 서버로 메시지를 전송하기 위한 출력 스트림
    private Image carImage; // 플레이어 자동차 이미지
    private String message = ""; // 게임 화면에 표시할 메시지

    public GamePanel(PrintWriter out, String carImageName) {
        this.out = out;
        this.setFocusable(true);
        this.addKeyListener(this); // 키 입력 리스너 추가
        loadCarImage(carImageName); // 자동차 이미지 로드
        timer = new Timer(50, this); // 50ms 간격으로 타이머 이벤트 실행
        timer.start();
        generateObstacles(); // 장애물 생성

        // "게임 시작" 버튼
        JButton startButton = new JButton("게임 시작");
        startButton.addActionListener(e -> {
            out.println("START"); // 서버에 게임 시작 신호 전송
            running = true; // 게임 상태 변경
            startButton.setEnabled(false); // 버튼 비활성화
            this.requestFocusInWindow(); // 키보드 포커스 설정
        });
        this.setLayout(new BorderLayout());
        this.add(startButton, BorderLayout.SOUTH); // 버튼을 화면 하단에 추가
    }

    private void loadCarImage(String carImageName) {
        // 자동차 이미지를 src/image 디렉토리에서 로드
        carImage = new ImageIcon("src/image/" + carImageName).getImage();
    }

    private void generateObstacles() {
        // 장애물 초기화 및 생성
        obstacles.clear();
        for (int i = 0; i < 5; i++) { // 장애물 5개 생성
            int x = (int) (Math.random() * 300) + 50; // 도로 범위 내 x 좌표
            int y = (int) (Math.random() * -600); // 화면 위쪽에 y 좌표
            obstacles.add(new Rectangle(x, y, 40, 40)); // 장애물 크기 40x40
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) return; // 게임이 진행 중이 아니면 실행하지 않음

        // 장애물 이동 처리
        for (Rectangle obstacle : obstacles) {
            obstacle.y += 10; // 장애물을 아래로 이동
            if (obstacle.y > 600) {
                // 화면을 벗어난 장애물을 위쪽으로 재생성
                obstacle.y = (int) (Math.random() * -600);
                obstacle.x = (int) (Math.random() * 300) + 50;
            }
        }

        // 자신의 자동차 위치를 서버에 전송
        out.println("POS:" + carX + "," + carY);

        // 충돌 감지
        for (Rectangle obstacle : obstacles) {
            if (new Rectangle(carX, carY, 40, 40).intersects(obstacle)) {
                running = false; // 게임 종료
                out.println("COLLISION"); // 서버에 충돌 메시지 전송
                break;
            }
        }

        repaint(); // 화면 갱신
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 도로 배경 그리기
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 400, 600);

        // 도로 중앙선 그리기
        g.setColor(Color.YELLOW);
        g.fillRect(190, 0, 10, 600);

        // 플레이어 자동차 그리기
        g.drawImage(carImage, carX, carY, 40, 60, this);

        // 장애물 그리기
        g.setColor(Color.BLACK);
        for (Rectangle obstacle : obstacles) {
            g.fillRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);
        }

        // 다른 플레이어 자동차 그리기
        g.setColor(Color.BLUE);
        for (String pos : otherPlayerPositions.values()) {
            String[] coords = pos.split(",");
            int otherX = Integer.parseInt(coords[0]);
            int otherY = Integer.parseInt(coords[1]);
            g.fillRect(otherX, otherY, 40, 60); // 다른 플레이어 자동차 크기
        }

        // 메시지 표시
        g.setColor(Color.WHITE);
        g.drawString(message, 10, 20);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!running) return; // 게임이 진행 중이 아니면 실행하지 않음

        // 자동차 이동 처리
        if (e.getKeyCode() == KeyEvent.VK_LEFT && carX > 50) {
            carX -= 10; // 왼쪽으로 이동
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && carX < 310) {
            carX += 10; // 오른쪽으로 이동
        }
        repaint(); // 화면 갱신
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public void displayMessage(String message) {
        // 메시지를 설정하고 다시 그리기
        this.message = message;
        repaint();
    }

    public void startGame() {
        // 게임 시작 상태 설정 및 키보드 포커스 확보
        running = true;
        this.requestFocusInWindow();
    }

    public void displayWinner(String resultMessage) {
        // 게임 결과 메시지를 표시하는 화면으로 변경
        JOptionPane.showMessageDialog(this, resultMessage, "게임 결과", JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateOtherPlayerPosition(String playerName, String position) {
        // 다른 플레이어의 위치 정보를 업데이트
        otherPlayerPositions.put(playerName, position);
        repaint(); // 화면 갱신
    }
}