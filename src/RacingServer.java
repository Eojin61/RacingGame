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

    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;
    private final String[] OBSTACLE_IMAGES = {"obstacle1.png", "obstacle2.png"};

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

            while (true) {
                Socket socket = serverSocket.accept();
                printDisplay("새 클라이언트 접속: " + socket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(socket, this, clients.size() + 1);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

                printDisplay("현재 접속한 플레이어 수: " + clients.size());

                if (clients.size() == PLAYER_COUNT) {
                    printDisplay("모든 플레이어가 접속하였습니다. 게임이 시작되길 기다립니다.");
                    break;
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

    private void generateObstacles() {
        obstacles.clear();
        Random random = new Random();

        for (int i = 0; i < OBSTACLE_COUNT; i++) {
            int x = random.nextInt(300) + 50;
            int y = -random.nextInt(600);
            String imageName = OBSTACLE_IMAGES[random.nextInt(OBSTACLE_IMAGES.length)];
            obstacles.add(new Obstacle(x, y, imageName));
        }
    }

    private void broadcastObstacles() {
        StringBuilder obstacleMessage = new StringBuilder("OBSTACLES:");
        for (Obstacle obstacle : obstacles) {
            obstacleMessage.append(obstacle.x)
                    .append(",")
                    .append(obstacle.y)
                    .append(",")
                    .append(obstacle.imageName)
                    .append(";");
        }
        broadcast(obstacleMessage.toString());
        System.out.println("Broadcasted Obstacles: " + obstacleMessage);
    }

    private void updateObstacles() {
        Random random = new Random();
        for (Obstacle obstacle : obstacles) {
            obstacle.y += 10;
            if (obstacle.y > 600) {
                obstacle.y = -random.nextInt(600);
                obstacle.x = random.nextInt(300) + 50;
                obstacle.imageName = OBSTACLE_IMAGES[random.nextInt(OBSTACLE_IMAGES.length)];
            }
        }
        broadcastObstacles();
    }

    public synchronized void startGame() {
        generateObstacles();
        broadcastObstacles();

        new Thread(() -> {
            while (isGameRunning) {
                try {
                    Thread.sleep(50);
                    updateObstacles();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public synchronized void recordStartTime(Socket socket) {
        if (isGameRunning) {
            sendMessageToClient(socket, "ERROR: 게임이 이미 진행 중입니다.");
            return;
        }
        isGameRunning = true;
        startTimes.put(socket, System.currentTimeMillis());

        startGame();

        broadcast("START_GAME");
        printDisplay(playerNames.get(socket) + " 게임 시작!");
    }

    public synchronized void recordEndTime(Socket socket) {
        if (!isGameRunning) {
            return;
        }

        endTimes.put(socket, System.currentTimeMillis());

        if (endTimes.size() == PLAYER_COUNT) {
            isGameRunning = false;
            calculateResults();
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

        List<Map.Entry<Socket, Long>> playerData = new ArrayList<>();

        for (Socket socket : startTimes.keySet()) {
            if (endTimes.containsKey(socket)) {
                long playTime = endTimes.get(socket) - startTimes.get(socket);
                playerData.add(new AbstractMap.SimpleEntry<>(socket, playTime));
            }
        }

        playerData.sort((entry1, entry2) -> Long.compare(entry2.getValue(), entry1.getValue()));

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

        if (winner != null) {
            results.append("\n승리자: ").append(winner).append("\n");
        } else {
            results.append("\n승리자를 결정할 수 없습니다.\n");
        }

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
            return socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String carImage = (clientNumber == 1) ? "Player1.png" : "Player2.png";
                out.println("CAR_IMAGE:" + carImage);

                String opponentCarImage = (clientNumber == 1) ? "Player2.png" : "Player1.png";
                server.broadcastToOthers(socket, "OPPONENT_CAR:" + opponentCarImage);

                String playerName = in.readLine();
                server.playerNames.put(socket, playerName);
                server.printDisplay("Player name received: " + playerName);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("START")) {
                        server.recordStartTime(socket);
                        server.broadcast(playerName + " has started the game!");
                    } else if (message.startsWith("COLLISION")) {
                        server.recordEndTime(socket);
                        server.broadcast(playerName + " collided with an obstacle!");
                    } else if (message.startsWith("RESULT:")) {
                        String result = message.split(":")[1];
                        server.broadcast("RESULT:" + playerName + ":" + result);
                    } else if (message.startsWith("POS:")) {
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