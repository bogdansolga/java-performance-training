package net.safedata.performance.training.lab.nplus1;

import net.safedata.performance.training.RunProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile(RunProfiles.N_PLUS_ONE_QUERIES)
public class StoreSummaryService {

    private final StoreRepository storeRepository;

    public StoreSummaryService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<StoreSummary> summaries() {
        return storeRepository.findAll().stream()
                .map(store -> {
                    long items = 0;
                    double stockValue = 0;
                    for (SectionEntity section : store.getSections()) {
                        for (ItemEntity item : section.getItems()) {
                            items++;
                            stockValue += item.getPrice();
                        }
                    }
                    return new StoreSummary(store.getName(), store.getSections().size(), items, stockValue);
                })
                .toList();
    }
}
