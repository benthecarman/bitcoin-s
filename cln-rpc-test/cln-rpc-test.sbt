Test / test := (Test / test dependsOn {
  Projects.clnRpc / TaskKeys.downloadCLN
}).value

Test / test := (Test / test dependsOn {
  Projects.bitcoindRpc / TaskKeys.downloadBitcoind
}).value
