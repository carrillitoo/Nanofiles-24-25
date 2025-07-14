package es.um.redes.nanoFiles.logic;

import java.net.InetSocketAddress;
import java.io.*;
import java.util.*;
import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.util.FileDigest;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
    private NFServer fileServer = null;
    private Thread serverThread = null;
    private static final int CHUNK_SIZE = 4096;

    /**
     * Test method for TCP server (used in test mode)
     */
    protected void testTCPServer() {
        assert (NanoFiles.testModeTCP);
        assert (fileServer == null);
        try {
            fileServer = new NFServer();
            int testPort = fileServer.getPort(); // Obtiene el puerto efímero asignado
            System.out.println("Test server started on port: " + testPort);
            fileServer.test();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.err.println("Cannot start the file server");
            fileServer = null;
        }
    }

    /**
     * Test method for TCP client (used in test mode)
     */
    public void testTCPClient() {
        assert (NanoFiles.testModeTCP);
        try {
            
            if (fileServer == null) {
                fileServer = new NFServer();
                int serverPort = fileServer.getPort();
                System.out.println("Test server started on port: " + serverPort);
                
               
                NFConnector nfConnector = new NFConnector(
                    new InetSocketAddress("localhost", serverPort)
                );
                nfConnector.test();
                nfConnector.close();
            } else {
                System.err.println("Server is already running");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Test client failed: " + e.getMessage());
        }
    }

    /**
     * Inicia el servidor de ficheros en segundo plano
     */
    protected boolean startFileServer() {
        if (fileServer != null) {
            System.err.println("File server is already running on port " + fileServer.getPort());
            return true;
        }

        try {
            fileServer = new NFServer();
            int assignedPort = fileServer.getPort();
            serverThread = new Thread(fileServer);
            serverThread.start();
            
            // Pequeña pausa para asegurar que el servidor se inicia
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return true;
        } catch (IOException e) {
            System.err.println("Cannot start file server: " + e.getMessage());
            fileServer = null;
            return false;
        }
    }

    protected boolean downloadFileFromServers(InetSocketAddress[] serverAddressList, 
            String targetFileNameSubstring, String localFileName) {
        if (serverAddressList.length == 0) {
            System.err.println("* Cannot start download - No servers provided");
            return false;
        }

        File localFile = new File(NanoFiles.sharedDirname + "/" + localFileName);
        if (localFile.exists()) {
            System.err.println("* File already exists locally: " + localFileName);
            return false;
        }

        // Primera fase: obtenemos información del archivo
        String fileHash = null;
        long fileSize = 0;
        String remoteFileName = null;
        
        try {
            NFConnector connector = new NFConnector(serverAddressList[0]);
            PeerMessage fileInfoRequest = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FILE);
            fileInfoRequest.setFilename(targetFileNameSubstring);
            PeerMessage response = connector.sendAndReceiveMessage(fileInfoRequest);
            connector.close();

            if (response.getOpcode() != PeerMessageOps.OP_DOWNLOAD_OK) {
                System.err.println("* File not found on server");
                return false;
            }

            fileHash = response.getFileHash();
            fileSize = response.getFileSize();
            remoteFileName = response.getFilename();
            
            System.out.println("* Downloading file: " + remoteFileName);
            System.out.println("* Size: " + fileSize + " bytes");
            System.out.println("* Hash: " + fileHash);

        } catch (IOException e) {
            System.err.println("* Cannot connect to initial server: " + e.getMessage());
            return false;
        }

        // Segunda fase: distribuimos la descarga entre los servidores disponibles
        try (RandomAccessFile outputFile = new RandomAccessFile(NanoFiles.sharedDirname + "/" + localFileName, "rw")) {
            outputFile.setLength(fileSize);
            
            // Calculamos el tamaño del bloque por servidor
            long bytesPerServer = fileSize / serverAddressList.length;
            // Ajustamo para que sea múltiplo de CHUNK_SIZE
            bytesPerServer = (bytesPerServer / CHUNK_SIZE + 1) * CHUNK_SIZE;
            
            for (int serverIndex = 0; serverIndex < serverAddressList.length; serverIndex++) {
                InetSocketAddress currentServer = serverAddressList[serverIndex];
                long startOffset = serverIndex * bytesPerServer;
                long endOffset = (serverIndex == serverAddressList.length - 1) ? 
                               fileSize : Math.min((serverIndex + 1) * bytesPerServer, fileSize);
                
                if (startOffset >= fileSize) break;
                
                try {
                    NFConnector connector = new NFConnector(currentServer);
                    
                    // Inicia la descarga con este servidor
                    PeerMessage fileRequest = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FILE);
                    fileRequest.setFilename(targetFileNameSubstring);
                    PeerMessage fileResponse = connector.sendAndReceiveMessage(fileRequest);
                    
                    if (fileResponse.getOpcode() == PeerMessageOps.OP_DOWNLOAD_OK) {
                        // Descarga el bloque completo de este servidor
                        long currentOffset = startOffset;
                        while (currentOffset < endOffset) {
                            int currentChunkSize = (int) Math.min(CHUNK_SIZE, endOffset - currentOffset);
                            PeerMessage chunkRequest = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_CHUNK);

                            chunkRequest.setOffset(currentOffset);
                            chunkRequest.setChunkSize(currentChunkSize);
                            
                            System.out.println("[NFController] Offset: " + currentOffset +", ChunkSize: " + currentChunkSize);
                            
                            PeerMessage chunkResponse = connector.sendAndReceiveMessage(chunkRequest);
                            if (chunkResponse.getOpcode() == PeerMessageOps.OP_DOWNLOAD_CHUNK_RESPONSE) {
                                byte[] chunk = chunkResponse.getChunk();
                                
//                                System.out.println("[NFController2] Chunk: " + new String(chunk, 0, chunk.length));
                                
                                if (chunk != null && chunk.length > 0) {
                                    outputFile.seek(currentOffset);
                                    outputFile.write(chunk);
                                    currentOffset += chunk.length;
                                    
                                    System.out.printf("* Downloaded from server %s: %.1f%%\n",
                                        currentServer.getAddress().getHostAddress(),
                                        ((double)currentOffset / fileSize) * 100);
                                }
                            }
                        }
                        
                        // Enviamos mensaje de finalización
                        PeerMessage completeMessage = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_COMPLETED);
                        connector.sendMessage(completeMessage);
                    }
                    
                    connector.close();
                    
                } catch (IOException e) {
                    System.err.println("* Error downloading from " + currentServer + ": " + e.getMessage());
                    // Si falla un servidor, se intenta redistribuir su parte entre los demás
                    if (serverIndex < serverAddressList.length - 1) {
                        bytesPerServer = (endOffset - startOffset) / (serverAddressList.length - serverIndex - 1);
                        bytesPerServer = (bytesPerServer / CHUNK_SIZE + 1) * CHUNK_SIZE;
                    }
                    continue;
                }
            }

            // Verificamos la integridad del archivo
            String downloadedHash = FileDigest.computeFileChecksumString(NanoFiles.sharedDirname + "/" + localFileName);
            if (!fileHash.equals(downloadedHash)) {
                System.err.println("* Warning: File integrity check failed");
                System.err.println("* Expected: " + fileHash);
                System.err.println("* Got: " + downloadedHash);
                new File(localFileName).delete();
                return false;
            }
            
            System.out.println("\n* Download completed successfully");
            return true;
            
        } catch (IOException e) {
            System.err.println("* Download failed: " + e.getMessage());
            new File(localFileName).delete();
            return false;
        }
    }

    /**
     * Obtiene el puerto de escucha del servidor
     */
    protected int getServerPort() {
        return fileServer != null ? fileServer.getPort() : 0;
    }

    /**
     * Detiene el servidor de ficheros
     */
    protected void stopFileServer() {
        if (fileServer != null) {
            fileServer.stopServer();
            fileServer = null;
            serverThread = null;
        }
    }
    
    /**
     * Checks if server is running
     */
    protected boolean serving() {
        return fileServer != null && fileServer.isAlive();
    }
    
    /**
     * Uploads a file to another server (to be implemented)
     */
    protected boolean uploadFileToServer(FileInfo matchingFile, String uploadToServer) {
        boolean result = false;
        
        // Parsea dirección y puerto
        int idx = uploadToServer.lastIndexOf(':');
        if (idx == -1) {
            System.err.println("* Upload error: invalid address format.");
            return false;
        }
        String host = uploadToServer.substring(0, idx);
        int port = Integer.parseInt(uploadToServer.substring(idx + 1));
        InetSocketAddress serverAddress = new InetSocketAddress(host, port);

        NFConnector connector = null;
        try {
            // Crea la conexión con el servidor
            connector = new NFConnector(serverAddress);
            
            // Envia la solicitud de upload y espera una respuesta
            PeerMessage uploadRequest = new PeerMessage(PeerMessageOps.OP_UPLOAD_REQUEST, matchingFile);
            PeerMessage response = connector.sendAndReceiveMessage(uploadRequest);

            if (response.getOpcode() != PeerMessageOps.OP_UPLOAD_OK) {
                System.err.println("* Upload request denied.");
                connector.close();
                return false;
            }

            // Calcula número de chunks
            final int upload_chunk_size = CHUNK_SIZE; // Usar tu constante definida
            int fileChunks = (int) Math.ceil((double) matchingFile.fileSize / upload_chunk_size);
            int remaining_chunks = fileChunks;

            // Leemos y enviamos chunks
            try (RandomAccessFile file = new RandomAccessFile(NanoFiles.sharedDirname + "/" + matchingFile.fileName, "r")) {
                while (remaining_chunks > 0) {
                    int requestBytes;
                    if (remaining_chunks == 1) {
                        long rest = matchingFile.fileSize % upload_chunk_size;
                        requestBytes = (rest == 0) ? upload_chunk_size : (int) rest;
                    } else {
                        requestBytes = upload_chunk_size;
                    }

                    long offset = (fileChunks - remaining_chunks) * upload_chunk_size;
                    file.seek(offset);
                    byte[] chunk = new byte[requestBytes];
                    file.readFully(chunk);

                    // Envia el chunk y no espera respuesta, excepto para el último
                    PeerMessage chunkMessage = new PeerMessage(PeerMessageOps.OP_UPLOAD_CHUNK, offset, requestBytes, chunk);
                    
                    if (remaining_chunks == 1) {
                        // Para el último chunk, esperamos confirmación
                        response = connector.sendAndReceiveMessage(chunkMessage);
                        if (response.getOpcode() == PeerMessageOps.OP_UPLOAD_FINISHED_OK) {
                            System.out.println("* Upload successful");
                            result = true;
                        } else {
                            System.err.println("* Upload failed: Invalid response from server");
                        }
                    } else {
                        // Para los chunks intermedios, solo enviamos
                        connector.sendMessage(chunkMessage);
                    }
                    
                    remaining_chunks--;
                }
            } catch (IOException e) {
                System.err.println("* Upload failed: " + e.getMessage());
                result = false;
            }

        } catch (IOException e) {
            System.err.println("* Upload error: " + e.getMessage());
            result = false;
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (IOException e) {
                    // Ignora errores al cerrar
                }
            }
        }

        return result;
    }
}