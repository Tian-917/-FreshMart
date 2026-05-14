/* 门店状态接口 */
package com.freshmart.controller.user;

import com.freshmart.result.Result;
import com.freshmart.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/user/store")
@RestController("userStoreController")
public class StoreController {

    @Autowired
    private StoreService storeService;

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
