# E-FilesFromCKAN #
----------

###General###

|                              |                                                               |
|------------------------------|---------------------------------------------------------------|
|**Name:**                     |E-FilesFromCKAN                                                |
|**Description:**              |Downloads file from CKAN resources.                            |
|**Status:**                   |Supported in Plugins v2.1.X Updated to use Plugin-DevEnv v2.1.X |
|                              |                                                               |
|**DPU class name:**           |FilesFromCKAN                                                  | 
|**Configuration class name:** |FilesFromCKANConfig_V1                                         |
|**Dialogue class name:**      |FilesFromCKANVaadinDialog                                      | 

***

###Configuration parameters###

|Parameter                        |Description                             |                                                        
|---------------------------------|----------------------------------------|
|dpu.uv-e-filesFromCKAN.secret.token |authentication token |
|dpu.uv-e-filesFromCKAN.catalog.api.url |URL to CKAN API internal_api, e.g. http://host/api/action/internal_api  |

***

### Inputs and outputs ###

|Name                |Type       |DataUnit                         |Description                        |
|--------------------|-----------|---------------------------------|-----------------------------------|
|output              |o          |FilesDataUnit                    |Downloaded file from CKAN resource |


***

### Version history ###

|Version            |Release notes                                   |
|-------------------|------------------------------------------------|
|1.0.0              |N/A                                             |                                


***

### Developer's notes ###

|Author            |Notes                 |
|------------------|----------------------|
|mvi               |The configuration parameters are needed in both frontend and backend configuration files. |
|mvi               |dependent on ckanext-odn-pipeline branch feature/edem| 
|mvi               |requires change in CKAN core to allow downloading files through API|
|                  |ckan/controllers/api.py in method action add code mentioned bellow|

after:
```python
result = function(context, request_data) (line 197)
```
add:
```python
if 'internal_api' == logic_function and request_data.get('action','') == 'resource_download':
	return result
```

