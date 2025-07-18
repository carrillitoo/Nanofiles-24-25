package es.um.redes.nanoFiles.udp.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DIRECTORY_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;
	/**
	 * Nombre/IP del host donde se ejecuta el directorio
	 */
	private String directoryHostname;



	public DirectoryConnector(String hostname) throws IOException {
		// Guardamos el string con el nombre/IP del host
		directoryHostname = hostname;
		/*
		 * TODO: (Boletín SocketsUDP) Convertir el string 'hostname' a InetAddress y
		 * guardar la dirección de socket (address:DIRECTORY_PORT) del directorio en el
		 * atributo directoryAddress, para poder enviar datagramas a dicho destino.
		 */
		InetAddress directoryInetAddress = InetAddress.getByName(directoryHostname);
	    directoryAddress = new InetSocketAddress(directoryInetAddress, DIRECTORY_PORT);
		
		/*
		 * TODO: (Boletín SocketsUDP) Crea el socket UDP en cualquier puerto para enviar
		 * datagramas al directorio
		 */
		socket = new DatagramSocket();
		socket.setSoTimeout(TIMEOUT);
	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	private byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}
		/*
		 * TODO: (Boletín SocketsUDP) Enviar datos en un datagrama al directorio y
		 * recibir una respuesta. El array devuelto debe contener únicamente los datos
		 * recibidos, *NO* el búfer de recepción al completo.
		 */
		/*
		 * TODO: (Boletín SocketsUDP) Una vez el envío y recepción asumiendo un canal
		 * confiable (sin pérdidas) esté terminado y probado, debe implementarse un
		 * mecanismo de retransmisión usando temporizador, en caso de que no se reciba
		 * respuesta en el plazo de TIMEOUT. En caso de salte el timeout, se debe volver
		 * a enviar el datagrama y tratar de recibir respuestas, reintentando como
		 * máximo en MAX_NUMBER_OF_ATTEMPTS ocasiones.
		 */
		/*
		 * TODO: (Boletín SocketsUDP) Las excepciones que puedan lanzarse al
		 * leer/escribir en el socket deben ser capturadas y tratadas en este método. Si
		 * se produce una excepción de entrada/salida (error del que no es posible
		 * recuperarse), se debe informar y terminar el programa.
		 */
		/*
		 * NOTA: Las excepciones deben tratarse de la más concreta a la más genérica.
		 * SocketTimeoutException es más concreta que IOException.
		 */
		 int attempts = 0; // Contador de intentos
		    boolean receivedResponse = false;

		    while (!receivedResponse && attempts < MAX_NUMBER_OF_ATTEMPTS) {
		        try {
		            // Crear el datagrama para enviar los datos al directorio
		            DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, directoryAddress);
		            socket.send(sendPacket); // Enviar el datagrama

		            // Crear el datagrama para recibir la respuesta
		            DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length);
		            socket.receive(receivePacket); // Recibir la respuesta

		            // Extraer solo los datos recibidos (no todo el búfer de recepción)
		            response = new byte[receivePacket.getLength()];
		            System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), response, 0, receivePacket.getLength());

		            receivedResponse = true; // Marcar que se recibió una respuesta
		        } catch (SocketTimeoutException e) {
		            // Timeout: no se recibió respuesta en el plazo esperado
		            attempts++;
		            System.err.println("DirectoryConnector.sendAndReceiveDatagrams: Timeout occurred. Attempt " + attempts + " of " + MAX_NUMBER_OF_ATTEMPTS);
		            if (attempts >= MAX_NUMBER_OF_ATTEMPTS) {
		                System.err.println("DirectoryConnector.sendAndReceiveDatagrams: Max attempts reached. No response from directory.");
		            }
		        } catch (IOException e) {
		            // Error de entrada/salida: no es posible recuperarse
		            System.err.println("DirectoryConnector.sendAndReceiveDatagrams: I/O error occurred: " + e.getMessage());
		            System.exit(-1);
		        }
		    }

		    if (response != null && response.length == responseData.length) {
		        System.err.println("Your response is as large as the datagram reception buffer!!\n"
		                + "You must extract from the buffer only the bytes that belong to the datagram!");
		    }
		    return response;
		}

	/**
	 * Método para probar la comunicación con el directorio mediante el envío y
	 * recepción de mensajes sin formatear ("en crudo")
	 * 
	 * @return verdadero si se ha enviado un datagrama y recibido una respuesta
	 */
	public boolean testSendAndReceive() {
	    boolean success = false;

	    /*
	     * TODO: (Boletín SocketsUDP) Probar el correcto funcionamiento de
	     * sendAndReceiveDatagrams. Se debe enviar un datagrama con la cadena "ping" y
	     * comprobar que la respuesta recibida empieza por "pingok". En tal caso,
	     * devuelve verdadero, falso si la respuesta no contiene los datos esperados.
	     */
	    try {
	        // Convertir la cadena "ping" en un array de bytes para enviarla
	        byte[] requestData = "ping".getBytes();

	        // Enviar el datagrama y recibir la respuesta
	        byte[] responseData = sendAndReceiveDatagrams(requestData);

	        // Verificar si se recibió una respuesta válida
	        if (responseData != null) {
	            // Convertir la respuesta a String
	            String response = new String(responseData);

	            // Comprobar si la respuesta comienza con "pingok"
	            if (response.startsWith("pingok")) {
	                System.out.println("testSendAndReceive: Se recibió una respuesta válida " + response);
	                success = true;
	            } else {
	                System.err.println("testSendAndReceive: Respuesta no válida " + response);
	            }
	        } else {
	            System.err.println("testSendAndReceive: No se recibió respuesta del directorio");
	        }
	    } catch (Exception e) {
	        // Capturar cualquier excepción inesperada
	        System.err.println("testSendAndReceive: Ocurrió un error inesperado: " + e.getMessage());
	    }

	    return success;
	}

	public String getDirectoryHostname() {
		return directoryHostname;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que
	 * usa un protocolo compatible. Este método no usa mensajes bien formados.
	 * 
	 * @return Verdadero si
	 */
	public boolean pingDirectoryRaw() {
		boolean success = false;
		/*
		 * TODO: (Boletín EstructuraNanoFiles) Basándose en el código de
		 * "testSendAndReceive", contactar con el directorio, enviándole nuestro
		 * PROTOCOL_ID (ver clase NanoFiles). Se deben usar mensajes "en crudo" (sin un
		 * formato bien definido) para la comunicación.
		 * 
		 * PASOS: 1.Crear el mensaje a enviar (String "ping&protocolId"). 2.Crear un
		 * datagrama con los bytes en que se codifica la cadena : 4.Enviar datagrama y
		 * recibir una respuesta (sendAndReceiveDatagrams). : 5. Comprobar si la cadena
		 * recibida en el datagrama de respuesta es "welcome", imprimir si éxito o
		 * fracaso. 6.Devolver éxito/fracaso de la operación.
		 */
		try {
	        // Paso 1: Crear el mensaje a enviar (String "ping&protocolId")
	        String protocolId = NanoFiles.PROTOCOL_ID; // Obtener el PROTOCOL_ID de la clase NanoFiles
	        String message = "ping&" + protocolId;

	        // Paso 2: Convertir el mensaje a un array de bytes
	        byte[] requestData = message.getBytes();

	        // Paso 3: Enviar el datagrama y recibir una respuesta
	        byte[] responseData = sendAndReceiveDatagrams(requestData);

	        // Paso 4: Comprobar si la respuesta es "welcome"
	        if (responseData != null) {
	            String response = new String(responseData); // Convertir la respuesta a String
	            if (response.equals("welcome")) {
	                System.out.println("pingDirectoryRaw: Respuesta válida: " + response);
	                success = true; // Paso 6: Éxito
	            } else {
	                System.err.println("pingDirectoryRaw: IRespuesta no válida: " + response);
	            }
	        } else {
	            System.err.println("pingDirectoryRaw: No se recibió respuesta del directorio.");
	        }
	    } catch (Exception e) {
	        // Capturar cualquier excepción inesperada
	        System.err.println("pingDirectoryRaw: Ocurrió un error inesperado" + e.getMessage());
	    }

	    // Paso 5: Imprimir si éxito o fracaso
	    if (success) {
	        System.out.println("pingDirectoryRaw: Ping al directorio exitoso");
	    } else {
	        System.err.println("pingDirectoryRaw: Ping al directorio fallido.");
	    }

	    //Paso 6
		return success;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que es
	 * compatible.
	 * 
	 * @return Verdadero si el directorio está operativo y es compatible
	 */
	public boolean pingDirectory() {
		boolean success = false;
		
		DirMessage pingMessage = new DirMessage(DirMessageOps.OPERATION_PING, NanoFiles.PROTOCOL_ID);
		String pingMessageAsString = pingMessage.toString();
		byte[]requestData = pingMessageAsString.getBytes();
		byte[]response = sendAndReceiveDatagrams(requestData);
		if( response != null) {
			String responseAsString = new String(response, 0, response.length);
			System.out.println("Receiving..." + responseAsString);
			
			DirMessage msgFromServer = DirMessage.fromString(responseAsString);
			if(msgFromServer != null && msgFromServer.getOperation().equals(DirMessageOps.OPERATION_PING_OK)) {
				success = true;
			}
		}
		return success;
	}

	/**
	 * Método para dar de alta como servidor de ficheros en el puerto indicado y
	 * publicar los ficheros que este peer servidor está sirviendo.
	 * 
	 * @param serverPort El puerto TCP en el que este peer sirve ficheros a otros
	 * @param files      La lista de ficheros que este peer está sirviendo.
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y acepta la lista de ficheros, falso en caso contrario.
	 */
	/*private String getActiveAddress() {
		String activeAddress = null;
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces.hasMoreElements()) {
				NetworkInterface networkinterface = interfaces.nextElement();
				if(!networkInterface.isUp()) || networkinterface.isLoopback() || networkinterface.isVirtual()
			}
		}
	}*/
	public boolean registerFileServer(int serverPort, FileInfo[] files) {
	    boolean success = false;
	    // String activeAddress = getActiveAddress(); // HAY QUE RETOCAR CÓDIGO PARA PASAR ESTA
	    //  FileInfo[] a ArrayList<FileInfo>
	    ArrayList<FileInfo> fileList = new ArrayList<>(Arrays.asList(files));
	    DirMessage serveMessage = new DirMessage(DirMessageOps.OPERATION_SERVE, serverPort, fileList);        
	    String serveMessageAsString = serveMessage.toString();            
	    byte[] requestData = serveMessageAsString.getBytes();            
	    byte[] response = sendAndReceiveDatagrams(requestData);            
	    
	    if (response != null) {
	        String responseAsString = new String(response, 0, response.length);
	        System.out.println("Receiving..." + responseAsString);
	        DirMessage msgFromServer = DirMessage.fromString(responseAsString);
	        
	        if (msgFromServer != null && msgFromServer.getOperation().equals(DirMessageOps.OPERATION_SERVE_OK)) {
	            success = true;
	        } else {
	            System.err.println("registerFileServer: El directorio no aceptó el registro del servidor.");
	        }
	    } else {
	        System.err.println("registerFileServer: No se recibió respuesta del directorio.");
	    }

	    return success;
	}

	/**
	 * Método para obtener la lista de ficheros que los peers servidores han
	 * publicado al directorio. Para cada fichero se debe obtener un objeto FileInfo
	 * con nombre, tamaño y hash. Opcionalmente, puede incluirse para cada fichero,
	 * su lista de peers servidores que lo están compartiendo.
	 * 
	 * @return Los ficheros publicados al directorio, o null si el directorio no
	 *         pudo satisfacer nuestra solicitud
	 */
	public FileInfo[] getFileList() {
	    // Create empty message first
	    DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_FILELIST);
	    String requestMessageAsString = requestMessage.toString();
	    byte[] requestData = requestMessageAsString.getBytes();
	    byte[] response = sendAndReceiveDatagrams(requestData);
	    
	    if (response != null) {
	        String responseAsString = new String(response, 0, response.length);
	        System.out.println("Receiving..." + responseAsString);

	        DirMessage responseMessage = DirMessage.fromString(responseAsString);
	        
	        if (responseMessage != null && responseMessage.getOperation().equals(DirMessageOps.OPERATION_FILELIST_OK)) {
	            FileInfo[] filelist = responseMessage.getFilesInfo();
	            System.out.println("getFileList: Lista de ficheros obtenida correctamente.");
	            return filelist;
	        }
	    }
	    return new FileInfo[0];
	}
	/**
	 * Método para obtener la lista de servidores que tienen un fichero cuyo nombre
	 * contenga la subcadena dada.
	 * 
	 * @filenameSubstring Subcadena del nombre del fichero a buscar
	 * 
	 * @return La lista de direcciones de los servidores que han publicado al
	 *         directorio el fichero indicado. Si no hay ningún servidor, devuelve
	 *         una lista vacía.
	 */
	public InetSocketAddress[] getServersSharingThisFile(String filenameSubstring) {
	    DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_GET_SERVERS);
	    requestMessage.setTarget(filenameSubstring);
	    
	    byte[] response = sendAndReceiveDatagrams(requestMessage.toString().getBytes());
	    
	    if (response != null) {
	        DirMessage responseMessage = DirMessage.fromString(new String(response));
	         /// CREO QUE LO QUE FALLA ES QUE LA COMPARACIÓN DEL OPERATION_SERVERS_LIST
	        if (responseMessage != null && responseMessage.getOperation().equals(DirMessageOps.OPERATION_SERVERS_LIST)) {
	            return responseMessage.getHosts(); // Recibe el array directamente
	        }
	    }
	    return new InetSocketAddress[0]; // Array vacío si falla
	}

	/**
	 * Método para darse de baja como servidor de ficheros.
	 * 
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y ha dado de baja sus ficheros.
	 */
	public boolean unregisterFileServer() {
	    boolean success = false;
	    
	    DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_UNREGISTER);
	    String requestMessageAsString = requestMessage.toString();
	    byte[] requestData = requestMessageAsString.getBytes();
	    byte[] response = sendAndReceiveDatagrams(requestData);
	    if (response != null) {
	        String responseAsString = new String(response, 0, response.length);
	        System.out.println("Receiving..." + responseAsString);
	        DirMessage responseMessage = DirMessage.fromString(responseAsString);
	        if (responseMessage != null && responseMessage.getOperation().equals(DirMessageOps.OPERATION_UNREGISTER_OK)) {
	            success = true;
	            System.out.println("unregisterFileServer: Servidor dado de baja correctamente.");
	        } else {
	            System.err.println("unregisterFileServer: El directorio no pudo dar de baja al servidor.");
	        }
	    } else {
	        System.err.println("unregisterFileServer: No se recibió respuesta del directorio.");
	    }
	    return success;
	}




}
