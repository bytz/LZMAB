/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package bytz.compression;

import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Bytz@
 */
public class FilesTableModel extends DefaultTableModel {
	FilesTableModel(int rows, int columns) {
		super(rows,columns);
	}
	@Override
	public boolean isCellEditable(int row, int column) {
		if(column>0)return false;
		else return true;
	}
}
