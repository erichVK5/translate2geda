// translate2geda.java - a utility for converting various EDA file
// formats to geda PCB footprints and gschem symbols
//
// translate2geda.java v1.0
// Copyright (C) 2016 Erich S. Heinzle, a1039181@gmail.com

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
//    translate2geda Copyright (C) 2016 Erich S. Heinzle a1039181@gmail.com


import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class translate2geda {

  static boolean verbose = false;
  static String outputDir = "";

  public static void main (String [] args) {

    boolean textOutputOnly = false;
    boolean quietMode = false;
    String filename = "";
    String [] convertedFiles = null;
    int argc = 0;

    if (args.length == 0) {
      printHelp();
      System.exit(0);
    } else {
      filename = args[0];
      for (String arg : args) {
	argc++;
        if (arg.equals("-t")) {
          textOutputOnly = true;
        } else if (arg.equals("-q")){
          quietMode = true;
        } else if (arg.equals("-v")){
          verbose = true;
        } else if (arg.equals("-o")){
          outputDir = args[argc];
        }
      }
    }

    if (!quietMode) {
      System.out.println("Using filename: " + filename);
    }

    // we'll now try and decide what to do with the supplied file
    // based on the file ending

    if (filename.endsWith(".bsd") ||
        filename.endsWith(".BSD")) {
      try {
        convertedFiles = parseBSDL(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if ((filename.endsWith(".bxl") ||
                filename.endsWith(".BXL")) && 
               textOutputOnly)  {
      textOnlyBXL(filename);
      System.exit(0);
    } else if (filename.endsWith(".bxl") ||
               filename.endsWith(".BXL"))  {
      try {
        convertedFiles = parseBXL(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".ibs") ||
               filename.endsWith(".IBS") ) {
      try {
        convertedFiles = parseIBIS(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".symdef") ||
               filename.endsWith(".SYMDEF") ) {
      try {
        convertedFiles = parseSymdef(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".lbr") ||
               filename.endsWith(".LBR") ) {
      try {
        convertedFiles = parseEagleLBR(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".asc") ||
               filename.endsWith(".ASC") ) {
      try {
        convertedFiles = parseLTSpice(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".sch") ||
               filename.endsWith(".SCH") ) {
      try { // NB: gschem also saves as .sch
        convertedFiles = parseQUCS(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".gbr") ||
               filename.endsWith(".GBR") ||
               filename.endsWith(".gbl") ||
               filename.endsWith(".GBL") ||
               filename.endsWith(".gtl") ||
               filename.endsWith(".GTL") ||
               filename.endsWith(".gto") ||
               filename.endsWith(".GTO") ||
               filename.endsWith(".gbo") ||
               filename.endsWith(".GBO") ||
               filename.endsWith(".gbs") ||
               filename.endsWith(".GBS") ||
               filename.endsWith(".gts") ||
               filename.endsWith(".GTS") ||
               filename.endsWith(".PHO") ||
               filename.endsWith(".pho") ) {
      try { // NB: there's a lot of variety here
        // i.e. .pho, .gm1, .gbo .gbs .gto .gts etc...
        convertedFiles = parseGerber(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".py") ||
               filename.endsWith(".PY") ) {
      try { // might be an eggbot font def file
        convertedFiles = parseHersheyData(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else {
      System.out.println("I didn't recognise a suitable file " +
                         "ending for conversion, i.e..\n" +
                         "\t.bxl, .bsd, .ibs, .symdef, .asc, .sch, " +
                         ".gbr, hersheydata.py");
    }

    if (convertedFiles != null &&
        !quietMode) {
      for (String converted : convertedFiles) {
        System.out.println(converted);
      }
    }

  }

  private static String [] parseHersheyData(String hersheyData) throws IOException {
    File hersheyDefs = new File(hersheyData);
    Scanner hersheyFonts = new Scanner(hersheyDefs);
    List<String> convertedFiles = new ArrayList<String>();
    ArrayList<String> fontDefs = new ArrayList<String>();
    String newElement = "";
    String newGlyph = "";
    String lastline = "";
    String currentLine = "";
    boolean inComment = false;

    while (hersheyFonts.hasNextLine() && (lastline != null)) {
      lastline = hersheyFonts.nextLine();// make nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("'''") && !inComment) {
        inComment = true;
        System.out.println("# Comment found in Hershey font def file");
      } else if (inComment) {
        if (currentLine.startsWith("'''")) {
          inComment = false;
        }
      } else if (currentLine.endsWith("\"]")) {
        if (currentLine.contains(" = ")) {
          fontDefs.add(currentLine);
          System.out.println("# Font def found:  "
                             + currentLine.substring(0,currentLine.indexOf(" = ")));
        }
      }
    }

    System.out.println("# Number of font defs = "
                       + fontDefs.size());

    System.out.println("# USE RESTRICTION:");
    System.out.println("#	This distribution of the Hershey Fonts may be used by anyone for");
    System.out.println("#	any purpose, commercial or otherwise, providing that:");
    System.out.println("#		1. The following acknowledgements must be distributed with");
    System.out.println("#			the font data:");
    System.out.println("#			- The Hershey Fonts were originally created by Dr.");
    System.out.println("#				A. V. Hershey while working at the U. S.");
    System.out.println("#				National Bureau of Standards.");
    System.out.println("#			- The format of the Font data in this distribution");
    System.out.println("#				was originally created by");
    System.out.println("#					James Hurt");
    System.out.println("#					Cognition, Inc.");
    System.out.println("#					900 Technology Park Drive");
    System.out.println("#					Billerica, MA 01821");
    System.out.println("#					(mit-eddie!ci-dandelion!hurt)");
    System.out.println("#		2. The font data in this distribution may be converted into");
    System.out.println("#			any other format *EXCEPT* the format distributed by");
    System.out.println("#			the U.S. NTIS (which organization holds the rights");
    System.out.println("#			to the distribution and use of the font data in that");
    System.out.println("#			particular format). Not that anybody would really");
    System.out.println("#			*want* to use their format... each point is described");
    System.out.println("#			in eight bytes as \"xxx yyy:\", where xxx and yyy are");
    System.out.println("#			the coordinate values as ASCII numbers.");

    float currentMinX = 0.0f;
    float currentMaxX = 0.0f;
    float currentMinY = 0.0f;
    float currentMaxY = 0.0f;
    float temp = 0.0f;
    for (String font : fontDefs) {
      String fontName = font.substring(0,font.indexOf(" = "));
      font = font.substring(font.indexOf(" = ") + 5, font.length() - 2);
      System.out.println("######################################################");
      System.out.println("# Font converted from EggBot font file ");
      if (fontName.startsWith("EMS")) {
	System.out.println("# SIL Open Font License http://scripts.sil.org/OFL applies.");
      } else {
        System.out.println("# NIST Hershey Font licence applies");
      }
      String [] tokens = font.split("\",\"");
      System.out.println("# Glyphs in "
                         + fontName +
                         " = " + tokens.length);
      // preliminary sizing iteration for font glyphs to establish scaling +/- offsets 
      float scaling = 150.0f; //a default value, shouldn't get used
      for (int i = 1; i < tokens.length; i++) { // now inside a font, skip initial size coords
        String coords [] = tokens[i].split(" ");
        for (int j = 0; j < coords.length; j++) { // now inside a glyph
          if (!coords[j].equals("L") && !coords[j].equals("M")) {
            temp = Float.parseFloat(coords[j]);
            if (currentMinX > temp) {
              currentMinX = temp;
            }
            if (currentMaxX < temp) {
              currentMaxX = temp;
            }
            temp = Float.parseFloat(coords[j+1]);
            if (currentMinY > temp) {
              currentMinY = temp;
            }
            if (currentMaxY < temp) {
              currentMaxY = temp;
            }
            j++;
          }
	}
      }
      scaling = 6000/(currentMaxY-currentMinY); // 600 decimils is about right for gEDA PCB
      System.out.println("#\tMinimum X coord: " + currentMinX
                        + "\n#\tMaximum X coord: " + currentMaxX
                        + "\n#\tMinimum Y coord: " + currentMinY
                        + "\n#\tMaximum Y coord: " + currentMaxY
                        + "\n#\tMaximal Y extent: " + (currentMaxY - currentMinY)
			+ "\n#\tMaximal X extent: " + (currentMaxX - currentMinX)
                        + "\n#\tY scaling to achieve 6000 centimils: " + scaling
                        + "\n######################################################");

      // with a value for scaling, we can now proceed in earnest

      for (int i = 0; i < tokens.length; i++) { // now inside a font, skip initial size coords for now
        String coords [] = tokens[i].split(" ");
	Float [] previous = new Float [2]; // for start coords for the line segments
        Float [] current = new Float [2];  // for end coords for the line segments
        current[0] = Float.parseFloat(coords[0]); // the x size of the glyph
	if (current[0] < 0)  {
		current[0] = -current[0]; // may need +/- something here i.e. half glyph width
	}
        current[1] = Float.parseFloat(coords[1]); // the y size of the glyph
        
        System.out.println("Symbol['" + (char)(i+32) + "' " // start with space char ' '
			+ (long)(current[0]*scaling)        // width based on x dimension +/- ?
			+ "]\n(");

        for (int j = 2; j < coords.length; j++) { // now inside a glyph, skip initial size coords
	  //System.out.println("Symbol('" + (char)(i+32) + "' 18)\n(");
          if (coords[j].equals("L") || coords[j].equals("M")) {
            current[0] = Float.parseFloat(coords[j+1]);
	    current[1] = Float.parseFloat(coords[j+2]);
          }
	  if (coords[j].equals("L")) {
            System.out.println("\tSymbolLine[" +
            	(long)(previous[0]*scaling) + " " +
            	(long)(previous[1]*scaling) + " " +
            	(long)(current[0]*scaling) + " " +
            	(long)(current[1]*scaling) + " 700]");
          }
	  if (coords[j].equals("L") || coords[j].equals("M")) {
            previous[0] = current[0];
            previous[1] = current[1];
            j += 2;
          }
        }
        System.out.println(")");
      }
      //convertedFiles.add(fontName);
    }    

    return convertedFiles.toArray(new String[convertedFiles.size()]); 
  }

  // Eagle libraries provide footprint data, pin mapping, and
  // schematic symbol data

  private static String [] parseEagleLBR(String LBRFile) throws IOException {
    File EagleLBR = new File(LBRFile);
    Scanner eagleLib = new Scanner(EagleLBR);

    String currentLine = "";
    String newElement = "";
    String newSymbol = "";
    String symAttributes = "";
    String elData = "";
    String elName = "";
    EagleLayers layers = new EagleLayers();
    EagleDeviceSet deviceSets = null;

    PinList pins = new PinList(0); // slots = 0

    List<String> convertedFiles = new ArrayList<String>();
    ArrayList<String> layerDefs = new ArrayList<String>();
    ArrayList<String> packageDefs = new ArrayList<String>();
    ArrayList<String> symbolDefs = new ArrayList<String>();
    ArrayList<String> deviceSetDefs = new ArrayList<String>();

    //List<String> currentPackage = new ArrayList<String>(); //not used

    long xOffset = 0;
    long yOffset = 0; // used to justify symbol
    long textXOffset = 0; // used for attribute fields

    String lastline = "";

    while (eagleLib.hasNextLine() && (lastline != null)) {
      lastline = eagleLib.nextLine();// make nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("<layers>")) {
        while (eagleLib.hasNextLine() &&
               !currentLine.startsWith("</layers>")) {
          currentLine = eagleLib.nextLine().trim();
          layerDefs.add(currentLine);
        }
      } else if (currentLine.startsWith("<packages>")) {
        while (eagleLib.hasNextLine() &&
              !currentLine.startsWith("</packages>")) {
          currentLine = eagleLib.nextLine().trim();
          packageDefs.add(currentLine);
        }
      } else if (currentLine.startsWith("<symbols>")) {
        while (eagleLib.hasNextLine() &&
              !currentLine.startsWith("</symbols>")) {
          currentLine = eagleLib.nextLine().trim();
          symbolDefs.add(currentLine);
        }
      } else if (currentLine.startsWith("<devicesets>")) {
        currentLine = eagleLib.nextLine().trim();
        while (eagleLib.hasNextLine() &&
              !currentLine.startsWith("</devicesets>")) {
          if (currentLine.startsWith("<deviceset ") &&
              eagleLib.hasNextLine()) {
            String currentGates = "";
            currentLine = eagleLib.nextLine().trim();
            while (eagleLib.hasNextLine() &&
                   !currentLine.startsWith("</deviceset>")) {
              if (currentLine.startsWith("<gates>")) {
                currentGates = currentLine + "\n";
                currentLine = eagleLib.nextLine().trim();
                while (eagleLib.hasNextLine() &&
                       !currentLine.startsWith("</gates>")) {
                  currentGates = currentGates + currentLine + "\n";
                  // System.out.println("Found some gates");
                  currentLine = eagleLib.nextLine().trim();
                }
                currentGates = currentGates + currentLine + "\n";
                //System.out.println("Found a set of gates");
              } else if (currentLine.startsWith("<device ")) {
                String currentDef = currentLine + "\n";
                // System.out.println("Found a device set line");
                currentLine = eagleLib.nextLine().trim();
                while (eagleLib.hasNextLine() &&
                       !currentLine.startsWith("</device>")) {
                  currentDef = currentDef + currentLine + "\n";
                  // System.out.println("Found a device set line");
                  currentLine = eagleLib.nextLine().trim();
                }
                currentDef = currentGates +
                    currentDef + currentLine + "\n";
                deviceSetDefs.add(currentDef);
                //System.out.println("Found a device set:");
                //System.out.println(currentDef);
              }
              lastline = eagleLib.nextLine();//make nextLine()nullsafe 
              currentLine = safelyTrim(lastline);// when using gcj libs
            }
          }
          lastline = eagleLib.nextLine();// make nextLine() null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
        } //resume while loop
      }
    }

    // first, we create the layer definition object
    // which is used for extraction of other elements
    if (layerDefs.size() == 0) {
      System.out.println("This eagle library appears to be missing "
                         + "layer definitions needed for conversion");
    }
    layers = new EagleLayers(layerDefs);

    // we now turn our ArrayList into a string to pass to a scanner
    // object
    String packageDefString = "";
    for (String packageLine : packageDefs) {
      packageDefString = packageDefString + packageLine + "\n";
    }    

    // next, we parse and process the package (=FP )defs
    if (verbose) {
      System.out.println("Moving onto FPs");
    }

    Scanner packagesBundle = new Scanner(packageDefString);

    lastline = "";
    while (packagesBundle.hasNextLine() && (lastline != null)) {
      lastline = packagesBundle.nextLine();//make nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("<package name")) {
        String [] tokens = currentLine.split(" ");
        String FPName
            = tokens[1].replaceAll("[\">=/]","").substring(4);
        while (packagesBundle.hasNextLine() &&
               !currentLine.startsWith("</package>")) {
          currentLine = packagesBundle.nextLine().trim();
          if (currentLine.startsWith("<smd") ||
              currentLine.startsWith("<pad") ||
              currentLine.startsWith("<hole")) {
            Pad newPad = new Pad();
            newPad.populateEagleElement(currentLine);
            newElement = newElement
                + newPad.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("<wire") &&
                     layers.isDrawnTopSilk(currentLine)) {
            if (!currentLine.contains("curve=")) {
              DrawnElement silkLine = new DrawnElement();
              silkLine.populateEagleElement(currentLine);
              newElement = newElement
                  + silkLine.generateGEDAelement(xOffset,yOffset,1.0f);
            } else {
              Arc silkArc = new Arc();
              silkArc.populateEagleElement(currentLine);
              //System.out.println("Arc element found");
              newElement = newElement
                  + silkArc.generateGEDAelement(xOffset,yOffset,1.0f);
              
            }
          } else if (currentLine.startsWith("<rectangle") &&
                     layers.isDrawnTopSilk(currentLine)) {
            DrawnElement [] silkLines
                = DrawnElement.eagleRectangleAsLines(currentLine);
            for (DrawnElement side : silkLines) {
              newElement = newElement
                  + side.generateGEDAelement(xOffset,yOffset,1.0f);
            }
          } else if (currentLine.startsWith("<circle") &&
                     layers.isDrawnTopSilk(currentLine)) {
            Circle silkCircle = new Circle();
            silkCircle.populateEagleElement(currentLine);
            //System.out.println("Arc element found");
            newElement = newElement
                + silkCircle.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("<polygon") &&
                     (layers.isTopCopper(currentLine) ||
                      layers.isBottomCopper(currentLine) ||
                      layers.isDrawnTopSilk(currentLine))) {
            System.out.println("Polygon omitted in: " + FPName + "\n\t"
                               +  currentLine);
          }

          
        } // end if for "<package name"
        
        // we now build the geda PCB footprint
        elData = "Element[\"\" \""
            + FPName
            + "\" \"\" \"\" 0 0 0 25000 0 100 \"\"]\n(\n"
            + newElement
            + ")";
        elName = FPName + ".fp";
        
        // we now write the element to a file
        elementWrite(outputDir + elName, elData);
        // add the FP to our list of converted elements
        convertedFiles.add(elName); 
        newElement = ""; // reset the variable for batch conversion
      } // end of this particular package while loop
    } // end of packagesBundle while loop


    // we now create the set of eagle devices from which pin mappings
    // can be retrieved
    if (deviceSetDefs.size() != 0) {
      if (verbose) {
        System.out.println("About to create EagleDeviceSet object"); 
      }
      deviceSets = new EagleDeviceSet(deviceSetDefs);
      if (verbose) {
        System.out.println("Created EagleDeviceSet object");
      }
    } // we leave it as null if none found during parsing

    if (verbose) {
      System.out.println("About to process SymbolDefs"); 
    }
    // we now turn our symbol ArrayList into a string to pass
    // to a scanner object
    // StringBuilder might perform better here...
    String symbolDefString = "";
    for (String symbolLine : symbolDefs) {
      symbolDefString = symbolDefString + symbolLine + "\n";
    }

    Scanner symbolBundle = new Scanner(symbolDefString);

    // next, we parse and process the package (=FP )defs
    if (verbose) {
      System.out.println("About to create individual symbols"); 
    }

    lastline = "";
    while (symbolBundle.hasNextLine()) {
      lastline = symbolBundle.nextLine();// make nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("<symbol ")) {
        String [] tokens = currentLine.split(" ");
        String symbolName // name="......"
            = tokens[1].substring(6).replaceAll("[\"\\/>]","");
        if (verbose) {
          System.out.println("Found symbol name:" + symbolName);
        }
        List<String> silkFeatures = new ArrayList<String>();
        List<String> attributeFields = new ArrayList<String>();
        pins = new PinList(0); // slots = 0
        while (symbolBundle.hasNextLine() &&
               !currentLine.startsWith("</symbol")) {
          currentLine = symbolBundle.nextLine().trim();
          if (currentLine.startsWith("<pin")) {
            //System.out.println("#Making new pin: " + currentLine);
            SymbolPin latestPin = new SymbolPin();
            latestPin.populateEagleElement(currentLine);
            pins.addPin(latestPin);
          } else if (currentLine.startsWith("Line") ||
                     currentLine.startsWith("Arc (Layer TOP_SILK")) {
            //silkFeatures.add(currentLine);
          } else if (currentLine.startsWith("Attribute")) {
            //attributeFields.add(currentLine);
          }
        }

        // now we have a list of pins, we can calculate the offsets
        // to justify the element in gschem, and justify the attribute
        // fields.

        // we may need to turn this off if converting entire schematics
        // at some point in the future
        if (!pins.empty()) {
          xOffset = pins.minX();
          yOffset = pins.minY()-200;  // includes bounding box
          // spacing of ~ 200 takes care of the bounding box
          textXOffset = pins.textRHS(); //??? broken for some reason
        }
        // additional bounding box extents are calculated by minY()

        for (String feature : silkFeatures) {
          if (feature.startsWith("Arc (Layer TOP_SILKSCREEN)")) {
            Arc silkArc = new Arc();
            silkArc.populateBXLElement(feature);
            newElement = newElement
                + silkArc.generateGEDAelement(0,-yOffset,1.0f);
          } else if (feature.startsWith("Line")) {
            SymbolPolyline symbolLine = new SymbolPolyline();
            symbolLine.populateBXLElement(feature);
            newElement = newElement
                + "\n" + symbolLine.toString(0,-yOffset);
          } 
        }

        String newSymbolHeader = "v 20110115 1"
            + newElement; // we have created the header for the symbol
        newElement = "";
        String FPField = "";

        // first, we see if there are devicedefs for this symbol
        // mapping its pins onto footprint pads
        //System.out.println("Requesting device defs for " +
        //                   symbolName);
        ArrayList<EagleDevice> symbolDeviceDefs 
            = deviceSets.supplyDevicesFor(symbolName);

        // we have two scenarios, the first is that we have symbols
        // +/- pins defined but no pin mapping for
        // them (= symbolDeviceDef)
        // the second is we have 1 or more pin mappings defined for
        // the symbol found
        if (symbolDeviceDefs.size() == 0) {
          // it seems we have no pin mappings applicable to the symbol
          System.out.println("No matching footprint specified for: "
                             + symbolName);
          attributeFields.add("footprint=unknown");

          SymbolText.resetSymbolTextAttributeOffsets();
          // we now generate the text attribute fields for the current
          // symbol
          for (String attr : attributeFields) {
            symAttributes = symAttributes
                + SymbolText.BXLAttributeString(textXOffset, 0, attr);
          }

          elData = "";
          if (!pins.empty()) { // sometimes Eagle has rubbish symbols
            // with no pins, so we test before we build the symbol
            // note that we did not have a pin mapping we could apply
            // so pin numbers will default to zero
            elData = pins.toString(-xOffset,-yOffset)
                //... header, and then
                + "\n"
                + pins.calculatedBoundingBox(0,0).toString(-xOffset,-yOffset);
          }
            
          // add some attribute fields
          newSymbol = newSymbolHeader + elData + symAttributes;

          // customise symbol filename to reflect applicable FP
          elName = symbolName + ".sym";
          
          // we now write the element to a file
          elementWrite(outputDir + elName, newSymbol);
          
          // add the symbol to our list of converted elements
          convertedFiles.add(elName);
          
          silkFeatures.clear();
          attributeFields.clear();
          symAttributes = "";
        
        } else { // we get here if >0 symbolDeviceDefs
          // TODO
          // need to generate n symbols for n pin mappings
          // also need to sort out FPName for each variant
          // also need to sort out sane naming convention for
          // the variants of the symbol
          
          for (int index = 0;
               index < symbolDeviceDefs.size();
               index++) {
            if (deviceSets != null &&
                deviceSets.containsSymbol(symbolName) ) {
              //System.out.println("About to renumber pins for "
              //                   + symbolName); 
              if (!pins.empty()) { // sometimes Eagle has odd symbols
                // for fiducials and so forth
                pins.applyEagleDeviceDef(symbolDeviceDefs.get(index));
                textXOffset = pins.textRHS(); // for text justification
              } 
              FPField = symbolDeviceDefs.get(index).supplyFPName();
              attributeFields.add("footprint=" + FPField);
              FPField = "_" + FPField;
            } // start with the first device def to begin with
            
            // when batch converting, we avoid incrementing the
            // justification of text from one symbol to the next, so 
            // we reset the offset variable for each new symbol thusly
            SymbolText.resetSymbolTextAttributeOffsets();
            // we no generate the text attribute fields for the current
            // symbol
            for (String attr : attributeFields) {
              symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, attr);
            }
            
            elData = "";
            if (!pins.empty()) { // sometimes Eagle has rubbish symbols
              // with no pins, so we test before we build the symbol
              elData = pins.toString(-xOffset,-yOffset)
                  //... header, and then
                  + "\n"
                  + pins.calculatedBoundingBox(0,0).toString(-xOffset,-yOffset);
            }
            
            // add some attribute fields
            newSymbol = newSymbolHeader + elData + symAttributes;
            // customise symbol filename to reflect applicable FP
            elName = symbolName + FPField + ".sym";
            
            // we now write the element to a file
            elementWrite(outputDir + elName, newSymbol);
            
            // add the symbol to our list of converted elements
            convertedFiles.add(elName);
          
            attributeFields.clear();
            symAttributes = "";
          } // end of for loop for pin mappings
          silkFeatures.clear();
        } // end of else statement for >=1 pin mappings

      }
    }
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  } 


  // BSDL files provide pin mapping suitable for symbol generation
  // but do not provide package/footprint information
  private static String [] parseBSDL(String BSDLFile) throws IOException {
    File inputBSDL = new File(BSDLFile);
    Scanner textBSDL = new Scanner(inputBSDL);

    String currentLine = "";
    List<String> portPinDef = new ArrayList<String>();
    //  String newElement = ""; //unused
    String newSymbol = "";
    String symAttributes = "";
    String FPName = "DefaultFPName";
    String elName = null;
    String elData = "";
    PinList pins = new PinList(0); // slots = 0 for BDSL data

    List<String> convertedFiles = new ArrayList<String>();

    long xOffset = 0;
    long yOffset = 0;
    String lastline = "";

    while (textBSDL.hasNextLine() && (lastline != null)) {
      lastline = textBSDL.nextLine();// make nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("entity")) {
        String [] tokens = currentLine.split(" ");
        String symName = tokens[1].replaceAll("[\"]","");
        while (textBSDL.hasNextLine()
               && !currentLine.startsWith("end")) {
          lastline = textBSDL.nextLine();// make nextLine() null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
          if (currentLine.startsWith("constant")) {
            currentLine = currentLine.replaceAll("[:=]"," ");
            tokens = currentLine.split(" ");
            FPName = tokens[1].replaceAll("[\"]","_");
            pins = new PinList(0); // slots = 0
            boolean lastLine = false;
            while (textBSDL.hasNextLine()
                   && !lastLine) {
              lastline = textBSDL.nextLine();//make nextLine()nullsafe 
              currentLine = safelyTrim(lastline);// when using gcj libs
              if ((currentLine.length() != 0) ) {
                //                  && !currentLine.equals(" ") ) {
                SymbolPin latestPin = new SymbolPin();
                latestPin.populateBSDLElement(currentLine);
                pins.addPin(latestPin);
                if (currentLine.endsWith(";")) {
                  lastLine = true;
                }
              }
            }
          } else if (currentLine.startsWith("port (")) {
            boolean endOfPinDef = false;
            while (textBSDL.hasNextLine() &&
                   !endOfPinDef) {
              lastline = textBSDL.nextLine();//make nextLine()nullsafe 
              currentLine = safelyTrim(lastline);// when using gcj libs
              if (currentLine.startsWith(")")) {
                endOfPinDef = true;
              } else {
                portPinDef.add(currentLine);
              }
            }
          }
        }
        
        pins.setBSDPinType(portPinDef.toArray(new String[portPinDef.size()]));

        PinList newPinList = pins.createDILSymbol();
        // with a pin list, we can now calculate text label positions
        long textRHSOffset = newPinList.textRHS();
        yOffset = newPinList.minY();// to justify the symbol in gschem 
        // header
        newSymbol = "v 20110115 1";
        // next some attributes
        symAttributes = symAttributes
            + SymbolText.BXLAttributeString(textRHSOffset, 0, "footprint=" + FPName)
            + SymbolText.BXLAttributeString(textRHSOffset, 0, "refdes=U?")
            + SymbolText.BXLAttributeString(textRHSOffset, 0, "documentation=" + BSDLFile);

        // we now build the symbol
        elData = newSymbol   // we now add pins to the...
            + newPinList.toString(xOffset,yOffset)
            //... header, and then
            + "\n"
            + newPinList.calculatedBoundingBox(0,0).toString(0,yOffset)
            + symAttributes;
        elName = symName + ".sym";

        // we now write the element to a file
        elementWrite(outputDir + elName, elData);
        convertedFiles.add(elName);

        symAttributes = ""; // reset symbol data if batch processing
        // TODO - might be nice to reset BSDL coords in SymbolPinClass
        // if batch converting; probably not essential for usual use
      }
    }
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  }

  // .symdef files provide pin mapping suitable for symbol generation
  // but do not provide package/footprint information
  private static String [] parseSymdef(String symDefFilename) throws IOException {

    File symDefFile = new File(symDefFilename);
    Scanner symDef = new Scanner(symDefFile);

    String currentLine = "";
    //String newElement = ""; // not used
    String newSymbol = "";
    String symAttributes = "";
    String elData = "";
    String elName = "";

    long xOffset = 0;
    long yOffset = 0; // used to justify symbol
    long textXOffset = 0; // used for attribute fields

    List<String> convertedFiles = new ArrayList<String>();
    List<String> textLabels = new ArrayList<String>();
    List<String> left = new ArrayList<String>();
    List<String> right = new ArrayList<String>();
    List<String> top = new ArrayList<String>();
    List<String> bottom = new ArrayList<String>();

    String currentState = "labels";
    String lastline = "";

    while (symDef.hasNext() && (lastline != null)) {
      lastline = symDef.nextLine();// make nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("[labels]") ||
          currentLine.startsWith("[LABELS]")) {
        currentState = "labels";
      } else if (currentLine.startsWith("[left]") ||
                 currentLine.startsWith("[LEFT]")) {
        currentState = "left";
      } else if (currentLine.startsWith("[right]") ||
                 currentLine.startsWith("[RIGHT]")) {
        currentState = "right";
      } else if (currentLine.startsWith("[top]") ||
                 currentLine.startsWith("[TOP]")) {
        currentState = "top";
      } else if (currentLine.startsWith("[bottom]") ||
                 currentLine.startsWith("[BOTTOM]")) {
        currentState = "bottom";
      } else if (currentLine.startsWith(".bus") ||
                 currentLine.startsWith(".BUS")) {
        // don't do anything
      } else if ((currentLine.length() > 1) &&
                 (!currentLine.startsWith("#"))) {
        if (currentState.equals("labels")) {
          if (currentLine.length() > 0) {
            textLabels.add(currentLine);
          }
        } else if (currentState.equals("left")) {
          left.add(currentLine);
        } else if (currentState.equals("bottom")) {
          bottom.add(currentLine);
        } else if (currentState.equals("right")) {
          right.add(currentLine);
        } else if (currentState.equals("top")) {
          top.add(currentLine);
        } 
      }
    }
    PinList pins = new PinList(0); // slots = 0
    for (String line : left) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "R");
      pins.addPin(newPin);
    }
    for (String line : bottom) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "U");
      pins.addPin(newPin);
    }
    for (String line : top) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "D");
      pins.addPin(newPin);
    }
    for (String line : right) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "L");
      pins.addPin(newPin);
    }

    // our pinsGridAligned method will make the pins nicely spaced
    // around the symbol. 
    PinList newPinList = pins.pinsGridAligned(200);

    // now we have a list of pins, we can calculate the offsets
    // to justify the element in gschem, and justify the attribute
    // fields.
    yOffset = newPinList.minY()-200;  // includes bounding box
    // spacing of ~ 200 takes care of the bounding box

    textXOffset = newPinList.textRHS();
    // additional bounding box extents are calculated by minY()

    for (String attr : textLabels) {
      symAttributes = symAttributes
          + SymbolText.symDefAttributeString(textXOffset, 0, attr);
    }

    newSymbol = "v 20110115 1"; // don;t need newElement
        //        + newElement; // we have created the header for the symbol
    //newElement = ""; //not used
    
    xOffset = 0;

    // we can now put the pieces of the BXL defined symbol together
    elName = "symDefSymbol.sym";
    elData = newSymbol   // we now add pins to the
        + newPinList.toString(xOffset,-yOffset) // the header, and then
        + "\n" + newPinList.boundingBox(0,0).toString(xOffset,-yOffset)
        + symAttributes; // the final attributes

    // we now write the element to a file
    elementWrite(outputDir + elName, elData);
    // add the symbol to our list of converted elements
    convertedFiles.add(elName);
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  } 


  // schematics:
  // 1) grid spacings of 20, and as little as 10
  // 2) most components 60 units high
  // 3) coord provided is centre of component
  // 4) rotation CCW +ve, 0 = 0, 1 = 90, 2 = 180, 3= 270
  // 5) +ve Y is down
  // qucs files contain components, and nets, which can be turned
  // into a gschem schematic file
  private static String [] parseQUCS(String QUCSsch) throws IOException {
    File input = new File(QUCSsch);
    Scanner inputQUCS = new Scanner(input);
    String currentLine = "";
    //    String newElement = ""; // unused
    //String newSymbol = ""; // unused
    String newSchematic = "";
    String symAttributes = "";
    // now we trim the .sch file ending off:
    String schematicName = QUCSsch.substring(0,QUCSsch.length()-4);
    long xOffset = 40000; // to centre things a bit in gschem
    long yOffset = 40000; // to centre things a bit in gschem
    //int lineCount = 0; //unused
    String lastline = "";
    long lastX = 0;
    long lastY = 0;

    // we start build a gschem schematic
    newSchematic = "v 20110115 1";

    while (inputQUCS.hasNext()
           && (lastline != null) ) {
      lastline = inputQUCS.nextLine(); // making nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("<Wires>")) {
        while (inputQUCS.hasNext()
               && !currentLine.startsWith("</Wires>")) {
          lastline = inputQUCS.nextLine(); // try to keep null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
          if (!currentLine.startsWith("</Wires>")) {
            SymbolNet wire = new SymbolNet(currentLine);
            newSchematic = newSchematic
                + "\n"
                + wire.toString(xOffset, yOffset);
          }
        }
      } else if (currentLine.startsWith("<Components>")) {
        // could move this code into the Symbol object in due course
        while (inputQUCS.hasNext()
               && !currentLine.startsWith("</Components>")) {
          lastline = inputQUCS.nextLine(); // try to keep null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
          if (!currentLine.startsWith("</Components>")) {
            String[] tokens = currentLine.split(" ");
            String elType = tokens[0];
            String symName = "";
            String valueField = null;
            int index1 = 0;
            int index2 = 0;
            //System.out.println("Element type: " + elType);

            // the following is just the first pass
            // bespoke symbols for QUCS purposes will be needed
            if (elType.equals("<R")) {
              symName = "resistor-QUCS.sym";
              index1 = currentLine.indexOf('"');
              index2 = currentLine.indexOf('"', index1 + 1);
              if (index1 != -1) {
                valueField = currentLine.substring(index1+1, index2);
                valueField = "value=" + valueField.replaceAll(" ", "");
              }
            } else if (elType.equals("<GND")) {
              symName = "ground-QUCS.sym";
            } else if (elType.equals("<C")) {
              symName = "capacitor-QUCS.sym";
              index1 = currentLine.indexOf('"');
              index2 = currentLine.indexOf('"', index1 + 1);
              if (index1 != -1) {
                valueField = currentLine.substring(index1+1, index2);
                valueField = "value=" + valueField.replaceAll(" ", "");
              }
            } else if (elType.equals("<L")) {
              symName = "inductor-QUCS.sym";
              index1 = currentLine.indexOf('"');
              index2 = currentLine.indexOf('"', index1 + 1);
              if (index1 != -1) {
                valueField = currentLine.substring(index1+1, index2);
                valueField = "value=" + valueField.replaceAll(" ", "");
              }
            } else if (elType.equals("<Lib")) {
              if (tokens[1].startsWith("LM3886")) {
                symName = "LM3886-opamp-QUCS.sym";
              } else if (tokens[1].startsWith("AD825")) {
                symName = "AD825-opamp-QUCS.sym";
              } else if (tokens[1].startsWith("OP")) {
                //System.out.println("op amp!... tokens[9]:"
                // + tokens[9]);
                if (tokens[9].equals("\"Ideal\"")) {
                  symName = "ideal-opamp-QUCS.sym";
                } else if (tokens[11].startsWith("\"uA741")) {
                  symName = "medium-opamp-QUCS.sym";
                } else {
                  symName = "opamp-QUCS.sym";
                }
              } else if(tokens[1].startsWith("D_")
                        || ((tokens[1].length() == 2) 
                            && tokens[1].startsWith("D"))) {
                symName = "diode-QUCS.sym";
              } else if(tokens[1].startsWith("LP1")
                        || tokens[1].startsWith("LP2")) {
                symName = "low-pass-filter-QUCS.sym";
              } else if(tokens[1].startsWith("BP2")) {
                symName = "band-pass-filter-QUCS.sym";
              } else if(tokens[1].startsWith("N2F")) {
                symName = "notch-filter-QUCS.sym";
              } else if(tokens[1].startsWith("HP1")
                        || tokens[1].startsWith("HP2")) {
                symName = "high-pass-filter-QUCS.sym";
              } else if(tokens[1].startsWith("AP1F")
                        || tokens[1].startsWith("AP2F")) {
                symName = "all-pass-filter-QUCS.sym";
              } else if(tokens[1].startsWith("LIM")) {
                if (tokens[11].startsWith("Hard")) {
                  symName = "hard-limiter-QUCS.sym";
                } else {
                  symName = "limiter-QUCS.sym";
                }
              } else if(tokens[1].startsWith("SQRT")) {
                symName = "square-root-QUCS.sym";
              } else if(tokens[1].startsWith("QNT")) {
                symName = "quantiser-QUCS.sym";
              } else if(tokens[1].startsWith("DIFF")) {
                symName = "differentiator-QUCS.sym";
              } else if(tokens[1].startsWith("DLY")) {
                symName = "Vdelay-QUCS.sym";
              } else if(tokens[1].startsWith("INT")) {
                symName = "integrator-QUCS.sym";
              } else if(tokens[1].startsWith("ABS")) {
                symName = "Abs-QUCS.sym";
              } else if(tokens[1].startsWith("MUL")) {
                symName = "multiplier-QUCS.sym";
              } else if(tokens[1].startsWith("VADD")) {
                symName = "VSum-QUCS.sym";
              } else if(tokens[1].startsWith("VSUB")) {
                symName = "VSub-QUCS.sym";
              } else {
                symName = "unknown-QUCS-Lib-" + tokens[1] + ".sym";
              }
            } else if (elType.equals("<Diode")) {
              symName = "diode-QUCS.sym";
            } else if (elType.equals("voltage")) {
              symName = "voltage-source-QUCS.sym";
            } else if (elType.equals("current")) {
              symName = "current-source-QUCS.sym";
            } else if (elType.equals("npn")) {
              symName = "npn-LTS.sym";
            } else {
              symName = "unknown-" + elType + "-QUCS.sym";
            }
            long xCoord = 0;
            long yCoord = 0;
            String rotation = "0";
            xCoord = (long)(10*Integer.parseInt(tokens[3]));
            yCoord = (long)(-(10*Integer.parseInt(tokens[4])));
            String elRotation = tokens[8].replaceAll(">","");
            //System.out.println("Rotation: " + elRotation);
            if (elRotation.equals("1")) {
              rotation = "90";
            } else if (elRotation.equals("2")) {
              rotation = "180";
            } else if (elRotation.equals("3")) {
              rotation = "270";
            }
            newSchematic = newSchematic
                + "\n"
                + "C "
                + (xOffset + xCoord) 
                + " "
                + (yOffset + yCoord)
                + " "
                + "1" + " "
                + rotation + " "
                + "0" + " " 
                + symName;
            lastX = xOffset + xCoord;
            lastY = yOffset + yCoord;// for use with refdes attribute
            if (tokens[1].equals("*")) {
              symAttributes = "refdes=GND";
            } else {
              symAttributes = "refdes=" + tokens[1];
            }
            SymbolText.resetSymbolTextAttributeOffsets();
            newSchematic = newSchematic
                + "\n{"
                + SymbolText.QUCSRefDesString(lastX,
                                              lastY,
                                              symAttributes);
            if (valueField != null) {
              newSchematic = newSchematic // it will be a touch lower
                  + SymbolText.QUCSValueString(lastX, // vs. refdes
                                               lastY,
                                               valueField);
              valueField = null;
            }
            newSchematic = newSchematic
                + "\n}";
          }
        }
      }
    }
    // we can now finalise the gschem schematic
    String networkName = schematicName + ".gschem.sch";
    // we now write the converted schematic data to a file
    elementWrite(outputDir + networkName, newSchematic);
    String [] returnedFilename = {networkName};
    return returnedFilename;
  }


  // LTSpice files contain components, and nets, which can be turned
  // into a gschem schematic file
  private static String [] parseLTSpice(String spiceFile) throws IOException {
    File input = new File(spiceFile);
    Scanner inputAsc = new Scanner(input);
    String currentLine = "";
    //    String newElement = ""; // unused
    //String newSymbol = ""; // unused
    String newSchematic = "";
    String symAttributes = "";
    // now we trim the .asc file ending off:
    String schematicName = spiceFile.substring(0,spiceFile.length()-4);
    long xOffset = 40000; // to centre things a bit in gschem
    long yOffset = 40000; // to centre things a bit in gschem
    //int lineCount = 0; //unused
    String lastline = "";
    long lastX = 0;
    long lastY = 0;

    // we start build a gschem schematic
    newSchematic = "v 20110115 1";

    String symbolAttributeSet = null;

    while (inputAsc.hasNext()
           && (lastline != null) ) {
      lastline = inputAsc.nextLine(); // making nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("SYMATTR")) {
        String[] tokens = currentLine.split(" ");
        if ("InstName".equals(tokens[1])) {
          symAttributes = "refdes=" + tokens[2];
          SymbolText.resetSymbolTextAttributeOffsets();
          if (symbolAttributeSet == null) {
            symbolAttributeSet = "\n{"
                + SymbolText.LTSpiceRefdesString(lastX,
                                                 lastY,
                                                 symAttributes);
          } else {
            symbolAttributeSet = symbolAttributeSet
                + SymbolText.LTSpiceRefdesString(lastX,
                                                 lastY,
                                                 symAttributes);
          }
        }
        if ("Value".equals(tokens[1])) {
          symAttributes = "value=" + tokens[2];
          if (symbolAttributeSet == null) {
            symbolAttributeSet = "\n{"
                + SymbolText.LTSpiceRefdesString(lastX,
                                                 lastY,
                                                 symAttributes);
          } else {
            symbolAttributeSet = symbolAttributeSet
                + SymbolText.LTSpiceRefdesString(lastX,
                                                 lastY,
                                                 symAttributes);
          }
        }
      } else if (currentLine.startsWith("WIRE")) {
        SymbolNet wire = new SymbolNet(currentLine);
        newSchematic = newSchematic
            + "\n"
            + wire.toString(xOffset, yOffset);
      } else if (currentLine.startsWith("SYMBOL")
                 || currentLine.startsWith("FLAG")) {
          // ? move this code into the Symbol object in due course
          if (symbolAttributeSet != null ) { // hmm, onto next symbol
            newSchematic = newSchematic
                + symbolAttributeSet
                + "\n}"; // so we finish off the last one's attributes
          }
            symbolAttributeSet = null; // reset this
            currentLine = currentLine.replaceAll("  "," ");
            String[] tokens = currentLine.split(" ");
            String elType = tokens[1];
            String symName = "";
            if (elType.equals("res")) {
              symName = "resistor-LTS.sym";
            } else if (elType.equals("cap")) {
              symName = "capacitor-LTS.sym";
            } else if (elType.equals("ind")) {
              symName = "inductor-LTS.sym";
            } else if (elType.equals("diode")) {
              symName = "diode-LTS.sym";
            } else if (elType.equals("voltage")) {
              symName = "voltage-source-LTS.sym";
            } else if (elType.equals("current")) {
              symName = "current-source-LTS.sym";
            } else if (elType.equals("npn")) {
              symName = "npn-LTS.sym";
            } else if (tokens[0].equals("FLAG")) {
              symName = "ground-LTS.sym";
            } else if (elType.startsWith("Opamps")) {
              symName = "opamp-LTS.sym";
            } else {
              symName = "unknown-" + elType + "-LTS.sym";
            }
            long xCoord = 0;
            long yCoord = 0;
            String rotation = "0";
            if (tokens[0].equals("FLAG")) { // it's a ground symbol
              xCoord = (long)(12.5*Integer.parseInt(tokens[1]));
              yCoord = (long)(-(12.5*Integer.parseInt(tokens[2])));
            } else { // not a ground symbol, an actual component
              xCoord = (long)(12.5*Integer.parseInt(tokens[2]));
              yCoord = (long)(-(12.5*Integer.parseInt(tokens[3])));
              String elRotation = tokens[4];
              if (elRotation.equals("R90")) {
                rotation = "270";
              } else if (elRotation.equals("R180")) {
                rotation = "180";
              } else if (elRotation.equals("R270")) {
                rotation = "90";
              }
            }
            newSchematic = newSchematic
                + "\n"
                + "C "
                + (xOffset + xCoord) 
                + " "
                + (yOffset + yCoord)
                + " "
                + "1" + " "
                + rotation + " "
                + "0" + " " 
                + symName;
            // will need to process "SYMATTR" here in due course
            //        while (inputAsc.hasNext() &&
            //   (!currentLine.startsWith("[")
            //    || (lineCount == 0))) {
            //currentLine = inputAsc.nextLine().trim();
          //          if (!currentLine.startsWith("[")) {
          //            }
            lastX = xOffset + xCoord;
            lastY = yOffset + yCoord;// for use with attributes, if any
      }
    }
    // we can now finalise the gschem schematic
    //symAttributes = symAttributes
    // + SymbolText.BXLAttributeString(newPinList.textRHS(),0, FPAttr);
    String networkName = schematicName + ".sch";
    // we now write the converted schematic data to a file
    elementWrite(outputDir + networkName, newSchematic);
    String [] returnedFilename = {networkName};
    return returnedFilename;
  }


  // IBIS files provide pin mapping suitable for symbol generation
  // but do not provide package/footprint information
  private static String [] parseIBIS(String IBISFile) throws IOException {
    File input = new File(IBISFile);
    Scanner inputIBIS = new Scanner(input);
    String currentLine = "";
    //String newElement = ""; // not used
    String newSymbol = "";
    String symAttributes = "";
    String FPName = "DefaultFPName";
    // now we trim the .ibs file ending off:
    String symName = IBISFile.substring(0,IBISFile.length()-4);
    PinList pins = new PinList(0); // slots = 0 for IBIS data

    long xOffset = 0;
    long yOffset = 0;
    boolean extractedSym = false;
    int lineCount = 0;
    String lastline = ""; 
    while (inputIBIS.hasNext()
           && !extractedSym
           && (lastline != null)) {
      lastline = inputIBIS.nextLine(); // making nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("[Pin]")) {
        while (inputIBIS.hasNext()
               && (lastline != null)
               && (!currentLine.startsWith("[") || (lineCount == 0))) {
          lastline = inputIBIS.nextLine();// make nextLine() null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
          lineCount++;
          if (!currentLine.startsWith("[")) {
            // the pin mapping info ends at the next [] marker
            pins = new PinList(0); // slots = 0
            //boolean lastLine = false; //unused
            while (inputIBIS.hasNext() &&
                   !extractedSym) {
              // we make sure it isn't a comment line, i.e. "|" prefix
              if (!currentLine.startsWith("|")) {
                SymbolPin latestPin = new SymbolPin();
                latestPin.populateIBISElement(currentLine);
                pins.addPin(latestPin);
              }
              lastline = inputIBIS.nextLine();//makenextLine()nullsafe 
              currentLine = safelyTrim(lastline);// when using gcj libs
              if (currentLine.startsWith("[")) {
                extractedSym = true;
              }
            }
          }
        }
      }
    }
    PinList newPinList = pins.createDILSymbol();

    // we can now build the final gschem symbol
    newSymbol = "v 20110115 1";
    String FPAttr = "footprint=" + FPName;
    symAttributes = symAttributes
        + SymbolText.BXLAttributeString(newPinList.textRHS(),0, FPAttr);       
    String elData = newSymbol   // we now add pins to the header...
        + newPinList.toString(xOffset,yOffset)
        // remembering that we built this symbol with coords of
        // our own choosing, i.e. well defined y coords, so don't need
        // to worry about justifying it to display nicely in gschem
        // unlike BXL or similar symbol definitions
        + "\n"
        + newPinList.calculatedBoundingBox(0,0).toString(0,0)
        + symAttributes;
    String elName = symName + ".sym";

    // we now write the element to a file
    elementWrite(outputDir + elName, elData);
    String [] returnedFilename = {elName};
    return returnedFilename;
  }

  // we use a modified version of Phillip Knirsch's gerber
  // parser/plotter code from the turn of the century
  // http://www.wizards.de/phil/java/rs274x.html
  // this routine exports a PCB footprint version of the gerber file
  // It still needs a bit of work, i.e. 
  // - arcs might be a bit broken
  // - a bit verbose with polygon processing
  // - only exports a footprint, not a layout file
  // - would need heuristics for pin/pad vs wire/trace detection
  // - this is tricky though due to naughty EDAs painting some features
  //   instead of flashing
  private static String [] parseGerber(String gerberFile)
    throws IOException {
    File input = new File(gerberFile);
    Scanner gerberData = new Scanner(input);
    String gerbText = "";
    while (gerberData.hasNextLine()) {
      gerbText = gerbText + gerberData.nextLine();
    }
    Plotter gerberPlotter = new Plotter();
    gerberPlotter.setScale(1.0, 1.0);
    gerberPlotter.setSize(800, 640); // might make it behave
    String [] retString = new String [1];
    try {
      gerberPlotter.generatePCBFile(gerbText,gerberFile);
      retString[0] = gerberFile + ".fp";
    }
    catch (Exception e) {
      retString[0] = "Error: Gerber plotter unable to parse file.";
      defaultFileIOError(e);
    }
    return retString;
  }

  private static void textOnlyBXL(String BXLFile) {
    SourceBuffer buffer = new SourceBuffer(BXLFile); 
    System.out.println(buffer.decode());
  }

  // BXL files provide both pin mapping suitable for symbol
  // generation as well as package/footprint information
  private static String [] parseBXL(String BXLFile) throws IOException {

    SourceBuffer buffer = new SourceBuffer(BXLFile); 
    Scanner textBXL = new Scanner(buffer.decode());

    String currentLine = "";
    String newElement = "";
    String newSymbol = "";
    String symAttributes = "";
    String elData = "";
    String elName = "";
    PadStackList padStacks = new PadStackList();
    PinList pins = new PinList(0); // slots = 0
    List<String> convertedFiles = new ArrayList<String>();

    long xOffset = 0;
    long yOffset = 0; // used to justify symbol
    long textXOffset = 0; // used for attribute fields
    String lastline = "";

    while (textBXL.hasNextLine() && (lastline != null)) {
      lastline = textBXL.nextLine(); // making nextLine() null safe 
      currentLine = safelyTrim(lastline); // when using gcj libs
      if (currentLine.startsWith("PadStack")) {
          newElement = currentLine;
          while (textBXL.hasNextLine() && (lastline != null) &&
                 !currentLine.startsWith("EndPadStack")) {
            lastline = textBXL.nextLine();//make nextLine() null safe 
            currentLine = safelyTrim(lastline); // when using gcj libs
            newElement = newElement + "\n" + currentLine;
          }
          padStacks.addPadStack(newElement);
          newElement = ""; // reset the variable
      } else if (currentLine.startsWith("Pattern ")) {
        String [] tokens = currentLine.split(" ");
        String FPName = tokens[1].replaceAll("[\"]","");
	FPName = FPName.replaceAll("[ /]","_"); // seriously, who puts slashes in filenames //
        while (textBXL.hasNextLine() && (lastline != null) &&
               !currentLine.startsWith("EndPattern")) {
          lastline = textBXL.nextLine();// making nextLine() null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
          if (currentLine.startsWith("Pad")) {
            //System.out.println("#Making new pad: " + currentLine);
            Pad newPad = padStacks.GEDAPad(currentLine);
            newElement = newElement
                + newPad.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("Line (Layer TOP_SILK")) {
            DrawnElement silkLine = new DrawnElement();
            silkLine.populateBXLElement(currentLine);
            newElement = newElement
                + silkLine.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("Arc (Layer TOP_SILK")) {
            Arc silkArc = new Arc();
            silkArc.populateBXLElement(currentLine);
            newElement = newElement
                + silkArc.generateGEDAelement(xOffset,yOffset,1.0f);
          }
        }

        // we now build the geda PCB footprint
        elData = "Element[\"\" \""
            + FPName
            + "\" \"\" \"\" 0 0 0 25000 0 100 \"\"]\n(\n"
            + newElement
            + ")";
        elName = FPName + ".fp";

        // we now write the element to a file
        elementWrite(outputDir + elName, elData);
        // add the FP to our list of converted elements
        convertedFiles.add(elName); 
        newElement = ""; // reset the variable for batch mode

      } else if (currentLine.startsWith("Symbol ")) {
        //String [] tokens = currentLine.split(" "); //unused
        //String SymbolName = tokens[1].replaceAll("[\"]","");// unused
        List<String> silkFeatures = new ArrayList<String>();
        List<String> attributeFields = new ArrayList<String>();
        pins = new PinList(0); // slots = 0
        while (textBXL.hasNextLine() && (lastline != null) &&
               !currentLine.startsWith("EndSymbol")) {
          lastline = textBXL.nextLine();// making nextLine() null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
          if (currentLine.startsWith("Pin")) {
            //System.out.println("#Making new pin: " + currentLine);
            SymbolPin latestPin = new SymbolPin();
            currentLine = currentLine + " " +
                textBXL.nextLine().trim() + " " +
                textBXL.nextLine().trim(); // we combine the 3 lines
            latestPin.populateBXLElement(currentLine);
            pins.addPin(latestPin);
          } else if (currentLine.startsWith("Line") ||
                     currentLine.startsWith("Arc (Layer TOP_SILK")) {
            silkFeatures.add(currentLine);
          } else if (currentLine.startsWith("Attribute")) {
            attributeFields.add(currentLine);
          }
        }

        // now we have a list of pins, we can calculate the offsets
        // to justify the element in gschem, and justify the attribute
        // fields.
        yOffset = pins.minY()-200;  // includes bounding box
        // spacing of ~ 200 takes care of the bounding box
        textXOffset = pins.textRHS();
        // additional bounding box extents are calculated by minY()

        for (String feature : silkFeatures) {
          if (feature.startsWith("Arc (Layer TOP_SILKSCREEN)")) {
            Arc silkArc = new Arc();
            silkArc.populateBXLElement(feature);
            newElement = newElement
                + silkArc.generateGEDAelement(0,-yOffset,1.0f);
          } else if (feature.startsWith("Line")) {
            SymbolPolyline symbolLine = new SymbolPolyline();
            symbolLine.populateBXLElement(feature);
            newElement = newElement
                + "\n" + symbolLine.toString(0,-yOffset);
          } 
        }

        for (String attr : attributeFields) {
          symAttributes = symAttributes
              + SymbolText.BXLAttributeString(textXOffset, 0, attr);
        }

        newSymbol = "v 20110115 1"
            + newElement; // we have created the header for the symbol
        newElement = "";
        silkFeatures.clear();
        attributeFields.clear();
        
      } else if (currentLine.startsWith("Component ")) {
        // we now parse the other attributes for the component
        String [] tokens = currentLine.split(" ");
        String symbolName = tokens[1].replaceAll("[\"]","");
	symbolName = symbolName.replaceAll("[ /]","_"); // c'mon, slashes in filenames? really? //
        while (textBXL.hasNextLine() && (lastline != null) &&
               !currentLine.startsWith("EndComponent")) {
          lastline = textBXL.nextLine();// making nextLine() null safe 
          currentLine = safelyTrim(lastline); // when using gcj libs
          if (currentLine.startsWith("Attribute")) {
            //SymbolText attrText = new SymbolText();
            //attrText.populateBXLElement(currentLine);
            symAttributes = symAttributes
                + SymbolText.BXLAttributeString(textXOffset, 0, currentLine);
          } else if (currentLine.startsWith("RefDesPrefix")) {
            currentLine = currentLine.replaceAll(" ", "");
            currentLine = currentLine.split("\"")[1];
            String refDesAttr = "refdes=" + currentLine + "?";
            symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, refDesAttr);
          } else if (currentLine.startsWith("PatternName")) {
            currentLine = currentLine.replaceAll(" ", "");
            currentLine = currentLine.split("\"")[1];
            String FPAttr = "footprint=" + currentLine;
            symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, FPAttr);
          } else if (currentLine.startsWith("AlternatePattern")) {
            currentLine = currentLine.replaceAll(" ", "");
            currentLine = currentLine.split("\"")[1];
            String AltFPAttr = "alt-footprint=" + currentLine;
            symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, AltFPAttr);
          } else if (currentLine.startsWith("CompPin ")) {
            pins.setBXLPinType(currentLine);
          }
        }

        // we can now put the pieces of the BXL defined symbol together
        elName = symbolName + ".sym";
        elData = newSymbol   // we now add pins to the
            + pins.toString(0,-yOffset) // the header, and then
            + symAttributes; // the final attributes

        // we now write the element to a file
        elementWrite(outputDir + elName, elData);
        // add the symbol to our list of converted elements
        convertedFiles.add(elName);
        // and we rest the variable for the next symbol
        symAttributes = "";
      }
    }
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  } 


  public static void elementWrite(String elementName,
                                  String data) throws IOException {
    try {
      File newElement = new File(elementName);
      PrintWriter elementOutput = new PrintWriter(newElement);
      elementOutput.println(data);
      elementOutput.close();
    } catch(Exception e) {
      System.out.println("There was an error saving: "
                         + elementName); 
      System.out.println(e);
    }
  }

  public static void printHelp() {
    System.out.println("usage:\n\n\tjava BSDL2GEDA BSDLFILE.bsd\n\n"
                       + "options:\n\n"
                       + "\t\t-t\tonly output converted text"
                       + " without further conversion\n\n"
                       + "example:\n\n"
                       + "\tjava BSDL2GEDA BSDLFILE.bsd"
                       + " -t > BSDLFILE.txt\n");

  }

  private static void defaultFileIOError(Exception e) {
        System.out.println("Hmm, that didn't work. "
                           + "Probably a file IO issue:");
        e.printStackTrace();
  }

  // the following method is used to avoid problems
  // with the gcj libs, which seem to occasionally return nulls
  // if hasNext() instead of hasNextLine() is used before
  // calling nextLine() to provide the string
  private static String safelyTrim(String text) {
    if (text != null) {
      return text.trim();
    } else {
      return "";
    }
  } 

  // Thought the following might be needed, but wasn't after all
  // the gcj implementation seems OK
  //  private static String[] safelySplit(String text, String pivot) {
  //  if (text != null) {
  //    String tokens[] = text.split(pivot); 
  //    if (tokens != null) {
  //      return tokens;
  //    } else {
  //      return new String[0];
  //    }
  //  } else {
  //    return new String[0];
  //  }
  // } 
  
}

