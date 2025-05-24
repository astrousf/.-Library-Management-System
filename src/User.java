public class User {
    public int id;
    public String name;
    public String role;
    public String password;

    public User(int id, String name, String role, String password) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.password = password;
    }

    public boolean is(String r) {
        return this.role.equalsIgnoreCase(r);
    }

    @Override
    public String toString() {
        return id + ": " + name + " (" + role + ")";
    }
}