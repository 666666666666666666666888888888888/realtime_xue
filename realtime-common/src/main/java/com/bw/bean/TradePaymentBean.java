package com.bw.bean;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@Data
@AllArgsConstructor
public class TradePaymentBean {
    // 窗口起始时间
    String stt;
    // 窗口终止时间
    String edt;
    // 当天日期
    String curDate;
    // 支付成功独立用户数
    Long paymentSucUniqueUserCount;
    // 支付成功新用户数
    Long paymentSucNewUserCount;
    // 时间戳
    @JSONField(serialize = false)
    Long ts;
}
