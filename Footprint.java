// KicadModuleToGEDA - a utility for turning kicad modules to gEDA PCB footprints
// Footprint.java v1.1
// Copyright (C) 2015 Erich S. Heinzle, a1039181@gmail.com

//    see LICENSE-gpl-v2.txt for software license
//    see README.txt
//    
//    This program is free software; you can redistribute it and/or
//    modify it under the terms of the GNU General Public License
//    as published by the Free Software Foundation; either version 2
//    of the License, or (at your option) any later version.
//    
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program; if not, write to the Free Software
//    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
//    
//    KicadModuleToGEDA Copyright (C) 2015 Erich S. Heinzle a1039181@gmail.com



/**
 *
 * This object coordinates the header, text descriptors,
 * drawn lines, drawn arcs, drawn circles
 * and also the pads for a given Kicad module,
 * passed as a string array of the form (String[] args)
 * and is then able to produce a gEDA footprint
 *
 */

import java.util.Scanner;

public class Footprint
{
  String footprintName = "newFootprint";
  String assembledGEDAelement = "";
  String passedString = "";
  FootprintHeader[] moduleTextDescriptors = new FootprintHeader[200];

  FootprintElementArchetype footprintElements[] =  new FootprintElementArchetype[2000];
  int FPFeatureCount = 0;

  Boolean metricSystem = false;   // this will be changed to "true", if needed,
  // with parsing in due course

  int moduleLineCountTotal = 0;	
  int padDefinitionLineCount = 0;

  String padDefinitionLines;

  int lineCount = 0;

  int padCount = 0;
  int drawSegmentCount = 0;
  int drawArcCount = 0;
  int drawCircleCount = 0;
  int textDescriptorCount = 0;

  String licenceText1 = null;

  String licenceText2 = "\n# dist-license: GPL\n# use-license: unlimited\n"; //# unless specified otherwise in source Kicad module\n";

  String clearanceWarningNotice1 = "# Users of the foot print must ensure that the solder mask reliefs and clearances\n# are compatible with the PCB manufacturer's process tolerances\n";

  String clearanceWarningNotice2 = null;

  long xOffset = 0; // in case we wish to translate the footprint silkscreen in x or y plane
  long yOffset = 0; // useful for building complex multiple device footprints
  float parsedValue = 0;	

  String reconstructedKicadModuleAsString = ""; // we'll return this from the toString() method

  public Footprint(String args, Boolean MmMetricUnits, long minimumViaAndDrillSizeNM)
  {

    metricSystem = MmMetricUnits;
    boolean moduleFinished = false;

    Scanner moduleDefinition = new Scanner(args);

    String parseString = "";
    String trimmedString = "";
    String[] tokens;

    //		System.out.println(args);
    
    licenceText1 = "# Footprint converted from Kicad Module ";
    clearanceWarningNotice2 = "# Pins and SMD pads have been converted from Kicad foot prints which\n# do not have solder mask relief or clearances specified.\n# Fairly sane default values have been used for solder mask relief and clearances.\n";

    while (moduleDefinition.hasNext() && !moduleFinished)
      {			
        parseString = moduleDefinition.nextLine();
        trimmedString = parseString.trim();
        tokens = trimmedString.split(" ");

        // we now move into the main parsing section
        // which decides what each line is all about and then deploys
        // it to construct the relevant footprint element object 

        if (tokens[0].startsWith("$INDEX"))
          {
            //	System.out.println(footprintName); // we don't care about the index
          }
        else if (tokens[0].startsWith("$MODULE") || tokens[0].startsWith("module"))
          {       // it all starts here, with the module header
            // first we look for double quotes inserted by the likes of madparts
            footprintName = tokens[1];
            if (footprintName.length() > 2) {
              if (footprintName.charAt(0) == '"') {
                footprintName = footprintName.substring(1);
              }
              if (footprintName.charAt(footprintName.length()-1) == '"') {
                footprintName = footprintName.substring(0,footprintName.length()-1);
              }
            }
            // now we get rid of characters ![a-zA-Z0-9.-] which
            // may be unacceptable for a filename
            footprintName = footprintName.replaceAll("[^a-zA-Z0-9.-]", "_");
            // System.out.println(footprintName + " is the footprint name found.");

            // we now step through the module line by line
            while (moduleDefinition.hasNext() && !moduleFinished)
              {
                parseString = moduleDefinition.nextLine();
                trimmedString = parseString.trim();
                //	System.out.println("Current footprint def line:" + 
                //			trimmedString);

                // we tokenize the line
                tokens = trimmedString.split(" ");
					

                // and we then decide what to do with the tokenized lines
                // we ignore "attr" lines at this stage
                if (tokens[0].startsWith("Po") || tokens[0].startsWith("at "))
                  {
                    // we find the xOffset in the position, and remember
                    // it is passed in Nm units
                    parsedValue = Float.parseFloat(tokens[1]);
                    xOffset = convertToNanometres(parsedValue, metricSystem);
                    parsedValue = Float.parseFloat(tokens[2]);
                    yOffset = convertToNanometres(parsedValue, metricSystem);
                  }
                else if (tokens[0].startsWith("T0"))
                  {
                    moduleTextDescriptors[textDescriptorCount] = new FootprintHeader();
                    moduleTextDescriptors[textDescriptorCount].populateHeader(trimmedString, metricSystem);
                    textDescriptorCount++;
                  }
                else if (tokens[0].startsWith("T") && !tokens[0].startsWith("T1"))
                  {
                    // we exclude the component value T1, and look for other text to render
                    footprintElements[FPFeatureCount] = new FootprintText();
                    footprintElements[FPFeatureCount].populateElement(trimmedString, metricSystem);
                    FPFeatureCount++;
                  }
                else if (tokens[0].startsWith("fp_text") && tokens[1].startsWith("reference"))
                  {
                    moduleTextDescriptors[textDescriptorCount] = new FootprintHeader();
                    moduleTextDescriptors[textDescriptorCount].populateHeader(trimmedString, metricSystem);
                    textDescriptorCount++;
                    // we only want to know what the reference text is
                  }

                else if (tokens[0].startsWith("DS"))
                  {
                    footprintElements[FPFeatureCount] = new DrawnElement();
                    footprintElements[FPFeatureCount].populateElement(trimmedString, metricSystem);
                    FPFeatureCount++;
                    drawSegmentCount++;
                  }
                else if (tokens[0].startsWith("fp_line"))
                  {
                    footprintElements[FPFeatureCount] = new DrawnElement();
                    footprintElements[FPFeatureCount].populateElement(trimmedString, metricSystem);
                    FPFeatureCount++;
                    drawSegmentCount++;
                  }
                else if (tokens[0].startsWith("DC"))
                  {
                    footprintElements[FPFeatureCount] = new Circle();
                    footprintElements[FPFeatureCount].populateElement(trimmedString, metricSystem);
                    FPFeatureCount++;
                    drawCircleCount++;

                  }
                else if (tokens[0].startsWith("fp_circle"))
                  {
                    footprintElements[FPFeatureCount] = new Circle();
                    footprintElements[FPFeatureCount].populateElement(trimmedString, metricSystem);
                    FPFeatureCount++;
                    drawCircleCount++;
                  }
                else if (tokens[0].startsWith("DA"))
                  {
                    footprintElements[FPFeatureCount] = new Arc();
                    footprintElements[FPFeatureCount].populateElement(trimmedString, metricSystem);
                    FPFeatureCount++;
                    drawArcCount++;
                  }
                else if (tokens[0].startsWith("fp_arc"))
                  {
                    footprintElements[FPFeatureCount] = new Arc();
                    footprintElements[FPFeatureCount].populateElement(trimmedString, metricSystem);
                    FPFeatureCount++;
                    drawArcCount++;
                  }
                else if (tokens[0].startsWith("pad"))
                  {  // we have identified a pad definition in the module
                    padDefinitionLines = trimmedString;
                    padCount++;

                    footprintElements[FPFeatureCount] = new Pad();
                    footprintElements[FPFeatureCount].populateElement(padDefinitionLines, metricSystem, minimumViaAndDrillSizeNM);
                    FPFeatureCount++;	
                  }
                else if (tokens[0].startsWith("$PAD"))
                  {  // we have identified a $PAD definition in the module
                    padDefinitionLines = "";
                    padDefinitionLineCount = 0;
                    padCount++;
                    while (!trimmedString.startsWith("$EndPAD") && moduleDefinition.hasNext())
                      { 	// we now turn the multiline $PAD definition 
                        // into a single string
                        padDefinitionLines = padDefinitionLines +
                            trimmedString + " \n" ;
                        parseString = moduleDefinition.nextLine();
                        trimmedString = parseString.trim();
                        padDefinitionLineCount++;
                      }
                    padDefinitionLines = padDefinitionLines + "$EndPad \n";
                    // having created a single string containing the
                    // $PAD definition
                    // we can now pass it to a Pad constructor
                    footprintElements[FPFeatureCount] = new Pad();
                    footprintElements[FPFeatureCount].populateElement(padDefinitionLines, metricSystem, minimumViaAndDrillSizeNM);
                    FPFeatureCount++;
                  }

                if (tokens[0].startsWith("$EndMOD"))
                  {
                    moduleFinished = true;
                  }

              }

          }
        else {
          // not much to see, not $INDEX or $MODULE
        }
        
      }
    
    // we also create a single string version of the module for
    // use by the toString() method
    reconstructedKicadModuleAsString = args;
    moduleDefinition.close();
  }

  public String generateGEDAfootprintFilename()
  {
    return footprintName + ".fp";
  }

  public String generateGEDAfootprint(float magnificationRatio)
  {
    assembledGEDAelement = licenceText1 + footprintName + licenceText2;
    if (padCount > 0)
      {
        assembledGEDAelement = assembledGEDAelement + 
            clearanceWarningNotice1 + clearanceWarningNotice2 ;
      }

    if (metricSystem)
      {
        assembledGEDAelement = assembledGEDAelement +
            "# Kicad module units: mm\n";
      }
    else
      {
        assembledGEDAelement = assembledGEDAelement +
            "# Kicad module units: 0.1 mil\n";
      }

    // we start by generating a generic gEDA footprint Element[...] field
    // in case the Kicad module failed to have a text field "T0 .... "
    // to base the gEDA element field on
    // We use the file name for the description, default text orientation
    // of 0, with an offset 250 mils to the right of centre, and 
    // text scaling of 100

    String gEDAfootprintElementField =
        "# Since the Kicad Module did not have a 'T0' field,\n" +
        "# The module name has been used for the Element description field\n" +
        "Element[\"\" \"" + footprintName +
        "\" \"\" \"\" 0 0 0 25000 0 100 \"\"]\n(\n";

    if (textDescriptorCount > 0)
      {
        gEDAfootprintElementField = moduleTextDescriptors[0].generateGEDAtextField(0,0);
      }

    assembledGEDAelement = assembledGEDAelement +
        ("# Footprint = module name: " + footprintName + "\n" +
         "# Text descriptor count: " + textDescriptorCount + "\n" +
         "# Draw segment object count: " + drawSegmentCount + "\n" +
         "# Draw circle object count: " + drawCircleCount + "\n" +
         "# Draw arc object count: " + drawArcCount + "\n" +
         "# Pad count: " + padCount + "\n#\n" +
         gEDAfootprintElementField );

    //		System.out.println(assembledGEDAelement);

    for (int counter = 0; counter < FPFeatureCount; counter++)
      {
        assembledGEDAelement = assembledGEDAelement +
            footprintElements[counter].generateGEDAelement(xOffset,yOffset,magnificationRatio);
      }
    return assembledGEDAelement;
  }


  public String getKicadModuleName()
  {
    return footprintName;
  }

  public String toString()
  {
    return reconstructedKicadModuleAsString;
  }

  private long convertToNanometres(float rawValue, boolean metricSystem)
  {
    if (metricSystem) // metricSystem = units mm
      {
        return (long)(1000000 * rawValue);
        // multiply mm by 1000000 to turn into nm
      }
    else
      {
        return (long)(2540 * rawValue);
        // multiply 0.1 mil units by 2540 to turn into nm
      }
  }



}
