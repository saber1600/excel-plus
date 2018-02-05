package io.github.biezhi.excel.plus.utils;

import io.github.biezhi.excel.plus.Constant;
import io.github.biezhi.excel.plus.annotation.ExcelField;
import io.github.biezhi.excel.plus.annotation.ExcelSheet;
import io.github.biezhi.excel.plus.annotation.ReadField;
import io.github.biezhi.excel.plus.annotation.WriteField;
import io.github.biezhi.excel.plus.converter.Converter;
import io.github.biezhi.excel.plus.converter.EmptyConverter;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.biezhi.excel.plus.Constant.TIP_MSG;

/**
 * Excel utils
 *
 * @author biezhi
 * @date 2018/2/4
 */
public class ExcelUtils {

    private static final Map<String, List<Field>> FIELD_CACHE = new HashMap<>(8);

    public static String getSheetName(Object item) {
        ExcelSheet excelSheet = item.getClass().getAnnotation(ExcelSheet.class);
        if (null == excelSheet) {
            return Constant.DEFAULT_SHEET_NAME;
        }
        return excelSheet.value();
    }

    public static List<String> getWriteFieldNames(Class<?> type) {
        List<Field>                 fields = getAndSaveFields(type);
        List<Pair<String, Integer>> pairs  = new ArrayList<>(fields.size());

        for (Field field : fields) {
            ExcelField excelField = field.getAnnotation(ExcelField.class);
            if (null != excelField) {
                Pair<String, Integer> pair = new Pair<>();
                pair.setK(excelField.columnName());

                WriteField writeField = field.getAnnotation(WriteField.class);
                if (null != writeField && writeField.order() != Constant.DEFAULT_ORDER) {
                    pair.setV(writeField.order());
                } else {
                    if (excelField.order() != Constant.DEFAULT_ORDER) {
                        pair.setV(excelField.order());
                    } else {
                        System.err.println(String.format("[%s.%s] order config error, %s", type.getName(), field.getName(), TIP_MSG));
                    }
                }
            }
        }
        pairs.sort(Comparator.comparingInt(Pair::getV));
        return pairs.stream().map(Pair::getK).collect(Collectors.toList());
    }

    public static String getColumnValue(Object item, int order) {
        List<Field> fields = getAndSaveFields(item.getClass());
        for (Field field : fields) {
            ExcelField excelField = field.getAnnotation(ExcelField.class);
            if (null == excelField) {
                continue;
            }
            ReadField readField = field.getAnnotation(ReadField.class);
            try {
                if (null != readField && readField.order() == order) {
                    field.setAccessible(true);
                    Object value = field.get(item);
                    return asString(field, value);
                } else {
                    if (excelField.order() == order) {
                        field.setAccessible(true);
                        Object value = field.get(item);
                        return asString(field, value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    private static List<Field> getAndSaveFields(Class<?> type) {
        List<Field> fields = FIELD_CACHE.getOrDefault(type.getName(), Arrays.asList(type.getDeclaredFields()));

        FIELD_CACHE.putIfAbsent(type.getClass().getName(), fields);
        return fields;
    }

    public static <T> T newInstance(Class<T> type) {
        try {
            return type.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Field getFieldByCols(Class<?> type, int col) {
        List<Field> fields = getAndSaveFields(type);

        for (Field field : fields) {
            ExcelField excelField = field.getAnnotation(ExcelField.class);
            if (null == excelField) {
                continue;
            }
            ReadField readField = field.getAnnotation(ReadField.class);
            if (null != readField && readField.order() == col) {
                return field;
            } else {
                if (excelField.order() == col) {
                    return field;
                }
            }
        }
        return null;
    }

    public static void writeToField(Object item, int col, String value) {
        Field field = ExcelUtils.getFieldByCols(item.getClass(), col);
        if (null != field) {
            try {
                field.setAccessible(true);
                Object newValue = asObject(field, value);
                field.set(item, newValue);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static Object asObject(Field field, String value) {
        ExcelField excelField = field.getAnnotation(ExcelField.class);
        if (!excelField.convertType().equals(EmptyConverter.class)) {
            Converter converter = newInstance(excelField.convertType());
            if (null != converter) {
                return converter.read(value);
            }
        }
        if (field.getType().equals(Date.class) && !"".equals(excelField.datePattern())) {

            return value;
        }
        if (field.getType().equals(String.class)) {
            return value;
        }
        if (field.getType().equals(BigDecimal.class)) {
            return new BigDecimal(value);
        }
        if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
            return Long.valueOf(value);
        }
        if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
            return Integer.valueOf(value);
        }
        if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
            return Double.valueOf(value);
        }
        if (field.getType().equals(Float.class) || field.getType().equals(float.class)) {
            return Float.valueOf(value);
        }
        if (field.getType().equals(Short.class) || field.getType().equals(short.class)) {
            return Short.valueOf(value);
        }
        if (field.getType().equals(Byte.class) || field.getType().equals(byte.class)) {
            return Byte.valueOf(value);
        }
        if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
            return Boolean.valueOf(value);
        }
        return value;
    }

    private static String asString(Field field, Object value) {
        if (null == value) {
            return "";
        }
        ExcelField excelField = field.getAnnotation(ExcelField.class);
        if (!excelField.convertType().equals(EmptyConverter.class)) {
            Converter converter = newInstance(excelField.convertType());
            return converter.write(value);
        }
        return value.toString();
    }


    public static long getReadFieldOrders(Class<?> type) {
        return getAndSaveFields(type).stream()
                .map(field -> field.getAnnotation(ExcelField.class))
                .filter(Objects::nonNull)
                .count();
    }
}