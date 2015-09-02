### Description

Downloads file from CKAN resources

### Configuration parameters

| Name | Description |
|:----|:----|
|Only my own datasets|If checked, only my organization's (public and private) datasets will be shown. Otherwise all public datasets of other organizations will be shown too.|
|Expand / Collapse all|Buttons that expand or collapse the loaded tree|
|Search dataset / file|Filtering according to tree node names|
|Dataset resource to download|Tree consisting from: Organization name (1st level), Dataset name (2nd level), Resource name (3rd level)|
|Rename downloaded file to|File name which it should be renamed to after download. If no name is provided, resource ID will be used.|

### Inputs and outputs

|Name |Type | DataUnit | Description | Mandatory |
|:--------|:------:|:------:|:-------------|:---------------------:|
|output |o| FilesDataUnit | Downloaded file from CKAN resource | |