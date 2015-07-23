package org.opendatanode.plugins.extractor.ckan.file;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.opendatanode.plugins.extractor.ckan.file.FilesFromCkanConfig_V2;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.config.VersionedConfig;

public class FilesFromCkanConfig_V1 implements VersionedConfig<FilesFromCkanConfig_V2> {
    
    private String fileName;
    
    private String packageId;
    
    private String resourceId;

    public FilesFromCkanConfig_V1() {
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

    @Override
    public FilesFromCkanConfig_V2 toNextVersion() throws DPUConfigException {
        FilesFromCkanConfig_V2 v2 = new FilesFromCkanConfig_V2();
        v2.setFileName(fileName);
        v2.setPackageId(packageId);
        v2.setResourceId(resourceId);
        v2.setShowOnlyMyOrgDatasets(true);
        return v2;
    }
}
