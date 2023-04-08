// KicadModuleToGEDA - a utility for turning kicad modules to gEDA PCB footprints
// BXLDecoder.java - a utility for converting Huffman encoded files
// Pad.java v1.1
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
* This class is used to store and process a pad/pin definition string from a Kicad footprint
* allowing it then generate a gEDA PCB compatible footprint
* Oblong hole definitions are processed sensibly, to produce a pair of joined pins, but
* slots implemented this way as a pair of pins with joining pads on both sides of the board
* require gerbers to be post-processed with a G85 directive to join the pins to make a slot
*
*/

import java.util.Scanner;

public class Pad extends FootprintElementArchetype
{
  
  String kicadShapePadName = "not defined";
  String kicadShapeNetName = "not defined yet";
  
  long kicadShapeXsizeNm = 0;
  long kicadShapeYsizeNm = 0;
  
  long kicadShapeXdeltaNm = 0;    // this is used to define trapezoidal pads
                                        // -ve xDelta = decrease in left edge length vs right
                                        // +ve xDelta = increase in right edge length vs left
  long kicadShapeYdeltaNm = 0;    // this is used to define trapezoidal pads
                                        // -ve yDelta = decrease in top edge length vs bottom
                                        // +ve yDelta = increase in bottom edge length vs top
                                        // Note: one or both of xDelta, yDelta, must be zero

  long kicadShapeOrientation = 0; // this is specified in decidegrees by kicad
  
  char kicadDrillShape = '0';
  long kicadDrillOneSizeNm = 0;
  long kicadDrillOneXoffsetNm = 0;
  long kicadDrillOneYoffsetNm = 0;
  char kicadDrillShapeTwo = '0' ;
  long kicadDrillSlotWidthNm = 0;
  long kicadDrillSlotHeightNm = 0;

  long kicadPadPositionXNm = 0;
  long kicadPadPositionYNm = 0;

  String kicadPadAttributeType = "null";

  long gEDAdefaultMetalClearance = 20; // NB defined here in thousandths of an inch = mils
                                        // (clearance/2) = minimum distance from pad/pin metal
                                        // to nearest copper
                                        // this gets multiplied by 100 for 0.01 mil units in output
  long gEDAdefaultSolderMaskRelief = 8; // NB defined here in thousandths of an inch = mils
                                        // solder mask relief
                                        //  = ((thickness of mask aperture) - (pad or pin thickness))
                                        // this gets multiplied by 100 for 0.01 mil units in output

  long gEDAdefaultMinimumDrillSizeNm = 0; // 300000 is big enough to be vaguely sane for vias
  // this can be imposed when creating the module by passing
  // a minimum drill/via size to the setter/constructor

  String topLayerPad = "";     // we use these temporary variables when building slots  
  String bottomLayerPad = "";  // and obroid pads
  
  Boolean equilateralPad = true; // also used when processing obroid, round and circular pads

// Pins and SMD pads have been converted from Kicad foot prints which do not have solder
// mask relief or clearances specified. Default values used for solder mask relief and
// clearance are as specified above.
//
// Users of the foot print must ensure that the solder mask reliefs and clearances are
// compatible with the PCB manufacturer's process tolerances

  String gEDAflag = "blah"; // hex values now deprecated i.e. "0x0000"

  public Pad() // the default constructor simply creates a simple default pad for testing
  {
    kicadShapePadName = "1";
    kicadShapeNetName = "GND";

    kicadShapeXsizeNm = 800*2540;
    kicadShapeYsizeNm = 800*2540;
    kicadShapeXdeltaNm = 0;
    kicadShapeYdeltaNm = 0;
    kicadShapeOrientation = 0;

    kicadDrillShape = 'C';
    kicadDrillOneSizeNm = 600;
    kicadDrillOneXoffsetNm = 0;
    kicadDrillOneYoffsetNm = 0;
    kicadDrillShapeTwo = 'C';
    kicadDrillSlotWidthNm = 0;
    kicadDrillSlotHeightNm = 0;

    kicadPadPositionXNm = 1000*2540;
    kicadPadPositionYNm = 1000*2540;

    kicadPadAttributeType = "STD";

  }

  public void populateElement(String arg, boolean metric, long minimumViaAndDrillSizeNM)
  {
    gEDAdefaultMinimumDrillSizeNm = minimumViaAndDrillSizeNM;
    String parseString = "";
    String[] tokens;

    //              System.out.println("Constructor has been passed:" + arg);

    float parsedValue = 0;

    Scanner padDefinition = new Scanner(arg);

    // the while statement takes care of legacy pad definitions
    // will need to fork here with an if, either legacy or s-file
    // since s-file fits almost all of it on one line

    while (padDefinition.hasNextLine())
      {
        // System.out.println("Now in while loop, processing: " + parseString);
        parseString = padDefinition.nextLine();
        tokens = parseString.split(" ");

        if (tokens[0].startsWith("Sh"))
          {
            kicadShapePadName = tokens[1];
            //  we get rid of odd characters that may interfere with pad naming:
            kicadShapePadName =
                kicadShapePadName.replaceAll("[^a-zA-Z0-9.-]", "_");
            kicadDrillShape = tokens[2].charAt(0);
            //  System.out.println("Drillshape: " + drillShape);
            parsedValue = Float.parseFloat(tokens[3]);
            kicadShapeXsizeNm =
                convertToNanometres(parsedValue, metric);
            parsedValue = Float.parseFloat(tokens[4]);
            kicadShapeYsizeNm =
                convertToNanometres(parsedValue, metric);
            //                              System.out.println(shapeXsize + " " + shapeYsize);
            parsedValue = Float.parseFloat(tokens[5]);
            kicadShapeXdeltaNm =
                convertToNanometres(parsedValue, metric);
            parsedValue = Float.parseFloat(tokens[6]);
            kicadShapeYdeltaNm =
                convertToNanometres(parsedValue, metric);
            kicadShapeOrientation = Integer.parseInt(tokens[7]);
            // this is the rotation of the pad in decidegrees
            // we need orientation to process obround pads
          }
        if (tokens[0].startsWith("Po"))
          {
            parsedValue = Float.parseFloat(tokens[1]);
            kicadPadPositionXNm =
                convertToNanometres(parsedValue, metric);
            //                              System.out.println(padPositionX);
            parsedValue = Float.parseFloat(tokens[2]);
            kicadPadPositionYNm =
                convertToNanometres(parsedValue, metric);
            //                              System.out.println(padPositionY);
          }
        if (tokens[0].startsWith("Ne"))
          {
            kicadShapeNetName = tokens[1];  
            //              we now need to cleanse the NetName of nasties like '$' which sometimes occur
            kicadShapeNetName =
                kicadShapeNetName.replaceAll("[^a-zA-Z0-9.-]", "_");
            //                              System.out.println("Shape's Net name: " + shapeNetName);

          }
        if (tokens[0].startsWith("At"))
          {
            kicadPadAttributeType = tokens[1];
            //                              System.out.println("Pad attribute type: " + padAttributeType);
          }

        if (tokens[0].startsWith("Dr"))
          {
            parsedValue = Float.parseFloat(tokens[1]);
            kicadDrillOneSizeNm =
                convertToNanometres(parsedValue, metric);
            //                              System.out.println("Drill size: " + drillOneSize);
            // we can capture drill x and y offset, but it may not be useful
            parsedValue = Float.parseFloat(tokens[2]);
            kicadDrillOneXoffsetNm =
                convertToNanometres(parsedValue, metric);
            parsedValue = Float.parseFloat(tokens[3]);
            kicadDrillOneYoffsetNm =
                convertToNanometres(parsedValue, metric);
            //                              System.out.println("First hole X and Y offsets: " +
            //                                      drillOneXoffset + "  " + drillOneYoffset);

            // and now we figure out if there is a second hole defined, i.e. slot
            if (tokens.length > 5)
              {
                //                                      System.out.print("hey, there's a second hole");
                //                                      System.out.print(" and it is defined as ");
                kicadDrillShapeTwo = tokens[4].charAt(0);
                //                                      System.out.println(drillShapeTwo);
                parsedValue = Float.parseFloat(tokens[5]);
                kicadDrillSlotWidthNm =
                    convertToNanometres(parsedValue, metric);
                parsedValue = Float.parseFloat(tokens[6]);
                kicadDrillSlotHeightNm = 
                    convertToNanometres(parsedValue, metric);
                //                                      System.out.println("DrillTwoX and DrillTwoY are: " + drillTwoX + "  " + drillTwoY);
              }
            else    // this captures scenarios where the pad is repopulated
              {       // with a pin rather than a slot
                kicadDrillShapeTwo = '0';
              }
          }
        if (parseString.startsWith("pad"))  // we move onto dedicated s-file parsing
          {
            metric = true;
            //                              System.out.println("Parsing s-file pad description");
            //                              while (padDefinition.hasNextLine())
            //                              {
            //                                      parseString = parseString + " " + padDefinition.nextLine();
            //                              }
            tokens = parseString.split(" ");
            // for (int counter = 0; counter < tokens.length; counter++)
            //                              {
            // System.out.println("Pad token #: " + counter + " : " + tokens[counter]);
            //                              }

            // we first grab the pad name
            kicadShapePadName =
                tokens[1].replaceAll("[^a-zA-Z0-9.-]", "_");
            // and rid the Pad Name of nasties like '$' which sometimes occur

            // next we glean the type of pad
            if (tokens[2].startsWith("thru_hole"))
              {
                kicadPadAttributeType = "STD";
              }
            else if (tokens[2].startsWith("smd"))
              {
                kicadPadAttributeType = "SMD";
              }
            else if (tokens[2].startsWith("connect"))
              {
                kicadPadAttributeType = "CONN";
              }
            else if (tokens[2].startsWith("np_thru"))
              {
                kicadPadAttributeType = "HOLE";
              }

            // next comes the shape of the pad or hole
            if (tokens[3].startsWith("circle"))
              {
                kicadDrillShape = 'C';
              }
            else if (tokens[3].startsWith("rect"))
              {
                kicadDrillShape = 'R';
              }
            else if (tokens[3].startsWith("oval"))
              {
                kicadDrillShape = 'O';
              }
            else if (tokens[3].startsWith("trapezoid"))
              {
                kicadDrillShape = 'T';
              }

            // now we parse the less predictable remaining attributes
            for (int parseIndex = 4; parseIndex < tokens.length; parseIndex++)
              {
                if (tokens[parseIndex].startsWith("at"))
                  {
                    parseIndex++;
                    parsedValue = Float.parseFloat(tokens[parseIndex]);
                    kicadPadPositionXNm =
                        convertToNanometres(parsedValue, metric);
                    // System.out.println(padPositionX);
                    parseIndex++;
                    parsedValue = Float.parseFloat(tokens[parseIndex]);
                    kicadPadPositionYNm =
                        convertToNanometres(parsedValue, metric);
                    // System.out.println(padPositionY);
                    kicadShapeOrientation = 0; // set a default value
                    // we now look to see if orientation is specified
                    if (!tokens[parseIndex + 1].startsWith("size"))
                      {
                        // them tricksy kicadians went and changed
                        // from decidegrees to degrees in the s-file
                        // format without telling anyone....
                        // hence the multiplication by 10......
                        kicadShapeOrientation =
                            10*Integer.parseInt(tokens[parseIndex+1]);
                        parseIndex++;
                      }
                  }
                else if (tokens[parseIndex].startsWith("size"))
                  {
                    parseIndex++;
                    parsedValue = Float.parseFloat(tokens[parseIndex]);
                    kicadShapeXsizeNm =
                        convertToNanometres(parsedValue, metric);
                    parseIndex++;
                    parsedValue = Float.parseFloat(tokens[parseIndex]);
                    kicadShapeYsizeNm =
                        convertToNanometres(parsedValue, metric);
                  }
                else if (tokens[parseIndex].startsWith("rect_delta"))
                  {
                    parseIndex++;
                    parsedValue = Float.parseFloat(tokens[parseIndex]);
                    kicadShapeXdeltaNm =
                        convertToNanometres(parsedValue, metric);
                    parseIndex++;
                    parsedValue = Float.parseFloat(tokens[parseIndex]);
                    kicadShapeYdeltaNm =
                        convertToNanometres(parsedValue, metric);
                  }
                else if (tokens[parseIndex].startsWith("drill"))
                  {
                    parseIndex++;
                    // we look to see if it is an oval hole 
                    if (tokens[parseIndex].startsWith("o"))
                      {
                        kicadDrillShapeTwo = 'O';
                        parseIndex++;
                        parsedValue =
                            Float.parseFloat(tokens[parseIndex]);
                        kicadDrillSlotWidthNm = 
                            convertToNanometres(parsedValue, metric);
                        parseIndex++;
                        parsedValue =
                            Float.parseFloat(tokens[parseIndex]);
                        kicadDrillSlotHeightNm =
                            convertToNanometres(parsedValue, metric);
                      }
                    else // it isn't an oval hole
                      {
                        parsedValue =
                            Float.parseFloat(tokens[parseIndex]);
                        kicadDrillOneSizeNm =
                            convertToNanometres(parsedValue, metric);
                        kicadDrillShapeTwo = '0';// flag lack of hole 2
                      }

                    // we now look for x,y offset of hole, if specified
                    if ((parseIndex < (tokens.length - 2)) && 
                        tokens[parseIndex + 1].startsWith("offset"))
                      {
                        parseIndex++; // we step past "offset"
                        parseIndex++; // and get to offsetX
                        parsedValue =
                            Float.parseFloat(tokens[parseIndex]);
                        kicadDrillOneXoffsetNm =
                            convertToNanometres(parsedValue, metric);
                        parseIndex++; // and then onto offsetY
                        parsedValue =
                            Float.parseFloat(tokens[parseIndex]);
                        kicadDrillOneYoffsetNm =
                            convertToNanometres(parsedValue, metric);
                      }
                  }
                else if (tokens[parseIndex].startsWith("net"))
                  {
                    parseIndex++;
                    // we will skip the net number
                    parseIndex++;
                    kicadShapeNetName = tokens[parseIndex];
                    // we now need to cleanse the NetName of
                    // nasties like '$' which sometimes occur
                    kicadShapeNetName = kicadShapeNetName.replaceAll("[^a-zA-Z0-9.-]", "_");
                  }

              }

          }
      }
    //              System.out.println("finished populating pad object");
  }

  public void populateEagleElement(String padDef) {
    String [] tokens = padDef.split(" ");
    for (String token : tokens) {
      if (token.startsWith("<smd")) {
        kicadPadAttributeType = "SMD";
        kicadDrillShape = 'R';
      } else if (token.startsWith("<pad")) {
        kicadPadAttributeType = "STD";
      } else if (token.startsWith("<hole")) {
        kicadPadAttributeType = "HOLE";
        kicadDrillShapeTwo = '0';
        gEDAflag = "hole";
      } else if (token.startsWith("name=")) {
        kicadShapeNetName = token.substring(5).replaceAll("[\"]","");
        kicadShapePadName = kicadShapeNetName;
      } else if (token.startsWith("x=")) {
        token = token.substring(2);
        kicadPadPositionXNm
            = (long)(1000000*Float.parseFloat(token.replaceAll("\"","")));
      } else if (token.startsWith("y=")) {
        token = token.substring(2);
        kicadPadPositionYNm
            = - (long)(1000000*Float.parseFloat(token.replaceAll("\"","")));

        // = (long)(1000000*Float.parseFloat(token.replaceAll("\"","")));
      } else if (token.startsWith("dx=")) {
        token = token.substring(3);
        kicadShapeXsizeNm
            = (long)(1000000*Float.parseFloat(token.replaceAll("\"","")));
      } else if (token.startsWith("dy=")) {
        token = token.substring(3);
        kicadShapeYsizeNm
            = (long)(1000000*Float.parseFloat(token.replaceAll("[\"/>]","")));
      } else if (token.startsWith("drill=")) {
        token = token.substring(6);
        kicadDrillOneSizeNm
            = (long)(1000000*Float.parseFloat(token.replaceAll("[\"/>]","")));
      } else if (token.startsWith("diameter=")) {
        token = token.substring(9);
        kicadShapeYsizeNm
            = (long)(1000000*Float.parseFloat(token.replaceAll("[\">/]","")));
        kicadShapeXsizeNm = kicadShapeYsizeNm;
        kicadDrillShape = 'C';
      } else if (token.startsWith("rot=")) {
        token = token.substring(4);
        kicadShapeOrientation
            = (long)(10*Float.parseFloat(token.replaceAll("[\"R>/]","")));
      } else if (token.startsWith("layer=")) {
        token = token.substring(6);
        if (token.replaceAll("[\"R>/]","").equals("16")) {
            gEDAflag = "onsolder";
        }
      }
    }
    kicadDrillShapeTwo = '0'; // ignore slot possibility for now

  }

  // here, we populate the pad  object with data
  // extracted from a BXL file
  // noting that y-coords are inverted relative
  // to gEDA and kicad
  // may need to think about effect on rotation
  public void populateBXLElement(long w,
                                 long h,
                                 long x,
                                 long y,
                                 char shape,
                                 long rot,
                                 long holeDiam,
                                 String attr,
                                 String pinNum,
                                 String pinName) {

    kicadShapeXsizeNm = w;
    kicadShapeYsizeNm = h;
    kicadPadPositionXNm = x;
    kicadPadPositionYNm = -y;
    kicadDrillShape = shape;
    kicadShapeOrientation = rot;
    kicadDrillOneSizeNm = holeDiam;
    kicadPadAttributeType = attr;
    kicadShapePadName = pinNum;
    kicadShapeNetName = pinName;

    // for now, we do not worry ourselves with slots, so...
    kicadDrillShapeTwo = '0';

  }


  public String generateGEDAelement(long xOffsetNm, long yOffsetNm, float magnificationRatio) 
  // offsets supplied in Nm, magnificationRatio supplied as float, not used as of yet
  {
    //              System.out.println("about to generate a gEDA pad element");
    String output = "For some reason, the pad object was not populated by the constructor";
    /**
     * the first task is to establish if the pad is a pin, pad, hole or square
     * octogons are not supported in kicad
     */
    String oblongSlotFlag = ""; // the default setting of rounded ends for a slot = ""

    // we now check to see if we have had a minimum drill and via size imposed
    // by the Footprint that constructed this pad
    // Note: could have a distinct method to change this, would be useful

    if (kicadDrillOneSizeNm != 0)
      {
        if (kicadDrillOneSizeNm < gEDAdefaultMinimumDrillSizeNm)
          {
            kicadDrillOneSizeNm = gEDAdefaultMinimumDrillSizeNm;
          }
      }

    // one of the first things to do is to establish if
    // the pad is a boring round or square pad, as this will
    // affect our processing of rectangular and obroid pads

    // but before we even do this, we have to catch an annoying deviation
    // from the file format, namely, when the pad shape is given as 'R'
    // and a kicadShapeX size is given but the given kicadShapeY is zero
    // presumably kicad automagically assumes it is square
    // this is an unusual problem, and is probably the result of an
    // eagle-kicad conversion tool quirk

    if (kicadShapeYsizeNm == 0)
      {
        kicadShapeYsizeNm = kicadShapeXsizeNm;
      } // with that out of the way, we can now decide if the pad is "equilateral"

    if (kicadShapeXsizeNm != kicadShapeYsizeNm)
      {
        equilateralPad = false;
      }
    else
      {
        equilateralPad = true;
      }


    switch (kicadDrillShape)
      {
        case 'O': // an obround pad shape can be done as a circle for now
          // probably will need to implement as a pin + a pad on
          // top layer plus, on the bottom "onsolder" layer
          // plus take care of the orientation stuff too
          // when determining stop, start of pad direction
          // which might make the centre pin round or square, too
          // .......but obround pins are a little rare, though
        case 'C':       if (kicadPadAttributeType.startsWith("STD")) // = plate through
          {
            gEDAflag = ""; // WAS "pin"; // "0x0001" now deprecated
          }
        else if (kicadPadAttributeType.startsWith("HOLE")) // = NPTH, round
          {
            gEDAflag = "hole"; // "0x0008" now deprecated
          }
          break;
        case 'R':       if (kicadPadAttributeType.startsWith("STD")) // = plate through
            {
              if (equilateralPad) //  it is square, let it be square
                {
                  gEDAflag = "square"; // "0x0100" now deprecated
                }
              else
                {
                  gEDAflag = ""; // make obroid pad pins rounded
                }
              // i.e. we don't want "square" set for square ended obroid pads
              oblongSlotFlag = "square";
            }
          else if (kicadPadAttributeType.startsWith("HOLE"))
            {
              gEDAflag = "hole"; // "0x0008" now deprecated   
            }
          else if (kicadPadAttributeType.startsWith("SMD") ||
                   kicadPadAttributeType.startsWith("CONN"))
            {
              if (gEDAflag.equals("onsolder"))
                {
                   gEDAflag = "square,onsolder"; // "0x0000" now deprecated
                }
	      else
                {
                   gEDAflag = "square"; // "0x0000" now deprecated
                }
            }
          break;

          /**
           *
           * additional pad "shape" options in, for example, 'Sh "2" C 1500 1500 0 0 2700' definition
           * include 'O' = oblong, 'T' = trapeze
           *
           * this is to be distinguished from from "drillshape" options, such as 'Dr 600 0 0 O 600 650
           * which can be used to specify an oblong 'O' hole which seems to be a slot 
           *
           */

        default:        gEDAflag = "blah";
          break;

      }               
        
    // further refinements would include the addition of a track to make more complicated
    // non square or non round pads, with onsolder and top layer elements

    // here we implement rudimentary support for pad orientation
    // we do this by noting that orientations of 900 or 2700 can be achieved 
    // by exchanging the value of kicadShapeXsizeNm and kicadShapeYsizeNm
    // which will work for simple pads with only one, centred hole
    // for this to work for pads with more than one hole, slot widths and heights
    // will need to be translated as well

    while (kicadShapeOrientation < 0) // we aim to produce 0 =< orientation values =< 3599
      {
        kicadShapeOrientation = 3600 + kicadShapeOrientation;
      }

    if (((kicadShapeOrientation >= 450) &&
         (kicadShapeOrientation <= 1350)) ||
        ((kicadShapeOrientation >= 2250) &&
         (kicadShapeOrientation <= 3150)))
      {
        long tempVal = kicadShapeXsizeNm;
        kicadShapeXsizeNm = kicadShapeYsizeNm;
        kicadShapeYsizeNm = tempVal;
      }
    // simple support for rotation has been effected with a range of orientation values,
    // quantised into either horizontal of vertical
    // To support it more rigorously again, a rotation matrix would be needed 
    // This rudimentary effort could break obround slots which rely on x and y offsets for the slot
    // in the form of slot height and width values, so we can test for those.
    // also, drill offsets in x and y would need translation

    /**
     *
     * the simplest scenario of all is an SMD pad or edge connector and we deal with this first
     * // ? have reworked the dimensions in the copied and pasted code for pins, ok so far
     *
     * also, the problem of pins defined with zero hole size are caught here and treated as a pad
     */
    if ((kicadPadAttributeType.startsWith("SMD") ||
         kicadPadAttributeType.startsWith("CONN")) ||
        (kicadPadAttributeType.startsWith("STD") &&
         (kicadDrillOneSizeNm == 0)))
      {

        // scenario with wider SMD pad than tall, which determines which dimension is used for thickness
        // i.e. shapeYsize is equivalent to gEDA's "thickness" attribute for a pad
        if (kicadShapeXsizeNm >= kicadShapeYsizeNm)
          {
            output = "Pad[" +
                ((xOffsetNm + kicadPadPositionXNm -
                  kicadShapeXsizeNm/2 + kicadShapeYsizeNm/2)/254)
                + " " +
                ((yOffsetNm + kicadPadPositionYNm)/254)
                + " " +
                ((xOffsetNm + kicadPadPositionXNm +
                  kicadShapeXsizeNm/2 - kicadShapeYsizeNm/2)/254)
                + " " +
                ((yOffsetNm + kicadPadPositionYNm)/254)
                + " " +
                (kicadShapeYsizeNm/254) + " " +
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief +
                 (kicadShapeYsizeNm/254)) + " " +
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' +
                gEDAflag +   // flag useful, square vs rounded ends of SMD pad
                '"' + "]\n";
          }

        // scenario with taller SMD pad than wide, which determines which dimension is used for thickness
        // i.e. shapeXsize is equivalent to gEDA's "thickness" attribute for a pad

        else
          {
            output = "Pad[" +
                ((xOffsetNm + kicadPadPositionXNm)/254) + " " +
                ((yOffsetNm + kicadPadPositionYNm - kicadShapeYsizeNm/2 + kicadShapeXsizeNm/2)/254) + " " +
                ((xOffsetNm + kicadPadPositionXNm)/254) + " " +
                ((yOffsetNm + kicadPadPositionYNm + kicadShapeYsizeNm/2 - kicadShapeXsizeNm/2)/254) + " " +
                (kicadShapeXsizeNm/254) + " " +
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' +
                gEDAflag + // sets rounded or square pad
                '"' + "]\n";
          }

      }
    /**
     *
     * the next simplest scenario captures a pin = "HOLE", with no additional hole, so, not an unplated
     * slot, and not a plated hole pin = "STD"
     *
     */

    else if (kicadPadAttributeType.startsWith("HOLE") && (kicadDrillShapeTwo == '0'))
      {
        output = "Pin[" + // square bracket indicates 1/100 mil resolution
            ((kicadPadPositionXNm + xOffsetNm)/254) + " " +
            ((kicadPadPositionYNm + yOffsetNm)/254) + " " +
            (kicadShapeXsizeNm/254) + " " + // pin outer diam., if round = shapeXsize = shapeYsize
            (100*gEDAdefaultMetalClearance) + " " + // gEDA: clearance is specified per pad/pin
            (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
            (kicadDrillOneSizeNm/254) + " " + // drill hole size in 0.01mil units
            '"' + kicadShapeNetName + "\" " + // arbitrary label for pin
            '"' + kicadShapePadName + "\" " + // pin number for attaching nets
            '"' + gEDAflag + '"' + "]\n"; // gEDAflag has already been set to "hole"
      }
        
    /**
     *
     * the next simplest scenario captures a standard pin = "STD" with no additional hole, so, not a slot
     * and also is an equilateralPad = true, so not oblong,
     * and it is a plated hole rather than a mechanical "HOLE"
     * This also captures kicadDrillShape = 'T' for trapezoidal pad shapes, which could be dealt with
     * in another bit of code if support is desirable
     *
     */
    else if (kicadPadAttributeType.startsWith("STD") && (kicadDrillShapeTwo == '0') && (equilateralPad)) // was: && kicadDrillShape != 'O'))
      {
        output = "Pin[" + // square bracket indicates 1/100 mil resolution
            ((kicadPadPositionXNm + xOffsetNm)/254) + " " +
            ((kicadPadPositionYNm + yOffsetNm)/254) + " " +
            (kicadShapeXsizeNm/254) + " " + // pin outer diam., if round = shapeXsize = shapeYsize
            (100*gEDAdefaultMetalClearance) + " " + // gEDA: clearance is specified per pad/pin
            (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
            (kicadDrillOneSizeNm/254) + " " + // drill hole size in 0.01mil units
            '"' + kicadShapeNetName + "\" " + // arbitrary label for pin
            '"' + kicadShapePadName + "\" " + // pin number for attaching nets
            '"' + gEDAflag + '"' + "]\n"; // square bracket indicates 1/100 mil resolution
      }

    /**
     *
     * the next simplest scenario captures a standard pin = "STD" with no additional hole, so, not a slot,
     * but kicadDrillShape = 'O', 'T' or 'R' and not an equilateral pad, making it an obroid pad,
     * and it is a plated hole rather than a mechanical "HOLE"
     *
     * - we also need to consider orientation of the pad in due course with rotational
     * translation of the pad
     * - in the first instance, rotation of 900 or 2700 decigrees can be supported by applying
     * horizontal pad processing for vertical pads, and vice versa
     *
     * we need to process kicadDrillShape = 'R' or 'O' or 'T' and at the same time !equilateralPad"
     *
     * The code produces a standard "STD" plate through hole, round, pin, with the addition
     * of a suitable pad on the top and on the bottom (="onsolder") layers of necessary dimensions
     *
     *
     * There is also the problem of some modules specifiying shape 'R' pads with arbitrary X size
     * but zero Y size, presumably KiCad defaults to a sqaure pad in these cases - We tested for this
     * prior and let y size = x size if y size = 0
     *
     */
    else if (kicadPadAttributeType.startsWith("STD") && (kicadDrillShapeTwo == '0') &&  (!equilateralPad) )
      {
        //                      if (kicadShapeYsizeNm == 0)
        //                      {
        //                              kicadShapeYsizeNm = kicadShapeXsizeNm; 
        //                      }// this could perhaps be done during parsing in case other scenarios exist

        if (kicadShapeXsizeNm >= kicadShapeYsizeNm) // we have a horizontal obroid pad
          {
            output = "Pin[" + // square bracket indicates 1/100 mil resolution
                ((kicadPadPositionXNm + xOffsetNm)/254) + " " +
                ((kicadPadPositionYNm + yOffsetNm)/254) + " " +
                (kicadShapeYsizeNm/254) + " " +
                // pin outer diam., if horizontal obroad pad = shapeYsize
                (100*gEDAdefaultMetalClearance) + " " +
                // gEDA: clearance is specified per pad/pin
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
                (kicadDrillOneSizeNm/254) + " " + // drill hole size in 0.01mil units
                '"' + kicadShapeNetName + "\" " + // arbitrary label for pin
                '"' + kicadShapePadName + "\" " + // pin number for attaching nets
                '"' + gEDAflag + '"' + "]\n";
            // square bracket indicates 1/100 mil resolution

            // rotational transformation could maybe be applied here for
            // kicadPadPositionXNm and kicadPadPositionYNm

            topLayerPad = "Pad[" +
                ((xOffsetNm + kicadPadPositionXNm - kicadShapeXsizeNm/2 + (kicadShapeYsizeNm)/2)/254) + " " + // drillTwoY is the slot height
                ((yOffsetNm + kicadPadPositionYNm)/254) + " " +
                ((xOffsetNm + kicadPadPositionXNm + kicadShapeXsizeNm/2 - (kicadShapeYsizeNm)/2)/254) + // drillTwoY is the slot height
                // it seems that oblong slot's drillTwoX and drillTwoY are absolute slot dimensions
                // not delta x,y vs first hole
                " " +
                ((yOffsetNm + kicadPadPositionYNm)/254) + " " +
                (kicadShapeYsizeNm/254) + " " +
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeYsizeNm/254)) + " " +
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' +
                oblongSlotFlag;

            bottomLayerPad = topLayerPad + ",onsolder";

            output = output + topLayerPad + "\"]\n" + bottomLayerPad + "\"]\n";

          }
        else if (kicadShapeXsizeNm < kicadShapeYsizeNm) // a vertical obroid pad
          {
            output = "Pin[" + // square bracket indicates 1/100 mil resolution
                ((kicadPadPositionXNm + xOffsetNm)/254) + " " +
                ((kicadPadPositionYNm + yOffsetNm)/254) + " " +
                (kicadShapeXsizeNm/254) + " " +
                // pin outer diam., if vertical obroid pad = shapeXsize
                (100*gEDAdefaultMetalClearance) + " " +
                // gEDA: clearance is specified per pad/pin
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
                (kicadDrillOneSizeNm/254) + " " + // drill hole size in 0.01mil units
                '"' + kicadShapeNetName + "\" " + // arbitrary label for pin
                '"' + kicadShapePadName + "\" " + // pin number for attaching nets
                '"' + gEDAflag + '"' + "]\n";
            // square bracket indicates 1/100 mil resolution

            // rotational transformation could maybe be applied here for
            // kicadPadPositionXNm and kicadPadPositionYNm


            topLayerPad = "Pad[" +
                ((xOffsetNm + kicadPadPositionXNm)/254) + " " +
                ((yOffsetNm + kicadPadPositionYNm - kicadShapeYsizeNm/2 + (kicadShapeXsizeNm)/2)/254) + " " +

                ((xOffsetNm + kicadPadPositionXNm)/254) + " " +
                ((yOffsetNm + kicadPadPositionYNm + kicadShapeYsizeNm/2 - (kicadShapeXsizeNm)/2)/254) +

                " " +
                (kicadShapeXsizeNm/254) + " " +
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' +
                oblongSlotFlag;

            bottomLayerPad = topLayerPad + ",onsolder";

            output = output + topLayerPad + "\"]\n" + bottomLayerPad + "\"]\n";



          }
      }


    /**
     *
     * the next section deals with oblong pins which are "STD" and drillShapeTwo = oblong "O"
     * by generating two pins the appropriate distance apart
     *
     * we really want the pins to be rounded, not square, if the drill offsets are
     * on a diagonal, and we first test to see if the oblong hole is on a diagonal 
     *
     * we then join the two pins with a pad of the right length and with rounded ends
     *
     */
    else if (kicadPadAttributeType.startsWith("STD") && (kicadDrillShapeTwo == 'O')) // not zero, but "O" for "Obround" or oval, and a slot
      {
        if (((kicadDrillSlotWidthNm - kicadDrillOneXoffsetNm) != 0) && ((kicadDrillSlotHeightNm - kicadDrillOneYoffsetNm) != 0)) // we capture (hmm, not diagonal, if slot not rotated) slots here
          {
            oblongSlotFlag = "";  // and make the slot ends rounded != "square"
            // i'm starting to think this only rounds off the ends for all slots
          }

        // next, we require an if statement to fork for the vertical or horizontal slot cases

        if (kicadShapeXsizeNm >= kicadShapeYsizeNm) // horizontal slot
          {
            output = "Pin[" + // square bracket indicates 1/100 mil resolution


                ((kicadPadPositionXNm + kicadDrillOneXoffsetNm - kicadDrillSlotWidthNm/2 + xOffsetNm + kicadDrillOneYoffsetNm/2)/254) + " " +
                ((kicadPadPositionYNm + kicadDrillOneYoffsetNm - yOffsetNm)/254) + 
                " " +
                (kicadShapeYsizeNm/254) + " " + // pin outer diam., slot pad height = shapeYsize
                (100*gEDAdefaultMetalClearance) + " " + // gEDA: clearance specified per pad/pin
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeYsizeNm/254)) + " " +
                (kicadDrillSlotHeightNm/254) + " " + // drill two Y size = slot height
                '"' + kicadShapeNetName + "\" " + // arbitrary label for pin
                '"' + kicadShapePadName + "\" " + // pin number for attaching nets
                '"' + 
                oblongSlotFlag + //   gEDAflag + 
                '"' + "]\n" + // square bracket indicates 1/100 ml resolution

                "Pin[" +


                ((kicadPadPositionXNm + kicadDrillOneXoffsetNm + xOffsetNm + kicadDrillSlotWidthNm/2 - kicadDrillOneXoffsetNm/2)/254) + " " +
                ((kicadPadPositionYNm + kicadDrillOneYoffsetNm + yOffsetNm)/254) + 
                " " +
                (kicadShapeYsizeNm/254) + " " + // pin outer diameter
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeYsizeNm/254)) + " " +
                (kicadDrillSlotHeightNm/254) + " " + // drill two Y equals slot height
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' + 
                oblongSlotFlag + //   gEDAflag +
                '"' + "]\n";
          }
        else if (kicadShapeYsizeNm > kicadShapeXsizeNm) // vertical slot
          {

            output = "Pin[" + // square bracket indicates 1/100 mil resolution
                ((kicadPadPositionXNm + kicadDrillOneXoffsetNm + xOffsetNm)/254) + " " +
                ((kicadPadPositionYNm + kicadDrillOneYoffsetNm - kicadDrillSlotHeightNm/2 + yOffsetNm + kicadDrillOneXoffsetNm/2)/254) + " " +
                (kicadShapeXsizeNm/254) + " " + // pin outer diam., if round = shapeXsize = slot pad width
                (100*gEDAdefaultMetalClearance) + " " + // gEDA: clearance specified per pad/pin
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
                (kicadDrillSlotWidthNm/254) + " " + // drill one hole size
                '"' + kicadShapeNetName + "\" " + // arbitrary label for pin
                '"' + kicadShapePadName + "\" " + // pin number for attaching nets
                '"' +
                oblongSlotFlag + //   gEDAflag + 
                '"' + "]\n" + // square bracket indicates 1/100 ml resolution

                "Pin[" +
                ((kicadPadPositionXNm + kicadDrillOneXoffsetNm + xOffsetNm)/254) + " " +
                ((kicadPadPositionYNm + kicadDrillOneYoffsetNm + yOffsetNm + kicadDrillSlotHeightNm/2- kicadDrillOneXoffsetNm/2)/254) + " " +
                (kicadShapeXsizeNm/254) + " " + // pin outer diameter
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
                (kicadDrillSlotWidthNm/254) + " " +
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' +
                oblongSlotFlag + //   gEDAflag +
                '"' + "]\n";


          }


        /**
         *
         * If we are to join two holes with a pad, the pad will need rounded ends, and a thickness
         * that increases in proportion to the sine of the angle/2, so for now we will simply
         * skip the gEDAflag that would probably be "square", and use the oblongSlotFlag instead
         * to keep the ends round - it is simpler this way.
         *
         */


        /**
         *
         * the first case addresses the scenario of a horizontally aligned, rectangular pad
         * or square with two holes making a slot
         *
         */

        if (kicadShapeXsizeNm >= kicadShapeYsizeNm) // i.e. horizontal slot
          {
            topLayerPad = "Pad[" +
                ((xOffsetNm + kicadDrillOneXoffsetNm + kicadPadPositionXNm - kicadShapeXsizeNm/2 + (kicadShapeYsizeNm - kicadDrillSlotHeightNm)/2)/254) + " " + // drillTwoY is the slot height
                ((yOffsetNm + kicadDrillOneYoffsetNm + kicadPadPositionYNm)/254) + " " +
                ((xOffsetNm + kicadDrillOneXoffsetNm + kicadPadPositionXNm + kicadShapeXsizeNm/2 - (kicadShapeYsizeNm - kicadDrillSlotHeightNm)/2)/254) + // drillTwoY is the slot height
                // it seems that oblong slot's drillTwoX and drillTwoY are absolute slot dimensions
                // not delta x,y vs first hole
                " " +
                ((yOffsetNm + kicadDrillOneYoffsetNm + kicadPadPositionYNm)/254) + " " +
                (kicadShapeYsizeNm/254) + " " +
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeYsizeNm/254)) + " " +
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' +
                oblongSlotFlag;

            bottomLayerPad = topLayerPad + ",onsolder";
 
            output = output + topLayerPad + "\"]\n" + bottomLayerPad + "\"]\n";
          }

        /**
         *
         * versus the remaining alternative of a vertically aligned rectangular pad with a hole
         * at each end to make a slot - as of yet nothing done for non orthogonal rectangular or
         * square slot pads, it has been implemented with round pads for diagonal slots
         *
         */
        else // vertical slot
          {
            topLayerPad = "Pad[" +
                ((xOffsetNm + kicadPadPositionXNm + kicadDrillOneXoffsetNm)/254) + " " +
                ((yOffsetNm + kicadPadPositionYNm + kicadDrillOneYoffsetNm - kicadShapeYsizeNm/2 + (kicadShapeXsizeNm - kicadDrillSlotWidthNm)/2)/254) + " " +

                ((xOffsetNm + kicadPadPositionXNm + kicadDrillOneXoffsetNm)/254) + " " +
                ((yOffsetNm + kicadPadPositionYNm + kicadDrillOneYoffsetNm + kicadShapeYsizeNm/2 - (kicadShapeXsizeNm - kicadDrillSlotWidthNm)/2)/254) +

                " " +
                (kicadShapeXsizeNm/254) + " " +
                (100*gEDAdefaultMetalClearance) + " " +
                (100*gEDAdefaultSolderMaskRelief + (kicadShapeXsizeNm/254)) + " " +
                '"' + kicadShapeNetName + "\" " +
                '"' + kicadShapePadName + "\" " +
                '"' +
                oblongSlotFlag;

            bottomLayerPad = topLayerPad + ",onsolder";

            output = output + topLayerPad + "\"]\n" + bottomLayerPad + "\"]\n";

          }
        /**
         *
         * having assembled a slotted oblong pair of pins with a pad between them
         * we return the string containing the gEDA compatible definition
         *
         */
      }
    return output;
                
  }

  private long convertToNanometres(float rawValue, Boolean metricSystem)
  {
    if (metricSystem)
      {
        return (long)(rawValue * 1000000); // 1,000,000 nm per mm
      }
    else
      {
        return (long)(rawValue * 2540); // 2540 nm per 0.1 mil
      }
  }


}
