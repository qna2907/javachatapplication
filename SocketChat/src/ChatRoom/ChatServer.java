package ChatRoom;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

public class ChatServer extends JFrame implements ActionListener {

    private JTextArea messageArea;
    private JList<String> userList;
    private DefaultListModel<String> model;
    private JLabel userCountLbl, statusLbl;
    private JButton startBtn, stopBtn, kickBtn;
    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clientHandlers;
    private int clientCount;
    private boolean serverRunning;
    private String clientName;

    public ChatServer() {
        setTitle("Chat Server");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(30, 30, 30));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(150, 150, 150)));
        startBtn = new JButton("Start");
        startBtn.setFocusPainted(false);
        startBtn.setBackground(new Color(40, 40, 40));
        startBtn.setForeground(Color.WHITE);
        startBtn.addActionListener(this);
        topPanel.add(startBtn);
        stopBtn = new JButton("Stop");
        stopBtn.setFocusPainted(false);
        stopBtn.setBackground(new Color(40, 40, 40));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.addActionListener(this);
        stopBtn.setEnabled(false);
        topPanel.add(stopBtn);
        kickBtn = new JButton("Kick");
        kickBtn.setFocusPainted(false);
        kickBtn.setBackground(new Color(40, 40, 40));
        kickBtn.setForeground(Color.WHITE);
        kickBtn.addActionListener(this);
        kickBtn.setEnabled(false);
        topPanel.add(kickBtn);
        userCountLbl = new JLabel("0 users connected");
        userCountLbl.setForeground(new Color(150, 150, 150));
        userCountLbl.setBorder(new EmptyBorder(0, 10, 0, 10));
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(userCountLbl);
        add(topPanel, BorderLayout.NORTH);

        JPanel middlePanel = new JPanel(new BorderLayout());
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setBackground(new Color(40, 40, 40));
        messageArea.setForeground(Color.WHITE);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setPreferredSize(new Dimension(480, 300));
        middlePanel.add(messageScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        model = new DefaultListModel<String>();
        userList = new JList<String>(model);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(new Color(40, 40, 40));
        userList.setForeground(Color.WHITE);
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    kickBtn.doClick();
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(0, 100));
        bottomPanel.add(userScrollPane, BorderLayout.CENTER);
        statusLbl = new JLabel("Server not running");
        statusLbl.setForeground(new Color(150, 150, 150));
        bottomPanel.add(statusLbl, BorderLayout.SOUTH);

        add(middlePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        clientHandlers = new ArrayList<ClientHandler>();
        clientCount = 0;
        serverRunning = false;
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == startBtn) {
            startServer();
        } else if (source == stopBtn) {
            stopServer();
        } else if (source == kickBtn) {
            kickClient();
        }
    }

    private void startServer() {
        if (serverRunning) {
            return;
        }
        try {
            serverSocket = new ServerSocket(11111);
            new ConnectionThread().start();
            serverRunning = true;
            statusLbl.setText("Server is running");
            statusLbl.setForeground(new Color(0, 200, 0));
            updateButtons();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void stopServer() {
        if (!serverRunning) {
            return;
        }
        try {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.dos.writeUTF("#SERVER_CLOSED");
                clientHandler.socket.close();
            }
            clientHandlers.clear();
            serverSocket.close();
            serverRunning = false;
            clientCount = 0;
            statusLbl.setText("Server not running");
            statusLbl.setForeground(new Color(150, 150, 150));
            model.clear();
            messageArea.setText("");
            updateButtons();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void kickClient() {
        int selectedIndex = userList.getSelectedIndex();
        if (selectedIndex == -1) {
            return;
        }
        try {
            clientHandlers.get(selectedIndex).dos.writeUTF("#KICK");
            clientHandlers.get(selectedIndex).socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void updateButtons() {
        startBtn.setEnabled(!serverRunning);
        stopBtn.setEnabled(serverRunning);
        kickBtn.setEnabled(clientCount > 0);
    }

    private void sendUserListToAll() {
        StringBuilder userListMsg = new StringBuilder("#USER_LIST");
        for (ClientHandler clientHandler : clientHandlers) {
            userListMsg.append(" " + clientHandler.clientName);
        }
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.dos.writeUTF(userListMsg.toString());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendMessageToAll(String message) {
        messageArea.append(clientName + ": " + message + "\n");
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.dos.writeUTF(clientName + ": " + message);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class ConnectionThread extends Thread {

        public void run() {
            while (serverRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String clientName = dis.readUTF();
                    System.out.println("[SERVER] Received connection request from client: " + clientName); // ghi log thông tin kết nối từ client
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    if (clientHandlers.size() >= 10) {
                        dos.writeUTF("#SERVER_FULL");
                        socket.close();
                    } else if (clientHandlers.stream().anyMatch(ch -> ch.clientName.equals(clientName))) {
                        dos.writeUTF("#DUPLICATE_NAME");
                        socket.close();
                    } else {
                        ClientHandler clientHandler = new ClientHandler(socket, dos, dis, clientName);
                        clientHandlers.add(clientHandler);
                        model.addElement(clientName);
                        clientCount++;
                        messageArea.append(clientName + " has connected to the server." + "\n");
                        userCountLbl.setText(clientCount + " users connected");
                        kickBtn.setEnabled(true);
                        sendUserListToAll();
                    }
                } catch (IOException ex) {
                    System.out.println("[SERVER] Error occurred while handling client connection: " + ex.getMessage()); // ghi lại lỗi xảy ra
                }
            }
        }
    }

    private class ClientHandler extends Thread {

        private Socket socket;
        private DataOutputStream dos;
        private DataInputStream dis;
        private String clientName;

        public ClientHandler(Socket socket, DataOutputStream dos, DataInputStream dis, String clientName) {
            this.socket = socket;
            this.dos = dos;
            this.dis = dis;
            this.clientName = clientName;
            start();
        }

        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                dos.writeUTF("#NAME_ACCEPTED");
                sendUserListToAll();

                while (true) {
                    String message = dis.readUTF();
                    if (message.startsWith("#PRIVATE")) {
                        // xử lý tin nhắn riêng tư
                        sendMessageToAll(clientName + ": " + message);
                    } else if (message.startsWith("#KICK")) {
                        // xử lý yêu cầu kick client khác
                        sendMessageToAll(clientName + "  has been kick by server");
                    } else {
                        sendMessageToAll(clientName + ": " + message);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    dis.close();
                    dos.close();
                    socket.close();
                    clientHandlers.remove(this);
                    model.removeElement(clientName);
                    clientCount--;
                    messageArea.append(clientName + " has left the chat.\n");
                    userCountLbl.setText(clientCount + " users connected");
                    sendUserListToAll();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.setVisible(true);
    }
}
