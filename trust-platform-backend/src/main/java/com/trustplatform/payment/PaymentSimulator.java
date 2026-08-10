package com.trustplatform.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Marker component that is only initialized when the production profile is NOT active.
 * Used to ensure sandbox payment simulation cannot run in production environments.
 */
@Component
@Profile("!prod")
public class PaymentSimulator {
}
