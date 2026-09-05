package com.petstore.order.dto;

import com.petstore.order.document.AddressDocument;
import com.petstore.order.document.LineItemDocument;
import com.petstore.order.document.PaymentDocument;
import java.io.Serializable;
import java.util.List;

/**
 * Request payload for customer purchase order checkout.
 *
 * @param userId customer identifier
 * @param locale requested storefront locale (defaults to en_US)
 * @param billing billing address details
 * @param shipping shipping address details
 * @param payment payment and credit card details
 * @param lineItems purchased cart line items
 */
public record CreateOrderRequest(
    String userId,
    String locale,
    AddressDocument billing,
    AddressDocument shipping,
    PaymentDocument payment,
    List<LineItemDocument> lineItems
) implements Serializable {}
