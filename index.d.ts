declare module 'react-native-braintree' {
  export interface BraintreeResponse {
    nonce: string;
    payerId: string;
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
    billingAddress: BraintreePostalAddress;
    shippingAddress: BraintreePostalAddress;
    deviceData: string;
  }

  export interface BraintreePostalAddress {
    countryCodeAlpha2: string;
    extendedAddress: string;
    locality: string;
    postalCode: string;
    recipientName: string;
    region: string;
    streetAddress: string;
  }

  export interface BraintreeOptions {
    clientToken: string;
    amount: string;
    currencyCode: string;
  }

  // Export

  interface RNBraintreeModule {
    showPayPalModule(options: BraintreeOptions): Promise<BraintreeResponse>;
  }

  const RNBraintree: RNBraintreeModule;

  export default RNBraintree;
}
