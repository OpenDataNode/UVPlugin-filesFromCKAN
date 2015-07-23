E-FilesFromCKAN
----------

### Documentation

Downloads file from CKAN resources

### Configuration parameters

|Parameter                        |Description                             |                                                        
|---------------------------------|----------------------------------------|
|org.opendatanode.CKAN.secret.token |Token used to authenticate to CKAN, has to be set in frontend.properties and backend.properties |
|org.opendatanode.CKAN.api.url    |URL to CKAN API internal_api, e.g. http://{host}/api/action/internal_api, has to be set in frontend.properties and backend.properties  |
|org.opendatanode.CKAN.http.header.[key] | Custom HTTP header added to requests on CKAN |

### Inputs and outputs

|Name                |Type       |DataUnit                         |Description                        |
|--------------------|-----------|---------------------------------|-----------------------------------|
|output              |o          |FilesDataUnit                    |Downloaded file from CKAN resource |

### Version history

* GUI changes: added new checkbox for showing only 'my' (organization) datasets
* i18n update
* fixed bug: after selecting resource, it was not selected in tree after reopening config dialog

#### v1.0.0
* Initial version


### Developer's notes

* dependent on ckanext-odn-pipeline v0.5.1+
* The configuration parameters are needed in both frontend and backend configuration files.
* Dependent on ckanext-odn-pipeline branch feature/edem requires change in CKAN core to allow downloading files through API ckan/controllers/api.py in method action add code mentioned bellow.


after:
```python
result = function(context, request_data) (line 197)
```
add:
```python
if 'internal_api' == logic_function and request_data.get('action','') == 'resource_download':
	return result
```
