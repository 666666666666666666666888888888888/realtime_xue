package com.zy.app;

import com.alibaba.fastjson.JSONObject;
import com.stream.common.utils.ConfigUtils;
import com.stream.common.utils.EnvironmentSettingUtils;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.zy.functions.DwdProcessFunction;
import com.zy.utils.CdcSourceUtils;
import lombok.SneakyThrows;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.BroadcastConnectedStream;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


public class DwdBaseDb {

    @SneakyThrows
    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettingUtils.defaultParameter(env);
        //读取主流
        MySqlSource<String> mySQLMainCdcSource = CdcSourceUtils.getMySQLCdcSource(
                ConfigUtils.getString("mysql.database"),
                "",
                ConfigUtils.getString("mysql.user"),
                ConfigUtils.getString("mysql.pwd"),
                StartupOptions.initial()
        );

        SingleOutputStreamOperator<String> cdcDwdMainStream = env.fromSource(mySQLMainCdcSource, WatermarkStrategy.noWatermarks(), "dwd_base_db_main")
                .uid("dwd_json_id")
                .name("dwd_json_name");

//        cdcDwdMainStream.print();

        //读取配置表
        MySqlSource<String> mySQLDwdCdcSource = CdcSourceUtils.getMySQLCdcSource(
                ConfigUtils.getString("mysql.databases.conf"),
                "gmall2024_config.table_process_dwd",
                ConfigUtils.getString("mysql.user"),
                ConfigUtils.getString("mysql.pwd"),
                StartupOptions.initial()
        );

        SingleOutputStreamOperator<String> cdcDwdStream = env.fromSource(mySQLDwdCdcSource, WatermarkStrategy.noWatermarks(), "dwd_base_db_config")
                .uid("dwd_base_dbConfig_id")
                .name("dwd_base_dbConfig_name");

//        cdcDwdStream.print();

        SingleOutputStreamOperator<JSONObject> cdcMainStreamMap= cdcDwdMainStream.map(JSONObject::parseObject)
                .uid("db_data_convert_json")
                .name("db_data_convert_json")
                .setParallelism(1);
        SingleOutputStreamOperator<JSONObject> cdcDwdStreamMap = cdcDwdStream.map(JSONObject::parseObject)
                .uid("dim_data_convert_json")
                .name("dim_data_convert_json")
                .setParallelism(1);

//        cdcMainStreamMap.print("aaa=====>");
//        cdcDwdStreamMap.print("bbbb======>");

        SingleOutputStreamOperator<JSONObject> cdcDwdClean = cdcDwdStreamMap.map(s -> {
//                    System.out.println(s+"=====================>");
                    s.remove("source");
                    s.remove("transaction");
                    JSONObject resJson = new JSONObject();
                    if ("d" .equals(s.getString("op"))) {
                        resJson.put("before", s.getJSONObject("before"));
                    } else {
                        resJson.put("after", s.getJSONObject("after"));
                    }
                    resJson.put("op", s.getString("op"));
                    return resJson;
                })
                .uid("clean_json_column_map")
                .name("clean_json_column_map");
//        cdcDwdClean.print();


        MapStateDescriptor<String, JSONObject> mapDescriptor = new MapStateDescriptor<>("mapDescriptor", String.class, JSONObject.class);
        //创建广播流
        BroadcastStream<JSONObject> broadcastStream = cdcDwdStreamMap.broadcast(mapDescriptor);
        //主流和广播流链接
        BroadcastConnectedStream<JSONObject,JSONObject> processStream = cdcMainStreamMap.connect(broadcastStream);

        SingleOutputStreamOperator<JSONObject> process = processStream.process(new DwdProcessFunction(mapDescriptor));
        process.print("kafka");

//        //过滤字段
//        process.map(new MapFunction<JSONObject, JSONObject>() {
//            @Override
//            public JSONObject map(JSONObject jsonObject) throws Exception {
//                JSONObject data = jsonObject.getJSONObject("data");
//                String tableName = jsonObject.getString("tableName");
//                System.out.println(tableName);
//                return jsonObject;
//            }
//        }).print("laskdasksdak");


        //Sink到Kafka






        env.execute("lala");
    }


}
