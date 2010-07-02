// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.editor.properties.controllers;

import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.CorePlugin;
import org.talend.core.database.EDatabaseTypeName;
import org.talend.core.model.metadata.builder.connection.CDCConnection;
import org.talend.core.model.metadata.builder.connection.CDCType;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.designerproperties.PropertyConstants.CDCTypeMode;
import org.talend.core.model.param.ERepositoryCategoryType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.FileItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.LinkRulesItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.core.properties.tab.IDynamicProperty;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.editor.cmd.ChangeValuesFromRepository;
import org.talend.designer.core.ui.editor.cmd.PropertyChangeCommand;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.EDatabaseComponentName;
import org.talend.designer.core.ui.views.properties.MultipleThreadDynamicComposite;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.dialog.RepositoryReviewDialog;

/**
 * DOC nrousseau class global comment. Detailled comment
 */
public class PropertyTypeController extends AbstractRepositoryController {

    public PropertyTypeController(IDynamicProperty dp) {
        super(dp);
    }

    /*
     * add by wzhang
     * 
     * @see
     * org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#createControl(org.eclipse
     * .swt.widgets.Composite, org.talend.core.model.process.IElementParameter, int, int, int,
     * org.eclipse.swt.widgets.Control)
     */
    @Override
    public Control createControl(Composite subComposite, IElementParameter param, int numInRow, int nbInRow, int top,
            Control lastControl) {
        Control lastControlUsed = lastControl;
        lastControlUsed = super.createControl(subComposite, param, numInRow, nbInRow, top, lastControl);
        // add a button if the value is Built-In
        if (EmfComponent.BUILTIN.equals(param.getChildParameters().get("PROPERTY_TYPE").getValue())) { //$NON-NLS-1$
            if (param.getElement() instanceof INode) {
                if (canSaveProperty(param)) {
                    lastControlUsed = addButton(subComposite, param, lastControlUsed, numInRow, top);
                }
            }
        }

        return lastControlUsed;
    }

    /**
     * DOC wzhang Comment method "canSaveProperty".
     * 
     * @param param
     * @return
     */
    private boolean canSaveProperty(IElementParameter param) {
        INode node = (INode) param.getElement();
        //
        boolean canSaved = false;
        String componentName = node.getComponent().getName();
        for (EDatabaseComponentName eComponent : EDatabaseComponentName.values()) {
            if (componentName.equals(eComponent.getInputComponentName())
                    || componentName.equals(eComponent.getOutPutComponentName())) {
                canSaved = true;
                break;
            }
            // Teradata
            /**
             * @author wzhang. For the property in EdatabaseComponentName class is "tELTTeradataInput" and
             * "tELTTeradataOutput". So define the String variable custom.
             */
            if (componentName.equals("tTeradataInput") || componentName.equals("tTeradataOutput")) { //$NON-NLS-1$ //$NON-NLS-2$
                canSaved = true;
            }
        }
        return canSaved;
    }

    /**
     * 
     * DOC wzhang Comment method "addButton".
     * 
     * @param subComposite
     * @param param
     * @param lastControl
     * @param numInRow
     * @param top
     * @return
     */
    private Control addButton(Composite subComposite, final IElementParameter param, Control lastControl, int numInRow, int top) {

        Button button;
        Button resetBtn = null;
        Control lastControlUsed = lastControl;
        Point buttonSize;
        FormData data;
        // if (!createFile) {
        button = getWidgetFactory().createButton(subComposite, "", SWT.PUSH); //$NON-NLS-1$
        buttonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        button.setImage(ImageProvider.getImage(EImage.SAVE_ICON));
        button.setToolTipText(Messages.getString("PropertyTypeController.saveToMetadata")); //$NON-NLS-1$
        button.setData(PARAMETER_NAME, param.getName());

        lastControlUsed = button;

        button.addSelectionListener(listenerSelection);

        CLabel labelLabel = getWidgetFactory().createCLabel(subComposite, ""); //$NON-NLS-1$
        data = new FormData();
        data.left = new FormAttachment(lastControl, 0);
        data.right = new FormAttachment(lastControl, labelLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
                + (ITabbedPropertyConstants.HSPACE * 2), SWT.RIGHT);
        if (resetBtn != null) {
            data.top = new FormAttachment(resetBtn, 0, SWT.CENTER);
        } else {
            data.top = new FormAttachment(0, top);
        }
        labelLabel.setLayoutData(data);
        if (numInRow != 1) {
            labelLabel.setAlignment(SWT.RIGHT);
        }

        data = new FormData();
        data.left = new FormAttachment(labelLabel, -1);
        data.right = new FormAttachment(labelLabel, STANDARD_BUTTON_WIDTH, SWT.RIGHT);

        if (resetBtn != null) {
            data.top = new FormAttachment(resetBtn, 0, SWT.CENTER);
        } else {
            data.top = new FormAttachment(0, top);
        }
        data.height = STANDARD_HEIGHT - 2;
        button.setLayoutData(data);

        dynamicProperty.setCurRowSize(buttonSize.y + ITabbedPropertyConstants.VSPACE);
        // } else {
        //            button = getWidgetFactory().createButton(subComposite, "", SWT.NONE); //$NON-NLS-1$
        //            button.setText("select file"); //$NON-NLS-N$
        // buttonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        //            button.setToolTipText("select a drl file"); //$NON-NLS-1$
        // button.setData(PARAMETER_NAME, param.getName());
        //
        // lastControlUsed = button;
        //
        // button.addSelectionListener(new SelectionListener() {
        //
        // public void widgetDefaultSelected(SelectionEvent e) {
        // // TODO Auto-generated method stub
        //
        // }
        //
        // public void widgetSelected(SelectionEvent e) {
        // // TODO Auto-generated method stub
        // FileDialog fd = new FileDialog(new Shell());
        // String fileName = fd.open();
        // }
        //
        // });
        //
        //            CLabel labelLabel = getWidgetFactory().createCLabel(subComposite, ""); //$NON-NLS-1$
        // data = new FormData();
        // data.left = new FormAttachment(lastControl, 0);
        // data.right = new FormAttachment(lastControl, labelLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
        // + (ITabbedPropertyConstants.HSPACE * 2), SWT.RIGHT);
        // if (resetBtn != null) {
        // data.top = new FormAttachment(resetBtn, 0, SWT.CENTER);
        // } else {
        // data.top = new FormAttachment(0, top);
        // }
        // labelLabel.setLayoutData(data);
        // if (numInRow != 1) {
        // labelLabel.setAlignment(SWT.RIGHT);
        // }
        //
        // data = new FormData();
        // data.left = new FormAttachment(labelLabel, -1);
        // data.right = new FormAttachment(labelLabel, STANDARD_BUTTON_WIDTH, SWT.RIGHT);
        //
        // if (resetBtn != null) {
        // data.top = new FormAttachment(resetBtn, 0, SWT.CENTER);
        // } else {
        // data.top = new FormAttachment(0, top);
        // }
        // data.height = STANDARD_HEIGHT - 2;
        // button.setLayoutData(data);
        //
        // dynamicProperty.setCurRowSize(buttonSize.y + ITabbedPropertyConstants.VSPACE);
        // }
        return lastControlUsed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#createControl(org.eclipse
     * .swt.widgets.Composite, org.talend.core.model.process.IElementParameter, int, int, int,
     * org.eclipse.swt.widgets.Control)
     */
    @Override
    protected Command createButtonCommand(Button button) {
        FileItem repositoryFileItem = null; // hwang add for feature 6484
        LinkRulesItem linkItem = null;
        Map<String, FileItem> repositoryFileItemMap = null;
        Map<String, LinkRulesItem> repositoryLinkRulesItemMap = null;
        String paramName = (String) button.getData(PARAMETER_NAME);
        IElementParameter param = elem.getElementParameter(paramName);
        Object data = button.getData(NAME);
        if (data != null && data.equals(REPOSITORY_CHOICE)) {
            RepositoryReviewDialog dialog = new RepositoryReviewDialog(Display.getCurrent().getActiveShell(),
                    ERepositoryObjectType.METADATA, param.getRepositoryValue());
            if (dialog.open() == RepositoryReviewDialog.OK) {
                String id = dialog.getResult().getObject().getId();

                IElementParameter repositoryParam = param.getChildParameters().get(
                        EParameterName.REPOSITORY_PROPERTY_TYPE.getName());

                if (repositoryParam != null) {
                    repositoryParam.setLinkedRepositoryItem(dialog.getResult().getObject().getProperty().getItem());
                }
                String fullParamName = paramName + ":" + getRepositoryChoiceParamName(); //$NON-NLS-1$

                Connection repositoryConnection = null;
                Map<String, ConnectionItem> repositoryConnectionItemMap = dynamicProperty.getRepositoryConnectionItemMap();

                if (repositoryConnectionItemMap.containsKey(id)) {
                    repositoryConnection = repositoryConnectionItemMap.get(id).getConnection();
                } else {
                    repositoryConnection = null;
                    if (repositoryParam != null) {
                        Item item = repositoryParam.getLinkedRepositoryItem();
                        if (item instanceof ConnectionItem) {
                            repositoryConnection = ((ConnectionItem) item).getConnection();
                        }
                    }
                }

                if (repositoryConnection != null) {
                    CompoundCommand compoundCommand = new CompoundCommand();

                    ChangeValuesFromRepository changeValuesFromRepository = new ChangeValuesFromRepository(elem,
                            repositoryConnection, fullParamName, id);
                    changeValuesFromRepository.setMaps(dynamicProperty.getRepositoryTableMap());

                    compoundCommand.add(changeValuesFromRepository);

                    updateDBType(compoundCommand, repositoryConnection);
                    return compoundCommand;
                }

                // for ruleItem,hywang add
                if (dynamicProperty instanceof MultipleThreadDynamicComposite) {
                    repositoryFileItemMap = ((MultipleThreadDynamicComposite) dynamicProperty).getRepositoryFileItemMap();
                    repositoryLinkRulesItemMap = ((MultipleThreadDynamicComposite) dynamicProperty)
                            .getRepositoryLinkRulesItemMap();

                }

                if (repositoryFileItemMap.containsKey(id)) {
                    repositoryFileItem = repositoryFileItemMap.get(id);
                } else if (repositoryLinkRulesItemMap.containsKey(id)) {
                    linkItem = repositoryLinkRulesItemMap.get(id);
                } else {
                    if (!repositoryFileItemMap.isEmpty()) {
                        repositoryFileItem = repositoryFileItemMap.values().iterator().next();
                    } else {
                        repositoryFileItem = null;
                    }
                }
                if (repositoryFileItem != null) {
                    CompoundCommand compoundCommand = new CompoundCommand();
                    final String showId = repositoryFileItem.getProperty().getId();
                    Command command = new PropertyChangeCommand(elem, EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), showId);
                    compoundCommand.add(command);
                    return compoundCommand;
                }
                if (linkItem != null) {
                    CompoundCommand compoundCommand = new CompoundCommand();
                    final String showId = linkItem.getProperty().getId();
                    Command command = new PropertyChangeCommand(elem, EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), showId);
                    compoundCommand.add(command);
                    return compoundCommand;
                }

            }
        } else {
            /**
             * add by wzhang. When click the icon at the right side of Built-In. The corresponding wizard open.
             */
            // 1. open wizard
            if (elem instanceof INode) {
                INode node = (INode) elem;
                final IRepositoryService repositoryService = CorePlugin.getDefault().getRepositoryService();
                if (param != null) {
                    RepositoryNode realNode = null;
                    String repositoryValue = param.getRepositoryValue();
                    if (repositoryValue != null && repositoryValue.startsWith(ERepositoryCategoryType.DATABASE.getName())) {
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_CONNECTIONS);
                    } else

                    // file delimited
                    if (ERepositoryCategoryType.DELIMITED.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_FILE_DELIMITED);
                    }
                    // file positional
                    if (ERepositoryCategoryType.POSITIONAL.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_FILE_POSITIONAL);
                    }
                    // file regexp
                    if (ERepositoryCategoryType.REGEX.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_FILE_REGEXP);
                    }
                    // file xml
                    if (ERepositoryCategoryType.XML.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_FILE_XML);
                    }
                    // file ldif
                    if (ERepositoryCategoryType.LDIF.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_FILE_LDIF);
                    }
                    // excel
                    if (ERepositoryCategoryType.EXCEL.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_FILE_EXCEL);
                    }
                    // generic schema
                    if (ERepositoryCategoryType.GENERIC.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_GENERIC_SCHEMA);
                    }
                    // ldap
                    if (ERepositoryCategoryType.LDAP.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_LDAP_SCHEMA);
                    }
                    // wsdl
                    if (ERepositoryCategoryType.WSDL.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_WSDL_SCHEMA);
                    }
                    // salesforce
                    if (ERepositoryCategoryType.SALESFORCE.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_SALESFORCE_SCHEMA);
                    }
                    // ebcdic
                    if (ERepositoryCategoryType.EBCDIC.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_FILE_EBCDIC);
                    }

                    // mdm
                    if (ERepositoryCategoryType.MDM.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_MDMCONNECTION);
                    }
                    // sap
                    if (ERepositoryCategoryType.SAP.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_SAPCONNECTIONS);
                    }

                    if (ERepositoryCategoryType.HEADERFOOTER.getName().equals(repositoryValue)) { //$NON-NLS-1$
                        realNode = repositoryService.getRootRepositoryNode(ERepositoryObjectType.METADATA_HEADER_FOOTER);
                    }

                    if (realNode != null) {
                        ConnectionItem connItem = repositoryService.openMetadataConnection(true, realNode, node);
                        if (connItem != null) {
                            // refresh
                            RepositoryManager.refreshCreatedNode(ERepositoryObjectType.METADATA_CONNECTIONS);

                            IElementParameter propertyParam = elem
                                    .getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE);
                            propertyParam.getChildParameters().get(EParameterName.PROPERTY_TYPE.getName()).setValue(
                                    EmfComponent.REPOSITORY);

                            // 2. commnd
                            Command cmd = new ChangeValuesFromRepository((Element) node, connItem.getConnection(), propertyParam
                                    .getName()
                                    + ":" + EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), connItem.getProperty().getId()); //$NON-NLS-1$
                            executeCommand(cmd);
                            // see bug in feature 5998.refresh repositoryList.
                            if (dynamicProperty instanceof MultipleThreadDynamicComposite) {
                                ((MultipleThreadDynamicComposite) dynamicProperty).updateRepositoryList();
                            }
                        }
                    }

                }
            }
        }
        return null;
    }

    // see bug 0004305
    private void updateDBType(CompoundCommand compoundCommand, Connection repositoryConnection) {

        if (repositoryConnection == null) {
            return;
        }
        if (!(repositoryConnection instanceof DatabaseConnection)) {
            return;
        }
        final String property = "DBTYPE"; //$NON-NLS-1$
        if (elem.getElementParameter(property) == null) {
            return;
        }

        String currentDbType = ((DatabaseConnection) repositoryConnection).getDatabaseType();
        EDatabaseTypeName typeName = EDatabaseTypeName.getTypeFromDbType(currentDbType);

        Command command = new PropertyChangeCommand(elem, property, typeName.getXMLType());
        compoundCommand.add(command);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#createComboCommand(org
     * .eclipse.swt.custom.CCombo)
     */
    @Override
    protected Command createComboCommand(CCombo combo) {
        Connection repositoryConnection = null;
        ConnectionItem repositoryConnectionItem = null;

        FileItem repositoryFileItem = null; // hwang add

        String paramName = (String) combo.getData(PARAMETER_NAME);

        IElementParameter param = elem.getElementParameter(paramName);

        String value = combo.getText();

        for (int j = 0; j < param.getListItemsValue().length; j++) {
            if (combo.getText().equals(param.getListItemsDisplayName()[j])) {
                value = (String) param.getListItemsValue()[j];
            }
        }
        if (value.equals(param.getValue())) {
            return null;
        }

        Map<String, ConnectionItem> repositoryConnectionItemMap = null;
        IElementParameter repositoryParam = null;
        Map<String, FileItem> repositoryFileItemMap = null; // hywang add for feature 6484

        if (value.equals(EmfComponent.REPOSITORY)) {
            repositoryConnectionItemMap = dynamicProperty.getRepositoryConnectionItemMap();

            if (dynamicProperty instanceof MultipleThreadDynamicComposite) {
                repositoryFileItemMap = ((MultipleThreadDynamicComposite) dynamicProperty).getRepositoryFileItemMap();
            }

            repositoryParam = param.getParentParameter().getChildParameters().get(
                    EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
            String connectionSelected = (String) repositoryParam.getValue();

            if (repositoryConnectionItemMap.containsKey(connectionSelected)) {
                repositoryConnectionItem = repositoryConnectionItemMap.get(connectionSelected);
                repositoryConnection = repositoryConnectionItem.getConnection();
            } else {
                if (!repositoryConnectionItemMap.isEmpty()) {
                    repositoryConnectionItem = repositoryConnectionItemMap.values().iterator().next();
                    repositoryConnection = repositoryConnectionItem.getConnection();
                } else {
                    repositoryConnection = null;
                }
            }
            //
            // for ruleItem,hywang add
            if (repositoryFileItemMap.containsKey(connectionSelected)) {
                repositoryFileItem = repositoryFileItemMap.get(connectionSelected);
            } else {
                if (!repositoryFileItemMap.isEmpty()) {
                    repositoryFileItem = repositoryFileItemMap.values().iterator().next();
                } else {
                    repositoryFileItem = null;
                }
            }

        }
        CompoundCommand cc = new CompoundCommand();
        if (repositoryConnectionItem != null) {
            initCDC(cc, repositoryConnectionItem);
        }
        ChangeValuesFromRepository changeValuesFromRepository1 = new ChangeValuesFromRepository(elem, repositoryConnection,
                paramName, value);
        changeValuesFromRepository1.setMaps(dynamicProperty.getRepositoryTableMap());
        cc.add(changeValuesFromRepository1);
        if (repositoryConnection != null) {
            ChangeValuesFromRepository changeValuesFromRepository2 = new ChangeValuesFromRepository(elem, repositoryConnection,
                    repositoryParam.getParentParameter().getName() + ":" + repositoryParam.getName(), repositoryConnectionItem //$NON-NLS-1$
                            .getProperty().getId());
            changeValuesFromRepository2.setMaps(dynamicProperty.getRepositoryTableMap());
            cc.add(changeValuesFromRepository2);
        }
        // hywang add for feature 6484
        if (repositoryFileItem != null) {
            final String id = repositoryFileItem.getProperty().getId();
            cc.add(new Command() {

                @Override
                public void execute() {
                    IElementParameter elementParameter = elem.getElementParameter(EParameterName.PROPERTY_TYPE.getName());
                    if (elementParameter != null) {
                        elementParameter = elementParameter.getChildParameters().get(
                                EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
                        if (elementParameter != null) {
                            elementParameter.setValue(id);
                        }
                    }
                }

            });
        }
        if (value.equals(EmfComponent.REPOSITORY)) {
            updateDBType(cc, repositoryConnection);
        }

        return cc;

    }

    private void initCDC(CompoundCommand cc, ConnectionItem originalConnectionItem) {
        if (!(elem instanceof Node)) {
            return;
        }
        Node node = (Node) elem;

        // ConnectionItem originalConnectionItem = repositoryConnectionItem;
        ConnectionItem connectionItem = originalConnectionItem;
        Connection originalConnection = connectionItem.getConnection();
        Connection connection = connectionItem.getConnection();
        if (node.getComponent().getName().contains("CDC")) { // to replace by a flag CDC in component? //$NON-NLS-1$
            if (originalConnectionItem instanceof DatabaseConnectionItem) {
                final DatabaseConnection databaseConnection = (DatabaseConnection) connection;
                CDCConnection cdcConn = databaseConnection.getCdcConns();
                if (cdcConn != null) {
                    EList cdcTypes = cdcConn.getCdcTypes();
                    if (cdcTypes != null && !cdcTypes.isEmpty()) {
                        CDCType cdcType = (CDCType) cdcTypes.get(0);
                        // replace property by CDC property.
                        String propertyId = cdcType.getLinkDB();
                        try {
                            IRepositoryViewObject object = ProxyRepositoryFactory.getInstance().getLastVersion(propertyId);
                            if (object != null) {
                                if (object.getProperty().getItem() instanceof DatabaseConnectionItem) {
                                    DatabaseConnectionItem dbConnItem = (DatabaseConnectionItem) object.getProperty().getItem();
                                    // replace connection by CDC connection
                                    connectionItem = dbConnItem;
                                    connection = dbConnItem.getConnection();
                                }
                            }
                        } catch (PersistenceException e) {
                            ExceptionHandler.process(e);
                        }
                        // set cdc type mode.
                        IElementParameter logModeParam = node.getElementParameter(EParameterName.CDC_TYPE_MODE.getName());
                        if (logModeParam != null) {
                            String cdcTypeMode = ((DatabaseConnection) originalConnection).getCdcTypeMode();
                            Command logModeCmd = new PropertyChangeCommand(node, EParameterName.CDC_TYPE_MODE.getName(),
                                    CDCTypeMode.LOG_MODE.getName().equals(cdcTypeMode));
                            cc.add(logModeCmd);
                        }
                        // set lib for as400 so far.
                        final String name = "SOURCE_LIB"; //$NON-NLS-1$
                        IElementParameter libParam = node.getElementParameter(name);
                        if (libParam != null) {
                            Command libSettingCmd = new PropertyChangeCommand(node, name, TalendTextUtils
                                    .addQuotes(databaseConnection.getSID()));
                            cc.add(libSettingCmd);
                        }

                    }
                }
            }

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#getRepositoryChoiceParamName
     * ()
     */
    @Override
    protected String getRepositoryChoiceParamName() {
        return EParameterName.REPOSITORY_PROPERTY_TYPE.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.designer.core.ui.editor.properties.controllers.AbstractRepositoryController#getRepositoryTypeParamName
     * ()
     */
    @Override
    protected String getRepositoryTypeParamName() {
        return EParameterName.PROPERTY_TYPE.getName();
    }

    // @Override
    // protected String getDisplayNameFromValue(IElementParameter param, String value) {
    // if (param == null || value == null || value.equals("")) { //$NON-NLS-1$
    // return null;
    // }
    // if (!param.getName().equals(getRepositoryChoiceParamName())) {
    // return null;
    // }
    // Item item = param.getLinkedRepositoryItem();
    // if (item == null || (item != null && !item.getProperty().getId().equals(value))) {
    // Map<String, ConnectionItem> itemMap = dynamicProperty.getRepositoryConnectionItemMap();
    // item = itemMap.get(value);
    // if (item == null) {
    // item = UpdateRepositoryUtils.getConnectionItemByItemId(value);
    // if (item != null) {
    // // set in map
    // itemMap.put(value, (ConnectionItem) item);
    // }
    // }
    // }
    // if (item != null && item instanceof ConnectionItem) {
    // return dynamicProperty.getRepositoryAliasName((ConnectionItem) item) + ":" + item.getProperty().getLabel();
    // //$NON-NLS-1$
    // }
    // return null;
    // }

}
