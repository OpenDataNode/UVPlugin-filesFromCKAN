package org.opendatanode.plugins.extractor.ckan.file;

import javax.json.JsonObject;

public class ResourceItem implements CkanTreeItem {
    
    public String packageId; // not necessary ?
    public String id;
    public String name;
    public String description;
    public String url;
    
    public ResourceItem(JsonObject resource, String packageId) {
        this.packageId = packageId;
        this.id = resource.getString("id");
        this.name = resource.getString("name", "");
        this.description = resource.getString("description", "");
        this.url = resource.getString("url", "");
    }
    
    /**
     * 
     * @param id
     * @param packageId
     */
    public ResourceItem(String id, String packageId) {
        // these parameters are needed for config
        this.id = id;
        this.packageId = packageId;
    }

    @Override
    public String toString() {
        if (name != null && !name.isEmpty()) {
            return name;
        } else {
            return "Unnamed resource";
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceItem other = (ResourceItem) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}