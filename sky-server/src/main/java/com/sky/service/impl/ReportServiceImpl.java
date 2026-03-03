package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return TurnoverReportVO
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1. 生成 begin~end 之间每一天的日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1); // 日期加一天
            dateList.add(begin);
        }

        // 2. 遍历每一天，查询当天已完成订单的金额合计
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 把 LocalDate 转为 LocalDateTime：当天 00:00:00 ~ 23:59:59
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // SELECT SUM(amount) FROM orders
            // WHERE order_time > beginTime AND order_time < endTime AND status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED); // 5=已完成
            Double turnover = orderMapper.sumByMap(map);

            // 若当天无已完成订单，sum 返回 null，记为 0.0
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        // 3. 用逗号拼接日期列表和营业额列表，构建 VO 返回
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return UserReportVO（dateList、newUserList、totalUserList 均为逗号拼接字符串）
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 1. 生成 begin~end 之间每一天的日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2. 遍历每一天，分别查询新增用户数 和 累计用户总数
        List<Integer> newUserList = new ArrayList<>();   // 每天新增
        List<Integer> totalUserList = new ArrayList<>(); // 累计总量

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 当天 00:00:00
            LocalDateTime endTime   = LocalDateTime.of(date, LocalTime.MAX); // 当天 23:59:59

            // 新增用户：create_time 在 [beginTime, endTime] 之间
            Integer newUser = getUserCount(beginTime, endTime);
            // 累计用户：create_time <= endTime（begin 传 null，表示不限开始时间）
            Integer totalUser = getUserCount(null, endTime);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        // 3. 拼接逗号字符串，构建 VO 返回
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 根据时间区间统计用户数量
     * @param beginTime 开始时间（可为 null，表示不限）
     * @param endTime   结束时间（可为 null，表示不限）
     * @return 用户数量
     */
    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        return userMapper.countByMap(map);
    }

    /**
     * 统计指定时间区间内的订单数据
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return OrderReportVO
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 1. 生成日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2. 遍历每一天，查询当天订单总数 和 有效订单数（状态=5 已完成）
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime   = LocalDateTime.of(date, LocalTime.MAX);

            // 当天所有订单（不限状态）
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            // 当天有效订单（状态=5 已完成）
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 3. 用 stream 累加区间汇总数据
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);

        // 4. 计算订单完成率（防止除以0）
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0
                : validOrderCount.doubleValue() / totalOrderCount;

        // 5. 构建 VO 返回
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据时间区间和订单状态统计订单数量
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param status    订单状态（null 表示不限）
     * @return 订单数量
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    /**
     * 查询指定时间区间内销量排名前10的商品
     * @param begin 开始日期
     * @param end   结束日期
     * @return SalesTop10ReportVO（nameList + numberList 逗号字符串）
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime   = LocalDateTime.of(end, LocalTime.MAX);

        // 一次查询拿到 Top10 列表（SQL 已排序：降序，LIMIT 10）
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        // 用 stream 分别提取 name 列 和 number 列
        List<String>  nameList   = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

}
