import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class BufferConsumer extends Thread {

    String threadName = "CONSUMER";    
    private PlayoutBuffer playoutBuffer;
    private List<PacketData<RTPpacket>> channelBuffer;
    private long start;
    private Function<Long, Void> callback;
    private boolean isRunning;
    
    public BufferConsumer(PlayoutBuffer playoutBuffer, List<PacketData<RTPpacket>> channelBuffer, long start, Function<Long, Void> callback) {
        this.playoutBuffer = playoutBuffer;
        this.channelBuffer = channelBuffer;
        this.start = start;
        this.callback = callback;
        this.isRunning = true;

    }

    
    private void print(String message) {
        AppLogger.log(threadName, message);
        Util.printInThread(threadName, message);
    }

    public void setIsRunning(boolean running) {
        this.isRunning = running;
    }

    @Override
    public void run(){

        try {
            long start = System.currentTimeMillis();
            print(String.format("add initial delay of %d milliseconds... / start = %d", playoutBuffer.getInitialDelayMillis(), start));
            // print(String.format("now: %s", new Date(System.currentTimeMillis()).toString()));
            Thread.sleep(playoutBuffer.getInitialDelayMillis());
            long end = System.currentTimeMillis();
            print(String.format("end inital playout buffer delay / diff = %d", end-start));
            print(String.format("size of playout buffer after delay: %d", playoutBuffer.getSize()));
        } catch (Exception e) {
            print("error: unable to make BufferConsumer thread sleep for initial delay");
        }
        
        // !(channelBuffer.isEmpty()) || !(playoutBuffer.isEmpty())
        while (this.isRunning || !(channelBuffer.isEmpty()) || !(playoutBuffer.isEmpty())) {
            
            print(String.format("isRunning: %b", isRunning));
            print(String.format("channel buffer size: %d", channelBuffer.size()));
            print(String.format("playout buffer size: %d", playoutBuffer.getSize()));
            

            if (this.playoutBuffer.isEmpty()) {
                // print("playout buffer vazio");
                try {
                    print("rebuffering...");
                    Thread.sleep(playoutBuffer.getRebufferingDelayMillis());
                    playoutBuffer.rebuffer();
                } catch (Exception e) {
                    // print("error: unable to make BufferConsumer thread sleep for rebuffering delay");
                }
            }
            
            try {
                print("consuming packet");
                playoutBuffer.consumePacket();
                Thread.sleep(playoutBuffer.getConsumePeriodMillis());
            } catch (java.util.NoSuchElementException e){
                // print("acabaram os pacotes");
            } catch (InterruptedException e) {
                // print(String.format("error: unable to make BufferConsumer thread sleep for consume delay"));
            } 
        }

        callback.apply(start);
    }

}
