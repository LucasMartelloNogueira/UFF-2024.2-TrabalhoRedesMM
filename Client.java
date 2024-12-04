/* ------------------
   Client
   usage: java Client [Server hostname] [Server RTSP listening port] [Video file requested]
   ---------------------- */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Function;


public class Client {



  // RTP variables:
  // ----------------
  DatagramPacket rcvdp; // UDP packet received from the server
  DatagramSocket RTPsocket; // socket to be used to send and receive UDP packets
  static int RTP_RCV_PORT = 25000; // port where the client will receive the RTP packets

  // Timer timer; // timer used to receive data from the UDP socket
  // java.util.Timer timer;
  WaitingTimer timer;
  byte[] buf; // buffer used to store data received from the server

  // RTSP variables
  // ----------------
  // rtsp states
  final static int INIT = 0;
  final static int READY = 1;
  final static int PLAYING = 2;
  static int state; // RTSP state == INIT or READY or PLAYING
  Socket RTSPsocket; // socket used to send/receive RTSP messages
  // input and output stream filters
  static BufferedReader RTSPBufferedReader;
  static BufferedWriter RTSPBufferedWriter;
  static String VideoFileName; // video file to request to the server
  int RTSPSeqNb = 0; // Sequence number of RTSP messages within the session
  int RTSPid = 0; // ID of the RTSP session (given by the RTSP Server)

  final static String CRLF = "\r\n";

  // Video constants:
  // ------------------
  static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video

  // parameters for playout buffer
  int minDelayNetworkMillis;
  int maxNetworkDelayMillis;
  long initalPlayoutBufferDelay;
  long consumeTimeMilis = 20;
  long rebufferingDelayMilis;
  int discartProbabilityPercent;

  PlayoutBuffer playoutBuffer;
  List<PacketData<RTPpacket>> channelBuffer;

  BufferProducer producer;
  BufferConsumer consumer;


  public Client(int minDelayNetworkMillis, int maxNetworkDelayMillis, long initalPlayoutBufferDelay, long rebufferingDelayMilis, int discartProbabilityPercent) {
    this.minDelayNetworkMillis = minDelayNetworkMillis;
    this.maxNetworkDelayMillis = maxNetworkDelayMillis;
    this.initalPlayoutBufferDelay = initalPlayoutBufferDelay;
    this.rebufferingDelayMilis = rebufferingDelayMilis;
    this.discartProbabilityPercent = discartProbabilityPercent;
    
    this.playoutBuffer = new PlayoutBuffer(initalPlayoutBufferDelay, consumeTimeMilis, rebufferingDelayMilis);
    this.channelBuffer = new ArrayList<>();
    
    this.producer = new BufferProducer(minDelayNetworkMillis, maxNetworkDelayMillis, playoutBuffer, channelBuffer, discartProbabilityPercent, (Void unused) -> {
      consumer.setIsRunning(false);
      return null;
    });
    
    long start = System.currentTimeMillis();
    this.consumer = new BufferConsumer(playoutBuffer, channelBuffer, start, (Long startTime) -> {
      teardown(startTime);
      return null;
    });

    buf = new byte[15000];
  }

  public static void main(String argv[]) throws Exception {

    // parameters for simulation
    int minDelayNetworkMillis = Integer.parseInt(argv[3]);
    int maxNetworkDelayMillis = Integer.parseInt(argv[4]);
    long initalPlayoutBufferDelay = Integer.parseInt(argv[5]);
    long rebufferingDelayMilis = Integer.parseInt(argv[6]);
    int discartProbabilityPercent = Integer.parseInt(argv[7]);

    // Create a Client object
    Client theClient = new Client(minDelayNetworkMillis, maxNetworkDelayMillis, 
                        initalPlayoutBufferDelay, rebufferingDelayMilis, discartProbabilityPercent);

    // get server RTSP port and IP address from the command line
    // ------------------
    int RTSP_server_port = Integer.parseInt(argv[1]);
    String ServerHost = argv[0];
    InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);

    // get video filename to request:
    VideoFileName = argv[2];

    // Establish a TCP connection with the server to exchange RTSP messages
    // ------------------
    theClient.RTSPsocket = new Socket(ServerIPAddr, RTSP_server_port);

    // Set input and output stream filters:
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPsocket.getInputStream()));
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsocket.getOutputStream()));

    // init RTSP state:
    state = INIT;

    theClient.setup();
    theClient.play();
  }

  class GetVideoTask extends TimerTask {

    private Function<Void, Void> callback;

    public GetVideoTask(Function<Void, Void> callback) {
      this.callback = callback;
    }

    @Override
    public void run() {
      rcvdp = new DatagramPacket(buf, buf.length);

      try {
        // receive the DP from the socket:
        RTPsocket.receive(rcvdp);
        
        // create an RTPpacket object from the DP
        RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
        // System.out.printf("seq num: %d\n", rtp_packet.SequenceNumber);

        // 65535: é o numero que vira quando server manda sequence number == -1
        if (rtp_packet.SequenceNumber == 65535) {
          callback.apply(null);
        }

        producer.addPacket(rtp_packet, rtp_packet.SequenceNumber);
        // System.out.printf("[CLIENT] - recebeu pacote %d\n", rtp_packet.SequenceNumber);
        
      } catch (InterruptedIOException iioe) {
        // System.out.println("Nothing to read");
      } catch (IOException ioe) {
        System.out.println("Exception caught: " + ioe);
      } 
    }
    
  }

  private void setup() {
    if (state == INIT) {
      // Init non-blocking RTPsocket that will be used to receive data
      try {

        RTPsocket = new DatagramSocket(RTP_RCV_PORT);
        RTPsocket.setSoTimeout(5);

      } catch (SocketException se) {
        System.out.println("Socket exception: " + se);
        System.exit(0);
      }

      // init RTSP sequence number
      RTSPSeqNb = 1;

      // Send SETUP message to the server
      send_RTSP_request("SETUP");

      int reply_code = parse_server_response();
      // Wait for the response
      if (reply_code!= 200)
        System.out.println("Invalid Server Response");
      else {
        // change RTSP state and print new state
        state = READY;
        System.out.println("S: RTSP/1.0 " + reply_code + " OK");
        // System.out.println(String.format("S: RTSP/1.0 %d OK", reply_code));  
        System.out.println("S: CSeq: " + RTSPSeqNb);
        System.out.println("S: Session: " + RTSPid);
        // System.out.println("New RTSP state: ....");
      }
    }
  }

  private void play() {
    if (state == READY) {
      // increase RTSP sequence number

      RTSPSeqNb += 1;

      // Send PLAY message to the server
      send_RTSP_request("PLAY");

      // Wait for the response
      if (parse_server_response() != 200)
        System.out.println("Invalid Server Response");
      else {
        // change RTSP state and print out new state
        state = PLAYING;
        System.out.println("New RTSP state: PLAYING");

        playoutBuffer.setStartTime(System.currentTimeMillis());

        // start the timer
        timer = new WaitingTimer(0, consumeTimeMilis);
        timer.setTask(new GetVideoTask((Void unused) -> {
          timer.stop();
          producer.setIsRunning(false);
          return null;
        }));
        timer.start();
        
        producer.start();
        consumer.start();
      }
    }
  }

  private void teardown(long start) {
      

      RTSPSeqNb += 1;

      // Send TEARDOWN message to the server
      send_RTSP_request("TEARDOWN");

      // Wait for the response
      if (parse_server_response() != 200)
        System.out.println("Invalid Server Response");
      else {
        // change RTSP state and print out new state
        // ........
        // System.out.println("New RTSP state: ...");

        state = INIT;
        System.out.println("New RTSP state: INIT");

        long end = System.currentTimeMillis();
        print(String.format("total time: %d\n", end-start));
        print(String.format("time rebuferring: %d\n", playoutBuffer.getTimeSpentRebuffering()));
        print(String.format("total times rebuffering: %d\n", playoutBuffer.getNumTimesRebuffering()));
        print(String.format("num consumed packets: %d\n", playoutBuffer.getConsumedPackets().size()));
        print(String.format("num late packets: %d\n", playoutBuffer.getLatePackets().size()));
        print(String.format("num out of order packets: %d\n", playoutBuffer.getOutOfOrderPackets().size()));
        print(String.format("num packets lost to network: %d\n", producer.getLostPackets().size()));

        Util.writeArrayListToCSV(getDataAsList(end-start), "../data.csv");

        System.exit(0);
      }
  }

  private List<String> getDataAsList(long timeMillis) {
    return Arrays.asList(
        String.valueOf(minDelayNetworkMillis),
        String.valueOf(maxNetworkDelayMillis),
        String.valueOf(initalPlayoutBufferDelay),
        String.valueOf(consumeTimeMilis),
        String.valueOf(rebufferingDelayMilis),
        String.valueOf(discartProbabilityPercent),
        String.valueOf(timeMillis),
        String.valueOf(playoutBuffer.getTimeSpentRebuffering()),
        String.valueOf(playoutBuffer.getNumTimesRebuffering()),
        String.valueOf(playoutBuffer.getConsumedPackets().size()),
        String.valueOf(playoutBuffer.getLatePackets().size()),
        String.valueOf(playoutBuffer.getOutOfOrderPackets().size()),
        String.valueOf(producer.getLostPackets().size())
    );
}


  private int parse_server_response() {
    int reply_code = 0;

    try {
      // parse status line and extract the reply_code:
      String StatusLine = RTSPBufferedReader.readLine();
      // System.out.println("RTSP Client - Received from Server:");
      System.out.println(StatusLine);

      StringTokenizer tokens = new StringTokenizer(StatusLine);
      tokens.nextToken(); // skip over the RTSP version
      reply_code = Integer.parseInt(tokens.nextToken());

      // if reply code is OK get and print the 2 other lines
      if (reply_code == 200) {
        String SeqNumLine = RTSPBufferedReader.readLine();
        System.out.println(SeqNumLine);

        String SessionLine = RTSPBufferedReader.readLine();
        System.out.println(SessionLine);

        // if state == INIT gets the Session Id from the SessionLine
        tokens = new StringTokenizer(SessionLine);
        tokens.nextToken(); // skip over the Session:
        RTSPid = Integer.parseInt(tokens.nextToken());
      }
    } catch (Exception ex) {
      System.out.println("Exception caught: " + ex);
      System.exit(0);
    }

    return (reply_code);
  }

  // ------------------------------------
  // Send RTSP Request
  // ------------------------------------

  private void send_RTSP_request(String request_type) {
    try {
      // Use the RTSPBufferedWriter to write to the RTSP socket

      // write the request line:
      // RTSPBufferedWriter.write(...);
      RTSPBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + CRLF);

      // write the CSeq line:
      RTSPBufferedWriter.write("CSEQ: " + String.valueOf(RTSPSeqNb) + CRLF);
      // ......

      // check if request_type is equal to "SETUP" and in this case write the
      // Transport: line advertising to the server the port used to receive the RTP

      // packets RTP_RCV_PORT
      // if ....

      if (request_type.equals("SETUP")) {
        RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + String.valueOf(RTP_RCV_PORT) + CRLF);
      }else { // otherwise, write the Session line from the RTSPid field
        RTSPBufferedWriter.write("Session: " + String.valueOf(RTSPid) + CRLF);
      }

      RTSPBufferedWriter.flush();
    } catch (Exception ex) {
      System.out.println("Exception caught: " + ex);
      System.exit(0);
    }
  }

  private void print(String msg){
    Util.printInThread("CLIENT", msg);
    AppLogger.log("CLIENT", msg);
  }

}// end of Class Client
