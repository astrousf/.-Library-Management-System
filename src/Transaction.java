import java.sql.Date;

public class Transaction {
    public int userId;
    public int bookId;
    public String action;
    public Date dueDate;
    public Date returnDate;
    public double fineAmount;
    
    public Transaction(int userId, int bookId, String action) {
        this(userId, bookId, action, null, null, 0.0);
    }
    
    public Transaction(int userId, int bookId, String action, Date dueDate, Date returnDate, double fineAmount) {
        this.userId = userId;
        this.bookId = bookId;
        this.action = action;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User ID ").append(userId).append(" ").append(action).append(" Book ID ").append(bookId);
        if (dueDate != null) {
            sb.append(" (Due: ").append(dueDate).append(")");
        }
        if (fineAmount > 0) {
            sb.append(" [Fine: $").append(String.format("%.2f", fineAmount)).append("]");
        }
        return sb.toString();
    }
}