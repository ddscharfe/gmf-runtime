/******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package org.eclipse.gmf.runtime.common.ui.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.gmf.runtime.common.core.command.CommandManager;
import org.eclipse.gmf.runtime.common.core.command.ICommand;
import org.eclipse.gmf.runtime.common.core.util.Log;
import org.eclipse.gmf.runtime.common.core.util.Trace;
import org.eclipse.gmf.runtime.common.ui.internal.CommonUIDebugOptions;
import org.eclipse.gmf.runtime.common.ui.internal.CommonUIPlugin;
import org.eclipse.gmf.runtime.common.ui.internal.CommonUIStatusCodes;

/**
 * This class defines convenience methods for executing a command and monitoring
 * the progress of that command in a cancelable
 * {@link org.eclipse.gmf.runtime.common.ui.dialogs.DispatchingProgressMonitorDialog}.
 * <P>
 * These convenience methods should be used for executing commands that interact
 * with the model server.
 * <P>
 * Because these convenience methods will instantiate a
 * DispatchingProgressMonitorDialog, they should only be used in code that is
 * meant to depend on the UI.
 * 
 * @author ldamus
 * @deprecated Clients should use the Eclipse ProgressMonitorDialog
 * @see org.eclipse.jface.dialogs.ProgressMonitorDialog
 */
public class DispatchingProgressDialogUtil {

	/**
	 * Runs <code>runnableWithProgress</code> in a cancelable
	 * {@link DispatchingProgressMonitorDialog}. The runnable runs in the same
	 * thread as the dialog.
	 * 
	 * @param runnableWithProgress
	 *            the runnable to run in the dialog
	 */
	public static void runWithDispatchingProgressDialog(
			IRunnableWithProgress runnableWithProgress) {

		try {
			if (System.getProperty("RUN_PROGRESS_IN_THREAD") != null) { //$NON-NLS-1$
				new ProgressMonitorDialog(null).run(true, true, runnableWithProgress);
			} else {
				new ProgressMonitorDialog(null).run(false, true, runnableWithProgress);
			}

		} catch (InvocationTargetException ite) {
			Trace.catching(CommonUIPlugin.getDefault(),
				CommonUIDebugOptions.EXCEPTIONS_CATCHING,
				DispatchingProgressDialogUtil.class,
				"runWithDispatchingProgressDialog", ite); //$NON-NLS-1$
			Log.error(CommonUIPlugin.getDefault(),
				CommonUIStatusCodes.SERVICE_FAILURE,
				"runWithDispatchingProgressDialog", ite); //$NON-NLS-1$
		} catch (InterruptedException ie) {
			Trace.catching(CommonUIPlugin.getDefault(),
				CommonUIDebugOptions.EXCEPTIONS_CATCHING,
				DispatchingProgressDialogUtil.class,
				"runWithDispatchingProgressDialog", ie); //$NON-NLS-1$
		}
	}

	/**
	 * Executes <code>command</code> in a cancelable
	 * {@link DispatchingProgressMonitorDialog}. The command runs in the same
	 * thread as the dialog.
	 * 
	 * @param command
	 *            the command to execute
	 */
	public static void executeWithDispatchingProgressDialog(
			final ICommand command) {

		IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {

				CommandManager.getDefault().execute(command, monitor);

			}
		};
		runWithDispatchingProgressDialog(runnableWithProgress);
	}

}