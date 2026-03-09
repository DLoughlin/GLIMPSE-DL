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
*/package glimpseElement;

import java.util.Date;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScenarioRow {

	private final StringProperty scenName = new SimpleStringProperty(this, "scenName", null);
	private final StringProperty components = new SimpleStringProperty(this, "components", null);
	private final StringProperty createdDate = new SimpleStringProperty(this, "createdDate", null);
	private final StringProperty startedDate = new SimpleStringProperty(this, "startedDate", null);
	private final StringProperty completedDate = new SimpleStringProperty(this, "completedDate", null);
	private final StringProperty status = new SimpleStringProperty(this, "status");
	private final StringProperty noErr = new SimpleStringProperty(this, "noErr");
	private final StringProperty unsolvedMarkets = new SimpleStringProperty(this, "unsolvedMarkets");
	private final StringProperty noIter = new SimpleStringProperty(this, "noIter");
	private final StringProperty runtime = new SimpleStringProperty(this, "runtime");

	// Cached tooltip text; null = not yet computed, "" = no content, else tooltip text
	private volatile String cachedTooltipText = null;
	// Guards against launching duplicate background threads for the same row
	private final AtomicBoolean computingTooltip = new AtomicBoolean(false);
	
	public ScenarioRow(String name) {
		this.scenName.set(name);
		this.status.set("");
	}
	

	public final StringProperty scenNameProperty() {
		return scenName;
	}
	
	public final String getScenName() {
		return scenName.get();
	}
	
	public final String getScenarioName() {
		return scenName.get();
	}
	
	public final void setSenarioName(String name) {
		scenName.set(name);
	}

	public final StringProperty componentsProperty() {
		return components;
	}
	
	public final String getComponents() {
		return components.get();
	}
	
	public final void setComponents(String txt) {
		components.set(txt);
	}
	
	public final StringProperty statusProperty() {
		return status;
	}
	
	public final String getStatus() {
		return status.get();
	}	

	public final void setStatus(String txt) {
		status.set(txt);
	}	
	
	public final StringProperty unsolvedMarketsProperty() {
		return unsolvedMarkets;
	}
	
	public final String getUnsolvedMarkets() {
		return unsolvedMarkets.get();
	}	
	
	public final void setUnsolvedMarkets(String txt) {
		unsolvedMarkets.set(txt);
	}	
	
	public final StringProperty noErrProperty() {
		return noErr;
	}
	
	public final String getNoErr() {
		return noErr.get();
	}	
	
	public final void setNoErr(String txt) {
		noErr.set(txt);
	}	
	
	public final StringProperty noIterProperty() {
		return noIter;
	}
	
	public final String getNoIter() {
		return noIter.get();
	}	
	
	public final void setNoIter(String txt) {
		noIter.set(txt);
	}	
	
	public final StringProperty runtimeProperty() {
		return runtime;
	}
	
	public final String getRuntime() {
		return runtime.get();
	}	
	
	public final void setRuntime(String txt) {
		runtime.set(txt);
	}	

	public final String getCachedTooltipText() {
		return cachedTooltipText;
	}

	public final void setCachedTooltipText(String text) {
		cachedTooltipText = text;
	}

	public final boolean markTooltipComputationStarted() {
		return computingTooltip.compareAndSet(false, true);
	}

	public final void markTooltipComputationFinished() {
		computingTooltip.set(false);
	}

	public final StringProperty createdDateProperty() {
		return createdDate;
	}
	
	public final String getCreatedDate() {
		return createdDate.get();
	}
	
	public final void setCreatedDate(Date date) {
		createdDate.set(date.toString());
	}	
	
	public final void setCreatedDate(String date) {
		createdDate.set(date);
	}	
	
	public final void setCreatedDate(Long date) {
		createdDate.set(new Date(date).toString());
	}		
	
	public final StringProperty completedDateProperty() {
		return completedDate;
	}
	
	public final String getCompletedDate() {
		return completedDate.get();
	}	
	
	public final void setCompletedDate(Date date) {
		completedDate.set(date.toString());
	}	
	
	public final void setCompletedDate(String date) {
		completedDate.set(date);
	}	
	
	public final void setCompletedDate(Long date) {
		completedDate.set(new Date(date).toString());
	}	
	
	public final StringProperty startedDateProperty() {
		return startedDate;
	}
	
	public final String getStartedDate() {
		return startedDate.get();
	}	
	
	public final void setStartedDate(Date date) {
		startedDate.set(date.toString());
	}	
	
	public final void setStartedDate(String date) {
		startedDate.set(date);
	}	
	
	public final void setStartedDate(Long date) {
		startedDate.set(new Date(date).toString());
	}	


}