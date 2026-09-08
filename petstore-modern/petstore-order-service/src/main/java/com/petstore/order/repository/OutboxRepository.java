package com.petstore.order.repository;

import com.petstore.order.document.OutboxDocument;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for Transactional Outbox event records.
 */
@Repository
public interface OutboxRepository extends MongoRepository<OutboxDocument, String> {

  /**
   * Retrieves pending outbox events ordered by creation timestamp for reliable sequential replay.
   *
   * @param status event status (e.g., "PENDING")
   * @param pageable pagination limit
   * @return list of matching outbox documents
   */
  List<OutboxDocument> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
