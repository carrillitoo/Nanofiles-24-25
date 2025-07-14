package es.um.redes.nanoFiles.tcp.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import es.um.redes.nanoFiles.tcp.message.*;
import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.util.FileDigest;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServer implements Runnable {
    private ServerSocket serverSocket = null;
    private static final int CHUNK_SIZE = 4096;
    private List<NFServerThread> clientThreads = Collections.synchronizedList(new ArrayList<>());
    private static boolean alive = false;
    private int ephemeralPort;  // Para almacenaar el puerto efímero asignado

    public NFServer() throws IOException {
        // Crea el ServerSocket con puerto 0 para que el SO asigne uno efímero
    	
    	InetSocketAddress ip = new InetSocketAddress(0);
        serverSocket = new ServerSocket();
        serverSocket.bind(ip);
        ephemeralPort = serverSocket.getLocalPort(); // Guardar el puerto asignado
        System.out.println("Server started on ephemeral port: " + ephemeralPort);
    }

    public void test() {
        if (serverSocket == null || !serverSocket.isBound()) {
            System.err.println("[fileServerTestMode] Failed to run file server");
            return;
        }
        System.out.println("NFServer running on 0.0.0.0:" + ephemeralPort);
        
        try {
            Socket socket = serverSocket.accept();
            serveFilesToClient(socket);
        } catch (IOException e) {
            System.err.println("Error accepting connection: " + e.getMessage());
        }
    }

    public void run() {
        alive = true;
        System.out.println("NFServer running on 0.0.0.0:" + ephemeralPort);
        
        try {
            while (alive) {
                Socket socket = serverSocket.accept();
                System.out.println("Received connection from " + socket.getInetAddress() + 
                                 " on port " + ephemeralPort);
                
                // Atiende a cada cliente en un hilo separado
                new Thread(() -> serveFilesToClient(socket)).start();
            }
        } catch (IOException e) {
            if (alive) {
                System.err.println("Server error: " + e.getMessage());
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server: " + e.getMessage());
            }
        }
    }
    
    public boolean isAlive() {
        return alive;
    }

    public static void serveFilesToClient(Socket socket) {
    	
    	FileInfo fileToReceive = null;
    	long remaining_size = 0;
    	
    	try {
            alive = true;
            System.out.println("Received connection from " + socket.getInetAddress() + ":" + socket.getPort());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            FileInfo reqFileInfo = null;
            RandomAccessFile fileStream = null;

            while (alive) {
                PeerMessage request = PeerMessage.readMessageFromInputStream(dis);
                System.out.println("Received message: " + PeerMessageOps.opcodeToOperation(request.getOpcode()));
                
                PeerMessage response = null;
                
                switch (request.getOpcode()) {
                    case PeerMessageOps.OP_DOWNLOAD_FILE: {
                        FileInfo[] matchingFiles = FileInfo.lookupFilenameSubstring(NanoFiles.db.getFiles(), request.getFilename());
                        if (matchingFiles.length == 1) {
                            reqFileInfo = matchingFiles[0];
                            try {
                                fileStream = new RandomAccessFile(reqFileInfo.filePath, "r");
                                response = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_OK);
                                response.setFilename(reqFileInfo.fileName);
                                response.setFileSize(reqFileInfo.fileSize);
                                response.setFileHash(reqFileInfo.fileHash);
                                
                            } catch (FileNotFoundException e) {
                                response = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FAIL);
                            }
                        } else {
                            response = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FAIL);
                        }
                        break;
                    }
                    
                    case PeerMessageOps.OP_DOWNLOAD_CHUNK: {
                        if (fileStream == null || reqFileInfo == null) {
                            response = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FAIL);
                            break;
                        }
                        
                        try {
                            fileStream.seek(request.getOffset());
                            byte[] chunk = new byte[request.getChunkSize()];
                            
                            int bytesRead = fileStream.read(chunk);
                            //fileStream.read(chunk); creo que esto sobra*****
                            
                            if (bytesRead > 0) {
                                response = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_CHUNK_RESPONSE);
                                response.setOffset(request.getOffset());
                                response.setChunkSize(bytesRead);
                                
                                response.setChunk(chunk, bytesRead);
                                
                                System.out.println("[NFServer] Offset: " + request.getOffset() + ", ChunkSize: " + request.getChunkSize() + ", bytesRead: " + bytesRead);
                                
                            } else {
                                response = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FAIL);
                            }
                        } catch (IOException e) {
                            response = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FAIL);
                        }
                        break;
                    }

                    case PeerMessageOps.OP_DOWNLOAD_COMPLETED: {
                        // El cliente ha terminado con este chunk, podemos salir del bucle limpiamente
                        System.out.println("Client finished downloading chunk");
                        return; 
                    }
                    case PeerMessageOps.OP_UPLOAD_REQUEST: {
                        FileInfo fileFromPeer = request.getMatchFile();
                        boolean exists = false;
                        for (FileInfo f : NanoFiles.db.getFiles()) {
                            if (f.equals(fileFromPeer)) {
                                response = new PeerMessage(PeerMessageOps.OP_UPLOAD_FAIL);
                                exists = true;
                                alive = false;
                                break;
                            }
                        }
                        if (!exists) {
                            fileToReceive = fileFromPeer;
                            remaining_size = fileToReceive.fileSize;
                            response = new PeerMessage(PeerMessageOps.OP_UPLOAD_OK);
                        }
                        break;
                    }

                    case PeerMessageOps.OP_UPLOAD_CHUNK: {
                        if (fileToReceive != null) {
                            RandomAccessFile raf = new RandomAccessFile(NanoFiles.sharedDirname + "/" + fileToReceive.fileName, "rw");
                            raf.seek(request.getOffset());
                            raf.write(request.getData());
                            remaining_size = remaining_size - request.getBytes();
                            raf.close();

                            if (remaining_size == 0) {
                                String computedHash = FileDigest.computeFileChecksumString(NanoFiles.sharedDirname + "/" + fileToReceive.fileName);
                                if (!fileToReceive.fileHash.equals(computedHash)) {
                                    response = new PeerMessage(PeerMessageOps.OP_UPLOAD_FAIL);
                                    alive = false;
                                } else {
                                    response = new PeerMessage(PeerMessageOps.OP_UPLOAD_FINISHED_OK);
                                    alive = false;
                                }
                            } else {
                                response = null; // No respondemos hasta el último chunk
                            }
                        } else {
                            response = null;
                        }
                        break;
                    }
                    
                    case PeerMessageOps.OP_FILELIST: {
                        FileInfo[] files = NanoFiles.db.getFiles();
                        response = new PeerMessage(PeerMessageOps.OP_FILELIST_OK);
                        response.setFileList(files);
                        break;
                    }
                    
                    default:
                        response = new PeerMessage(PeerMessageOps.OP_INVALID);
                        break;
                }
                
                if (response != null) {
                    System.out.println("Sending response: " + PeerMessageOps.opcodeToOperation(response.getOpcode()));
                    response.writeMessageToOutputStream(dos);
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public void stopServer() {
        alive = false;
        // Cierra todos los hilos de clientes
        for (NFServerThread thread : clientThreads) {
            thread.interrupt();
        }
        clientThreads.clear();
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    public int getPort() {
        return ephemeralPort;
    }
}