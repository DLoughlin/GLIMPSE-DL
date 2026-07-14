# Tutorial 4꞉ Additional tools for comparing scenarios

*A note: these tutorials have been created using GLIMPSE-CE v2.03 featuring GCAM v8.2, but, aside from version numbers, they should still be mostly accurate for the latest release.*

## T4.1 Overview

The ModelInterface provides a variety of features for examining the differences from one scenario to another. Several examples were provided in [Tutorial 2](Tutorial%202꞉%20Examining%20model%20results.md), including transposing plots so to compare data by series and creating "difference" graphs. These options are particularly useful if you know ahead of time which results you would like to examine. However, in some instances, it may be difficult to determine where to get started. Helping identify major changes between scenarios is a strength of the "Diff Query" feature, which is discussed here.

## T4.2 Using the Diff Query

Using the "Diff Query" is similar to executing a query using the "Run Query" button. 

First, select the scenarios and regions of interest. Here, we have selected GLIMPSE-8.2-Ref and our new carbon tax scenario from Tutorial Part 3. For this demonstration, we will start with examining national totals, so select all of the states and the USA region, then check the box next to "Total". 

The "Diff Query" option can be used for any query, but it is often most useful for queries that return results across many sectors, including "Inputs by Tech", "Outputs by Tech", and "Prices for all markets". For this tutorial, we have chosen "8.2 Outputs by tech".

<img src='..\UsersGuideGraphics\T4-1.png' title='Selecting the "outputs by tech" query'/>

Next, press the "Diff Query" button. After several seconds to several minutes have passed, a dialog will appear that provides options for how you would like differences to be displayed. 

The top area is where you select the from which scenario differences will be calculated. Below that area, you can select the smallest difference value (Minimum value) and smallest percent difference (Minimum percent) that you are willing to display. Differences that do not meet these criteria are not displayed. 

The final option allows you to display the query results as values or percent differences. 

<img src='..\UsersGuideGraphics\T4-2.png' title='Selecting Diff Query options, including the reference scenario, difference criteria, and whether to report differences as percents'/>

Here, parameters that differ at least by "0.00001" and "30%" at some point over the modeled time horizon are shown in the table as percent changes from GLIMPSE-8.2-Ref. Note that instances involving divide-by-zero are reported as "1000" in the table.

Click "OK" to display results.

<img src='..\UsersGuideGraphics\T4-3.png' title='Diff Query results shown as percents'/>

The items and values in the table provide valuable insights into the model's response to the carbon tax. For example, in the commercial sector, by 2050, space heating by electric heat pumps and wood furnaces has increased by 57% and 56% respectively, while heating from natural gas furnaces has declined by 32%. Other sectors where major changes are occurring are residential water heating, domestic and international aviation, and cement. 

Applying the "Diff Query" to prices can also provide valuable insights. Prices are not additive (if a good is $1/unit in state A, and $4/unit in state B, the sum of those values would not be $5/unit), however, so it is recommended that the "Diff Query" be applied to prices for a single state or region at a time. 

Here, we are examining the change in "7.7 Building service costs" in North Carolina, using a minimum percent difference of 15%. These results could be useful from a policy design standpoint, particularly in determining how carbon tax proceeds could be allocated to reduce impacts. 

<img src='..\UsersGuideGraphics\T4-4.png' title='Differences in energy service costs in buildings'/>


## T4.3 Additional visualization tools

### T4.3.1 Mapping
Mapping is useful when trying to determine how policies will affect different regions.

We will use a "Diff Query" to compare CO<sub>2</sub> emissions by state between the "GLIMPSE-8.2-NZ" and "GLIMPSE-8.2-Ref" scenarios. Select both of these scenarios, the "United States" group of regions (not totaled), and the query "5.1 CO2 emissions by region", then click the "Diff Query" button. When the "Difference Options" dialog pops up, choose "GLIMPSE-8.2-Ref" as the reference scenario and select "Percent" from the "Show differences as:" dropdown, then click "OK".

<img src='..\UsersGuideGraphics\T4-5.png' title='Difference Options dialog'/>

The result will be the relative change in CO<sub>2</sub> emissions by region from the "GLIMPSE-8.2-Ref" reference scenario to the "GLIMPSE-8.2-NZ" scenario. Click the "Mapping" button at the top of the table of query results to view a map of the results. This may take a while.

When the map comes up, select a year for which the scenarios have significantly different results (e.g., 2035), and adjust the palette and color scale as desired.


<img src='..\UsersGuideGraphics\T4-6.png' title=' Map of relative change in CO2 emissions by region from the reference scenario to GLIMPSE-8.2-NZ'/>

Be aware that it may take several minutes to generate a map, and that updates to the year, palette, etc. may also be slow. A possible solution can be found in the [troubleshooting](Chapter%206꞉%20Reference.md#642-common-errors-using-the-modelinterface) section.

## T4.4 Additional analysis suggestions

Use "Diff Query" to uncover to examine technology changes in the residential, commercial, and transportation sectors. 

If you apply the "Diff Query" to emissions by sector or emissions by technology, what do the results tell you about potential low-hanging fruit for mitigating CO<sub>2</sub> emissions?

Try graphing "value" and "percent" results of the "Diff Query".
