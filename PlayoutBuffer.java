import java.awt.Image;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class PlayoutBuffer {

    private List<PacketData<RTPpacket>> buffer;
    private long initialDelayMillis;
    private long consumePeriodMillis;
    private long rebufferingDelayMillis;
    private int sequenceNumber;
    private int numTimesRebuffering;
    private long firstPacketTimeMillis;

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
        this.consumedPackets = new ArrayList<PacketData<RTPpacket>>();
        this.latePackets = new ArrayList<PacketData<RTPpacket>>();
        this.outOfOrderPackets = new ArrayList<PacketData<RTPpacket>>();
    }


    private void print(String msg) {
        System.out.printf("[PlayoutBuffer]: %s\n", msg);
    }


    public void consumePacket(JLabel iconLabel) throws java.util.NoSuchElementException{

        
        PacketData<RTPpacket> packet = buffer.removeFirst();
        firstPacketTimeMillis = sequenceNumber == -1 ? System.currentTimeMillis() : firstPacketTimeMillis;
        sequenceNumber++;

        long scheduledPlayoutTimeMillis = firstPacketTimeMillis + (sequenceNumber-1) * consumePeriodMillis + numTimesRebuffering * rebufferingDelayMillis;
        
        System.out.printf("scheduled date: %s / timestamp = %d\n" , new Date(scheduledPlayoutTimeMillis).toString(), scheduledPlayoutTimeMillis);
        System.out.printf("packet date: %s / timestamp = %d\n", new Date(packet.getArrivalTimeMillis()), packet.getArrivalTimeMillis());
        
        if (packet.getSequenceNumber() < sequenceNumber) {
            print(String.format("pacote %d chegou fora de ordem / sequence number = %d", packet.getSequenceNumber(), sequenceNumber));
            outOfOrderPackets.add(packet);
            return;
        }

        if (packet.getArrivalTimeMillis() > scheduledPlayoutTimeMillis) {
            print(String.format("pacote %d chegou atrasado", packet.getSequenceNumber()));
            latePackets.add(packet);
            return;
        }
        
        consumedPackets.add(packet);

        RTPpacket rtp_packet = packet.getPacket();

        // get the payload bitstream from the RTPpacket object
        int payload_length = rtp_packet.getpayload_length();
        byte[] payload = new byte[payload_length];
        rtp_packet.getpayload(payload);

        // get an Image object from the payload bitstream
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(payload, 0, payload_length);

        // display the image as an ImageIcon object
        ImageIcon icon = new ImageIcon(image);
        iconLabel.setIcon(icon);
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

}