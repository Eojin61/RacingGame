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
import java.util.AbstractMap;

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
            printDisplay("서버가 시작되었습니다. 플레이어의 접속을 기다리고 있습니다.");

            while (clients.size() < PLAYER_COUNT) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this, clients.size() + 1);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                printDisplay("현재 접속한 플레이어 수 = " + clients.size());
            }

            printDisplay("모든 플레이어가 접속하였습니다. 게임이 시작되길 기다립니다.");
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

    public synchronized void recordStartTime(Socket socket) {
        startTimes.put(socket, System.currentTimeMillis());
        printDisplay("Game started for: " + playerNames.get(socket));
        printDisplay("Current startTimes: " + startTimes);
    }

    public synchronized void recordEndTime(Socket socket) {
        endTimes.put(socket, System.currentTimeMillis());
        printDisplay("Game ended for: " + playerNames.get(socket));
        printDisplay("Current endTimes: " + endTimes);

        if (endTimes.size() == PLAYER_COUNT) {
            calculateResults(); // 결과 계산
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

        // 플레이 시간을 기준으로 정렬
        playerData.sort(Map.Entry.comparingByValue());

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

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String carImage = (clientNumber == 1) ? "Player1.png" : "Player2.png";
                out.println("CAR_IMAGE:" + carImage);

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
                    } else if (message.startsWith("POS:")) {
                        server.broadcast(message);
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