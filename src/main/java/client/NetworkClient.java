package client;

import network.ClientSocketHolder;
import network.Protocol;
import network.ProtocolType;

public class NetworkClient {
    public static Protocol sendRequest(byte code, Object data) {
        try {
            Protocol request = new Protocol(ProtocolType.REQUEST, code, data);
            ClientSocketHolder.os.write(request.getBytes());
            ClientSocketHolder.os.flush();

            // 응답 받기
            byte[] header = new byte[6];
            ClientSocketHolder.is.read(header);

            int len = ((header[2] & 0xff) << 24) |
                    ((header[3] & 0xff) << 16) |
                    ((header[4] & 0xff) << 8) |
                    (header[5] & 0xff);

            byte[] body = new byte[len];
            ClientSocketHolder.is.read(body);

            byte[] packet = new byte[6 + len];
            System.arraycopy(header, 0, packet, 0, 6);
            System.arraycopy(body, 0, packet, 6, len);

            return new Protocol(packet);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}