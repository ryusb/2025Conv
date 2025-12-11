package network;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class Deserializer {
    final static String UID_FIELD_NAME = "serialVersionUID";
    final static long DEFAULT_UID = 0L;
    final static int INT_LENGTH = 4;
    final static int LONG_LENGTH = 8;
    final static int DOUBLE_LENGTH = 8;

    public static Object getObject(byte[] objInfo) throws Exception {
        if (objInfo == null || objInfo.length < INT_LENGTH) {
            return null;
        }

        int idx = INT_LENGTH; // 시작 오프셋 (보통 헤더 이후)

        /* 1. 클래스 이름 길이 읽기 */
        // [방어 코드] 남은 데이터가 INT_LENGTH(4바이트)보다 적으면 읽을 수 없음
        if (idx + INT_LENGTH > objInfo.length) {
            throw new Exception("[Deserializer] 데이터 부족: 클래스 이름 길이를 읽을 수 없습니다.");
        }

        byte[] lengthByteArray = new byte[INT_LENGTH];
        System.arraycopy(objInfo, idx, lengthByteArray, 0, INT_LENGTH);
        idx += INT_LENGTH;
        int length = byteArrayToInt(lengthByteArray);

        /* 2. 유효성 검사 (핵심 수정 부분) */
        // 읽어온 길이가 음수이거나, 남은 데이터보다 길다면 에러 처리
        if (length < 0 || idx + length > objInfo.length) {
            throw new Exception("[Deserializer] 잘못된 데이터 패킷: " +
                    "클래스 이름 길이(" + length + ")가 남은 데이터(" + (objInfo.length - idx) + ")보다 큽니다.");
        }

        /* 3. 클래스 이름 읽기 */
        byte[] stringByteArray = new byte[length];
        System.arraycopy(objInfo, idx, stringByteArray, 0, length);
        idx += length;
        String name = new String(stringByteArray);

        Class<?> c = Class.forName(name);
        idx = checkVersion(c, objInfo, idx);

        return makeObject(c, objInfo, idx);
    }

    public static int checkVersion(Class<?> c, byte[] objInfo, int idx) throws Exception {
        if (c.getName().startsWith("java.")) {
            return idx + LONG_LENGTH;
        }

        long destUID = DEFAULT_UID;
        try {
            Field uidField = c.getDeclaredField(UID_FIELD_NAME);
            uidField.setAccessible(true);
            destUID = (long) uidField.get(null); // static 필드는 인스턴스 대신 null 사용
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 필드가 없거나 접근 불가하면 기본값 유지
        }

        byte[] longByteArray = new byte[LONG_LENGTH];
        System.arraycopy(objInfo, idx, longByteArray, 0, LONG_LENGTH);
        idx += LONG_LENGTH;
        long srcUID = byteArrayToLong(longByteArray);

        if (destUID != srcUID) {
            throw new Exception("버전(serialVersionUID)이 일치하지 않습니다. 클래스: " + c.getName());
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
        if (c == Boolean.class) {
            byte[] arr = new byte[1];
            System.arraycopy(objInfo, idx, arr, 0, 1);
            return arr[0] != 0;
        }
        if (c == String.class) {
            byte[] lenBytes = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
            int len = byteArrayToInt(lenBytes);
            byte[] strBytes = new byte[len];
            System.arraycopy(objInfo, idx, strBytes, 0, len);
            return new String(strBytes);
        }
        if (c == byte[].class) {
            byte[] lenBytes = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
            int len = byteArrayToInt(lenBytes);

            byte[] data = new byte[len];
            System.arraycopy(objInfo, idx, data, 0, len);
            return data;
        }
        if (c == LocalTime.class) {
            byte[] buf = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int hour = byteArrayToInt(buf);
            System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int minute = byteArrayToInt(buf);
            System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int second = byteArrayToInt(buf);
            return LocalTime.of(hour, minute, second);
        }
        if (List.class.isAssignableFrom(c)) {
            List<Object> list = new ArrayList<>();
            byte[] lenBytes = new byte[INT_LENGTH];
            System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
            int size = byteArrayToInt(lenBytes);

            for (int i = 0; i < size; i++) {
                System.arraycopy(objInfo, idx, lenBytes, 0, INT_LENGTH); idx += INT_LENGTH;
                int elemLen = byteArrayToInt(lenBytes);
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

        Arrays.sort(member, Comparator.comparing(Field::getName));

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
                } else if (typeStr.equals("boolean") || typeStr.contains("Boolean")) {
                    byte[] arr = new byte[1];
                    System.arraycopy(objInfo, idx, arr, 0, 1); idx += 1;
                    member[i].set(result, arr[0] != 0);
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
                } else if (typeStr.contains("LocalTime")) {
                    byte[] buf = new byte[INT_LENGTH];
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int hour = byteArrayToInt(buf);
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int minute = byteArrayToInt(buf);
                    System.arraycopy(objInfo, idx, buf, 0, INT_LENGTH); idx += INT_LENGTH; int second = byteArrayToInt(buf);
                    member[i].set(result, LocalTime.of(hour, minute, second));
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
}