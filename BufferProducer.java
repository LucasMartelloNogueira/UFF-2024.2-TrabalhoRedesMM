import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.function.Function;

public class BufferProducer extends Thread {

    String threadName = "PRODUCER";
    SplittableRandom random = new SplittableRandom();

    private int minDelayMiliseconds;
    private int maxDelayMiliseconds;
    private List<PacketData<RTPpacket>> channelBuffer;
    private PlayoutBuffer playoutBuffer;
    private List<PacketData<RTPpacket>> lostPackets;
    private int discartProbabilityPercent;
    private boolean isRunning;
    private Function<Void, Void> consumerCallback;

    public BufferProducer(int minDelayMiliseconds, int maxDelayMiliseconds, PlayoutBuffer playoutBuffer,
            List<PacketData<RTPpacket>> channelBuffer, int discartProbabilityPercent, Function<Void, Void> consumerCallback) {
        this.minDelayMiliseconds = minDelayMiliseconds;
        this.maxDelayMiliseconds = maxDelayMiliseconds;
        this.channelBuffer = channelBuffer;
        this.playoutBuffer = playoutBuffer;
        this.lostPackets = new ArrayList<PacketData<RTPpacket>>();
        this.discartProbabilityPercent = discartProbabilityPercent;
        this.isRunning = true;
        this.consumerCallback = consumerCallback;
    }

    private void print(String message) {
        AppLogger.log(threadName, message);
        Util.printInThread(threadName, message);
    }
    
    // simula quando um pacote é perdido pela rede
    private void fowardToPlayoutBufferOrRandomDiscart(int discartProbabilityPercent, PacketData<RTPpacket> packet) {

        if (discartProbabilityPercent == 0) {
            print(String.format("retirou pacote %d do channelBuffer e adicionou ao playoutBuffer", packet.getPacket().SequenceNumber));
            playoutBuffer.add(packet);
            return;
        }

        boolean willDiscartPacket = random.nextInt(0, 100) <= discartProbabilityPercent;
        if (willDiscartPacket) {
            print(String.format("pacote %d perdido pela rede", packet.getPacket().SequenceNumber));
            lostPackets.add(packet);
        } else {
            print(String.format("retirou pacote %d do channelBuffer e adicionou ao playoutBuffer", packet.getPacket().SequenceNumber));
            playoutBuffer.add(packet);
        }
    }

    public List<PacketData<RTPpacket>> getLostPackets() {
        return this.lostPackets;
    }

    public void addPacket(RTPpacket packet, int sequenceNumber) {
        PacketData<RTPpacket> packetData = new PacketData<RTPpacket>(packet, 0, sequenceNumber);
        channelBuffer.add(packetData);
    }

    public void setIsRunning(boolean running) {
        this.isRunning = running;
    }

    public void run() {

        long timestampMilis = System.currentTimeMillis();

        while (isRunning) {

            print(String.format("channel buffer size: %d", channelBuffer.size()));
            // print("aqui");

            if (channelBuffer.size() > 0) {

                print("removendo pacote do channel buffer");
                
                long networkDelay = new Random().nextInt(maxDelayMiliseconds - minDelayMiliseconds) + minDelayMiliseconds;
                try {
                    Thread.sleep(networkDelay);
                    long now = System.currentTimeMillis();
                    PacketData<RTPpacket> packet = this.channelBuffer.removeFirst();
                    packet.setArrivalTimeMilllis(now);
                    
                    fowardToPlayoutBufferOrRandomDiscart(discartProbabilityPercent, packet);
    
                    long currTimeMilis = System.currentTimeMillis();
                    // print(String.format("intervalo entre pacotes : %d", currTimeMilis - timestampMilis));
                    timestampMilis = currTimeMilis;
                } catch (Exception e) {
                    print(e.toString());
                    // print("erro ao deixar a thread dormir / nao retirou elemento da fila");
    
                }
            }
        }

        consumerCallback.apply(null);
    }
}
