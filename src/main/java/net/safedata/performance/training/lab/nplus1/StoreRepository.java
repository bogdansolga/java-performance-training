package net.safedata.performance.training.lab.nplus1;

import net.safedata.performance.training.RunProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile(RunProfiles.N_PLUS_ONE_QUERIES)
public interface StoreRepository extends JpaRepository<StoreEntity, Long> {
}
