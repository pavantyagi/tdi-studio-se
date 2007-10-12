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
package org.talend.repository.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.events.SelectionListener;
import org.talend.commons.utils.workbench.extensions.ExtensionImplementationProvider;
import org.talend.commons.utils.workbench.extensions.ExtensionPointLimiterImpl;
import org.talend.commons.utils.workbench.extensions.IExtensionPointLimiter;

/**
 * Provides, using extension points, implementation of many factories.
 * 
 * <ul>
 * <li>IProcessFactory</li>
 * </ul>
 * 
 * $Id$
 */
public class RepositoryFactoryProvider {

    private static List<IRepositoryFactory> list = null;

    public static final IExtensionPointLimiter REPOSITORY_PROVIDER = new ExtensionPointLimiterImpl(
            "org.talend.core.repository_provider", //$NON-NLS-1$
            "RepositoryFactory", 1, -1); //$NON-NLS-1$

    public static List<IRepositoryFactory> getAvailableRepositories() {
        if (list == null) {
            list = new ArrayList<IRepositoryFactory>();
            List<IConfigurationElement> extension = ExtensionImplementationProvider.getInstanceV2(REPOSITORY_PROVIDER);

            for (IConfigurationElement current : extension) {
                try {
                    IRepositoryFactory currentAction = (IRepositoryFactory) current.createExecutableExtension("class"); //$NON-NLS-1$
                    currentAction.setId(current.getAttribute("id")); //$NON-NLS-1$
                    currentAction.setName(current.getAttribute("name")); //$NON-NLS-1$
                    currentAction.setAuthenticationNeeded(new Boolean(current.getAttribute("authenticationNeeded"))); //$NON-NLS-1$
                    currentAction.setDisplayToUser(new Boolean(current.getAttribute("displayToUser")).booleanValue()); //$NON-NLS-1$

                    // Getting dynamic login fields:
                    for (IConfigurationElement currentLoginField : current.getChildren("loginField")) { //$NON-NLS-1$
                        DynamicFieldBean key = new DynamicFieldBean(currentLoginField.getAttribute("id"), //$NON-NLS-1$
                                currentLoginField.getAttribute("name"), //$NON-NLS-1$
                                new Boolean(currentLoginField.getAttribute("required")), //$NON-NLS-1$
                                new Boolean(currentLoginField.getAttribute("password"))); //$NON-NLS-1$
                        currentAction.getFields().add(key);
                    }

                    for (IConfigurationElement currentLoginField : current.getChildren("button")) { //$NON-NLS-1$
                        DynamicButtonBean key = new DynamicButtonBean(currentLoginField.getAttribute("id"), //$NON-NLS-1$
                                currentLoginField.getAttribute("name"), //$NON-NLS-1$
                                (SelectionListener) currentLoginField.createExecutableExtension("selectionListener")); //$NON-NLS-1$
                        currentAction.getButtons().add(key);
                    }

                    list.add(currentAction);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    public static IRepositoryFactory getRepositoriyById(String id) {
        for (IRepositoryFactory current : getAvailableRepositories()) {
            if (current.getId().equals(id)) {
                return current;
            }
        }
        return null;
    }
}
