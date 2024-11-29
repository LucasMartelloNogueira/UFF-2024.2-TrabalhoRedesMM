public class PacketData <T> {
    
    private T packet;
    private long arrivalTimeMillis;
    private int sequenceNumber;

    public PacketData(T packet, long arrivalTimeMillis, int sequenceNumber){
        this.packet = packet;
        this.arrivalTimeMillis = arrivalTimeMillis;
        this.sequenceNumber = sequenceNumber;
    }

    public T getPacket() {
        return packet;
    }

    public long getArrivalTimeMillis() {
        return arrivalTimeMillis;
    }

    public void setArrivalTimeMilllis(long time) {
        arrivalTimeMillis = time;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

}
