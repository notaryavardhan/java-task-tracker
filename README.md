# Java CLI Task Tracker

A fast, customizable, and persistent command-line task manager written entirely in Java. No bloated GUIs—just a clean, highly functional terminal interface to help you get things done.

## 🚀 Features

* **Dynamic Visual Progress Bar:** Watch a real-time progress bar fill up and track your completion percentage as you check off tasks.
* **Custom Terminal Theming:** Personalize your UI. Use ANSI color codes to change the menu text, box border colors, progress bar color, and choose your preferred box style (Solid, Dashes, or Dots).
* **Smart Task Sorting:** The application automatically organizes your list—sorting first by priority (HIGH, MEDIUM, LOW), and then chronologically by due date.
* **Intelligent Date Parsing:** Enter exact due dates and times, or leave them blank to let the app automatically default to the end of the day, month, or year based on your settings.
* **Detailed Task Notes:** Add extended sub-notes and details to any task for better context.
* **Persistent Auto-Saving:** Built with Java Object Serialization, all your tasks and custom UI settings are securely written to local files (`.ser`). You will never lose your data between sessions.
* **Crash-Proof Validation:** Built-in error handling prevents the application from crashing if you accidentally type a letter instead of a number.

---

## 💻 How to Run

**1. Compile the code:**
Open your terminal in the project folder and run the Java compiler:
`javac TaskTracker.java`

**2. Start the tracker:**
Once compiled, launch the application by running:
`java TaskTracker`

**3. Usage:**
Navigate the menu using your number keys. When you are finished, always use the Exit option in the menu to ensure your latest tasks and theme settings are safely saved to your local drive.
