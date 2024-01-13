/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chatapp;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.swing.*;

public class ChatClient extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JTextField textField;
    private JTextField usernameField;
    private JTextArea messageArea;
    private JButton sendButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private JList<String> userList; // Thêm bảng hiển thị danh sách người dùng
    private ArrayList<Socket> clientSockets;
    private ArrayList<String> usernames; // Thêm danh sách tên người dùng
    private boolean connected;
    private ArrayList<PrintWriter> writers = new ArrayList<>();

    public ChatClient() {
        super("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textField = new JTextField(40);
        usernameField = new JTextField(10);
        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(80, 30));
        sendButton.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        sendButton.setForeground(Color.blue);
        sendButton.setBorder(BorderFactory.createLoweredBevelBorder());
        sendButton.setBackground(Color.lightGray);
        sendButton.addActionListener(this);

        messageArea = new JTextArea(10, 40);
        messageArea.setEditable(false);
        messageArea.setForeground(Color.blue);
        messageArea.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        messageArea.setBorder(BorderFactory.createLoweredBevelBorder());
        JScrollPane scrollPane = new JScrollPane(messageArea);

        userList = new JList<String>(); // Khởi tạo bảng hiển thị danh sách người dùng
        userList.setBorder(BorderFactory.createLoweredBevelBorder());
        userList.setFont(new Font("Arial", Font.PLAIN, 16));
        userList.setPreferredSize(new Dimension(150, 0));
        userList.setBorder(BorderFactory.createTitledBorder("User List"));
        userList.setForeground(Color.blue);
        JScrollPane userscrollPane = new JScrollPane(userList);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        connectButton.setPreferredSize(new Dimension(80, 30));
        connectButton.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        connectButton.setForeground(Color.red);
        connectButton.setBorder(BorderFactory.createLoweredBevelBorder());
        connectButton.setBackground(Color.lightGray);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        disconnectButton.setPreferredSize(new Dimension(90, 30));
        disconnectButton.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        disconnectButton.setForeground(Color.black);
        disconnectButton.setBorder(BorderFactory.createLoweredBevelBorder());
        disconnectButton.setBackground(Color.lightGray);
        disconnectButton.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Username: "));
        inputPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        inputPanel.add(usernameField);
        usernameField.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        usernameField.setBorder(BorderFactory.createLoweredBevelBorder());
        usernameField.setEnabled(true);

        inputPanel.add(new JLabel("Message: "));
        inputPanel.add(textField);
        textField.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        textField.setBorder(BorderFactory.createLoweredBevelBorder());
        inputPanel.add(sendButton);

        Container contentPane = getContentPane();
        contentPane.add(buttonPanel, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(inputPanel, BorderLayout.SOUTH);
        contentPane.add(userscrollPane, BorderLayout.EAST);

        pack();
        setVisible(true);

        connected = false;
        clientSockets = new ArrayList<>();
        writers = new ArrayList<>();
        usernames = new ArrayList<>(); // Khởi tạo danh sách tên người dùng
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == connectButton) {
            connect();
        } else if (event.getSource() == disconnectButton) {
            disconnect();
        } else if (event.getSource() == sendButton) {
            send();
        }
    }

    private void connect() {
        String username = usernameField.getText();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String serverAddress = JOptionPane.showInputDialog(this, "Enter IP Address of the Server:", "Welcome to the ChatRoom", JOptionPane.QUESTION_MESSAGE);

        if (serverAddress == null || serverAddress.isEmpty()) {
            return;
        }

        try {
            socket = new Socket(serverAddress, 4444);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clientSockets.add(socket); // Thêm socket mới vào danh sách

            out.println(username);
            connected = true;

            messageArea.append("Connected to the chat server.\n");
            messageArea.append("Welcome" + " " + username + "\n");
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            usernameField.setEnabled(false);
            
            username = in.readLine();
            usernames.add(username); // Thêm tên người dùng vào danh sách
            userList.setListData(usernames.toArray(new String[usernames.size()])); // Cập nhật bảng hiển thị danh sách người 

            new Thread(new IncomingReader()).start();
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(this, "Unknown host: " + serverAddress, "Error", JOptionPane.ERROR_MESSAGE);
            usernameField.setEnabled(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            usernameField.setEnabled(true);
        }
    }

    private void disconnect() {
        out.println("bye");
        connected = false;
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        usernameField.setEnabled(true);
        usernames.clear(); // Xóa tên người dùng khỏi danh sách
        userList.setListData(usernames.toArray(new String[usernames.size()])); // Cập nhật bảng hiển thị danh sách người dùng
        try {
            socket.close();
        } catch (IOException e) {
            // do nothing
        }
    }

    private void send() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "You are not connected to the chat server.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String message = textField.getText();
        if (message.isEmpty()) {
            return;
        }

        out.println(message);
        textField.setText("");
    }

    public static void main(String[] args) {
        new ChatClient();
    }

class IncomingReader implements Runnable {

    public void run() {
        try {
            while (connected) {
                String message = in.readLine();
                if (message == null) {
                    break;
                }
                if (message.startsWith("USERLIST: ")) {
                    String[] usernames = message.substring(11, message.length()-1).split(",");
                    ArrayList<String> userListData = new ArrayList<>();
                    for (String username : usernames) {
                        userListData.add(username.trim());
                    }
                    // cập nhật danh sách người dùng trên bảng hiển thị
                    userList.setListData(userListData.toArray(new String[userListData.size()]));
                } else {
                    messageArea.append(message + "\n");
                    messageArea.setCaretPosition(messageArea.getDocument().getLength());
                    for (PrintWriter writer : writers) {
                        writer.println(message);
                    }
                }
            }
        } catch (IOException e) {
            // do nothing
        } finally {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            usernameField.setEnabled(true);
            usernames.clear();
            userList.setListData(usernames.toArray(new String[usernames.size()]));
            messageArea.append("You have disconnected. Reconnect.\n");
        }
    }
}

}
