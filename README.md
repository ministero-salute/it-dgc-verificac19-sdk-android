
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
    <a href="CODE_OF_CONDUCT.md">  
      <img src="https://img.shields.io/badge/badge-green.svg" />  
    </a>  
    <a href="/">  
      <img alt="java11"  
      src="https://img.shields.io/badge/badge-red.svg">  
    </a>  
    <a href="/">  
      <img alt="security: bandit"  
      src="https://img.shields.io/badge/badge-yellow.svg">  
    </a>  
</div>  
  
  
# Table of contents  
  
- [Context](#context)  
- [Installation](#installation)  
- [Usage](#usage)  
- [Contributing](#contributing)  
  - [Contributors](#contributors)  
- [Licence](#licence)  
  - [Authors / Copyright](#authors--copyright)  
  - [Third-party component licences](#third-party-component-licences)  
  - [Licence details](#licence-details)  
  
  
# Context  
  
**Please take the time to read and consider the other repositories in full before digging into the source code or opening an Issue. They contain a lot of details that are fundamental to understanding the source code and this repository's documentation.**  
  
# Installation  
Clone this project as the same level as the decoder and the verifier application. By Verifier application, we mean the custom DGC verifier app which is using the decoder and the sdk.

```
project_folder
|___verifier-app-android_app_repo
|___sdk_repo
|___dgca-app-core-android
|___dgc-certlogic-android
```

###   

# Usage  

The verifier application will need to import the decoder and the SDK.
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

Among the received parameters from the REST API, a Minimum App Version is received. Compare this value using `it.ministerodellasalute.verificaC19sdk.model.FirstViewModel#getAppMinVersion`,
passing the SDK's current version present in `BuildConfig.VERSION_NAME` in order to guarantee a matching API response and SDK version in the UI level. In case these values don't match correctly, the SDK will throw a `VerificaMinVersionException` during the DGC verification which needs to be handled correctly (for example by redirecting the user to PlayStore).
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

At this point it's possible to use a QrCodeScanner library of choice and pass the extracted string to `it.ministerodellasalute.verificaC19sdk.model.VerificationViewModel#init`.
Example:

```kotlin
try {  
    viewModel.init(args.qrCodeText)  
}  
catch (e: VerificaMinVersionException)  
{  
    Log.d("VerificationFragment", "Min Version Exception")  
    createForceUpdateDialog()  
}
```

Observing the LiveData response of the method, a Certificate object is returned `it.ministerodellasalute.verificaC19sdk.model.CertificateSimple` which contains the decoded and validated response of the verification. The data model contains person data, birthday and the verification status.

Based on these data, it's possible to draw the UI and prompt the operator about the status of the DGC.

  
# Contributing  
Contributions are most welcome. Before proceeding, please read the [Code of Conduct](./CODE_OF_CONDUCT.md) for guidance on how to approach the community and create a positive environment. Additionally, please read our [CONTRIBUTING](./CONTRIBUTING.md) file, which contains guidance on ensuring a smooth contribution process.  
  
## Contributors  
Here is a list of contributors. Thank you to everyone involved for improving this project, day by day.  
  
<a href="https://github.com/ministero-salute/it-dgc-verificac19-sdk-android)">  
  <img  
  src="https://contributors-img.web.app/image?repo=ministero-salute/it-dgc-verificac19-sdk-android"  
  />  
</a>  
  
# Licence  
  
## Authors / Copyright  
  
Copyright 2021 (c) Ministero della Salute.  
  
Please check the [AUTHORS](./AUTHORS) file for extended reference.  
  
## Third-party component licences  
  
## Licence details  
  
The licence for this repository is a [GNU Affero General Public Licence version 3](https://www.gnu.org/licenses/agpl-3.0.html) (SPDX: AGPL-3.0). Please see the [LICENSE](./LICENSE) file for full reference.
