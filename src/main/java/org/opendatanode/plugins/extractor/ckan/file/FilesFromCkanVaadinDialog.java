package org.opendatanode.plugins.extractor.ckan.file;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.ItemSorter;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.Button.ClickEvent;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;

@SuppressWarnings("serial")
public class FilesFromCkanVaadinDialog extends AbstractDialog<FilesFromCkanConfig_V2> {

    private static final Logger LOG = LoggerFactory.getLogger(FilesFromCkanVaadinDialog.class);

    private static final int COMPONENT_WIDTH_PERCENTAGE = 75;

    private ResourceItem resource;

    final private ObjectProperty<String> fileName = new ObjectProperty<String>("");

    final private ObjectProperty<Boolean> showOnlyMyDatasets = new ObjectProperty<Boolean>(true);

    private CheckBox showOnlyMyOwnDatasetCheckbox; // needing this too for enabling/disabling

    private Tree datasetResourceTree;

    private ProgressBar loadingBar = null;

    private String apiUrl = null;

    private String token = null;

    private String userExternalId = null;

    private Map<String, String> additionalHttpHeaders = null;

    private OrganizationItem myOrg = null;

    private List<DatasetItem> publicDatasets = null;

    private boolean triedLoadingOrg = false;

    private Label errorLabel;

    private Label catalogErrorLabel;

    private Collator collator;

    private static final String CONFIGURATION_LOCALE = "locale";

    public FilesFromCkanVaadinDialog() {
        super(FilesFromCkan.class);
    }

    @Override
    protected void buildDialogLayout() {
        Map<String, String> env = this.getContext().getEnvironment();
        apiUrl = env.get(FilesFromCkan.CONFIGURATION_CATALOG_API_LOCATION);
        token = env.get(FilesFromCkan.CONFIGURATION_SECRET_TOKEN);
        userExternalId = this.getContext().getUserExternalId();
        additionalHttpHeaders = new HashMap<>();
        for (Map.Entry<String, String> configEntry : env.entrySet()) {
            if (configEntry.getKey().startsWith(FilesFromCkan.CONFIGURATION_HTTP_HEADER)) {
                String headerName = configEntry.getKey().replace(FilesFromCkan.CONFIGURATION_HTTP_HEADER, "");
                String headerValue = configEntry.getValue();
                additionalHttpHeaders.put(headerName, headerValue);
            }
        }
        String localeString = env.get(CONFIGURATION_LOCALE);
        this.collator = Collator.getInstance(Locale.forLanguageTag(localeString));
        this.collator.setStrength(Collator.SECONDARY);

        setSizeFull();
        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setHeight("-1px");
        mainLayout.setMargin(true);

        mainLayout.setWidth(100, Unit.PERCENTAGE);

        showOnlyMyOwnDatasetCheckbox = new CheckBox(ctx.tr("CkanVaadinDialog.show.my.datasets.label"), showOnlyMyDatasets);
        showOnlyMyOwnDatasetCheckbox.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                // fill the tree
                datasetResourceTree.removeAllItems();

                loadingBar.setVisible(true);
                showOnlyMyOwnDatasetCheckbox.setEnabled(false);

                final LoadThread thread = new LoadThread();
                thread.start();
            }
        });

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

        loadingBar = new ProgressBar();
        loadingBar.setIndeterminate(true);

        final TextField filterField = new TextField();
        filterField.setInputPrompt(ctx.tr("CkanVaadinDialog.filter.tooltip"));
        filterField.setWidth(COMPONENT_WIDTH_PERCENTAGE, Unit.PERCENTAGE);
        datasetResourceTree = new Tree(ctx.tr("CkanVaadinDialog.tree.label"));
        datasetResourceTree.setImmediate(true);
        // setting up for filtering
        datasetResourceTree.setMultiSelect(false);
        datasetResourceTree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        datasetResourceTree.setItemCaptionPropertyId("caption");
        datasetResourceTree.addContainerProperty("caption", String.class, "");

        // sort alphabetically asc 
        ((HierarchicalContainer) datasetResourceTree.getContainerDataSource()).setItemSorter(new ItemSorter() {

            @Override
            public void setSortProperties(Sortable container, Object[] propertyId, boolean[] ascending) {
            }

            @Override
            public int compare(Object itemId1, Object itemId2) {
                if (itemId1 instanceof CkanTreeItem && itemId2 instanceof CkanTreeItem) {
                    String i1 = ((CkanTreeItem) itemId1).toString();
                    String i2 = ((CkanTreeItem) itemId2).toString();

                    return collator.compare(i1, i2);
                }
                return 0;
            }
        });

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

                if (item instanceof ResourceItem) {
                    ResourceItem res = (ResourceItem) item;
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

                    if (item instanceof ResourceItem) {
                        resource = (ResourceItem) item;
                        errorLabel.setVisible(false);
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

        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setStyleName("dpu-error-label");

        catalogErrorLabel = new Label("", ContentMode.HTML);
        errorLabel.setVisible(false);
        catalogErrorLabel.setStyleName("dpu-error-label");

        buttonsLayout.addComponent(expandAllButton);
        buttonsLayout.addComponent(collapseAllButton);
        mainLayout.addComponent(catalogErrorLabel);
        mainLayout.addComponent(errorLabel);
        mainLayout.addComponent(showOnlyMyOwnDatasetCheckbox);
        mainLayout.addComponent(buttonsLayout);
        mainLayout.addComponent(filterField);
        mainLayout.addComponent(datasetResourceTree);
        mainLayout.addComponent(loadingBar);
        mainLayout.addComponent(fileNameTextField);
        setCompositionRoot(mainLayout);
    }

    private void setPreviouslySelectedResource() {
        if (resource == null) {
            errorLabel.setValue(ctx.tr("errors.resource.not.selected"));
            errorLabel.setVisible(true);
            return;
        }

        datasetResourceTree.setValue(resource);

        if (datasetResourceTree.getValue() == null) {
            errorLabel.setValue(ctx.tr("errors.resource.selected.but.not.found"));
            errorLabel.setVisible(true);
            return;
        } else {
            errorLabel.setVisible(false);
        }

        Object datasetItemId = datasetResourceTree.getParent(resource);
        datasetResourceTree.expandItem(datasetItemId);

        Object orgItemId = datasetResourceTree.getParent(datasetItemId);
        if (orgItemId != null) {
            datasetResourceTree.expandItem(orgItemId);
        }
    }

    private CatalogApiConfig getApiConfig() {
        CatalogApiConfig apiConfig = new CatalogApiConfig(apiUrl, -1, userExternalId, token, additionalHttpHeaders);
        catalogErrorLabel.setVisible(!catalogErrorLabel.getValue().isEmpty());

        if (apiUrl == null || apiUrl.isEmpty()) {
            addLineToCatalogErrorLabel(ctx.tr("errors.api.missing", userExternalId));
            return null;
        }

        if (token == null || token.isEmpty()) {
            addLineToCatalogErrorLabel(ctx.tr("errors.token.missing", userExternalId));
            return null;
        }

        return apiConfig;
    }

    private void addLineToCatalogErrorLabel(String line) {
        String prevValue = catalogErrorLabel.getValue();
        if (prevValue.isEmpty()) {
            catalogErrorLabel.setValue(line);
        } else if (!prevValue.contains(line)) {
            catalogErrorLabel.setValue(prevValue + "<br/>" + line);
        }
        catalogErrorLabel.setVisible(true);
    }

    private List<DatasetItem> getPackages(CatalogApiConfig apiConfig) {
        if (apiConfig == null) {
            return null;
        }

        try {
            return new FilesFromCkanHelper(ctx).getPackageListWithResources(apiConfig);
        } catch (Exception e) {
            String errMsg = ctx.tr("errors.failed.retrieve.datasets");
            LOG.error(errMsg, e);
            addLineToCatalogErrorLabel(errMsg + " " + e.getMessage());
            return null;
        }
    }

    private OrganizationItem getLoggedUserOrganization(CatalogApiConfig apiConfig) {
        if (apiConfig == null) {
            return null;
        }

        try {
            return new FilesFromCkanHelper(ctx).getOrganization(apiConfig, userExternalId);
        } catch (Exception e) {
            String errMsg = ctx.tr("errors.failed.retrieve.org.datasets", userExternalId);
            LOG.error(errMsg + ": " + e.getMessage());
            addLineToCatalogErrorLabel(errMsg + " " + e.getMessage());
            return null;
        }
    }

    private void fillTree() {
        addMyOrgDatasets();
        if (!showOnlyMyDatasets.getValue()) {
            addPublicDatasets();
        }
        // start sorting after the tree is filled
        ((HierarchicalContainer) datasetResourceTree.getContainerDataSource()).sort(null, null);
    }

    /**
     * adds private and public datasets of my organization
     */
    private void addMyOrgDatasets() {

        if (myOrg != null) {
            for (DatasetItem dataset : myOrg.datasets) {
                addTreeItem(dataset, null, dataset.resources.size() != 0);
                for (ResourceItem res : dataset.resources) {
                    addTreeItem(res, dataset, false);
                }
            }
        }
    }

    /**
     * Adds all public dataset excluding myOrg datasets,
     * because they should be already loaded
     */
    private void addPublicDatasets() {

        if (publicDatasets == null) {
            return;
        }

        String exludeOrg = myOrg == null ? null : myOrg.id;
        for (DatasetItem dataset : publicDatasets) {
            if ((dataset.org != null && exludeOrg == dataset.org.id)
                    || datasetResourceTree.getItem(dataset) != null) {
                continue; // skip my org's datasets (already added)
            }
            addTreeItem(dataset, null, dataset.resources.size() != 0);

            for (ResourceItem res : dataset.resources) {
                addTreeItem(res, dataset, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addTreeItem(CkanTreeItem item, CkanTreeItem parent, boolean childrenAllowed) {

        // adding item and property for filtering
        datasetResourceTree.addItem(item).getItemProperty("caption").setValue(item.toString());
        datasetResourceTree.setChildrenAllowed(item, childrenAllowed);

        if (item instanceof DatasetItem) {
            DatasetItem dataset = (DatasetItem) item;
            setDatasetParent(datasetResourceTree, dataset);
        } else {
            datasetResourceTree.setParent(item, parent);
        }
    }

    @SuppressWarnings("unchecked")
    private void setDatasetParent(Tree tree, DatasetItem dataset) {
        OrganizationItem orgItemId = dataset.org;

        if (dataset.org == null) {
            orgItemId = new OrganizationItem("no-org", ctx.tr("CkanVaadinDialog.tree.org.for.datasets.without.org.label"));
        }

        Item orgItem = tree.getItem(orgItemId);
        if (orgItem == null) {
            tree.addItem(orgItemId).getItemProperty("caption").setValue(orgItemId.toString());
        }

        tree.setParent(dataset, orgItemId);
    }

    private void loadData() {
        if (myOrg == null && !triedLoadingOrg) {
            load(DatasetType.MY_ORGS);
            triedLoadingOrg = true;
        }
        if (publicDatasets == null && !showOnlyMyDatasets.getValue()) {
            load(DatasetType.ALL_PUBLIC);
        }
    }

    /**
     * Contacts catalog for datasets
     * 
     * @param type
     */
    private void load(final DatasetType type) {
        final CatalogApiConfig apiConfig = getApiConfig();

        if (type == DatasetType.MY_ORGS) {
            myOrg = getLoggedUserOrganization(apiConfig);
        } else {
            publicDatasets = getPackages(apiConfig);
        }

    }

    @Override
    protected FilesFromCkanConfig_V2 getConfiguration() throws DPUConfigException {
        FilesFromCkanConfig_V2 result = new FilesFromCkanConfig_V2();
        if (resource != null) {
            result.setPackageId(resource.packageId);
            result.setResourceId(resource.id);
        }

        String fName = fileName.getValue().trim();
        if (!fName.isEmpty() && !isValidFileName(fName)) {
            throw new DPUConfigException(ctx.tr("error.file.name.not.valid"));
        }
        result.setFileName(fName);
        result.setShowOnlyMyOrgDatasets(showOnlyMyDatasets.getValue());

        return result;
    }

    @Override
    protected void setConfiguration(FilesFromCkanConfig_V2 config) throws DPUConfigException {
        if (config.getResourceId() != null && !config.getResourceId().isEmpty()) {
            resource = new ResourceItem(config.getResourceId(), config.getPackageId());
        }

        fileName.setValue(config.getFileName() == null ? "" : config.getFileName());
        triedLoadingOrg = false;
        // this triggers listener -> the tree loads
        showOnlyMyDatasets.setValue(config.isShowOnlyMyOrgDatasets());
    }

    private static boolean isValidFileName(String fileName) {
        return fileName.matches("[\\w\\.]*");
    }

    private class LoadThread extends Thread {

        @Override
        public void run() {
            loadData();

            // Update the UI thread-safely
            UI.getCurrent().access(new Runnable() {
                @Override
                public void run() {
                    // disable filters
                    Collection<Filter> oldFilters = removeFilters();

                    fillTree();
                    setPreviouslySelectedResource();

                    loadingBar.setVisible(false);
                    showOnlyMyOwnDatasetCheckbox.setEnabled(true);

                    // enable filters
                    addFilters(oldFilters);
                }
            });
        }
    }

    /**
     * Resets tree filters
     * 
     * @return
     *         removed filters
     */
    private Collection<Filter> removeFilters() {
        HierarchicalContainer ds = (HierarchicalContainer) datasetResourceTree.getContainerDataSource();

        Collection<Filter> returnValue = new ArrayList<Filter>();
        returnValue.addAll(ds.getContainerFilters());

        ds.removeAllContainerFilters();

        return returnValue;
    }

    private void addFilters(Collection<Filter> filters) {

        HierarchicalContainer ds = (HierarchicalContainer) datasetResourceTree.getContainerDataSource();
        for (Filter filter : filters) {
            ds.addContainerFilter(filter);
        }
    }
}
