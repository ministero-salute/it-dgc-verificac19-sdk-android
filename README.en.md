<h1 align="center">Digital Green Certificate SDK</h1>

<div align="center">
<img width="256" height="256" src="img/logo-dcg.png">
</div>
<br />
<div align="center">
    <!-- CoC -->
    <a href="CODE_OF_CONDUCT.md">
      <img src="https://img.shields.io/badge/Contributor%20Covenant-v2.0%20adopted-ff69b4.svg" />
    </a>
    <a href="https://github.com/ministero-salute/it-dgc-verificac19-sdk-android/actions/workflows/ci.yml">
      <img src="https://github.com/ministero-salute/it-dgc-verificac19-sdk-android/actions/workflows/ci.yml/badge.svg" />
    </a>
</div>

*Leggi in altre lingue: [Italiano](README.md).*

# Table of contents
- [Context](#context)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
  - [Contributors](#contributors)
- [License](#license)
  - [Authors / Copyright](#authors--copyright)
  - [License details](#license-details)

# Context
 **Please take the time to read and consider the other repositories in full before digging into the source code or opening an Issue. They contain a lot of details that are fundamental to understanding the source code and this repository's documentation.**    
 
# Installation
Clone this project alongside

- [dgca-app-core-android repo](https://github.com/eu-digital-green-certificates/dgca-app-core-android)
- [dgc-certlogic-android repo](https://github.com/eu-digital-green-certificates/dgc-certlogic-android)

```
your_project_folder
|___your_app
|___sdk_repo
|___dgca-app-core-android
|___dgc-certlogic-android
```

Verifier application `VerificaC19` leverages on this SDK to work.
  
###   
# Usage
The application will need to import the decoder and the SDK.  
In the `settings.gradle` file add the following lines (change according to your directory structure):  
  
```gradle
include ':app'  
include ':dgc-sdk'  
include ':decoder'  
rootProject.name = "dgp-whitelabel-android"  
project(':dgc-sdk').projectDir = new File("../it-dgc-verificac19-sdk-android/sdk")  
project(':decoder').projectDir = new File("../dgca-app-core-android/decoder")
```
  
Then start the workmanager (`LoadKeysWorker`) located in `it.ministerodellasalute.verificaC19sdk.worker.LoadKeysWorker` in order to sync the rules and key certificates upon app start.  
  
Among the received parameters from the REST API, a Minimum App Version is received. Compare this value using `it.ministerodellasalute.verificaC19sdk.model.FirstViewModel#getAppMinVersion`, passing the SDK's current version present in `BuildConfig.VERSION_NAME` in order to guarantee a matching API response and SDK version in the UI level. In case these values don't match correctly, the SDK will throw a `VerificaMinVersionException` during the DGC verification which needs to be handled correctly (for example by redirecting the user to PlayStore).  
Example:  
  
```kotlin
override fun onResume() {  
    super.onResume()  
    viewModel.getAppMinVersion().let {  
        if (Utility.versionCompare(it, BuildConfig.VERSION_NAME) > 0) {  
            createForceUpdateDialog()  
        }  
    }
}
```
  
The method `it.ministerodellasalute.verificaC19sdk.model.FirstViewModel#isSDKVersionObsoleted` should be used to check if the min SDK version returned from the server is bigger than or equal to the current SDK version. In case this check isn't done correctly in UI level, the SDK will throw a `VerificaMinSDKVersionException` which might cause the application to crash if not handled correctly.

At this point it's possible to use a QrCodeScanner library of choice and pass the extracted string to `it.ministerodellasalute.verificaC19sdk.model.VerificationViewModel#init`.  
Example:  
  
```kotlin
try {  
    viewModel.init(args.qrCodeText)  
} catch (e: VerificaMinVersionException) {  
    Log.d("VerificationFragment", "Min Version Exception")  
    createForceUpdateDialog()  
}
```

Observing the LiveData response of the method, a Certificate object is returned `it.ministerodellasalute.verificaC19sdk.model.CertificateSimple` which contains the decoded and validated response of the verification. The data model contains person data, birthday, verification timestamp and the verification status.  
  
Based on these data, it's possible to draw the UI and prompt the operator about the status of the DGC.  
  
# Contributing

Contributions are most welcome. Before proceeding, please read the [Code of Conduct](./CODE_OF_CONDUCT.md) for guidance on how to approach the community and create a positive environment. Additionally, please read our [CONTRIBUTING](./CONTRIBUTING.md) file, which contains guidance on ensuring a smooth contribution process.
    
## Contributors

Here is a list of contributors. Thank you to everyone involved for improving this project, day by day.    

<a href="https://github.com/ministero-salute/it-dgc-verificac19-sdk-android">
  <img
  src="https://contributors-img.web.app/image?repo=ministero-salute/it-dgc-verificac19-sdk-android"
  />
</a>
    
# License

## Authors / Copyright    
Copyright 2021 (c) Ministero della Salute.    
    
Please check the [AUTHORS](./AUTHORS) file for extended reference.    

## License details    
The license for this repository is Apache License 2.0. Please see the [LICENSE](./LICENSE) file for full reference.
