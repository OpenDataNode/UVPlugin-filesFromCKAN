package org.opendatanode.plugins.extractor.ckan.file;

public interface CkanTreeItem {

    public String getId();
    public String getName();
    /**
     * 
     * @return String showed in tooltip of the tree item
     */
    public String getDescription();
    /**
     * 
     * @return String showed as "name" of the tree item
     */
    public String toString();
}
