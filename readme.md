## Note: This fork is a derivative of the EPA GLIMPSE Project. GLIMPSE-CE is not sponsored by the U.S. EPA.

# GLIMPSE Project

NOTICES: 
* If you would like to use GLIMPSE-CE, please see the link to the releases to the right. The releases include all necessary files and libraries, including a version of the Jave JRE that includes the JavaFX libraries used by the Scenario Builder.
* In a recent commit, the gcam 8.2 files were removed from the GCAM-Model folder. These files are included in the downloadable releases.

## Overview

GLIMPSE (GCAM Long-term Interactive Multi-Pollutant Scenario Evaluator; https://epa.gov/glimpse) is a graphical user interface for GCAM (Global Change Analysis Model; https://github.com/JGCRI/gcam-core), and open-source human-Earth systems model. GCAM development is organized by the Joint Global Change Research Institute (JGCRI; https://www.pnnl.gov/jgcri). If you would like to be on an email list for GLIMPSE-CE updates, please email Dan Loughlin at Dan@En2MG.com.  

Please note: GLIMPSE-CE is a separate project from PNNL's GLIMPSE, which is a tool for visualizing power grids and not related to GCAM or integrated assessment modeling. GLIMPSE-CE is a derivative of U.S. EPA's GLIMPSE software.

## Requirements

We recommend installation on computers with 20 GB of RAM or more and with more than 100 GB of free hard disk space. GLIMPSE consists of two major components: the GLIMPSE-CE ScenarioBuilder and the GLIMPSE-CE ModelInterface. The ScenarioBuilder has been developed and tested on Windows 10, Windows 11. It has also recently been ported to Ubuntu linux. The GLIMPSE-CE ModelInterface can be used independently and has been succesfully tested on Mac and Linux operating systems.

## Important information

The User's Guide is carried over from EPA's GLIMPSE for now. Please note that the results shown in the guide are for a different version of GCAM and will be different. Also, many aspects of the GLIMPSE-CE graphical user interface have been updated, so features may be slightly different than they appear in the User's Guide. Nonetheless, the User's Guide is a good place to start with GLIMPSE-CE. PThe User's Guide, which can be found in the Docs folder, for installation instructions. We also recommend the tutorials as a good starting place for learning to operate many of GLIMPSE's features. 

Several additional notes for consideration:

* You can find the full GLIMPSE-CE downloadable package at the "Releases" link to the right (GLIMPSE-CE-Windows-Y.Y-vYear.Month.Day.zip and LIMPSE-CE-Linux-Y.Y-vYear.Month.Day.zip). The Linux version has been tested on Ubuntu only. We expect a Mac version to be released at some point in the future. Those who would like to use the GLIMPSE-CE ModelInterface independently from the rest of the GLIMPSE package can download that executable and associated files (GLIMPSE-CE-ModelInterface-standalone-vYear.Month.Day.zip).
* Please do not install GLIMPSE-CE in a folder that includes spaces in its full path.
* It is recommended that you modify your computer's power settings such that it will not go to sleep while GCAM is running.
* Please wait for the GLIMPSE-CE zip file to fully download before unzipping and to unzip fully before installing.
* Some Windows computers automatically disable execution rights for downloaded EXE and BAT files. Double-clicking will bring up a warning window. Click on the "More Info" button, which will reveal a "Run Anyway" button. This will change the permissions and allow you to execute that file. Alternatively, on some computers, you may need to right-click on the file and choose to unblock it. Similarly, on Linux or other platforms, you may need to make shell scripts executable. There are several packages on Linux machines that may need to be installed. For example, the Linux version of GLIMPSE uses Xterm.
* When naming folders, scenarios, and scenario components, please use alpha-numerical characters, as well as "\_" or "-". Spaces or special characters such as ">", "", "%", or "$" may cause problems when the GLIMPSE-CE software parses the text.
* Windows limits file paths to 256 characters. Because GLIMPSE-CE and GCAM involve many nested folders, some users have experienced problems when installing GLIMPSE to a folder that has a long path. We recommend installing in a location such as C:\\Projects\\GLIMPSE or C:\\Users\\USERNAME\\local\_folder to avoid this problem.
* As indicated in the Users' Guide, please do not install GLIMPSE-CE to a location that is continuously backed up, such as OneDrive, as this may lead to model execution and synchronization issues.

## Starting GLIMPSE

* To start GLIMPSE-CE configured for GCAM-USA X.X, double-click on "run\_GLIMPSE\_GCAM-USA-X.X.bat".
* To start GLIMPSE configured for global GCAM X.X, double-click on "run\_GLIMPSE\_GCAM-global-X.X.bat".

### Acknowledgements

Contributors to software development of the EPA GLIMPSE software include the following:
EPA - Dan Loughlin (ret'd), Tai Wu (ret'd), Chris Nolte (ret'd)
ORISE - Farid Alborzi
ARA - Aaron Parks, Yadong Xu

Additionally, the GLIMPSE-CE ModelInterface is derived from the GCAM ModelInterface, for which the lead developer was Pralit Patel of PNNL.

GLIMPSE-CE modifications to date have been performed by Dan Loughlin in his personal capacity.

### Disclaimer

This code is provided on an "as is" basis and the user assumes responsibility for its use.  EPA has relinquished control of the information and no longer has responsibility to protect the integrity , confidentiality, or availability of the information.  Any reference to specific commercial products, processes, or services by service mark, trademark, manufacturer, or otherwise, does not constitute or imply their endorsement, recommendation or favoring by EPA.  The EPA seal and logo shall not be used in any manner to imply endorsement of any commercial product or activity by EPA or the United States Government.





