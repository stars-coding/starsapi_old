package com.stars.starsapicommon.model.staticclass;

import com.stars.starsapicommon.model.entity.*;

import java.util.HashMap;

/**
 * ID添加参数映射工具
 * 该工具类维护了一个ID到参数对象的映射，根据不同的ID可以获取相应的参数对象实例。
 *
 * @author stars
 */
public class IdAndParams {

    private HashMap<Integer, Object> map;

    /**
     * 构造一个新的IdAndParams实例。
     * 初始化映射关系，将不同的ID映射到对应的参数对象。
     */
    public IdAndParams() {
        map = new HashMap<>(64);
        map.put(1, new TestUser());
        map.put(2, new AvatarParams());
        map.put(3, new BaiduParams());
        map.put(4, new MLyParams());
        map.put(5, new MD5Params());
        map.put(6, new IKunParams());
    }

    /**
     * 根据给定的ID获取相应的参数对象实例。
     *
     * @param id 参数对象的唯一标识符
     * @return 对应ID的参数对象实例，如果ID不存在则返回null
     */
    public Object getInstance(Integer id) {
        return map.get(id);
    }
}
