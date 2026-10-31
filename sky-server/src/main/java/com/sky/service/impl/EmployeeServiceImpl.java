package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
// import com.sky.dto.PasswordEditDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.*;
// import com.sky.exception.PasswordEditFailedException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
// import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

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
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 明文密码加密
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

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void addEmployee(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //对象属性复制
        BeanUtils.copyProperties(employeeDTO, employee);
        //设置默认值
        employee.setStatus(StatusConstant.ENABLE);//默认启用
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //设置创建人和更新人的id
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.insert(employee);

    }
    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 为紧接着执行的查询开启分页
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        // 执行查询，PageHelper 会自动处理分页和总记录数查询
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        // 封装总记录数与当前页数据
        return new PageResult(page.getTotal(), page.getResult());
    }
    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setStatus(status);
        employeeMapper.startOrStop(employee);
    }

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @Override
    public EmployeeDTO getById(Long id) {
        if(id == null)
            throw new BaseException("ID不能为空");
        // 根据id查询员工
        Employee employee = employeeMapper.getById(id);
        if(employee == null)
            throw new BaseException("员工不存在");
        // 封装员工DTO
        EmployeeDTO employeeDTO = new EmployeeDTO();
        //对象属性复制
        BeanUtils.copyProperties(employee, employeeDTO);
        return employeeDTO;
    }

    /**
     * 根据id修改员工信息
     * @param employeeDTO
     */
    @Override
    public void updateEmployee(EmployeeDTO employeeDTO) {
        if(employeeDTO == null)
            throw new BaseException("员工信息不能为空");
        if(employeeDTO.getId() == null)
            throw new BaseException("员工ID不能为空");
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        //设置更新时间和服务员id
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        //执行更新
        int rows = employeeMapper.startOrStop(employee);
        if (rows == 0) {
            throw new BaseException("更新员工信息失败");
        }

    }

    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        // 检查参数
        if (passwordEditDTO == null
                || !StringUtils.hasText(passwordEditDTO.getOldPassword())
                || !StringUtils.hasText(passwordEditDTO.getNewPassword())) {
            throw new PasswordEditFailedException("员工ID、新旧密码不能为空");
        }

        // 只能修改当前登录员工自己的密码
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null || !currentId.equals(passwordEditDTO.getEmpId())) {
            throw new PasswordEditFailedException("只能修改自己的密码");
        }

        Employee employee = employeeMapper.getById(currentId);
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 旧密码做 MD5 后，再与数据库里的密码比较
        String oldPassword = DigestUtils.md5DigestAsHex(
                passwordEditDTO.getOldPassword().getBytes(StandardCharsets.UTF_8));

        if (!oldPassword.equals(employee.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        // 新密码按项目现有登录逻辑，做 MD5 后存储
        Employee update = new Employee();
        update.setId(currentId);
        update.setPassword(DigestUtils.md5DigestAsHex(passwordEditDTO.getNewPassword().getBytes(StandardCharsets.UTF_8)));
        update.setUpdateTime(LocalDateTime.now());
        update.setUpdateUser(currentId);
        employeeMapper.startOrStop(update);
    }
}
