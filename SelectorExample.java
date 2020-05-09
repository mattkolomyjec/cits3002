import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Scanner;

import java.util.*;
import java.nio.*;
import java.time.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.net.SocketAddress;

import javax.xml.validation.Schema;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.Buffer;

public class SelectorExample {
    public static void main(String[] args) throws IOException {
        // Get selector
        Selector selector = Selector.open();

        System.out.println("Selector open: " + selector.isOpen());

        // Get server socket channel and register with selector
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        // serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5454);
        serverSocket.socket().bind(hostAddress, 0);
        serverSocket.configureBlocking(false);
        int ops = serverSocket.validOps();
        SelectionKey selectKy = serverSocket.register(selector, ops, null);

        for (;;) {

            System.out.println("Waiting for select...");

            int noOfKeys = selector.select();

            Set selectedKeys = selector.selectedKeys();
            Iterator iter = selectedKeys.iterator();

            while (iter.hasNext()) {

                SelectionKey ky = (SelectionKey) iter.next();

                if (ky.isAcceptable()) {
                    // Accept the new client connection
                    SocketChannel client = serverSocket.accept();
                    client.configureBlocking(false);

                    // Add the new connection to the selector
                    client.register(selector, SelectionKey.OP_READ);

                    System.out.println("[" + new java.sql.Timestamp(System.currentTimeMillis()) + "]"
                            + "Accepted new connection from client: " + client);

                } else if (ky.isReadable()) {
                    // Read the data from client

                    SocketChannel client = (SocketChannel) ky.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(2048);
                    int i = client.read(buffer);
                    String output = new String(buffer.array());
                    System.out.println("Message read from client: " + output);
                    output = output.trim();
                    output = output + "\r\n";
                    String message = "<html> <body> <h1>Hello from nc!</h1></body></html>";
                    output = output + message;
                    // *********write message here******
                    byte[] a = output.getBytes();
                    ByteBuffer bb = ByteBuffer.wrap(a);
                    client.write(bb);

                    buffer.clear();
                    System.out.println("   Sent    Message ");
                    if (i == -1) {
                        client.close();
                        System.out.println("Client messages are complete; close.");
                    }

                } // end if (ky...)
                  // countkey++;
                iter.remove();
            } // end while loop

        } // end for loop
    }
}