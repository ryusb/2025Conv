package network;

import java.io.InputStream;
import java.io.OutputStream;

public class ClientSocketHolder {
    public static InputStream is;
    public static OutputStream os;

    public static void init(InputStream input, OutputStream output) {
        is = input;
        os = output;
    }
}
