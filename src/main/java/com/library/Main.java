package com.library;

import com.library.entity.Book;
import com.library.entity.BookIssue;
import com.library.entity.Member;
import com.library.exception.LibraryException;
import com.library.service.LibraryService;
import com.library.util.HibernateUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/** Console menu. Contains no database code: everything goes through LibraryService. */
public class Main {

    private static final Scanner SC = new Scanner(System.in);
    private static final LibraryService SERVICE = new LibraryService();

    public static void main(String[] args) {
        try {
            HibernateUtil.getSessionFactory();     // connect now so a bad setup is reported immediately
        } catch (LibraryException e) {
            System.out.println("\nERROR: " + e.getMessage());
            return;
        }

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("");
            try {
                switch (choice) {
                    case 1 -> addBook();
                    case 2 -> viewAllBooks();
                    case 3 -> searchBook();
                    case 4 -> updateBook();
                    case 5 -> deleteBook();
                    case 6 -> addMember();
                    case 7 -> viewAllMembers();
                    case 8 -> searchMember();
                    case 9 -> updateMember();
                    case 10 -> deleteMember();
                    case 11 -> issueBook();
                    case 12 -> returnBook();
                    case 13 -> viewIssuedBooks();
                    case 14 -> viewMemberHistory();
                    case 15 -> running = false;
                    default -> System.out.println("Invalid choice. Please enter a number from 1 to 15.");
                }
            } catch (LibraryException e) {
                System.out.println("\nERROR: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("\nUnexpected error: " + e.getMessage());
            }
        }
        HibernateUtil.shutdown();
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=================================");
        System.out.println("     LIBRARY MANAGEMENT SYSTEM");
        System.out.println("=================================");
        System.out.println();
        System.out.println("1. Add Book");
        System.out.println("2. View All Books");
        System.out.println("3. Search Book");
        System.out.println("4. Update Book");
        System.out.println("5. Delete Book");
        System.out.println();
        System.out.println("6. Add Member");
        System.out.println("7. View All Members");
        System.out.println("8. Search Member");
        System.out.println("9. Update Member");
        System.out.println("10. Delete Member");
        System.out.println();
        System.out.println("11. Issue Book");
        System.out.println("12. Return Book");
        System.out.println("13. View Issued Books");
        System.out.println("14. View Member Borrowing History");
        System.out.println();
        System.out.println("15. Exit");
        System.out.println();
        System.out.print("Enter your choice: ");
    }

    // ------------------------------------------------------------------ books

    private static void addBook() {
        System.out.println("\n--- Add Book ---");
        String title = readRequired("Title: ");
        String author = readRequired("Author: ");
        String isbn = readRequired("ISBN: ");
        String category = readLine("Category: ");
        int quantity = readPositiveInt("Quantity: ");
        Book book = SERVICE.addBook(title, author, isbn, category, quantity);
        System.out.println("Book added successfully. ID = " + book.getBookId());
    }

    private static void viewAllBooks() {
        System.out.println("\n--- All Books ---");
        printBooks(SERVICE.getAllBooks());
        System.out.println("Total titles: " + SERVICE.countBooks());
    }

    private static void searchBook() {
        System.out.println("\n--- Search Book ---");
        System.out.println("1. By title");
        System.out.println("2. By author");
        System.out.println("3. Available books only");
        int option = readInt("Choose: ");
        switch (option) {
            case 1 -> printBooks(SERVICE.searchBooksByTitle(readRequired("Title contains: ")));
            case 2 -> printBooks(SERVICE.searchBooksByAuthor(readRequired("Author contains: ")));
            case 3 -> printBooks(SERVICE.getAvailableBooks());
            default -> System.out.println("Invalid option.");
        }
    }

    private static void updateBook() {
        System.out.println("\n--- Update Book (press Enter to keep the current value) ---");
        long id = readLong("Book ID: ");
        Book book = SERVICE.getBook(id);
        if (book == null) {
            System.out.println("Book not found with ID " + id + ".");
            return;
        }
        System.out.println("Current: " + book);
        String title = readLine("New title: ");
        String author = readLine("New author: ");
        String isbn = readLine("New ISBN: ");
        String category = readLine("New category: ");
        Integer quantity = readOptionalInt("New total quantity: ");
        SERVICE.updateBook(id, title, author, isbn, category, quantity);
        System.out.println("Book updated successfully.");
    }

    private static void deleteBook() {
        System.out.println("\n--- Delete Book ---");
        long id = readLong("Book ID: ");
        if (confirm("Delete book " + id + "?")) {
            SERVICE.deleteBook(id);
            System.out.println("Book deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // ---------------------------------------------------------------- members

    private static void addMember() {
        System.out.println("\n--- Add Member ---");
        String name = readRequired("Name: ");
        String email = readRequired("Email: ");
        String phone = readLine("Phone: ");
        String address = readLine("Address: ");
        Member member = SERVICE.addMember(name, email, phone, address);
        System.out.println("Member added successfully. ID = " + member.getMemberId());
    }

    private static void viewAllMembers() {
        System.out.println("\n--- All Members ---");
        printMembers(SERVICE.getAllMembers());
    }

    private static void searchMember() {
        System.out.println("\n--- Search Member ---");
        printMembers(SERVICE.searchMemberByName(readRequired("Name contains: ")));
    }

    private static void updateMember() {
        System.out.println("\n--- Update Member (press Enter to keep the current value) ---");
        long id = readLong("Member ID: ");
        Member member = SERVICE.getMember(id);
        if (member == null) {
            System.out.println("Member not found with ID " + id + ".");
            return;
        }
        System.out.println("Current: " + member);
        String name = readLine("New name: ");
        String email = readLine("New email: ");
        String phone = readLine("New phone: ");
        String address = readLine("New address: ");
        SERVICE.updateMember(id, name, email, phone, address);
        System.out.println("Member updated successfully.");
    }

    private static void deleteMember() {
        System.out.println("\n--- Delete Member ---");
        long id = readLong("Member ID: ");
        if (confirm("Delete member " + id + "?")) {
            SERVICE.deleteMember(id);
            System.out.println("Member deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // ----------------------------------------------------------- issue/return

    private static void issueBook() {
        System.out.println("\n--- Issue Book ---");
        long memberId = readLong("Member ID: ");
        long bookId = readLong("Book ID: ");
        BookIssue issue = SERVICE.issueBook(memberId, bookId);
        System.out.println("Book issued successfully!");
        System.out.println("  Issue ID   : " + issue.getIssueId());
        System.out.println("  Book       : " + issue.getBook().getTitle());
        System.out.println("  Member     : " + issue.getMember().getName());
        System.out.println("  Issue date : " + issue.getIssueDate());
        System.out.println("  Due date   : " + issue.getDueDate());
        System.out.println("  Copies left: " + issue.getBook().getAvailableQuantity());
    }

    private static void returnBook() {
        System.out.println("\n--- Return Book ---");
        long issueId = readLong("Issue ID: ");
        BookIssue issue = SERVICE.returnBook(issueId);
        System.out.println("Book returned successfully!");
        System.out.println("  Book        : " + issue.getBook().getTitle());
        System.out.println("  Member      : " + issue.getMember().getName());
        System.out.println("  Due date    : " + issue.getDueDate());
        System.out.println("  Return date : " + issue.getReturnDate());
        if (issue.getFineAmount().signum() > 0) {
            System.out.println("  Fine        : Rs. " + issue.getFineAmount()
                    + " (Rs. " + LibraryService.FINE_PER_DAY + " per late day)");
        } else {
            System.out.println("  Fine        : Rs. 0 (returned on time)");
        }
        System.out.println("  Copies available now: " + issue.getBook().getAvailableQuantity());
    }

    private static void viewIssuedBooks() {
        System.out.println("\n--- Issued Books (not yet returned) ---");
        List<BookIssue> issues = SERVICE.getActiveIssues();
        printIssues(issues);
        System.out.println("Active issues: " + SERVICE.countActiveIssues()
                + "   Overdue: " + SERVICE.getOverdueIssues().size());
    }

    private static void viewMemberHistory() {
        System.out.println("\n--- Member Borrowing History ---");
        long memberId = readLong("Member ID: ");
        Member member = SERVICE.getMember(memberId);
        if (member == null) {
            System.out.println("Member not found with ID " + memberId + ".");
            return;
        }
        System.out.println("Member: " + member.getName());
        printIssues(SERVICE.getIssuesByMember(memberId));
    }

    // --------------------------------------------------------------- printing

    private static void printBooks(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        System.out.printf("%-4s %-34s %-24s %-15s %-12s %5s %5s%n",
                "ID", "Title", "Author", "ISBN", "Category", "Qty", "Avail");
        System.out.println("-".repeat(104));
        for (Book b : books) {
            System.out.printf("%-4d %-34s %-24s %-15s %-12s %5d %5d%n",
                    b.getBookId(), cut(b.getTitle(), 34), cut(b.getAuthor(), 24), b.getIsbn(),
                    cut(b.getCategory(), 12), b.getQuantity(), b.getAvailableQuantity());
        }
    }

    private static void printMembers(List<Member> members) {
        if (members.isEmpty()) {
            System.out.println("No members found.");
            return;
        }
        System.out.printf("%-4s %-20s %-30s %-12s %s%n", "ID", "Name", "Email", "Phone", "Address");
        System.out.println("-".repeat(100));
        for (Member m : members) {
            System.out.printf("%-4d %-20s %-30s %-12s %s%n", m.getMemberId(), cut(m.getName(), 20),
                    cut(m.getEmail(), 30), m.getPhone() == null ? "" : m.getPhone(),
                    m.getAddress() == null ? "" : m.getAddress());
        }
    }

    private static void printIssues(List<BookIssue> issues) {
        if (issues.isEmpty()) {
            System.out.println("No issue records found.");
            return;
        }
        System.out.printf("%-6s %-28s %-16s %-11s %-11s %-11s %-9s %6s%n",
                "Issue", "Book", "Member", "Issued", "Due", "Returned", "Status", "Fine");
        System.out.println("-".repeat(106));
        LocalDate today = LocalDate.now();
        for (BookIssue i : issues) {
            String status = i.getStatus();
            if (BookIssue.STATUS_ISSUED.equals(status) && i.getDueDate().isBefore(today)) {
                status = "OVERDUE";
            }
            System.out.printf("%-6d %-28s %-16s %-11s %-11s %-11s %-9s %6s%n",
                    i.getIssueId(), cut(i.getBook().getTitle(), 28), cut(i.getMember().getName(), 16),
                    i.getIssueDate(), i.getDueDate(),
                    i.getReturnDate() == null ? "-" : i.getReturnDate().toString(),
                    status, i.getFineAmount());
        }
    }

    private static String cut(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    // ------------------------------------------------------------ safe input

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SC.hasNextLine()) {          // input stream closed (Ctrl+D): leave the program cleanly
            HibernateUtil.shutdown();
            System.exit(0);
        }
        return SC.nextLine().trim();
    }

    private static String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    /** Reads an int; keeps asking until the user types a valid whole number. */
    private static int readInt(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("The number must be greater than 0.");
        }
    }

    private static long readLong(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    /** Blank input returns null (= keep current value). */
    private static Integer readOptionalInt(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return null;
            }
            try {
                int value = Integer.parseInt(text);
                if (value >= 0) {
                    return value;
                }
                System.out.println("The number cannot be negative.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number or press Enter to skip.");
            }
        }
    }

    private static boolean confirm(String question) {
        while (true) {
            String answer = readLine(question + " (y/n): ").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("Please type y or n.");
        }
    }
}
