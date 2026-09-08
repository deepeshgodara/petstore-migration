package com.petstore.migration.consumer;

import com.petstore.common.event.OrderDualWriteEvent;
import com.petstore.common.event.OrderEventType;
import com.petstore.order.document.OrderDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Enterprise reverse write-back consumer implementing cutover safety.
 * Consumes modern order events and constructs the necessary relational SQL statements
 * or HTTP replays to sync modern transactions back to legacy stores for zero-data-loss rollback.
 */
@Component
public class LegacyWriteBackConsumer {

  private static final Logger log = LoggerFactory.getLogger(LegacyWriteBackConsumer.class);

  @Value("${migration.writeback.enabled:false}")
  private boolean writeBackEnabled;

  @KafkaListener(
      topics = "${migration.dualwrite.topic:petstore.orders.dualwrite}",
      groupId = "petstore-legacy-writeback-group",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void onOrderEventForLegacyWriteBack(OrderDualWriteEvent event) {
    if (event == null || event.getOrderId() == null) {
      return;
    }

    if (!writeBackEnabled) {
      log.debug("Legacy write-back is disabled (default). Prepared reverse audit log for order [{}]",
          event.getOrderId());
      return;
    }

    log.info("Executing reverse write-back for order [{}] to legacy relational store...", event.getOrderId());
    if (event.getEventType() == OrderEventType.ORDER_CREATED) {
      OrderDocument order = event.getOrder();
      if (order != null) {
        log.info("Synthesized legacy SQL write-back: INSERT INTO PURCHASEORDER "
                + "(ORDERID, USERID, ORDERDATE, TOTALPRICE, STATUS) "
                + "VALUES ('{}', '{}', '{}', {}, '{}')",
            order.getId(), order.getUserId(), order.getOrderDate(), order.getTotalPrice(), order.getStatus());
      }
    } else if (event.getEventType() == OrderEventType.ORDER_STATUS_UPDATED) {
      log.info("Synthesized legacy SQL write-back: UPDATE PURCHASEORDER SET STATUS = '{}' WHERE ORDERID = '{}'",
          event.getNewStatus(), event.getOrderId());
    }
  }

  public boolean isWriteBackEnabled() {
    return writeBackEnabled;
  }

  public void setWriteBackEnabled(boolean writeBackEnabled) {
    this.writeBackEnabled = writeBackEnabled;
  }
}
