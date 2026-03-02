/*
 * LEGAL NOTICE
 * This computer software was prepared by Battelle Memorial Institute,
 * hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
 * with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
 * CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 * 
 * Copyright 2012 Battelle Memorial Institute.  All Rights Reserved.
 * Distributed as open-source under the terms of the Educational Community 
 * License version 2.0 (ECL 2.0). http://www.opensource.org/licenses/ecl2.php
 */
package ModelInterface.common;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractButton;

/**
 * Aligns Swing button sizing with ScenarioBuilder's standard button dimensions.
 */
public final class SwingButtonSizer {
	/**
	 * A more typical minimum size for desktop Swing buttons.
	 * <p>
	 * We intentionally treat this as a <em>minimum</em> and preserve the Look & Feel
	 * preferred size if it's larger.
	 */
	public static final int STANDARD_BUTTON_WIDTH = 96;
	public static final int STANDARD_BUTTON_HEIGHT = 32;
	/**
	 * Only applied when the button has no/very small margin.
	 */
	public static final Insets STANDARD_BUTTON_MARGIN = new Insets(6, 14, 6, 14);
	private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

	private SwingButtonSizer() {
	}

	public static void applyStandardSize(AbstractButton button) {
		if (button == null) {
			return;
		}
		// Respect L&F defaults; only enforce a margin when it's effectively missing.
		Insets curMargin = button.getMargin();
		if (curMargin == null 
				|| (curMargin.top + curMargin.bottom) < 6 
				|| (curMargin.left + curMargin.right) < 12) {
			button.setMargin(STANDARD_BUTTON_MARGIN);
		}

		final Dimension pref = button.getPreferredSize();
		final int width = Math.max(pref.width, STANDARD_BUTTON_WIDTH);
		final int height = Math.max(pref.height, STANDARD_BUTTON_HEIGHT);
		final Dimension sized = new Dimension(width, height);

		// Set a minimum so buttons don't end up tiny, but don't force a maximum size
		// (maximum sizing can cause small/awkward layouts in BoxLayout, etc.).
		button.setMinimumSize(new Dimension(STANDARD_BUTTON_WIDTH, STANDARD_BUTTON_HEIGHT));
		button.setPreferredSize(sized);
	}

	public static void installGlobalListener() {
		if (!INSTALLED.compareAndSet(false, true)) {
			return;
		}
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				if (!(event instanceof ContainerEvent)) {
					return;
				}
				ContainerEvent ce = (ContainerEvent) event;
				if (ce.getID() != ContainerEvent.COMPONENT_ADDED) {
					return;
				}
				Component child = ce.getChild();
				if (child instanceof AbstractButton) {
					applyStandardSize((AbstractButton) child);
				}
			}
		}, AWTEvent.CONTAINER_EVENT_MASK);
	}
}