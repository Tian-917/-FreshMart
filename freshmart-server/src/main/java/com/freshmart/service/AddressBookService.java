/* 收货地址业务接口 */
package com.freshmart.service;

import com.freshmart.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    /**
     * 查询当前登录用户的所有地址信息
     * @return
     */
    List<AddressBook> list();

    void save(AddressBook addressBook);

    AddressBook getById(Long id);

    void update(AddressBook addressBook);

    void setDefault(AddressBook addressBook);

    void deleteById(Long id);

    /**
     * 查询当前登录用户的默认地址
     * @return
     */
    AddressBook getDefault();

}
