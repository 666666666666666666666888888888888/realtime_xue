package com.zy.app;

import com.alibaba.fastjson.JSONObject;

import com.stream.common.utils.ConfigUtils;
import com.stream.common.utils.EnvironmentSettingUtils;
import com.stream.common.utils.KafkaUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

public class DwdCartAdd {
    private static final String topic_db = ConfigUtils.getString("kafka.topic.db");
    private static final String kafka_bootstrap_servers = ConfigUtils.getString("kafka.bootstrap.servers");

//    private static final String TOPIC_DWD_TRADE_CART_ADD = ConfigUtils.getString("TOPIC_DWD_TRADE_CART_ADD");


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettingUtils.defaultParameter(env);
        DataStreamSource<String> kafka_topic_db = env.fromSource(KafkaUtils.buildKafkaSource(kafka_bootstrap_servers, topic_db, "kafka-consumer-db", OffsetsInitializer.earliest()), WatermarkStrategy.noWatermarks(), "kafka_topic_db");

//        kafka_topic_db.print();
        //加购数据
//2> {"before":{"id":4659,"consignee":"雷娟英","consignee_tel":"13873972786","total_amount":"DpIU","order_status":"1005","user_id":1287,"payment_way":"3501","delivery_address":null,"order_comment":null,"out_trade_no":"749379929825935","trade_body":"联想（Lenovo） 拯救者Y9000P 2022 16英寸游戏笔记本电脑 i7-12700H 512G RTX3060 钛晶灰等1件商品","create_time":1731624371000,"operate_time":1731625465000,"expire_time":null,"process_status":null,"tracking_no":null,"parent_order_id":null,"img_url":null,"province_id":30,"activity_reduce_amount":"Yag=","coupon_reduce_amount":"AA==","original_total_amount":"DvO8","feight_fee":null,"feight_fee_reduce":null,"refundable_time":1732229171000},"after":{"id":4659,"consignee":"雷娟英","consignee_tel":"13873972786","total_amount":"DpIU","order_status":"1006","user_id":1287,"payment_way":"3501","delivery_address":null,"order_comment":null,"out_trade_no":"749379929825935","trade_body":"联想（Lenovo） 拯救者Y9000P 2022 16英寸游戏笔记本电脑 i7-12700H 512G RTX3060 钛晶灰等1件商品","create_time":1731624371000,"operate_time":1731629070000,"expire_time":null,"process_status":null,"tracking_no":null,"parent_order_id":null,"img_url":null,"province_id":30,"activity_reduce_amount":"Yag=","coupon_reduce_amount":"AA==","original_total_amount":"DvO8","feight_fee":null,"feight_fee_reduce":null,"refundable_time":1732229171000},"source":{"version":"1.9.7.Final","connector":"mysql","name":"mysql_binlog_source","ts_ms":1735005845000,"snapshot":"false","db":"gmall","sequence":null,"table":"order_info","server_id":1,"gtid":null,"file":"mysql-bin.000008","pos":7587751,"row":0,"thread":525,"query":null},"op":"u","ts_ms":1735005845137,"transaction":null}
        SingleOutputStreamOperator<JSONObject> mapjson = kafka_topic_db.map(JSONObject::parseObject).uid("dwd_convent_json").name("dwd_convent_json");
//        mapjson.print();
//        SingleOutputStreamOperator<JSONObject> flatMap = mapjson.flatMap((FlatMapFunction<JSONObject, JSONObject>) (jsonObject, collector) -> {
//            if (jsonObject.getJSONObject("source").getString("table").equals("cart_info")) {
//                collector.collect(jsonObject);
//            }
//        });
//        flatMap.print();

        StreamTableEnvironment tenv = StreamTableEnvironment.create(env);

        tenv.executeSql("CREATE TABLE topic_db (\n" +
                "  op string," +
                "db string," +
                "before map<String,String>," +
                "after map<String,String>," +
                "source map<String,String>," +
                "ts_ms bigint," +
                "row_time as TO_TIMESTAMP_LTZ(ts_ms,3)," +
                "WATERMARK FOR row_time AS row_time - INTERVAL '5' SECOND" +
                ") WITH (\n" +
                "  'connector' = 'kafka',\n" +
                "  'topic' = '" + topic_db + "',\n" +
                "  'properties.bootstrap.servers' = '" + kafka_bootstrap_servers + "',\n" +
                "  'properties.group.id' = '1',\n" +
                "  'scan.startup.mode' = 'earliest-offset',\n" +
                "  'format' = 'json'\n" +
                ")");

        Table table1 = tenv.sqlQuery("select * from topic_db");
//        tenv.toDataStream(table1).print();

        Table table = tenv.sqlQuery("select " +
                "`after` ['id'] as id ,\n" +
                "`after` ['user_id'] as user_id ,\n" +
                "`after` ['sku_id'] as sku_id ,\n" +
                "`after` ['cart_price'] as cart_price ,\n" +
                "if(op='c',cast(after['sku_num'] as bigint),cast(after['sku_num'] as bigint)-cast(before['sku_num'] as bigint)) sku_num ,\n" +
                "`after` ['img_url'] as img_url ,\n" +
                "`after` ['sku_name'] as sku_name,\n" +
                "`after` ['is_checked'] as is_checked ,\n" +
                "`after` ['create_time'] as create_time ,\n" +
                "`after` ['operate_time'] as operate_time ,\n" +
                "`after` ['is_ordered'] as is_ordered ,\n" +
                "`after` ['order_time'] as order_time ," +
                "ts_ms as ts_ms " +
                "from topic_db " +
                "where source['table']='cart_info'   " +
                "and (op='c' or (op='u' and before['sku_num'] is not null " +
                "and cast (after['sku_num'] as bigint) > cast(before['sku_num'] as bigint)))");
        tenv.toDataStream(table).print();

//        DataStream<Row> rowDataStream = tenv.toDataStream(table);
//        SingleOutputStreamOperator<String> map = rowDataStream.map(String::valueOf);
////        map.print();
//        map.sinkTo(
//                KafkaUtils.buildKafkaSink(kafka_bootstrap_servers, TOPIC_DWD_TRADE_CART_ADD)
//        );


        env.execute();
    }
}
