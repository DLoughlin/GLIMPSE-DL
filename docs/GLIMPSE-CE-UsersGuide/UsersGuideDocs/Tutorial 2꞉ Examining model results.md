# Tutorial 2꞉ Examining model results

*A note: these tutorials have been created using GLIMPSE-CE v2.03 featuring GCAM v8.2, but, aside from version numbers, they should still be mostly accurate for the latest release.*

## T2.1 Overview

The *ModelInterface* is used to access results by performing queries on the GCAM-USA database. Results are provided in a table, and additional tools are available for filtering, sorting, and visualizing the data.

## T2.2 Viewing model results with the *ModelInterface*

To view GCAM results, you will need to open the output database via the *ModelInterface*. The "results" button, <img src='..\UsersGuideGraphics\results.png' style='height:16pt;'/>, will open the *ModelInterface*, initialized to the output database specified in the options file. Alternatively, press <img src='..\UsersGuideGraphics\results-selected.png' style='height:16pt;'/> to initialize the *ModelInterface* to the database referenced in the selected scenario.

For this tutorial, select the scenario "GLIMPSE-8.2-Ref" in the *Scenario Builder*'s *Scenario Library*. Then, click the results button with the arrow, <img src='..\UsersGuideGraphics\results-selected.png' style='height:16pt;'/>.

Within a few seconds, the *ModelInterface* window should appear. You should see the "GLIMPSE-8.2-Ref" scenario in the "Scenarios" pane, followed by the date and time that it was loaded into the output database. Any other loaded scenarios will also appear.

<img src='..\UsersGuideGraphics\T2-1.png' title='The ModelInterface'/>


In the "Regions" pane, all the socio-economic regions included in GCAM-USA are listed, as well as the model's electricity transmission grid regions within the U.S. 

Note that most of the energy system-related activities within the U.S. have been disaggregated to the state level. Thus, the "USA" region does not provide national totals. Instead, "USA" includes several sectors that have not been apportioned to states, including agriculture, coal mining, oil and gas operations, and hydrogen production. 

The "Queries" pane lists the various outputs that can be extracted from the output database. Currently, several hundred queries are available. The queries are organized in a tree structure. 

The "GLIMPSE" set of queries includes those that we anticipate will be of particular interest to many GLIMPSE users. These queries are grouped into sub-categories, including "Primary and final energy", "Energy use by end-use sector", "Conversion technologies inputs and outputs", "End-use technology energy use and service output", "Emissions", "Impacts", "Markets, prices, and costs", "Inputs and outputs", "Assumptions", and "Other". For the queries under "GLIMPSE", hovering the mouse over a query will pop up a tooltip with a description of the query. Several queries are specific to either GCAM or GCAM-USA, as indicated in their title. Different versions of these queries are necessary because of the naming and structural differences between the two versions of the model. 

Additional queries follow, grouped into the category "Standard GCAM 8.2 queries". These are the queries that are distributed with GCAM by PNNL. 

For this tutorial, we will use several commonly examined outputs, including: 
-	"CO2 emissions by region"
-	"Electricity generation by aggregate subsector with renewable detail"

In the "Scenario" pane, select "GLIMPSE-8.2-Ref".

Next, in the "Regions" pane, we want to select all the states and the "USA" region. An easy way to do this is with the following steps: 
-	Scroll down to "WY" and click select it.
-	Scroll up to "AK" and shift-click select it.
-	Scroll up to the top and control-click on "USA". 

Alternately, use the "Group:" dropdown to select "United States".

In the "Queries" list, scroll down and select the query: *Queries->GLIMPSE queries->5. Emissions->5.1 CO2 emissions by region*.

Next, click on the "Run Query" button. 

A tab will appear at the bottom of the *ModelInterface*, showing the message "Waiting for query to complete. Close to terminate." After a short period of time, the tab will be populated by a table that shows the query results for each selected scenario and region. 


<img src='..\UsersGuideGraphics\T2-2.png' title='Viewing the results of a query at the regional level'/>


The *ModelInterface* also makes it very easy to develop regional totals. To do this, click on the "Total" button at the bottom of the "Regions" panel, then re-run the query by clicking on "Run Query". The values that are reported in the table now represent the sum across all selected regions. 

<img src='..\UsersGuideGraphics\T2-3.png' title='Viewing the results of a query, totaled across selected regions'/>


## T2.3 Analyzing model results outside of the *ModelInterface*

There are several ways to analyze the results of the queries. One is to move the data to a spreadsheet. If you have Excel open, you can drag the label of the tab associated with a query over to an Excel worksheet and drop the data there. This approach moves the table headings along with the data. Using the "Copy" button in the *ModelInterface* and then pasting the data into Excel has the same effect.

For large datasets (e.g., with several hundred rows or more), this approach can be very overly memory intensive. An alternative is to select rows in your table (multi-selecting with shift-click or control-click), then pressing Ctrl-C to copy the data. You can then paste the data into the spreadsheet by choosing a location and pressing Ctrl-V. This second approach only pastes the contents of the table; it does not paste the headings. 

The *ModelInterface* offers a third mechanism for exporting query results. If you have already executed the queries of interest, one or more tables of data will be shown on tabs. Choose "File->Export tabs as CSVs" from the main *ModelInterface* menu bar. A file browser will appear, and you can select the folder where you would like to place the query results. The data on each tab, including the column headers, are then written to that folder as CSV files, with the file names based on the tab names. It is worth noting that an issue may arise when opening these CSV files as spreadsheets: the scenario name features a comma, so this value may be split between two cells, resulting in mislabeled columns and some other issues with text-to-columns conversion.

For practice, use each of the approaches described above to transfer the query results to an Excel workbook. 


## T2.4 Using the *ModelInterface* to visualize model results

The *ModelInterface* includes graphical capabilities that are intended to support interactive, exploratory data analysis – allowing the user to quickly visualize and iteratively explore the model results. In this section, we use these tools to understand electricity production in the "GLIMPSE-8.2-Ref" scenario.

Verify that the "GLIMPSE-8.2-Ref" scenario is selected, the regions for all of the states and the USA are selected, and the "Total" box is checked, then run the query "3.10 Electricity generation by aggregate subsector with renewable detail". When the query is complete, your *ModelInterface* should report resulting electricity production values.

<img src='..\UsersGuideGraphics\T2-4.png' title='Examining electricity production by subsector'/>

Click the "Graph" button above the table. A simple line chart will appear to the right of the table. 

<img src='..\UsersGuideGraphics\T2-5.png' title='Generating a thumbnail graphic'/>

This image is a thumbnail. Clicking on it will display a larger version with a legend. Right-clicking on this larger image and choosing "Copy" will add a copy of the image to your computer's clipboard, allowing it to be pasted into other documents. <span style='color:red'>Note that this option may be problematic on Corretto's versions of Java and may cause the *ModelInterface* to freeze.</span> Alternatively, you can right-click and choose "Save As…" to save the image as a PNG file or use Windows screen capture tools to copy the image. 

<img src='..\UsersGuideGraphics\T2-6.png' title='Clicking on the thumbnail produces a larger version with legend'/>

There are a variety of options for modifying the figure, including changing the texture and shading of the lines. These changes can be saved and are applied automatically the next time a similar figure is plotted.

Other types of graphs can also be displayed. Above the thumbnail, change the selection in the pulldown menu from "LineChart" to "StackedBarChart". The thumbnail is updated to reflect the change. 

<img src='..\UsersGuideGraphics\T2-7.png' title='Viewing query results as a stacked bar chart'/>

Try out some of the other graphing options.

## T2.5 Comparing results across scenarios

Next, we will walk through some ways you can use the ModelInterface to compare the results of two scenarios. 

We will be comparing CO<sub>2</sub> emissions in the "GLIMPSE-8.2-Ref" and "GLIMPSE-8.2-NZ" scenarios. If the "GLIMPSE-8.2-NZ" scenario has not been run yet, close the *ModelInterface* and run that scenario before continuing. Once finished, reopen the *ModelInterface*.

In the Scenario pane at the top left of the ModelInterface, keep "GLIMPSE-8.2-Ref" selected, but add "GLIMPSE-8.2-NZ" to the selection by control-clicking on it.

In the Query pane on the top right, change your selection to "5.2 CO2 emissions by aggregate sector". 
Make sure the "Total" checkbox is checked, then click on the "Run Query" button. After several seconds, the table will be populated with CO2 emissions data. 

Click on the "Graph" button to generate thumbnails. You may need to reposition the dividers to see both thumbnails fully. Click on the "Options" button in the green area above the thumbnails and click "Same Scale". This will ensure that the Y axis is shown at the same scale for all thumbnails. 

<img src='..\UsersGuideGraphics\T2-8.png' title='Comparing two results scenarios graphically'/>

As before, you can click on each thumbnail to view a larger version. However, additional features facilitate comparison. 
Click on "Options" in the green area above the thumbnails and click "Transpose".

A popup window of thumbnails appears, but this time each series is given its own plot and the results of the two scenarios are compared. 

<img src='..\UsersGuideGraphics\T2-9.png' title='Using the transpose option to compare the scenario results by series'/>

If we click the "electricity" thumbnail, we can see that CO<sub>2</sub> emissions from electricity production are lower in "GLIMPSE-8.2-NZ" (red) than in the reference scenario (blue).

<img src='..\UsersGuideGraphics\T2-10.png' title='Comparing output from coal plants in the electric sector from one scenario across scenarios'/>

Close these windows when you are ready.

Next, we will generate a "difference" plot.

First, change the chart type to "StackedBarChart" and ensure "Same Scale" is selected to synchronize the magnitude of the Y-axis. 

<img src='..\UsersGuideGraphics\T2-11.png' title='Viewing the thumbnails as stacked bar charts'/>

Click on "Options" and then "Difference". A popup appears with the names of all of the scenarios in the table. First, select the GLIMPSE-8.2-NZ scenario, then press "OK".

<img src='..\UsersGuideGraphics\T2-12.png' title='Specifying which scenario to subtract from the other (typically, the Reference Scenario is the second selected)'/>

A new popup should appear. Select the GLIMPSE-8.2-Ref reference scenario and press "OK".

A difference plot appears. Items that are greater in the NZ scenario appear above the line, while those that are less are shown below it.

<img src='..\UsersGuideGraphics\T2-13.png' title='Difference plot where items above the axes represent increases and items below the axis represent decreases'/>

The updates in GLIMPSE-8.2-NZ have resulted in increased CO<sub>2</sub> emissions due to fuel production but reduced emissions in every other sector, with particularly large changes in emissions due to electricity production and biomass growth.

Stacked bar plots are particularly useful for showing changes from one scenario to another.

## T2.6 More visualization options
Additional visualization options for analyzing query results are available in GLIMPSE v2.

### T2.6.1 Sankey diagrams
Sankey diagrams are useful for visualizing energy and material flows. 

We will be looking at the results of the "End-use energy consumption (aggregated)" query for the "GLIMPSE-8.2-Ref" scenario, totaled across the United States. 

<img src='..\UsersGuideGraphics\T2-14.png' title='Examining aggregated end-use energy consumption'/>

After running the query, click on the "Sankey" button located above the table. A Sankey diagram should appear quickly.

<img src='..\UsersGuideGraphics\T2-15.png' title='Sankey diagram of end-use energy consumption by input and sector'/>

Hover over lines on the diagram to see their values. For the "GLIMPSE-8.2-Ref" scenario, the model projects that 14.0 EJ of energy from refined liquids will be consumed by the on-road transport sector in 2050.

It is recommended that Sankey diagrams generated from queries not in section "10.4 for Sankey diagrams" be treated with caution. Flows may be displayed incorrectly when more than two categories (e.g., input, technology, subsector, sector) are included. Also, a query's Sankey diagram will not be useful if the units of the results are inconsistent, though this can be dealt with by filtering results to include only one type of unit.

### T2.6.2 Mapping

To create a map, ensure that the "Total" box is not checked when you run a query, then select a row of the query results and click the "Mapping" button above the table. It may take several seconds for a map to appear, but if it takes multiple minutes, consider [troubleshooting](Chapter%206%EA%9E%89%20Reference.md#642-common-errors-using-the-modelinterface). A step-by-step tutorial is available in [Tutorial 4](Tutorial%204꞉%20Additional%20tools%20for%20comparing%20scenarios.md#t431-mapping).


## T2.7 Additional suggestions for exploration

Use the *ModelInterface*'s graphical capabilites to examine other outputs of interest, and how these outputs differ between scenarios. Suggestions include:
- 1.4 Final energy consumption by aggregate sector
- 4.4 Building service output by tech 
- 3.15 Refined liquids production by tech (GCAM-USA)
- 4.8 Passenger car and truck service output by tech

