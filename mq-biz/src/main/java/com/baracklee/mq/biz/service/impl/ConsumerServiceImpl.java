package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.util.SoaConfig;
import com.baracklee.mq.biz.dal.meta.ConsumerRepository;
import com.baracklee.mq.biz.dto.LogDto;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterResponse;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterResponse;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.ConsumerGroupTopicService;
import com.baracklee.mq.biz.service.ConsumerService;
import com.baracklee.mq.biz.service.LogService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ConsumerServiceImpl extends AbstractBaseService<ConsumerEntity> implements ConsumerService {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Resource
    private ConsumerRepository consumerRepository;
    @Resource
    private LogService logService;
    @Resource
    private ConsumerGroupService consumerGroupService;
    @Autowired
    private ConsumerGroupTopicService consumerGroupTopicService;
    @Resource
    SoaConfig soaConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumerRegisterResponse register(ConsumerRegisterRequest request) {
        ConsumerRegisterResponse response = new ConsumerRegisterResponse();
        response.setSuc(true);
        checkVaild(request,response);
        if(!response.isSuc()){
            return response;
        }
        addRegisterLog(request);
        ConsumerEntity consumerEntity=doRegisterConsumer(request);
        response.setId(consumerEntity.getId());
        if (soaConfig.getSdkVersion().compareTo(request.getSdkVersion()) > 0) {
            response.setMsg(
                    "??????mq3???????????????????????????????????????????????????:" + soaConfig.getSdkVersion() + ",???????????????:" + request.getSdkVersion());
        }
        return response;
    }

    @Override
    public ConsumerGroupRegisterResponse registerConsumerGroup(ConsumerGroupRegisterRequest request) {
        ConsumerGroupRegisterResponse response = new ConsumerGroupRegisterResponse();
        ConsumerEntity consumerEntity=get(request.getConsumerId());
        if(null==consumerEntity){
            response.setSuc(false);
            response.setMsg("ConsumerId_" + request.getConsumerId() + "????????????");
            return response;
        }
        response.setBroadcastConsumerGroupName(new HashMap<>());
        response.setConsumerGroupNameNew(new HashMap<>());
        //??????????????????
        checkBroadcastAndSubEnv(request,response);
        doRegisterConsumerGroup(request,response,consumerEntity);
        if (!response.isSuc()){
            addRegisterConsumerGroupLog(request, response);
        }
        return response;
    }

    private void checkBroadcastAndSubEnv(ConsumerGroupRegisterRequest request,
                                         ConsumerGroupRegisterResponse response) {
        Map<String, ConsumerGroupEntity> cache = consumerGroupService.getCache();
        if(request==null){
            response.setSuc(false);
            response.setMsg("??????????????????");
            return;
        }
        if(request.getConsumerGroupNames()==null
        ||request.getConsumerGroupNames().size()==0){
            response.setSuc(false);
            response.setMsg("????????????????????????");
        }
        // ????????????????????????????????????ConcurrentModificationException ??????
        List<String> consumerGroupNames= new ArrayList<>(request.getConsumerGroupNames().keySet());
        for (String name : consumerGroupNames) {
            if(!cache.containsKey(name)){
                response.setSuc(false);
                response.setMsg("????????????" + name + "????????????");
                return;
            }
        }
        Map<Long, Map<String, ConsumerGroupTopicEntity>> gtopicMap=consumerGroupTopicService.getCache();

    }

    private ConsumerEntity doRegisterConsumer(ConsumerRegisterRequest request) {
        ConsumerEntity consumerEntity = null;
        try {
            consumerEntity = new ConsumerEntity();
            consumerEntity.setName(request.getName());
            consumerEntity.setSdkVersion(request.getSdkVersion());
            consumerEntity.setLan(request.getLan());
            consumerEntity.setIp(request.getClientIp());
            consumerEntity.setHeartTime(new Date());
            consumerRepository.register(consumerEntity);
        } catch (Exception e) {
            Map<String,Object> condition= new HashMap<>();
            condition.put(ConsumerEntity.FdName,request.getName());
            consumerEntity = consumerRepository.get(condition);
        }
        return consumerEntity;
    }

    private void addRegisterLog(ConsumerRegisterRequest request) {
        LogDto logDto= new LogDto();
        logDto.setAction("is_register");
        logDto.setConsumerName(request.getName());
        logDto.setType("3");
        logService.addBrokerLog(logDto);
    }

    private void checkVaild(ConsumerRegisterRequest request, ConsumerRegisterResponse response) {
        if (request == null) {
            response.setSuc(false);
            response.setMsg("ConsumerRegisterRequest???????????????");
            return;
        }
        if (StringUtils.isEmpty(request.getName())) {
            response.setSuc(false);
            response.setMsg("ConsumerName???????????????");
            return;
        }
        if (StringUtils.isEmpty(request.getClientIp())) {
            response.setSuc(false);
            response.setMsg("Ip???????????????");
            return;
        }
        if (StringUtils.isEmpty(request.getSdkVersion())) {
            response.setSuc(false);
            response.setMsg("SdkVersion???????????????");
        }
    }
}
