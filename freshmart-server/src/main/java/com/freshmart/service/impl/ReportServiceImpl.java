/* 报表业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.freshmart.dto.ProductSalesDTO;
import com.freshmart.entity.TradeOrder;
import com.freshmart.entity.User;
import com.freshmart.mapper.OrderMapper;
import com.freshmart.mapper.UserMapper;
import com.freshmart.service.ReportService;
import com.freshmart.service.WorkspaceService;
import com.freshmart.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 销售额数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public GmvReportVO getGmvStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的销售额
        List<Double> gmvList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的销售额数据，状态为5(已完成)的订单合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //select sum(amount) from trade_order where order_time > beginTime and order_time < endTime and status = 5
            Double gmv = getGmv(beginTime, endTime, TradeOrder.COMPLETED);
            //如果销售额为null，说明该日期没有已完成的订单，销售额为0
            if (gmv == null) {
                gmv = 0.0;
            }
            gmvList.add(gmv);
        }

        return GmvReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .gmvList(StringUtils.join(gmvList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内、指定状态的订单销售额合计
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Double getGmv(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        List<Object> objs = orderMapper.selectObjs(
                new QueryWrapper<TradeOrder>()
                        .select("ifnull(sum(amount), 0)")
                        .gt("order_time", beginTime)
                        .lt("order_time", endTime)
                        .eq(status != null, "status", status));
        if (objs == null || objs.isEmpty() || objs.get(0) == null) {
            return 0.0;
        }
        return ((Number) objs.get(0)).doubleValue();
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的新增用户数量 select count(id) from user where create_time > ? and create_time <= ?
        List<Integer> newUserList = new ArrayList<>();
        //存放每天的总用户数量 select count(id) from user where create_time <= ?
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //总用户数量 create_time <= ?
            Integer totalUser = getTotalUserCount(endTime);

            //新增用户数量 create_time > ? and create_time <= ?
            Integer newUser = getNewUserCount(beginTime, endTime);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        //封装结果数据
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();

    }

    /**
     * 统计指定时间之前注册的用户总数
     * @param endTime
     * @return
     */
    private Integer getTotalUserCount(LocalDateTime endTime) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().le(User::getCreateTime, endTime));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 统计指定时间区间内注册的用户数
     * @param beginTime
     * @param endTime
     * @return
     */
    private Integer getNewUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .gt(User::getCreateTime, beginTime)
                        .le(User::getCreateTime, endTime));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的订单数
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天有效的订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        //遍历集合，查询每天的订单数
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天的订单总数 select count(id) from trade_order where order_time > ? and order_time < ?
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            //查询每天的有效订单数 select count(id) from trade_order where order_time > ? and order_time < ? and status = 5 (已完成)
            Integer validOrderCount = getOrderCount(beginTime, endTime, TradeOrder.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //计算时间区间内订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //计算时间区间内有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        //封装数据并返回
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据条件查询订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        LambdaQueryWrapper<TradeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(TradeOrder::getOrderTime, begin)
                .lt(TradeOrder::getOrderTime, end)
                .eq(status != null, TradeOrder::getStatus, status);
        Long count = orderMapper.selectCount(queryWrapper);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 统计指定时间区间内的top10数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public ProductTop10ReportVO getTop10Products(LocalDate begin, LocalDate end) {

        //select od.name as productName, sum(od.number) as soldCount from trade_order_item od, `trade_order` o
        //      where od.order_id = o.id and o.status = 5 and o.order_time > ? and o.order_time < ?
        //          group by od.name
        //              order by number desc
        //                  limit 0,10
        //查询销量排名top10商品
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<ProductSalesDTO> salesTop10 = orderMapper.getTop10Products(beginTime, endTime);
        //销量排名top10商品封装成字符串
        List<String> productNameList = salesTop10.stream().map(ProductSalesDTO::getProductName).collect(Collectors.toList());

        //销量排名top10商品销量封装成字符串
        List<Integer> soldCountList = salesTop10.stream().map(ProductSalesDTO::getSoldCount).collect(Collectors.toList());

        //封装返回结果数据
        return ProductTop10ReportVO
                .builder()
                .productNameList(StringUtils.join(productNameList, ","))
                .soldCountList(StringUtils.join(soldCountList, ","))
                .build();
    }

    /**
     * 导出最近 30 天的销售日报（POI 模板填充）
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1.查询数据库，获取最近 30 天的经营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        // 2.通过POI将数据写入到Excel模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/fulfillment-daily-report.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据 -- 时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获取第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getGmv());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获取第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获取某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getGmv());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }
            // 3.通过输出流将Execl文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            //关闭资源
            outputStream.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
