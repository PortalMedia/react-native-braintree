#import "RNBraintree.h"
#import "BraintreeCore.h"
#import "BTCardClient.h"
#import "BraintreePayPal.h"

@interface RNBraintree() <BTViewControllerPresentingDelegate>
@property (nonatomic, strong) BTAPIClient *apiClient;
@end

@implementation RNBraintree

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(showPayPalModule: (NSDictionary *)options
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject) {
    NSString *clientToken = options[@"clientToken"];
    NSString *amount = options[@"amount"];
    NSString *currencyCode = options[@"currencyCode"];

    self.apiClient = [[BTAPIClient alloc] initWithAuthorization: clientToken];
    BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient: self.apiClient];

    BTPayPalCheckoutRequest *request= [[BTPayPalCheckoutRequest alloc] initWithAmount:amount];
    request.currencyCode = currencyCode;
    [payPalDriver tokenizePayPalAccountWithPayPalRequest:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
        if (error) {
            reject(@"ONE_TIME_PAYMENT_FAILED", error.localizedDescription, nil);
            return;
        }
        if (!tokenizedPayPalAccount) {
            reject(@"ONE_TIME_PAYMENT_CANCELLED", @"Payment has been cancelled", nil);
            return;
        }
        NSMutableDictionary *billingAddressDictionary = [NSMutableDictionary dictionary];
        if (tokenizedPayPalAccount.billingAddress) {
            [billingAddressDictionary addEntriesFromDictionary:@{
                @"countryCodeAlpha2": tokenizedPayPalAccount.billingAddress.countryCodeAlpha2 ?: [NSNull null],
                @"extendedAddress": tokenizedPayPalAccount.billingAddress.extendedAddress ?: [NSNull null],
                @"locality": tokenizedPayPalAccount.billingAddress.locality ?: [NSNull null],
                @"postalCode": tokenizedPayPalAccount.billingAddress.postalCode ?: [NSNull null],
                @"recipientName": tokenizedPayPalAccount.billingAddress.recipientName ?: [NSNull null],
                @"region": tokenizedPayPalAccount.billingAddress.region ?: [NSNull null],
                @"streetAddress": tokenizedPayPalAccount.billingAddress.streetAddress ?: [NSNull null],
            }];
        }
        NSMutableDictionary *shippingAddressDictionary = [NSMutableDictionary dictionary];
        if (tokenizedPayPalAccount.shippingAddress) {
            [shippingAddressDictionary addEntriesFromDictionary:@{
                @"countryCodeAlpha2": tokenizedPayPalAccount.shippingAddress.countryCodeAlpha2 ?: [NSNull null],
                @"extendedAddress": tokenizedPayPalAccount.shippingAddress.extendedAddress ?: [NSNull null],
                @"locality": tokenizedPayPalAccount.shippingAddress.locality ?: [NSNull null],
                @"postalCode": tokenizedPayPalAccount.shippingAddress.postalCode ?: [NSNull null],
                @"recipientName": tokenizedPayPalAccount.shippingAddress.recipientName ?: [NSNull null],
                @"region": tokenizedPayPalAccount.shippingAddress.region ?: [NSNull null],
                @"streetAddress": tokenizedPayPalAccount.shippingAddress.streetAddress ?: [NSNull null],
            }];
        }
        resolve(@{
            @"nonce": tokenizedPayPalAccount.nonce ?: [NSNull null],
            @"payerId": tokenizedPayPalAccount.payerID ?: [NSNull null],
            @"email": tokenizedPayPalAccount.email ?: [NSNull null],
            @"firstName": tokenizedPayPalAccount.firstName ?: [NSNull null],
            @"lastName": tokenizedPayPalAccount.lastName ?: [NSNull null],
            @"phone": tokenizedPayPalAccount.phone ?: [NSNull null],
            @"billingAddress": billingAddressDictionary,
            @"shippingAddress": shippingAddressDictionary,
        });
    }];
}

#pragma mark - BTViewControllerPresentingDelegate
- (void)paymentDriver:(nonnull id)driver requestsPresentationOfViewController:(nonnull UIViewController *)viewController {
    [self.reactRoot presentViewController:viewController animated:YES completion:nil];
}

- (void)paymentDriver:(nonnull id)driver requestsDismissalOfViewController:(nonnull UIViewController *)viewController {
    [viewController dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - RootController
- (UIViewController*)reactRoot {
    UIViewController *topViewController  = [UIApplication sharedApplication].keyWindow.rootViewController;
    if (topViewController.presentedViewController) {
        topViewController = topViewController.presentedViewController;
    }
    return topViewController;
}

@end
