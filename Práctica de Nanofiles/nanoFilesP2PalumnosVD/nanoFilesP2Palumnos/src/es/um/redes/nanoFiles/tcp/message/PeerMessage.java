package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {

    private byte opcode;
    private String filename;
    private long fileSize;
    private String fileHash;
    private long offset;
    private int chunkSize;
    private byte[] chunkData;
    private FileInfo[] fileList;
    private FileInfo matchFile; // Para uploadRequest
    private byte[] data;        // Para uploadChunk
    private int bytes;          // Tamaño real de data recibido
    
    /*
     * TODO: (Boletín MensajesBinarios) Añadir atributos u otros constructores
     * específicos para crear mensajes con otros campos, según sea necesario
     */

    public PeerMessage() {
        opcode = PeerMessageOps.OPCODE_INVALID_CODE;
    }

    // Constructor para mensajes de control básicos
    public PeerMessage(byte op) {
        opcode = op;
    }
    
    // Constructor para mensajes de descarga de archivo
    public PeerMessage(byte op, String filename, long fileSize, String fileHash) {
        opcode = op;
        this.filename = filename;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
    }
    
    // Constructor para mensajes de solicitud de chunk
    public PeerMessage(byte op, String filename, long offset, int chunkSize) {
        opcode = op;
        this.filename = filename;
        this.offset = offset;
        this.chunkSize = chunkSize;
    }
    
    // Constructor para mensajes de respuesta de chunk
    public PeerMessage(byte op, byte[] chunkData, int length) {
        opcode = op;
        this.chunkData = new byte[length];
        System.arraycopy(chunkData, 0, this.chunkData, 0, length);
    }
    
    // Constructor para mensajes de lista de archivos
    public PeerMessage(byte op, FileInfo[] fileList) {
        opcode = op;
        this.fileList = fileList;
    }
    
    public PeerMessage(byte op, long offset, int bytes, byte[] chunkData) {
        opcode = op;
        this.offset = offset;
        this.bytes = bytes;
        this.data = chunkData;
    }

    public PeerMessage(byte op, FileInfo file)
    {
        opcode = op;
        matchFile = file;
    }

	/*
     * TODO: (Boletín MensajesBinarios) Crear métodos getter y setter para obtener
     * los valores de los atributos de un mensaje. Se aconseja incluir código que
     * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
     * esté definido para el tipo de mensaje dado por "operation".
     */
    public byte getOpcode() {
        return opcode;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
    
    public FileInfo[] getFileList() {
        return fileList;
    }

    public void setFileList(FileInfo[] fileList) {
        this.fileList = fileList;
    }
    
    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }


    public void setChunk(byte[] data, int length) {
        if (length > 0) {
        	/*
            chunkData = new byte[length];
            System.arraycopy(data, 0, chunkData, 0, length);
            chunkSize = length; */
        	//System.out.println(new String(data,0, data.length));
        	chunkData = data;
        	chunkSize = length;
        }
    }

    public byte[] getChunk() {
        return chunkData;
    }
    
    public void setMatchFile(FileInfo f) {
    	this.matchFile = f;
    }
    
    public FileInfo getMatchFile() {
    	return matchFile;
    }

    public void setData(byte[] data) {
    	this.data = data; 
    }
    
    public byte[] getData() {
    	return data;
    }

    public void setBytes(int bytes) {
    	this.bytes = bytes;
    }
    
    public int getBytes() {
    	return bytes;
    }

    public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {
        PeerMessage message = new PeerMessage();
        try {
            message.opcode = dis.readByte();
            
            switch (message.opcode) {
                case PeerMessageOps.OP_DOWNLOAD_FILE:
                    int nameLength = dis.readInt();
                    byte[] nameBytes = new byte[nameLength];
                    dis.readFully(nameBytes);
                    message.filename = new String(nameBytes, "UTF-8");
                    break;
                    
                case PeerMessageOps.OP_DOWNLOAD_OK:
                    nameLength = dis.readInt();
                    nameBytes = new byte[nameLength];
                    dis.readFully(nameBytes);
                    message.filename = new String(nameBytes, "UTF-8");
                    message.fileSize = dis.readLong();
                    int hashLength = dis.readInt();
                    byte[] hashBytes = new byte[hashLength];
                    dis.readFully(hashBytes);
                    message.fileHash = new String(hashBytes, "UTF-8");
                    break;
                    
                case PeerMessageOps.OP_DOWNLOAD_CHUNK:
                    message.offset = dis.readLong();
                    message.chunkSize = dis.readInt();
                    if (message.fileHash != null) {
                        hashLength = dis.readInt();
                        hashBytes = new byte[hashLength];
                        dis.readFully(hashBytes);
                        message.fileHash = new String(hashBytes, "UTF-8");
                    }
                    break;
                    
                case PeerMessageOps.OP_DOWNLOAD_CHUNK_RESPONSE: {
                	message.offset = dis.readLong();
                	message.chunkSize = dis.readInt();
                	byte[] data = new byte[message.chunkSize];
                	dis.readFully(data);
                	message.setChunk(data, message.chunkSize);
                	break;
                }
                    
                case PeerMessageOps.OP_UPLOAD_REQUEST:
                {
                    short namesize = dis.readShort();
                    byte[] namedata = new byte[namesize];
                    dis.readFully(namedata);
                    short hashsize = dis.readShort();
                    byte[] hashdata = new byte[hashsize];
                    dis.readFully(hashdata);
                    long filesize = dis.readLong();
                    String hashstring = new String(hashdata, 0, hashdata.length);
                    String namestring = new String(namedata,0,namedata.length);
                    FileInfo file = new FileInfo();
                    file.fileHash = hashstring;
                    file.fileName = namestring;
                    file.fileSize = filesize;
                    message.setMatchFile(file);
                    break;
                }
                case PeerMessageOps.OP_UPLOAD_CHUNK:
                {
                    message.setOffset(dis.readLong());
                    message.setBytes(dis.readInt());
                    byte[] data = new byte[message.getBytes()];
                    dis.readFully(data);
                    message.setData(data);
                    break;
                }
            }
            return message;
        } catch (EOFException e) {
            throw new IOException("Connection closed by peer");
        }
    }

    public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {
        dos.writeByte(opcode);
        
        switch (opcode) {
            case PeerMessageOps.OP_DOWNLOAD_FILE:
                byte[] nameBytes = filename.getBytes("UTF-8");
                dos.writeInt(nameBytes.length);
                dos.write(nameBytes);
                break;
                
            case PeerMessageOps.OP_DOWNLOAD_OK:
                nameBytes = filename.getBytes("UTF-8");
                dos.writeInt(nameBytes.length);
                dos.write(nameBytes);
                dos.writeLong(fileSize);
                byte[] hashBytes = fileHash.getBytes("UTF-8");
                dos.writeInt(hashBytes.length);
                dos.write(hashBytes);
                break;
                
            case PeerMessageOps.OP_DOWNLOAD_CHUNK:
                dos.writeLong(offset);
                dos.writeInt(chunkSize);
                if (fileHash != null) {
                    hashBytes = fileHash.getBytes("UTF-8");
                    dos.writeInt(hashBytes.length);
                    dos.write(hashBytes);
                }
                break;
                
            case PeerMessageOps.OP_DOWNLOAD_CHUNK_RESPONSE:
                dos.writeLong(offset);
                dos.writeInt(chunkSize);
                if (chunkData != null && chunkSize > 0) {
                    dos.write(chunkData, 0, chunkSize);
                }
                break;
            case PeerMessageOps.OP_UPLOAD_REQUEST:
            {
                byte[] filenamebytes = matchFile.fileName.getBytes();
                dos.writeShort(filenamebytes.length);
                dos.write(filenamebytes);
                byte[] filehashbytes = matchFile.fileHash.getBytes();
                dos.writeShort(filehashbytes.length);
                dos.write(filehashbytes);
                dos.writeLong(matchFile.fileSize);
                break;
            }
            case PeerMessageOps.OP_UPLOAD_CHUNK:
            {
                dos.writeLong(offset);
                dos.writeInt(bytes);
                dos.write(data);
                break;
            }

        }
        dos.flush();
    }
}