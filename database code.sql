CREATE DATABASE IF NOT EXISTS library_system;
USE library_system;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role ENUM('Admin', 'Librarian', 'Member') NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(100) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    action ENUM('borrowed', 'returned') NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (book_id) REFERENCES books(id)
);

INSERT INTO users (name, role, password) VALUES 
('Youssef', 'Admin', '123'),
('mohammed', 'Librarian', '123'),
('ahmed', 'Member', '123');

INSERT INTO books (title, author, genre) VALUES
('java lerning', 'youssef ayman', 'programming'),
('python learning', 'mohmmed mahmoud', 'programming'),
('GOAT', 'cristiano ronaldo', 'sport'),
('The Last Dance', 'luca modric', 'sport');

ALTER TABLE transactions 
ADD COLUMN due_date DATE,
ADD COLUMN return_date DATE,
ADD COLUMN fine_amount DECIMAL(10,2) DEFAULT 0.00;