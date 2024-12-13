import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RacingClient extends JFrame {

    private JTextField t_nameField, t_hostAddr, t_portNum;
    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    private String carImageName;
    private GamePanel gamePanel;
    private String playerName;

    private String serverAddress = "localhost"; // 서버 컴퓨터의 IP 주소
    private int serverPort = 54321;

    public RacingClient() {
        super("Racing Game Client");
        buildGUI();

        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER);

        JPanel p_input = new JPanel(new GridLayout(2, 0));
        p_input.add(createInfoPanel());
        p_input.add(createControlPanel());
        add(p_input, BorderLayout.SOUTH);
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setEditable(false);

        p.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p;
    }

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        t_nameField = new JTextField(10);
        t_hostAddr = new JTextField(12);
        t_portNum = new JTextField(5);

        t_hostAddr.setText(serverAddress);
        t_portNum.setText(String.valueOf(serverPort));

        t_portNum.setHorizontalAlignment(JTextField.CENTER);

        p.add(new JLabel("플레이어 이름: "));
        p.add(t_nameField);

        p.add(new JLabel("서버 주소: "));
        p.add(t_hostAddr);

        p.add(new JLabel("포트 번호: "));
        p.add(t_portNum);

        return p;
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel(new GridLayout(0, 3));

        b_connect = new JButton("접속하기");
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // 서버 주소를 GUI 입력 필드에서 가져오기
                    serverAddress = t_hostAddr.getText();

                    // 서버에 연결
                    connectToServer();

                    // 플레이어 이름 전송
                    playerName = t_nameField.getText();
                    sendPlayerName();
                } catch (IOException ex) {
                    showError("서버 연결 실패: " + ex.getMessage());
                }
            }
        });b_connect = new JButton("접속하기");
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // 서버 주소를 GUI 입력 필드에서 가져오기
                    serverAddress = t_hostAddr.getText();

                    // 서버에 연결
                    connectToServer();

                    // 플레이어 이름 전송
                    playerName = t_nameField.getText();
                    sendPlayerName();
                } catch (IOException ex) {
                    showError("서버 연결 실패: " + ex.getMessage());
                }
            }
        });

        b_disconnect = new JButton("연결 끊기");
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });

        b_exit = new JButton("종료");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        p.add(b_connect);
        p.add(b_disconnect);
        p.add(b_exit);

        return p;
    }

    private void connectToServer() throws IOException {
        socket = new Socket(serverAddress, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        new Thread(new ServerListener()).start();
        printDisplay("서버에 연결되었습니다.");
    }

    private void sendPlayerName() {
        if (playerName != null && !playerName.isEmpty()) {
            out.println(playerName);
            printDisplay("Player name sent: " + playerName);
        } else {
            printDisplay("Player name is empty!");
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                printDisplay("서버와의 연결을 종료했습니다.");
            }
        } catch (IOException e) {
            printDisplay("연결 종료 중 오류 발생: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    private void printDisplay(String message) {
        t_display.append(message + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    private void setupGameUI(String carImageName) {
        JFrame gameFrame = new JFrame("Racing Game - " + playerName);

        // GamePanel 객체 생성
        gamePanel = new GamePanel(out, carImageName);

        // GamePanel 추가
        gameFrame.add(gamePanel);
        gameFrame.setSize(400, 600);
        gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameFrame.setVisible(true);

    }

    public void sendCollisionMessage() {
        if (out != null) {
            out.println("COLLISION");
            printDisplay("COLLISION 메시지가 서버로 전송되었습니다.");
        }
    }


    class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CAR_IMAGE:")) {
                        // 자신의 자동차 이미지 처리
                        carImageName = message.split(":")[1];
                        setupGameUI(carImageName);
                    } else if (message.startsWith("OPPONENT_CAR:")) {
                        // 상대방 자동차 이미지 처리
                        String opponentCarImageName = message.split(":")[1];
                        gamePanel.setOpponentCarImage(opponentCarImageName); // 상대방 이미지 설정
                    } else if (message.startsWith("START_GAME")) {
                        // 게임 시작 신호 처리
                        gamePanel.startGame();
                    } else if (message.startsWith("POS:")) {
                        // 상대방 자동차 위치 업데이트
                        String[] position = message.substring(4).split(",");
                        int x = Integer.parseInt(position[0]);
                        int y = Integer.parseInt(position[1]);
                        gamePanel.updateOpponentPosition(x, y);
                    } else if (message.startsWith("OBSTACLES:")) {
                        // 장애물 데이터 수신 및 업데이트
                        String[] obstacleData = message.substring(10).split(";");
                        List<Obstacle> receivedObstacles = new ArrayList<>();
                        for (String data : obstacleData) {
                            if (!data.isEmpty()) {
                                String[] parts = data.split(",");
                                int x = Integer.parseInt(parts[0]);       // x 좌표
                                int y = Integer.parseInt(parts[1]);       // y 좌표
                                String imageName = parts[2];              // 이미지 이름
                                receivedObstacles.add(new Obstacle(x, y, imageName)); // 이미지 이름 포함한 장애물 생성
                            }
                        }
                        gamePanel.updateObstacles(receivedObstacles); // 장애물 정보 업데이트
                    } else if (message.startsWith("COLLISION")) {
                        // 충돌 메시지 처리
                        gamePanel.displayMessage(message);
                    } else if (message.startsWith("RESULT:")) {
                        // 결과 메시지 처리
                        String[] data = message.split(":");
                        String playerName = data[1];
                        String result = data[2];
                        gamePanel.displayMessage(playerName + "의 결과: " + result);
                    } else if (message.startsWith("*** 게임 결과 ***")) {
                        // 게임 결과 처리
                        StringBuilder resultMessage = new StringBuilder(message).append("\n");
                        while (!(message = in.readLine()).isEmpty()) {
                            resultMessage.append(message).append("\n");
                        }
                        JOptionPane.showMessageDialog(null, resultMessage.toString(), "게임 결과", JOptionPane.INFORMATION_MESSAGE);
                    } else if (message.startsWith("ERROR:")) {
                        // 오류 메시지 처리
                        JOptionPane.showMessageDialog(null, message.substring(6), "오류", JOptionPane.ERROR_MESSAGE);
                    } else {
                        // 기타 메시지 처리
                        gamePanel.displayMessage(message);
                    }
                }
            } catch (IOException e) {
                printDisplay("서버와의 연결이 종료되었습니다.");
            }
        }
    }

    public static void main(String[] args) {
        new RacingClient();
    }
}
