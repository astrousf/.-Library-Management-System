import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class App {
    private static List<Book> books = new ArrayList<>();
    private static List<User> users = new ArrayList<>();
    private static List<Transaction> transactions = new ArrayList<>();
    private static User currentUser;
    private static UserManager userManager;
    private static LibraryManager libraryManager;
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            Font mainFont = new Font("SansSerif", Font.PLAIN, 14);
            Color primaryColor = new Color(51, 102, 153);
            Color accentColor = new Color(0, 153, 204);
            
            UIManager.put("Button.background", new Color(240, 240, 240));
            UIManager.put("Button.foreground", new Color(50, 50, 50));
            UIManager.put("Button.font", mainFont);
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
            UIManager.put("Button.select", accentColor);

            UIManager.put("OptionPane.background", new Color(250, 250, 250));
            UIManager.put("OptionPane.messageFont", mainFont);
            UIManager.put("OptionPane.buttonFont", mainFont);
            UIManager.put("OptionPane.messageForeground", new Color(50, 50, 50));
            
            UIManager.put("Panel.background", new Color(250, 250, 250));
            UIManager.put("Label.font", mainFont);
            UIManager.put("Label.foreground", new Color(50, 50, 50));
            
            UIManager.put("TextField.font", mainFont);
            UIManager.put("TextField.caretForeground", primaryColor);
            UIManager.put("TextField.selectionBackground", accentColor);
            
        } catch (Exception e) {
            System.out.println("Could not set look and feel: " + e.getMessage());
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found.");
            System.exit(1);
        }

        userManager = new UserManager(users);
        libraryManager = new LibraryManager(books, transactions);

        boolean loggedIn = false;
        while (!loggedIn) {
            String username = JOptionPane.showInputDialog(null, "Enter username:", 
                "Library System Login", JOptionPane.PLAIN_MESSAGE);
            if (username == null) {
                System.exit(0);
            }

            String password = JOptionPane.showInputDialog(null, "Enter password:", 
                "Library System Login", JOptionPane.PLAIN_MESSAGE);
            if (password == null) {
                System.exit(0);
            }

            currentUser = authenticateUser(username, password);
            if (currentUser != null) {
                loggedIn = true;
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password. Try again.", 
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }

        createMainWindow();
    }

    private static User authenticateUser(String username, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement("SELECT * FROM users WHERE name = ? AND password = ?");
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("role"),
                    rs.getString("password")
                );
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error authenticating user: " + e.getMessage());
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
        return null;
    }

    private static void createMainWindow() {
        JFrame frame = new JFrame("Library System - " + currentUser.name + " (" + currentUser.role + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 650);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBackground(new Color(51, 102, 153));
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Library Management System");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel userLabel = new JLabel(currentUser.name + " (" + currentUser.role + ")");
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        userLabel.setForeground(Color.WHITE);
        headerPanel.add(userLabel, BorderLayout.EAST);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1, 10, 10));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JButton listBooksBtn = createStyledButton("List Books");
        JButton searchBookBtn = createStyledButton("Search Book");
        JButton borrowBookBtn = createStyledButton("Borrow Book");
        JButton returnBookBtn = createStyledButton("Return Book");
        JButton manageBooksBtn = createStyledButton("Manage Books");
        JButton listUsersBtn = createStyledButton("List Users");
        JButton viewTransactionsBtn = createStyledButton("View Transactions");
        JButton addUserBtn = createStyledButton("Add User");
        JButton removeUserBtn = createStyledButton("Remove User");
        JButton editUserBtn = createStyledButton("Edit User");
        JButton calculateFinesBtn = createStyledButton("Calculate Fines");
        JButton exitBtn = createStyledButton("Exit");


        buttonPanel.add(listBooksBtn);
        buttonPanel.add(searchBookBtn);

        if (currentUser.is("Member") || currentUser.is("Librarian")) {
            buttonPanel.add(borrowBookBtn);
            buttonPanel.add(returnBookBtn);
        }

        if (currentUser.is("Admin") || currentUser.is("Librarian")) {
            buttonPanel.add(manageBooksBtn);
            buttonPanel.add(listUsersBtn);
            buttonPanel.add(viewTransactionsBtn);
        }

        if (currentUser.is("Admin")) {
            buttonPanel.add(addUserBtn);
            buttonPanel.add(removeUserBtn);
            buttonPanel.add(editUserBtn);
            buttonPanel.add(calculateFinesBtn);
        }

        buttonPanel.add(exitBtn);

        listBooksBtn.addActionListener(e -> libraryManager.listBooks());
        searchBookBtn.addActionListener(e -> libraryManager.searchBooks());
        borrowBookBtn.addActionListener(e -> libraryManager.borrowBook(currentUser));
        returnBookBtn.addActionListener(e -> libraryManager.returnBook(currentUser));
        manageBooksBtn.addActionListener(e -> libraryManager.manageBooks());
        listUsersBtn.addActionListener(e -> userManager.listUsers());
        viewTransactionsBtn.addActionListener(e -> libraryManager.showTransactions());
        addUserBtn.addActionListener(e -> userManager.addUser());
        removeUserBtn.addActionListener(e -> userManager.removeUser());
        editUserBtn.addActionListener(e -> userManager.editUser());
        calculateFinesBtn.addActionListener(e -> {
            userManager.listUsers();
            try {
                String input = JOptionPane.showInputDialog("Enter user ID to calculate fines:");
                if (input == null) return;
                
                int userId = Integer.parseInt(input);
                User selectedUser = users.stream()
                    .filter(u -> u.id == userId)
                    .findFirst()
                    .orElse(null);
                
                if (selectedUser != null) {
                    libraryManager.calculateUserFines(selectedUser);
                } else {
                    JOptionPane.showMessageDialog(null, "User not found.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid user ID.");
            }
        });
        
        exitBtn.addActionListener(e -> System.exit(0));

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        
        JLabel footerLabel = new JLabel("© 2025 Library Management System");
        footerLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(120, 120, 120));
        footerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        footerLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);
        
        frame.add(mainPanel);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setBackground(new Color(240, 240, 240));
        button.setForeground(new Color(51, 51, 51));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(230, 230, 230));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(240, 240, 240));
            }
        });
        
        return button;
    }
}
