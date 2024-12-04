import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AppLogger {

    private static final Logger logger = initlogger();
    
    public static void log(String label, String msg) {
        if (logger != null) {
            logger.info(Util.getLabelMsg(label, msg));
        } else {
            System.out.println("Log == null");
            System.exit(1);
        }
    }
    
    private static String getLogName() {
        File dir = new File("../logs");
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            int len = files != null ? files.length : 0;
            return String.valueOf(len);
        }

        return "0";
    }

    private static Logger initlogger() {
        try {
            Logger logger = Logger.getLogger(AppLogger.class.getName());
            FileHandler fileHandler = new FileHandler(String.format("../logs/%s.log", getLogName()), true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
            return logger;
        } catch (Exception e) {
            System.out.println("Failed to set up Logger");
            System.exit(1);
        }

        return null;
    }

}
