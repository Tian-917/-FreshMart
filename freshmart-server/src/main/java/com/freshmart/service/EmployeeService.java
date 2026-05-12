/* 员工业务接口 */
package com.freshmart.service;

import com.freshmart.dto.EmployeeDTO;
import com.freshmart.dto.EmployeeLoginDTO;
import com.freshmart.dto.EmployeePageQueryDTO;
import com.freshmart.dto.PasswordEditDTO;
import com.freshmart.entity.Employee;
import com.freshmart.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    //新增员工
    void save(EmployeeDTO employeeDTO);

    /**
     * 员工分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuary(EmployeePageQueryDTO employeePageQueryDTO);


    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     * @return
     */
    void starOrStop(Integer status, Long id);


    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    Employee getById(Long id);


    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    void update(EmployeeDTO employeeDTO);

    /**
     * 修改密码：校验旧密码后更新
     * @param passwordEditDTO
     */
    void editPassword(PasswordEditDTO passwordEditDTO);
}
