E-FilesFromCKAN
----------

### Documentation

Downloads file from CKAN resources

### Configuration parameters

|Parameter                        |Description                             |                                                        
|---------------------------------|----------------------------------------|
|dpu.uv-e-filesFromCKAN.secret.token |authentication token |
|dpu.uv-e-filesFromCKAN.catalog.api.url |URL to CKAN API internal_api, e.g. http://host/api/action/internal_api  |

### Inputs and outputs

|Name                |Type       |DataUnit                         |Description                        |
|--------------------|-----------|---------------------------------|-----------------------------------|
|output              |o          |FilesDataUnit                    |Downloaded file from CKAN resource |


### Version history

#### v1.0.0
* Initial version


### Developer's notes

The configuration parameters are needed in both frontend and backend configuration files.
Dependent on ckanext-odn-pipeline branch feature/edem requires change in CKAN core to allow downloading files through API ckan/controllers/api.py in method action add code mentioned bellow.

after:
```python
result = function(context, request_data) (line 197)
```
add:
```python
if 'internal_api' == logic_function and request_data.get('action','') == 'resource_download':
	return result
```
