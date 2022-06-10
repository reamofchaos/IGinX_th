package cn.edu.tsinghua.iginx.influxdb.tools;

import cn.edu.tsinghua.iginx.engine.shared.data.read.Field;
import cn.edu.tsinghua.iginx.thrift.DataType;
import cn.edu.tsinghua.iginx.utils.Pair;
import com.influxdb.query.FluxColumn;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static cn.edu.tsinghua.iginx.influxdb.tools.DataTypeTransformer.fromInfluxDB;

public class SchemaTransformer {

    public static Field toField(String bucket, FluxTable table) {
        FluxRecord record = table.getRecords().get(0);
        String measurement = record.getMeasurement();
        String field = record.getField();
        List<FluxColumn> columns = table.getColumns();
        columns = columns.subList(8, columns.size());
        List<Pair<String, String>> tagKVs = new ArrayList<>();
        for (FluxColumn column : columns) {
            String tagK = column.getLabel();
            String tagV = (String) record.getValueByKey(tagK);
            tagKVs.add(new Pair<>(tagK, tagV));
        }
        tagKVs.sort(Comparator.comparing(o -> o.k));
        DataType dataType = fromInfluxDB(table.getColumns().stream().filter(x -> x.getLabel().equals("_value")).collect(Collectors.toList()).get(0).getDataType());

        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(bucket);
        pathBuilder.append('.');
        pathBuilder.append(measurement);
        pathBuilder.append('.');
        pathBuilder.append(field);
        for (Pair<String, String> tagKV: tagKVs) {
            pathBuilder.append('.');
            pathBuilder.append(tagKV.k);
            pathBuilder.append('.');
            pathBuilder.append(tagKV.v);
        }
        return new Field(pathBuilder.toString(), dataType);
    }

    public static Pair<String, String> processPatternForQuery(String pattern) { // 返回的是 bucket_name, query 的信息
        String[] parts = pattern.split("\\.");
        int index = 0;
        String bucketName = parts[index++];
        if (index >= parts.length) {
            return new Pair<>(bucketName, "true");
        }
        StringBuilder queryBuilder = new StringBuilder("(");
        boolean prefixAnd = false;
        String measurementName = parts[index++];
        if (!measurementName.equals("*")) {
            queryBuilder.append(String.format("r._measurement ==\"%s\"", measurementName));
            prefixAnd = true;
        }
        if (index < parts.length) {
            // 接着处理 field
            String fieldName = parts[index++];
            if (!fieldName.equals("*")) {
                if (prefixAnd) {
                    queryBuilder.append(" and ");
                } else {
                    prefixAnd = true;
                }
                queryBuilder.append(String.format("r._field ==\"%s\"", fieldName));
            }
            while (index + 1 < parts.length) {
                String tagK = parts[index++];
                String tagV = parts[index++];
                if (tagK.equals("*")) {
                    throw new IllegalArgumentException("tag key shouldn't be \"*\"");
                }
                if (tagV.equals("*")) {
                    continue;
                }
                if (prefixAnd) {
                    queryBuilder.append(" and ");
                } else {
                    prefixAnd = true;
                }
                queryBuilder.append(String.format("r.%s ==\"%s\"", tagK, tagV));
            }
        }
        queryBuilder.append(")");
        return new Pair<>(bucketName, queryBuilder.toString());
    }

}