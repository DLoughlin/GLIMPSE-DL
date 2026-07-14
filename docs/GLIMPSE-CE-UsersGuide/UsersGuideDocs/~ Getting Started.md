# Getting Started

<details open><summary><b>Sections</b></summary>
<br>
  
[Overview](#overview)

[Installation instructions](#installation-instructions)

[Next steps](#next-steps)
</details>

## Overview
GLIMPSE-CE (GCAM Long-term Interactive Multi-Pollutant Scenario Evaluator - Community Edition) is a graphical user interface for [GCAM](https://github.com/JGCRI/gcam-core) (Global Change Analysis Model), an open-source human-Earth systems model. GCAM development is organized by the [Joint Global Change Research Institute](https://www.pnnl.gov/jgcri) (JGCRI). If you would like to be on an email list for GLIMPSE-CE updates, please email Dan Loughlin at [Dan@En2MG.com](Dan@En2MG.com).  

Please note: GLIMPSE-CE is a separate project from PNNL's GLIMPSE, which is a tool for visualizing power grids and not related to GCAM or integrated assessment modeling. GLIMPSE-CE is a derivative of [U.S. EPA's GLIMPSE software](https://epa.gov/glimpse).

If you would like to use GLIMPSE-CE, please see the [latest releases](https://github.com/DLoughlin/GLIMPSE-CE/releases) on GitHub. The releases include all necessary files and libraries, including a version of the Jave JRE that includes the JavaFX libraries used by the Scenario Builder.

**This Users' Guide is up to date through GLIMPSE-CE version 2.2, though the tutorials and various figures have been created with version 2.03.**

From here on, the GLIMPSE-CE software will primarily be referred to as GLIMPSE. 

## Installation instructions

### System requirements

GLIMPSE is available on Windows PCs and Macs only, although a Linux version is expected to be supported in the future. We recommend installation on computers with 20 GB of RAM or more and with more than 100 GB of free hard disk space. 

Typical model runtime is 30 minutes to 5 hours, depending on computational power, memory, and the complexity of the scenarios being simulated.

### Installation steps

Please read these instructions in full before installing.

#### Step 1: Download GLIMPSE zip file from the [release page](https://github.com/DLoughlin/GLIMPSE-CE/releases/tag/v2.0-2026.03.05).

The GLIMPSE package is approximately 1GB in its zipped form.

#### Step 2: Unzip file to install the GLIMPSE package.

The unzipped GLIMPSE file will be approximately 7GB in size.

Recommendations when selecting a location for the GLIMPSE folder:

* a hard disk with fast read/write speeds and at least several hundred free GB of storage
* a location that is **not** automatically backed up
(GCAM generates 1-2 GB of output with every execution. Users have experienced difficulties with installing GLIMPSE on OneDrive.)
* a location without spaces in folder names
(these result in errors in filename parsing within GLIMPSE)

One option is to create the following folders and install GLIMPSE there:
'C:/Users/\[USERNAME]/Documents/\[LOCAL\_FOLDER]/GLIMPSE-CE-\[VERSION]'

For the purposes of these instructions, GLIMPSE is being installed in the following location:
'D:/mhlou/Projects/GLIMPSE-CE-2.03'

This folder should include the contents shown in the image below.

<img src='..\UsersGuideGraphics\T1-1.png' title='The GLIMPSE folder'/>

When unzipping, please note that various unzip programs treat the root folder of the zipped file differently, and a common installation issue is the nesting of folders:
'D:/mhlou/Projects/GLIMPSE-CE-2.03/GLIMPSE-CE-2.03'

If this is the case in your installation, please move the contents of the nested GLIMPSE folder (D:/mhlou/Projects/GLIMPSE-CE-2.03/GLIMPSE-CE-2.03) to the parent folder (D:/mhlou/Projects/GLIMPSE-CE-2.03). You can then delete the nested folder (D:/mhlou/Projects/GLIMPSE-CE-2.03/GLIMPSE-CE-2.03).

#### Step 3: Test the GLIMPSE software.

Start GLIMPSE by double-clicking on 'run\_GLIMPSE\_GCAM-USA-9.1.bat' or the equivalent (here 'run\_GLIMPSE\_GCAM-USA-8.2.bat'). The GLIMPSE *Scenario Builder* may take a few minutes to open.

If the GLIMPSE window does not appear, a common cause is that Windows is preventing execution of the "run\_GLIMPSE\_GCAM-USA-9.1.bat" file since execution of ".bat" files can be a security issue. To signal to Windows that it is OK to allow this file to be run, right-click on the file, choose "properties", and then check the box next to "unblock execution". Then try double-clicking on the file again.

Once GLIMPSE appears, the Component Library in the top left pane should include a number of scenario components, and there should be at least one scenario listed in the Scenario Library table at the bottom. If there are no scenario components, this indicates that the installation was not successful, and that GLIMPSE cannot find the correct folder.

<img src='..\UsersGuideGraphics\T1-2.png' title='The Scenario Builder'/>

Next, click on "Tools->Check Installation" from the main menu bar of the Scenario Builder. When you do this, GLIMPSE analyzes the options that were loaded from the "options\_GCAM-USA-9.1.txt" file, noting when critical options have not been specified or where folders specified in the file do not exist.

Then, GLIMPSE checks for a frequent problem when installing updates to GLIMPSE that leads to an incorrect folder structure. Finally, GLIMPSE checks the JAVA\_HOME environmental variable that was specified in "run\_GLIMPSE\_GCAM-USA\_9.1.bat" to ensure that this version of Java is on the system. The results of these checks are reported to a popup window. If all checks are successful, the text will include: "Installation appears to be successful."

<img src='..\UsersGuideGraphics\T1-0.png' title='Output from the Check Installation option'/>

#### Step 4 (Optional): Configure GLIMPSE to use specific text and XML editors

By default, GLIMPSE is configured to use Window's Notepad application to open text and XML files. Alternatively, you can specify that GLIMPSE use different applications to open these files. For example, Notepad++ is an open-source text editor that will automatically color-code and format XML files. To change the specified editors, open the "options\_GCAM-USA-9.1.txt" file and find the lines starting with "textEditor" and "xmlEditor". Change these to refer to your preferred applications. If you have installed Notepad++ on your computer, you can alternatively remove the comment symbol, #, from "#textEditor" and "#xmlEditor" and add it to the start of the prior lines.
This change will not take effect until you either restart GLIMPSE or choose "File->Reload Options" from the main pulldown menu of the Scenario Builder.

## Next steps

At this point, GLIMPSE should be set up correctly on your computer. We recommend reading [Chapter 1: Overview](UsersGuideDocs/Chapter%201꞉%20Overview.md) and [Chapter 2:  GLIMPSE Reference Scenario (coming soon)](UsersGuideDocs/Chapter%202꞉%20Placeholder.md), then going through the provided [tutorials](UsersGuideDocs/Tutorial%201꞉%20Running%20GCAM%20through%20GLIMPSE.md).


