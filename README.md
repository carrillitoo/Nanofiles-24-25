# Nanofiles-24-25
P2P and Server-Client model with some useful functionalities.

The NanoFiles program will interact with the user through a command line interface. When executed, it can optionally be provided with an argument, which is the path to the directory where the files shared by the peer in the nanoFiles system are located. By default, the NanoFiles program will look for a directory called nf-shared starting from the current directory, which will be created when the program is run if it does not already exist. In the usual scenario where the program is run from the Eclipse IDE, the nf-shared shared files directory will be created in the root directory of the Java project (e.g., nanoFilesP2Palumnos), at the same level as the bin and src directories.

Once running, the client program accepts a set of commands, although the functionality associated with most of these commands is not yet implemented and therefore has no effect when typed. This functionality must be added by the students, although the implementation of some of the commands already accepted by the NanoFiles shell is not part of the minimum required functionality, but rather constitutes possible improvements. Both the minimum required functionality and the possible improvements will be described later in this document.

The commands whose functionality is already implemented, and which do not involve any inter-process communication, are:

help: Displays help about the supported commands.
myfiles: Shows the files in the folder shared by this peer, indicating their name, size, and hash. The default directory is nf-shared, and it will generally be located within the Eclipse project directory.

On the other hand, the commands recognized by the NanoFiles shell whose functionality is yet to be implemented are:

--> ping: Checks that the directory server is active and uses a protocol compatible with the peer.
--> filelist: Shows the files that are being shared by other peers, which have been published in the directory.
--> serve: Launches a file server that listens for connections on an ephemeral port. When a file server is launched, the metadata of the files that this peer has in its shared folder is automatically published to the directory.
--> download <filename_substring> <local_filename>: Downloads the file identified by the first parameter (<filename_substring>) from all file servers that have it available, and saves it with the name indicated as the second parameter (<local_filename>).
--> upload <filename_substring> <remote_server>: Uploads (copies) a local file to another remote peer that is serving files. The first parameter (<filename_substring>) indicates the local file to send, using a substring of the file name that uniquely identifies it. The second parameter (<remote_server>) is the address of the destination server for the upload.
--> quit: Exits the program
