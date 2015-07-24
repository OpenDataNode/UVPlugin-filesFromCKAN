E-FilesFromCKAN
----------

### Documentation

* see [Plugin Documentation](./doc/About.md)
* see [Plugin Documentation](./doc/About_sk.md) (in Slovak)

### Version history

* see [Changelog](./CHANGELOG.md)


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