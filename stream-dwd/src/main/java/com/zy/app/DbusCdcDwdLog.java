//package com.zy.app;
//import com.alibaba.fastjson.JSONObject;
//import com.alibaba.ververica.connectors.common.util.StringUtils;
//import com.retailersv1.func.ProcessSplitStream;
//import com.stream.common.utils.*;
//import lombok.SneakyThrows;
//import org.apache.flink.api.common.eventtime.WatermarkStrategy;
//import org.apache.flink.api.common.functions.RichMapFunction;
//import org.apache.flink.api.common.state.StateTtlConfig;
//import org.apache.flink.api.common.state.ValueState;
//import org.apache.flink.api.common.state.ValueStateDescriptor;
//import org.apache.flink.api.common.time.Time;
//import org.apache.flink.configuration.Configuration;
//import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
//import org.apache.flink.streaming.api.datastream.*;
//import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
//import org.apache.flink.streaming.api.functions.ProcessFunction;
//import org.apache.flink.util.Collector;
//import org.apache.flink.util.OutputTag;
//import java.util.Date;
//import java.util.HashMap;
//
///**
// * xqy
// *  2024-12-24
// */
//public class DbusCdcDwdLog {
//    private static final String kafka_topic_base_log_data = ConfigUtils.getString("REALTIME.KAFKA.LOG.TOPIC");
//    private static final String kafka_botstrap_servers = ConfigUtils.getString("kafka.bootstrap.servers");
//    private static final String kafka_err_log = ConfigUtils.getString("kafka.err.log");
//    private static final String kafka_start_log = ConfigUtils.getString("kafka.start.log");
//    private static final String kafka_display_log = ConfigUtils.getString("kafka.display.log");
//    private static final String kafka_action_log = ConfigUtils.getString("kafka.action.log");
//    private static final String kafka_dirty_topic = ConfigUtils.getString("kafka.dirty.topic");
//    private static final String kafka_page_topic = ConfigUtils.getString("kafka.page.topic");
//    private static final OutputTag<String> errTag = new OutputTag<String>("errTag") {
//    };
//    private static final OutputTag<String> startTag = new OutputTag<String>("startTag") {
//    };
//    private static final OutputTag<String> displayTag = new OutputTag<String>("displayTag") {
//    };
//    private static final OutputTag<String> actionTag = new OutputTag<String>("actionTag") {
//    };
//    private static final OutputTag<String> dirtyTag = new OutputTag<String>("dirtyTag") {
//    };
//    private static final HashMap<String, DataStream<String>> collectDsMap = new HashMap<>();
//
//    @SneakyThrows
//    public static void main(String[] args) {
//
//        CommonUtils.printCheckPropEnv(
//                //true 只执行本方法里的 flase是会往下执行
//                false,
//                kafka_topic_base_log_data,
//                kafka_botstrap_servers,
//                kafka_page_topic,
//                kafka_err_log,
//                kafka_start_log,
//                kafka_display_log,
//                kafka_action_log,
//                kafka_dirty_topic
//        );
//        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//        EnvironmentSettingUtils.defaultParameter(env);
//        //String server, String groupId, String offset
//        DataStreamSource<String> kafkaSourceDs = env.fromSource(KafkaUtils.buildKafkaSource(
//                        kafka_botstrap_servers,
//                        kafka_topic_base_log_data,
//                        new Date().toString(),
//                        OffsetsInitializer.earliest()),
//                WatermarkStrategy.noWatermarks(), "kafkaSource");
//        //5> {"common":{"ar":"8","ba":"vivo","ch":"oppo","is_new":"0","md":"vivo x90","mid":"mid_135","os":"Android 12.0","sid":"b5fb30f4-dff6-4b55-a517-01df29facd9d","uid":"203","vc":"v2.1.134"},"page":{"during_time":13010,"item":"554","item_type":"order_id","last_page_id":"order","page_id":"payment"},"ts":1731510150272}
//
////        kafkaSource.print();
//
//        //  todo 数据清洗
//        SingleOutputStreamOperator<JSONObject> processDS = kafkaSourceDs.process(new ProcessFunction<String, JSONObject>() {
//                    @Override
//                    public void processElement(String s, ProcessFunction<String, JSONObject>.Context context, Collector<JSONObject> collector) {
//                        try {
//                            collector.collect(JSONObject.parseObject(s));
//                        } catch (Exception e) {
//                            context.output(dirtyTag, s);
//                            System.err.println("Convert JsonData Error !");
//                        }
//                    }
//                }).uid("convert_json_process")
//                .name("convert_json_process");
//        //todo 不满组json的数据存入测流到kafka cw_log
//        SideOutputDataStream<String> dirtyDS = processDS.getSideOutput(dirtyTag);
//        dirtyDS.print("dirtyDS -> ");
//        dirtyDS.sinkTo(KafkaUtils.buildKafkaSink(kafka_botstrap_servers, kafka_dirty_topic))
//                .uid("sink_dirty_data_to_kafka")
//                .name("sink_dirty_data_to_kafka");
//        //添加水位线，解决数据乱序问题，并分组，使同一mid层
//        KeyedStream<JSONObject, String> keyedStream = processDS.keyBy(obj -> obj.getJSONObject("common").getString("mid"));
//        // 8> {"common":{"ar":"26","uid":"198","os":"Android 13.0","ch":"wandoujia","is_new":"0","md":"Redmi k50","mid":"mid_468","vc":"v2.1.132","ba":"Redmi","sid":"04fe741d-b1f0-4ae1-b1f2-213823a4489b"},"page":{"page_id":"payment","item":"543","during_time":16361,"item_type":"order_id","last_page_id":"order"},"ts":1731511536924}
////        keyedStream.print();
//
//        //todo 新老用户校验
//        SingleOutputStreamOperator<JSONObject> mapDs = keyedStream.map(new RichMapFunction<JSONObject, JSONObject>() {
//                    private ValueState<String> lastVisitDateState;
//
//                    @Override
//                    public void open(Configuration parameters) {
//                        ValueStateDescriptor<String> valueStateDescriptor = new ValueStateDescriptor<>("lastVisitDateState", String.class);
//                        //todo 设置水位线
//                        valueStateDescriptor.enableTimeToLive(StateTtlConfig.newBuilder(Time.seconds(10))
//                                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
//                                .build());
//                        lastVisitDateState = getRuntimeContext().getState(valueStateDescriptor);
//                    }
//
//                    @Override
//                    public JSONObject map(JSONObject jsonObject) throws Exception {
//                        String isNew = jsonObject.getJSONObject("common").getString("is_new");
//                        String lastVisitDate = lastVisitDateState.value();
//                        Long ts = jsonObject.getLong("ts");
//                        String curVisitDate = DateTimeUtils.tsToDate(ts);
//                        if ("1".equals(isNew)) {
//                            //如果is_new的值为1
//                            if (StringUtils.isEmpty(lastVisitDate)) {
//                                //如果键控状态为null，认为本次是该访客首次访问 APP，将日志中 ts 对应的日期更新到状态中，不对 is_new 字段做修改；
//                                lastVisitDateState.update(curVisitDate);
//                            } else {
//                                //如果键控状态不为null，且首次访问日期不是当日，说明访问的是老访客，将 is_new 字段置为 0；
//                                if (!lastVisitDate.equals(curVisitDate)) {
//                                    isNew = "0";
//                                    jsonObject.getJSONObject("common").put("is_new", isNew);
//                                }
//                            }
//
//                        } else {
//                            //如果 is_new 的值为 0
//                            //	如果键控状态为 null，说明访问 APP 的是老访客但本次是该访客的页面日志首次进入程序。当前端新老访客状态标记丢失时，
//                            // 日志进入程序被判定为新访客，Flink 程序就可以纠正被误判的访客状态标记，只要将状态中的日期设置为今天之前即可。本程序选择将状态更新为昨日；
//                            if (StringUtils.isEmpty(lastVisitDate)) {
//                                String yesDay = DateTimeUtils.tsToDate(ts - 24 * 60 * 60 * 1000);
//                                lastVisitDateState.update(yesDay);
//                            }
//                        }
//                        return jsonObject;
//                    }
//
//                    @Override
//                    public void close() throws Exception {
//                        super.close();
//                    }
//                }).uid("fix_isNew_map")
//                .name("fix_isNew_map");
//        // 8> {"common":{"ar":"25","uid":"38","os":"iOS 13.2.3","ch":"Appstore","is_new":0,"md":"iPhone 14 Plus","mid":"mid_400","vc":"v2.1.134","ba":"iPhone","sid":"a5d6cc53-caf4-4c82-afab-247c75f4ea68"},"page":{"from_pos_seq":0,"page_id":"good_detail","item":"26","during_time":17207,"item_type":"sku_id","last_page_id":"home","from_pos_id":9},"displays":[{"pos_seq":0,"item":"30","item_type":"sku_id","pos_id":4},{"pos_seq":1,"item":"30","item_type":"sku_id","pos_id":4}],"actions":[{"item":"26","action_id":"favor_add","item_type":"sku_id","ts":1731511881499}],"ts":1731511879499}
//        //mapDs.print();
//
//        //分流
//        SingleOutputStreamOperator<String> processTagDs = mapDs.process(new ProcessSplitStream(errTag, startTag, displayTag, actionTag))
//                .uid("flag_stream_process")
//                .name("flag_stream_process");
//
//        SideOutputDataStream<String> sideOutputErrDS = processTagDs.getSideOutput(errTag);
//        SideOutputDataStream<String> sideOutputStartDS = processTagDs.getSideOutput(startTag);
//        SideOutputDataStream<String> sideOutputDisplayTagDS = processTagDs.getSideOutput(displayTag);
//        SideOutputDataStream<String> sideOutputActionTagTagDS = processTagDs.getSideOutput(actionTag);
//
//        collectDsMap.put("errTag", sideOutputErrDS);
//        collectDsMap.put("startTag", sideOutputStartDS);
//        collectDsMap.put("displayTag", sideOutputDisplayTagDS);
//        collectDsMap.put("actionTag", sideOutputActionTagTagDS);
//        collectDsMap.put("page", processTagDs);
//        SplitDs2kafkaTopicMsg(collectDsMap);
//        env.disableOperatorChaining();
//        env.execute();
////err_log_sideOutput.print("err_log>>>>");
//// err_log>>>>:11> {"common":{"ar":"12","uid":"180","os":"Android 12.0","ch":"vivo","is_new":0,"md":"xiaomi 12 ultra ","mid":"mid_58","vc":"v2.1.132","ba":"xiaomi","sid":"3258754e-5566-4350-aae1-de64dc0a6055"},"err":{"msg":" Exception in thread \\  java.net.SocketTimeoutException\\n \\tat com.atgugu.gmall2020.mock.bean.AppError.main(AppError.java:xxxxxx)","error_code":1057},"page":{"from_pos_seq":2,"page_id":"good_detail","item":"29","during_time":13717,"item_type":"sku_id","last_page_id":"cart","from_pos_id":5},"displays":[{"pos_seq":0,"item":"8","item_type":"sku_id","pos_id":4},{"pos_seq":1,"item":"10","item_type":"sku_id","pos_id":4},{"pos_seq":2,"item":"1","item_type":"sku_id","pos_id":4},{"pos_seq":3,"item":"24","item_type":"sku_id","pos_id":4},{"pos_seq":4,"item":"35","item_type":"sku_id","pos_id":4},{"pos_seq":5,"item":"9","item_type":"sku_id","pos_id":4}],"actions":[{"item":"29","action_id":"favor_add","item_type":"sku_id","ts":1731470900880},{"item":"29","action_id":"cart_add","item_type":"sku_id","ts":1731470903880}],"ts":1731470898880}
//
////start_log_sideOutput.print("start_log>>>>");
////start_log>>>>:5> {"common":{"ar":"19","uid":"48","os":"iOS 13.3.1","ch":"Appstore","is_new":"0","md":"iPhone 13","mid":"mid_173","vc":"v2.1.134","ba":"iPhone","sid":"2ac30fb4-1cee-44e5-89b2-0d0d84ffdca4"},"start":{"entry":"icon","open_ad_skip_ms":0,"open_ad_ms":2983,"loading_time":4221,"open_ad_id":18},"ts":1731509064178}
//
//// display_log_sideOutput.print("display_log>>>>");
////display_log>>>>:7> {"pos_seq":19,"item":"15","common":{"ar":"29","uid":"59","os":"Android 13.0","ch":"xiaomi","is_new":"0","md":"Redmi k50","mid":"mid_128","vc":"v2.1.134","ba":"Redmi","sid":"f8d47490-a5c3-4d32-b661-9baf18956320"},"item_type":"sku_id","pos_id":2,"page":{"page_id":"home","refer_id":"3","during_time":8218},"ts":1731502096692}
//
//// action_log_sideOutput.print("action_log>>>>");
//        // action_log>>>>:1> {"item":"2","common":{"ar":"25","uid":"247","os":"iOS 13.2.3","ch":"Appstore","is_new":"0","md":"iPhone 14","mid":"mid_5","vc":"v2.1.134","ba":"iPhone","sid":"e3d78065-3772-4a78-92ba-782cbe668abe"},"action_id":"get_coupon","item_type":"coupon_id","page":{"from_pos_seq":0,"page_id":"good_detail","item":"29","during_time":5263,"item_type":"sku_id","last_page_id":"good_detail","from_pos_id":4},"ts":1731508211828}
//
//    }
//
//    public static void SplitDs2kafkaTopicMsg(HashMap<String, DataStream<String>> dataStreamHashMap) {
//
//        dataStreamHashMap.get("errTag").sinkTo(KafkaUtils.buildKafkaSink(kafka_botstrap_servers, kafka_err_log))
//                .uid("sk_errMsg2Kafka")
//                .name("sk_errMsg2Kafka");
//
//        dataStreamHashMap.get("startTag").sinkTo(KafkaUtils.buildKafkaSink(kafka_botstrap_servers, kafka_start_log))
//                .uid("sk_startMsg2Kafka")
//                .name("sk_startMsg2Kafka");
//
//        dataStreamHashMap.get("displayTag").sinkTo(KafkaUtils.buildKafkaSink(kafka_botstrap_servers, kafka_display_log))
//                .uid("sk_displayMsg2Kafka")
//                .name("sk_displayMsg2Kafka");
//
//        dataStreamHashMap.get("actionTag").sinkTo(KafkaUtils.buildKafkaSink(kafka_botstrap_servers, kafka_action_log))
//                .uid("sk_actionMsg2Kafka")
//                .name("sk_actionMsg2Kafka");
//
//        dataStreamHashMap.get("page").sinkTo(KafkaUtils.buildKafkaSink(kafka_botstrap_servers, kafka_page_topic))
//                .uid("sk_pageMsg2Kafka")
//                .name("sk_pageMsg2Kafka");
//    }
//}