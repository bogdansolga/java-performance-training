package net.safedata.performance.training.lab.retention;

import net.safedata.performance.training.RunProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile(RunProfiles.UNBOUNDED_RETENTION)
public class QuoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteService.class);

    //TODO simulate a random wait time in the quote method
    //TODO convert the SKU into an entity, save a few hundred SKUs in the DB, return them from the DB, instead of mocked
    // after each, run the tests again

    private static final int SNAPSHOT_BYTES = 8 * 1024;

    // Every quote is kept, so a disputed price can be traced back to its exact pricing input.
    private final Map<String, Quote> auditTrail = new ConcurrentHashMap<>();

    public Quote quote(String sku) {
        Quote quote = new Quote(UUID.randomUUID().toString(), sku, priceFor(sku), Instant.now(),
                new byte[SNAPSHOT_BYTES]);
        auditTrail.put(quote.id(), quote);
        LOGGER.info("Returning the quote for the SKU '{}'", sku);
        return quote;
    }

    public int auditedQuotes() {
        return auditTrail.size();
    }

    private static double priceFor(String sku) {
        return 10 + Math.floorMod(sku.hashCode(), 990);
    }
}
