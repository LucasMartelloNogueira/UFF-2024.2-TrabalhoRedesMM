import java.util.Timer;
import java.util.TimerTask;

public class WaitingTimer {
    private Timer timer;
    private TimerTask task;
    private boolean isRunning;
    private long delayMillis;
    private long peridodMillis;

    public WaitingTimer(TimerTask task, long delayMillis, long peridodMillis) {
        timer = new Timer();
        this.task = task;
        isRunning = false;
        this.delayMillis = delayMillis;
        this.peridodMillis = peridodMillis;
    }

    public WaitingTimer(long delayMillis, long peridodMillis) {
        timer = new Timer();
        isRunning = false;
        this.delayMillis = delayMillis;
        this.peridodMillis = peridodMillis;
    }

    public void setTask(TimerTask task) {
        this.task = task;
    }

    public void start() {
        if (isRunning) {
            System.out.println("Timer is already running!");
            return;
        }

        if (task == null) {
            System.out.println("No task set. Use setTask() to define the task.");
            return;
        }

        timer.scheduleAtFixedRate(task, delayMillis, peridodMillis);
        isRunning = true;
        System.out.println("Timer started.");
    }

    public void stop() {
        if (!isRunning) {
            System.out.println("Timer is not running!");
            return;
        }

        task.cancel();
        timer.purge();
        isRunning = false;
        System.out.println("Timer stopped.");
    }

    public boolean getIsRunning() {
        return isRunning;
    }
}
