package network;
import lombok.Getter;
import lombok.Setter;
import persistence.dto.DTO;

@Getter
@Setter
public class Protocol {
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
        // RESULT 타입은 데이터 없이 상태 코드만 내려온다고 가정한다.
        if (type == ProtocolType.RESULT) {
            return null;
        }

        // 요청/응답만 직렬화/역직렬화 대상
        if (arr == null || arr.length == 0) {
            return null;
        }
        if (type == ProtocolType.REQUEST || type == ProtocolType.RESPONSE) {
            return (DTO) Deserializer.getObject(arr);
        }

        // 정의되지 않은 타입은 null 처리
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
        System.arraycopy(arr, 2 + INT_LENGTH, dataArray, 0, dataLength); pos += dataLength;
        try {
            data = byteArrayToData(type, code, dataArray);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
