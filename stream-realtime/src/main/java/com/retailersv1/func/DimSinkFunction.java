package com.retailersv1.func;

import com.alibaba.fastjson.JSONObject;
import com.stream.common.utils.HbaseUtils;
import org.apache.commons.math3.analysis.function.Constant;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DimSinkFunction extends RichSinkFunction<JSONObject> {
    private Connection hbaseConnect;
    @Override
    public void open(Configuration parameters) throws Exception {
        hbaseConnect = HbaseUtils.getHbaseConnect();
    }

    @Override
    public void close() throws Exception {
        HbaseUtils.closeHBaseConn(hbaseConnect);
    }

    @Override
    public void invoke(JSONObject value, Context context) throws Exception {
        //主流数据
        JSONObject f0 = value.getJSONObject("f0");
        System.out.println(f0);
        //配置数据
        JSONObject f1 = value.getJSONObject("f1");

        String sinkTable = f1.getString("sinkTable");
        String sinkRowKey = f1.getString("sinkRowKey");
        String sinkFamily = f1.getString("sinkFamily");
        JSONObject data = f0.getJSONObject("data");

        String type = data.getString("type");
        String rowKey = data.getString(sinkRowKey);

        if ("delete".equals(type)){
            HbaseUtils.deleteTable(hbaseConnect, "gmall",sinkTable,rowKey);
        }else {
            HbaseUtils.putCells(hbaseConnect, "gmall",sinkTable,rowKey,sinkFamily,data);
        }
    }
}
