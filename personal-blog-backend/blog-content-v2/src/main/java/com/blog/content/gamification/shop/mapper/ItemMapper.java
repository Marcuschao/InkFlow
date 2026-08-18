package com.blog.content.gamification.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.gamification.shop.model.entity.Item;
import com.blog.content.gamification.shop.model.vo.ShopItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    @Select("""
            SELECT i.*,
                   EXISTS (
                       SELECT 1 FROM user_item ui
                       WHERE ui.item_id = i.id
                         AND ui.user_id = #{userId}
                         AND ui.status <> 'EXPIRED'
                         AND (ui.expire_time IS NULL OR ui.expire_time > NOW())
                   ) AS owned
            FROM item i
            WHERE i.status = 'ON_SALE'
            ORDER BY i.price ASC, i.id ASC
            """)
    List<ShopItemVo> selectShopItems(@Param("userId") Long userId);
}
