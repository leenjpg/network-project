import java.io.*; //for handling input and output streams
import java.net.*; //networking classes for client-server communication
import javax.swing.*; //Swing components for GUI
import java.awt.*; //for layout and UI styling

public class Client {
    private static PrintWriter out; //output stream to send messages to the server
    private static String playerName; 
    private static JFrame frame; // windows GUI
    private static DefaultListModel<String> playerListModel = new DefaultListModel<>(); //stores connected players, we made it model because it changes dynamically 
    private static DefaultListModel<String> waitingListModel = new DefaultListModel<>(); // stores waiting list players
    private static JList<String> playerList; //displays the player list model in GUI
    private static JList<String> waitingList; //displays the waiting list model in GUI
    private static JButton playButton;
    private static JButton startGameButton;
    private static JButton connectButton;

    public static void main(String[] args) { //main just initializes the GUI, it doesn't need to do anything else because the gui waits for client's input 
        SwingUtilities.invokeLater(Client::showLoginScreen); //ensures GUI components created safely,
    }

    private static void showLoginScreen() {
        frame = new JFrame("Math Game - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(173, 216, 230));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel nameLabel = new JLabel("Enter Name:");
        nameLabel.setFont(new Font("Serif", Font.BOLD, 14));
        panel.add(nameLabel, gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(10);
        nameField.setFont(new Font("Serif", Font.PLAIN, 14));
        panel.add(nameField, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        connectButton = new JButton("Connect");
        styleButton(connectButton);
        panel.add(connectButton, gbc);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);

        connectButton.addActionListener(e -> {
            playerName = nameField.getText().trim();
            if (!playerName.isEmpty()) {
                new Thread(Client::connectToServer).start();
            }
        });
    }

    private static void showGameScreen() {
        frame.getContentPane().removeAll();
        frame.setTitle("Math Game - Connected Players");
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(173, 216, 230));

        JLabel titleLabel = new JLabel("The Connected Players", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));

        playerList = new JList<>(playerListModel);
        playerList.setFont(new Font("Serif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(playerList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        playButton = new JButton("Play");
        styleButton(playButton);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(173, 216, 230));
        buttonPanel.add(playButton);

        frame.add(titleLabel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        playButton.addActionListener(e -> {
            if (out != null) {
                out.println("play");
            }
        });
    }

    private static void showWaitingRoom() {
        frame.getContentPane().removeAll();
        frame.setTitle("Math Game - Waiting Room");
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(173, 216, 230));

        JLabel titleLabel = new JLabel("Waiting Room", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));

        waitingList = new JList<>(waitingListModel);
        waitingList.setFont(new Font("Serif", Font.PLAIN, 14));
        JScrollPane waitingScrollPane = new JScrollPane(waitingList);
        waitingScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        startGameButton = new JButton("Start Game");
        startGameButton.setEnabled(false);
        styleButton(startGameButton);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(173, 216, 230));
        buttonPanel.add(startGameButton);

        frame.add(titleLabel, BorderLayout.NORTH);
        frame.add(waitingScrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        startGameButton.addActionListener(e -> {
            if (out != null) {
                out.println("start");
            }
        });
    }

    private static void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 5555);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(playerName);
            SwingUtilities.invokeLater(Client::showGameScreen);

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        final String message = serverMessage;
                        SwingUtilities.invokeLater(() -> processServerMessage(message));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processServerMessage(String message) {
        if (message.contains("Players connected:")) {
            playerListModel.clear();
            for (String name : message.replace("Players connected: ", "").split(", ")) {
                if (!name.trim().isEmpty()) {
                    playerListModel.addElement(name);
                }
            }
        } else if (message.equals("WAITING_ROOM")) {
            SwingUtilities.invokeLater(Client::showWaitingRoom);
        } else if (message.startsWith("Waiting Room:")) {
            waitingListModel.clear();
            for (String name : message.replace("Waiting Room: ", "").split(", ")) {
                if (!name.trim().isEmpty()) {
                    waitingListModel.addElement(name);
                }
            }
        } else if (message.equals("WAITING_ROOM_FULL")) {
            if (playButton != null) {
                playButton.setEnabled(false);
            }
        } else if (message.equals("START_BUTTON_ENABLED")) {
            if (startGameButton != null) {
                startGameButton.setEnabled(true);
            }
        } else if (message.equals("START_BUTTON_DISABLED")) {
            if (startGameButton != null) {
                startGameButton.setEnabled(false);
            }
        } else if (message.equals("GAME_STARTED")) {
            JOptionPane.showMessageDialog(frame, "Game has started!", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Button styling function 
    private static void styleButton(JButton button) {
        button.setFont(new Font("Serif", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 102, 204));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 30));
    }
}
