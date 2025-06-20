package com.opurex.client.payment;

import android.content.Intent;
import com.opurex.client.Configure;
import com.opurex.client.models.Payment;
import com.opurex.client.activities.TrackedActivity;

public abstract class PaymentProcessor {

	protected TrackedActivity parentActivity;
	
	protected Payment payment;
	
	protected PaymentListener listener;
	
	public enum Status {
		VALIDATED,
		PENDING
	}

	protected PaymentProcessor (TrackedActivity parentActivity, PaymentListener listener, Payment payment) {
		this.parentActivity= parentActivity;
		this.listener = listener;
		this.payment = payment;
	}

	public abstract void handleIntent(int requestCode, int resultCode,
            Intent data);

	public abstract Status initiatePayment();

	public static PaymentProcessor getProcessor(TrackedActivity parentActivity, PaymentListener listener, Payment payment) { 
		if ("magcard".equals(payment.getMode().getCode())) {
			String cardProcessor = Configure.getCardProcessor(parentActivity);
			if ("none".equals(cardProcessor)) {
				return null;
			}
			else if ("payleven".equals(cardProcessor))
				return new PaylevenPaymentProcessor(parentActivity, listener, payment);
			else
				// Atos is "generic"
				return new AtosPaymentProcessor(parentActivity, listener, payment);
		}
		return null;
	}
	
	public interface PaymentListener {
		void registerPayment(Payment p);
	}
}
