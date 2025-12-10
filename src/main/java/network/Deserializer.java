package network;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

public class Deserializer {
    final static String UID_FIELD_NAME = "serialVersionUID";
    final static long DEFAULT_UID = 0l;
    final static int INT_LENGTH = 4;
    final static int LONG_LENGTH = 8;
    final static int DOUBLE_LENGTH = 8;

    public static Object getObject(byte[] objInfo) throws Exception {
        if (objInfo.length < INT_LENGTH) {
            return null;
        }

        int idx = INT_LENGTH;

        /* find class */
        String name;
        byte[] lengthByteArray = new byte[INT_LENGTH];
        System.arraycopy(objInfo, idx, lengthByteArray, 0, INT_LENGTH); idx += INT_LENGTH;
        int length = byteArrayToInt(lengthByteArray);

        byte[] stringByteArray = new byte[length];
        System.arraycopy(objInfo, idx, stringByteArray,  0, length); idx += length;
        name = new String(stringByteArray);

        Class<?> c = Class.forName(name);
        idx = checkVersion(c, objInfo, idx);
        Object result = null;

        result = makeObject(c, objInfo, idx);

        return result;
    }


    public static int checkVersion(Class<?> c, byte[] objInfo, int idx) throws Exception {
        long destUID = DEFAULT_UID;

        try {
            Field uidField = c.getDeclaredField(UID_FIELD_NAME);
            uidField.setAccessible(true);
            destUID = (long) uidField.get(c);
        }
        catch (NoSuchFieldException e) {  }

        byte[] longByteArray = new byte[LONG_LENGTH];
        System.arraycopy(objInfo, idx, longByteArray, 0, LONG_LENGTH); idx += LONG_LENGTH;
        long srcUID = byteArrayToLong(longByteArray);

        if (destUID != srcUID) {
            throw new Exception("not match version");
        }

        return idx;
    }


    public static Object makeObject(Class<?> c, byte[] objInfo, int idx) throws Exception {
        Object result = c.getConstructor().newInstance();
        Field[] member = c.getDeclaredFields();

        Class<?> type;
        String typeStr;
        byte[] arr;

        for (int i = 0; i < member.length; i++) {
            if (!Modifier.isStatic(member[i].getModifiers())) {
                member[i].setAccessible(true);
                type = member[i].getType();

                if (objInfo[idx++] == 0) {
                    member[i].set(result, null);
                }
                else {
                    typeStr = type.toString();
                    if (typeStr.equals("int") || typeStr.contains("Integer")) {
                        arr = new byte[INT_LENGTH];
                        System.arraycopy(objInfo, idx, arr, 0, INT_LENGTH);
                        idx += INT_LENGTH;
                        member[i].set(result, byteArrayToInt(arr));
                    }
                    else if (typeStr.equals("long") || typeStr.contains("Long")) {
                        arr = new byte[LONG_LENGTH];
                        System.arraycopy(objInfo, idx, arr, 0, LONG_LENGTH);
                        idx += LONG_LENGTH;
                        member[i].set(result, byteArrayToLong(arr));
                    }
                    else if (typeStr.equals("double") || typeStr.contains("Double")) {
                        arr = new byte[DOUBLE_LENGTH];
                        System.arraycopy(objInfo, idx, arr, 0, DOUBLE_LENGTH);
                        idx += DOUBLE_LENGTH;
                        member[i].set(result, byteArrayToDouble(arr));
                    }
                    else if (typeStr.contains("String")) {
                        arr = new byte[INT_LENGTH];
                        System.arraycopy(objInfo, idx, arr, 0, INT_LENGTH);
                        idx += INT_LENGTH;
                        int length = byteArrayToInt(arr);

                        arr = new byte[length];
                        System.arraycopy(objInfo, idx, arr, 0, length);
                        idx += length;
                        member[i].set(result, new String(arr));
                    }
                    else if (typeStr.contains("LocalDateTime")) {
                        byte[] yearByteArray = new byte[INT_LENGTH];
                        byte[] monthByteArray = new byte[INT_LENGTH];
                        byte[] dayByteArray = new byte[INT_LENGTH];
                        byte[] hourByteArray = new byte[INT_LENGTH];
                        byte[] minuteByteArray = new byte[INT_LENGTH];

                        System.arraycopy(objInfo, idx, yearByteArray, 0, INT_LENGTH); idx += INT_LENGTH;
                        System.arraycopy(objInfo, idx, monthByteArray, 0, INT_LENGTH); idx += INT_LENGTH;
                        System.arraycopy(objInfo, idx, dayByteArray, 0, INT_LENGTH); idx += INT_LENGTH;
                        System.arraycopy(objInfo, idx, hourByteArray, 0, INT_LENGTH); idx += INT_LENGTH;
                        System.arraycopy(objInfo, idx, minuteByteArray, 0, INT_LENGTH); idx += INT_LENGTH;

                        int year = byteArrayToInt(yearByteArray);
                        int month = byteArrayToInt(monthByteArray);
                        int day = byteArrayToInt(dayByteArray);
                        int hour = byteArrayToInt(hourByteArray);
                        int minute = byteArrayToInt(minuteByteArray);

                        member[i].set(result, LocalDateTime.of(year, month, day, hour, minute));
                    }
                    else if (typeStr.equals("class [B") || typeStr.contains("byte[]")) {

                        arr = new byte[INT_LENGTH];
                        System.arraycopy(objInfo, idx, arr, 0, INT_LENGTH);
                        idx += INT_LENGTH;
                        int length = byteArrayToInt(arr);

                        byte[] byteArray = new byte[length];
                        System.arraycopy(objInfo, idx, byteArray, 0, length);
                        idx += length;

                        member[i].set(result, byteArray);
                    }

                    else {
                        for (Class<?> temp : type.getInterfaces()) {
                            if (temp.getName().contains("Serializable")) {
                                arr = new byte[INT_LENGTH];
                                System.arraycopy(objInfo, idx, arr, 0, INT_LENGTH);
                                int length = byteArrayToInt(arr);

                                arr = new byte[INT_LENGTH + length];
                                System.arraycopy(objInfo, idx, arr, 0, length + INT_LENGTH);
                                idx += length + INT_LENGTH;

                                member[i].set(result, getObject(arr));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }


    public static int byteArrayToInt(byte[] arr) {
        return (int)(
                (0xff & arr[0]) << 8*3 |
                        (0xff & arr[1]) << 8*2 |
                        (0xff & arr[2]) << 8*1 |
                        (0xff & arr[3]) << 8*0
        );
    }

    public static long byteArrayToLong(byte[] arr) {
        return (long)( (0xff & arr[0]) << 8*7 | (0xff & arr[1]) << 8*6 | (0xff & arr[2]) << 8*5 |
                (0xff & arr[3]) << 8*4 | (0xff & arr[4]) << 8*3 | (0xff & arr[5]) << 8*2 |
                (0xff & arr[6]) << 8 | (0xff & arr[7]));
    }

    public static double byteArrayToDouble(byte[] arr){
        return (double)( (0xff & arr[0]) << 8*7 | (0xff & arr[1]) << 8*6 | (0xff & arr[2]) << 8*5 |
                (0xff & arr[3]) << 8*4 | (0xff & arr[4]) << 8*3 | (0xff & arr[5]) << 8*2 |
                (0xff & arr[6]) << 8 | (0xff & arr[7]));
    }

 //llm 수정 권장 내용 인지
    public static LocalDateTime byteArrayToDate(byte[] arr) {
        final int LENGTH = 4;

        byte[] yearByteArray = new byte[LENGTH];
        byte[] monthByteArray = new byte[LENGTH];
        byte[] dayByteArray = new byte[LENGTH];
        byte[] hourByteArray = new byte[LENGTH];
        byte[] minuteByteArray = new byte[LENGTH];

        int pos = 0;
        System.arraycopy(yearByteArray, 0, arr, pos, LENGTH); pos += LENGTH;
        System.arraycopy(monthByteArray, 0, arr, pos, LENGTH); pos += LENGTH;
        System.arraycopy(dayByteArray, 0, arr, pos, LENGTH); pos += LENGTH;
        System.arraycopy(hourByteArray, 0, arr, pos, LENGTH); pos += LENGTH;
        System.arraycopy(minuteByteArray, 0, arr, pos, LENGTH); pos += LENGTH;

        int year = byteArrayToInt(yearByteArray);
        int month = byteArrayToInt(monthByteArray);
        int day = byteArrayToInt(dayByteArray);
        int hour = byteArrayToInt(hourByteArray);
        int minute = byteArrayToInt(minuteByteArray);

        return LocalDateTime.of(year, month, day, hour, minute);
    }
}