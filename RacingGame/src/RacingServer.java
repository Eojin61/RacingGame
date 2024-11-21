import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RacingServer {
    private static final int PORT = 30000; // 서버 포트 번호
    private static final int PLAYER_COUNT = 2; // 최대 플레이어 수
    private List<ClientHandler> clients = new ArrayList<>(); // 연결된 클라이언트 핸들러 리스트
    private Map<Socket, String> playerNames = new HashMap<>(); // 소켓별 플레이어 이름 매핑
    private Map<Socket, Long> startTimes = new HashMap<>(); // 플레이어별 게임 시작 시간 기록
    private Map<Socket, Long> endTimes = new HashMap<>(); // 플레이어별 게임 종료 시간 기록
    private JTextArea serverLog; // 서버 로그를 출력할 UI 컴포넌트

    public static void main(String[] args) {
        new RacingServer().start();
    }

    public void start() {
        setupUI(); // 서버 로그 UI 구성

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logMessage("Racing Server is waiting for players...");

            while (clients.size() < PLAYER_COUNT) {
                // 새로운 클라이언트 접속 대기
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this, clients.size() + 1);
                clients.add(clientHandler); // 클라이언트를 리스트에 추가
                new Thread(clientHandler).start(); // 클라이언트 핸들러 실행
                logMessage("Player connected: Current count = " + clients.size());
            }

            logMessage("All players connected. Waiting for the game to start...");
        } catch (IOException e) {
            logMessage("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupUI() {
        // 서버 로그를 표시할 GUI 구성
        JFrame frame = new JFrame("Racing Server");
        serverLog = new JTextArea();
        serverLog.setEditable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JScrollPane(serverLog), BorderLayout.CENTER);

        frame.add(panel);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public synchronized void logMessage(String message) {
        // 서버 로그에 메시지 추가
        serverLog.append(message + "\n");
    }

    public synchronized void broadcast(String message) {
        // 모든 클라이언트에 메시지 전송
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        logMessage("Broadcast: " + message);
    }

    public synchronized void recordStartTime(Socket socket) {
        // 게임 시작 시간을 기록
        startTimes.put(socket, System.currentTimeMillis());
        logMessage("Game started for: " + playerNames.get(socket));
    }

    public synchronized void recordEndTime(Socket socket) {
        // 게임 종료 시간을 기록
        endTimes.put(socket, System.currentTimeMillis());
        logMessage("Game ended for: " + playerNames.get(socket));

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
        logMessage(finalResults); // 서버 로그에 결과 출력
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
                server.logMessage("Player name received: " + playerName);

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
                server.logMessage("Error: " + e.getMessage());
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

        public void sendMessage(String message) {
            out.println(message); // 클라이언트로 메시지 전송
        }
    }
}