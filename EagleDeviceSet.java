// translate2geda - a utility for turning all the things to gEDA elements
// EagleDeviceSet.java v1.0
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

import java.util.ArrayList;
import java.util.Scanner;

public class EagleDeviceSet {

  String currentLine = "";
  
  ArrayList<EagleDevice> deviceSet = new ArrayList<EagleDevice>();

  // we use this to turn a supplied arraylist of deviceSet descriptions
  // into an ArrayList of EagleDevices
  public EagleDeviceSet(ArrayList<String> theDeviceSets) {
    for (String singleDeviceSet : theDeviceSets) {
      Scanner newSet = new Scanner(singleDeviceSet);
      ArrayList<String> gateNames = new ArrayList<String>();
      ArrayList<String> currentDevice = new ArrayList<String>();
      while (newSet.hasNextLine()) {// seems gcj hasNext != hasNextLine
        currentLine = newSet.nextLine();
        if (currentLine.startsWith("<deviceset ")) {
          String[] deviceSetTokens = currentLine.split(" ");
          for (String field : deviceSetTokens) {
            if (field.startsWith("name=")) {
              // may not need to use the deviceset name
              // since device gate listings include symbol names
              // String deviceSetName
              //   = field.substring(5).replaceAll("\"","");
            }
          }
        } else if (currentLine.startsWith("<gates")) {
          while (!currentLine.startsWith("</gates")) {
            currentLine = newSet.nextLine();
            gateNames.add(currentLine);
            //System.out.println("Added gate description: " + 
            //                   currentLine + " for passing" +
            //                   " to EagleDevice in a moment");
          }
          //          gateCountList.add(gateCount);
        } else if (currentLine.startsWith("<device ")) {
          while (!currentLine.startsWith("</device>")) {
            //currentLine = newSet.nextLine();
            //if (currentLine.startsWith("<device ")) {
              currentDevice.add(currentLine);
              currentLine = newSet.nextLine();
              while (!currentLine.startsWith("</device>")) {
                currentDevice.add(currentLine);
                currentLine = newSet.nextLine();
              }
              currentDevice.add(currentLine);// one device put together
              //System.out.println("Created device desc:");
              //System.out.println(currentDevice);
              //System.out.println("And gate names:");
              //System.out.println(gateNames);
              EagleDevice deviceDesc
                  = new EagleDevice(currentDevice, gateNames);
              deviceSet.add(deviceDesc);
              //}
          }
        }
      }
      newSet.close();
    }
  }

  // may not need this if we return an empty ArrayList from
  // next method
  public boolean containsSymbol(String symbolName) {
    boolean contains = false; 
    //System.out.println("Testing for following symbol " + symbolName);
    for (EagleDevice currentDevice : deviceSet) {
      if (currentDevice.isForSymbol(symbolName)) {
        contains = true;
      }
    }
    return contains;
  }

  // we can use this to return a list of devices using a given
  // symbol name
  public ArrayList<EagleDevice> supplyDevicesFor(String symbolName) {
    ArrayList<EagleDevice> returnedDevices
        = new ArrayList<EagleDevice>();
    for (EagleDevice currentDevice : deviceSet) {
      if (currentDevice.isForSymbol(symbolName)) {
        returnedDevices.add(currentDevice);
      }
    }
    return returnedDevices;
  }

}
