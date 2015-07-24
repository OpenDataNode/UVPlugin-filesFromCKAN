### Description

Downloads file from CKAN resources

### Configuration parameters

| Name | Description |
|:----|:----|
|**org.opendatanode.CKAN.secret.token** |token used to authenticate to CKAN, has to be set in frontend.properties and backend.properties |
|**org.opendatanode.CKAN.api.url** |URL to CKAN API internal_api, e.g. http://{host}/api/action/internal_api, has to be set in frontend.properties and backend.properties  |
|**org.opendatanode.CKAN.http.header.[key]** | custom HTTP header added to requests on CKAN |

### Inputs and outputs

|Name |Type | DataUnit | Description | Mandatory |
|:--------|:------:|:------:|:-------------|:---------------------:|
|output|o|FilesDataUnit|Downloaded file from CKAN resource||