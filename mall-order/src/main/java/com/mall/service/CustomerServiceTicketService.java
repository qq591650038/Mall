package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.CustomerServiceMessage;
import com.mall.entity.CustomerServiceTicket;
import java.util.List;

public interface CustomerServiceTicketService {
    CustomerServiceTicket create(Long userId, CustomerServiceTicket ticket, String initialMessage);
    Page<CustomerServiceTicket> pageForUser(Long userId, Integer current, Integer size);
    Page<CustomerServiceTicket> pageForAdmin(Integer current, Integer size, Integer status);
    List<CustomerServiceMessage> messages(Long userId, Long ticketId, boolean admin);
    void reply(Long userId, Long ticketId, String role, String content, boolean admin);
    void close(Long adminId, Long ticketId);
}
