package ChatRoom;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class ChatClient extends JFrame implements ActionListener {

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String serverIP;
    private int serverPort;
    private List<String> privateMessageRecipients;

    private JLabel nameLabel;
    private JTextField nameTextField;
    private JLabel statusLbl;

    private JPanel chatPanel;
    private JTextArea messageArea;
    private JScrollPane messageScrollPane;
    private JTextField messageTextField;
    private JButton sendButton;

    private JPanel userListPanel;
    private JList<String> userList;
    private DefaultListModel<String> model;
    private JScrollPane userScrollPane;

    private JButton connectButton;
    private JButton disconnectButton;

    public ChatClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        privateMessageRecipients = new ArrayList<String>();
        setupUI();
        try {
            connectToServer();
            receiveMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            disconnectFromServer();
        }
    }

    private void setupUI() {
        setTitle("Chat Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel connectPanel = new JPanel(new BorderLayout());
        nameLabel = new JLabel("Name:");
        nameTextField = new JTextField(10);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        connectButton.setEnabled(true);
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(this);
        statusLbl = new JLabel("Not connected to server");
        statusLbl.setForeground(Color.RED);
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(nameLabel);
        namePanel.add(nameTextField);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        connectPanel.add(namePanel, BorderLayout.WEST);
        connectPanel.add(buttonPanel, BorderLayout.EAST);
        connectPanel.add(statusLbl, BorderLayout.SOUTH);
        add(connectPanel, BorderLayout.NORTH);

        chatPanel = new JPanel(new BorderLayout());
        messageArea = new JTextArea();
        messageArea.setFont(new Font("Calibri", Font.PLAIN, 16));
        messageArea.setEditable(false);
        messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        messageTextField = new JTextField();
        messageTextField.setFont(new Font("Calibri", Font.PLAIN, 16));
        messageTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        messageTextField.getActionMap().put("send", new SendAction());
        nameTextField.setEnabled(true);
        sendButton = new JButton("Send");
        sendButton.addActionListener(this);
        chatPanel.add(messageScrollPane, BorderLayout.CENTER);
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageTextField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(messagePanel, BorderLayout.SOUTH);

        userListPanel = new JPanel(new BorderLayout());
        model = new DefaultListModel<String>();
        userList = new JList<String>(model);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userScrollPane = new JScrollPane(userList);
        userListPanel.add(new JLabel("Online Users", SwingConstants.CENTER), BorderLayout.NORTH);
        userListPanel.add(userScrollPane, BorderLayout.CENTER);

        add(chatPanel, BorderLayout.CENTER);
        add(userListPanel, BorderLayout.EAST);

        setMinimumSize(new Dimension(500, 300));
        setPreferredSize(new Dimension(600, 400));
    }

    private void connectToServer() throws IOException {
        socket = new Socket(serverIP, serverPort);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        dos.writeUTF(nameTextField.getText());
        dos.flush();
        statusLbl.setText("Connected to server");
        statusLbl.setForeground(Color.GREEN);
        nameTextField.setEditable(false);
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);
        messageTextField.requestFocusInWindow();
    }

    private void receiveMessages() {
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String message = dis.readUTF();
                         handleMessage(message);
                        messageArea.append(message + "\n");
                    } catch (IOException e) {
                        disconnectFromServer();
                        break;
                    }
                }
            }
        });
        receiveThread.start();
    }

    private void handleMessage(String message) {
        if (message.startsWith("#USER_LIST")) {
            String[] parts = message.split(" ");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    model.clear();
                    for (int i = 1; i < parts.length; i++) {
                        model.addElement(parts[i]);
                    }
                }
            });
        } else if (message.startsWith("#MESSAGE")) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    messageArea.append(message.substring(8) + "\n");
                    JScrollBar vertical = messageScrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
            });
        } else if (message.startsWith("#PRIVATE")) {
            String[] parts = message.split(" ", 3);
            if (parts[1].equals(nameTextField.getText())) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        messageArea.append("(private) " + parts[2] + "\n");
                        JScrollBar vertical = messageScrollPane.getVerticalScrollBar();
                        vertical.setValue(vertical.getMaximum());
                    }
                });
            }
        } else if (message.startsWith("#SET_COLOR")) {
            String[] parts = message.split(" ");
            int r = Integer.parseInt(parts[1]);
            int g = Integer.parseInt(parts[2]);
            int b = Integer.parseInt(parts[3]);
            Color color = new Color(r, g, b);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    messageArea.setBackground(color);
                }
            });
        }
    }

    private void disconnectFromServer() {
        try {
            if (dis != null) {
                dis.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (socket != null) {
                socket.close();
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    messageArea.setText("");
                    model.clear();
                    nameTextField.setEditable(true);
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                    statusLbl.setText("Not connected to server");
                    statusLbl.setForeground(Color.RED);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == connectButton) {
            if (nameTextField.getText().length() == 0) {
                JOptionPane.showMessageDialog(this, "Please enter a name.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    connectToServer();
                    receiveMessages();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (e.getSource() == disconnectButton) {
            disconnectFromServer();
        } else if (e.getSource() == sendButton) {
            if (messageTextField.getText().length() > 0) {
                String recipients = "";
                if (privateMessageRecipients.size() > 0) {
                    for (String recipient : privateMessageRecipients) {
                        recipients += recipient + ",";
                    }
                    recipients = recipients.substring(0, recipients.length() - 1);
                }

                String message = messageTextField.getText();
                
                

                try {
                    if (recipients.isEmpty()) { // Tin nhắn công khai
                        dos.writeUTF(message);
                    } else { // Tin nhắn riêng
                        dos.writeUTF(recipients + " " + message);
                    }
                    dos.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                messageTextField.setText("");
                privateMessageRecipients.clear();
            }
        }

    }

    private class SendAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            sendButton.doClick();
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient("localhost", 11111);
        client.setVisible(true);
    }
}
