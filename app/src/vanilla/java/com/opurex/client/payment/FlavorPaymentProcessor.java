package com.opurex.client.payment;

import com.opurex.client.models.Payment;
import com.opurex.client.activities.TrackedActivity;

public abstract class FlavorPaymentProcessor extends PaymentProcessor {

    protected FlavorPaymentProcessor(TrackedActivity parentActivity, PaymentListener listener, Payment payment) {
        super(parentActivity, listener, payment);
    }

}
