package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.mapper.KnowledgeMapper;
import com.zz.usercenter.model.domain.Knowledge;
import com.zz.usercenter.service.KnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements KnowledgeService {

    @Resource
    private VectorStore vectorStore;

    private static final int BATCH_SIZE = 10;

    private final AtomicBoolean syncing = new AtomicBoolean(false);

    private volatile String lastSyncResult = "";

    @Override
    public List<Knowledge> listByCategory(String category) {
        QueryWrapper<Knowledge> wrapper = new QueryWrapper<>();
        wrapper.eq("category", category).eq("isActive", 1);
        return list(wrapper);
    }

    @Override
    public boolean addKnowledge(Knowledge knowledge) {
        boolean saved = save(knowledge);
        if (saved) {
            embedAndInsert(knowledge);
        }
        return saved;
    }

    @Override
    public boolean updateKnowledge(Knowledge knowledge) {
        boolean updated = updateById(knowledge);
        if (updated) {
            vectorStore.delete(List.of(String.valueOf(knowledge.getId())));
            embedAndInsert(knowledge);
        }
        return updated;
    }

    @Override
    public boolean deleteKnowledge(Long id) {
        boolean removed = removeById(id);
        if (removed) {
            vectorStore.delete(List.of(String.valueOf(id)));
        }
        return removed;
    }

    @Override
    @Async
    public void syncToVectorStoreAsync() {
        if (!syncing.compareAndSet(false, true)) {
            lastSyncResult = "同步正在进行中，请勿重复触发";
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            doSync();
        } catch (Exception e) {
            log.error("知识库同步异常", e);
            lastSyncResult = "同步异常: " + e.getMessage();
        } finally {
            syncing.set(false);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("知识库同步完成，耗时 {}ms", elapsed);
        }
    }

    private void doSync() {
        // 增量查询：已启用，且 (syncedAt 为空 或 updateTime > syncedAt)
        QueryWrapper<Knowledge> wrapper = new QueryWrapper<>();
        wrapper.eq("isActive", 1)
                .and(w -> w.isNull("syncedAt").or().apply("updateTime > syncedAt"));

        List<Knowledge> pending = list(wrapper);

        if (pending.isEmpty()) {
            lastSyncResult = "没有需要同步的知识条目";
            log.info("增量同步：无需同步的条目");
            return;
        }

        log.info("增量同步：发现 {} 条待同步知识", pending.size());

        // 先删除这些条目的旧向量
        List<String> ids = pending.stream().map(k -> String.valueOf(k.getId())).toList();
        try {
            vectorStore.delete(ids);
        } catch (Exception e) {
            log.warn("删除旧向量异常（可能不存在），继续同步: {}", e.getMessage());
        }

        // 分批写入
        List<Document> docs = pending.stream().map(k -> {
            String text = k.getTitle() + " " + k.getContent();
            return new Document(
                    String.valueOf(k.getId()),
                    text,
                    Map.of("knowledgeId", String.valueOf(k.getId()),
                           "category", k.getCategory() != null ? k.getCategory() : "")
            );
        }).toList();

        int successCount = 0;
        Date now = new Date();

        for (int i = 0; i < docs.size(); i += BATCH_SIZE) {
            List<Document> batch = docs.subList(i, Math.min(i + BATCH_SIZE, docs.size()));
            List<Long> batchIds = pending.subList(i, Math.min(i + BATCH_SIZE, docs.size()))
                    .stream().map(Knowledge::getId).toList();

            try {
                vectorStore.add(batch);
                updateSyncedAt(batchIds, now);
                successCount += batch.size();
                log.info("同步批次 {}/{}，本批 {} 条", (i / BATCH_SIZE) + 1,
                        (docs.size() + BATCH_SIZE - 1) / BATCH_SIZE, batch.size());
            } catch (Exception e) {
                log.error("同步批次失败（第{}-{}条）: {}", i + 1, i + batch.size(), e.getMessage());
                // 单条重试
                for (int j = 0; j < batch.size(); j++) {
                    try {
                        vectorStore.add(List.of(batch.get(j)));
                        updateSyncedAt(List.of(batchIds.get(j)), now);
                        successCount++;
                    } catch (Exception ex) {
                        log.error("单条重试失败 id={}: {}", batchIds.get(j), ex.getMessage());
                    }
                }
            }
        }

        lastSyncResult = "同步完成，成功 " + successCount + "/" + pending.size() + " 条";
        log.info("增量同步完成，成功 {}/{} 条", successCount, pending.size());
    }

    private void updateSyncedAt(List<Long> ids, Date time) {
        // 批量更新 syncedAt
        ids.forEach(id -> {
            try {
                Knowledge update = new Knowledge();
                update.setId(id);
                update.setSyncedAt(time);
                updateById(update);
            } catch (Exception e) {
                log.error("更新 syncedAt 失败 id={}: {}", id, e.getMessage());
            }
        });
    }

    @Override
    public String getSyncStatus() {
        if (syncing.get()) {
            return "syncing";
        }
        return lastSyncResult.isEmpty() ? "idle" : lastSyncResult;
    }

    private void embedAndInsert(Knowledge knowledge) {
        try {
            String text = knowledge.getTitle() + " " + knowledge.getContent();
            Document doc = new Document(
                    String.valueOf(knowledge.getId()),
                    text,
                    Map.of("knowledgeId", String.valueOf(knowledge.getId()),
                           "category", knowledge.getCategory() != null ? knowledge.getCategory() : "")
            );
            vectorStore.add(List.of(doc));
            // 同步成功后更新 syncedAt
            knowledge.setSyncedAt(new Date());
            updateById(knowledge);
        } catch (Exception e) {
            log.error("知识条目 {} 入 Chroma 失败，MySQL 数据正常，可后续手动重试", knowledge.getId(), e);
        }
    }
}
