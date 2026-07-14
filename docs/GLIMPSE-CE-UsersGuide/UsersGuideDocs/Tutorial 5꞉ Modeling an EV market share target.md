# Tutorial 5꞉ Modeling an EV market share target

*A note: these tutorials have been created using GLIMPSE-CE v2.03 featuring GCAM v8.2, but, aside from version numbers, they should still be mostly accurate for the latest release.*

## T5.1 Overview

In this part of the tutorial, we use a combination of GLIMPSE's "Market Share" and "Tech Avail" features to simulate a scenario in which the EV sales share for onroad passenger cars and trucks increases to 100% nationally by 2050. 

## T5.2 Constructing the EV sales target components

Implementing the electrification target requires two steps: representing the market share constraint and adding a complementary that addresses numerical. 

### T5.2.1 Introducing a market share constraint

We will begin by creating EV market share targets for passenger cars and trucks.  

In the *Scenario Builder*, click on the <img src='..\UsersGuideGraphics\add.png' style='height:16pt;'/> button to open the *New Scenario Component Creater* dialog, then click on the "Market Share" tab.

Select the pulldown menu next to "Type?". The menu shows several types of market share constraints, including a Renewable Portfolio Standard (RPS), Clean Energy Standard (CES), and EV targets for various vehicles classes and combinations, as well as options for light-emitting diodes (LEDs) and heat pumps. 

<img src='..\UsersGuideGraphics\T5-1.png' title='Options available in the "Type?" pulldown menu of the "Market Share" tab'/>

Select the "EV passenger cars and trucks" category.

This selection results in the "Subset" and "Superset" pulldown menus being populated. Check the settings in those pulldown menus. 

The "Subset" menu shows all "Car", "Large Car and Truck" and "Mini-Car" technologies, but only the battery electric vehicle (BEV) options have check marks next to them. 

The "Superset" menu shows the same set of technologies, but all have checked boxes. 

The selected items on these menus specify numerator (subset) and denominator (superset) for the market share constraint. The settings were chosen automatically based on the "Type" selection. However, the specific technology selections can be customized. For example, if you also want fuel-cell electric vehicles to be eligible to meet the market share constraint, you can click the box next to the FCEV technologies in the subset menu.

Next, click on the "Constraint" pulldown menu. The options listed are "Lower" and "Fixed". Choosing "Lower" indicates that you would like the constraint to be a lower bound, meaning that GCAM can opt to exceed that percentage. "Fixed" requires GCAM to hit the specified market share exactly, which can be more computationally challenging. Select "Lower". 

Next, click on the "Applied to" pulldown menu. Here you have the option of applying the constraint to "All Stock" or just to "Sales". Select "Sales" since we are setting an EV sales target. 

Then, click on the "Treatment" pulldown menu. This allows the constraint to be applied "To Each Region" or "Across Selected Regions". The latter provides GCAM with more flexibility since it may choose to have some states be above and others below the constraint as it seeks to hit the target at the lowest cost. Choose "Across Selected Regions". 

Next, we will select the regions that will be constrained. On the tree at the right side of the dialog, click the triangle next to the "USA" region. This will expand the region to show the 50 states and DC. The tree allows you to select one or more constraints. In addition, the "Presets" option at the bottom will automatically check the boxes next to specific regions, such as "New England", "North America", or "Europe". Click on the check box next to "USA", which will select all the states. 

The next step is to populate the data table in the center of the dialog. One approach is to enter a year and value in the text fields next to "Add", then pressing the "Add" button. Alternatively, you can use the "Populate" options at the bottom left to add data to the table. We use this approach in the tutorial. 

Enter "2025" in the text field next to "Start Year". 

Next, for "Initial %", enter "15", and for "Final %", enter "100". 

Press the "Populate" button, which will add your data to the table. 

<img src='..\UsersGuideGraphics\T5-2.png' title='Options chosen for the passenger vehicle market share constraint'/>

Next, press the "Save" button to save this new policy representation to the Component Library. Name the new component "EV-passenger-cars-and-trucks_All_Reg_100x50.csv". 


### T5.2.2 Addressing numerical issues using Tech Avail

A next step could be to create a new scenario based upon GLIMPSE-8.2-Ref that includes the new policy component. However, that scenario would experience solution problems in 2050 because of how the logit function assigns market shares in GCAM (see Chapter 3 for a description of how the logit works). In summary, is not possible for GCAM to find a subsidy that would achieve a 100% market share since technology costs are represented in the logit function as distributions with infinite tails. Instead, we must also introduce a complementary measure that eliminates non-EV technologies in that year. 

We can use a "Tech Avail" scenario component for this purpose. "Tech Avail" allows the range of years over which a technology is available to be specified. For the years outside this range, the technology share weight is set to zero by GLIMPSE, effectively eliminating those technologies from their respective markets. Go to this tab of the *New Scenario Component Creator*.

Start by filtering the list to show just the technologies within the on-road transit category. From the "Filter by Category" pull-down menu, choose "Trn-Onroad". In the Sector:Subsector:Technology:Units//Category column of the table, you will be able to identify the items of interest, those in the sector "trn_pass_road_LDV_4W". These technologies are the ones available to the "Car", "Large Car and Truck", and "Mini Car" categories. Only rows with checks in the "Never?" or "Range?" columns are being constrained, so currently all the boxes are unchecked.

Clicking on the "Never?" checkbox the technology in that row is not available in any year. Clicking on "Range?" results in the technology only being available between the "First" and "Last" years that are specified, inclusive of those years. 

Start by clicking the "Range" box next to all of the non-BEV technologies. Next, enter "2045" in the "Last yr" textfield near the bottom and press "Set Years". This will update the years for all technologies visible in the table. 

Next, click on the checkbox next to the "USA" region.

Your dialog should look like this.

<img src='..\UsersGuideGraphics\T5-3.png' title='"Tech Avail" setup to eliminate non-EV passenger cars and trucks in 2050'/>

Finally, press "Save" to save this new component to the Component Library. Name this component "EV-passenger-cars-and-trucks_All_Reg_100x50-part2.csv".

Add both "EV-passenger-cars-and-trucks_All_Reg_100x50.csv" and "EV-passenger-cars-and-trucks_All_Reg_100x50-part2.csv" to "GLIMPSE-8.2-Ref", then name the new scenario "GLIMPSE-8.2-PassEV100x50". 

Create the new scenario by pressing <img src='..\UsersGuideGraphics\create.png' style='height:16pt;'/>.

When you are ready, add the scenario to the execution queue by pressing <img src='..\UsersGuideGraphics\play.png' style='height:16pt;'/>. After the scenario execution completes successfully, move on to the next section of the tutorial.

## T5.3 Verifying the performance of the policy

First, we will verify that the new policy files achieved their objective. Select the "GLIMSPE-8.2-PassEV100x50" scenario, select all states and the "USA" region, select the "4.7 Transport service output by tech (sales)", click on the check box next to "Total", then press "Run Query". 

After several moments the table will be populated with data representing new sales for each transportation subsector (in units of capacity, which are million pass-km for passenger vehicles and million ton-km for freight).

We are interested in the "Car" and "Large Car and Truck" categories, so use the "Filter" option to view just those subsectors. Note that the USA region of the model does not currently include "Minicar", so there are no results for that subsector. 

Click "Graph" to visualize the data and choose "StackedBarChart" as the format.

<img src='..\UsersGuideGraphics\T5-4.png' title='Sales (in units of million pass-km) for the "Car" and "Large Car and Truck" transportation subsectors'/>

By examining the sales data, we can verify by that target was met. 

*Table T5.1 Onroad BEV sales shares by subsector and total*

| Category            | 2021 | 2025 | 2030 | 2035 | 2040 | 2045 | 2050 |
| :------------------ | :--: | ---- | ---- | ---- | ---- | ---- | ---- |
| Car                 |  0%  | 14%  | 35%  | 52%  | 71%  | 88%  | 100% |
| Large car and truck |  0%  | 16%  | 27%  | 44%  | 58%  | 74%  | 100% |
| Total               |  0%  | 15%  | 32%  | 49%  | 66%  | 82%  | 100% |

The bars for 2015 and 2021 are considerably higher than the others because 2015 and 2021 are calibration years, and vintaging is not tracked in those years. When evaluating new capacity or sales, you may choose to filter out those years from the query results, using the Filter button and related dialog to uncheck calibration years (all those prior to 2025).

<img src='..\UsersGuideGraphics\T5-5.png' title='Filtered (post-2021) sales (in units of million pass-km) for the "Car" and "Large Car and Truck" transportation subsectors'/>

Some interesting dynamics occurring in 2040 through 2050 in the "Large Car and Truck" subsector. We applied our constraint across the "Car" and "Large Car and Truck" categories. However, there are differences in the costs of electrifying vehicles in these two categories, resulting in GCAM choosing to electrify the "Car" category at a level higher than the target and the "Large Car and Truck" at a level below the target. Furthermore, our constraint was applied based on capacity in units of "million pass-km", but cars are assumed to have an average ridership of 1.58 people per vehicle, while the "Large Car and Truck" ridership is assumed to be 1.66 people per vehicle. Finally, note that our policy representation resulted in a small increase in the cost of onroad light duty travel, leading to a small amount of mode switching to buses and motorcycles, as well as a small decrease in demand for passenger travel overall.

## T5.4 Exploring the response to the EV market share target

Based upon what you have learned through the course of these tutorials, explore the answers to some of the following questions: 
-	How is electricity demand changing under this scenario?
-	How is the additional electricity being produced? 
-	How is demand for refined liquids changing across sectors? Natural gas?
-	How is refinery output changed? What is the impact on biofuel production? 
-	What are the impacts on the price of electricity, refined liquids, and natural gas?
-	How are CO<sub>2</sub> emissions impacted overall and by sector? How about air pollutants?

Remember that it does not make sense to sum prices across states or regions. 


