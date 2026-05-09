package com.zokomart.backend.catalog.stats;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CategoryStatsMapper extends BaseMapper<CategoryStatsEntity> {

    @Update("""
            UPDATE category_stats
            SET view_count = view_count + #{delta},
                updated_at = CURRENT_TIMESTAMP
            WHERE category_id = #{categoryId}
            """)
    int incrementExisting(@Param("categoryId") String categoryId, @Param("delta") long delta);

    @Insert("""
            INSERT INTO category_stats (id, category_id, view_count, created_at, updated_at)
            VALUES (#{categoryId}, #{categoryId}, #{delta}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    int insertInitial(@Param("categoryId") String categoryId, @Param("delta") long delta);

    @Select("""
            SELECT
                c.id AS id,
                c.category_code AS categoryCode,
                COALESCE(c.code, c.category_code) AS code,
                c.name AS name,
                c.image_url AS imageUrl,
                COALESCE(cs.view_count, 0) AS viewCount
            FROM category_stats cs
            INNER JOIN categories c ON c.id = cs.category_id
            WHERE c.status = 'ACTIVE'
              AND c.deleted_at IS NULL
            ORDER BY cs.view_count DESC, c.sort_order ASC, c.name ASC, c.id ASC
            LIMIT #{limit}
            """)
    List<TopCategoryRow> selectTopActiveCategories(@Param("limit") int limit);

    @Select("""
            <script>
            SELECT
                c.id AS id,
                c.category_code AS categoryCode,
                COALESCE(c.code, c.category_code) AS code,
                c.name AS name,
                c.image_url AS imageUrl,
                COALESCE(cs.view_count, 0) AS viewCount
            FROM categories c
            LEFT JOIN category_stats cs ON cs.category_id = c.id
            WHERE c.status = 'ACTIVE'
              AND c.deleted_at IS NULL
              AND c.id IN
              <foreach collection="categoryIds" item="categoryId" open="(" separator="," close=")">
                  #{categoryId}
              </foreach>
            </script>
            """)
    List<TopCategoryRow> selectActiveCategoriesByIds(@Param("categoryIds") Collection<String> categoryIds);

    record TopCategoryRow(
            String id,
            String categoryCode,
            String code,
            String name,
            String imageUrl,
            long viewCount
    ) {
    }
}
