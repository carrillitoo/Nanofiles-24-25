
package es.um.redes.nanoFiles.tcp.message;

import java.util.Map;
import java.util.TreeMap;

public class PeerMessageOps {

    public static final byte OPCODE_INVALID_CODE = 0;
    
    /*
	 * TODO: (Boletín MensajesBinarios) Añadir aquí todas las constantes que definen
	 * los diferentes tipos de mensajes del protocolo de comunicación con un par
	 * servidor de ficheros (valores posibles del campo "operation").
	 */
    public static final byte OP_DOWNLOAD_FILE = 1;
    public static final byte OP_DOWNLOAD_OK = 2;
    public static final byte OP_DOWNLOAD_FAIL = 3;
    public static final byte OP_DOWNLOAD_CHUNK = 4;
    public static final byte OP_DOWNLOAD_CHUNK_RESPONSE = 5;
    public static final byte OP_DOWNLOAD_COMPLETED = 6;
    public static final byte OP_UPLOAD_REQUEST = 7;
    public static final byte OP_UPLOAD_OK = 8;
    public static final byte OP_UPLOAD_FAIL = 9;
    public static final byte OP_UPLOAD_CHUNK = 10;
    public static final byte OP_UPLOAD_FINISHED_OK = 11;
    public static final byte OP_FILELIST = 12;
    public static final byte OP_FILELIST_OK = 13;
    public static final byte OP_INVALID = 14;

    /*
     * TODO: (Boletín MensajesBinarios) Definir constantes con nuevos opcodes de
     * mensajes definidos anteriormente, añadirlos al array "valid_opcodes" y añadir
     * su representación textual a "valid_operations_str" EN EL MISMO ORDEN.
     */
    private static final Byte[] _valid_opcodes = { 
        OPCODE_INVALID_CODE,
        OP_DOWNLOAD_FILE,
        OP_DOWNLOAD_OK,
        OP_DOWNLOAD_FAIL,
        OP_DOWNLOAD_CHUNK,
        OP_DOWNLOAD_CHUNK_RESPONSE,
        OP_DOWNLOAD_COMPLETED,
        OP_UPLOAD_REQUEST,
        OP_UPLOAD_OK,
        OP_UPLOAD_FAIL,
        OP_UPLOAD_CHUNK,
        OP_UPLOAD_FINISHED_OK,
        OP_FILELIST,
        OP_FILELIST_OK,
        OP_INVALID
    };
    
    private static final String[] _valid_operations_str = { 
        "invalid_opcode",
        "downloadFile",
        "downloadOk",
        "downloadFail",
        "downloadChunk",
        "downloadChunkOk",
        "downloadCompleted",
        "uploadRequest",
        "uploadOk",
        "uploadFail",
        "uploadChunk",
        "uploadFinishedOk",
        "filelist",
        "filelistOk",
        "invalid_operation"
    };

    private static Map<String, Byte> _operation_to_opcode;
    private static Map<Byte, String> _opcode_to_operation;

    static {
        _operation_to_opcode = new TreeMap<>();
        _opcode_to_operation = new TreeMap<>();
        for (int i = 0; i < _valid_operations_str.length; ++i) {
            _operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
            _opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
        }
    }

    /**
     * Transforma una cadena en el opcode correspondiente
     */
    protected static byte operationToOpcode(String opStr) {
        return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OPCODE_INVALID_CODE);
    }

    /**
     * Transforma un opcode en la cadena correspondiente
     */
    public static String opcodeToOperation(byte opcode) {
        return _opcode_to_operation.getOrDefault(opcode, null);
    }
}
