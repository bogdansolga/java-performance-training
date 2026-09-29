package net.safedata.performance.training.lab.nplus1;

import net.safedata.performance.training.RunProfiles;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile(RunProfiles.N_PLUS_ONE_QUERIES)
public class StoreDataSeeder implements ApplicationRunner {

    static final int STORES = 20;
    static final int SECTIONS_PER_STORE = 5;
    static final int ITEMS_PER_SECTION = 10;

    private final StoreRepository storeRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public StoreDataSeeder(StoreRepository storeRepository, EntityManagerFactory entityManagerFactory, EntityManager entityManager,
                           PlatformTransactionManager transactionManager) {
        this.storeRepository = storeRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> {
            if (storeRepository.count() > 0) {
                return;
            }
            long sectionId = 0;
            long itemId = 0;
            for (long storeId = 1; storeId <= STORES; storeId++) {
                StoreEntity store = new StoreEntity(storeId, "Store " + storeId);
                for (int s = 0; s < SECTIONS_PER_STORE; s++) {
                    SectionEntity section = new SectionEntity(++sectionId, "Section " + sectionId, store);
                    store.getSections().add(section);
                    for (int i = 0; i < ITEMS_PER_SECTION; i++) {
                        itemId++;
                        ItemEntity item = new ItemEntity(itemId, "Item " + itemId, 1 + (itemId % 97), section);
                        section.getItems().add(item);
                    }
                }
                entityManager.persist(store);
            }
        });
        entityManagerFactory.unwrap(SessionFactory.class).getStatistics().clear();
    }
}
