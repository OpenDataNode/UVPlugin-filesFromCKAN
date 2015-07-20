package org.opendatanode.plugins.extractor.ckan.file;

import java.util.Map;

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
import eu.unifiedviews.plugins.extractor.ckan.file.FilesFromCkanConfig_V1;

@DPU.AsExtractor
public class FilesFromCkan extends AbstractDpu<FilesFromCkanConfig_V1> {

    private static final Logger LOG = LoggerFactory.getLogger(FilesFromCkan.class);

    public static final String CONFIGURATION_SECRET_TOKEN = "dpu.uv-e-filesFromCKAN.secret.token";

    public static final String CONFIGURATION_CATALOG_API_LOCATION = "dpu.uv-e-filesFromCKAN.catalog.api.url";

    @DataUnit.AsOutput(name = "output")
    public WritableFilesDataUnit filesOutput;

    private DPUContext context;

    public FilesFromCkan() {
        super(FilesFromCkanVaadinDialog.class, ConfigHistory.noHistory(FilesFromCkanConfig_V1.class));
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
        final String token = environment.get(CONFIGURATION_SECRET_TOKEN);
        final String catalogApiLocation = environment.get(CONFIGURATION_CATALOG_API_LOCATION);

        if (token == null || token.isEmpty()) {
            throw ContextUtils.dpuException(this.ctx, "errors.token.missing");
        }

        if (catalogApiLocation == null || catalogApiLocation.isEmpty()) {
            throw ContextUtils.dpuException(this.ctx, "errors.api.missing");
        }

        CatalogApiConfig apiConfig = new CatalogApiConfig(catalogApiLocation, pipelineId, userId, token);

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
