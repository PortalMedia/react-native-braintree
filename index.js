// main index.js

import {NativeModules} from 'react-native';

const {RNBraintree} = NativeModules;

export default {
  showPayPalModule: RNBraintree.showPayPalModule,
};
