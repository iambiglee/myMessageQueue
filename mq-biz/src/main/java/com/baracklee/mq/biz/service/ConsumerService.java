package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterResponse;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterResponse;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.service.common.BaseService;

public interface ConsumerService extends BaseService<ConsumerEntity> {
    ConsumerRegisterResponse register(ConsumerRegisterRequest request);

    ConsumerGroupRegisterResponse registerConsumerGroup(ConsumerGroupRegisterRequest request);
}
