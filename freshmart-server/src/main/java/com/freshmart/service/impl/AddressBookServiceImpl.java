/* 收货地址业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.freshmart.constant.MessageConstant;
import com.freshmart.context.BaseContext;
import com.freshmart.entity.AddressBook;
import com.freshmart.exception.AddressBookBusinessException;
import com.freshmart.mapper.AddressBookMapper;
import com.freshmart.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 查询当前登录用户的所有地址信息
     *
     * @return
     */
    public List<AddressBook> list() {
        Long userId = BaseContext.getCurrentId();
        return addressBookMapper.selectList(
                new LambdaQueryWrapper<AddressBook>().eq(AddressBook::getUserId, userId));
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        return addressBookMapper.selectById(id);
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();

        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBookMapper.update(null, new LambdaUpdateWrapper<AddressBook>()
                .eq(AddressBook::getUserId, userId)
                .set(AddressBook::getIsDefault, 0));

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        AddressBook defaultAddress = new AddressBook();
        defaultAddress.setId(addressBook.getId());
        defaultAddress.setIsDefault(1);
        addressBookMapper.updateById(defaultAddress);
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    /**
     * 查询当前登录用户的默认地址
     *
     * @return
     */
    public AddressBook getDefault() {
        //SQL:select * from address_book where user_id = ? and is_default = 1
        Long userId = BaseContext.getCurrentId();
        List<AddressBook> list = addressBookMapper.selectList(
                new LambdaQueryWrapper<AddressBook>()
                        .eq(AddressBook::getUserId, userId)
                        .eq(AddressBook::getIsDefault, 1));

        if (list != null && list.size() == 1) {
            return list.get(0);
        }

        throw new AddressBookBusinessException(MessageConstant.DEFAULT_ADDRESS_NOT_FOUND);
    }
}
