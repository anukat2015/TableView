/*
 * @(#) $RCSfile: OpenTableSource.java,v $ $Revision: 1.1 $ $Date: 2003/08/04 18:57:00 $ $Name: TableView1_3 $
 *
 * Center for Computational Genomics and Bioinformatics
 * Academic Health Center, University of Minnesota
 * Copyright (c) 2000-2002. The Regents of the University of Minnesota  
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * see: http://www.gnu.org/copyleft/gpl.html
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 */


package edu.umn.genomics.table;

import java.io.Serializable;
import java.io.*;

/**
 * Associates a named table data source and the
 * TableModel interface to that data source.
 *
 * @author       J Johnson
 * @version $Revision: 1.1 $ $Date: 2003/08/04 18:57:00 $  $Name: TableView1_3 $ 
 * @since        1.0
 */
public interface OpenTableSource {
  /**
   * Open the data source and create a TableModel.
   * @param tableSource The URL or file path for the table data.
   * @throws IOException An exception occurred when opening or reading the File or URL.
   */
  public void openTableSource(String tableSource) throws IOException;
}
