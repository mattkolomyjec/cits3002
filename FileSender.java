
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender {

    public static void main(String[] args) throws IOException {
        FileSender client = new FileSender();
        SocketChannel socketChannel = client.createChannel();
        client.sendFile(socketChannel);
    }

    private void sendFile(SocketChannel socketChannel) throws IOException {
        // Sender File Location
        String filePath = "/index.html";

        // Read a File From Disk. It's Filesize Is 1KB
        Path path = Paths.get(filePath);
        FileChannel inChannel = FileChannel.open(path);

        // Allocate a ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (inChannel.read(buffer) > 0) {
            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();
        }
        socketChannel.close();
    }

    private SocketChannel createChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        SocketAddress socketAddr = new InetSocketAddress("localhost", 2041);
        socketChannel.connect(socketAddr);
        return socketChannel;
    }
}