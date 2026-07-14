# Tutorial 3꞉ Modeling and evaluating a carbon tax

*A note: these tutorials have been created using GLIMPSE-CE v2.03 featuring GCAM v8.2, but, aside from version numbers, they should still be mostly accurate for the latest release.*

## T3.1 Overview
In this portion of the tutorial, we will develop a new scenario that incorporates a hypothetical economy-wide tax on Carbon. The tax will start at a value of \$100/tC in 2025 (in 1990\$s) and will increase at 5% per year. Please note that this tax is being applied per metric tonne of Carbon. Converting this its \$/tCO<sub>2</sub> equivalent involves multiplying it by 12/44ths, the ratio of the molecular weight of C to the molecular weight of CO<sub>2</sub>. By default, GCAM uses 1990\$s. \$1 in 1990 is the equivalent of \$2.27 in 2022. Considering both conversions, our starting tax is approximately \$62/tCO<sub>2</sub> in 2022\$s. 

## T3.2 Constructing a carbon tax scenario component

Our first step is to create a new Scenario Component that represents our hypothetical tax. First, click on the "New" button in the *Component Library*, <img src='..\UsersGuideGraphics\add.png' style='height:16pt;'/>. This will pop up the *New Scenario Component Creator*. Tabs across the top represent several types of policies or alternative assumptions that can be created. Ensure that "Pollutant Tax/Cap" is selected. 

<img src='..\UsersGuideGraphics\T3-1.png' title='The New Scenario Component Creator dialog'/>

The top-left portion is where we define what type of policy we will be creating. The middle portion will contain the values associated with the policy. The tree at the right allows us to specify to which regions or states the policy applies. The bottom-left portion can be used to automate the process of populating the central table with data.

In the top-left portion, under the "Measure:" choice menu, you will find the two types of policies that can be created on this tab. Select "Emission Tax ($/t)". 

Next, the "Pollutant:" choice menu can be used to select to which pollutant the tax will be applied, choose "CO2 (MTC)".

We want this to be an economy-wide tax, so in the "Category:" choice menu, choose "All". 

The tax will start at $100/tC, starting in 2025, so change the "Start Year:" value to "2025" and add "100" and "5" in the "Initial Val:" and "Growth (%):" boxes, respectively.

Above the table, press the "Populate" button to populate the table with values that reflect these options. 

The tax will be applied to all US states, so click the check box next to "USA" on the region table. 

When you have completed these steps, the scenario component window should look like this: 

<img src='..\UsersGuideGraphics\T3-2.png' title='Specifying the options for our carbon tax policy'/>

Next, press the "Save" button. The "Save As" dialog will appear, and a default name will be provided. Here, the name has been changed to be more descriptive. When naming scenario components and scenarios in GLIMPSE it is important only to use alpha-numeric characters and either "_" or "-". Do not use spaces, or other non-alpha-numeric characters, such as "+", "/" or "\". Do not use spaces in Scenario Component filenames.

<img src='..\UsersGuideGraphics\T3-3.png' title='Saving our scenario with a unique name'/>

Note that the New Scenario Component Creator does not automatically disappear after you have clicked "Save". This behavior is intentional and is intended to allow the user to modify the constraint readily. 

Once you have pressed "Save", the component will appear in the Component Library. By default, the components are in alphabetical order. You can click on the appropriate column name to sort the table. Here, the components are sorted in reverse order by the date they were created: 

<img src='..\UsersGuideGraphics\T3-4.png' title='Selecting a scenario to modify'/>

The next step is to construct a scenario that includes this policy. We will do this by modifying an existing scenario. 

Start by clicking on "GLIMPSE-8.2-Ref" in the *Scenario Library*. Then, click on the <img src='..\UsersGuideGraphics\up_right_arrow.png' style='height:16pt;'/> button to allow you to modify it.

If there were components associated with "GLIMPSE-8.2-Ref", they would appear in the table at the top right; as it is, the table should remain empty.

Change the name above the table to "GLIMPSE-8.2-Ref-Tax-C100dpt5pct" to describe your new scenario. Then, click on the new tax component in the *Component Library* and press the <img src='..\UsersGuideGraphics\right_arrow.png' style='height:16pt;'/> button to add it to your scenario:

<img src='..\UsersGuideGraphics\T3-5.png' title='Adding the new scenario component to the scenario'/>

To create the new scenario, press the <img src='..\UsersGuideGraphics\create.png' style='height:16pt;'/> button. A dialog will appear that indicates the name of the scenario, the database to which the results will be sent, the final year to include in the simulation, which region to use as the debug region, whether to create the debug file, and check boxes allowing you to choose which files to save when the scenario run is complete.

Many of these settings are available in the GLIMPSE options file, but this dialog gives the ability to override those settings. The debug file includes detailed outputs for a specific region. Only one region or state can be selected since the debug file is very large.

The "Use all available processors?" option allows you to indicate that GCAM should distribute the computational load across all available processing cores. In general, this option should be selected. However, there may be instances where unsolved markets occur as a result of the preemptive calculations that occur during parallel computations. For scenarios that have unsolved markets for which the cause is unclear, repeating the model run, but without the "Use all available processors?" box checked, may help pinpoint or eliminate errors introduced by parallel calculations as the cause. 

There is a box to add comments or other meta-data that describes the scenario. 

<img src='..\UsersGuideGraphics\T3-6.png' title='The Creating Scenario dialog'/>

Note that the database size is important. When the database reaches approximately 40 GB, it can no longer be opened in Windows. If you attempt to create a new scenario and the database size is already 36 GB, a warning message will occur. See the Users' Guide for information on managing database size. 

Once you press "OK", the new scenario will be added to the *Scenario Library*. Double-clicking on the new scenario's name in the *Scenario Library* will display its configuration file. 

<img src='..\UsersGuideGraphics\T3-7.png' title="The new scenario's configuration file"/>

Opening the scenario folder by pressing <img src='..\UsersGuideGraphics\open_folder1.png' style='height:16pt;'/> will show the XML-formatted files that were generated from scenario components during the scenario's creation.

<img src='..\UsersGuideGraphics\T3-8.png' title="Contents of the scenario's folder"/>

Close the text editor and scenario folder.

Next, start running the scenario by clicking on "GLIMPSE-8.2-Ref-Tax-C100dpt5pct" and pressing play, <img src='..\UsersGuideGraphics\play.png' style='height:16pt;'/>.

To view diagnostic information about the run, open the GLIMPSE Console <img src='..\UsersGuideGraphics\console.png' style='height:16pt;'/>.

Once the run has completed after 0.5 to 3 hours, you will see its status updated in the Scenario Library. 

<img src='..\UsersGuideGraphics\T3-9.png' title=After the new scenario run has been completed, its status is updated''/>

## T3.3 Exploring the response to the tax

The ModelInterface provides a variety of features for examining the differences between scenarios. Several examples were provided in Tutorial Part 2, including transposing plots so to compare data by series and creating "difference" graphs. In this section, we use similar approaches to examine the impacts of the carbon tax policy.

First, start up the *ModelInterface* by clicking on "GLIMPSE-8.2-Ref-Tax-C100dpt5pct" in the *Scenario Library*, then clicking the Results button with the arrow, <img src='..\UsersGuideGraphics\results-selected.png' style='height:16pt;'/>. GLIMPSE will then access the scenario's configuration file, read the name of the database to which its results were stored, and then open that database via the *ModelInterface*.

For this tutorial we will be exploring the national-scale impacts of the tax policy. Select "GLIMPSE-8.2-Ref" and "GLIMPSE-8.2-Ref-Tax-C100dpt5pct" in the Scenario pane. Next, in the Regions pane, select all states and the USA region. Click the check box next to "Total" to obtain national totals.

Select "5.1 C02 emissions by region", then click the "Run Query" button. The results will populate the table after several seconds.

<img src='..\UsersGuideGraphics\T3-10.png' title='CO2 emissions for the two scenarios'/>

Under the policy, CO<sub>2</sub> emissions decrease to 472 MTC by 2050. This is 592 MTC less than "GLIMPSE-8.2-Ref" in 2050 and a reduction of nearly 64% from 2021 levels. Pressing the "Graph" button shows each scenario's CO<sub>2</sub> trajectory on a separate thumbnail. However, using the "Options->Transpose" option and clicking on the resulting thumbnail generates the following graphic.

<img src='..\UsersGuideGraphics\T3-11.png' title='Visualizing the CO2 trajectories for the two scenarios'/>

Next, we will explore from which sectors GCAM is obtaining CO<sub>2</sub> reductions.

Select and execute the "5.2 CO2 emissions by aggregate sector" query. Graph the results, then change the graph format to stacked bar charts.

Next, using "Options->Difference", generate a difference plot. When the popup appears to select scenarios, first click on "GLIMPSE-8.2-Ref-Tax-C100dpt5pct" and click "OK", then click on "GLIMPSE-8.2-Ref" and click "OK".

This ordering - policy case, then reference case - is typical because values that go up as a result of the policy will be shown in the positive direction and values that go down will be negative.

<img src='..\UsersGuideGraphics\T3-12.png' title='Difference graph showing the sectoral CO2 response to the tax'/>

In the graphic, we see that the tax has resulted in reductions in emissions from the electric sector that grow to 210 MTC in 2050. Negative emissions associated with biomass growth grow to approximately 314 MTC in 2050. Emission reduction contributions from other sectors are comparatively small in this scenario.

Next, we will examine the electric sector and biomass responses further.

Run the "3.10 Electricity generation by aggregated subsector with renewable detail" query to view how electricity is being produced for each scenario.

Graph and view the results as stacked bar charts.

<img src='..\UsersGuideGraphics\T3-13.png' title='Electricity production by technology category for the Reference and Tax scenarios'/>

Next, use "Options->Difference" to show the changes in electricity production under the tax. The results suggest that the tax may result in reductions in electricity production from conventional coal and gas, offset by renewables, fossil production with CCS, biomass with CCS, and nuclear power. Overall, the quantity of electricity produced does not change substantially since the height of the stacked bars above and below 0 for each time period are roughly the same. 

<img src='..\UsersGuideGraphics\T3-14.png' title='Difference graph showing the electric sector response to the tax'/>

In this result, biomass use is increasing in the electric sector. Next, we will explore the degree to which biomass use is increasing in other sectors as well. 

Run the query "2.5 Biomass use by aggregate sector", graph the results, and display them as stacked bar charts. The results suggest that the greatest increase in biomass use is in the fuel production sector. 

<img src='..\UsersGuideGraphics\T3-15.png' title='Results of "biomass use by aggregate sector" query'/>

The query "3.15 Refined liquids production by tech (GCAM-USA)" provides insight into how this biomass is being used.

Run that query, then generate a difference plot indicating how refined liquids production technologies are responding to the tax. Here, we see a reduction in oil refining, which is being offset by an increase in bio-refining, some of which integrates CCS. 


<img src='..\UsersGuideGraphics\T3-16.png' title='Difference graph showing the response to the tax in refined liquids production'/>

In the [next part of the tutorial](Tutorial%204꞉%20Additional%20tools%20for%20comparing%20scenarios.md), we explore additional ways to identify the impacts of policy across scenarios.

