package dev.brny;
// the logger
// made to unclutter the cli
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;

public class Log {
    private static Logger logger;
    public static void setup() {
        logger = Logger.getLogger("dev.brny");
        logger.setUseParentHandlers(false);
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String logFilePath = tempDir + File.separator + "jchat.log";

            FileHandler fileHandler = new FileHandler(logFilePath, true);
            fileHandler.setFormatter(new SimpleFormatter());

            logger.addHandler(fileHandler);
            logger.info("Logger OK " + Protocol.header);
        } catch (IOException e) {
            System.err.println("[ERROR] Logger could not be set up " + e.getMessage());
        }
    }
    public static Logger get() {
        if (logger == null) setup();
        return logger;
    }
}
