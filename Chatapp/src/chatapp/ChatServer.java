/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package chatapp;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class ChatServer extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JTextArea messageArea;
    private JButton sendButton;
    private JTextField textField;
    private JButton startButton;
    private JButton stopButton;
    private JButton kickButton;
    private ServerSocket serverSocket;
    private JList<String> userList; // Thêm bảng hiển thị danh sách người dùng
    private List<Socket> clientSockets;
    private List<PrintWriter> clientWriters;
    private List<String> usernames; // Thêm danh sách tên người dùng
    private boolean running;
    private String message;

    public ChatServer() {
        super("Chat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        messageArea = new JTextArea(10, 40);
        messageArea.setEditable(false);
        messageArea.setForeground(Color.blue);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 16));
        messageArea.setBorder(BorderFactory.createLoweredBevelBorder());
        JScrollPane scrollPane = new JScrollPane(messageArea);

        textField = new JTextField(40);
        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(80, 30));
        sendButton.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        sendButton.setForeground(Color.blue);
        sendButton.setBorder(BorderFactory.createLoweredBevelBorder());
        sendButton.setBackground(Color.lightGray);
        sendButton.setEnabled(false);
        sendButton.addActionListener(this);

        JPanel inputPanel = new JPanel();
        inputPanel.add(textField);
        inputPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        textField.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        textField.setBorder(BorderFactory.createLoweredBevelBorder());
        inputPanel.add(sendButton);

        startButton = new JButton("Start");
        startButton.addActionListener(this);
        startButton.setPreferredSize(new Dimension(70, 30));
        startButton.setFont(new Font("Arial", Font.PLAIN, 16));
        startButton.setForeground(Color.black);
        startButton.setBorder(BorderFactory.createLoweredBevelBorder());
        startButton.setBackground(Color.lightGray);

        stopButton = new JButton("Stop");
        stopButton.addActionListener(this);
        stopButton.setPreferredSize(new Dimension(70, 30));
        stopButton.setFont(new Font("Arial", Font.PLAIN, 16));
        stopButton.setForeground(Color.blue);
        stopButton.setBorder(BorderFactory.createLoweredBevelBorder());
        stopButton.setBackground(Color.lightGray);
        stopButton.setEnabled(false);

        kickButton = new JButton("Kick"); // Thêm nút kick
        kickButton.addActionListener(this);
        kickButton.setPreferredSize(new Dimension(70, 30));
        kickButton.setFont(new Font("Arial", Font.PLAIN, 16));
        kickButton.setForeground(Color.red);
        kickButton.setBorder(BorderFactory.createLoweredBevelBorder());
        kickButton.setBackground(Color.lightGray);
        kickButton.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(kickButton); // Thêm nút kick

        userList = new JList<String>(); // Khởi tạo bảng hiển thị danh sách người dùng
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setPreferredSize(new Dimension(200, 0)); // Đặt kích thước cho bảng hiển thị danh sách người dùng
        userListScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(BorderFactory.createTitledBorder("User List"));
        userPanel.setFont(new Font("Arial", Font.PLAIN, 16));
        userPanel.add(userListScrollPane, BorderLayout.CENTER);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(userPanel, BorderLayout.EAST); // Thêm bảng hiển thị danh sách người dùng vào giao diện
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        pack();
        setVisible(true);

        running = false;
        clientSockets = new ArrayList<>();
        clientWriters = new ArrayList<>();
        usernames = new ArrayList<>(); // Khởi tạo danh sách tên người dùng

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            kickButton.setEnabled(true); // Kích hoạt nút kick khi khởi động server
            sendButton.setEnabled(true);
            running = true;
            new Thread(new ServerThread()).start();
            messageArea.append("Server started.\n");
        } else if (e.getSource() == stopButton) {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            kickButton.setEnabled(false); // Vô hiệu hóa nút kick khi tắt server
            sendButton.setEnabled(false);
            running = false;
            try {
                for (PrintWriter writer : clientWriters) {
                    writer.close();
                }
                for (Socket socket : clientSockets) {
                    socket.close();
                }
                clientSockets.clear();
                clientWriters.clear();
                usernames.clear(); // Xóa danh sách tên người dùng khi tắt server
                userList.setListData(usernames.toArray(new String[usernames.size()])); // Cập nhật bảng hiển thị danh sách người dùng
                serverSocket.close();
                messageArea.append("Server stopped.\n");
            } catch (IOException ex) {
                System.err.println("Error stopping server: " + ex.getMessage());
            }
        } else if (e.getSource() == kickButton) {
            int index = userList.getSelectedIndex();
            if (index != -1) {
                String username = usernames.get(index);
                usernames.remove(index);
                userList.setListData(usernames.toArray(new String[usernames.size()]));
                PrintWriter writer = clientWriters.get(index);
                clientWriters.remove(index);
                Socket socket = clientSockets.get(index);
                clientSockets.remove(index);
                try {
                    writer.println("You have been kicked by the server");
                    writer.close();
                    socket.close();
                    messageArea.append(username + " has been kicked\n");

                } catch (IOException ex) {
                    messageArea.append("Error kicking user: " + ex.getMessage() + "\n");
                }
            }
        } else if (e.getSource() == sendButton) {
            message = textField.getText();
            messageArea.append("Server: " + message + " \n");
            textField.setText("");
            for (PrintWriter writer : clientWriters) {
                writer.println("Server: " + message);
            }

        }
    }

    class ServerThread implements Runnable {

        public void run() {
            try {
                serverSocket = new ServerSocket(4444);
                while (running) {
                    messageArea.append("Waiting for client...\n");
                    Socket clientSocket = serverSocket.accept();
                    clientSockets.add(clientSocket);
                    messageArea.append("Client connected: " + clientSocket.getInetAddress().getHostName() + "\n");
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    clientWriters.add(out);

                    // Prompt client for username
                    String username = null;
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    username = in.readLine();
                    usernames.add(username); // Thêm tên người dùng vào danh sách
                    userList.setListData(usernames.toArray(new String[usernames.size()])); // Cập nhật bảng hiển thị danh sách người dùng
                    out.println(username);
                    messageArea.append(username + " has joined the chat.\n");

                    for (PrintWriter writer : clientWriters) {
                        writer.println(username + " has joined the chat.");
                        messageArea.append("USERLIST: " + usernames + "\n");
                        writer.println("USERLIST: " + usernames);
                    }

                    // Start a new thread to handle the client
                    new Thread(new ClientThread(clientSocket, username)).start();
                }
            } catch (IOException ex) {
                System.err.println("Error starting server: " + ex.getMessage());
            }
        }
    }

    class ClientThread implements Runnable {

        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ClientThread(Socket clientSocket, String username) {
            this.clientSocket = clientSocket;
            this.username = username;
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException ex) {
                System.err.println("Error creating client thread: " + ex.getMessage());
            }
        }

        public void run() {
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null) {
                    messageArea.append(username + ": " + inputLine + "\n");
                    for (PrintWriter writer : clientWriters) {
                        writer.println(username + ": " + inputLine);
                    }
                }
            } catch (IOException ex) {
                System.err.println("Error handling client input: " + ex.getMessage());
            } finally {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                    clientSockets.remove(clientSocket);
                    clientWriters.remove(out);
                    usernames.remove(username); // Xóa tên người dùng khỏi danh sách
                    userList.setListData(usernames.toArray(new String[usernames.size()])); // Cập nhật bảng hiển thị danh sách người dùng
                    messageArea.append(username + " has been disconnected.\n");
                    for (PrintWriter writer : clientWriters) {
                        writer.println("USERLIST: " + usernames);
                    }

                    for (PrintWriter writer : clientWriters) {
                        writer.println(username + " has been disconnected.");
                    }

                } catch (IOException ex) {
                    System.err.println("Error closing client thread: " + ex.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        new ChatServer();
    }
}
