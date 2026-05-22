package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.Knowledge;

import java.util.List;

public interface KnowledgeService extends IService<Knowledge> {

    List<Knowledge> listByCategory(String category);

    boolean addKnowledge(Knowledge knowledge);

    boolean updateKnowledge(Knowledge knowledge);

    boolean deleteKnowledge(Long id);

    /**
     * 异步增量同步到向量库，立即返回
     */
    void syncToVectorStoreAsync();

    /**
     * 获取同步状态信息
     */
    String getSyncStatus();
}
