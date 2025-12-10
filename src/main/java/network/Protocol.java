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
        if (type == ProtocolType.REQUEST) {
            return (DTO) Deserializer.getObject(arr);
        }

        else if (type == ProtocolType.RESPONSE) {
            return (DTO) Deserializer.getObject(arr);
        }

        else if (type == ProtocolType.RESULT) {
            if (code == ProtocolCode.SUCCESS) {
                return null;
            }

            else if (code == ProtocolCode.FAIL) {
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