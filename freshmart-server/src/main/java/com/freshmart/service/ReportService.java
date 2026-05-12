/* 报表业务接口 */
package com.freshmart.service;

import com.freshmart.vo.OrderReportVO;
import com.freshmart.vo.ProductTop10ReportVO;
import com.freshmart.vo.GmvReportVO;
import com.freshmart.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 销售额数据统计
     * @param begin
     * @param end
     * @return
     */
    GmvReportVO getGmvStatistics(LocalDate begin ,LocalDate end);

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的top10数据
     * @param begin
     * @param end
     * @return
     */
    ProductTop10ReportVO getTop10Products(LocalDate begin, LocalDate end);

    /**
     * 导出运营数据报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
