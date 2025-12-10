package network;
import lombok.Getter;
import lombok.Setter;
import persistence.dto.DTO;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;

@Getter
@Setter
public class Protocol {
    public static final int HEADER_SIZE = 6;

    private byte type;
    private byte code;
    private int dataLength;
    private Object data;

    public Protocol(byte type, byte code, Object data) {
        this.type = type;
        this.code = code;
        this.data = data;

        // 데이터가 있으면 미리 직렬화하여 길이를 계산해 둡니다.
        if (data != null) {
            try {
                // Serializer를 이용해 실제 바이트 길이를 구함
                byte[] bytes = Serializer.getBytes(data);
                this.dataLength = bytes.length;
            } catch (Exception e) {
                e.printStackTrace();
                this.dataLength = 0;
            }
        } else {
            this.dataLength = 0;
        }
    }

    public Protocol(byte t, byte c, int dL, Object d) {
        type = t;
        code = c;
        dataLength = dL;
        data = d;
    }

    public Protocol(byte[] arr) {
        byteArrayToProtocol(arr);
    }

    public byte[] getBytes() {
        byte[] dataByteArray = new byte[0];
        if (data != null) {
            try {
                // Serializer는 외부 구현에 의존
                dataByteArray = Serializer.getBytes(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dataLength = dataByteArray.length;
        byte[] typeAndCodeByteArray = Serializer.bitsToByteArray(type, code);
        byte[] dataLengthByteArray = Serializer.intToByteArray(dataLength);

        int resultArrayLength = typeAndCodeByteArray.length + dataLengthByteArray.length + dataByteArray.length;
        byte[] resultArray = new byte[resultArrayLength];

        int pos = 0;
        System.arraycopy(typeAndCodeByteArray, 0, resultArray, pos, typeAndCodeByteArray.length); pos += typeAndCodeByteArray.length;
        System.arraycopy(dataLengthByteArray, 0, resultArray, pos, dataLengthByteArray.length); pos += dataLengthByteArray.length;
        System.arraycopy(dataByteArray, 0, resultArray, pos, dataByteArray.length); pos += dataByteArray.length;

        return resultArray;
    }

    private Object byteArrayToData(byte type, byte code, byte[] arr) throws Exception {
        if (type == ProtocolType.REQUEST || type == ProtocolType.RESPONSE) {
            return Deserializer.getObject(arr);
        }
        else if (type == ProtocolType.RESULT) {
            // 결과 코드 범위 체크 (성공~서버에러)
            if (code >= ProtocolCode.SUCCESS && code <= ProtocolCode.SERVER_ERROR) {
                return null;
            }
        }
        try {
            String hexCode = Integer.toHexString(code & 0xFF).toUpperCase();
            throw new Exception("타입과 코드가 맞지 않음. Type: " + type + ", Code: 0x" + hexCode);
        } catch (Exception e) {
            System.out.println("Error Type: " + type + ", Code: 0x" + Integer.toHexString(code & 0xFF).toUpperCase());
            e.printStackTrace();
        }
        return null;
    }

    public void byteArrayToProtocol(byte[] bytes) {
        this.type = bytes[0];
        this.code = bytes[1];
        this.dataLength = java.nio.ByteBuffer.wrap(bytes, 2, 4).getInt();

        if (dataLength > 0) {
            byte[] payload = new byte[dataLength];
            System.arraycopy(bytes, HEADER_SIZE, payload, 0, dataLength);

            try {
                this.data = byteArrayToData(type, code, payload);
            } catch (Exception e) {
                System.out.println("데이터 역직렬화 실패: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            this.data = null;
        }
    }
}
