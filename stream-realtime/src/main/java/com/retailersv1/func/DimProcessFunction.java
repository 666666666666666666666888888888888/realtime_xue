package com.retailersv1.func;

import com.alibaba.fastjson.JSONObject;
import com.retailersv1.domain.TableProcessDim;
import com.stream.common.utils.JdbcUtils;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DimProcessFunction extends BroadcastProcessFunction<JSONObject, JSONObject, JSONObject> {

    private static final Logger LOGGER = Logger.getLogger(DimProcessFunction.class.getName());

    // 用于存储维度数据的HashMap，以某个唯一标识为键
    private final HashMap<String, JSONObject> hashMap = new HashMap<>();
    private final MapStateDescriptor<String, JSONObject> mapDescriptor;

    public DimProcessFunction(MapStateDescriptor<String, JSONObject> mapDescriptor) {
        this.mapDescriptor = mapDescriptor;
    }

    // 初始化链接存入HashMap
    @Override
    public void open(Configuration parameters) throws Exception {
        Connection mysqlConnection = null;
        try {
            // 创建链接MySql
            mysqlConnection = JdbcUtils.getMySQLConnection("jdbc:mysql://cdh03:3306", "root", "root");
            // 通过链接进行查询表中数据
            List<JSONObject> tableProcessDims = JdbcUtils.queryList(mysqlConnection, "select * from gmall2024_config.table_process_dim", JSONObject.class, true);
            // 将数据存入到hashmap中，添加空指针检查，避免因JSONObject字段为空导致异常
            for (JSONObject tableProcessDim : tableProcessDims) {
                String sourceTable = tableProcessDim.getString("sourceTable");
                if (sourceTable!= null) {
                    hashMap.put(sourceTable, tableProcessDim);
                } else {
                    LOGGER.log(Level.WARNING, "发现JSONObject中sourceTable字段为null，跳过该条数据插入");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "数据库操作出现异常", e);
            throw new Exception("数据库操作出现异常，请检查配置和数据库状态", e);
        } finally {
            // 确保连接无论如何都能关闭
            if (mysqlConnection!= null) {
                try {
                    JdbcUtils.closeMySQLConnection(mysqlConnection);
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "关闭数据库连接时出现异常", ex);
                }
            }
        }
    }

    // 处理主流
    @Override
    public void processElement(JSONObject jsonObject, ReadOnlyContext readOnlyContext, Collector<JSONObject> collector) throws Exception {
        // 取出当前表名，添加空指针检查，避免因表名字段不存在或为null导致异常
//        System.out.println(jsonObject);
        JSONObject source = jsonObject.getJSONObject("source");
        String table = source.getString("table");
//        System.out.println(table);
//        if (table == null) {
//            LOGGER.log(Level.WARNING, "输入的JSONObject中'table'字段为null，无法处理");
//            return;
//        }
        // 获取状态中
        ReadOnlyBroadcastState<String, JSONObject> broadcastState = readOnlyContext.getBroadcastState(mapDescriptor);
        // 获取状态中的table值
        JSONObject tableProcessDim = broadcastState.get(table);
        if (tableProcessDim == null) {
            tableProcessDim = hashMap.get(table);
//            if (tableProcessDim!= null) {
//                LOGGER.log(Level.INFO, "从本地HashMap中获取到维度数据，表名: " + table);
//                collector.collect(tableProcessDim);
//            } else {
//                LOGGER.log(Level.WARNING, "未从广播状态和本地HashMap中找到对应表名的维度数据，表名: " + table);
//            }
        }
        if (tableProcessDim!= null) {
            collector.collect(tableProcessDim);
        }
    }

    // 处理广播流
    @Override
    public void processBroadcastElement(JSONObject jsonObject, Context context, Collector<JSONObject> collector) throws Exception {
        BroadcastState<String, JSONObject> broadcastState = context.getBroadcastState(mapDescriptor);
        // 应该从jsonObject中获取相应信息
        String sourceTable = jsonObject.getString("sourceTable");
//        System.out.println(jsonObject);
//        System.out.println(sourceTable);
        JSONObject tableProcessDim = jsonObject;
        System.out.println(tableProcessDim);
        if (sourceTable!= null) {
            String op = jsonObject.getString("op");
            if ("d".equals(op)) {
                broadcastState.remove(sourceTable);
                hashMap.remove(sourceTable);
                LOGGER.log(Level.INFO, "根据广播流信息，删除了表名 '" + sourceTable + "' 对应的维度数据");
            } else {
                broadcastState.put(sourceTable, tableProcessDim);
                LOGGER.log(Level.INFO, "根据广播流信息，更新了表名 '" + sourceTable + "' 对应的维度数据");
            }
        } else {
            LOGGER.log(Level.WARNING, "广播流数据中'sourceTable'字段为null，无法处理");
        }
    }
}