package org.opendatanode.plugins.extractor.ckan.file;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.files.FilesDataUnit.Entry;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUContext;
import eu.unifiedviews.dpu.DPUContext.MessageType;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dataunit.files.FilesDataUnitUtils;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.helpers.dpu.exec.AbstractDpu;

@DPU.AsExtractor
public class FilesFromCkan extends AbstractDpu<FilesFromCkanConfig_V2> {

    private static final Logger LOG = LoggerFactory.getLogger(FilesFromCkan.class);

    public static final String CONFIGURATION_SECRET_TOKEN = "org.opendatanode.CKAN.secret.token";

    public static final String CONFIGURATION_CATALOG_API_LOCATION = "org.opendatanode.CKAN.api.url";

    public static final String CONFIGURATION_HTTP_HEADER = "org.opendatanode.CKAN.http.header.";

    @DataUnit.AsOutput(name = "output")
    public WritableFilesDataUnit filesOutput;

    private DPUContext context;

    public FilesFromCkan() {
        super(FilesFromCkanVaadinDialog.class,
                ConfigHistory.history(FilesFromCkanConfig_V2.class)
                    .alternative(FilesFromCkanConfig_V1.class)
                    .addCurrent(FilesFromCkanConfig_V2.class));
    }

    @Override
    protected void innerExecute() throws DPUException {
        this.context = this.ctx.getExecMasterContext().getDpuContext();
        String shortMessage = this.ctx.tr("dpu.ckan.starting", this.getClass().getSimpleName());
        String longMessage = String.valueOf(this.config);
        this.context.sendMessage(DPUContext.MessageType.INFO, shortMessage, longMessage);

        Map<String, String> environment = this.context.getEnvironment();
        final long pipelineId = this.context.getPipelineId();
        final String userId = this.context.getPipelineExecutionOwnerExternalId();

        String token = environment.get(CONFIGURATION_SECRET_TOKEN);
        if (StringUtils.isEmpty(token)) {
            LOG.debug("Missing global configuration property {} for CKAN secret token", CONFIGURATION_SECRET_TOKEN);
            throw ContextUtils.dpuException(this.ctx, "errors.token.missing");
        }

        String catalogApiLocation = environment.get(CONFIGURATION_CATALOG_API_LOCATION);
        if (StringUtils.isEmpty(catalogApiLocation)) {
            LOG.debug("Missing global configuration property {} for CKAN API location", CONFIGURATION_CATALOG_API_LOCATION);
            throw ContextUtils.dpuException(this.ctx, "errors.api.missing");
        }

        Map<String, String> additionalHttpHeaders = new HashMap<>();
        for (Map.Entry<String, String> configEntry : environment.entrySet()) {
            if (configEntry.getKey().startsWith(CONFIGURATION_HTTP_HEADER)) {
                String headerName = configEntry.getKey().replace(CONFIGURATION_HTTP_HEADER, "");
                String headerValue = configEntry.getValue();
                additionalHttpHeaders.put(headerName, headerValue);
            }
        }

        CatalogApiConfig apiConfig = new CatalogApiConfig(catalogApiLocation, pipelineId, userId, token, additionalHttpHeaders);

        if (ctx.canceled()) {
            throw ContextUtils.dpuExceptionCancelled(ctx);
        }

        final String packageId = config.getPackageId();
        final String resourceId = config.getResourceId();
        final String fileName = config.getFileName() == null || config.getFileName().isEmpty()
                ? resourceId
                : config.getFileName();

        try {
            LOG.debug("Downloading CKAN resource with id {} from package / dataset with id {}", packageId, packageId);
            Entry destinationFile = FilesDataUnitUtils.createFile(filesOutput, fileName);
            FilesFromCkanHelper.resourceDownload(apiConfig, packageId, resourceId, destinationFile.getFileURIString());
        } catch (DataUnitException e) {
            ContextUtils.dpuException(ctx, e, "Failed to create destination file.");
        } catch (Exception e) {
            String errMsg = "Failed to download file.";
            LOG.error(errMsg, e);
            this.context.sendMessage(MessageType.ERROR, errMsg, e.getMessage());
        }
    }
}
