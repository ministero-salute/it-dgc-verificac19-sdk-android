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

*Read this in other languages: [English](README.en.md).*

# Indice
- [Contesto](#contesto)
- [Installazione](#installazione)
- [Uso](#uso)
- [Contribuzione](#contribuzione)
- [Licenza](#licenza)
  - [Autori / Copyright](#autori--copyright)
  - [Dettaglio licenza](#dettaglio-licenza)

# Contesto
Questo repository contiene un Software Development Kit (SDK), rilasciato 
dal Ministero della Salute italiano, che consente di integrare nei sistemi
 di controllo degli accessi, inclusi quelli di rilevazione delle presenze, 
le funzionalità di verifica della Certificazione verde COVID-19, mediante 
la lettura del QR code. 

# Trattamento dati personali
Il trattamento dei dati personali svolto dalle soluzioni applicative sviluppate
a partire dalla presente SDK deve essere effettuato limitatamente alle
informazioni pertinenti e alle operazioni strettamente necessarie alla verifica
della validità delle Certificazioni verdi COVID-19. Inoltre è fatto esplicito
divieto di conservare il codice a barre bidimensionale (QR code) delle
Certificazioni verdi COVID-19 sottoposte a verifica, nonché di estrarre,
consultare, registrare o comunque trattare per finalità ulteriori rispetto
a quelle previste per la verifica della Certificazione verde COVID-19 o le
informazioni rilevate dalla lettura dei QR code e le informazioni fornite in
esito ai controlli, come indicato nel DPCM 12 ottobre 2021    
 
# Installazione
E' necessario clonare questo progetto insieme ai seguenti:

- [dgca-app-core-android repo](https://github.com/eu-digital-green-certificates/dgca-app-core-android)
- [dgc-certlogic-android repo](https://github.com/eu-digital-green-certificates/dgc-certlogic-android)

nel seguente modo:

```
your_project_folder
|___your_app
|___sdk_repo
|___dgca-app-core-android
|___dgc-certlogic-android
```
 
###   

# Uso
L'applicazione di verifica dovrà importare la componente `decoder` e `SDK`.
Nel file `settings.gradle` è necessario aggiungere le seguenti informazioni
(da cambiare a seconda della struttura delle directory del progetto):
 
```gradle
include ':app'  
include ':dgc-sdk'  
include ':decoder'  
rootProject.name = "dgp-whitelabel-android"  
project(':dgc-sdk').projectDir = new File("../it-dgc-verificac19-sdk-android/sdk")  
project(':decoder').projectDir = new File("../dgca-app-core-android/decoder")
```

A questo punto è possibile inizializzare il `workmanager` (`LoadKeysWorker`)
posizionato in `it.ministerodellasalute.verificaC19sdk.worker.LoadKeysWorker`
che consente di sincronizzare le regole di validazione e i certificati di firma
prima di iniziare ad utilizzare l'applicazione.

Tra i parametri restituiti dalle API REST, c'è la `Minimum SDK Version`.   
Questo valore è confrontato con la versione corrente dell'SDK presente in
`BuildConfig.SDK_VERSION`, tramite il metodo 
`it.ministerodellasalute.verificaC19sdk.model.FirstViewModel#isSDKVersionObsoleted`,
in modo da garantire che la versione di SDK utilizzata non sia obsoleta e potenzialmente
non valida. Nel caso in cui la versione di SDK utilizzata sia inferiore a quella restituita dalle
API REST, l'SDK lancerà una `VerificaMinSDKVersionException`
che potrebbe causare un crash dell'applicazione se non gestita correttamente.

A questo punto è possibile utilizzare una libreria di scansione di QR Code
a scelta che, dopo aver letto un QR Code di un EU DCC, passi la stringa
estratta a  
`it.ministerodellasalute.verificaC19sdk.model.VerificationViewModel#init`.  

Esempio:  
 
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

Osservando la risposta del metodo, un `Certificate object` è restituito
`it.ministerodellasalute.verificaC19sdk.model.CertificateSimple` che contiene
il risultato della verifica. Il data model contiene i dati relativi alla
persona, la data di nascita, il timestamp di verifica e lo stato della
verifica.

Basandosi su questi dati è possibile disegnare la UI e fornire all'operatore lo
stato della verifica del DCC.
 
# Contribuzione

Ogni contributo è ben accetto. Prima di procedere è buona norma consultare il
[Code of Conduct](./CODE_OF_CONDUCT.md) per ottenere una aiuto relativamente
alle modalità di approccio alla community.
Inoltre è possibile consultare il file [CONTRIBUTING](./CONTRIBUTING.md)
che contiene le informazioni pratiche per assicurare una contribuzione efficace
e efficiente.

## Contributori

Qui c'è una lista di contributori. Grazie per essere partecipi nel
miglioramento del progetto giorno dopo giorno!
    
<a href="https://github.com/ministero-salute/it-dgc-verificac19-sdk-android">  
  <img    
  src="https://contributors-img.web.app/image?repo=ministero-salute/it-dgc-verificac19-sdk-android"   
  />    
</a>    
    
# Licenza

## Autori / Copyright
Copyright 2021 (c) Ministero della Salute.    
    
Il file [AUTHORS](./AUTHORS) contiene le informazioni sugli autori.

## Dettaglio Licenza
La licenza per questo repository è una `Apache License 2.0`.
All'interno del file [LICENSE](./LICENSE) sono presenti le informazioni
specifiche.
