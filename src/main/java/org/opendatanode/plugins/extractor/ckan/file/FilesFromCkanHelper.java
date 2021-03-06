package org.opendatanode.plugins.extractor.ckan.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue.ValueType;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.helpers.dataunit.resource.Resource;
import eu.unifiedviews.helpers.dataunit.resource.ResourceConverter;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.UserDialogContext;

public class FilesFromCkanHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FilesFromCkanHelper.class);

    /**
     * <p>
     * Http connect timeout for request to CKAN.<br/>
     * Limits how long it tries to connect to CKAN, not how long the request can take.
     * </p>
     * <p>
     * unit: miliseconds
     * </p>
     */
    public static final int CONNECT_TIMEOUT = 5 * 1000;

    public static final String PROXY_API_ACTION = "action";

    public static final String PROXY_API_PIPELINE_ID = "pipeline_id";

    public static final String PROXY_API_USER_ID = "user_id";

    public static final String PROXY_API_TOKEN = "token";

    public static final String PROXY_API_DATA = "data";

    private static final String API_ACTION_ORG_SHOW = "organization_show";

    private static final String API_ACTION_PACKAGES_WITH_RESOURCES = "current_package_list_with_resources";

    private static final String API_ACTION_PCK_SHOW = "package_show";

    private static final String API_ACTION_RESOURCE_SHOW = "resource_show";

    private static final String ACTION_RESOURCE_DOWNLOAD = "resource_download";
    
    /**
     * WARN: 2.4.x API change its default value is false
     */
    private static final String PARAM_INCLUDE_DATASETS = "include_datasets";
    
    private static final String PARAM_ID = "id";
    
    private static final String PARAM_PACKAGE_ID = "package_id";

    private UserDialogContext ctx; // needed for i18n

    public FilesFromCkanHelper(UserDialogContext ctx) {
        this.ctx = ctx;
    }

    public static JsonObject buildJSON(Map<String, Object> values) {
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        JsonObjectBuilder builder = factory.createObjectBuilder();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            builder.add(entry.getKey(), entry.getValue().toString());
        }
        return builder.build();
    }

    public static List<DatasetItem> getPackageListWithResources(CatalogApiConfig apiConfig) throws Exception {
        List<DatasetItem> datasetsList = new ArrayList<DatasetItem>();

        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        FileOutputStream fos = null;

        Map<String, String> additionalHttpHeaders = apiConfig.getAdditionalHttpHeaders();

        try {
            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
                httpPost.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
            }
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).build());

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, API_ACTION_PACKAGES_WITH_RESOURCES, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_DATA, "{}", ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();

            httpPost.setEntity(entity);
            response = client.execute(httpPost);

            checkResponseIsJSON(response);

            JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
            JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
            JsonObject responseJson = reader.readObject();

            checkResponseSuccess(responseJson);

            JsonArray datasets = responseJson.getJsonArray("result");
            for (JsonObject dataset : datasets.getValuesAs(JsonObject.class)) {
                datasetsList.add(new DatasetItem(dataset));
            }
        } finally {
            FilesFromCkanHelper.tryCloseHttpResponse(response);
            FilesFromCkanHelper.tryCloseHttpClient(client);
            if (fos != null) {
                fos.close();
            }
        }
        return datasetsList;
    }

    /**
     * <p>
     * Return organization with its public datasets and if the user is member, it also includes private datasets
     * <p>
     * <p>
     * WARNING: the datasets have no resources loaded, for this purposet use CkanHelper.getOrganizationWithDatasetAndResources
     * </p>
     * 
     * @param ckanApiUrl
     * @param orgId
     * @return
     * @throws Exception
     */
    public static OrganizationItem getOrganization(CatalogApiConfig apiConfig, String orgId) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        Map<String, String> additionalHttpHeaders = apiConfig.getAdditionalHttpHeaders();

        try {

            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
                httpPost.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
            }
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).build());

            Map<String, Object> values = new HashMap<String, Object>(2);
            values.put(PARAM_ID, orgId);
            values.put(PARAM_INCLUDE_DATASETS, true);
            String data = buildJSON(values).toString();

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, API_ACTION_ORG_SHOW, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_DATA, data, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();

            httpPost.setEntity(entity);
            response = client.execute(httpPost);

            checkResponseIsJSON(response);

            JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
            JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
            JsonObject responseJson = reader.readObject();

            checkResponseSuccess(responseJson);

            JsonObject orgInfo = responseJson.getJsonObject("result");
            final OrganizationItem org = new OrganizationItem(orgInfo);
            
            loadResources(apiConfig, org);
            
            return org;
        } finally {
            FilesFromCkanHelper.tryCloseHttpResponse(response);
            FilesFromCkanHelper.tryCloseHttpClient(client);
        }
    }
    
    /**
     * Checks if resources were returned by organization_show API call
     * <br/><br/>
     * There was a change in CKAN API from version 2.2.x to 2.3<br/>
     * In version 2.3 it no longer returns dataset resources
     * 
     * @param apiConfig
     * @param org
     * @throws Exception
     */
    private static void loadResources(CatalogApiConfig apiConfig, OrganizationItem org) throws Exception {
        DatasetItem act_dataset;
        boolean resourcesNotLoaded = false;
        for (DatasetItem dataset : org.datasets) {
            resourcesNotLoaded = dataset.numOfResources > dataset.resources.size();
            if (resourcesNotLoaded) {
                act_dataset = getDataset(apiConfig, dataset.id);
                dataset.resources = act_dataset.resources;
            }
        }
    }

    /**
     * @param apiConfig
     * @param datasetIdOrName
     * @return
     * @throws Exception
     */
    public static DatasetItem getDataset(CatalogApiConfig apiConfig, String datasetIdOrName) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        Map<String, String> additionalHttpHeaders = apiConfig.getAdditionalHttpHeaders();

        try {

            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
                httpPost.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
            }
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).build());

            Map<String, Object> values = new HashMap<String, Object>(2);
            values.put(PARAM_ID, datasetIdOrName);
            String data = buildJSON(values).toString();

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, API_ACTION_PCK_SHOW, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_DATA, data, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();

            httpPost.setEntity(entity);
            response = client.execute(httpPost);

            checkResponseIsJSON(response);

            JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
            JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
            JsonObject responseJson = reader.readObject();

            checkResponseSuccess(responseJson);

            return new DatasetItem(responseJson.getJsonObject("result"));
        } finally {
            FilesFromCkanHelper.tryCloseHttpResponse(response);
            FilesFromCkanHelper.tryCloseHttpClient(client);
        }
    }

    public static void resourceDownload(CatalogApiConfig apiConfig, String packageId, String resourceId, String fileURI) throws Exception {
        // has to have lax redirect strategy, because the resource can link
        // to file outside of CKAN and because of that is redirected by CKAN
        // when making API call for download
        CloseableHttpClient client = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy()).build();
        CloseableHttpResponse response = null;
        FileOutputStream fos = null;
        Map<String, String> additionalHttpHeaders = apiConfig.getAdditionalHttpHeaders();

        try {
            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            for (Map.Entry<String, String> additionalHeader : additionalHttpHeaders.entrySet()) {
                httpPost.addHeader(additionalHeader.getKey(), additionalHeader.getValue());
            }
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).build());

            Map<String, Object> values = new HashMap<String, Object>(2);
            values.put(PARAM_ID, resourceId);
            values.put(PARAM_PACKAGE_ID, packageId);
            String data = buildJSON(values).toString();

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, ACTION_RESOURCE_DOWNLOAD, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_DATA, data, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();

            httpPost.setEntity(entity);
            response = client.execute(httpPost);

            checkResponseCodeOk(response);

            LOG.debug("Downloading file");
            // download the file ...
            entity = response.getEntity();
            LOG.debug("downloaded file content-type: " + ContentType.get(entity));
            fos = new FileOutputStream(new File(URI.create(fileURI)));
            IOUtils.copy(entity.getContent(), fos);
            fos.flush();

        } finally {
            FilesFromCkanHelper.tryCloseHttpResponse(response);
            FilesFromCkanHelper.tryCloseHttpClient(client);
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static Resource getResourceMetadata(CatalogApiConfig apiConfig, String resourceId) throws Exception {

        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        Resource resource = null;

        try {
            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).build());

            Map<String, Object> values = new HashMap<String, Object>(2);
            values.put(PARAM_ID, resourceId);
            String data = buildJSON(values).toString();

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, API_ACTION_RESOURCE_SHOW, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_DATA, data, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();

            httpPost.setEntity(entity);
            response = client.execute(httpPost);

            checkResponseCodeOk(response);

            JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
            JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
            JsonObject responseJson = reader.readObject();

            checkResponseSuccess(responseJson);
            resource = getResourceFromCkanResponse(responseJson.getJsonObject("result"));

        } finally {
            tryCloseHttpResponse(response);
            tryCloseHttpClient(client);
        }

        return resource;
    }

    private static Resource getResourceFromCkanResponse(JsonObject showResourceResult) throws Exception {
        Set<String> jsonKeys = showResourceResult.keySet();
        Map<String, String> resourceMap = new HashMap<>();
        for (String key : jsonKeys) {
        	if (PARAM_ID.equalsIgnoreCase(key)) {
        		// ID metadata would cause creation of resource with existing ID, so skip 
				continue;
			}
        	
            if (showResourceResult.get(key).getValueType() == ValueType.STRING) {
                resourceMap.put(key, showResourceResult.getString(key));
            }
        }
        return ResourceConverter.resourceFromMap(resourceMap);

    }

    private static void checkResponseSuccess(JsonObject responseJson) throws Exception {
        boolean bSuccess = responseJson.getBoolean("success");

        if (!bSuccess) {
            String errorType = responseJson.getJsonObject("error").getString("__type");
            String errorMessage = responseJson.getJsonObject("error").getString("message");
            throw new Exception(String.format("CKAN error response: [%s] %s", errorType, errorMessage));
        }
    }

    private static void checkResponseCodeOk(CloseableHttpResponse response) throws Exception {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new Exception("HttpError " + statusCode + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private static void checkResponseIsJSON(CloseableHttpResponse response) throws Exception {
        if (ContentType.get(response.getEntity()).getMimeType().equalsIgnoreCase("application/json")) {
            return;
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new Exception("HttpError " + statusCode + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    public static void tryCloseHttpClient(CloseableHttpClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                LOG.warn("Failed to close HTTP client", e);
            }
        }
    }

    public static void tryCloseHttpResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                LOG.warn("Failed to close HTTP response", e);
            }
        }
    }
}
