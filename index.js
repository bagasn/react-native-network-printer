import {NativeModules} from 'react-native';

const {RNRnNetworkPrinter, Network} = NativeModules;
const Print = async (ip, commands) => {
  return new Promise((resolve, reject) => {
    RNRnNetworkPrinter.Print(ip, commands, (err, result) => {
      if (err) reject(err);
      resolve(result);
    });
  });
};

const PrintLabel = async (ip, commands) => {
  return new Promise((resolve, reject) => {
    RNRnNetworkPrinter.PrintLabel(ip, commands, (err, result) => {
      if (err) reject(err);
      resolve(result);
    });
  });
};

const GetPrinters = async () => {
  const data = await Promise.all([getPrinter(0), getPrinter(1), getPrinter(2)]);
  return data[0].concat(data[1]).concat(data[2]);
};

const getPrinter = async type => {
  return new Promise((resolve, reject) => {
    Network.list(type || 0, (err, result) => {
      if (err) reject(err);
      resolve(result);
    });
  });
};

const NetworkPrinter = Print;
export {Print, PrintLabel, NetworkPrinter, GetPrinters};
