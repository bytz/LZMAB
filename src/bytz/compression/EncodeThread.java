/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package bytz.compression;

/**
 *
 * @author Bytz@
 */
public class EncodeThread extends Thread {
	private MainJFrame mainFrame;
	public EncodeThread(MainJFrame mainFrame){
		this.mainFrame=mainFrame;
	}
	@Override
	public void run(){
		mainFrame.encodeFiles();
	}
}
