package network;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Deserializer {
    final static String UID_FIELD_NAME = "serialVersionUID";
    final static long DEFAULT_UID = 0L;
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
        // 기본 타입 처리
        if (c == Integer.class) {
            byte[] arr = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, arr, 0, INT_LENGTH);
            return byteArrayToInt(arr);
        }
        if (c == Long.class) {
            byte[] arr = new byte[LONG_LENGTH];
            System.arraycopy(objInfo, idx, arr, 0, LONG_LENGTH);
            return byteArrayToLong(arr);
        }
        if (c == Double.class) {
            byte[] arr = new byte[DOUBLE_LENGTH];
            System.arraycopy(objInfo, idx, arr, 0, DOUBLE_LENGTH);
            return byteArrayToDouble(arr);
        }
        if (c == String.class) {
            byte[] lenBytes = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
            int len = byteArrayToInt(lenBytes);
            byte[] strBytes = new byte[len];
            System.arraycopy(objInfo, idx, strBytes, 0, len);
            return new String(strBytes);
        }

        // List 복원
        if (List.class.isAssignableFrom(c)) {
            List<Object> list = new ArrayList<>();
            byte[] lenBytes = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
            int size = byteArrayToInt(lenBytes);

            for (int i = 0; i < size; i++) {
                // 요소 길이 읽기
                System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
                int elemLen = byteArrayToInt(lenBytes);
                // 요소 데이터 읽어서 객체로 복원
                byte[] elemData = new byte[elemLen];
                System.arraycopy(objInfo, idx, elemData, 0, elemLen); idx += elemLen;
                list.add(getObject(elemData));
            }
            return list;
        }

        // Map 복원
        if (Map.class.isAssignableFrom(c)) {
            Map<Object, Object> map = new HashMap<>();
            byte[] lenBytes = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
            int size = byteArrayToInt(lenBytes);

            for (int i = 0; i < size; i++) {
                // Key
                System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
                int keyLen = byteArrayToInt(lenBytes);
                byte[] keyData = new byte[keyLen];
                System.arraycopy(objInfo, idx, keyData, 0, keyLen); idx += keyLen;
                Object key = getObject(keyData);

                // Value
                System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
                int valLen = byteArrayToInt(lenBytes);
                byte[] valData = new byte[valLen];
                System.arraycopy(objInfo, idx, valData, 0, valLen); idx += valLen;
                Object val = getObject(valData);

                map.put(key, val);
            }
            return map;
        }

        Object result = c.getConstructor().newInstance();
        Field[] member = c.getDeclaredFields();

        for (int i = 0; i < member.length; i++) {
            if (!Modifier.isStatic(member[i].getModifiers())) {
                member[i].setAccessible(true);

                if (objInfo[idx++] == 0) { // Null check
                    member[i].set(result, null);
                    continue;
                }

                String typeStr = member[i].getType().toString();
                if (typeStr.equals("int") || typeStr.contains("Integer")) {
                    byte[] arr = new byte[INT_LENGTH];
                    System.arraycopy(objInfo, idx, arr, 0, INT_LENGTH); idx += INT_LENGTH;
                    member[i].set(result, byteArrayToInt(arr));
                } else if (typeStr.equals("long") || typeStr.contains("Long")) {
                    byte[] arr = new byte[LONG_LENGTH];
                    System.arraycopy(objInfo, idx, arr, 0, LONG_LENGTH); idx += LONG_LENGTH;
                    member[i].set(result, byteArrayToLong(arr));
                } else if (typeStr.equals("double") || typeStr.contains("Double")) {
                    byte[] arr = new byte[DOUBLE_LENGTH];
                    System.arraycopy(objInfo, idx, arr, 0, DOUBLE_LENGTH); idx += DOUBLE_LENGTH;
                    member[i].set(result, byteArrayToDouble(arr));
                } else if (typeStr.contains("String")) {
                    byte[] lenBytes = new byte[INT_LENGTH];
                    System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
                    int len = byteArrayToInt(lenBytes);
                    byte[] strBytes = new byte[len];
                    System.arraycopy(objInfo, idx, strBytes, 0, len); idx += len;
                    member[i].set(result, new String(strBytes));
                } else if (typeStr.contains("LocalDateTime")) {
                    byte[] buf = new byte[INT_LENGTH];
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int year = byteArrayToInt(buf);
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int month = byteArrayToInt(buf);
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int day = byteArrayToInt(buf);
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int hour = byteArrayToInt(buf);
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int minute = byteArrayToInt(buf);
                    member[i].set(result, LocalDateTime.of(year, month, day, hour, minute));
                } else {
                    // DTO 필드 복원 (재귀)
                    byte[] lenBytes = new byte[INT_LENGTH];
                    System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
                    int len = byteArrayToInt(lenBytes);
                    byte[] objData = new byte[len];
                    System.arraycopy(objInfo, idx, objData, 0, len); idx += len;
                    member[i].set(result, getObject(objData));
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