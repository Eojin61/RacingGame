import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;

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

    private String serverAddress = "localhost";
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
                serverAddress = t_hostAddr.getText();
                serverPort = Integer.parseInt(t_portNum.getText());
                playerName = t_nameField.getText().trim();

                if (playerName.isEmpty()) {
                    showError("플레이어 이름을 입력하세요.");
                    return;
                }

                try {
                    connectToServer();
                    sendPlayerName();
                } catch (IOException ex) {
                    printDisplay("서버 연결 오류: " + ex.getMessage());
                    return;
                }

                b_connect.setEnabled(false);
                b_disconnect.setEnabled(true);
                b_exit.setEnabled(false);

                t_nameField.setEditable(false);
                t_hostAddr.setEditable(false);
                t_portNum.setEditable(false);
            }
        });

        b_disconnect = new JButton("접속 끊기");
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();

                b_connect.setEnabled(true);
                b_disconnect.setEnabled(false);
                b_exit.setEnabled(true);

                t_nameField.setEditable(true);
                t_hostAddr.setEditable(true);
                t_portNum.setEditable(true);
            }
        });

        b_exit = new JButton("종료하기");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        p.add(b_connect);
        p.add(b_disconnect);
        p.add(b_exit);

        b_disconnect.setEnabled(false);

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
        out.println(playerName); // 서버에 이름 전송
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
        gamePanel = new GamePanel(out, carImageName);
        gameFrame.add(gamePanel);
        gameFrame.setSize(400, 600);
        gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameFrame.setVisible(true);
    }

    class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CAR_IMAGE:")) {
                        carImageName = message.split(":")[1];
                        setupGameUI(carImageName);
                    } else if (message.startsWith("START_GAME")) {
                        gamePanel.startGame();
                    } else if (message.startsWith("*** 게임 결과 ***")) {
                        StringBuilder resultMessage = new StringBuilder(message).append("\n");
                        while (!(message = in.readLine()).isEmpty()) {
                            resultMessage.append(message).append("\n");
                        }
                        gamePanel.displayWinner(resultMessage.toString());
                    } else {
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