package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin 开始日期
     * @param end   结束日期
     * @return TurnoverReportVO（dateList 和 turnoverList 均为逗号拼接字符串）
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的用户数据
     * @param begin 开始日期
     * @param end   结束日期
     * @return UserReportVO（dateList、newUserList、totalUserList 均为逗号拼接字符串）
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的订单数据
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 查询指定时间区间内销量排名前10的商品
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

}
