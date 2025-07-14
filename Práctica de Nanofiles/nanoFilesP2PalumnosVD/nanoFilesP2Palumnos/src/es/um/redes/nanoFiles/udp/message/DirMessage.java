package es.um.redes.nanoFiles.udp.message;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases DirectoryServer y
 * DirectoryConnector, y se codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class DirMessage {
	public static final int PACKET_MAX_SIZE = 65507; // 65535 - 8 (UDP header) - 20 (IP header)

	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	/*
	 * TODO: (Boletín MensajesASCII) Definir de manera simbólica los nombres de
	 * todos los campos que pueden aparecer en los mensajes de este protocolo
	 * (formato campo:valor)
	 */
	private static final String FIELDNAME_PROTOCOL = "protocol";
	private static final String FIELDNAME_FILELIST = "files";
	
	private static final String FIELDNAME_FILEHASH = "filehash";
	private static final String FIELDNAME_FILENAME = "filename";
	private static final String FIELDNAME_FILESIZE = "filesize";

	private static final String FIELDNAME_SERVERPORT = "serverport";

	private static final String FIELDNAME_SEARCHFILE= "searchfile";
	private static final String FIELDNAME_HOSTS= "hosts";
	
	private static final String FIELDNAME_PEER_IP = "peerip";
	private static final String FIELDNAME_PEER_PORT = "peerport";

	private static final String FIELDNAME_TARGET = "target";
	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation = DirMessageOps.OPERATION_INVALID;
	/**
	 * Identificador de protocolo usado, para comprobar compatibilidad del directorio.
	 */
	private String protocolId;
	/*
	 * TODO: (Boletín MensajesASCII) Crear un atributo correspondiente a cada uno de
	 * los campos de los diferentes mensajes de este protocolo.
	 */
	private ArrayList<FileInfo> files;
	private int serverport;
	private String filename;
	private Set<InetSocketAddress> hosts;
	
	
	/*
	 * TODO: (Boletín MensajesASCII) Crear diferentes constructores adecuados para
	 * construir mensajes de diferentes tipos con sus correspondientes argumentos
	 * (campos del mensaje)
	 */
	//FILELIST SERVE_OK 
	public DirMessage(String op) {
		operation = op;
		files = new ArrayList<FileInfo>();
		hosts = new HashSet<InetSocketAddress>();
	}
	//PING
	public DirMessage(String op, String idOfProtocol) {
		operation = op;
		protocolId = idOfProtocol;
	}
	//FILELIST_OK
	public DirMessage(String op, ArrayList<FileInfo> files) {	
		operation = op;
		this.files= files;
	}
	//SERVE
	public DirMessage(String op, int serverPort, ArrayList<FileInfo> files) {	
		operation = op;
		this.serverport = serverPort;
		this.files= files;
	}
	
	public DirMessage(String op, Set<InetSocketAddress> hosts) {
		this.operation = op;
		this.hosts = hosts;
	}
	
	public String getOperation() {
		return operation;
	}

	/*
	 * TODO: (Boletín MensajesASCII) Crear métodos getter y setter para obtener los
	 * valores de los atributos de un mensaje. Se aconseja incluir código que
	 * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
	 * esté definido para el tipo de mensaje dado por "operation".
	 */
	public void setProtocolID(String protocolIdent) {
		if (!operation.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException(
					"DirMessage: setProtocolId called for message of unexpected type (" + operation + ")");
		}
		protocolId = protocolIdent;
	}

	public String getProtocolId() {
		if (!operation.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException(
					"DirMessage: setProtocolId called for message of unexpected type (" + operation + ")");
		}
		return protocolId;
	}
	
	public void setServerPort(String serverPortAsString) {
		if (!operation.equals(DirMessageOps.OPERATION_SERVE)) {
			throw new RuntimeException(
					"DirMessage: setServerPort called for message of unexpected type (" + operation + ")");
		}
		serverport = Integer.parseInt(serverPortAsString);
	}
	
	public int getServerPort() {
		if (!operation.equals(DirMessageOps.OPERATION_SERVE)) {
			throw new RuntimeException(
					"DirMessage: getServerPort called for message of unexpected type (" + operation + ")");
		}
		return serverport;
	}
	
	public void setFileInfo(FileInfo file) {
	    if (!(operation.equals(DirMessageOps.OPERATION_SERVE) || 
	         operation.equals(DirMessageOps.OPERATION_FILELIST_OK))) {
	        throw new RuntimeException(
	            "DirMessage: setFileInfo called for message of unexpected type (" + operation + ")");
	    }
	    files.add(file);
	}
	
	public FileInfo[] getFilesInfo() {
	    if (!(operation.equals(DirMessageOps.OPERATION_SERVE) || 
	          operation.equals(DirMessageOps.OPERATION_FILELIST_OK))) {
	        throw new RuntimeException(
	            "DirMessage: getFilesInfo called for message of unexpected type (" + operation + ")");
	    }
	    return files.toArray(new FileInfo[0]);
	}
	
	public void setHosts(InetSocketAddress peerAddr) {
		if (!operation.equals(DirMessageOps.OPERATION_SERVERS_LIST)) {
			throw new RuntimeException(
					"DirMessage: setHosts called for message of unexpected type (" + operation + ")");
		}
		hosts.add(peerAddr);
	}
	
	public InetSocketAddress[] getHosts() {
	    if (!(operation.equals(DirMessageOps.OPERATION_FILEFOUND) || operation.equals(DirMessageOps.OPERATION_SERVERS_LIST))) {
	        throw new RuntimeException(
	            "DirMessage: getHosts called for message of unexpected type (" + operation + ")");
	    }
	    return hosts.toArray(new InetSocketAddress[0]);
	}

	public void setTarget(String filename) {
	    if (!(operation.equals(DirMessageOps.OPERATION_GET_SERVERS) || 
	         operation.equals(DirMessageOps.OPERATION_SEARCH_FILE))) {
	        throw new RuntimeException(
	            "DirMessage: setFilename called for message of unexpected type (" + operation + ")");
	    }
	    this.filename = filename;
	}

	public String getTarget() {
	    if (!(operation.equals(DirMessageOps.OPERATION_GET_SERVERS) || 
	         operation.equals(DirMessageOps.OPERATION_SEARCH_FILE))) {
	        throw new RuntimeException(
	            "DirMessage: getFilename called for message of unexpected type (" + operation + ")");
	    }
	    return filename;
	}
	
	
	
	
	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static DirMessage fromString(String message) {
		/*
		 * TODO: (Boletín MensajesASCII) Usar un bucle para parsear el mensaje línea a
		 * línea, extrayendo para cada línea el nombre del campo y el valor, usando el
		 * delimitador DELIMITER, y guardarlo en variables locales.
		 */

		String[] lines = message.split(END_LINE + "");
		// Variables locales para almacenar datos
		DirMessage m = null;
		String IP = null;
		FileInfo tempFile = null;
		
		for (String line : lines) {
			int idx = line.indexOf(DELIMITER); // Posición del delimitador
			String fieldName = line.substring(0, idx).toLowerCase(); // minúsculas
			String value = line.substring(idx + 1).trim();

			switch (fieldName) {
			case FIELDNAME_OPERATION: {
				assert (m == null);
				m = new DirMessage(value);
				break;
			}
			case FIELDNAME_PROTOCOL: {
				m.setProtocolID(value);
				break;
			}
			case FIELDNAME_SERVERPORT: {
				m.setServerPort(value);
				break;
			}
			case FIELDNAME_FILENAME: {
				assert (tempFile != null);
				tempFile = new FileInfo();
				tempFile.fileName = value;
				break;
			}
			case FIELDNAME_FILESIZE: {
				assert (tempFile != null);
				tempFile.fileSize = Long.parseLong(value);
				m.setFileInfo(tempFile);
				break;
			}
			case FIELDNAME_FILEHASH: {
				tempFile.fileHash = value;
				break;
			}
			case FIELDNAME_HOSTS: {
			    assert (m != null);
			    String[] parts = value.split(":");
			    if (parts.length != 2) {
			        throw new IllegalArgumentException("Invalid format for HOSTS: " + value);
			    }
			    String ip = parts[0];
			    int port = Integer.parseInt(parts[1]);
			    InetSocketAddress addr = new InetSocketAddress(ip, port);
			    m.setHosts(addr);
			    break;
			}
			case FIELDNAME_TARGET: {
				assert(m != null);
				m.setTarget(value);
				break;
			}
			case FIELDNAME_SEARCHFILE: {
			    assert (m != null);
			    m.setTarget(value);
			    break;
			}
			case FIELDNAME_PEER_IP: {
			    IP = value;
			    break;
			}
			case FIELDNAME_PEER_PORT: {
			    assert(IP != null);
			    m.setHosts(new InetSocketAddress(IP, Integer.parseInt(value)));
			    break;
			}
			
			default:
				System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
				System.err.println("Message was:\n" + message);
				System.exit(-1);
			}
		}

		return m;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toString() {
	    StringBuffer sb = new StringBuffer();
	    sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE); // Construimos el campo de operación

	    /*
	     * TODO: (Boletín MensajesASCII) En función de la operación del mensaje, crear
	     * una cadena con la operación y concatenar el resto de campos necesarios usando los
	     * valores de los atributos del objeto.
	     */
	    
	    
	    switch (operation) {
	        case DirMessageOps.OPERATION_PING: {
	            sb.append(FIELDNAME_PROTOCOL + DELIMITER + protocolId + END_LINE);
	            break;
	        }
	        case DirMessageOps.OPERATION_SERVE:
	        case DirMessageOps.OPERATION_FILELIST: {
				if (operation.equals(DirMessageOps.OPERATION_SERVE))		
					sb.append(FIELDNAME_SERVERPORT + DELIMITER + serverport + END_LINE);
				for (FileInfo f : files) {
					sb.append(FIELDNAME_FILENAME + DELIMITER + f.fileName + END_LINE);
					sb.append(FIELDNAME_FILESIZE + DELIMITER + f.fileSize + END_LINE);
					sb.append(FIELDNAME_FILEHASH + DELIMITER + f.fileHash + END_LINE);
				}
				break;
			}
	        case DirMessageOps.OPERATION_FILELIST_OK: {
				for (FileInfo f : files) {
					sb.append(FIELDNAME_FILENAME + DELIMITER + f.fileName + END_LINE);
					sb.append(FIELDNAME_FILESIZE + DELIMITER + f.fileSize + END_LINE);
					sb.append(FIELDNAME_FILEHASH + DELIMITER + f.fileHash + END_LINE);
				}
				break;
			}
	        case DirMessageOps.OPERATION_GET_SERVERS: {
	            sb.append(FIELDNAME_SEARCHFILE + DELIMITER + filename + END_LINE);
	            break;
	        }
	        case DirMessageOps.OPERATION_SERVERS_LIST: {
	            for (InetSocketAddress h : hosts) {
	                String hostString = h.getAddress().getHostAddress() + ":" + h.getPort();
	                sb.append(FIELDNAME_HOSTS + DELIMITER + hostString + END_LINE);
	            }
	            break;
	        
	        }

	      
	    }

	    sb.append(END_LINE); // Marcamos el final del mensaje
	    return sb.toString();
	}

}
