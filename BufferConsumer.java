import java.util.Date;

public class BufferConsumer extends Thread {

    String threadName = "CONSUMER";
    
    private PlayoutBuffer playoutBuffer;
    
    public BufferConsumer(PlayoutBuffer playoutBuffer) {
        this.playoutBuffer = playoutBuffer;
    }

    private void print(String message) {
        AppLogger.log(threadName, message);
        Util.printInThread(threadName, message);
    }

    public void run(){

        try {
            print("adding playout buffer delay...");
            // print(String.format("now: %s", new Date(System.currentTimeMillis()).toString()));
            Thread.sleep(playoutBuffer.getInitialDelayMillis());
        } catch (Exception e) {
            print("error: unable to make BufferConsumer thread sleep for initial delay");
        }
        
        // !(channelBuffer.isEmpty()) || !(playoutBuffer.isEmpty())
        while (true) {
            
            print(String.format("playout buffer size: %d", playoutBuffer.getSize()));
            print(String.format("now: %s", new Date(System.currentTimeMillis()).toString()));

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

    }

}
