E-FilesFromCKAN
----------

### Documentation

* see [Plugin Documentation](./doc/About.md)
* see [Plugin Documentation](./doc/About_sk.md) (in Slovak)

### Technical notes

* These properties have to be set in frontend.properties and backend.properties of UnifiedViews for correct functionality of DPU

| Property Name | Description |
|:----|:----|
`org.opendatanode.CKAN.secret.token` |Token used to authenticate to CKAN |
`org.opendatanode.CKAN.api.url` | URL where CKAN api is located, has to be set in backend.properties |
`org.opendatanode.CKAN.http.header.[key]` | Custom HTTP header added to requests on CKAN |

Example:

```INI
org.opendatanode.CKAN.secret.token = 12345678901234567890123456789012
org.opendatanode.CKAN.api.url = ﻿http://localhost:9080/internalcatalog/api/action/internal_api
org.opendatanode.CKAN.http.header.X-Forwarded-Host = www.myopendatanode.org
org.opendatanode.CKAN.http.header.X-Forwarded-Proto = https
```

* dependent on ckanext-odn-pipeline v0.5.1+
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

### Version history

* see [Changelog](./CHANGELOG.md)

