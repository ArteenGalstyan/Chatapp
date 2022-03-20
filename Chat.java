
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Chat {
    private List<Peer> connectedPeers;
    private Integer listenPort;
    private String myPort;
    private String myIP;
    private ServerSocket listenSocket;
    private BufferedReader input;
    private Map<Peer, DataOutputStream> peerMap;

    public Chat(String portNumber) throws IOException {

        myIP = Inet4Address.getLocalHost().getHostAddress();
        myPort = portNumber;

        // list of all clients (peers) connected to this host
        connectedPeers = new ArrayList<Peer>();

        input = new BufferedReader(new InputStreamReader(System.in));

        // map a peer to an output stream
        peerMap = new HashMap<Peer, DataOutputStream>();
    }

    private void startServer() throws IOException {
        new Thread(() -> {
            while (true) {
                try {
                    Socket connectionSocket = listenSocket.accept();
                    new Thread(new PeerHandler(connectionSocket)).start();
                } catch (IOException e) {
                }
            }
        }).start();
    }

    private class PeerHandler implements Runnable {

        private Socket peerSocket;

        public PeerHandler(Socket socket) {
            this.peerSocket = socket;
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));

                // read all messages sent to the host
                while (true) {
                    String jsonStr = input.readLine();

                    // when the other end of the input stream is closed,
                    // will received null; when null, close thread
                    if (jsonStr == null) {
                        return;
                    }

                    String ip = JSONHelper.parse(jsonStr, "ip");
                    int port = Integer.valueOf(JSONHelper.parse(jsonStr, "port"));

                    // each JSON string received/written can be of 3 types
                    Type type = Type.valueOf(JSONHelper.parse(jsonStr, "type"));
                    switch (type) {
                        case CONNECT:
                            displayConnectSuccess(jsonStr);
                            break;
                        case MESSAGE:
                            String message = JSONHelper.parse(jsonStr, "message");
                            displayMessage(ip, port, message);
                            break;
                        case TERMINATE:
                            displayTerminateMessage(ip, port);
                            terminateConnection(findPeer(ip, port));
                            removePeer(findPeer(ip, port));
                            input.close();
                            return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Message: Connection drop");
            }
        }
    }

    private void displayHelp() {
        for (int i = 0; i < 100; i++) {
            System.out.print("-"); // header
        }
        System.out.println("\nchat <port number>\t Run chat listening on <port number>");
        System.out.println("\nhelp\tDisplay information about the available user interface commands.");
        System.out.println("\nmyip\t Display your IP address.");
        System.out
                .println("\nmyport\t Display the port # on which this process is listening for incoming connections.");
        System.out.println("\nconnect\t <destination> <port no> This command establishes a new TCP connection to the "
                + "specified <destination> at the \nspecified <port no>. <destination> is the IP address of the destination.");
        System.out.println("\nlist Display a list of all the peers you are connected to. More specifically, it displays"
                + "the index id #, IP address, and port # of each peer.");
        System.out.println(
                "\nterminate <connection id> Terminate the connection to a peer by their id given in the list command.");
        System.out.println(
                "\nsend\t <connection id.> <message> Send a message to a peer by their id given in the list command."
                        + "The message to be sent can be \nup-to 100 characters long, including blank spaces.");
        System.out.println("\nexit\t Close all connections and terminate this process.");
        for (int i = 0; i < 100; i++) {
            System.out.print("-"); // footer
        }
        System.out.println("\n");
    }

    private void displayList() {
        if (connectedPeers.isEmpty())
            System.out.println("No peers connected.");
        else {
            System.out.println("id:   IP Address     Port No.");
            for (int i = 0; i < connectedPeers.size(); i++) {
                Peer peer = connectedPeers.get(i);
                System.out.println((i + 1) + "    " + peer.getHost() + "     " + peer.getPort());
            }
            System.out.println("Total Peers: " + connectedPeers.size());
        }
    }

    private void sendMessage(Peer peer, String message) {
        try {
            peerMap.get(peer).writeBytes(message + "\r\n");

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void connect(String ip, int port) throws IOException {
        Socket peerSocket = null;

        // try to connect but will stop after MAX_ATTEMPTS
        do {
            try {
                peerSocket = new Socket(ip, port);
            } catch (IOException e) {
                System.out.println("*** connection failed ***");
            }
        } while (peerSocket == null);

        System.out.println("connected to " + ip + " " + port);
        Peer peer = new Peer(ip, port);
        connectedPeers.add(peer);

        // map this peer to an output stream
        peerMap.put(peer, new DataOutputStream(peerSocket.getOutputStream()));

        // tell the peer your host address and port number
        // tell the peer to connect to you
        sendMessage(peer, generateConnectJson());
    }

    private void breakPeerConnections() throws IOException {

        // terminate each peer connection; notify them
        for (Peer peer : connectedPeers) {
            sendMessage(peer, generateTerminateJson());
            terminateConnection(peer);
        }

        // close each output stream
        for (Entry<Peer, DataOutputStream> e : peerMap.entrySet()) {
            e.getValue().close();
        }

        listenSocket.close();
        System.out.println("Chat client closed, good bye.");
    }

    private boolean isValidPeer(int id) {
        return id >= 0 && id < connectedPeers.size();
    }

    private Peer findPeer(String ip, int port) {
        for (Peer p : connectedPeers)
            if (p.getHost().equals(ip) && p.getPort() == port)
                return p;
        return null;
    }

    private void removePeer(Peer peer) {
        connectedPeers.remove(peer);
        peerMap.remove(peer);
    }

    private void terminateConnection(Peer peer) {
        try {
            peer.getSocket().close();
            peerMap.get(peer).close();
        } catch (IOException e) {

        }
    }

    private void displayConnectSuccess(String jsonStr) throws IOException {
        String ip = JSONHelper.parse(jsonStr, "ip");
        int port = Integer.valueOf(JSONHelper.parse(jsonStr, "port"));
        System.out.println("\nPeer [ip: " + ip + ", port: " + port + "] connects to you");
        System.out.print("-> ");
        // save peer's info, used for a lot of other stuff
        Peer peer = new Peer(ip, port);
        connectedPeers.add(peer);
        peerMap.put(peer, new DataOutputStream(peer.getSocket().getOutputStream()));
    }

    private void displayMessage(String ip, int port, String message) {
        System.out.println("\nMessage received from IP: " + ip);
        System.out.println("Sender's Port: " + port);
        System.out.println("Message: " + message);

        System.out.print("-> ");
    }

    private void displayTerminateMessage(String ip, int port) {
        System.out.println();
        System.out.println("Peer [ip: " + ip + " port: " + port + "] has terminated the connection");
        System.out.print("-> ");
    }

    private void processConnect(String userInput) throws IOException {
        String[] args = userInput.split(" ");
        String ip;
        int port;

        ip = args[1];
        port = Integer.valueOf(args[2]);
        connect(ip, port);
    }

    private void processSend(String userInput) {
        String[] args = userInput.split(" ");
        if (args.length >= 3) {
            try {
                int id = Integer.valueOf(args[1]) - 1;
                if (isValidPeer(id)) {
                    String msg = "";
                    for (int i = 2; i < args.length; i++)
                        msg += args[i] + " ";
                    sendMessage(connectedPeers.get(id), generateMessageJson(msg));
                } else {
                    System.out.println("Error: Please select a valid peer id from the list command.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Second argument should be a integer.");
            }
        } else {
            System.out.println("Error: Invalid format for 'send' command. See 'help' for details.");
        }
    }

    private void processTerminate(String userInput) {
        String[] args = userInput.split(" ");
        if (args.length == 2) {
            try {
                int id = Integer.valueOf(args[1]) - 1;
                if (isValidPeer(id)) {
                    // notify peer that connection will be drop
                    Peer peer = connectedPeers.get(id);
                    sendMessage(peer, generateTerminateJson());
                    System.out.println("You dropped peer [ip: " + peer.getHost() + " port: " + peer.getPort() + "]");
                    terminateConnection(peer);
                    removePeer(peer);
                } else {
                    System.out.println("Error: Please select a valid peer id from the list command.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Second argument should be a integer.");
            }
        } else {
            System.out.println("Error: Invalid format for 'terminate' command. See 'help' for details.");
        }
    }

    private ServerSocket createListenSocket(String portNumber) throws IOException {

            int port = Integer.valueOf(portNumber);
            try {
                return listenSocket = new ServerSocket(port);
            } catch (Exception e) {
                return null;
            }
    }

    private void initChat(String portNumber) throws IOException {
        listenSocket = createListenSocket(portNumber);

        if (listenSocket != null) {
            listenPort = listenSocket.getLocalPort();
            myIP = Inet4Address.getLocalHost().getHostAddress();
            System.out.println("you are listening on port: " + listenPort);
            startServer();
        }
    }

	private String generateConnectJson() {
		return JSONHelper.makeJson(Type.CONNECT, myIP, listenPort).toJSONString();
	}

	private String generateMessageJson(String message) {
		return JSONHelper.makeJson(Type.MESSAGE, myIP, listenPort, message).toJSONString();
	}

	private String generateTerminateJson() {
		return JSONHelper.makeJson(Type.TERMINATE, myIP, listenPort).toJSONString();
	}
        
    
    public void acceptInputs() throws IOException {
        System.out.println("Welcome to Chat");
        initChat(myPort);
        while (true) {
            System.out.print("-> ");
            String choice = input.readLine();
            String option = choice.split(" ")[0].toLowerCase();

            switch (option) {
                case "help":
                    displayHelp();
                    break;
                case "myip":
                    System.out.println("My IP Address: " + myIP);
                    break;
                case "myport":
                    if (listenSocket == null)
                        System.out.println("Error: you are not connected");
                    else
                        System.out.println("Listening on port: " + listenPort);
                    break;
                case "connect":
                    if (listenSocket == null)
                        System.out.println("Error: you are not connected");
                    else
                        processConnect(choice);
                    break;
                case "list":
                    if (listenSocket == null)
                        System.out.println("Error: you are not connected");
                    else
                        displayList();
                    break;
                case "send":
                    if (listenSocket == null)
                        System.out.println("Error: you are not connected");
                    else
                        processSend(choice);
                    break;
                case "terminate":
                    if (listenSocket == null)
                        System.out.println("Error: you are not connected");
                    else
                        processTerminate(choice);
                    break;
                case "exit":
                    breakPeerConnections();
                    System.exit(0);
                    break;
                default:
                    System.out.println("not a recognized command");
            }
        }
    }

    public static void main(String[] args) {
        try {
            Chat chat = new Chat(args[0]);
            chat.acceptInputs();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
