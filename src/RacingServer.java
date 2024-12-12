import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.*;

public class RacingServer extends JFrame {
    private int port;
    private ServerSocket serverSocket = null;

    private Thread acceptThread = null;

    private static final int PLAYER_COUNT = 2;
    private List<ClientHandler> clients = new ArrayList<>();
    private Map<Socket, String> playerNames = new HashMap<>();
    private Map<Socket, Long> startTimes = new HashMap<>();
    private Map<Socket, Long> endTimes = new HashMap<>();
    private JTextArea serverLog;

    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;

    // 장애물 관련 필드
    private List<Obstacle> obstacles = new ArrayList<>(); // 장애물 리스트
    private final int OBSTACLE_COUNT = 5;

    private boolean isGameRunning = false; // 게임 진행 상태

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
        try {
            // 모든 네트워크 인터페이스에서 연결을 수락하려면 InetAddress.getByName("0.0.0.0") 또는 생략.
            serverSocket = new ServerSocket(port);

            printDisplay("서버가 시작되었습니다. 포트: " + port);
            printDisplay("플레이어의 접속을 기다리고 있습니다...");

            while (true) { // 무제한으로 클라이언트 연결을 수락
                Socket socket = serverSocket.accept();
                printDisplay("새 클라이언트 접속: " + socket.getInetAddress().getHostAddress());

                // 클라이언트 핸들러 생성 및 추가
                ClientHandler clientHandler = new ClientHandler(socket, this, clients.size() + 1);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

                printDisplay("현재 접속한 플레이어 수: " + clients.size());

                // 모든 플레이어가 접속했을 경우 메시지를 출력
                if (clients.size() == PLAYER_COUNT) {
                    printDisplay("모든 플레이어가 접속하였습니다. 게임이 시작되길 기다립니다.");
                    break; // 필요한 경우 무한 루프를 종료
                }
            }
        } catch (IOException e) {
            printDisplay("오류: " + e.getMessage());
        }
    }


    public synchronized void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        printDisplay("Broadcast: " + message);
    }

    public synchronized void broadcastToOthers(Socket excludeSocket, String message) {
        for (ClientHandler client : clients) {
            if (!client.getSocket().equals(excludeSocket)) {
                client.sendMessage(message);
            }
        }
    }

    // 장애물 생성
    private void generateObstacles() {
        obstacles.clear();
        Random random = new Random();

        for (int i = 0; i < OBSTACLE_COUNT; i++) {
            int x = random.nextInt(300) + 50;   // x 좌표: 50 ~ 349
            int y = -random.nextInt(600);       // y 좌표: -600 ~ -1
            obstacles.add(new Obstacle(x, y)); // 장애물 추가
        }
    }

    // 장애물 상태를 클라이언트로 브로드캐스트
    private void broadcastObstacles() {
        StringBuilder obstacleMessage = new StringBuilder("OBSTACLES:");
        for (Obstacle obstacle : obstacles) {
            obstacleMessage.append(obstacle.x).append(",").append(obstacle.y).append(";");
        }
        broadcast(obstacleMessage.toString());
        // 브로드캐스트 데이터 로그 출력
        System.out.println("Broadcasted Obstacles: " + obstacleMessage);
    }

    private void updateObstacles() {
        for (Obstacle obstacle : obstacles) {
            obstacle.y += 10; // 장애물을 아래로 이동
            if (obstacle.y > 600) {
                obstacle.y = -(new Random().nextInt(600)); // 새로운 y 좌표 (음수 값)
                obstacle.x = new Random().nextInt(300) + 50; // 새로운 x 좌표 (50 ~ 349)
            }
        }
        broadcastObstacles(); // 업데이트된 장애물 상태를 클라이언트로 전송
    }

    // 게임 시작 시 장애물 초기화 및 동기화 시작
    public synchronized void startGame() {
        generateObstacles();
        broadcastObstacles();

        new Thread(() -> {
            while (isGameRunning) {
                try {
                    Thread.sleep(50); // 장애물 업데이트 주기
                    updateObstacles();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public synchronized void recordStartTime(Socket socket) {
        if (isGameRunning) {
            sendMessageToClient(socket, "ERROR: 게임이 이미 진행 중입니다."); // 이미 게임 중인 경우
            return;
        }
        isGameRunning = true; // 게임 시작
        startTimes.put(socket, System.currentTimeMillis());

        startGame(); // 게임 시작 로직 호출

        broadcast("START_GAME"); // 모든 클라이언트에게 게임 시작 신호
        printDisplay(playerNames.get(socket) + " 게임 시작!");
    }

    public synchronized void recordEndTime(Socket socket) {
        endTimes.put(socket, System.currentTimeMillis());
        if (endTimes.size() == PLAYER_COUNT) {
            calculateResults();
            isGameRunning = false; // 게임 종료
        }
    }

    public void sendMessageToClient(Socket socket, String message) {
        for (ClientHandler client : clients) {
            if (client.getSocket().equals(socket)) {
                client.sendMessage(message);
                break;
            }
        }
    }

    private void calculateResults() {
        String winner = null;
        StringBuilder results = new StringBuilder();
        results.append("*** 게임 결과 ***\n");

        // 플레이어 데이터를 저장할 리스트
        List<Map.Entry<Socket, Long>> playerData = new ArrayList<>();

        // 데이터를 수집
        for (Socket socket : startTimes.keySet()) {
            if (endTimes.containsKey(socket)) {
                long playTime = endTimes.get(socket) - startTimes.get(socket);
                playerData.add(new AbstractMap.SimpleEntry<>(socket, playTime));
            }
        }

        // 플레이 시간을 기준으로 정렬 (긴 시간이 1등이 되도록 역순 정렬)
        playerData.sort((entry1, entry2) -> Long.compare(entry2.getValue(), entry1.getValue()));


        // 순위 및 결과 출력
        int rank = 1;
        for (Map.Entry<Socket, Long> entry : playerData) {
            String playerName = playerNames.get(entry.getKey());
            long playTime = entry.getValue();
            results.append(rank).append("등: ").append(playerName)
                    .append(" - 플레이 시간: ").append(playTime / 1000.0).append("초\n");

            if (rank == 1) {
                winner = playerName;
            }
            rank++;
        }

        // 승리자 출력
        if (winner != null) {
            results.append("\n승리자: ").append(winner).append("\n");
        } else {
            results.append("\n승리자를 결정할 수 없습니다.\n");
        }

        // 결과 전송 및 출력
        String finalResults = results.toString();
        broadcast(finalResults);
        printDisplay(finalResults);
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private RacingServer server;
        private int clientNumber;

        public ClientHandler(Socket socket, RacingServer server, int clientNumber) {
            this.socket = socket;
            this.server = server;
            this.clientNumber = clientNumber;
        }

        public Socket getSocket() {
            return socket; // 소켓 반환
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 클라이언트에게 자동차 이미지 전송
                String carImage = (clientNumber == 1) ? "Player1.png" : "Player2.png";
                out.println("CAR_IMAGE:" + carImage);

                // 상대방 자동차 이미지 브로드캐스트
                String opponentCarImage = (clientNumber == 1) ? "Player2.png" : "Player1.png";
                server.broadcastToOthers(socket, "OPPONENT_CAR:" + opponentCarImage);

                // 플레이어 이름 수신
                String playerName = in.readLine();
                server.playerNames.put(socket, playerName);
                server.printDisplay("Player name received: " + playerName);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("START")) {
                        // 게임 시작 기록
                        server.recordStartTime(socket);
                        server.broadcast(playerName + " has started the game!");
                    } else if (message.startsWith("COLLISION")) {
                        // 충돌 상태 브로드캐스트
                        server.recordEndTime(socket);
                        server.broadcast(playerName + " collided with an obstacle!");
                    } else if (message.startsWith("RESULT:")) {
                        // 결과 데이터 브로드캐스트
                        String result = message.split(":")[1];
                        server.broadcast("RESULT:" + playerName + ":" + result);
                    } else if (message.startsWith("POS:")) {
                        // 위치 정보 브로드캐스트 (송신 클라이언트를 제외한 나머지에게만)
                        server.broadcastToOthers(socket, message);
                    }
                }
            } catch (IOException e) {
                server.printDisplay("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                server.clients.remove(this);
                server.broadcast("Player " + clientNumber + " disconnected.");
            }
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }
    }

    public static void main(String[] args) {
        int port = 54321;

        RacingServer server = new RacingServer(port);
    }
}