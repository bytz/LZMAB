/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MainJFrame.java
 *
 * Created on Feb 14, 2010, 11:45:32 PM
 */

package bytz.compression;

import SevenZip.ICodeProgress;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Bytz@
 */
public class MainJFrame extends javax.swing.JFrame implements ICodeProgress {

	public class AlgorithmParams
	{
		public int DictionarySize = 1 << 23;

		public int Lc = 3;
		public int Lp = 0;
		public int Pb = 2;

		public int Fb = 128;

		public boolean Eos = false;

		public int Algorithm = 2;
		public int MatchFinder = 1;
	}
	AlgorithmParams params;
	public class EncoderParams{
		public ArrayList<Long> fileLengthPositions;
		public ArrayList<Long> fileLengths;
		public File inFile;
		public File outFile;
		public java.io.BufferedOutputStream outStream;
		public SevenZip.Compression.LZMA.Encoder encoder;
		public long totalSize=0;
	}
	EncoderParams encoderParams;
	public class DecoderParams{
		public ArrayList<Long> filePositions;
		public ArrayList<Long> fileLengths;
		public File inFile;
		public File outFile;
		public java.io.BufferedInputStream inStream;
		public SevenZip.Compression.LZMA.Decoder decoder;
		public RandomAccessFile raf;
		public long totalSize=0;
		public long processedSize=0;
	}
	DecoderParams decoderParams;
	File defaultDir=new File(System.getProperty("user.dir"));
	TreeMap<String,Integer> fileNames=new TreeMap<String,Integer>();
	TreeMap<Integer,String> fileNamesReverse=new TreeMap<Integer,String>();
	TableRowSorter tableRowSorter;
    /** Creates new form MainJFrame */
    public MainJFrame() {
        initComponents();
		dragdrop();
        FilesTableModel dataModel=new FilesTableModel(0,0);
        jTableFiles.setModel(dataModel);
        ((FilesTableModel)jTableFiles.getModel()).addColumn("Filename");
        ((FilesTableModel)jTableFiles.getModel()).addColumn("Size");
		tableRowSorter=new TableRowSorter(dataModel);
		jTableFiles.setRowSorter(tableRowSorter);
		TableModelListener();
		params = new AlgorithmParams();
		archiveFilter();
		createLayout();
    }
	private void TableModelListener(){
		jTableFiles.getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				int column=e.getColumn();
				if(e.getType()==TableModelEvent.UPDATE&&column==0)
					for(int row=e.getFirstRow();row<=e.getLastRow();row++)
						if(row>=0){
							String oldValue=fileNamesReverse.get(row);
							String newValue=(String)jTableFiles.getModel().getValueAt(row,column);
							fileNames.remove(oldValue);
							if(fileNames.containsKey(newValue)){
								fileNames.put(oldValue,row);
								jTableFiles.getModel().setValueAt(oldValue, row, column);
								JOptionPane.showMessageDialog(rootPane, "Duplicate file \""+newValue+"\"!", "Duplicate", JOptionPane.WARNING_MESSAGE);
							}
							else{
								fileNames.put(newValue,row);
								fileNamesReverse.put(row,newValue);
							}
							tableRowSorter.sort();
						}
			}
		});
	}
	private void dragdrop(){
        DropTarget dt=new DropTarget(jScrollPane, new DropTargetListener() {
			@Override
            public void dragEnter(DropTargetDragEvent dtde) {
				if(decoderParams==null)dtde.acceptDrag(1);
				else dtde.rejectDrag();
            }
			@Override
            public void dragOver(DropTargetDragEvent dtde) {
				if(decoderParams==null)dtde.acceptDrag(1);
				else dtde.rejectDrag();
            }
			@Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
				if(decoderParams==null)dtde.acceptDrag(1);
				else dtde.rejectDrag();
            }
			@Override
            public void dragExit(DropTargetEvent dte) {
            }
			@Override
            public void drop(DropTargetDropEvent dtde) {
				dropHandler(dtde);
            }
        });
	}
	private void dropHandler(DropTargetDropEvent dtde){
		dtde.acceptDrop(dtde.getDropAction());
		Transferable transf=dtde.getTransferable();
		List<File> fileList=null;
		DataFlavor[] flavors=transf.getTransferDataFlavors();
		for(int i=0;i<flavors.length;i++)
			if(flavors[i].equals(DataFlavor.javaFileListFlavor)){
				try {
					fileList=(List<File>)transf.getTransferData(DataFlavor.javaFileListFlavor);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(rootPane, ex.getMessage());
				}
			}
			else if(flavors[i].equals(DataFlavor.stringFlavor)){
				try{
					fileList=new ArrayList<File>();
					String stringList=(String)transf.getTransferData(DataFlavor.stringFlavor);
					StringTokenizer strtok=new StringTokenizer(stringList,"\n");
					while(strtok.hasMoreTokens()){
						String string=strtok.nextToken();
						string=string.substring(string.indexOf(File.pathSeparator)+1);
						fileList.add(new File(string));
					}
				}catch(Exception ex){
					JOptionPane.showMessageDialog(rootPane, ex.getMessage());
				}
			}
		if(fileList==null)return;
		for(int i=0;i<fileList.size();i++){
			File file=fileList.get(i);
			String filepath=file.getAbsolutePath();
			String filename=file.getName();//filepath.substring(filepath.lastIndexOf(File.separator)+1);
			if(fileNames.containsKey(filename))JOptionPane.showMessageDialog(rootPane, "Duplicate file \""+filename+"\"!", "Duplicate", JOptionPane.WARNING_MESSAGE);
			else{
				((FilesTableModel)jTableFiles.getModel()).addRow(new String[]{filename,String.valueOf(file.length()),filepath});
				int row=jTableFiles.getRowCount()-1;
				fileNames.put(filename,row);
				fileNamesReverse.put(row,filename);
			}
		}
		jLabelStatus.setText(jTableFiles.getRowCount()+" files");
	}
	private void archiveFilter(){
		FileFilter archiveFilter=new FileFilter() {
			@Override
			public boolean accept(File f) {
				if(f.getName().endsWith(".lzmab")||f.isDirectory())return true;
				else return false;
			}
			@Override
			public String getDescription() {
				return "LZMA archives (*.lzmab)";
			}
		};
		jFileChooserOpen.setFileFilter(archiveFilter);
		jFileChooserSave.setFileFilter(archiveFilter);
	}
	public void setColumnsSizes(){
		int columnSize=(int)(jScrollPane.getWidth()/3f)-1;
		jTableFiles.getColumnModel().getColumn(0).setPreferredWidth(columnSize);
		jTableFiles.getColumnModel().getColumn(1).setPreferredWidth(columnSize);
		jTableFiles.getColumnModel().getColumn(2).setPreferredWidth(columnSize);
	}
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooserSave = new javax.swing.JFileChooser();
        jFileChooserOpen = new javax.swing.JFileChooser();
        jFileChooserExtract = new javax.swing.JFileChooser();
        jFileChooserAdd = new javax.swing.JFileChooser();
        jScrollPane = new javax.swing.JScrollPane();
        jTableFiles = new javax.swing.JTable();
        jPanelBottom = new javax.swing.JPanel();
        jProgressBar = new javax.swing.JProgressBar();
        jLabelStatus = new javax.swing.JLabel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemOpen = new javax.swing.JMenuItem();
        jMenuItemClose = new javax.swing.JMenuItem();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItemAdd = new javax.swing.JMenuItem();
        jMenuItemDelete = new javax.swing.JMenuItem();
        jMenuItemRename = new javax.swing.JMenuItem();
        jMenuItemExtract = new javax.swing.JMenuItem();

        jFileChooserSave.setCurrentDirectory(defaultDir);
        jFileChooserSave.setDialogTitle("Save Archive");
        jFileChooserSave.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        jFileChooserSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooserSaveActionPerformed(evt);
            }
        });

        jFileChooserOpen.setCurrentDirectory(defaultDir);
        jFileChooserOpen.setDialogTitle("Open Archive");
        jFileChooserOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooserOpenActionPerformed(evt);
            }
        });

        jFileChooserExtract.setCurrentDirectory(defaultDir);
        jFileChooserExtract.setDialogTitle("Extract Files");
        jFileChooserExtract.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        jFileChooserExtract.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        jFileChooserExtract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooserExtractActionPerformed(evt);
            }
        });

        jFileChooserAdd.setCurrentDirectory(defaultDir);
        jFileChooserAdd.setDialogTitle("Add Files");
        jFileChooserAdd.setMultiSelectionEnabled(true);
        jFileChooserAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooserAddActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Compression");
        setName("frame"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTableFiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jTableFiles.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTableFiles.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTableFilesKeyPressed(evt);
            }
        });
        jScrollPane.setViewportView(jTableFiles);

        getContentPane().add(jScrollPane, java.awt.BorderLayout.CENTER);

        jPanelBottom.setLayout(new java.awt.BorderLayout());
        jPanelBottom.add(jProgressBar, java.awt.BorderLayout.PAGE_START);

        jLabelStatus.setText("Ready");
        jPanelBottom.add(jLabelStatus, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(jPanelBottom, java.awt.BorderLayout.PAGE_END);

        jMenu1.setText("File");

        jMenuItemOpen.setText("Open");
        jMenuItemOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemOpen);

        jMenuItemClose.setText("Close");
        jMenuItemClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCloseActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemClose);

        jMenuItemSave.setText("Save");
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSave);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemExit);

        jMenuBar.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuItemAdd.setText("Add");
        jMenuItemAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAddActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemAdd);

        jMenuItemDelete.setText("Delete");
        jMenuItemDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeleteActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemDelete);

        jMenuItemRename.setText("Rename");
        jMenuItemRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRenameActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemRename);

        jMenuItemExtract.setText("Extract");
        jMenuItemExtract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExtractActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemExtract);

        jMenuBar.add(jMenu2);

        setJMenuBar(jMenuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jFileChooserSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooserSaveActionPerformed
		if(evt.getActionCommand().equals("ApproveSelection")){
			EncodeThread encodeThread=new EncodeThread(this);
			encodeThread.start();
		}
	}//GEN-LAST:event_jFileChooserSaveActionPerformed

	private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
		if(decoderParams!=null)closeArchive();
		if(encoderParams==null&&decoderParams==null)System.exit(0);
	}//GEN-LAST:event_jMenuItemExitActionPerformed

	private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveActionPerformed
		if(jTableFiles.getRowCount()>0)jFileChooserSave.showSaveDialog(this);
		else JOptionPane.showMessageDialog(rootPane, "Add files to create an archive!", "No files", JOptionPane.WARNING_MESSAGE);
	}//GEN-LAST:event_jMenuItemSaveActionPerformed

    private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenActionPerformed
        jFileChooserOpen.showOpenDialog(this);
    }//GEN-LAST:event_jMenuItemOpenActionPerformed

    private void jFileChooserOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooserOpenActionPerformed
		if(evt.getActionCommand().equals("ApproveSelection")){
			if(decoderParams!=null)closeArchive();
			decoderParams=new DecoderParams();
            decoderParams.inFile=new File(jFileChooserOpen.getSelectedFile().getAbsolutePath());
			if(decoderParams.inFile.exists()){
				viewLayout();
				openArchive();
			}
			else{
				createLayout();
				JOptionPane.showMessageDialog(rootPane, "File \""+decoderParams.inFile.getName()+"\" does not exist!", "No file", JOptionPane.WARNING_MESSAGE);
				decoderParams=null;
			}
        }
    }//GEN-LAST:event_jFileChooserOpenActionPerformed

	private void jMenuItemExtractActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExtractActionPerformed
		jFileChooserExtract.showSaveDialog(this);
	}//GEN-LAST:event_jMenuItemExtractActionPerformed

	private void jFileChooserExtractActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooserExtractActionPerformed
		if(evt.getActionCommand().equals("ApproveSelection")){
			DecodeThread decodeThread=new DecodeThread(this);
			decodeThread.start();
		}
}//GEN-LAST:event_jFileChooserExtractActionPerformed

	private void jMenuItemCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCloseActionPerformed
		closeArchive();
		createLayout();
	}//GEN-LAST:event_jMenuItemCloseActionPerformed

	private void jMenuItemDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDeleteActionPerformed
		int[] selrows=jTableFiles.getSelectedRows();
		int selrowslength=selrows.length;
		while(selrowslength>0){
			String filename=(String)jTableFiles.getModel().getValueAt(selrows[0],0);
			fileNames.remove(filename);
			((FilesTableModel)jTableFiles.getModel()).removeRow(selrows[0]);
			selrows=jTableFiles.getSelectedRows();
			selrowslength--;
		}
		fileNamesReverse.clear();
		for(int i=0;i<jTableFiles.getRowCount();i++)fileNamesReverse.put(i,(String)jTableFiles.getModel().getValueAt(i,0));
		jLabelStatus.setText(jTableFiles.getRowCount()+" files");
	}//GEN-LAST:event_jMenuItemDeleteActionPerformed

	private void jMenuItemAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAddActionPerformed
		jFileChooserAdd.showOpenDialog(this);
	}//GEN-LAST:event_jMenuItemAddActionPerformed

	private void jMenuItemRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRenameActionPerformed
		jTableFiles.editCellAt(jTableFiles.getSelectedRow(), 0);
	}//GEN-LAST:event_jMenuItemRenameActionPerformed

	private void jFileChooserAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooserAddActionPerformed
		if(evt.getActionCommand().equals("ApproveSelection")){
			File[] files=jFileChooserAdd.getSelectedFiles();
			for(int i=0;i<files.length;i++)
				if(files[i].exists()){
					String filepath=files[i].getAbsolutePath();
					String filename=files[i].getName();
					if(fileNames.containsKey(filename))JOptionPane.showMessageDialog(rootPane, "Duplicate file \""+filename+"\"!", "Duplicate", JOptionPane.WARNING_MESSAGE);
					else{
						((FilesTableModel)jTableFiles.getModel()).addRow(new String[]{filename,String.valueOf(files[i].length()),filepath});
						int row=jTableFiles.getRowCount()-1;
						fileNames.put(filename,row);
						fileNamesReverse.put(row,filename);
					}
				}
			jLabelStatus.setText(jTableFiles.getRowCount()+" files");
		}
	}//GEN-LAST:event_jFileChooserAddActionPerformed

	private void jTableFilesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTableFilesKeyPressed
		if(decoderParams==null){
			if(evt.getKeyCode()==KeyEvent.VK_DELETE){
				jMenuItemDeleteActionPerformed(null);
			}
		}
	}//GEN-LAST:event_jTableFilesKeyPressed

	private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
		jMenuItemExitActionPerformed(null);
	}//GEN-LAST:event_formWindowClosing

	public void disableUI(){
		jTableFiles.setEnabled(false);
		jMenu1.setEnabled(false);
		jMenu2.setEnabled(false);
	}
	public void enableUI(){
		jProgressBar.setValue(0);
		jTableFiles.setEnabled(true);
		jMenu1.setEnabled(true);
		jMenu2.setEnabled(true);
	}
	public void createLayout(){
		((FilesTableModel)jTableFiles.getModel()).setRowCount(0);
		((FilesTableModel)jTableFiles.getModel()).setColumnCount(2);
        ((FilesTableModel)jTableFiles.getModel()).addColumn("Path");
		tableRowSorter.toggleSortOrder(0);
		fileNames.clear();
		fileNamesReverse.clear();
		jMenuItemClose.setVisible(false);
		jMenuItemSave.setVisible(true);
		jMenuItemAdd.setVisible(true);
		jMenuItemDelete.setVisible(true);
		jMenuItemExtract.setVisible(false);
		setColumnsSizes();
		jLabelStatus.setText("Ready");
	}
	public void viewLayout(){
		((FilesTableModel)jTableFiles.getModel()).setRowCount(0);
		((FilesTableModel)jTableFiles.getModel()).setColumnCount(2);
		((FilesTableModel)jTableFiles.getModel()).addColumn("Ratio");
		tableRowSorter.toggleSortOrder(0);
		jMenuItemClose.setVisible(true);
		jMenuItemSave.setVisible(false);
		jMenuItemAdd.setVisible(false);
		jMenuItemDelete.setVisible(false);
		jMenuItemExtract.setVisible(true);
		setColumnsSizes();
	}
	@Override
	public void SetProgress(long inSize, long outSize){
		int progress=(int)((float)inSize/encoderParams.totalSize*100);
		jProgressBar.setValue(progress);
	}
	private void createArchive(){
		try{
			java.io.File outFile = encoderParams.outFile;
			java.io.BufferedOutputStream outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile));
			encoderParams.outStream=outStream;
			boolean eos = false;
			if (params.Eos)
				eos = true;
			SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
			encoderParams.encoder=encoder;
			if (!encoder.SetAlgorithm(params.Algorithm))
				throw new Exception("Incorrect compression mode");
			if (!encoder.SetDictionarySize(params.DictionarySize))
				throw new Exception("Incorrect dictionary size");
			if (!encoder.SetNumFastBytes(params.Fb))
				throw new Exception("Incorrect -fb value");
			if (!encoder.SetMatchFinder(params.MatchFinder))
				throw new Exception("Incorrect -mf value");
			if (!encoder.SetLcLpPb(params.Lc, params.Lp, params.Pb))
				throw new Exception("Incorrect -lc or -lp or -pb value");
			encoder.SetEndMarkerMode(eos);
			encoder.WriteCoderProperties(outStream);
			encoder.create(outStream);
			encoderParams.fileLengthPositions=new ArrayList<Long>();
			encoderParams.fileLengths=new ArrayList<Long>();
		}catch(Exception e){
			JOptionPane.showMessageDialog(rootPane, e.getMessage());
		}
	}
	public void encodeFiles(){
		encoderParams=new EncoderParams();
		String outFile=jFileChooserSave.getSelectedFile().getAbsolutePath();
		if(!outFile.endsWith(".lzmab"))outFile+=".lzmab";
		encoderParams.outFile=new File(outFile);
		if(!encoderParams.outFile.exists()||JOptionPane.showConfirmDialog(rootPane,"Overwrite \""+encoderParams.outFile.getName()+"\" ?","Overwrite?",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION){
			List<SortKey> sortKeys=new ArrayList<SortKey>();
			SortKey sortKey=new SortKey(1,SortOrder.ASCENDING);
			sortKeys.add(sortKey);
			tableRowSorter.setSortKeys(sortKeys);
			disableUI();
			jLabelStatus.setText("Compressing...");
			createArchive();
			File[] files=new File[jTableFiles.getRowCount()];
			encoderParams.totalSize=0;
			for(int i=0;i<jTableFiles.getRowCount();i++){
				int pos=jTableFiles.convertRowIndexToModel(i);
				files[i]=new File((String)jTableFiles.getModel().getValueAt(pos,2));
				encoderParams.totalSize+=files[i].length();
			}
			for(int i=0;i<jTableFiles.getRowCount();i++){
				encoderParams.inFile=files[i];
				encodeFile();
			}
			endArchive();
			viewLayout();
			decoderParams=new DecoderParams();
			decoderParams.inFile=encoderParams.outFile;
			encoderParams=null;
			openArchive();
			enableUI();
		}
	}
	private void encodeFile(){
		try{
			java.io.File inFile = encoderParams.inFile;
			java.io.File outFile = encoderParams.outFile;
			java.io.BufferedInputStream inStream  = new java.io.BufferedInputStream(new java.io.FileInputStream(inFile));
			java.io.BufferedOutputStream outStream=encoderParams.outStream;
            String filename=inFile.getName();
            int stringSize=filename.length();
            outStream.write(stringSize);
            byte[] bbuf=new byte[stringSize];
            for(int i=0;i<filename.length();i++)bbuf[i]=(byte)filename.charAt(i);
            outStream.write(bbuf);
			long fileSize;
//			if (params.Eos)
//				fileSize = -1;
//			else
				fileSize = inFile.length();
			for (int i = 0; i < 8; i++)
				outStream.write((int)(fileSize >>> (8 * i)) & 0xFF);
			outStream.flush();
			encoderParams.fileLengthPositions.add(outFile.length());
			long compressedFileSize=0;
			for (int i = 0; i < 8; i++)
				outStream.write((int)(compressedFileSize >>> (8 * i)) & 0xFF);
			outStream.flush();
			long beginPosition=outFile.length();
			encoderParams.encoder.Code(inStream, outStream, -1, -1, this);//null
			outStream.flush();
			encoderParams.fileLengths.add(outFile.length()-beginPosition);
			inStream.close();
		}catch(Exception e){
			JOptionPane.showMessageDialog(rootPane, e.getMessage());
		}
	}
	private void endArchive(){
		try{
			encoderParams.outStream.close();
			RandomAccessFile raf=new RandomAccessFile(encoderParams.outFile,"rw");
			for(int i=0;i<encoderParams.fileLengths.size();i++){
				raf.seek(encoderParams.fileLengthPositions.get(i));
				long compressedFileSize=encoderParams.fileLengths.get(i);
				for(int j = 0; j < 8; j++)
					raf.write((int)(compressedFileSize >>> (8 * j)) & 0xFF);
			}
			raf.close();
		}catch(Exception e){
			JOptionPane.showMessageDialog(rootPane, e.getMessage());
		}
	}
    private void openArchive(){
        try{
			decoderParams.fileLengths=new ArrayList<Long>();
			decoderParams.filePositions=new ArrayList<Long>();
			decoderParams.totalSize=0;
            java.io.File inFile = decoderParams.inFile;
			RandomAccessFile raf=new RandomAccessFile(inFile,"rw");
			decoderParams.raf=raf;
            int propertiesSize = 5;
            byte[] properties = new byte[propertiesSize];
            if (raf.read(properties, 0, propertiesSize) != propertiesSize)
                throw new Exception("input .lzmab file is too short");
            SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
			decoderParams.decoder=decoder;
            if (!decoder.SetDecoderProperties(properties))
                throw new Exception("Incorrect stream properties");
			while(raf.getFilePointer()<raf.length()){
				int stringSize=raf.read();
				byte[] bbuf=new byte[stringSize];
				raf.read(bbuf,0,stringSize);
				char[] cbuf=new char[stringSize];
				for(int i=0;i<bbuf.length;i++)cbuf[i]=(char)bbuf[i];
				String filename=String.valueOf(cbuf);
				long outSize = 0;
				for (int i = 0; i < 8; i++)
				{
					int v = raf.read();
					if (v < 0)
						throw new Exception("Can't read stream size");
					outSize |= ((long)v) << (8 * i);
				}
				decoderParams.fileLengths.add(outSize);
				decoderParams.totalSize+=outSize;
				long compressedSize = 0;
				for (int i = 0; i < 8; i++)
				{
					int v = raf.read();
					if (v < 0)
						throw new Exception("Can't read stream size");
					compressedSize |= ((long)v) << (8 * i);
				}
				long filePosition=raf.getFilePointer();
				decoderParams.filePositions.add(filePosition);
				raf.seek(filePosition+compressedSize);
				((FilesTableModel)jTableFiles.getModel()).addRow(new String[]{filename,String.valueOf(outSize),String.format("%1$.0f",(float)compressedSize/outSize*100)+"%"});//String.valueOf(compressedSize)+" | "+
			}
			setTitle("Compression - "+decoderParams.inFile.getName());
			jLabelStatus.setText(jTableFiles.getRowCount()+" files");
        }catch(Exception e){
			JOptionPane.showMessageDialog(rootPane, e.getMessage());
        }
    }
	public void decodeFiles(){
		disableUI();
		jLabelStatus.setText("Extracting...");
		try{
			decoderParams.decoder.dictInit();
		}catch(Exception e){
			JOptionPane.showMessageDialog(rootPane, e.getMessage());
		}
		String extractPath=jFileChooserExtract.getSelectedFile().getAbsolutePath();
		decoderParams.processedSize=0;
		for(int i=0;i<jTableFiles.getRowCount();i++){
			String filename=(String)jTableFiles.getModel().getValueAt(i,0);
			int pos=jTableFiles.convertRowIndexToView(i);
			decoderParams.outFile=new File(extractPath+File.separatorChar+filename);
			boolean isRowSelected=jTableFiles.isRowSelected(pos);
			if(!isRowSelected||!decoderParams.outFile.exists()||JOptionPane.showConfirmDialog(rootPane,"Overwrite \""+decoderParams.outFile.getName()+"\" ?","Overwrite?",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION)
			decodeFile(i,isRowSelected);
			decoderParams.processedSize+=decoderParams.outFile.length();
			int progress=(int)((float)decoderParams.processedSize/decoderParams.totalSize*100);
			jProgressBar.setValue(progress);
		}
		jLabelStatus.setText(jTableFiles.getRowCount()+" files");
		enableUI();
	}
	private void decodeFile(int i,boolean output){
		java.io.BufferedOutputStream outStream=null;
		try{
			RandomAccessFile raf=new RandomAccessFile(decoderParams.inFile,"rw");
			decoderParams.raf=raf;
            java.io.File outFile = decoderParams.outFile;
			raf.seek(decoderParams.filePositions.get(i));
            java.io.BufferedInputStream inStream  = new java.io.BufferedInputStream(new java.io.FileInputStream(raf.getFD()));
			if(output)outStream = new java.io.BufferedOutputStream(new FileOutputStream(outFile));
			long outSize=decoderParams.fileLengths.get(i);
            if (!decoderParams.decoder.Code(inStream, outStream, outSize))
                throw new Exception("Error in data stream");
        }catch(Exception e){
			JOptionPane.showMessageDialog(rootPane, e.getMessage());
        }
		finally{
			try{
				if(outStream!=null)outStream.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	private void closeArchive(){
		try{
			decoderParams.raf.close();
        }catch(Exception e){
			JOptionPane.showMessageDialog(rootPane, e.getMessage());
        }
		decoderParams=null;
		setTitle("Compression");
	}
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
            public void run() {
                new MainJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFileChooser jFileChooserAdd;
    private javax.swing.JFileChooser jFileChooserExtract;
    private javax.swing.JFileChooser jFileChooserOpen;
    private javax.swing.JFileChooser jFileChooserSave;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenuItem jMenuItemAdd;
    private javax.swing.JMenuItem jMenuItemClose;
    private javax.swing.JMenuItem jMenuItemDelete;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemExtract;
    private javax.swing.JMenuItem jMenuItemOpen;
    private javax.swing.JMenuItem jMenuItemRename;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JScrollPane jScrollPane;
    private javax.swing.JTable jTableFiles;
    // End of variables declaration//GEN-END:variables

}
