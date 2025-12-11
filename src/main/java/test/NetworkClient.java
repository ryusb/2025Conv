package test;

import network.Protocol;
import util.OutputHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class NetworkClient {
    private final String host;
    private final int port;
    private Socket socket;
    private InputStream is;
    private OutputStream os;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        is = socket.getInputStream();
        os = socket.getOutputStream();
        OutputHandler.showOut("서버 연결 성공");
    }

    public void send(Protocol p) throws IOException {
        os.write(p.getBytes());
        os.flush();
    }

    public Protocol receive() throws IOException {
        byte[] header = new byte[Protocol.HEADER_SIZE];
        int totalRead = 0;
        while (totalRead < Protocol.HEADER_SIZE) {
            int r = is.read(header, totalRead, Protocol.HEADER_SIZE - totalRead);
            if (r == -1) throw new IOException("서버 연결 끊김");
            totalRead += r;
        }

        int len = ByteBuffer.wrap(header, 2, 4).getInt();

        byte[] body = new byte[len];
        totalRead = 0;
        while (totalRead < len) {
            int r = is.read(body, totalRead, len - totalRead);
            if (r == -1) throw new IOException("서버 연결 끊김");
            totalRead += r;
        }

        byte[] packet = new byte[Protocol.HEADER_SIZE + len];
        System.arraycopy(header, 0, packet, 0, Protocol.HEADER_SIZE);
        if (len > 0) System.arraycopy(body, 0, packet, Protocol.HEADER_SIZE, len);

        return new Protocol(packet);
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e) { }
    }
}
