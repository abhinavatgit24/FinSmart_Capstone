package com.finsmart.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.BsonType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Repairs records written before MongoConfig stored LocalDate values at UTC
 * midnight. Such legacy IST records have the distinctive 18:30:00Z timestamp.
 * It is disabled by default and only writes when both enabled and apply are true.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.legacy-date-migration.enabled", havingValue = "true")
public class LegacyTransactionDateMigration {

    private final MongoTemplate mongoTemplate;

    @Value("${app.legacy-date-migration.apply:false}")
    private boolean apply;

    @Bean
    ApplicationRunner migrateLegacyTransactionDates() {
        return args -> {
            MongoCollection<Document> transactions = mongoTemplate.getCollection("transactions");
            long candidates = 0;
            long updated = 0;

            for (Document document : transactions.find(Filters.type("date", BsonType.DATE_TIME))) {
                Date stored = document.getDate("date");
                Instant instant = stored.toInstant();
                var utc = instant.atZone(ZoneOffset.UTC);
                if (utc.getHour() != 18 || utc.getMinute() != 30 || utc.getSecond() != 0) continue;

                candidates++;
                LocalDate intendedDate = utc.toLocalDate().plusDays(1);
                Date corrected = Date.from(intendedDate.atStartOfDay(ZoneOffset.UTC).toInstant());
                if (apply) {
                    transactions.updateOne(Filters.eq("_id", document.get("_id")),
                            new Document("$set", new Document("date", corrected)));
                    updated++;
                }
            }

            if (apply) {
                log.warn("Legacy date migration completed: corrected {} transaction(s).", updated);
            } else {
                log.warn("Legacy date migration dry run: found {} candidate(s). " +
                        "Back up MongoDB, then set app.legacy-date-migration.apply=true to correct them.", candidates);
            }
        };
    }
}
