package eu.unifiedviews.plugins.extractor.ckan.file;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;

@SuppressWarnings("serial")
public class FilesFromCkanVaadinDialog extends AbstractDialog<FilesFromCkanConfig_V1> {
    
    private static final Logger LOG = LoggerFactory.getLogger(FilesFromCkanVaadinDialog.class);

    private static final int COMPONENT_WIDTH_PERCENTAGE = 75;
    
    private Resource resource;
    
    final private ObjectProperty<String> fileName = new ObjectProperty<String>("");

    private Tree datasetResourceTree;

    private TextArea logs;

    private Label loadingLabel = null;
    
    private String apiUrl = null;
    private String token = null;
    private String userExternalId = null;

    private Map<String, String> additionalHttpHeaders = null;
    
    public FilesFromCkanVaadinDialog() {
        super(FilesFromCkan.class);
    }
    
    private void addLine(final TextArea logs, String line, String line2) {
        String oldValue = logs.getValue();
        if (oldValue == null || oldValue.isEmpty()) {
            logs.setValue(line);
        } else {
            logs.setValue(logs.getValue() + "\n" + line);
        }
        
        if (line2 != null && !line2.isEmpty()) {
            logs.setValue(logs.getValue() + "\nError: " + line2);
        }
        
        // scroll to end
        logs.setCursorPosition(logs.getValue().length());
    }

    @Override
    protected void buildDialogLayout() {
        Map<String, String> env = this.getContext().getEnvironment();
        apiUrl = env.get(FilesFromCkan.CONFIGURATION_CATALOG_API_LOCATION);
        token = env.get(FilesFromCkan.CONFIGURATION_SECRET_TOKEN);
        userExternalId = this.getContext().getUserExternalId();
        additionalHttpHeaders = new HashMap<>();
        for (Map.Entry<String, String> configEntry : env.entrySet()) {
            if (configEntry.getKey().startsWith(FilesFromCkan.CONFIGURATION_DPU_HTTP_HEADER)) {
                String headerName = configEntry.getKey().replace(FilesFromCkan.CONFIGURATION_DPU_HTTP_HEADER, "");
                String headerValue = configEntry.getValue();
                additionalHttpHeaders.put(headerName, headerValue);
            }
        }

        setSizeFull();
//        final FormLayout mainLayout = new FormLayout();
        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setHeight("-1px");
        mainLayout.setMargin(true);
//        mainLayout.setSpacing(true);
//        mainLayout.setMargin(new MarginInfo(true, true, false, true));
        
        mainLayout.setWidth(100, Unit.PERCENTAGE);

        logs = new TextArea(ctx.tr("CkanVaadinDialog.logs.label"));
        logs.setValue("");
        logs.setWidth(COMPONENT_WIDTH_PERCENTAGE, Unit.PERCENTAGE);
        logs.setHeight(100, Unit.PIXELS);
        logs.setEnabled(false);
        
        final Label horrizontalLine = new Label("<hr/>", ContentMode.HTML);
        horrizontalLine.setWidth(COMPONENT_WIDTH_PERCENTAGE, Unit.PERCENTAGE);
        
        TextField fileNameTextField = new TextField(ctx.tr("CkanVaadinDialog.filename.label"), fileName);
        fileNameTextField.setInputPrompt("my_file.csv");
        fileNameTextField.setWidth(COMPONENT_WIDTH_PERCENTAGE, Unit.PERCENTAGE);
        
        final Button expandAllButton = new Button(ctx.tr("CkanVaadinDialog.tree.button.expand.all"));
        final Button collapseAllButton = new Button(ctx.tr("CkanVaadinDialog.tree.button.collapse.all"));
        final HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setHeight("-1px");
        buttonsLayout.setMargin(true);
        buttonsLayout.setMargin(new MarginInfo(true, false, true, false));
        buttonsLayout.setWidth(250, Unit.PIXELS);
        
        loadingLabel = new Label(ctx.tr("CkanVaadinDialog.tree.loading.label"));
        
        final TextField filterField = new TextField(ctx.tr("CkanVaadinDialog.filter.label"));
        filterField.setWidth(COMPONENT_WIDTH_PERCENTAGE, Unit.PERCENTAGE);
        datasetResourceTree = new Tree(ctx.tr("CkanVaadinDialog.tree.label"));
        // setting up for filtering
        datasetResourceTree.setMultiSelect(false);
        datasetResourceTree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        datasetResourceTree.setItemCaptionPropertyId("caption");
        datasetResourceTree.addContainerProperty("caption", String.class, "");

        // tree item tooltip generator
        datasetResourceTree.setItemDescriptionGenerator(new ItemDescriptionGenerator() {
            
            @Override
            public String generateDescription(Component source, Object itemId, Object propertyId) {
                CkanTreeItem item = (CkanTreeItem) itemId;
                StringBuilder descr = new StringBuilder();
                descr.append("<p>");
                descr.append(item.getDescription() == null || item.getDescription().isEmpty()
                        ? ctx.tr("CkanVaadinDialog.tree.item.tooltip.no_description")
                        : item.getDescription());
                
                if (item instanceof Resource) {
                    Resource res = (Resource) item;
                    descr.append("<p>");
                    descr.append("URL:</br>");
                    descr.append(res.url).append("</p>");
                }
                
                return descr.toString();
            }
        });
        
        // filter listener
        filterField.addTextChangeListener(new FieldEvents.TextChangeListener() {
            
            @Override
            public void textChange(TextChangeEvent event) {
                
                String filterText = event.getText();
                
                HierarchicalContainer ds = (HierarchicalContainer) datasetResourceTree.getContainerDataSource();
                ds.removeAllContainerFilters();
                
                if (!filterText.isEmpty()) {
                    SimpleStringFilter filter = new SimpleStringFilter("caption", event.getText(), true, false);
                    ds.addContainerFilter(filter);
                }
            }
        });
        
        // expanding / collapsing item when clicking dataset name
        datasetResourceTree.addItemClickListener(new ItemClickListener() {
            
            @Override
            public void itemClick(ItemClickEvent event) {
                Object itemId = event.getItemId();
                
                if (datasetResourceTree.hasChildren(itemId)) {
                    if (datasetResourceTree.isExpanded(itemId)) {
                        datasetResourceTree.collapseItem(itemId);
                    } else {
                        datasetResourceTree.expandItem(itemId);
                    }
                }
            }
        });
        
        // on selecting resource => change resource saved to dpu config
        datasetResourceTree.addValueChangeListener(new ValueChangeListener() {
            
            @Override
            public void valueChange(ValueChangeEvent event) {
                CkanTreeItem item = (CkanTreeItem) datasetResourceTree.getValue();
                
                if (item != null) {
                    
                    if (item instanceof Resource) {
                        resource = (Resource) item;
                        addLine(logs, ctx.tr("CkanVaadinDialog.logs.item.selection.changed") + resource, null);
                    }
                }
            }
        });
        
        expandAllButton.addClickListener(new Button.ClickListener() {
            
            @Override
            public void buttonClick(ClickEvent event) {
                for (Object itemId : datasetResourceTree.getItemIds()) {
                    datasetResourceTree.expandItem(itemId);
                }
            }
        });
        
        collapseAllButton.addClickListener(new Button.ClickListener() {
            
            @Override
            public void buttonClick(ClickEvent event) {
                for (Object itemId : datasetResourceTree.getItemIds()) {
                    datasetResourceTree.collapseItem(itemId);
                }
            }
        });
        
        buttonsLayout.addComponent(expandAllButton);
        buttonsLayout.addComponent(collapseAllButton);
//        buttonsLayout.addComponent(filterField);
        mainLayout.addComponent(fileNameTextField);
        mainLayout.addComponent(buttonsLayout);
        mainLayout.addComponent(filterField);
        mainLayout.addComponent(datasetResourceTree);
        mainLayout.addComponent(loadingLabel);
        mainLayout.addComponent(horrizontalLine);
        mainLayout.addComponent(logs);
        setCompositionRoot(mainLayout);
    }

    private void setPreviouslySelectedResource() {
        if (resource == null) {
            return;
        }
        
        Item resItem = datasetResourceTree.getItem(resource);
        
        if (resItem == null) {
            return;
        }
        
        datasetResourceTree.setValue(resItem);
        addLine(logs, ctx.tr("CkanVaadinDialog.selected.item.log", resItem.toString()), null);
        
        Object datasetItemId = datasetResourceTree.getParent(resource);
        datasetResourceTree.expandItem(datasetItemId);
        
        Object orgItemId = datasetResourceTree.getParent(datasetItemId);
        if (orgItemId != null) {
            datasetResourceTree.expandItem(orgItemId);
        }
    }

    private CatalogApiConfig getApiConfig() {
        CatalogApiConfig apiConfig = new CatalogApiConfig(apiUrl, -1, userExternalId, token, new HashMap<String, String>());
        
        if (apiUrl == null || apiUrl.isEmpty()) {
            addLine(logs, ctx.tr("errors.api.missing", userExternalId), null);
            return null;
        }
        
        if (token == null || token.isEmpty()) {
            addLine(logs, ctx.tr("errors.token.missing", userExternalId), null);
            return null;
        }
        
        return apiConfig;
    }
    
    private List<Dataset> getPackages(CatalogApiConfig apiConfig) {
        if (apiConfig == null) {
            return null;
        }
        
        try {
            return FilesFromCkanHelper.getPackageListWithResources(apiConfig);
        } catch (Exception e) {
            String errMsg = ctx.tr("errors.failed.retrieve.datasets");
            LOG.error(errMsg, e);
            addLine(logs, errMsg, e.getMessage());
            return null;
        }
    }
    
    private Organization getLoggedUserOrganization(CatalogApiConfig apiConfig) {
        if (apiConfig == null) {
            return null;
        }
        
        try {
            return FilesFromCkanHelper.getOrganization(apiConfig, userExternalId);
        } catch (Exception e) {
            String errMsg = ctx.tr("errors.failed.retrieve.org.datasets", userExternalId);
            LOG.warn(errMsg + ": " + e.getMessage());
            addLine(logs, errMsg, e.getMessage());
            return null;
        }
    }

    private void fillTree() {
        // config information
        CatalogApiConfig apiConfig = getApiConfig();
        List<Dataset> packages = getPackages(apiConfig);
        Organization myOrg = getLoggedUserOrganization(apiConfig);
        
        
        // fill the tree
        datasetResourceTree.removeAllItems();
        
        // add private datasets of my organization
        String exludeOrg = null;
        if (myOrg != null) {
            exludeOrg = myOrg.id;
            for (Dataset dataset : myOrg.datasets) {
                addTreeItem(dataset, null, dataset.resources.size() != 0);
                for (Resource res : dataset.resources) {
                    addTreeItem(res, dataset, false);
                }
            }
        }
        
        if (packages == null) {
            return;
        }
        
        // add public datasets
        for (Dataset dataset : packages) {
            if ((dataset.org != null && exludeOrg == dataset.org.id)
                    || datasetResourceTree.getItem(dataset) != null) {
                continue; // skip my org's datasets (already added)
            }
            addTreeItem(dataset, null, dataset.resources.size() != 0);
            
            for (Resource res : dataset.resources) {
                addTreeItem(res, dataset, false);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addTreeItem(CkanTreeItem item, CkanTreeItem parent, boolean childrenAllowed) {
        
        // adding item and property for filtering
        datasetResourceTree.addItem(item).getItemProperty("caption").setValue(item.toString());
        datasetResourceTree.setChildrenAllowed(item, childrenAllowed);
        
        if (item instanceof Dataset) {
            Dataset dataset = (Dataset) item;
            setDatasetParent(datasetResourceTree, dataset);
        } else {
            datasetResourceTree.setParent(item, parent);
        }
    }

    @SuppressWarnings("unchecked")
    private void setDatasetParent(Tree tree, Dataset dataset) {
        Organization orgItemId = dataset.org; 
        
        if (dataset.org == null) {
            orgItemId = new Organization("no-org", ctx.tr("CkanVaadinDialog.tree.org.for.datasets.without.org.label"));
        }
        
        Item orgItem = tree.getItem(orgItemId);
        if (orgItem == null) {
            tree.addItem(orgItemId).getItemProperty("caption").setValue(orgItemId.toString());
        }
        
        tree.setParent(dataset, orgItemId);
    }

    @Override
    protected FilesFromCkanConfig_V1 getConfiguration() throws DPUConfigException {
        FilesFromCkanConfig_V1 result = new FilesFromCkanConfig_V1();
        if (resource != null) {
            result.setPackageId(resource.packageId);
            result.setResourceId(resource.id);
        }
        
        result.setFileName(fileName.getValue());
        
        return result;
    }

    @Override
    protected void setConfiguration(FilesFromCkanConfig_V1 config) throws DPUConfigException {
        
        if (config.getResourceId() != null && !config.getResourceId().isEmpty()) {
            resource = new Resource(config.getResourceId(), config.getPackageId());
        }
        
        fileName.setValue(config.getFileName() == null ? "" : config.getFileName());
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                fillTree();
                setPreviouslySelectedResource();
                datasetResourceTree.markAsDirty(); // repaint tree
                loadingLabel.setVisible(false);
            }
        }).start();
    }
}