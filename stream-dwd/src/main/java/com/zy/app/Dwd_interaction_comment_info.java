package com.zy.app;

import com.stream.common.utils.ConfigUtils;
import com.stream.common.utils.EnvironmentSettingUtils;
import lombok.SneakyThrows;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class Dwd_interaction_comment_info {
    private static final String topic_db = ConfigUtils.getString("kafka.topic.db");
    private static final String kafka_bootstrap_servers = ConfigUtils.getString("kafka.bootstrap.servers");

    @SneakyThrows
    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettingUtils.defaultParameter(env);


        StreamTableEnvironment tenv = StreamTableEnvironment.create(env);

        tenv.executeSql("CREATE TABLE topic_db (\n" +
                "  op string," +
                "db string," +
                "before map<String,String>," +
                "after map<String,String>," +
                "source map<String,String>," +
                "ts_ms bigint," +
                "row_time as TO_TIMESTAMP_LTZ(ts_ms * 1000,3)," +
                "WATERMARK FOR row_time AS row_time - INTERVAL '5' SECOND" +
                ") WITH (\n" +
                "  'connector' = 'kafka',\n" +
                "  'topic' = '" + topic_db + "',\n" +
                "  'properties.bootstrap.servers' = '" + kafka_bootstrap_servers + "',\n" +
                "  'properties.group.id' = '1',\n" +
                "  'scan.startup.mode' = 'earliest-offset',\n" +
                "  'format' = 'json'\n" +
                ")");
        Table table1 = tenv.sqlQuery("select * from topic_db where source['table']='comment_info'");
//        tenv.toDataStream(table1).print();
        table1.execute().print();

        env.execute("kakak");

    }
}
