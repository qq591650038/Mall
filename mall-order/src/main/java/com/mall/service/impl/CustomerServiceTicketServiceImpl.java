package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.CustomerServiceMessage;
import com.mall.entity.CustomerServiceTicket;
import com.mall.exception.BusinessException;
import com.mall.mapper.CustomerServiceMessageMapper;
import com.mall.mapper.CustomerServiceTicketMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.RefundMapper;
import com.mall.service.CustomerServiceTicketService;
import com.mall.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerServiceTicketServiceImpl implements CustomerServiceTicketService {
    private final CustomerServiceTicketMapper tickets; private final CustomerServiceMessageMapper messages;
    private final OrderMapper orders; private final RefundMapper refunds; private final NotificationService notifications;
    public CustomerServiceTicketServiceImpl(CustomerServiceTicketMapper tickets, CustomerServiceMessageMapper messages, OrderMapper orders, RefundMapper refunds, NotificationService notifications) { this.tickets=tickets; this.messages=messages; this.orders=orders; this.refunds=refunds; this.notifications=notifications; }
    @Override @Transactional public CustomerServiceTicket create(Long userId, CustomerServiceTicket ticket, String initialMessage) {
        if (ticket.getSubject()==null || ticket.getSubject().isBlank() || initialMessage==null || initialMessage.isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单标题和内容不能为空");
        if (ticket.getOrderId()!=null) { var order=orders.selectById(ticket.getOrderId()); if(order==null || !userId.equals(order.getUserId())) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"); }
        if (ticket.getRefundId()!=null) { var refund=refunds.selectById(ticket.getRefundId()); if(refund==null || !userId.equals(refund.getUserId())) throw new BusinessException(ErrorCode.NOT_FOUND, "售后单不存在"); }
        ticket.setUserId(userId); ticket.setCategory(ticket.getCategory()==null?"ORDER":ticket.getCategory()); ticket.setStatus(0); ticket.setPriority(ticket.getPriority()==null?0:ticket.getPriority()); ticket.setCreateTime(LocalDateTime.now()); ticket.setUpdateTime(LocalDateTime.now()); tickets.insert(ticket);
        append(ticket.getId(), userId, "USER", initialMessage); return ticket;
    }
    @Override public Page<CustomerServiceTicket> pageForUser(Long userId,Integer current,Integer size) { return tickets.selectPage(new Page<>(current,size),new LambdaQueryWrapper<CustomerServiceTicket>().eq(CustomerServiceTicket::getUserId,userId).orderByDesc(CustomerServiceTicket::getUpdateTime)); }
    @Override public Page<CustomerServiceTicket> pageForAdmin(Integer current,Integer size,Integer status) { var q=new LambdaQueryWrapper<CustomerServiceTicket>(); if(status!=null)q.eq(CustomerServiceTicket::getStatus,status); return tickets.selectPage(new Page<>(current,size),q.orderByDesc(CustomerServiceTicket::getPriority).orderByDesc(CustomerServiceTicket::getUpdateTime)); }
    @Override public List<CustomerServiceMessage> messages(Long userId,Long ticketId,boolean admin) { var ticket=tickets.selectById(ticketId); if(ticket==null || (!admin && !userId.equals(ticket.getUserId()))) throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在"); return messages.selectList(new LambdaQueryWrapper<CustomerServiceMessage>().eq(CustomerServiceMessage::getTicketId,ticketId).orderByAsc(CustomerServiceMessage::getId)); }
    @Override @Transactional public void reply(Long userId,Long ticketId,String role,String content,boolean admin) { var ticket=tickets.selectById(ticketId); if(ticket==null || (!admin && !userId.equals(ticket.getUserId()))) throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在"); if(ticket.getStatus()==2) throw new BusinessException(ErrorCode.CONFLICT,"工单已关闭"); append(ticketId,userId,role,content); ticket.setStatus(admin?1:0); if(admin) ticket.setHandledBy(userId); ticket.setUpdateTime(LocalDateTime.now()); tickets.updateById(ticket); if(admin) notifications.notify(ticket.getUserId(),"SERVICE","客服回复",ticket.getSubject(),"TICKET",ticketId); }
    @Override @Transactional public void close(Long adminId,Long ticketId) { var ticket=tickets.selectById(ticketId); if(ticket==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在"); ticket.setStatus(2);ticket.setHandledBy(adminId);ticket.setCloseTime(LocalDateTime.now());ticket.setUpdateTime(LocalDateTime.now());tickets.updateById(ticket); notifications.notify(ticket.getUserId(),"SERVICE","工单已结案",ticket.getSubject(),"TICKET",ticketId); }
    private void append(Long ticketId,Long senderId,String role,String content){if(content==null||content.isBlank())throw new BusinessException(ErrorCode.BAD_REQUEST,"回复内容不能为空");var m=new CustomerServiceMessage();m.setTicketId(ticketId);m.setSenderId(senderId);m.setSenderRole(role);m.setContent(content);m.setCreateTime(LocalDateTime.now());messages.insert(m);}
}
