import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public class BufferProducer extends Thread {

    String threadName = "PRODUCER";
    String separator = "#";
    SplittableRandom random = new SplittableRandom();

    private int minDelayMiliseconds;
    private int maxDelayMiliseconds;
    private List<PacketData<RTPpacket>> channelBuffer;
    private PlayoutBuffer playoutBuffer;
    private List<PacketData<RTPpacket>> lostPackets;
    private int discartProbabilityPercent;

    public BufferProducer(int minDelayMiliseconds, int maxDelayMiliseconds, PlayoutBuffer playoutBuffer,
            List<PacketData<RTPpacket>> channelBuffer, int discartProbabilityPercent) {
        this.minDelayMiliseconds = minDelayMiliseconds;
        this.maxDelayMiliseconds = maxDelayMiliseconds;
        this.channelBuffer = channelBuffer;
        this.playoutBuffer = playoutBuffer;
        this.lostPackets = new ArrayList<PacketData<RTPpacket>>();
        this.discartProbabilityPercent = discartProbabilityPercent;
    }

    private void print(String message) {
        Util.printInThread(threadName, separator, message);
    }
    
    // simula quando um pacote é perdido pela rede
    private void fowardToPlayoutBufferOrRandomDiscart(int discartProbabilityPercent, PacketData<RTPpacket> packet) {
        boolean willDiscartPacket = random.nextInt(0, 100) <= discartProbabilityPercent;
        if (willDiscartPacket) {
            print(String.format("pacote %d perdido pela rede", packet.getPacket()));
            lostPackets.add(packet);
        } else {
            print(String.format("retirou pacote %d do channelBuffer e adicionou ao playoutBuffer", packet.getPacket()));
            playoutBuffer.add(packet);
        }
    }

    public void addPacket(RTPpacket packet, int sequenceNumber) {
        PacketData<RTPpacket> packetData = new PacketData<RTPpacket>(packet, 0, sequenceNumber);
        channelBuffer.add(packetData);
    }

    public void run() {

        long timestampMilis = System.currentTimeMillis();

        while (true) {

            print(String.format("channel buffer size: %d", channelBuffer.size()));

            if (channelBuffer.size() == 0) {
                continue;
            }

            long networkDelay = new Random().nextInt(maxDelayMiliseconds - minDelayMiliseconds) + minDelayMiliseconds;
            try {
                Thread.sleep(networkDelay);
                long now = System.currentTimeMillis();
                PacketData<RTPpacket> packet = this.channelBuffer.removeFirst();
                packet.setArrivalTimeMilllis(now); // TODO: melhor usar now ou setar tempo quando pacote chega no buffer + o jitter?
            
                fowardToPlayoutBufferOrRandomDiscart(discartProbabilityPercent, packet);

                long currTimeMilis = System.currentTimeMillis();
                print(String.format("intervalo entre pacotes : %d", currTimeMilis - timestampMilis));
                timestampMilis = currTimeMilis;
            } catch (Exception e) {
                print(e.getMessage());
                print("erro ao deixar a thread dormir / nao retirou elemento da fila");

            }
        }

        // StringBuffer sb = new StringBuffer();
        // sb.append("{");
        // lostPackets.forEach(packetData -> sb.append(packetData.getPacket()));
        // sb.append("}");

        // print(String.format("lost packets: %s", sb.toString()));
    }
}
