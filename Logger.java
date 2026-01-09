import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Logger class handles game logging to console and file
 */
public class Logger {
    private List<String> logs;
    private String filename;
    private boolean fileLoggingEnabled;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public Logger() {
        this.logs = new ArrayList<>();
        this.filename = "connect5_game_" + System.currentTimeMillis() + ".txt";
        this.fileLoggingEnabled = true;
    }
    
    public Logger(String customFilename) {
        this();
        this.filename = customFilename;
    }
    
    /**
     * Log a message to console and file
     */
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = "[" + timestamp + "] " + message;
        
        System.out.println(logEntry);
        logs.add(logEntry);
        
        if (fileLoggingEnabled) {
            writeToFile(logEntry);
        }
    }
    
    /**
     * Log an error message
     */
    public void logError(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = "[" + timestamp + "] ERROR: " + message;
        
        System.err.println(logEntry);
        logs.add(logEntry);
        
        if (fileLoggingEnabled) {
            writeToFile(logEntry);
        }
    }
    
    /**
     * Log a game event
     */
    public void logGameEvent(String player, String action) {
        log(player + " " + action);
    }
    
    /**
     * Write log entry to file
     */
    private void writeToFile(String logEntry) {
        try (FileWriter fw = new FileWriter(filename, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(logEntry);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
    
    /**
     * Save game summary to file
     */
    public void saveGameSummary(String gameInfo) {
        try (FileWriter fw = new FileWriter(filename, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.newLine();
            bw.write("=== GAME SUMMARY ===");
            bw.newLine();
            bw.write(gameInfo);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Failed to save game summary: " + e.getMessage());
        }
    }
    
    /**
     * Get all logs
     */
    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }
    
    /**
     * Clear logs
     */
    public void clearLogs() {
        logs.clear();
    }
    
    /**
     * Get the log filename
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Enable or disable file logging
     */
    public void setFileLoggingEnabled(boolean enabled) {
        this.fileLoggingEnabled = enabled;
    }
}
