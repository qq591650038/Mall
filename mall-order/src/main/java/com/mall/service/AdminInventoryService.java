package com.mall.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.InventoryLog;
import com.mall.vo.InventoryWarningVO;
import java.util.List;
public interface AdminInventoryService { Page<InventoryLog> page(Integer current,Integer size,String operation); void retry(Long id); void adjust(String type,Long productId,Long skuId,String action,Integer quantity,Long adminId); List<InventoryWarningVO> lowStock(Integer threshold); }
