import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Util {

    public static void printInThread(String label, String message) {
        System.out.printf("[%s]: %s\n", label, message);
    }

    public static String getLabelMsg(String label, String msg) {
        return String.format("[%s] - %s\n", label, msg);
    }

    public static String listToString(List<Integer> list) {
        StringBuffer sb = new StringBuffer();
        sb.append("{ ");
        list.forEach(item -> sb.append(String.format("%d ", item)));
        sb.append("}");
        return sb.toString();
    }

    public static void writeArrayListToCSV(List<String> data, String filePath) {
    
        StringBuilder csvLine = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            csvLine.append(data.get(i));
            if (i < data.size() - 1) {
                csvLine.append(",");
            }
        }
        csvLine.append("\n");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(csvLine.toString());
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the CSV file: " + e.getMessage());
        }
    }

    public static String getLastFileNumFromDir(String dirName) {
        File dir = new File(dirName);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            int len = files != null ? files.length : 0;
            return String.valueOf(len);
        }

        return "0";
    }
}
