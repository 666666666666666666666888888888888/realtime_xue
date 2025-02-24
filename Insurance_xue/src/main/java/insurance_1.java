public class insurance_1 {
//    -- 计算未决赔款准备金对净资产比率和保险负债占比
//            SELECT
//    company_id,
//    business_type,
//            -- 未决赔款准备金对净资产比率
//            CASE
//    WHEN owner_equity = 0 THEN NULL
//    ELSE unpaid_claims_reserve / owner_equity
//    END AS unpaid_claims_reserve_to_equity_ratio,
//            -- 保险负债
//            (
//                    deposited_margin +
//                    prepaid_premium +
//                    insurance_security_fund +
//                    unexpired_liability_reserve +
//                    life_insurance_liability_reserve +
//                    long_term_health_insurance_liability_reserve +
//                    payable_claims +
//                    payable_policy_dividends +
//                    unpaid_claims_reserve +
//                    payable_reinsurance_accounts +
//                    policyholder_deposits_and_investment_funds +
//                    satellite_launch_insurance_fund
//            ) AS insurance_liabilities,
//    -- 负债与所有者权益合计
//    total_liabilities + owner_equity AS total_liabilities_and_equity,
//            -- 保险负债占比
//            CASE
//    WHEN (total_liabilities + owner_equity) = 0 THEN NULL
//    ELSE (
//            (
//            deposited_margin +
//                    prepaid_premium +
//                    insurance_security_fund +
//                    unexpired_liability_reserve +
//                    life_insurance_liability_reserve +
//                    long_term_health_insurance_liability_reserve +
//                    payable_claims +
//                    payable_policy_dividends +
//                    unpaid_claims_reserve +
//                    payable_reinsurance_accounts +
//                    policyholder_deposits_and_investment_funds +
//                    satellite_launch_insurance_fund
//    ) / (total_liabilities + owner_equity)
//            ) * 100
//    END AS insurance_liability_ratio
//            FROM
//    insurance_financial_data
//            WHERE
//    -- 仅筛选适用的业务类型
//    business_type IN ('财险业务', '人身险业务', '再保财险业务', '再保人身险业务');
//
//
//
//    -- 从 insurance_data 表中选取所需数据并计算保险负债占比
//            SELECT
//    business_type,
//            -- 计算保险负债
//            (
//                    deposited_margin +
//                    prepaid_premium +
//                    insurance_security_fund +
//                    unexpired_liability_reserve +
//                    life_insurance_liability_reserve +
//                    long_term_health_insurance_liability_reserve +
//                    payable_claims +
//                    payable_policy_dividends +
//                    unpaid_claims_reserve +
//                    payable_reinsurance_accounts +
//                    policyholder_deposits_and_investment_funds +
//                    satellite_launch_insurance_fund
//            ) AS insurance_liability,
//    -- 计算负债与所有者权益合计
//    total_liabilities + owner_equity AS total_liabilities_and_equity,
//            -- 计算保险负债占比，同时处理分母为零的情况
//            CASE
//    WHEN total_liabilities + owner_equity = 0 THEN NULL
//    ELSE
//            (
//                (
//                deposited_margin +
//                        prepaid_premium +
//                        insurance_security_fund +
//                        unexpired_liability_reserve +
//                        life_insurance_liability_reserve +
//                        long_term_health_insurance_liability_reserve +
//                        payable_claims +
//                        payable_policy_dividends +
//                        unpaid_claims_reserve +
//                        payable_reinsurance_accounts +
//                        policyholder_deposits_and_investment_funds +
//                        satellite_launch_insurance_fund
//            ) / (total_liabilities + owner_equity)
//            ) * 100
//    END AS insurance_liability_ratio
//            FROM
//    insurance_data
//            WHERE
//    -- 筛选出适用的业务类型
//    business_type IN ('财险业务', '人身险业务', '再保财险业务', '再保人身险业务');


//    SELECT
//            business_type,
//    -- 计算偿付能力充足率
//            CASE
//    WHEN minimum_capital = 0 THEN NULL
//    ELSE ((admitted_assets - admitted_liabilities) / minimum_capital) * 100
//    END AS solvency_ratio,
//            -- 计算实际资本变化率
//            CASE
//    WHEN beginning_actual_capital = 0 THEN NULL
//    ELSE ((ending_actual_capital - beginning_actual_capital) / beginning_actual_capital) * 100
//    END AS actual_capital_change_rate,
//            -- 计算认可资产负债率
//            CASE
//    WHEN admitted_assets = 0 THEN NULL
//    ELSE (admitted_liabilities / admitted_assets) * 100
//    END AS admitted_asset_liability_ratio
//            FROM
//    insurance_solvency_data
//            WHERE
//    business_type IN ('财险业务', '人身险业务', '再保财险业务', '再保人身险业务');
}
