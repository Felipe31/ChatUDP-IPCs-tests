# ChatUDP-IPCs-tests
Chat using UDP. Echo server using TCP with Threads and another using UDP.

# UDP Chat

On the package named `ServidorChatUDP`, the chat should be executed directly from the file.

The `ServerChatUDP.java` use always the same port, so do not execute more then one instance at a time.

The `ClientChatUDP.java` use a random port, so you can execute as many instances as you like.

# TCP with Threads and UDP echo

Each IPC has its own package.
The rules are the same as the UDP Chat.

You can execute as many instances as you like of `Client.java`, and do not execute more then one instance at a time of `Server.java`.
