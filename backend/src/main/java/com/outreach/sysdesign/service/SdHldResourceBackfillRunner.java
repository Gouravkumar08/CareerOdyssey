package com.outreach.sysdesign.service;

import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.model.SdItem;
import com.outreach.sysdesign.model.SdTopic;
import com.outreach.sysdesign.repository.SdItemRepository;
import com.outreach.sysdesign.repository.SdTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Idempotent backfill for databases seeded by {@link SdSeedRunner} before resource links existed.
 * Only touches HLD items whose {@code resourceLink} is still empty, so links the user set or
 * edited through the app are never overwritten, and a second run changes nothing. Never touches
 * {@code UserSdItemProgress}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SdHldResourceBackfillRunner implements ApplicationRunner {

    private final SdItemRepository itemRepository;
    private final SdTopicRepository topicRepository;

    @Override
    public void run(ApplicationArguments args) {
        Map<Long, String> topicNames = topicRepository.findAll().stream()
                .collect(Collectors.toMap(SdTopic::getId, SdTopic::getName));

        List<SdItem> toUpdate = new ArrayList<>();
        for (SdItem item : itemRepository.findAll()) {
            if (item.getCategory() != Category.HLD
                    || (item.getResourceLink() != null && !item.getResourceLink().isBlank())) {
                continue;
            }
            SdHldResourceCatalog.Resource r = SdHldResourceCatalog.resolve(
                    topicNames.get(item.getTopicId()), item.getSubTopic(), item.getTitle());
            if (r == null) {
                continue;
            }
            item.setResourceLink(r.link());
            item.setDescription(SdHldResourceCatalog.mergeNote(item.getDescription(), r.note()));
            toUpdate.add(item);
        }

        if (toUpdate.isEmpty()) {
            log.info("HLD resource backfill: nothing to update.");
            return;
        }
        itemRepository.saveAll(toUpdate);
        log.info("HLD resource backfill: set link + notes on {} items.", toUpdate.size());
    }
}
