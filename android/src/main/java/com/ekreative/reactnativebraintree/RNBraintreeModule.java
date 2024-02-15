// RNBraintreeModule.java

package com.ekreative.reactnativebraintree;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.UserCanceledException;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

public class RNBraintreeModule extends ReactContextBaseJavaModule
        implements ActivityEventListener, LifecycleEventListener {

    private final Context mContext;
    private FragmentActivity mCurrentActivity;
    private Promise mPromise;
    private String mToken;
    private BraintreeClient mBraintreeClient;
    private PayPalClient mPayPalClient;

    @NonNull
    @Override
    public String getName() {
        return "RNBraintree";
    }

    public RNBraintreeModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mContext = reactContext;

        reactContext.addLifecycleEventListener(this);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mCurrentActivity != null) {
            mCurrentActivity.setIntent(intent);
        }
    }

    @Override
    public void onHostResume() {
        if (mBraintreeClient != null && mCurrentActivity != null) {
            BrowserSwitchResult browserSwitchResult =
                    mBraintreeClient.deliverBrowserSwitchResult(mCurrentActivity);
            if (browserSwitchResult != null) {
                if (browserSwitchResult.getRequestCode() == BraintreeRequestCodes.PAYPAL) {
                    if (mPayPalClient != null) {
                        mPayPalClient.onBrowserSwitchResult(
                                browserSwitchResult,
                                this::handlePayPalResult
                        );
                    }
                }
            }
        }
    }

    @ReactMethod
    public void showPayPalModule(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("MISSING_CLIENT_TOKEN", "You must provide a clientToken");
        } else {
            setup(parameters.getString("clientToken"));

            String currency = "USD";
            if (!parameters.hasKey("amount")) {
                promise.reject("MISSING_AMOUNT", "You must provide a amount");
            }
            if (parameters.hasKey("currencyCode")) {
                currency = parameters.getString("currencyCode");
            }
            if (mCurrentActivity != null) {
                mPayPalClient = new PayPalClient(mBraintreeClient);
                PayPalCheckoutRequest request = new PayPalCheckoutRequest(
                        parameters.getString("amount")
                );
                request.setCurrencyCode(currency);
                request.setIntent(PayPalPaymentIntent.AUTHORIZE);
                mPayPalClient.tokenizePayPalAccount(
                        mCurrentActivity,
                        request,
                        e -> handlePayPalResult(null, e));
            }
        }
    }

    private void handlePayPalResult(
            @Nullable PayPalAccountNonce payPalAccountNonce,
            @Nullable Exception error
    ) {
        if (error != null) {
            handleError(error);
            return;
        }
        if (payPalAccountNonce != null) {
            sendPaymentMethodDetailResult(payPalAccountNonce);
        }
    }

    private void sendPaymentMethodDetailResult(PayPalAccountNonce nonce) {
        if (mPromise == null) {
            return;
        }
        WritableMap result = Arguments.createMap();
        result.putString("nonce", nonce.getString());
        result.putString("payerId", nonce.getPayerId());
        result.putString("email", nonce.getEmail());
        result.putString("firstName", nonce.getFirstName());
        result.putString("lastName", nonce.getLastName());
        result.putString("phone", nonce.getPhone());
        PostalAddress billingAddress = nonce.getBillingAddress();
        result.putMap("billingAddress", createAddressMap(billingAddress));
        PostalAddress shippingAddress = nonce.getShippingAddress();
        result.putMap("shippingAddress", createAddressMap(shippingAddress));
        mPromise.resolve(result);
    }

    private WritableMap createAddressMap(PostalAddress address) {
        WritableMap addressMap = Arguments.createMap();
        if (address != null) {
            addressMap.putString("countryCodeAlpha2", address.getCountryCodeAlpha2());
            addressMap.putString("extendedAddress", address.getExtendedAddress());
            addressMap.putString("locality", address.getLocality());
            addressMap.putString("postalCode", address.getPostalCode());
            addressMap.putString("recipientName", address.getRecipientName());
            addressMap.putString("region", address.getRegion());
            addressMap.putString("streetAddress", address.getStreetAddress());
        } else {
            addressMap.putNull("countryCodeAlpha2");
            addressMap.putNull("extendedAddress");
            addressMap.putNull("locality");
            addressMap.putNull("postalCode");
            addressMap.putNull("recipientName");
            addressMap.putNull("region");
            addressMap.putNull("streetAddress");
        }
        return addressMap;
    }

    private void setup(final String token) {
        if (mBraintreeClient == null || !token.equals(mToken)) {
            mCurrentActivity = (FragmentActivity) getCurrentActivity();
            mBraintreeClient = new BraintreeClient(mContext, token);
            mToken = token;
        }
    }

    private void handleError(Exception error) {
        if (mPromise != null) {
            if (error instanceof UserCanceledException) {
                mPromise.reject("USER_CANCELLATION", "The user cancelled");
            }
            mPromise.reject("ERROR", error.getMessage());
        }
    }

    @Override
    public void onHostPause() {
        //NOTE: empty implementation
    }

    @Override
    public void onHostDestroy() {
        //NOTE: empty implementation
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        //NOTE: empty implementation
    }
}
