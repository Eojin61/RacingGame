import java.io.*;
import java.net.*;
import javax.swing.*;

public class RacingClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame;
    private GamePanel gamePanel;
    private String carImageName;

    public void start() {
        try {
            // 서버와 연결
            socket = new Socket("localhost", 30000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 서버 메시지 처리 스레드 시작
            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupLoginUI() {
        // 로그인 화면 생성
        frame = new JFrame("Racing Game - Login");
        JTextField nameField = new JTextField(15);
        JButton loginButton = new JButton("로그인");

        loginButton.addActionListener(e -> {
            String playerName = nameField.getText().trim();
            if (!playerName.isEmpty()) {
                out.println(playerName); // 서버에 이름 전송
                frame.dispose(); // 로그인 화면 닫기
            }
        });

        JPanel panel = new JPanel();
        panel.add(new JLabel("이름:"));
        panel.add(nameField);
        panel.add(loginButton);

        frame.add(panel);
        frame.setSize(300, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void setupGameUI(String carImageName) {
        // 게임 화면 생성
        frame = new JFrame("Racing Game");
        gamePanel = new GamePanel(out, carImageName);
        frame.add(gamePanel);
        frame.setSize(400, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    // 서버 메시지 처리
                    if (message.equals("ENTER_NAME")) {
                        // 서버에서 이름 입력 요청 시 로그인 UI 표시
                        setupLoginUI();
                    } else if (message.startsWith("CAR_IMAGE:")) {
                        // 서버에서 자동차 이미지 정보를 수신
                        carImageName = message.split(":")[1];
                        setupGameUI(carImageName); // 게임 UI 설정
                    } else if (message.startsWith("START_GAME")) {
                        // 서버에서 게임 시작 메시지 수신
                        gamePanel.startGame();
                    } else if (message.startsWith("*** 게임 결과 ***")) {
                        // 게임 결과 메시지 수신 및 전체 표시
                        StringBuilder resultMessage = new StringBuilder(message).append("\n");
                        while (!(message = in.readLine()).isEmpty()) { // 빈 줄이 나올 때까지 읽기
                            resultMessage.append(message).append("\n");
                        }
                        gamePanel.displayWinner(resultMessage.toString()); // 전체 메시지 전달
                    } else {
                        // 기타 메시지는 게임 화면에 표시
                        gamePanel.displayMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new RacingClient().start();
    }
}