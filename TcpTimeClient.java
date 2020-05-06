import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

public class TcpTimeClient {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("usage: <address> <port1> <port2>...");
            System.exit(1);
        }

        try (final Selector selector = Selector.open()) {
            tryAllConnections(selector, args[0], Arrays.stream(args).skip(1).mapToInt(Integer::parseInt));
            if (selector.keys().size() == 0) {
                System.out.println("no connection can be attempted");
                System.exit(1);
            }

            boolean gotResp = false;
            while (!gotResp) {
                if (selector.select(3000) == 0) {
                    System.out.println("-");
                    continue;
                }
                final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    final SelectionKey key = it.next();
                    if (key.isConnectable()) {
                        final SocketChannel sc = (SocketChannel) key.channel();
                        if (sc.finishConnect()) {
                            System.out.println("connected to: " + sc.getRemoteAddress());
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                    if (key.isReadable() && !gotResp) {
                        final SocketChannel sc = (SocketChannel) key.channel();
                        System.out.print("winner from: " + sc.getRemoteAddress());
                        final ByteBuffer bb = ByteBuffer.allocate(512);
                        final int bytesRead = sc.read(bb);
                        if (bytesRead == -1) {
                            System.out.println(" closed prematurely");
                            continue;
                        }
                        System.out.println(" replied: " + new String(bb.array(), 0, bytesRead));
                        gotResp = true;
                        sc.close(); // just care about staff from first read
                    }
                    it.remove();
                }
            }
            closeAll(selector);
        }
    }

    private static void closeAll(Selector selector) throws IOException {
        for (SelectionKey key : selector.keys()) {
            key.channel().close();
        }
    }

    private static void tryAllConnections(Selector selector, String address, IntStream ports) {
        ports.forEach(port -> {
            try {
                final SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.connect(new InetSocketAddress(address, port));
                sc.register(selector, SelectionKey.OP_CONNECT);
            } catch (IOException e) {
                System.out.println("connecting attempt to " + address + ":" + port + " failed");
            }
        });
    }
}
