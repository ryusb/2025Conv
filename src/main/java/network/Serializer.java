package network;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Serializer {
    final static String UID_FIELD_NAME = "serialVersionUID";
    final static long DEFAULT_UID = 0L;

    public static byte[] getBytes(Object obj) throws Exception{

        if (obj == null)
            throw new IllegalArgumentException("Object cannot be null");

        Class<?> c = obj.getClass();
        ArrayList<Byte> result = new ArrayList<>();
        byte[] head = makeHeader(obj);
        byte[] body = makeBody(obj);

        addArrList(result, intToByteArray(head.length + body.length));
        addArrList(result, head);
        addArrList(result, body);
        return byteListToArray(result);
    }

    public static byte[] makeHeader(Object obj) throws Exception {
        Class<?> c = obj.getClass();
        String cName = c.getName();
        long uid = DEFAULT_UID; // 기본값 사용

        ArrayList<Byte> result = new ArrayList<>();

        try {
            // 사용자 정의 클래스에 대해 serialVersionUID 확인
            if (!c.getName().startsWith("java.")) {
                Field uidField = c.getDeclaredField(UID_FIELD_NAME);
                uidField.setAccessible(true);
                uid = (Long) uidField.get(null); // 정적 필드 접근
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // 기본값 유지
        }

        addArrList(result, stringToByteArray(cName));
        addArrList(result, longToByteArray(uid));
        return byteListToArray(result);
    }


    public static byte[] makeBody(Object obj) throws Exception {
        // List 처리 (필드가 아니라 객체 자체가 List일 때)
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            ArrayList<Byte> result = new ArrayList<>();
            addArrList(result, intToByteArray(list.size()));
            for (Object element : list) {
                byte[] elementBytes = getBytes(element);
                addArrList(result, intToByteArray(elementBytes.length));
                addArrList(result, elementBytes);
            }
            return byteListToArray(result);
        }

        // Map 처리
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            ArrayList<Byte> result = new ArrayList<>();
            addArrList(result, intToByteArray(map.size()));
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                byte[] keyBytes = getBytes(entry.getKey());
                addArrList(result, intToByteArray(keyBytes.length));
                addArrList(result, keyBytes);
                byte[] valBytes = getBytes(entry.getValue());
                addArrList(result, intToByteArray(valBytes.length));
                addArrList(result, valBytes);
            }
            return byteListToArray(result);
        }

        // 기본 타입 처리
        if (obj instanceof Integer) {
            return intToByteArray((Integer) obj);
        }
        if (obj instanceof Long) {
            return longToByteArray((Long) obj);
        }
        if (obj instanceof Double) {
            return doubleToByteArray((Double) obj);
        }
        if (obj instanceof Boolean) {
            return new byte[] { (byte)((Boolean)obj ? 1 : 0) };
        }
        if (obj instanceof String) {
            return stringToByteArray((String) obj);
        }
        if (obj instanceof LocalDate) {
            return localDateToByteArray((LocalDate) obj);
        }
        if (obj instanceof LocalDateTime) {
            return dateToByteArray((LocalDateTime) obj);
        }

        Class<?> c = obj.getClass();
        Field[] member = c.getDeclaredFields();
        ArrayList<Byte> result = new ArrayList<>();
        byte[] arr = new byte[0];

        for (Field field : member) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Object memberVal = field.get(obj);

                if (memberVal == null) {
                    addArrList(result, new byte[]{0});
                } else {
                    addArrList(result, new byte[]{1});
                    String typeStr = field.getType().toString();

                    if (typeStr.equals("int") || typeStr.contains("Integer")) {
                        arr = intToByteArray((int) memberVal);
                    } else if (typeStr.equals("long") || typeStr.contains("Long")) {
                        arr = longToByteArray((long) memberVal);
                    } else if (typeStr.equals("double") || typeStr.contains("Double")) {
                        arr = doubleToByteArray((double) memberVal);
                    } else if (typeStr.equals("boolean") || typeStr.contains("Boolean")) {
                        arr = new byte[] { (byte)((Boolean)memberVal ? 1 : 0) };
                    } else if (typeStr.contains("String")) {
                        arr = stringToByteArray((String) memberVal);
                    } else if (typeStr.contains("LocalDateTime")) {
                        arr = dateToByteArray((LocalDateTime) memberVal);
                    } else {
                        // DTO 내부의 객체 필드 (재귀 직렬화)
                        arr = getBytes(memberVal);
                        byte[] len = intToByteArray(arr.length);
                        addArrList(result, len);
                    }
                    addArrList(result, arr);
                }
            }
        }
        return byteListToArray(result);
    }

    public static byte[] intToByteArray(int val) {
        return new byte[] {
                (byte)((val >> 8*3) & 0xff),
                (byte)((val >> 8*2) & 0xff),
                (byte)((val >> 8) & 0xff),
                (byte)((val) & 0xff)
        };
    }

    public static byte[] longToByteArray(long val) {
        return new byte[] {
                (byte)((val >> 8*7) & 0xff),
                (byte)((val >> 8*6) & 0xff),
                (byte)((val >> 8*5) & 0xff),
                (byte)((val >> 8*4) & 0xff),
                (byte)((val >> 8*3) & 0xff),
                (byte)((val >> 8*2) & 0xff),
                (byte)((val >> 8) & 0xff),
                (byte)((val) & 0xff)
        };
    }

    public static byte[] doubleToByteArray(double val) {
        long bits = Double.doubleToLongBits(val); // double을 long으로 변환
        return new byte[] {
                (byte)((bits >> 8*7) & 0xff),
                (byte)((bits >> 8*6) & 0xff),
                (byte)((bits >> 8*5) & 0xff),
                (byte)((bits >> 8*4) & 0xff),
                (byte)((bits >> 8*3) & 0xff),
                (byte)((bits >> 8*2) & 0xff),
                (byte)((bits >> 8) & 0xff),
                (byte)(bits & 0xff)
        };
    }

    public static byte[] stringToByteArray(String str) {
        ArrayList<Byte> result = new ArrayList<>();
        byte[] arr = str.getBytes();

        int length = arr.length;
        byte[] lengthByteArray = Serializer.intToByteArray(length);

        addArrList(result, lengthByteArray);
        addArrList(result, arr);
        return byteListToArray(result);
    }

    public static byte[] localDateToByteArray(LocalDate val) {
        byte[] year = intToByteArray(val.getYear());
        byte[] month = intToByteArray(val.getMonthValue());
        byte[] day = intToByteArray(val.getDayOfMonth());

        byte[] result = new byte[12];
        System.arraycopy(year, 0, result, 0, 4);
        System.arraycopy(month, 0, result, 4, 4);
        System.arraycopy(day, 0, result, 8, 4);
        return result;
    }

    public static byte[] dateToByteArray(LocalDateTime val) {
        byte[] yearByteArray = intToByteArray(val.getYear());
        byte[] monthByteArray = intToByteArray(val.getMonth().getValue());
        byte[] dayByteArray = intToByteArray(val.getDayOfMonth());
        byte[] hourByteArray = intToByteArray(val.getHour());
        byte[] minuteByteArray = intToByteArray(val.getMinute());

        int resultArrayLength = yearByteArray.length + monthByteArray.length + dayByteArray.length + hourByteArray.length + minuteByteArray.length;
        byte[] resultArray = new byte[resultArrayLength];

        int pos = 0;
        System.arraycopy(yearByteArray, 0, resultArray, pos, yearByteArray.length);
        pos += yearByteArray.length;
        System.arraycopy(monthByteArray, 0, resultArray, pos, monthByteArray.length);
        pos += monthByteArray.length;
        System.arraycopy(dayByteArray, 0, resultArray, pos, dayByteArray.length);
        pos += dayByteArray.length;
        System.arraycopy(hourByteArray, 0, resultArray, pos, hourByteArray.length);
        pos += hourByteArray.length;
        System.arraycopy(minuteByteArray, 0, resultArray, pos, minuteByteArray.length);
        pos += minuteByteArray.length;

        return resultArray;
    }

    public static byte[] bitsToByteArray(byte val1, byte val2) {
        return new byte[] { val1, val2 };
    }

    /* util */
    public static void addArrList(ArrayList<Byte> result, byte[] arr) {
        for (byte b : arr) {
            result.add(b);
        }
    }

    public static byte[] byteListToArray(ArrayList<Byte> byteList) {
        byte[] returnArray = new byte[byteList.size()];
        for(int i = 0; i < byteList.size(); i++) {
            returnArray[i] = byteList.get(i);
        }

        return returnArray;
    }
}