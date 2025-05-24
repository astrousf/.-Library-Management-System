import javax.swing.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LibraryManager {
    private List<Book> books;
    private List<Transaction> transactions;

    public LibraryManager(List<Book> books, List<Transaction> transactions) {
        this.books = books;
        this.transactions = transactions;
        loadBooksFromDB();
        loadTransactionsFromDB();
    }

    private void loadBooksFromDB() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM books");
            
            books.clear();
            while (rs.next()) {
                Book book = new Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("genre")
                );
                book.isAvailable = rs.getBoolean("is_available");
                books.add(book);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading books: " + e.getMessage());
        } finally {
            DBConnection.close(conn, stmt, rs);
        }
    }

    private void loadTransactionsFromDB() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM transactions");
            
            transactions.clear();
            while (rs.next()) {
                transactions.add(new Transaction(
                    rs.getInt("user_id"),
                    rs.getInt("book_id"),
                    rs.getString("action"),
                    rs.getDate("due_date"),
                    rs.getDate("return_date"),
                    rs.getDouble("fine_amount")
                ));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading transactions: " + e.getMessage());
        } finally {
            DBConnection.close(conn, stmt, rs);
        }
    }

    public void listBooks() {
        loadBooksFromDB();
        StringBuilder list = new StringBuilder("Book List:\n\n");
        for (Book b : books) {
            list.append(b).append("\n");
        }
        JOptionPane.showMessageDialog(null, list.toString());
    }

    public void borrowBook(User user) {
        try {
            String input = JOptionPane.showInputDialog("Enter book ID to borrow:");
            if (input == null) return;
            
            int bookId = Integer.parseInt(input);
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            try {
                conn = DBConnection.getConnection();
                pstmt = conn.prepareStatement("SELECT * FROM books WHERE id = ?");
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    if (rs.getBoolean("is_available")) {
                        LocalDate dueDate = LocalDate.now().plusDays(7);
                        
                        pstmt = conn.prepareStatement("UPDATE books SET is_available = FALSE WHERE id = ?");
                        pstmt.setInt(1, bookId);
                        pstmt.executeUpdate();
                        
                        pstmt = conn.prepareStatement(
                            "INSERT INTO transactions (user_id, book_id, action, due_date) VALUES (?, ?, 'borrowed', ?)");
                        pstmt.setInt(1, user.id);
                        pstmt.setInt(2, bookId);
                        pstmt.setDate(3, Date.valueOf(dueDate));
                        pstmt.executeUpdate();
                        
                        JOptionPane.showMessageDialog(null, 
                            "Book borrowed successfully. Due date: " + dueDate);
                        loadBooksFromDB();
                        loadTransactionsFromDB();
                    } else {
                        JOptionPane.showMessageDialog(null, "Book is already borrowed");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Book not found");
                }
            } finally {
                DBConnection.close(conn, pstmt, null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error borrowing book: " + e.getMessage());
        }
    }

    public void returnBook(User user) {
        try {
            String input = JOptionPane.showInputDialog("Enter book ID to return:");
            if (input == null) return;
            
            int bookId = Integer.parseInt(input);
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            try {
                conn = DBConnection.getConnection();
                pstmt = conn.prepareStatement(
                    "SELECT * FROM transactions WHERE book_id = ? AND user_id = ? AND action = 'borrowed' AND return_date IS NULL");
                pstmt.setInt(1, bookId);
                pstmt.setInt(2, user.id);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    Date dueDate = rs.getDate("due_date");
                    LocalDate returnDate = LocalDate.now();
                    double fine = 0.0;
                    
                    if (returnDate.isAfter(dueDate.toLocalDate())) {
                        long daysLate = ChronoUnit.DAYS.between(dueDate.toLocalDate(), returnDate);
                        fine = daysLate * 1.00; 
                    }
                    
                    pstmt = conn.prepareStatement("UPDATE books SET is_available = TRUE WHERE id = ?");
                    pstmt.setInt(1, bookId);
                    pstmt.executeUpdate();
                    
                    pstmt = conn.prepareStatement(
                        "INSERT INTO transactions (user_id, book_id, action, return_date, fine_amount) VALUES (?, ?, 'returned', ?, ?)");
                    pstmt.setInt(1, user.id);
                    pstmt.setInt(2, bookId);
                    pstmt.setDate(3, Date.valueOf(returnDate));
                    pstmt.setDouble(4, fine);
                    pstmt.executeUpdate();
                    
                    pstmt = conn.prepareStatement(
                        "UPDATE transactions SET return_date = ? WHERE id = ?");
                    pstmt.setDate(1, Date.valueOf(returnDate));
                    pstmt.setInt(2, rs.getInt("id"));
                    pstmt.executeUpdate();
                    
                    String message = "Book returned successfully";
                    if (fine > 0) {
                        message += "\nLate fine: $" + String.format("%.2f", fine);
                    }
                    JOptionPane.showMessageDialog(null, message);
                    
                    loadBooksFromDB();
                    loadTransactionsFromDB();
                } else {
                    JOptionPane.showMessageDialog(null, "No active borrow record found for this book");
                }
            } finally {
                DBConnection.close(conn, pstmt, null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error returning book: " + e.getMessage());
        }
    }

    public void searchBooks() {
        String keyword = JOptionPane.showInputDialog("Enter search keyword (title, author, or genre):");
        if (keyword == null || keyword.trim().isEmpty()) return;
        
        loadBooksFromDB();
        StringBuilder result = new StringBuilder("Search Results:\n\n");
        String searchTerm = keyword.toLowerCase();
        
        for (Book b : books) {
            if (b.title.toLowerCase().contains(searchTerm) ||
                b.author.toLowerCase().contains(searchTerm) ||
                b.genre.toLowerCase().contains(searchTerm)) {
                result.append(b).append("\n");
            }
        }
        
        JOptionPane.showMessageDialog(null, 
            result.length() > 0 ? result.toString() : "No books found matching '" + keyword + "'");
    }

    public void manageBooks() {
        try {
            listBooks();
            String input = JOptionPane.showInputDialog("Enter book ID to manage:");
            if (input == null) return;
            
            int bookId = Integer.parseInt(input);
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            try {
                conn = DBConnection.getConnection();
                pstmt = conn.prepareStatement("SELECT * FROM books WHERE id = ?");
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("genre")
                    );
                    book.isAvailable = rs.getBoolean("is_available");
                    
                    String[] options = {"Edit", "Delete", "Cancel"};
                    int choice = JOptionPane.showOptionDialog(null, 
                        "Manage Book:\n" + book, "Book Management", 
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, 
                        null, options, options[0]);
                    
                    if (choice == 0) {
                        String title = JOptionPane.showInputDialog("Enter new title:", book.title);
                        if (title == null || title.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(null, "Title cannot be empty");
                            return;
                        }
                        
                        String author = JOptionPane.showInputDialog("Enter new author:", book.author);
                        if (author == null || author.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(null, "Author cannot be empty");
                            return;
                        }
                        
                        String genre = JOptionPane.showInputDialog("Enter new genre:", book.genre);
                        if (genre == null || genre.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(null, "Genre cannot be empty");
                            return;
                        }
                        
                        pstmt = conn.prepareStatement(
                            "UPDATE books SET title = ?, author = ?, genre = ? WHERE id = ?");
                        pstmt.setString(1, title);
                        pstmt.setString(2, author);
                        pstmt.setString(3, genre);
                        pstmt.setInt(4, bookId);
                        pstmt.executeUpdate();
                        
                        JOptionPane.showMessageDialog(null, "Book updated successfully");
                        loadBooksFromDB();
                    } else if (choice == 1) { 
                        int confirm = JOptionPane.showConfirmDialog(null, 
                            "Delete this book?", "Confirm", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            pstmt = conn.prepareStatement("DELETE FROM books WHERE id = ?");
                            pstmt.setInt(1, bookId);
                            pstmt.executeUpdate();
                            JOptionPane.showMessageDialog(null, "Book deleted successfully");
                            loadBooksFromDB();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Book not found");
                }
            } finally {
                DBConnection.close(conn, pstmt, null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error managing book: " + e.getMessage());
        }
    }

    public void showTransactions() {
        loadTransactionsFromDB();
        StringBuilder log = new StringBuilder("Transaction History:\n\n");
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(
                "SELECT t.*, u.name as user_name, b.title as book_title " +
                "FROM transactions t " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN books b ON t.book_id = b.id " +
                "ORDER BY t.transaction_date DESC");
            
            while (rs.next()) {
                log.append("User: ").append(rs.getString("user_name")).append("\n");
                log.append("Book: ").append(rs.getString("book_title")).append("\n");
                log.append("Action: ").append(rs.getString("action")).append("\n");
                log.append("Date: ").append(rs.getTimestamp("transaction_date")).append("\n");
                
                if (rs.getString("action").equals("borrowed")) {
                    log.append("Due Date: ").append(rs.getDate("due_date")).append("\n");
                    if (rs.getDate("return_date") != null) {
                        log.append("Returned On: ").append(rs.getDate("return_date")).append("\n");
                    }
                }
                
                if (rs.getDouble("fine_amount") > 0) {
                    log.append("Fine: $").append(String.format("%.2f", rs.getDouble("fine_amount"))).append("\n");
                }
                
                log.append("----------------\n");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading transactions: " + e.getMessage());
        } finally {
            DBConnection.close(conn, stmt, rs);
        }
        
        JOptionPane.showMessageDialog(null, 
            log.length() > 0 ? log.toString() : "No transactions found",
            "Transaction History", JOptionPane.INFORMATION_MESSAGE);
    }

    public void calculateUserFines(User user) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(
                "SELECT SUM(fine_amount) as total_fines FROM transactions WHERE user_id = ?");
            pstmt.setInt(1, user.id);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                double total = rs.getDouble("total_fines");
                JOptionPane.showMessageDialog(null, 
                    "Total fines for " + user.name + ": $" + String.format("%.2f", total));
            } else {
                JOptionPane.showMessageDialog(null, "No fines found for this user");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error calculating fines: " + e.getMessage());
        } finally {
            DBConnection.close(conn, pstmt, rs);
        }
    }
}