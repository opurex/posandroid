package com.opurex.client.payment;

import net.atos.sdk.tpe.PaymentAsyncTaskActions;
import net.atos.sdk.tpe.PaymentManager;
import net.atos.sdk.tpe.datas.PaymentResponse;
import net.atos.sdk.tpe.enums.AuthorizationCall;
import net.atos.sdk.tpe.enums.Delay;
import net.atos.sdk.tpe.enums.PaymentMethod;
import net.atos.sdk.tpe.enums.PaymentResponseCode;
import net.atos.sdk.tpe.enums.ResponseIndicatorField;
import net.atos.sdk.tpe.enums.TransactionStatus;
import net.atos.sdk.tpe.enums.TransactionType;
import net.atos.sdk.tpe.exceptions.IncompatibleTerminalMethodException;
import net.atos.sdk.tpe.terminalmethods.XengoTerminalMethod;
import net.atos.sdk.tpe.terminalmethods.YomaniNetworkTerminalMethod;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import com.opurex.client.R;
import com.opurex.client.Configure;
import com.opurex.client.models.Payment;
import com.opurex.client.activities.TrackedActivity;

public class AtosPaymentProcessor extends FlavorPaymentProcessor {
	private PaymentManager paymentManager;

	public AtosPaymentProcessor(TrackedActivity parentActivity,
			PaymentListener listener, Payment payment) {
		super(parentActivity, listener, payment);
		
		String cardProcessor = Configure.getCardProcessor(parentActivity);
		
		if (!cardProcessor.startsWith("atos_")) {
			throw new RuntimeException("No Atos TPE enabled in configuration"); 
		}

		paymentManager = PaymentManager.getInstance();
		if ("atos_classic".equals(cardProcessor)) {
			YomaniNetworkTerminalMethod yomani = new YomaniNetworkTerminalMethod(
					1, Configure.getWorldlineAddress(parentActivity), 3333,
					ResponseIndicatorField.NO_FIELD, PaymentMethod.INDIFFERENT,
					null, Delay.END_OF_TRANSACTION_RESPONSE,
					AuthorizationCall.TPE_DECISION);
			try {
				paymentManager.addTerminalMethod(yomani);
			} catch (IncompatibleTerminalMethodException e) {
				e.printStackTrace();
			}
		} else if ("atos_xengo".equals(cardProcessor)) {
			String userId = Configure.getXengoUserId(parentActivity);
			String terminalId = Configure.getXengoTerminalId(parentActivity);
			String password = Configure.getXengoPassword(parentActivity);
			//"demo_a554314", "20017884", "motdepasse"
			XengoTerminalMethod xengo = new XengoTerminalMethod(2, parentActivity,
					"https://macceptance.sygea.com/tpm/tpm-shop-service/",
					"https://macceptance.sygea.com/tpm/tpm-update-service/",
					userId, terminalId, password, "", "", "");
			try {
				paymentManager.addTerminalMethod(xengo);
			} catch (IncompatibleTerminalMethodException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleIntent(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub

	}

	@Override
	public Status initiatePayment() {
		paymentManager.proceedPayment(1, TransactionType.DEBIT,
				payment.getAmount(), payment.getCurrency().getCurrencyCode(),
				null, null, new WorldlineTPEResultHandler(payment));

		return Status.PENDING;
	}

	/** Worldline TPE response callback */
	private class WorldlineTPEResultHandler implements PaymentAsyncTaskActions {

		private Payment payment;
		private ProgressDialog paymentDialog;

		public WorldlineTPEResultHandler(Payment p) {
			this.paymentDialog = new ProgressDialog(AtosPaymentProcessor.this.parentActivity);
			this.payment = p;
		}

		@Override
		public void onTransactionStatusChange(TransactionStatus status) {
		}

		@Override
		public void onPrePayment() {
			Activity parentActivity = AtosPaymentProcessor.this.parentActivity;
			if (paymentDialog == null) {
				paymentDialog = new ProgressDialog(parentActivity);
			}
			paymentDialog.setCancelable(false);
			paymentDialog.setMessage(parentActivity.getString(R.string.card_payment_startup));
			paymentDialog.show();
		}

		@Override
		public void onPostPayment(PaymentResponse response) {
			Activity parentActivity = AtosPaymentProcessor.this.parentActivity;
			
			if (paymentDialog != null) {
				paymentDialog.dismiss();
				paymentDialog = null;
			}
			if (response.getPaymentResponseCode() != PaymentResponseCode.OK) {
				AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
				builder.setMessage(parentActivity.getString(R.string.card_payment_failure));
				builder.setNeutralButton(android.R.string.ok, null);
				builder.show();
				return;
			}
			TransactionStatus status = response.getTransactionStatus();
			switch (status) {
			case SUCCESS: // Validated
				AtosPaymentProcessor.this.listener.registerPayment(this.payment);
				break;
			case REFUSED: // Canceled
				AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
				builder.setMessage(parentActivity.getString(R.string.card_payment_cancelled));
				builder.setNeutralButton(android.R.string.ok, null);
				builder.show();
				break;
			default:
				// We ignore the other states
				break;
			}
		}
	}

}
