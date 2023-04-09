// BXLDecoder.java - a utility for converting Huffman encoded files
// into text, ported to Java from the vala code by Geert Jordaens
//
// SourceBuffer.java - an object used to cache and convert
//                     a Huffman encoded file
//
// BXLDecoder.java v1.0
// SourceBuffer.java v1.0
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
//    BXLDecoder Copyright (C) 2016 Erich S. Heinzle a1039181@gmail.com

import java.lang.StringBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SourceBuffer {
  private MappedByteBuffer source_buffer = null;
  private int length = 0;
  private int source_index = 4;
  private int source_char = 0;
  private int bit = 0;

  public SourceBuffer(String BXLFile) {
    Path filename = Paths.get(BXLFile);
    try (FileChannel channel = FileChannel.open(filename)) {
	  length = (int) channel.size();
      source_buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, length);
    } catch (Exception e) {
	  System.out.println("Exception: " + e);
    }
  }

  public int read_next_bit() {
    int result = 0;
    if (bit < 0) {
      // Fetch next byte from source_buffer
      bit = 7;
      // System.out.println("About to get byte number " +
	  // source_index + " from source buffer");
      source_char = source_buffer.get(source_index);
      result = source_char & (1 << bit);
      source_index++;
    } else {
      result = source_char & (1 << bit);
    }
    bit--;
    // System.out.println("bit now: " + bit);
    // System.out.println("source_index now: " + source_index);
    return result;
  }
	
  public int uncompressed_size() {
  /* Uncompressed size = 
     B0b7 * 1<<0 + B0b6 * 1<<1 + ... + B0b0 * 1<<7 + 
     B1b7 * 1<<0 + B1b6 * 1<<1 + ... + B2b0 * 1<<7 + 
     B2b7 * 1<<0 + B2b6 * 1<<1 + ... + B3b0 * 1<<7 + 
     B3b7 * 1<<0 + B3b6 * 1<<1 + ... + B4b0 * 1<<7
   */
   int size = 0;
   int mask = 0;
   for (int i = 7; i >= 0; i--) {
     if ((source_buffer.get(0) & (1 << i)) != 0) {
       size |= (1 << mask);
     }
     mask++;
   }
   for (int i = 7; i >= 0; i--) {
     if ((source_buffer.get(1) & (1 << i)) != 0) {
       size |= (1 << mask);
     }
	mask++;
  }
  for (int i = 7; i >= 0; i--) {
    if ((source_buffer.get(2) & (1 << i)) != 0) {
	  size |= (1 << mask);
	}
    mask++;
  }
  for (int i = 7; i >= 0; i--) {
    if ((source_buffer.get(3) & (1 << i)) != 0) {
	  size |= (1 << mask);
    }
    mask++;
   }
   return size;
}

  public String decode() {

    NodeTree tree = new NodeTree();

    int out_file_length = uncompressed_size();
    // String sb = "";
    // immutable Strings replaced with more efficient string handling, suggested by wlbaker:
    StringBuffer sb = new StringBuffer(out_file_length);
    // System.out.println("About to enter decoding while loop...");
    while (source_index < length && sb.length() != out_file_length) {
      // System.out.println("Have entered decoding while loop...");
      Node node = tree.getRoot();
      // System.out.println("About to enter leaf finding while loop...");
      while (!node.is_leaf()) {
        // find leaf node
        // System.out.println("now searching for leaf node...");
        if (read_next_bit() != 0) {
          // if (read_next_bit(source_index, source_char, bit, source_buffer) != 0) {
          node = node.left;
          // System.out.println("Picking left node, source bit != 0.");
        } else {
          node = node.right;
          // System.out.println("Picking right node, source bit == 0.");
        }
      }
      // System.out.println("Node symbol: " + (char)(node.symbol));
      // System.out.println("Node symbol as toString: " + node);
      sb.append((char) node.symbol); // more efficient string building, thanks wlbaker
      // sb = sb + (char)(node.symbol);
      
      // sb = sb + node;
      // sb = sb + ((char)(node.symbol & 0xff));
      // node.weight += 1;
      node.incrementWeight();
      // System.out.println("decoded text so far is: " +
      // sb + ", now to update tree...");
      tree.update_tree(node);
    }
    // source_buffer = null; // not needed for standalone utility
    // is_filled = false;
    return sb.toString();
  }

}
