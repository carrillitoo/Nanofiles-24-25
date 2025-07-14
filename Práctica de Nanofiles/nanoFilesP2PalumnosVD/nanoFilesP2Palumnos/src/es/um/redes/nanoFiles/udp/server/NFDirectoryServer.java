package es.um.redes.nanoFiles.udp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFDirectoryServer {
    public static final int DIRECTORY_PORT = 6868;
    private DatagramSocket socket = null;
    private double messageDiscardProbability;

    // Estructuras para el almacenamiento de datos
    private final Map<InetSocketAddress, List<FileInfo>> filesByServer;
    private final Map<String, Set<InetSocketAddress>> fileIndexByHash;
    private final Map<String, Set<InetSocketAddress>> fileIndexByName;

    public NFDirectoryServer(double corruptionProbability) throws SocketException {
        this.messageDiscardProbability = corruptionProbability;
        this.filesByServer = new ConcurrentHashMap<>();
        this.fileIndexByHash = new ConcurrentHashMap<>();
        this.fileIndexByName = new ConcurrentHashMap<>();

        try {
            this.socket = new DatagramSocket(DIRECTORY_PORT);
            System.out.println("Server started on port " + DIRECTORY_PORT);
        } catch (SocketException e) {
            System.err.println("Error starting socket: " + e.getMessage());
            throw e;
        }

        if (NanoFiles.testModeUDP && socket == null) {
            System.err.println("[testMode] NFDirectoryServer: code not fully functional!");
            System.exit(-1);
        }
    }

    public synchronized void registerFiles(InetSocketAddress serverAddr, FileInfo[] files) {
        unregisterServer(serverAddr);
        
        List<FileInfo> fileList = Arrays.asList(files);
        filesByServer.put(serverAddr, fileList);
        
        for (FileInfo file : files) {
            fileIndexByHash.computeIfAbsent(file.fileHash, k -> ConcurrentHashMap.newKeySet()).add(serverAddr);
            fileIndexByName.computeIfAbsent(file.fileName, k -> ConcurrentHashMap.newKeySet()).add(serverAddr);
        }
        System.out.println("Registered " + files.length + " files from " + serverAddr);
    }

    public synchronized void unregisterServer(InetSocketAddress serverAddr) {
        List<FileInfo> oldFiles = filesByServer.remove(serverAddr);
        if (oldFiles != null) {
            oldFiles.forEach(file -> {
                fileIndexByHash.getOrDefault(file.fileHash, Collections.emptySet()).remove(serverAddr);
                fileIndexByName.getOrDefault(file.fileName, Collections.emptySet()).remove(serverAddr);
            });
            System.out.println("Server unregistered: " + serverAddr);
        }
    }

    public List<FileInfo> getAllFiles() {
        return filesByServer.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public Set<InetSocketAddress> getServersForFile(String filename) {
        // 1. Primero busca la coincidencia exacta
        Set<InetSocketAddress> servers = fileIndexByName.getOrDefault(filename, Collections.emptySet());
        
        if (!servers.isEmpty()) {
            return servers;
        }
        
        // 2. Si no hay coincidencia exacta, busca por subcadena
        Set<InetSocketAddress> result = ConcurrentHashMap.newKeySet();
        
        fileIndexByName.forEach((key, value) -> {
            if (key.contains(filename)) {  // Búsqueda por subcadena
                result.addAll(value);
            }
        });
        
        return result.isEmpty() ? Collections.emptySet() : result;
    }

    public DatagramPacket receiveDatagram() throws IOException {
        byte[] buffer = new byte[DirMessage.PACKET_MAX_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while (true) {
            socket.receive(packet);
            if (Math.random() >= messageDiscardProbability) {
                System.out.println("Packet received from " + packet.getSocketAddress());
                return packet;
            }
            System.err.println("Packet discarded (simulation)");
        }
    }

    public void run() throws IOException {
        System.out.println("Directory server started. Waiting for connections...");
        while (true) {
            DatagramPacket packet = receiveDatagram();
            sendResponse(packet);
        }
    }

    private void sendResponse(DatagramPacket pkt) throws IOException {
        String received = new String(pkt.getData(), 0, pkt.getLength());
        DirMessage request = DirMessage.fromString(received);
        InetSocketAddress clientAddr = (InetSocketAddress) pkt.getSocketAddress();

        if (request == null) {
            System.err.println("Invalid message received");
            return;
        }

        DirMessage response = processRequest(request, clientAddr);
        if (response != null) {
            String responseStr = response.toString();
            byte[] responseData = responseStr.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, 
                responseData.length, 
                clientAddr
            );
            socket.send(responsePacket);
        }
    }

    private DirMessage processRequest(DirMessage request, InetSocketAddress clientAddr) {
        switch (request.getOperation()) {
            case DirMessageOps.OPERATION_PING:
                return handlePing(request);
                
            case DirMessageOps.OPERATION_SERVE:
                return handleServe(request, clientAddr);
                
            case DirMessageOps.OPERATION_FILELIST:
                return handleFileList();
                
            case DirMessageOps.OPERATION_GET_SERVERS:
                return handleGetServers(request);
                
            case DirMessageOps.OPERATION_UNREGISTER:
                return handleUnregister(clientAddr);
                
            default:
                System.err.println("Unsupported operation: " + request.getOperation());
                return new DirMessage(DirMessageOps.OPERATION_INVALID);
        }
    }

    private DirMessage handlePing(DirMessage request) {
        System.out.println("Ping received");
        if (request.getProtocolId().equals(NanoFiles.PROTOCOL_ID)) {
            return new DirMessage(DirMessageOps.OPERATION_PING_OK);
        }
        return new DirMessage(DirMessageOps.OPERATION_PING_BAD);
    }

    private DirMessage handleServe(DirMessage request, InetSocketAddress clientAddr) {
        int serverPort = request.getServerPort();
        InetSocketAddress serverAddr = new InetSocketAddress(
            clientAddr.getAddress(), 
            serverPort
        );
        
        registerFiles(serverAddr, request.getFilesInfo());
        return new DirMessage(DirMessageOps.OPERATION_SERVE_OK);
    }

    private DirMessage handleFileList() {
        DirMessage response = new DirMessage(DirMessageOps.OPERATION_FILELIST_OK);
        getAllFiles().forEach(file -> {
            response.setFileInfo(file);
        });
        return response;
    }

    private DirMessage handleGetServers(DirMessage request) {
        Set<InetSocketAddress> servers = getServersForFile(request.getTarget());
        DirMessage response = new DirMessage(DirMessageOps.OPERATION_SERVERS_LIST);
        servers.forEach(response::setHosts);
        return response;
    }

    private DirMessage handleUnregister(InetSocketAddress clientAddr) {
        unregisterServer(clientAddr);
        return new DirMessage(DirMessageOps.OPERATION_UNREGISTER_OK);
    }
    
    private void sendResponseTestMode(DatagramPacket pkt) throws IOException {
	    /*
	     * TODO: (Boletín SocketsUDP) Construir un String partir de los datos recibidos
	     * en el datagrama pkt. A continuación, imprimir por pantalla dicha cadena a
	     * modo de depuración.
	     */
	    String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
	    System.out.println("Data received: " + messageFromClient);

	    /*
	     * TODO: (Boletín SocketsUDP) Después, usar la cadena para comprobar que su
	     * valor es "ping"; en ese caso, enviar como respuesta un datagrama con la
	     * cadena "pingok". Si el mensaje recibido no es "ping", se informa del error y
	     * se envía "invalid" como respuesta.
	     */
	    String responseMessage;
	    if (messageFromClient.equals("ping")) {
	        responseMessage = "pingok"; // Respuesta para el mensaje "ping"
	        System.out.println("Received 'ping'. Responding with 'pingok'.");
	    } else if (messageFromClient.startsWith("ping&")) {
	        if (messageFromClient.equals("ping&" + NanoFiles.PROTOCOL_ID)) {
	            responseMessage = "welcome"; // Respuesta para protocolo compatible
	            System.out.println("Received valid protocol ID. Responding with 'welcome'.");
	        } else {
	            responseMessage = "denied"; // Respuesta para protocolo no compatible
	            System.out.println("Received invalid protocol ID. Responding with 'denied'.");
	        }
	    } else {
	        responseMessage = "invalid"; // Respuesta para mensajes no reconocidos
	        System.err.println("Received invalid message. Responding with 'invalid'.");
	    }
	    // Enviamos la respuesta al cliente
	    byte[] responseBytes = responseMessage.getBytes(); // Convertimos la respuesta a bytes
	    DatagramPacket responsePacket = new DatagramPacket(
	        responseBytes,
	        responseBytes.length,
	        pkt.getAddress(),
	        pkt.getPort()
	    );
	    socket.send(responsePacket); // Enviar el datagrama de respuesta
	    System.out.println("Response sent: " + responseMessage);
		/*
		 * TODO: (Boletín Estructura-NanoFiles) Ampliar el código para que, en el caso
		 * de que la cadena recibida no sea exactamente "ping", comprobar si comienza
		 * por "ping&" (es del tipo "ping&PROTOCOL_ID", donde PROTOCOL_ID será el
		 * identificador del protocolo diseñado por el grupo de prácticas (ver
		 * NanoFiles.PROTOCOL_ID). Se debe extraer el "protocol_id" de la cadena
		 * recibida y comprobar que su valor coincide con el de NanoFiles.PROTOCOL_ID,
		 * en cuyo caso se responderá con "welcome" (en otro caso, "denied").
		 */
	    
		String messageFromClient1 = new String(pkt.getData(), 0, pkt.getLength());
		System.out.println("Data received: " + messageFromClient1);

	}
    
    public void runTest() throws IOException {

		System.out.println("[testMode] Directory starting...");

		System.out.println("[testMode] Attempting to receive 'ping' message...");
		DatagramPacket rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);

		System.out.println("[testMode] Attempting to receive 'ping&PROTOCOL_ID' message...");
		rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);
	}
}