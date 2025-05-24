import javax.swing.*;
import java.sql.*;
import java.util.List;

public class UserManager {
    private List<User> users;

    public UserManager(List<User> users) {
        this.users = users;
        loadUsersFromDB();
    }

    private void loadUsersFromDB() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM users");
            
            users.clear();
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("role"),
                    rs.getString("password")
                ));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading users: " + e.getMessage());
        } finally {
            DBConnection.close(conn, stmt, rs);
        }
    }

    public void listUsers() {
        loadUsersFromDB();
        StringBuilder list = new StringBuilder("User List:\n\n");
        for (User u : users) {
            list.append(u).append("\n");
        }
        JOptionPane.showMessageDialog(null, list.toString());
    }

    public void addUser() {
        try {
            String name = JOptionPane.showInputDialog("Enter user name:");
            if (name == null || name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Name cannot be empty");
                return;
            }

            String[] roles = {"Admin", "Librarian", "Member"};
            String role = (String) JOptionPane.showInputDialog(null, "Select role:", 
                "User Role", JOptionPane.QUESTION_MESSAGE, null, roles, roles[2]);
            if (role == null) return;

            String password = JOptionPane.showInputDialog("Enter password:");
            if (password == null || password.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Password cannot be empty");
                return;
            }

            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = DBConnection.getConnection();
                pstmt = conn.prepareStatement("INSERT INTO users (name, role, password) VALUES (?, ?, ?)");
                pstmt.setString(1, name);
                pstmt.setString(2, role);
                pstmt.setString(3, password);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(null, "User added successfully");
                loadUsersFromDB();
            } finally {
                DBConnection.close(conn, pstmt, null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding user: " + e.getMessage());
        }
    }

    public void removeUser() {
        try {
            listUsers();
            String input = JOptionPane.showInputDialog("Enter user ID to remove:");
            if (input == null) return;
            
            int id = Integer.parseInt(input);
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            try {
                conn = DBConnection.getConnection();
                pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int confirm = JOptionPane.showConfirmDialog(null, 
                        "Delete user: " + rs.getString("name") + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        pstmt = conn.prepareStatement("DELETE FROM users WHERE id = ?");
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                        JOptionPane.showMessageDialog(null, "User deleted");
                        loadUsersFromDB();
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "User not found");
                }
            } finally {
                DBConnection.close(conn, pstmt, null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error removing user: " + e.getMessage());
        }
    }

    public void editUser() {
        try {
            listUsers();
            String input = JOptionPane.showInputDialog("Enter user ID to edit:");
            if (input == null) return;
            
            int id = Integer.parseInt(input);
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            try {
                conn = DBConnection.getConnection();
                pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    String currentName = rs.getString("name");
                    String currentRole = rs.getString("role");
                    
                    String newName = JOptionPane.showInputDialog("Enter new name:", currentName);
                    if (newName == null || newName.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Name cannot be empty");
                        return;
                    }
                    
                    String[] roles = {"Admin", "Librarian", "Member"};
                    String newRole = (String) JOptionPane.showInputDialog(null, 
                        "Select role:", "User Role", JOptionPane.QUESTION_MESSAGE, null, roles, currentRole);
                    if (newRole == null) return;
                    
                    pstmt = conn.prepareStatement("UPDATE users SET name = ?, role = ? WHERE id = ?");
                    pstmt.setString(1, newName);
                    pstmt.setString(2, newRole);
                    pstmt.setInt(3, id);
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(null, "User updated");
                    loadUsersFromDB();
                } else {
                    JOptionPane.showMessageDialog(null, "User not found");
                }
            } finally {
                DBConnection.close(conn, pstmt, null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error editing user: " + e.getMessage());
        }
    }
}