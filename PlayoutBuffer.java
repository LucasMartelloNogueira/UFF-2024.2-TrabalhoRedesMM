import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class PlayoutBuffer {

    private static final String label = "PLAYOUT-BUFFER";
    private final String timesFilename = getCsvDataFilename();

    private List<PacketData<RTPpacket>> buffer;
    private long initialDelayMillis;
    private long consumePeriodMillis;
    private long rebufferingDelayMillis;
    private int sequenceNumber;
    private int numTimesRebuffering;
    private long firstPacketTimeMillis;
    private long startTime;

    private List<PacketData<RTPpacket>> consumedPackets;
    private List<PacketData<RTPpacket>> latePackets;
    private List<PacketData<RTPpacket>> outOfOrderPackets;

    public PlayoutBuffer(long initialDelayMillis, long consumePeriodMillis, long rebufferingDelayMillis) {
        this.buffer = new ArrayList<PacketData<RTPpacket>>();
        this.initialDelayMillis = initialDelayMillis;
        this.consumePeriodMillis = consumePeriodMillis;
        this.rebufferingDelayMillis = rebufferingDelayMillis;
        this.sequenceNumber = -1;
        this.numTimesRebuffering = 0;
        this.firstPacketTimeMillis = 0;
        this.startTime = 0;
        this.consumedPackets = new ArrayList<PacketData<RTPpacket>>();
        this.latePackets = new ArrayList<PacketData<RTPpacket>>();
        this.outOfOrderPackets = new ArrayList<PacketData<RTPpacket>>();

        print(String.format("initial delay: %d", initialDelayMillis));
        print(String.format("consume period: %d", consumePeriodMillis));
        print(String.format("rebuffering delay: %d", rebufferingDelayMillis));
    }


    private void print(String msg) {
        AppLogger.log(label, msg);
        Util.printInThread(label, msg);
    }

    private String getCsvDataFilename() {
        String filename = String.format("../packetsTimesData/%s.csv", Util.getLastFileNumFromDir("../packetsTimesData"));
        Util.writeArrayListToCSV(Arrays.asList("packetSequenceNumber", "scheduledPlayoutTimeMillis", "arrivalTimeMillis"), filename);
        return filename;
    }


    public void consumePacket() throws java.util.NoSuchElementException{

        PacketData<RTPpacket> packet = buffer.removeFirst();
        // como todos os pacotes esperam o atraso inicial antes de serem consumidos, temos que tirar esse tempo
        long packetActualTime = System.currentTimeMillis() - initialDelayMillis;
        // firstPacketTimeMillis = sequenceNumber == -1 ? packetActualTime : firstPacketTimeMillis;
        sequenceNumber++;

        // long scheduledPlayoutTimeMillis = firstPacketTimeMillis + (sequenceNumber * consumePeriodMillis) + (numTimesRebuffering * rebufferingDelayMillis);
        long scheduledPlayoutTimeMillis = startTime + initialDelayMillis + (sequenceNumber * consumePeriodMillis) + (numTimesRebuffering * rebufferingDelayMillis);
        
        print(String.format("start: %d", startTime));
        print(String.format("consume time: %d", sequenceNumber * consumePeriodMillis));
        print(String.format("rebuffering delay: %d", numTimesRebuffering * rebufferingDelayMillis));
        print(String.format("scheduled date: %s / timestamp = %d\n" , new Date(scheduledPlayoutTimeMillis).toString(), scheduledPlayoutTimeMillis));
        print(String.format("comsumption date: %s / timestamp = %d\n" , new Date(packetActualTime).toString(), packetActualTime));

        // String.valueOf(packet.getArrivalTimeMillis()

        List<String> times = Arrays.asList(String.valueOf(sequenceNumber), String.valueOf(scheduledPlayoutTimeMillis), String.valueOf(packetActualTime));
        Util.writeArrayListToCSV(times, timesFilename);
        
        if (packet.getSequenceNumber() < sequenceNumber) {
            print(String.format("pacote %d chegou fora de ordem / sequence number = %d", packet.getSequenceNumber(), sequenceNumber));
            outOfOrderPackets.add(packet);
            return;
        }

        if (packetActualTime > scheduledPlayoutTimeMillis) {
            print(String.format("pacote %d chegou atrasado", packet.getSequenceNumber()));
            latePackets.add(packet);
            return;
        }
        
        consumedPackets.add(packet);
    }


    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public void rebuffer() {
        numTimesRebuffering++;
    }

    public void add(PacketData<RTPpacket> packet) {
        buffer.add(packet);
    }

    public int getNumTimesRebuffering() {
        return numTimesRebuffering;
    }

    public long getTimeSpentRebuffering() {
        return numTimesRebuffering * rebufferingDelayMillis;
    }

    public long getInitialDelayMillis() {
        return initialDelayMillis;
    }

    public long getRebufferingDelayMillis() {
        return rebufferingDelayMillis;
    }

    public long getConsumePeriodMillis() {
        return consumePeriodMillis;
    }

    public int getSize() {
        return buffer.size();
    }

    public List<PacketData<RTPpacket>> getLatePackets() {
        return latePackets;
    }

    public List<PacketData<RTPpacket>> getOutOfOrderPackets() {
        return outOfOrderPackets;
    }

    public List<PacketData<RTPpacket>> getConsumedPackets() {
        return consumedPackets;
    }

    public void setStartTime(long start) {
        this.startTime = start;
    }

}