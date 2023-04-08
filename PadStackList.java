// translate2geda.java - a utility for converting device definitions
// to geda gschem and PCB compatibl formats
// PadStackList.java interprets BXL padstack definitions
//
// PadStackList.java v1.0
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


import java.util.Scanner;

public class PadStackList {

  PadStack [] list = new PadStack [10]; // default size
  int padStackCount = 0;

  public PadStackList() {
  }

  public PadStackList(String padStackDefinition) {
    addPadStacks(padStackDefinition);
  }

  public void addPadStacks(String definition) { // multi stack def
    Scanner multiPadStackDefinition = new Scanner(definition);
    boolean inPadStack = false;
    String currentLine = "";
    String currentPadStack = "";
    while (multiPadStackDefinition.hasNext()) {
      currentLine = multiPadStackDefinition.nextLine().trim();
      if (currentLine.startsWith("PadStack")) {
        inPadStack = true;
        currentPadStack = currentLine;
      } else if (currentLine.startsWith("EndPadStack")) {
        currentPadStack = currentPadStack + "\n" + currentLine;
        addPadStack(currentPadStack);
        inPadStack = false;        
      } else if (inPadStack) {
        currentPadStack = currentPadStack + "\n" + currentLine;
      }
    }
    multiPadStackDefinition.close();
  }
  
  public void addPadStack(String definition) { // single stack def
    list[padStackCount] = new PadStack(definition);
    padStackCount++;
    if (padStackCount == list.length) {
      PadStack [] newList = new PadStack [list.length * 2]; // double it
      for (int index = 0; index < (list.length -1); index++) {
        newList[index] = list[index];
      }
      list = newList;
    }
  } 

  public String GEDAdef(String identifier, long x, long y, int rot) {
    String returnFP = "";
    for (int index = 0; index < padStackCount; index++) {
      if (list[index].is("identifier")) {
        returnFP = returnFP + list[index].fpText(x, y, rot);
      }
    } 
    return returnFP;
  }

  public Pad GEDAPad(String BXLPadDef) { //, long x, long y, int rot) {
    Pad newGEDAPad = null;
    String [] tempPadDef = BXLPadDef.trim().split(" ");
    String padIdentifier = "";
    for (int index = 0; index < tempPadDef.length - 1; index++) {
      if (tempPadDef[index].equals("(PadStyle")) {
        padIdentifier = tempPadDef[index + 1].replaceAll("[\"():,]","");
      }
    }
    int index = 0;
    //    System.out.println("Sought pad identifier in padstack: "
    //                   + padIdentifier);
    while ((newGEDAPad == null) && (index < padStackCount)) {
      if (list[index].is(padIdentifier)) {
        newGEDAPad = list[index].BXLDefToPad(BXLPadDef);
      }
      index++;
    }
    return newGEDAPad;
  }

}
