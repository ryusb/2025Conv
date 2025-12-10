package network;

public class ProtocolType {
    // public을 추가하여 외부 패키지(server)에서도 접근 가능하도록 합니다.
    public static final byte REQUEST = 0x01;
    public static final byte RESPONSE = 0x02;
    public static final byte RESULT = 0x03;
}