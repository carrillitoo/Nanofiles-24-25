package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDigest;

public class NFConnector {
    private Socket socket;
    private InetSocketAddress serverAddr;
    private DataInputStream dis;
    private DataOutputStream dos;

    public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
        serverAddr = fserverAddr;
        /*
         * TODO: (Boletín SocketsTCP) Se crea el socket a partir de la dirección del
         * servidor (IP, puerto). La creación exitosa del socket significa que la
         * conexión TCP ha sido establecida.
         */
        socket = new Socket(serverAddr.getAddress(), serverAddr.getPort());
        /*
         * TODO: (Boletín SocketsTCP) Se crean los DataInputStream/DataOutputStream a
         * partir de los streams de entrada/salida del socket creado. Se usarán para
         * enviar (dos) y recibir (dis) datos del servidor.
         */
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Sends a message to the server and returns the response
     */
    public PeerMessage sendAndReceiveMessage(PeerMessage message) throws IOException {
        message.writeMessageToOutputStream(dos);
     // Recibe respuesta
        try {
            return PeerMessage.readMessageFromInputStream(dis);
        } catch (IOException e) {
            System.err.println("Error reading response: " + e.getMessage());
            throw e;
        }
    }
    // nuevo metodo para SOLO enviar el ultimo OK, sin esperar respuesta del servidor al cliente
    public void sendMessage(PeerMessage message) throws IOException {
        message.writeMessageToOutputStream(dos);
    }

    public void test() {
        /*
         * TODO: (Boletín SocketsTCP) Enviamos un entero cualquiera a través del socket y
         * después recibir otro entero, comprueba que se trata del mismo valor.
         */
        try {
            // 
            int integerToSend = 1;
            int integerReceived;
            System.out.println("Sending... " + integerToSend);
            dos.writeInt(integerToSend);
            integerReceived = dis.readInt();
            System.out.println("Integer received : " + integerReceived);
            socket.close();
            
            
        } catch (IOException e) {
            System.err.println("Communication exception" + e.getMessage());
        }
    }

    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public InetSocketAddress getServerAddr() {
        return serverAddr;
    }
}