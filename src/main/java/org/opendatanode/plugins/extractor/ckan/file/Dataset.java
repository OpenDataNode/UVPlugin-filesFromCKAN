package org.opendatanode.plugins.extractor.ckan.file;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class Dataset implements CkanTreeItem {
    
    public String id;
    public String name;
    public String title;
    public String notes; // description
    public Organization org;
    
    public List<Resource> resources;
    
    public Dataset(JsonObject dataset) {
        this.id = dataset.getString("id");
        this.name = dataset.getString("name", "");
        this.title = dataset.getString("title", "");
        this.notes = dataset.getString("notes", "");
        
        this.resources = new ArrayList<Resource>();
        
        loadResources(dataset.getJsonArray("resources"));
        try {
            loadOrganization(dataset.getJsonObject("organization"));
        } catch (ClassCastException e) {
            // no organization -> ignore
        }
    }
    
    public void loadResources(JsonArray resources) {
        if (resources != null) {
            this.resources.clear();
            for (JsonObject resource : resources.getValuesAs(JsonObject.class)) {
                this.resources.add(new Resource(resource, this.id));
            }
        }
    }
    
    private void loadOrganization(JsonObject org) {
        if (org != null) {
            JsonObject new_name = (JsonObject) org;
            this.org = new Organization(new_name);
        }
    }
    
    @Override
    public String toString() {
        return title != null && !title.isEmpty() ? title : name;
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
        return notes;
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
        Dataset other = (Dataset) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
