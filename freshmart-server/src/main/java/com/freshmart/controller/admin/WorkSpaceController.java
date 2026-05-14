/* 工作台接口 */
package com.freshmart.controller.admin;

import com.freshmart.result.Result;
import com.freshmart.service.WorkspaceService;
import com.freshmart.vo.BusinessDataVO;
import com.freshmart.vo.ProductOverViewVO;
import com.freshmart.vo.OrderOverViewVO;
import com.freshmart.vo.BundleOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台
 */
@RestController
@RequestMapping("/admin/workspace")
@Slf4j
@Api(tags = "工作台相关接口")
public class WorkSpaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 工作台今日数据查询
     * @return
     */
    @GetMapping("/businessData")
    @ApiOperation("工作台今日数据查询")
    public Result<BusinessDataVO> businessData(){
        BusinessDataVO businessDataVO = workspaceService.getTodayBusinessData();
        return Result.success(businessDataVO);
    }

    /**
     * 查询订单管理数据
     * @return
     */
    @GetMapping("/overviewTradeOrder")
    @ApiOperation("查询订单管理数据")
    public Result<OrderOverViewVO> orderOverView(){
        return Result.success(workspaceService.getOrderOverView());
    }

    /**
     * 查询商品总览
     * @return
     */
    @GetMapping("/overviewProductes")
    @ApiOperation("查询商品总览")
    public Result<ProductOverViewVO> productOverView(){
        return Result.success(workspaceService.getProductOverView());
    }

    /**
     * 查询组合商品总览
     * @return
     */
    @GetMapping("/overviewBundles")
    @ApiOperation("查询组合商品总览")
    public Result<BundleOverViewVO> bundleOverView(){
        return Result.success(workspaceService.getBundleOverView());
    }
}
