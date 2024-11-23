import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RacingServer extends JFrame {
    private int port;
    private ServerSocket serverSocket = null;

    private Thread acceptThread = null;

    private static final int PLAYER_COUNT = 2; // 최대 플레이어 수
    private List<ClientHandler> clients = new ArrayList<>(); // 연결된 클라이언트 핸들러 리스트
    private Map<Socket, String> playerNames = new HashMap<>(); // 소켓별 플레이어 이름 매핑
    private Map<Socket, Long> startTimes = new HashMap<>(); // 플레이어별 게임 시작 시간 기록
    private Map<Socket, Long> endTimes = new HashMap<>(); // 플레이어별 게임 종료 시간 기록
    private JTextArea serverLog; // 서버 로그를 출력할 UI 컴포넌트

    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;

    public RacingServer(int port) {
        super("Racing Server");

        buildGUI();

        setSize(400, 300);
        setLocation(400, 0);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setVisible(true);

        this.port = port;
    }

    private void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER);

        add(createControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setEditable(false);

        p.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p;
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel(new GridLayout(1, 3));

        b_connect = new JButton("서버 시작");
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptThread = new Thread(() -> startServer());
                acceptThread.start();

                b_connect.setEnabled(false);
                b_disconnect.setEnabled(true);
                b_exit.setEnabled(false);
            }

        });

        b_disconnect = new JButton("서버 종료");
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();

                b_connect.setEnabled(true);
                b_disconnect.setEnabled(false);
                b_exit.setEnabled(true);
            }

        });

        b_exit = new JButton("종료");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(-1);
            }

        });

        p.add(b_connect);
        p.add(b_disconnect);
        p.add(b_exit);

        b_disconnect.setEnabled(false);

        return p;
    }

    private void disconnect() {
        try {
            acceptThread = null;
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류> " + e.getMessage());
            System.exit(-1);
        }
    }

    public void startServer() {
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다. Player의 접속을 기다리고 있습니다...");

            while (clients.size() < PLAYER_COUNT) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this, clients.size() + 1);
                clients.add(clientHandler); // 클라이언트를 리스트에 추가
                new Thread(clientHandler).start(); // 클라이언트 핸들러 실행
                printDisplay("현재 접속한 플레이어 수 = " + clients.size());
            }

            printDisplay("모든 플레이어가 접속하였습니다. 게임이 시작되길 기다립니다...");
        } catch (IOException e) {
            printDisplay("오류: " + e.getMessage());
        }
    }

    public synchronized void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    public synchronized void broadcast(String message) {
        // 모든 클라이언트에 메시지 전송
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        printDisplay("Broadcast: " + message);
    }

    public synchronized void recordStartTime(Socket socket) {
        // 게임 시작 시간을 기록
        startTimes.put(socket, System.currentTimeMillis());
        printDisplay("Game started for: " + playerNames.get(socket));
    }

    public synchronized void recordEndTime(Socket socket) {
        // 게임 종료 시간을 기록
        endTimes.put(socket, System.currentTimeMillis());
        printDisplay("Game ended for: " + playerNames.get(socket));

        // 모든 플레이어가 종료되었는지 확인
        if (endTimes.size() == PLAYER_COUNT) {
            calculateResults(); // 결과 계산
        }
    }

    private void calculateResults() {
        String winner = null; // 승리자 이름
        long longestTime = 0; // 최장 플레이 시간

        StringBuilder results = new StringBuilder();
        results.append("*** 게임 결과 ***\n");

        // 각 플레이어의 플레이 시간 계산 및 승자 결정
        for (Socket socket : startTimes.keySet()) {
            long playTime = endTimes.get(socket) - startTimes.get(socket); // 플레이 시간 계산
            results.append(playerNames.get(socket))
                    .append(" 플레이 시간: ")
                    .append(playTime / 1000.0)
                    .append("초\n");

            if (playTime > longestTime) {
                longestTime = playTime;
                winner = playerNames.get(socket);
            }
        }

        results.append("승리자: ").append(winner).append("\n");

        // 결과를 모든 클라이언트에 전송
        String finalResults = results.toString();
        broadcast(finalResults); // 결과 메시지 브로드캐스트
        printDisplay(finalResults); // 서버 로그에 결과 출력
    }

    class ClientHandler implements Runnable {
        private Socket socket; // 클라이언트 소켓
        private PrintWriter out; // 클라이언트로 메시지를 보내기 위한 스트림
        private BufferedReader in; // 클라이언트로부터 메시지를 받기 위한 스트림
        private RacingServer server;
        private int clientNumber; // 클라이언트 번호

        public ClientHandler(Socket socket, RacingServer server, int clientNumber) {
            this.socket = socket;
            this.server = server;
            this.clientNumber = clientNumber;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 자동차 이미지 전송
                String carImage = (clientNumber == 1) ? "Player1.png" : "Player2.png";
                out.println("CAR_IMAGE:" + carImage);

                // 이름 입력 요청
                out.println("ENTER_NAME");
                String playerName = in.readLine();
                server.playerNames.put(socket, playerName); // 플레이어 이름 저장
                server.printDisplay("Player name received: " + playerName);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("START")) {
                        server.recordStartTime(socket); // 게임 시작 시간 기록
                        server.broadcast(playerName + " has started the game!");
                    } else if (message.startsWith("COLLISION")) {
                        server.recordEndTime(socket); // 게임 종료 시간 기록
                        server.broadcast(playerName + " collided with an obstacle!");
                    } else if (message.startsWith("POS:")) {
                        server.broadcast(message); // 위치 정보 브로드캐스트
                    }
                }
            } catch (IOException e) {
                server.printDisplay("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close(); // 클라이언트 소켓 닫기
                } catch (IOException e) {
                    e.printStackTrace();
                }
                server.clients.remove(this); // 클라이언트 핸들러 리스트에서 제거
                server.broadcast("Player " + clientNumber + " disconnected.");
            }
        }

        public void sendMessage(String msg) {
            out.println(msg); // 클라이언트로 메시지 전송
        }
    }

    public static void main(String[] args) {
        int port = 54321;

        RacingServer server = new RacingServer(port);
    }
}