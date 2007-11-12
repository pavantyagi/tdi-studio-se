// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2007 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.repository.ui.actions.metadata.database.wizard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.MessageBoxExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.ui.swt.dialogs.ProgressDialog;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.ui.images.ECoreImage;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.ui.actions.metadata.database.ConnectionDBTableHelper;
import org.talend.repository.ui.actions.metadata.database.DBProcessRecords;
import org.talend.repository.ui.actions.metadata.database.DBTableForDelimitedBean;
import org.talend.repository.ui.actions.metadata.database.DBProcessRecords.ProcessType;
import org.talend.repository.ui.actions.metadata.database.DBProcessRecords.RecordsType;
import org.talend.repository.ui.wizards.RepositoryWizard;

/**
 * ggu class global comment. Detailled comment <br/>
 * 
 */
public class ImportDBTableWizard extends RepositoryWizard implements IImportWizard {

    private static Logger log = Logger.getLogger(ImportDBTableWizard.class);

    private static final IProxyRepositoryFactory FACTORY = ProxyRepositoryFactory.getInstance();

    private ImportDBTableWizardPage importWizardPage;

    private DBProcessRecords processRecords = new DBProcessRecords();

    private static int rejectedNum = 0;

    /**
     * ggu ImportDBTableWizard constructor comment.
     */
    public ImportDBTableWizard(IWorkbench workbench, ISelection selection) {
        super(workbench, true);
        this.selection = selection;
        setNeedsProgressMonitor(true);
        initSetting();

    }

    private void initSetting() {
        if (selection == null) {
            pathToSave = new Path(""); //$NON-NLS-1$
            return;
        }

        Object userSelection = ((IStructuredSelection) selection).getFirstElement();
        if (userSelection instanceof RepositoryNode) {
            switch (((RepositoryNode) userSelection).getType()) {
            case SIMPLE_FOLDER:
                pathToSave = RepositoryNodeUtilities.getPath((RepositoryNode) userSelection);
                break;
            case SYSTEM_FOLDER:
                pathToSave = new Path(""); //$NON-NLS-1$
                break;
            default:
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.setWorkbench(workbench);
        this.selection = selection;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {
        setWindowTitle(Messages.getString("ImportDBTableWizard.WizardTitle")); //$NON-NLS-1$
        setDefaultPageImageDescriptor(ImageProvider.getImageDesc(ECoreImage.METADATA_CONNECTION_WIZ));
        importWizardPage = new ImportDBTableWizardPage();
        importWizardPage.setTitle(Messages.getString("ImportDBTableWizard.Title")); //$NON-NLS-1$
        importWizardPage.setDescription(Messages.getString("ImportDBTableWizard.Description")); //$NON-NLS-1$
        importWizardPage.setPageComplete(false);
        addPage(importWizardPage);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        if (importWizardPage.isPageComplete()) {

            File file = importWizardPage.getFormSetting().getImportFile();
            if (file == null) {
                return false;
            }
            progressDialog(file);
            return true;
        }
        return false;
    }

    private void progressDialog(final File file) {
        Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        ProgressDialog progressDialog = new ProgressDialog(activeShell) {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                process(file);
            }
        };

        try {
            progressDialog.executeProcess();
        } catch (InvocationTargetException e) {
            MessageBoxExceptionHandler.process(e.getTargetException(), activeShell);
        } catch (InterruptedException e) {
            // Nothing to do
        }
    }

    private void process(File file) {
        BufferedReader reader = null;
        ConnectionDBTableHelper.initGenTableName();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {

                DBTableForDelimitedBean bean = ConnectionDBTableHelper.getRowData(line);
                if (bean != null) { // the line is suitable format.

                    ConnectionItem connItem = null;
                    try {
                        connItem = ConnectionDBTableHelper.setConnectionItemData(bean);
                    } catch (PersistenceException e) {
                        writeRejects(line, bean);
                        continue;
                    } catch (BusinessException e) {
                        writeRejects(line, bean);
                        continue;
                    }
                    if (connItem == null) {
                        writeRejects(line, bean);
                        continue;
                    }
                    try {
                        if (ConnectionDBTableHelper.isConnectionCreated()) {
                            connItem.getProperty().setId(FACTORY.getNextId());
                            FACTORY.create(connItem, pathToSave);
                        } else {
                            FACTORY.save(connItem);
                        }
                    } catch (PersistenceException e) {
                        writeRejects(line, bean);
                        continue;
                    }
                    addRecords(ProcessType.IMPORT, bean);
                } else { // this line isn't right format. record it in ".log" and ".rejects"
                    // bean = null
                    writeRejects(line, null);

                }
            }
            // write the .log
            writeLogs();
        } catch (FileNotFoundException e) {
            MessageBoxExceptionHandler.process(e, getShell());

        } catch (IOException e) {
            MessageBoxExceptionHandler.process(e, getShell());

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // nothing to do
                }
                reader = null;
            }
        }

    }

    private void writeLogs() {
        StringBuffer sb = new StringBuffer();

        int conNum = processRecords.getRecord(ProcessType.IMPORT, RecordsType.CONNECTION);
        int tableNum = processRecords.getRecord(ProcessType.IMPORT, RecordsType.TABLE);
        int fieldNum = processRecords.getRecord(ProcessType.IMPORT, RecordsType.FIELD);

        sb.append(Messages.getString("ImportDBTableWizard.Imported")); //$NON-NLS-1$
        sb.append(conNum);
        sb.append(Messages.getString("ImportDBTableWizard.Connections")); //$NON-NLS-1$
        sb.append(tableNum);
        sb.append(Messages.getString("ImportDBTableWizard.Tables")); //$NON-NLS-1$
        sb.append(fieldNum);
        sb.append(Messages.getString("ImportDBTableWizard.Fields")); //$NON-NLS-1$

        conNum = processRecords.getRecord(ProcessType.REJECT, RecordsType.CONNECTION);
        tableNum = processRecords.getRecord(ProcessType.REJECT, RecordsType.TABLE);
        fieldNum = processRecords.getRecord(ProcessType.REJECT, RecordsType.FIELD);
        // can't parse some line. add rejected number.
        conNum += rejectedNum;
        tableNum += rejectedNum;
        fieldNum += rejectedNum;

        sb.append(Messages.getString("ImportDBTableWizard.Rejected")); //$NON-NLS-1$
        sb.append(conNum);
        sb.append(Messages.getString("ImportDBTableWizard.Connections")); //$NON-NLS-1$
        sb.append(tableNum);
        sb.append(Messages.getString("ImportDBTableWizard.Tables")); //$NON-NLS-1$
        sb.append(fieldNum);
        sb.append(Messages.getString("ImportDBTableWizard.Fields")); //$NON-NLS-1$
        log.info(sb.toString());
    }

    private void writeRejects(String line, DBTableForDelimitedBean bean) {
        // write .rejects
        try {
            String logs = System.getProperty("osgi.logfile"); //$NON-NLS-1$
            if (ConnectionDBTableHelper.isNullable(logs)) {
                return;
            }

            String rejectsFile = new File(logs).getParent() + File.separator + ".rejects"; //$NON-NLS-1$
            PrintWriter pw = new PrintWriter(new FileWriter(rejectsFile, true), true);
            pw.println(line);
            pw.flush();
            pw.close();

        } catch (IOException e) {
            //
        }

        if (bean == null) {
            rejectedNum++;
        } else {
            addRecords(ProcessType.REJECT, bean);
        }
    }

    private void addRecords(ProcessType rType, DBTableForDelimitedBean bean) {
        processRecords.addRecord(rType, RecordsType.CONNECTION, bean.getName());
        processRecords.addRecord(rType, RecordsType.TABLE, bean.getTableName());
        processRecords.addRecord(rType, RecordsType.FIELD, bean.getLabel());
    }

}
