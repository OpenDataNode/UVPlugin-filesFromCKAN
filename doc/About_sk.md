### Popis

Stiahne súbor zo zdrojov CKAN

### Konfiguračné parametre

| Meno | Popis |
|:----|:----|
|**org.opendatanode.CKAN.secret.token** |reťazec použitý pre autentifikáciu v CKAN, nastavuje sa v súboroch frontend.properties and backend.properties |
|**org.opendatanode.CKAN.api.url** |URL k CKAN API internal_api, napr. http://{host}/api/action/internal_api, nastavuje sa vo frontend.properties and backend.properties  |
|**org.opendatanode.CKAN.http.header.[key]** | aktuálna HTTP hlavička pridana k žiadosti na CKAN |

### Vstupy a výstupy ###

|Meno |Typ | Dátová hrana | Popis | Povinné |
|:--------|:------:|:------:|:-------------|:---------------------:|
|output|o|FilesDataUnit|Stiahnutý súbor zo zdrojov CKAN||