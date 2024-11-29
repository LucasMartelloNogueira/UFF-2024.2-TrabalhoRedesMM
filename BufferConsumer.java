import java.util.List;

import javax.swing.JLabel;

public class BufferConsumer extends Thread {

    String threadName = "CONSUMER";
    String separator = "=";
    
    private List<PacketData<RTPpacket>> channelBuffer;
    private PlayoutBuffer playoutBuffer;
    private JLabel iconLabel;
    
    public BufferConsumer(List<PacketData<RTPpacket>> channelBuffer, PlayoutBuffer playoutBuffer, JLabel iconLabel) {
        this.channelBuffer = channelBuffer;
        this.playoutBuffer = playoutBuffer;
        this.iconLabel = iconLabel;
    }

    private void print(String message) {
        Util.printInThread(threadName, separator, message);
    }

    public void run(){

        long start = System.currentTimeMillis();

        try {
            print("adding playout buffer delay...");
            Thread.sleep(playoutBuffer.getInitialDelayMillis());
        } catch (Exception e) {
            print("error: unable to make BufferConsumer thread sleep for initial delay");
        }
        
        // !(channelBuffer.isEmpty()) || !(playoutBuffer.isEmpty())
        while (true) {
            
            if (this.playoutBuffer.isEmpty()) {
                print("playout buffer vazio");
                try {
                    print("rebuffering...");
                    Thread.sleep(playoutBuffer.getRebufferingDelayMillis());
                    playoutBuffer.rebuffer();
                } catch (Exception e) {
                    print("error: unable to make BufferConsumer thread sleep for rebuffering delay");
                }
            }
            
            try {
                playoutBuffer.consumePacket(iconLabel);
                Thread.sleep(playoutBuffer.getConsumePeriodMillis());
            } catch (java.util.NoSuchElementException e){
                print("acabaram os pacotes");
            } catch (InterruptedException e) {
                print(String.format("error: unable to make BufferConsumer thread sleep for consume delay"));
            } 
        }

        // long end = System.currentTimeMillis();
        // print(String.format("END - total time spent: %d", end-start));

        // StringBuffer sb = new StringBuffer();
        // sb.append("{");
        // playoutBuffer.getLatePackets().forEach(packetData -> sb.append(packetData.getPacket()));
        // sb.append("}");

        // print(String.format("late packets: %s", sb.toString()));

        // StringBuffer sb2 = new StringBuffer();
        // sb2.append("{");
        // playoutBuffer.getOutOfOrderPackets().forEach(packetData -> sb.append(packetData.getPacket()));
        // sb2.append("}");

        // print(String.format("out of order packets: %s", sb2.toString()));

        // print(String.format("time spent rebuffering: %d", playoutBuffer.getTimeSpentRebuffering()));
        // print(String.format("number of rebufferings: %d", playoutBuffer.getNumTimesRebuffering()));
    }

}
