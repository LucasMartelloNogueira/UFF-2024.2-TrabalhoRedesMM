import java.util.List;

public class Util {

    public static void printInThread(String threadName, String sepatorSymbol, String message) {
        System.out.printf("[%s]: %s\n", threadName, message);
    }

    public static String listToString(List<Integer> list) {
        StringBuffer sb = new StringBuffer();
        sb.append("{ ");
        list.forEach(item -> sb.append(String.format("%d ", item)));
        sb.append("}");
        return sb.toString();
    }
}
