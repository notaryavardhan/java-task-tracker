import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

enum Priority {
    HIGH, MEDIUM, LOW
}

enum BlankDateBehavior {
    END_OF_DAY, END_OF_MONTH, END_OF_YEAR
}

enum BoxStyle {
    SOLID, DASHES, DOTS
}

class AppSettings implements Serializable {
    private static final long serialVersionUID = 7L;
    public String menu = "\u001B[38;5;255m";
    public String success = "\u001B[38;5;255m";
    public String error = "\u001B[38;5;255m";
    public String muted = "\u001B[38;5;255m";
    public String boxColor = "\u001B[38;5;255m";
    public String progressBar = "\u001B[38;5;255m";
    public String reset = "\u001B[0m";
    public BlankDateBehavior dateBehavior = BlankDateBehavior.END_OF_MONTH;
    public BoxStyle boxStyle = BoxStyle.SOLID;

    public void setCustomColor(String element, int colorCode) {
        String code = "\u001B[38;5;" + colorCode + "m";
        switch (element) {
            case "menu" -> menu = code;
            case "success" -> success = code;
            case "error" -> error = code;
            case "box" -> boxColor = code;
            case "progress" -> progressBar = code;
        }
    }
}

class Task implements Serializable, Comparable<Task> {
    private static final long serialVersionUID = 7L;
    private int id;
    private String description;
    private String details;
    private boolean isCompleted;
    private Priority priority;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;

    public Task(int id, String description, String details, Priority priority, LocalDateTime dueDate) {
        this.id = id;
        this.description = description;
        this.details = details;
        this.isCompleted = false;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void markCompleted() {
        this.isCompleted = true;
    }

    @Override
    public int compareTo(Task other) {
        int priorityComparison = this.priority.compareTo(other.priority);
        if (priorityComparison != 0)
            return priorityComparison;
        return this.dueDate.compareTo(other.dueDate);
    }

    public void printTask(AppSettings settings) {
        String status = isCompleted ? settings.success + "[X]" + settings.reset
                : settings.muted + "[ ]" + settings.reset;
        String prioColor = priority == Priority.HIGH ? "\u001B[38;5;255m" : settings.muted;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm");

        System.out.printf("%d. %s %s (Priority: %s%s%s) - Due: %s\n",
                id, status, description, prioColor, priority, settings.reset, dueDate.format(fmt));
        System.out.printf("   %s└─ Created: %s%s\n", settings.muted, createdAt.format(fmt), settings.reset);

        if (details != null && !details.isEmpty()) {
            System.out.printf("   %s└─ Notes: %s%s\n", settings.muted, details, settings.reset);
        }
    }
}

public class TaskTracker {
    private static final String TASKS_FILE = "tasks_data_v7.ser";
    private static final String SETTINGS_FILE = "settings_v7.ser";

    private ArrayList<Task> tasks;
    private AppSettings settings;
    private int nextId;
    private transient Scanner scanner;

    public TaskTracker() {
        tasks = new ArrayList<>();
        settings = new AppSettings();
        nextId = 1;
        scanner = new Scanner(System.in);
        loadData();
    }

    public void start() {
        boolean running = true;
        while (running) {
            printBoxedMenu();
            System.out.print(settings.menu + "Choose an option: " + settings.reset);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewTasks();
                case "2" -> addTask();
                case "3" -> completeTask();
                case "4" -> deleteTask();
                case "5" -> openSettings();
                case "6" -> {
                    saveData();
                    running = false;
                    System.out.println(settings.success + "Data saved securely. Goodbye!" + settings.reset);
                }
                default -> System.out.println(
                        settings.error + "Invalid option. Please enter a number from 1 to 6." + settings.reset);
            }
        }
    }

    private void printBoxedMenu() {
        char hLine = switch (settings.boxStyle) {
            case SOLID -> '═';
            case DASHES -> '-';
            case DOTS -> '.';
        };
        char vLine = switch (settings.boxStyle) {
            case SOLID -> '║';
            case DASHES -> '|';
            case DOTS -> ':';
        };
        char tl = switch (settings.boxStyle) {
            case SOLID -> '╔';
            default -> '+';
        };
        char tr = switch (settings.boxStyle) {
            case SOLID -> '╗';
            default -> '+';
        };
        char bl = switch (settings.boxStyle) {
            case SOLID -> '╚';
            default -> '+';
        };
        char br = switch (settings.boxStyle) {
            case SOLID -> '╝';
            default -> '+';
        };

        String border = String.valueOf(hLine).repeat(33);

        System.out.println("\n" + settings.boxColor + tl + border + tr + settings.reset);
        System.out.println(settings.boxColor + vLine + settings.menu + "        CLI Task Tracker         "
                + settings.boxColor + vLine + settings.reset);
        System.out.println(settings.boxColor + tl + border + tr + settings.reset);

        String[] options = { "1. View Tasks", "2. Add Task", "3. Complete Task", "4. Delete Task", "5. Settings",
                "6. Exit & Save" };
        for (String opt : options) {
            System.out.printf("%s%c %s%-31s %s%c%s\n", settings.boxColor, vLine, settings.menu, opt, settings.boxColor,
                    vLine, settings.reset);
        }
        System.out.println(settings.boxColor + bl + border + br + settings.reset);

        printProgressBar();
    }

    private void printProgressBar() {
        if (tasks.isEmpty())
            return;
        long completed = tasks.stream().filter(Task::isCompleted).count();
        int percent = (int) ((completed * 100) / tasks.size());
        int filled = percent / 5;

        System.out.print(settings.muted + "Progress: [");
        System.out.print(settings.progressBar + "█".repeat(filled));
        System.out.print(settings.muted + "░".repeat(20 - filled));
        System.out.println("] " + completed + "/" + tasks.size() + " Tasks (" + percent + "%)" + settings.reset + "\n");
    }

    private void viewTasks() {
        if (tasks.isEmpty()) {
            System.out.println(settings.success + "\nNo tasks found. You're all caught up!" + settings.reset);
            return;
        }
        Collections.sort(tasks);
        System.out.println(settings.menu + "\n--- Your Tasks ---" + settings.reset);
        for (Task t : tasks) {
            t.printTask(settings);
            System.out.println();
        }
    }

    private void addTask() {
        System.out.print("Enter task description: ");
        String desc = scanner.nextLine().trim();
        if (desc.isEmpty()) {
            System.out.println(settings.error + "Description cannot be empty. Aborted." + settings.reset);
            return;
        }

        System.out.print(settings.muted + "Enter detailed notes (optional, press Enter to skip): " + settings.reset);
        String details = scanner.nextLine().trim();

        Priority priority = getValidPriority();
        LocalDateTime dueDate = getValidDateTime();

        tasks.add(new Task(nextId++, desc, details, priority, dueDate));
        System.out.println(settings.success + "Task added successfully!" + settings.reset);
    }

    private Priority getValidPriority() {
        while (true) {
            System.out.print("Enter priority (HIGH, MEDIUM, LOW): ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                return Priority.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println(settings.error + "Invalid priority." + settings.reset);
            }
        }
    }

    private LocalDateTime getValidDateTime() {
        while (true) {
            System.out.println("Enter due date (Format options: yyyy-MM, yyyy-MM-dd, or yyyy-MM-dd HH:mm)");
            System.out.print(
                    settings.muted + "[Leave blank to use default: " + settings.dateBehavior + "]: " + settings.reset);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                return switch (settings.dateBehavior) {
                    case END_OF_DAY -> now.toLocalDate().atTime(23, 59);
                    case END_OF_MONTH -> YearMonth.from(now).atEndOfMonth().atTime(23, 59);
                    case END_OF_YEAR -> now.toLocalDate().withMonth(12).withDayOfMonth(31).atTime(23, 59);
                };
            }

            try {
                if (input.length() == 7) {
                    return YearMonth.parse(input).atEndOfMonth().atTime(23, 59);
                } else if (input.length() == 10) {
                    return LocalDate.parse(input).atTime(23, 59);
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    return LocalDateTime.parse(input, formatter);
                }
            } catch (DateTimeParseException e) {
                System.out.println(settings.error + "Invalid format. Use YYYY-MM, YYYY-MM-DD, or YYYY-MM-DD HH:MM"
                        + settings.reset);
            }
        }
    }

    private void completeTask() {
        viewTasks();
        if (tasks.isEmpty())
            return;

        int id = getValidInt("Enter Task ID to mark as completed: ");
        for (Task t : tasks) {
            if (t.getId() == id) {
                t.markCompleted();
                System.out.println(settings.success + "Task completed! Great job." + settings.reset);
                return;
            }
        }
        System.out.println(settings.error + "Task ID not found." + settings.reset);
    }

    private void deleteTask() {
        viewTasks();
        if (tasks.isEmpty())
            return;

        int id = getValidInt("Enter Task ID to delete: ");
        boolean removed = tasks.removeIf(t -> t.getId() == id);
        if (removed) {
            System.out.println(settings.success + "Task successfully deleted." + settings.reset);
        } else {
            System.out.println(settings.error + "Task ID not found." + settings.reset);
        }
    }

    private void openSettings() {
        System.out.println(settings.menu + "\n--- App Settings ---" + settings.reset);
        System.out.println("1. Change Menu Text Color");
        System.out.println("2. Change Menu Box Color");
        System.out.println("3. Change Success Color");
        System.out.println("4. Change Error Color");
        System.out.println("5. Change Progress Bar Color");
        System.out.println("6. Change Menu Box Style (Solid, Dashes, Dots)");
        System.out.println("7. Change Default Blank Date Behavior");
        System.out.println("8. Back to Main Menu");

        String choice = scanner.nextLine().trim();
        if (choice.equals("8"))
            return;

        if (choice.equals("6")) {
            System.out.println("1. SOLID  2. DASHES  3. DOTS");
            int styleChoice = getValidInt("Choose (1-3): ");
            if (styleChoice == 1)
                settings.boxStyle = BoxStyle.SOLID;
            else if (styleChoice == 2)
                settings.boxStyle = BoxStyle.DASHES;
            else if (styleChoice == 3)
                settings.boxStyle = BoxStyle.DOTS;
            return;
        }

        if (choice.equals("7")) {
            System.out.println("1. END_OF_DAY  2. END_OF_MONTH  3. END_OF_YEAR");
            int dateChoice = getValidInt("Choose (1-3): ");
            if (dateChoice == 1)
                settings.dateBehavior = BlankDateBehavior.END_OF_DAY;
            else if (dateChoice == 2)
                settings.dateBehavior = BlankDateBehavior.END_OF_MONTH;
            else if (dateChoice == 3)
                settings.dateBehavior = BlankDateBehavior.END_OF_YEAR;
            return;
        }

        System.out.println(settings.muted + "ANSI color codes: 0 to 255. (e.g., 51=Cyan, 226=Yellow, 196=Red, 114=Mint)"
                + settings.reset);
        int code = getValidInt("Enter ANSI color code (0-255): ");
        if (code < 0 || code > 255)
            return;

        if (choice.equals("1"))
            settings.setCustomColor("menu", code);
        else if (choice.equals("2"))
            settings.setCustomColor("box", code);
        else if (choice.equals("3"))
            settings.setCustomColor("success", code);
        else if (choice.equals("4"))
            settings.setCustomColor("error", code);
        else if (choice.equals("5"))
            settings.setCustomColor("progress", code);
    }

    private int getValidInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(settings.error + "Invalid input." + settings.reset);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TASKS_FILE))) {
            tasks = (ArrayList<Task>) ois.readObject();
            if (!tasks.isEmpty())
                nextId = tasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
        } catch (Exception e) {
            /* First run */ }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SETTINGS_FILE))) {
            settings = (AppSettings) ois.readObject();
        } catch (Exception e) {
            /* Keep defaults */ }
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TASKS_FILE))) {
            oos.writeObject(tasks);
        } catch (IOException e) {
            System.out.println(settings.error + "Error saving tasks." + settings.reset);
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SETTINGS_FILE))) {
            oos.writeObject(settings);
        } catch (IOException e) {
            System.out.println(settings.error + "Error saving settings." + settings.reset);
        }
    }

    public static void main(String[] args) {
        new TaskTracker().start();
    }
}