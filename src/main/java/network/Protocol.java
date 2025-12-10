package network;
import lombok.Getter;
import lombok.Setter;
import persistence.dto.DTO;

@Getter
@Setter
public class Protocol {
    public static final int HEADER_SIZE = 6;

    private byte type;
    private byte code;
    private int dataLength;
    private Object data;

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

    private DTO byteArrayToData(byte type, byte code, byte[] arr) throws Exception {
        if (type == ProtocolType.REQUEST) {
            return (DTO) Deserializer.getObject(arr);
        }

        else if (type == ProtocolType.RESPONSE) {
            return (DTO) Deserializer.getObject(arr);
        }

        else if (type == ProtocolType.RESULT) {
            // 0x50 ~ 0x5F 사이의 코드는 모두 Result 코드로 인정하여 null 반환
            // SUCCESS(0x50) ~ SERVER_ERROR(0x5F)
            if (code >= ProtocolCode.SUCCESS && code <= ProtocolCode.SERVER_ERROR) {
                return null;
            }
        }
        return null;
    }

    public void byteArrayToProtocol(byte[] arr) {
        final int INT_LENGTH = 4;
        type = arr[0];
        code = arr[1];

        int pos = 0;
        pos += 2;
        byte[] dataLengthByteArray = new byte[4];
        System.arraycopy(arr, pos, dataLengthByteArray, 0, INT_LENGTH); pos += 4;
        dataLength = Deserializer.byteArrayToInt(dataLengthByteArray);

        byte[] dataArray = new byte[dataLength];
        // dataLength는 arr[2]부터 arr[5]에 있으므로, data는 arr[6]부터 시작합니다.
        System.arraycopy(arr, HEADER_SIZE, dataArray, 0, dataLength); pos += dataLength;
        try {
            data = byteArrayToData(type, code, dataArray);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}