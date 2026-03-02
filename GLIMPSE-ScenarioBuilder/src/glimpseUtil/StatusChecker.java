/*
* LEGAL NOTICE
* This computer software was prepared by US EPA.
* THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* EXPORT CONTROL
* User agrees that the Software will not be shipped, transferred or
* exported into any country or used in any manner prohibited by the
* United States Export Administration Act or any other applicable
* export laws, restrictions or regulations (collectively the "Export Laws").
* Export of the Software may require some form of license or other
* authority from the U.S. Government, and failure to obtain such
* export control license may result in criminal liability under
* U.S. laws. In addition, if the Software is identified as export controlled
* items under the Export Laws, User represents and warrants that User
* is not a citizen, or otherwise located within, an embargoed nation
* (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
*     and that User is not otherwise prohibited
* under the Export Laws from receiving the Software.
*
* SUPPORT
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors * from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. Coding contributions have also been made by Aaron 
* Parks and Yadong Xu of ARA through the EPA�s Environmental Modeling and 
* Visualization Laboratory contract. 
* 
*  * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 * For the GLIMPSE project, GCAM development, data processing, and support for 
 * policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
 * Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
 * Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
 * Binsted, and Pralit Patel. 
 * The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
 * Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
 * Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
 * Laboratory contract.
* 
*/
package glimpseUtil;

import gui.Client;

public class StatusChecker extends Thread {
	boolean terminate = false;

	public void run() {
		int count = 0;
		while (!terminate) {
			count++;
			try {
				ExecutionThreadLike et = null;
				try {
					// Avoid hard failure if Client isn't initialized in a headless harness.
					et = (Client.gCAMExecutionThread != null) ? new ExecutionThreadLike(Client.gCAMExecutionThread) : null;
				} catch (Throwable ignored) {
					et = null;
				}

				boolean shouldRefresh = false;
				try {
					if (et != null) {
						shouldRefresh = et.didNumDoneChange() || (count == 5);
					}
				} catch (Throwable ignored) {
				}

				if (shouldRefresh) {
					fireRefreshButtonSafe();
					count = 0;
				}

				Thread.sleep(5000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	private void fireRefreshButtonSafe() {
		try {
			Object button = null;
			try {
				java.lang.reflect.Field f = gui.Client.class.getDeclaredField("buttonRefreshScenarioStatus");
				f.setAccessible(true);
				button = f.get(null);
			} catch (Throwable ignored) {
				button = null;
			}

			if (button == null) {
				return;
			}

			final Object btn = button;
			Runnable fire = () -> {
				try {
					java.lang.reflect.Method fireMethod = btn.getClass().getMethod("fire");
					fireMethod.invoke(btn);
				} catch (Throwable ignored) {}
			};

			// If JavaFX is present, marshal to the FX thread.
			try {
				Class<?> platformClass = Class.forName("javafx.application.Platform");
				java.lang.reflect.Method runLater = platformClass.getMethod("runLater", Runnable.class);
				runLater.invoke(null, fire);
				return;
			} catch (Throwable noFx) {
				// No JavaFX available; fall back to direct call.
			}

			fire.run();
		} catch (Throwable ignored) {
			// Never let StatusChecker crash the app.
		}
	}

	public void terminate() {
		terminate = true;
	}

	/**
	 * Tiny adapter to avoid linking additional packages/classes from this utility thread.
	 */
	private static final class ExecutionThreadLike {
		private final gui.ExecutionThread delegate;

		ExecutionThreadLike(gui.ExecutionThread delegate) {
			this.delegate = delegate;
		}

		boolean didNumDoneChange() {
			return delegate.didNumDoneChange();
		}
	}

}