package es.um.redes.nanoFiles.udp.message;

public class DirMessageOps {
    // Operación inválida
    public static final String OPERATION_INVALID = "invalid_operation";

    // Ping
    public static final String OPERATION_PING = "ping";
    public static final String OPERATION_PING_OK = "pingOk";
    public static final String OPERATION_PING_BAD = "pingBad";

    // Registro de un servidor de ficheros
    public static final String OPERATION_SERVE = "serve";
    public static final String OPERATION_SERVE_OK = "serveOk";

    // Obtención de la lista de ficheros
    public static final String OPERATION_FILELIST = "filelist";
    public static final String OPERATION_FILELIST_OK = "filelistOk";

    // Obtención de la lista de servidores que comparten un fichero
    public static final String OPERATION_GET_SERVERS = "servers";
    public static final String OPERATION_SERVERS_LIST = "serversOk";
    
    // Baja de un servidor de ficheros
    public static final String OPERATION_UNREGISTER = "unregister";
    public static final String OPERATION_UNREGISTER_OK = "unregisterOk";

    // Búsqueda de ficheros por nombre o hash
    public static final String OPERATION_SEARCH_FILE = "searchFile";
    public static final String OPERATION_SEARCH_FILE_OK = "searchFileOk";

    // Encuentra el fichero buscado
    public static final String OPERATION_FILEFOUND = "filefound";
    public static final String OPERATION_FILEFOUND_OK = "filefoundOk";
    
}