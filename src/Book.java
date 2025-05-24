public class Book {
    public int id;
    public String title;
    public String author;
    public String genre;
    public boolean isAvailable;

    public Book(int id, String title, String author, String genre) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.isAvailable = true;
    }

    @Override
    public String toString() {
        return id + ": " + title + " by " + author + " [" + genre + "] - " + (isAvailable ? "Available" : "Borrowed");
    }
}