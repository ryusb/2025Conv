package network;
import lombok.Getter;
import lombok.Setter;
import persistence.dto.DTO;

@Getter
@Setter
public class Protocol {
    // 💡 1 Type + 1 Code + 4 DataLength (int) = 6 bytes
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

<<<<<<< HEAD
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
=======
    private DTO byteArrayToData(byte type, byte code, byte[] arr) throws Exception {
        if (type == ProtocolType.REQUEST || type == ProtocolType.RESPONSE) {
            return (DTO) Deserializer.getObject(arr);
        }
        else if (type == ProtocolType.RESULT) {
            // RESULT 타입은 DTO가 없을 수 있음
            if (code == ProtocolCode.SUCCESS || code == ProtocolCode.FAIL) {
                return null;
            }
        }

        try {
            throw new Exception("타입과 코드가 맞지 않음");
        } catch (Exception e) {
            System.out.println(type + " " + code);
            e.printStackTrace();
        }

        return null;
    }
>>>>>>> main

    public void byteArrayToProtocol(byte[] arr) {
        final int INT_LENGTH = 4;
        type = arr[0];
        code = arr[1];

        int pos = 0;
        pos += 2; // Type, Code 스킵

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
