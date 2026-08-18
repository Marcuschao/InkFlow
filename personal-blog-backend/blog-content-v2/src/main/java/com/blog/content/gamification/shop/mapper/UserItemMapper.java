package com.blog.content.gamification.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.gamification.shop.model.entity.UserItem;
import com.blog.content.gamification.shop.model.vo.EquippedItemVo;
import com.blog.content.gamification.shop.model.vo.UserItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserItemMapper extends BaseMapper<UserItem> {

    @Select("""
            SELECT ui.*, i.name, i.description, i.type, i.effect_config, i.price, i.duration_days, i.icon_url
            FROM user_item ui
            INNER JOIN item i ON i.id = ui.item_id
            WHERE ui.user_id = #{userId}
            ORDER BY ui.status ASC, ui.obtain_time DESC
            """)
    List<UserItemVo> selectUserItems(@Param("userId") Long userId);

    @Select("""
            SELECT ui.id AS user_item_id, i.id AS item_id, i.name, i.type, i.effect_config, i.icon_url
            FROM user_item ui
            INNER JOIN item i ON i.id = ui.item_id
            WHERE ui.user_id = #{userId}
              AND ui.status = 'ACTIVE'
              AND (ui.expire_time IS NULL OR ui.expire_time > NOW())
            """)
    List<EquippedItemVo> selectEquippedItems(@Param("userId") Long userId);

    @Update("""
            UPDATE user_item ui
            INNER JOIN item i ON i.id = ui.item_id
            SET ui.status = 'UNUSED'
            WHERE ui.user_id = #{userId}
              AND ui.status = 'ACTIVE'
              AND i.type = #{type}
            """)
    int unequipActiveByType(@Param("userId") Long userId, @Param("type") String type);

    @Update("UPDATE user_item SET status = 'ACTIVE', used_time = NOW() WHERE id = #{id} AND user_id = #{userId} AND status = 'UNUSED' AND (expire_time IS NULL OR expire_time > NOW())")
    int equipById(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_item SET status = 'UNUSED' WHERE id = #{id} AND user_id = #{userId} AND status = 'ACTIVE'")
    int unequipById(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_item SET status = 'EXPIRED' WHERE status <> 'EXPIRED' AND expire_time IS NOT NULL AND expire_time <= NOW()")
    int markExpired();
}
