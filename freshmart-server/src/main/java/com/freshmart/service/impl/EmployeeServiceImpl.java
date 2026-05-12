/* 员工业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmart.constant.MessageConstant;
import com.freshmart.constant.PasswordConstant;
import com.freshmart.constant.StatusConstant;
import com.freshmart.dto.EmployeeDTO;
import com.freshmart.dto.EmployeeLoginDTO;
import com.freshmart.dto.EmployeePageQueryDTO;
import com.freshmart.dto.PasswordEditDTO;
import com.freshmart.entity.Employee;
import com.freshmart.exception.AccountLockedException;
import com.freshmart.exception.AccountNotFoundException;
import com.freshmart.exception.PasswordErrorException;
import com.freshmart.mapper.EmployeeMapper;
import com.freshmart.result.PageResult;
import com.freshmart.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, username));

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 对前端传过来的明文密码进行md5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    //新增员工
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        //对象属性拷贝  -- 前提二者类中的属性名必须一致
        BeanUtils.copyProperties(employeeDTO, employee);

        //设置账号的默认状态，默认正常状态1表示正常 0表示锁定
        employee.setStatus(StatusConstant.ENABLE);

        //设置密码，默认密码为123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //公共字段（创建时间、修改时间、创建人、修改人）由 MyMetaObjectHandler 自动填充
        employeeMapper.insert(employee);
    }

    //员工分页查询
    @Override
    public PageResult pageQuary(EmployeePageQueryDTO employeePageQueryDTO) {
        Page<Employee> page = new Page<>(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(employeePageQueryDTO.getName() != null && !"".equals(employeePageQueryDTO.getName()),
                        Employee::getName, employeePageQueryDTO.getName())
                .orderByDesc(Employee::getCreateTime);

        employeeMapper.selectPage(page, queryWrapper);

        long total = page.getTotal();
        List<Employee> records = page.getRecords();
        return new PageResult(total, records);
    }

    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     * @return
     */
    @Override
    public void starOrStop(Integer status, Long id) {
        Employee employee = Employee.builder().status(status).id(id).build();
        employeeMapper.updateById(employee);
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.selectById(id);
        employee.setPassword("****");
        return employee;
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        //由于mapper中update接收类型为Employee,因此得将EmployeeDTO拷贝为Employee
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        //公共字段（修改时间、修改人）由 MyMetaObjectHandler 自动填充
        employeeMapper.updateById(employee);
    }

    /**
     * 修改密码
     * @param passwordEditDTO
     */
    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        Employee employee = employeeMapper.selectById(passwordEditDTO.getEmpId());
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        String oldPasswordMd5 = DigestUtils.md5DigestAsHex(passwordEditDTO.getOldPassword().getBytes());
        if (!oldPasswordMd5.equals(employee.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        Employee update = Employee.builder()
                .id(employee.getId())
                .password(DigestUtils.md5DigestAsHex(passwordEditDTO.getNewPassword().getBytes()))
                .build();
        employeeMapper.updateById(update);
    }

}
