package com.zy.app;

import com.stream.common.utils.ConfigUtils;
import com.stream.common.utils.KafkaUtils;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import lombok.SneakyThrows;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class TestCDCToKafka {
    @SneakyThrows
    public static void main(String[] args) {
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("cdh03")
                .port(3306)
                .databaseList("gmall") // set captured database
                .tableList("") // set captured table
                .username("root")
                .password("root")
                .startupOptions(StartupOptions.initial())
                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
                .build();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3000);
        env.setParallelism(1);
        DataStreamSource<String> source = env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");
        source.print("alala");
        source.sinkTo(KafkaUtils.buildKafkaSink(ConfigUtils.getString("kafka.bootstrap.servers"),"topic_db"));

        env.execute();
    }

}
