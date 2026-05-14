/* 门店状态接口 */
package com.freshmart.controller.admin;

import com.freshmart.result.Result;
import com.freshmart.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequestMapping("/admin/store")
@RestController("adminStoreController")
public class StoreController {

    @Autowired
    private StoreService storeService;

    /**
     * 设置门店营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        storeService.setStatus(status);
        return Result.success();
    }

    /**
     * 获取门店的营业状态
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = storeService.getStatus();
        return Result.success(status);
    }
}
