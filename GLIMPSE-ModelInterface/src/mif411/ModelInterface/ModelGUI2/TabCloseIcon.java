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
*/
package ModelInterface.ModelGUI2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.tables.BaseTableModel;

public class TabCloseIcon implements Icon {

	private static final int CLOSE_ICON_WIDTH_DELTA = 8;
	private static final int CLOSE_ICON_HEIGHT_DELTA = 10;
	private static final int CLOSE_ICON_TEXT_GAP = 6;
	private static final int TAB_ICON_LEFT_INSET = 4;
	private static final int CLOSE_ICON_HIT_PADDING = 3;
	private static final Color CLOSE_ICON_BACKGROUND_COLOR = new Color(95, 95, 95);
	private static final Color CLOSE_ICON_PRESSED_BACKGROUND_COLOR = new Color(70, 70, 70);
	/** Red background shown when the mouse hovers over the close button. */
	private static final Color HOVER_BACKGROUND_COLOR = new Color(200, 30, 30);
	private static final Color RUNNING_ICON_BACKGROUND_COLOR = new Color(205, 205, 205);
	/** Tab label text color while a query is still running. */
	private static final Color RUNNING_LABEL_COLOR = new Color(150, 150, 150);
	/** Tab label text color once the query has finished. */
	private static final Color FINISHED_LABEL_COLOR = Color.BLACK;
	/** Tab label text color when the tab is the currently selected tab. */
	private static final Color SELECTED_LABEL_COLOR = Color.WHITE;
	private static final int LOAD_ICON_FOREGROUND_BRIGHTNESS_THRESHOLD = 210;
	private static final int RUNNING_ICON_MIN_FOREGROUND_CHANNEL = 245;

	private static final ImageIcon closeIconTemplate = new ImageIcon(TabCloseIcon.class.getResource("icons/closeTab.PNG"));
	private static final int CLOSE_ICON_DRAW_WIDTH = Math.max(1, closeIconTemplate.getIconWidth() + CLOSE_ICON_WIDTH_DELTA);
	private static final int CLOSE_ICON_HEIGHT = Math.max(1, closeIconTemplate.getIconHeight() + CLOSE_ICON_HEIGHT_DELTA);

	// Padded icons — used in legacy "icon mode" where TabCloseIcon is the tab's left-side icon.
	private static final Icon closeIcon = createLoadStyleCloseIcon("icons/loadTab.PNG", CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT, CLOSE_ICON_BACKGROUND_COLOR, CLOSE_ICON_TEXT_GAP);
	private static final Icon mPressCloseIcon = createLoadStyleCloseIcon("icons/loadTab.PNG", CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT, CLOSE_ICON_PRESSED_BACKGROUND_COLOR, CLOSE_ICON_TEXT_GAP);
	private static final Icon loadingIcon = createLightenedRunningIcon("icons/loadTab.PNG", CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT, RUNNING_ICON_BACKGROUND_COLOR, CLOSE_ICON_TEXT_GAP);

	// Unpadded icons — used inside TabHeaderPanel's close button (gap is handled by FlowLayout).
	private static final Icon closeIconBtn     = createLoadStyleCloseIcon("icons/loadTab.PNG", CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT, CLOSE_ICON_BACKGROUND_COLOR, 0);
	private static final Icon pressCloseIconBtn = createLoadStyleCloseIcon("icons/loadTab.PNG", CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT, CLOSE_ICON_PRESSED_BACKGROUND_COLOR, 0);
	private static final Icon hoverCloseIconBtn = createLoadStyleCloseIcon("icons/loadTab.PNG", CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT, HOVER_BACKGROUND_COLOR, 0);
	private static final Icon loadingIconBtn   = createLightenedRunningIcon("icons/loadTab.PNG", CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT, RUNNING_ICON_BACKGROUND_COLOR, 0);

	private JTabbedPane tabPane = null;
	private Icon showingIcon;
	private transient Rectangle position = null;
	private transient int pressedCloseTabIndex = -1;
	private transient boolean closeClickInProgress = false;
	private transient boolean restoringSelection = false;
	private transient int lastStableSelectedIndex = -1;
	private volatile boolean finished = false;
	/** Non-null when this instance is used in "header panel mode" (right-side close button). */
	private TabHeaderPanel tabHeaderPanel = null;

	public TabCloseIcon(JTabbedPane tabPaneIn) {
		showingIcon = loadingIcon;
		tabPane = tabPaneIn;
		lastStableSelectedIndex = tabPane.getSelectedIndex();
		tabPane.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent event) {
				if (restoringSelection) {
					return;
				}
				final int selectedIndex = tabPane.getSelectedIndex();
				if (closeClickInProgress && pressedCloseTabIndex >= 0 && selectedIndex == pressedCloseTabIndex
						&& lastStableSelectedIndex >= 0 && lastStableSelectedIndex < tabPane.getTabCount()) {
					restoringSelection = true;
					try {
						tabPane.setSelectedIndex(lastStableSelectedIndex);
					} finally {
						restoringSelection = false;
					}
					return;
				}
				if (!closeClickInProgress && selectedIndex >= 0) {
					lastStableSelectedIndex = selectedIndex;
				}
			}
		});
		tabPane.addMouseListener(new MouseAdapter() {
			@Override public void mouseReleased( MouseEvent e ) {
				// In header-panel mode the close button handles this directly.
				if (tabHeaderPanel != null) return;
				// asking for isConsumed is *very* important, otherwise more than one tab might get closed!
				if ( !e.isConsumed() ) {
					final int index = pressedCloseTabIndex >= 0 ? pressedCloseTabIndex : getCloseTabIndexAt(e);
					pressedCloseTabIndex = -1;
					final int selectedBeforeClose = lastStableSelectedIndex;
					if (index < 0) {
						closeClickInProgress = false;
						return;
					}

					if (index >= tabPane.getTabCount()) {
						return;
					}

					QueryResultsPanel closeThread = (QueryResultsPanel)(tabPane.getComponentAt(index));
					BaseTableModel btm = DbViewer.getTableModelFromComponent(closeThread);
					if (InterfaceMain.getInstance() != null) {
						InterfaceMain.getInstance().fireProperty("Query", btm, null);
					}
					try {
						closeThread.killThreadAndWait();
					} catch (Exception ex) {
						// Best-effort shutdown; still allow the tab to close.
					}

					tabPane.removeMouseListener(this);
					tabPane.remove( index );
					restoreTabSelectionAfterClose(selectedBeforeClose, index);
					closeClickInProgress = false;
					e.consume();
				}
			}
			@Override public void mouseExited( MouseEvent e ) {
				if (tabHeaderPanel != null) return;
				if ( !e.isConsumed() ) {
					if (finished) {
						showingIcon = closeIcon;
					} else {
						showingIcon = loadingIcon;
					}
				}
			}
			@Override public void mousePressed( MouseEvent e ) {
				if (tabHeaderPanel != null) return;
				if ( !e.isConsumed() ) {
					pressedCloseTabIndex = getCloseTabIndexAt(e);
					closeClickInProgress = pressedCloseTabIndex >= 0;
				}
				if ( !e.isConsumed()  && pressedCloseTabIndex >= 0 ) {
					if (finished) {
						showingIcon = mPressCloseIcon;
					}
				} else {
					pressedCloseTabIndex = -1;
					closeClickInProgress = false;
				}
			}
		});
	}

	/**
	 * Creates a tab header component with the title label on the left and the
	 * close button on the right end of the tab. Install it via
	 * {@link JTabbedPane#setTabComponentAt(int, Component)}.
	 *
	 * @param title    the tab label text
	 * @param dbViewer the DbViewer instance used for the "Save As..." context menu action
	 */
	public TabHeaderPanel createTabHeader(final String title, final DbViewer dbViewer) {
		tabHeaderPanel = new TabHeaderPanel(title, dbViewer);
		return tabHeaderPanel;
	}

	private void restoreTabSelectionAfterClose(final int selectedBeforeClose, final int closedTabIndex) {
		if (selectedBeforeClose < 0 || tabPane.getTabCount() == 0) {
			return;
		}
		int restoreIndex = selectedBeforeClose;
		if (closedTabIndex < selectedBeforeClose) {
			restoreIndex = selectedBeforeClose - 1;
		}
		restoreIndex = Math.max(0, Math.min(restoreIndex, tabPane.getTabCount() - 1));
		restoringSelection = true;
		try {
			tabPane.setSelectedIndex(restoreIndex);
			lastStableSelectedIndex = restoreIndex;
		} finally {
			restoringSelection = false;
		}
	}

	private int getCloseTabIndexAt(final MouseEvent e) {
		final int tabIndex = tabPane.indexAtLocation(e.getX(), e.getY());
		if (tabIndex < 0) {
			return -1;
		}
		if (tabPane.getIconAt(tabIndex) != this) {
			return -1;
		}
		final Rectangle iconBounds = getIconBoundsForTab(tabIndex);
		return iconBounds != null && iconBounds.contains(e.getX(), e.getY()) ? tabIndex : -1;
	}

	private Rectangle getIconBoundsForTab(final int tabIndex) {
		final Rectangle tabBounds = tabPane.getBoundsAt(tabIndex);
		if (tabBounds == null) {
			return null;
		}
		int iconX = tabBounds.x + TAB_ICON_LEFT_INSET;
		int iconY = tabBounds.y + Math.max(0, (tabBounds.height - getIconHeight()) / 2);
		if (position != null && tabBounds.contains(position.x, position.y)) {
			iconX = position.x;
			iconY = position.y;
		}
		return new Rectangle(iconX - CLOSE_ICON_HIT_PADDING, iconY - CLOSE_ICON_HIT_PADDING,
				getIconWidth() + (2 * CLOSE_ICON_HIT_PADDING), getIconHeight() + (2 * CLOSE_ICON_HIT_PADDING));
	}

	private static Icon createScaledIcon(final String iconPath, final int targetWidth, final int targetHeight) {
		final ImageIcon original = new ImageIcon(TabCloseIcon.class.getResource(iconPath));
		final int scaledWidth = Math.max(1, targetWidth);
		final int scaledHeight = Math.max(1, targetHeight);
		final Image scaledImage = original.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
		return new ImageIcon(scaledImage);
	}

	private static Icon createLoadStyleCloseIcon(final String iconPath, final int targetWidth, final int targetHeight,
			final Color backgroundColor, final int rightPadding) {
		final Icon scaledIcon = createScaledIcon(iconPath, targetWidth, targetHeight);
		final BufferedImage source = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D sourceGraphics = source.createGraphics();
		scaledIcon.paintIcon(null, sourceGraphics, 0, 0);
		sourceGraphics.dispose();

		final BufferedImage recolored = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < targetHeight; ++y) {
			for (int x = 0; x < targetWidth; ++x) {
				final int argb = source.getRGB(x, y);
				final int alpha = (argb >>> 24) & 0xFF;
				if (alpha == 0) {
					continue;
				}
				final int red = (argb >>> 16) & 0xFF;
				final int green = (argb >>> 8) & 0xFF;
				final int blue = argb & 0xFF;
				final int brightness = (red + green + blue) / 3;
				if (brightness >= LOAD_ICON_FOREGROUND_BRIGHTNESS_THRESHOLD) {
					recolored.setRGB(x, y, argb);
				} else {
					final int replacement = (alpha << 24) | (backgroundColor.getRed() << 16)
							| (backgroundColor.getGreen() << 8) | backgroundColor.getBlue();
					recolored.setRGB(x, y, replacement);
				}
			}
		}

		return new PaddedIcon(new ImageIcon(recolored), rightPadding);
	}

	private static Icon createLightenedRunningIcon(final String iconPath, final int targetWidth, final int targetHeight,
			final Color backgroundColor, final int rightPadding) {
		final Icon scaledIcon = createScaledIcon(iconPath, targetWidth, targetHeight);
		final BufferedImage source = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D sourceGraphics = source.createGraphics();
		scaledIcon.paintIcon(null, sourceGraphics, 0, 0);
		sourceGraphics.dispose();

		final BufferedImage lightened = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < targetHeight; ++y) {
			for (int x = 0; x < targetWidth; ++x) {
				final int argb = source.getRGB(x, y);
				final int alpha = (argb >>> 24) & 0xFF;
				if (alpha == 0) {
					continue;
				}
				final int red = (argb >>> 16) & 0xFF;
				final int green = (argb >>> 8) & 0xFF;
				final int blue = argb & 0xFF;
				final int brightness = (red + green + blue) / 3;
				if (brightness >= LOAD_ICON_FOREGROUND_BRIGHTNESS_THRESHOLD) {
					final int boostedRed = Math.max(red, RUNNING_ICON_MIN_FOREGROUND_CHANNEL);
					final int boostedGreen = Math.max(green, RUNNING_ICON_MIN_FOREGROUND_CHANNEL);
					final int boostedBlue = Math.max(blue, RUNNING_ICON_MIN_FOREGROUND_CHANNEL);
					final int boosted = (alpha << 24) | (boostedRed << 16) | (boostedGreen << 8) | boostedBlue;
					lightened.setRGB(x, y, boosted);
				} else {
					final int replacement = (alpha << 24) | (backgroundColor.getRed() << 16)
							| (backgroundColor.getGreen() << 8) | backgroundColor.getBlue();
					lightened.setRGB(x, y, replacement);
				}
			}
		}

		return new PaddedIcon(new ImageIcon(lightened), rightPadding);
	}

	private static final class PaddedIcon implements Icon {
		private final Icon delegate;
		private final int rightPadding;

		private PaddedIcon(final Icon delegate, final int rightPadding) {
			this.delegate = delegate;
			this.rightPadding = rightPadding;
		}

		public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
			delegate.paintIcon(c, g, x, y);
		}

		public int getIconWidth() {
			return delegate.getIconWidth() + rightPadding;
		}

		public int getIconHeight() {
			return delegate.getIconHeight();
		}
	}

	// -----------------------------------------------------------------------
	// Header-panel mode: title label on left, close button on right
	// -----------------------------------------------------------------------

	/**
	 * A tab header component that places the tab title on the left and a close
	 * button on the right. The close button background turns red on hover and
	 * darker on press. Clicking it closes the tab without bringing it to the front.
	 */
	public final class TabHeaderPanel extends JPanel {
		private final JLabel titleLabel;
		private final JButton closeButton;
		private final DbViewer dbViewer;

		private TabHeaderPanel(final String title, final DbViewer dbViewer) {
			super(new FlowLayout(FlowLayout.LEFT, 9, 0));
			this.dbViewer = dbViewer;
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

			titleLabel = new JLabel(title);
			titleLabel.setForeground(RUNNING_LABEL_COLOR);
			add(titleLabel);

			// Keep label color in sync with tab selection state.
			tabPane.addChangeListener(new ChangeListener() {
				public void stateChanged(final ChangeEvent e) {
					updateLabelColor();
				}
			});

			closeButton = new JButton(loadingIconBtn);
			closeButton.setPreferredSize(new Dimension(CLOSE_ICON_DRAW_WIDTH, CLOSE_ICON_HEIGHT));
			closeButton.setContentAreaFilled(false);
			closeButton.setBorderPainted(false);
			closeButton.setFocusPainted(false);
			closeButton.setRolloverEnabled(false); // we manage icon changes manually
			closeButton.setMargin(new Insets(0, 0, 0, 0));
			closeButton.setOpaque(false);
			add(closeButton);

			closeButton.addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(final MouseEvent e) {
					// Show red hover icon for both running and finished tabs.
					closeButton.setIcon(hoverCloseIconBtn);
				}
				@Override public void mouseExited(final MouseEvent e) {
					closeButton.setIcon(finished ? closeIconBtn : loadingIconBtn);
				}
				@Override public void mousePressed(final MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e)) return;
					closeButton.setIcon(pressCloseIconBtn);
					closeClickInProgress = true;
					lastStableSelectedIndex = tabPane.getSelectedIndex();
					pressedCloseTabIndex = tabPane.indexOfTabComponent(TabHeaderPanel.this);
				}
				@Override public void mouseReleased(final MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e)) return;
					// Stay on hover icon since the cursor is still over the button.
					closeButton.setIcon(hoverCloseIconBtn);
				}
			});

			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					doClose();
				}
			});

			// Combined listener on the panel and title label:
			//   - left-click  → forward to JTabbedPane so tab selection changes normally
			//                    (without this, adding a MouseListener to titleLabel prevents
			//                     the JTabbedPane from seeing the click and changing tabs)
			//   - right-click → show context menu deferred via invokeLater so the popup
			//                    appears AFTER the current event cycle, preventing the
			//                    mouseReleased event that triggered the popup from
			//                    immediately activating the menu item under the cursor.
			final MouseAdapter tabInteractionListener = new MouseAdapter() {
				@Override public void mousePressed(final MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						final int idx = tabPane.indexOfTabComponent(TabHeaderPanel.this);
						if (idx >= 0) tabPane.setSelectedIndex(idx);
					}
					if (e.isPopupTrigger()) {
						final MouseEvent ev = e;
						SwingUtilities.invokeLater(new Runnable() {
							public void run() { showContextMenu(ev); }
						});
					}
				}
				@Override public void mouseReleased(final MouseEvent e) {
					if (e.isPopupTrigger()) {
						final MouseEvent ev = e;
						SwingUtilities.invokeLater(new Runnable() {
							public void run() { showContextMenu(ev); }
						});
					}
				}
			};
			addMouseListener(tabInteractionListener);
			titleLabel.addMouseListener(tabInteractionListener);
		}

		/** Closes this tab — shared by the close button action and the right-click "Close" menu item. */
		private void doClose() {
			final int index = tabPane.indexOfTabComponent(TabHeaderPanel.this);
			if (index < 0 || index >= tabPane.getTabCount()) {
				closeClickInProgress = false;
				return;
			}
			final int selectedBeforeClose = lastStableSelectedIndex >= 0
					? lastStableSelectedIndex : tabPane.getSelectedIndex();
			closeClickInProgress = true;
			final QueryResultsPanel closeThread = (QueryResultsPanel) tabPane.getComponentAt(index);
			final BaseTableModel btm = DbViewer.getTableModelFromComponent(closeThread);
			if (InterfaceMain.getInstance() != null) {
				InterfaceMain.getInstance().fireProperty("Query", btm, null);
			}
			try {
				closeThread.killThreadAndWait();
			} catch (final Exception ex) {
				// Best-effort shutdown; still allow the tab to close.
			}
			tabPane.remove(index);
			restoreTabSelectionAfterClose(selectedBeforeClose, index);
			closeClickInProgress = false;
		}

		/** Builds and shows the right-click context popup. */
		private void showContextMenu(final MouseEvent e) {
			final JPopupMenu popup = new JPopupMenu();

			final JMenuItem closeItem = new JMenuItem("Close");
			closeItem.setEnabled(true); // always enabled — kill thread if still running
			closeItem.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent ae) {
					doClose();
				}
			});
			popup.add(closeItem);

			final JMenuItem saveAsItem = new JMenuItem("Save As...");
			saveAsItem.setEnabled(finished && dbViewer != null);
			saveAsItem.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent ae) {
					final int index = tabPane.indexOfTabComponent(TabHeaderPanel.this);
					if (index >= 0 && dbViewer != null) {
						dbViewer.exportSingleTabCSV(index);
					}
				}
			});
			popup.add(saveAsItem);

			popup.show(e.getComponent(), e.getX(), e.getY());
		}

		/**
		 * Called once this panel has been connected to the component hierarchy
		 * (i.e. after {@link JTabbedPane#setTabComponentAt} installs it).
		 * We use this moment to apply the correct initial label color.
		 */
		@Override
		public void addNotify() {
			super.addNotify();
			updateLabelColor();
		}

		/** Called by {@link TabCloseIcon#finishedLoading()} to switch to the close icon and restore label color. */
		void onFinished() {
			closeButton.setIcon(closeIconBtn);
			updateLabelColor();
		}

		/** Sets the label foreground to white if this tab is selected, otherwise to running/finished color. */
		private void updateLabelColor() {
			final int myIndex = tabPane.indexOfTabComponent(TabHeaderPanel.this);
			if (myIndex >= 0 && tabPane.getSelectedIndex() == myIndex) {
				titleLabel.setForeground(SELECTED_LABEL_COLOR);
			} else {
				titleLabel.setForeground(finished ? FINISHED_LABEL_COLOR : RUNNING_LABEL_COLOR);
			}
		}
	}

	public void finishedLoading() {
		// mark finished so mouse events will start showing the close/pressed icons
		finished = true;
		showingIcon = closeIcon;
		if (tabHeaderPanel != null) {
			tabHeaderPanel.onFinished();
		}
		tabPane.repaint();
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		position = new Rectangle( x,y, getIconWidth(), getIconHeight() );
		showingIcon.paintIcon(c, g, x, y );
	}

	public int getIconWidth() {
		return showingIcon.getIconWidth();
	}

	public int getIconHeight() {
		return showingIcon.getIconHeight();
	}
}
